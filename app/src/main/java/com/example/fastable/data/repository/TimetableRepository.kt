package com.example.fastable.data.repository

import android.content.Context
import android.util.Log
import com.example.fastable.data.local.AppDatabase
import com.example.fastable.data.models.Course
import com.example.fastable.data.models.TimetableSession
import com.example.fastable.data.remote.CourseExtractor
import com.example.fastable.data.remote.GoogleSheetsService
import com.example.fastable.data.remote.TimetableExtractor
import com.example.fastable.utils.TimeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TimetableRepository(context: Context) {

    private val TAG = "TimetableRepository"
    private val database = AppDatabase.getDatabase(context)
    private val courseDao = database.courseDao()
    private val timetableDao = database.timetableDao()
    private val sheetsService = GoogleSheetsService(context)

    private var lastSyncTime: Long = 0
    private val SYNC_INTERVAL = 30 * 1000 // 30 seconds - very aggressive

    // Cache the spreadsheet to avoid repeated API calls
    private var cachedSpreadsheet: com.google.api.services.sheets.v4.model.Spreadsheet? = null
    private var cacheTime: Long = 0
    private val CACHE_DURATION = 30 * 60 * 1000 // 30 minutes - very long cache
    private var isFetching = false // Prevent multiple simultaneous fetches

    /**
     * Prefetch spreadsheet in background (call on app start)
     */
    suspend fun prefetchSpreadsheet() {
        if (cachedSpreadsheet != null || isFetching) {
            return // Already cached or currently fetching
        }

        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Prefetching spreadsheet in background...")
                getSpreadsheet()
            } catch (e: Exception) {
                Log.e(TAG, "Prefetch failed: ${e.message}")
            }
        }
    }

    /**
     * Clear the cached spreadsheet (forces a fresh fetch on next use)
     */
    fun clearSpreadsheetCache() {
        Log.d(TAG, "Clearing spreadsheet cache")
        cachedSpreadsheet = null
        cacheTime = 0
    }

    /**
     * Get spreadsheet from cache or fetch new
     */
    private suspend fun getSpreadsheet(): com.google.api.services.sheets.v4.model.Spreadsheet? {
        val currentTime = System.currentTimeMillis()

        // Return cached if still valid
        if (cachedSpreadsheet != null && (currentTime - cacheTime) < CACHE_DURATION) {
            Log.d(TAG, "Using cached spreadsheet (age: ${(currentTime - cacheTime) / 1000}s)")
            return cachedSpreadsheet
        }

        // Prevent multiple simultaneous fetches - wait for existing fetch
        if (isFetching) {
            Log.d(TAG, "Already fetching spreadsheet, waiting...")
            var waitCount = 0
            while (isFetching && waitCount < 120) { // Wait up to 12 seconds (10s timeout + 2s buffer)
                kotlinx.coroutines.delay(100)
                waitCount++
            }

            // After waiting, check if we now have a cached result
            if (cachedSpreadsheet != null) {
                Log.d(TAG, "Fetch completed while waiting, using cached result")
                return cachedSpreadsheet
            } else {
                Log.e(TAG, "Wait timed out and no cached spreadsheet available")
                // If still no cache, try fetching ourselves (the previous fetch might have failed)
            }
        }

        isFetching = true
        try {
            // Fetch new
            Log.d(TAG, "Fetching fresh spreadsheet from Google Sheets...")
            val startTime = System.currentTimeMillis()
            val spreadsheet = sheetsService.fetchSpreadsheet()
            val fetchTime = System.currentTimeMillis() - startTime

            if (spreadsheet != null) {
                Log.d(TAG, "Spreadsheet fetched successfully in ${fetchTime}ms")
                cachedSpreadsheet = spreadsheet
                cacheTime = currentTime
            } else {
                Log.e(TAG, "Failed to fetch spreadsheet after ${fetchTime}ms")
            }
            return spreadsheet
        } finally {
            isFetching = false
        }
    }

    /**
     * FAST: Sync only batches (fetch just header rows, not full spreadsheet)
     * This is much faster than syncData() and should be called when user opens batch selection
     */
    suspend fun syncBatchesOnly(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "FAST SYNC: Fetching batches only...")
                val startTime = System.currentTimeMillis()

                // Fetch only header rows (5 rows from Monday) - SUPER FAST
                val headerSpreadsheet = sheetsService.fetchBatchHeaders()
                if (headerSpreadsheet == null) {
                    Log.e(TAG, "Failed to fetch batch headers")
                    return@withContext Result.failure(Exception("Cannot fetch batch data"))
                }

                // Extract batch names only
                val batchNames = TimetableExtractor.extractBatchNamesOnly(headerSpreadsheet)

                if (batchNames.isEmpty()) {
                    Log.w(TAG, "No batches found in headers")
                    return@withContext Result.failure(Exception("No batches found"))
                }

                // Create minimal course entries just for batch display
                // This allows the batch spinner to populate without full course sync
                val minimalCourses = batchNames.map { batch ->
                    Course(
                        name = "Placeholder",  // Placeholder course name
                        department = TimeParser.extractDepartmentFromBatch(batch),
                        section = "A",  // Default section
                        batch = batch,
                        colorCode = "",
                        fullEntry = batch,
                        isSelected = false
                    )
                }

                // Only insert if we don't have courses already
                val existingCourses = courseDao.getAllCoursesOnce()
                if (existingCourses.isEmpty()) {
                    courseDao.insertCourses(minimalCourses)
                    Log.d(TAG, "FAST SYNC: Inserted ${minimalCourses.size} minimal batch entries")
                } else {
                    Log.d(TAG, "FAST SYNC: Courses already exist, skipping insertion")
                }

                val fetchTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "FAST SYNC: Completed in ${fetchTime}ms - ${batchNames.size} batches")

                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "FAST SYNC failed: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * Sync data from Google Sheets
     */
    suspend fun syncData(forceRefresh: Boolean = false): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // PERFORMANCE: Check if we have courses first - skip sync if we do
                val existingCourses = courseDao.getAllCoursesOnce()
                if (existingCourses.isNotEmpty() && !forceRefresh) {
                    Log.d(TAG, "Using existing ${existingCourses.size} courses - SKIP SYNC")
                    return@withContext Result.success(true)
                }

                val currentTime = System.currentTimeMillis()
                if (!forceRefresh && (currentTime - lastSyncTime) < SYNC_INTERVAL) {
                    Log.d(TAG, "Skipping sync - last sync was ${(currentTime - lastSyncTime) / 1000}s ago")
                    return@withContext Result.success(true)
                }

                val spreadsheet = getSpreadsheet()
                if (spreadsheet == null) {
                    return@withContext Result.failure(Exception(
                        "Cannot access Google Sheets data.\n\n" +
                        "Share the spreadsheet with:\n" +
                        "timetable-bot-876@time-table-project-450013.iam.gserviceaccount.com"
                    ))
                }

                // Extract and save courses
                Log.d(TAG, "Extracting courses from spreadsheet...")
                val courses = CourseExtractor.extractAllCourses(spreadsheet)

                // Preserve selection state from existing courses
                val existingSelections = courseDao.getAllCoursesOnce()
                    .filter { it.isSelected }
                    .map { it.getCourseKey() }  // Use full key: name + department + section + batch
                    .toSet()

                // Mark courses as selected if they were previously selected
                val coursesWithSelections = courses.map { course ->
                    val wasSelected = existingSelections.contains(course.getCourseKey())
                    if (wasSelected) course.copy(isSelected = true) else course
                }

                courseDao.deleteAllCourses()
                courseDao.insertCourses(coursesWithSelections)
                Log.d(TAG, "Saved ${courses.size} courses to database (preserved ${existingSelections.size} selections)")

                lastSyncTime = currentTime
                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get sessions from database (INSTANT - no network call)
     */
    fun getSessionsFromDatabase(batch: String, section: String): Flow<List<TimetableSession>> {
        return timetableDao.getSessionsByBatchAndSection(batch, section)
    }

    /**
     * Get sessions from database ONCE (for immediate loading)
     */
    suspend fun getSessionsFromDatabaseOnce(batch: String, section: String): List<TimetableSession> {
        return withContext(Dispatchers.IO) {
            timetableDao.getSessionsByBatchAndSectionOnce(batch, section)
        }
    }

    /**
     * Get batch timetable (with caching for speed)
     * @param forceFresh if true, clears the spreadsheet cache and fetches fresh data
     */
    suspend fun getBatchTimetable(batch: String, section: String, forceFresh: Boolean = false): Result<List<TimetableSession>> {
        return withContext(Dispatchers.IO) {
            try {
                // Clear cache if force refresh requested
                if (forceFresh) {
                    Log.d(TAG, "getBatchTimetable: Force refresh - clearing spreadsheet cache")
                    cachedSpreadsheet = null
                    cacheTime = 0
                }

                // Use cached spreadsheet - INSTANT instead of 40 seconds!
                val spreadsheet = getSpreadsheet()
                if (spreadsheet == null) {
                    return@withContext Result.failure(Exception(
                        "Cannot access Google Sheets data.\n\n" +
                        "Share the spreadsheet with:\n" +
                        "timetable-bot-876@time-table-project-450013.iam.gserviceaccount.com"
                    ))
                }

                val sessions = TimetableExtractor.getBatchTimetable(spreadsheet, batch, section)

                // Save to database
                timetableDao.deleteSessionsForBatchAndSection(batch, section)
                timetableDao.insertSessions(sessions)

                Result.success(sessions)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get timetable", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get custom timetable for selected courses
     */
    suspend fun getCustomTimetable(selectedCourses: List<Course>): Result<List<TimetableSession>> {
        return withContext(Dispatchers.IO) {
            try {
                val spreadsheet = getSpreadsheet()
                    ?: return@withContext Result.failure(Exception("Failed to fetch spreadsheet"))

                val sessions = TimetableExtractor.getCustomTimetable(spreadsheet, selectedCourses)

                // Save to database
                timetableDao.deleteCustomSessions()
                timetableDao.insertSessions(sessions)

                Result.success(sessions)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get all courses (from cache)
     */
    fun getAllCourses(): Flow<List<Course>> {
        return courseDao.getAllCourses()
    }

    /**
     * Search courses
     */
    fun searchCourses(query: String): Flow<List<Course>> {
        return courseDao.searchCourses(query)
    }

    /**
     * Get selected courses
     */
    fun getSelectedCourses(): Flow<List<Course>> {
        return courseDao.getSelectedCourses()
    }

    /**
     * Toggle course selection
     */
    suspend fun toggleCourseSelection(course: Course) {
        withContext(Dispatchers.IO) {
            courseDao.updateCourseSelection(course.id, !course.isSelected)
        }
    }

    /**
     * Clear all selections
     */
    suspend fun clearAllSelections() {
        withContext(Dispatchers.IO) {
            courseDao.clearAllSelections()
        }
    }

    /**
     * Get departments
     */
    fun getDepartments(): Flow<List<String>> {
        return courseDao.getAllDepartments()
    }

    /**
     * Get batches
     */
    fun getBatches(): Flow<List<String>> {
        return courseDao.getAllBatches()
    }

    /**
     * Check if online
     */
    fun isOnline(): Boolean {
        return sheetsService.isOnline()
    }

    /**
     * Dashboard Session Operations
     */

    /**
     * Get all dashboard sessions
     */
    fun getDashboardSessions(): Flow<List<com.example.fastable.data.models.DashboardSession>> {
        return database.dashboardDao().getAllDashboardSessions()
    }

    /**
     * Delete a dashboard session
     */
    suspend fun deleteDashboardSession(session: com.example.fastable.data.models.DashboardSession) {
        withContext(Dispatchers.IO) {
            database.dashboardDao().deleteDashboardSession(session)
        }
    }

    /**
     * Set batch as default (clear old batch sessions and add new)
     */
    suspend fun setDefaultBatch(batch: String, section: String) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "setDefaultBatch: Starting for $batch - Section $section")

            try {
                // Delete all batch (non-custom) sessions
                database.dashboardDao().deleteBatchSessions()
                Log.d(TAG, "setDefaultBatch: Deleted old batch sessions")

                // Clear spreadsheet cache to ensure we use the latest link
                clearSpreadsheetCache()

                // Get sessions for this batch (fetch fresh from updated spreadsheet)
                Log.d(TAG, "setDefaultBatch: Calling getBatchTimetable...")
                val sessions = getBatchTimetable(batch, section, forceFresh = true).getOrNull() ?: emptyList()
                Log.d(TAG, "setDefaultBatch: Retrieved ${sessions.size} sessions from getBatchTimetable")

                if (sessions.isEmpty()) {
                    Log.w(TAG, "setDefaultBatch: No sessions found for $batch - Section $section")
                }

                // Convert to dashboard sessions
                val dashboardSessions = sessions.map { session ->
                    com.example.fastable.data.models.DashboardSession(
                        day = session.day,
                        timeSlot = session.timeSlot,
                        room = session.room,
                        sessionType = session.sessionType,
                        courseName = session.courseName,
                        section = session.section,
                        batch = session.batch,
                        department = session.department,
                        rank = session.rank,
                        colorCode = session.colorCode,
                        isCustom = false
                    )
                }

                Log.d(TAG, "setDefaultBatch: Converted to ${dashboardSessions.size} dashboard sessions")

                // Save new batch sessions
                database.dashboardDao().insertDashboardSessions(dashboardSessions)
                Log.d(TAG, "setDefaultBatch: Inserted dashboard sessions to database")

                // Save default batch info
                val defaultBatch = com.example.fastable.data.models.DefaultBatch(
                    batchName = batch,
                    section = section
                )
                database.defaultBatchDao().setDefaultBatch(defaultBatch)
                Log.d(TAG, "setDefaultBatch: Saved default batch info - COMPLETE")
            } catch (e: Exception) {
                Log.e(TAG, "setDefaultBatch: Exception occurred", e)
                throw e
            }
        }
    }


    /**
     * Add custom courses to dashboard
     */
    suspend fun addCustomCoursesToDashboard(courses: List<Course>) {
        withContext(Dispatchers.IO) {
            // Get spreadsheet to extract full session data
            val spreadsheet = getSpreadsheet() ?: return@withContext

            // Get custom timetable sessions for selected courses
            val sessions = TimetableExtractor.getCustomTimetable(spreadsheet, courses)

            // Get existing dashboard sessions to avoid duplicates
            val existingSessions = database.dashboardDao().getAllDashboardSessionsOnce()

            // Convert to dashboard sessions, filtering out duplicates
            val dashboardSessions = sessions.mapNotNull { session ->
                val newSession = com.example.fastable.data.models.DashboardSession(
                    day = session.day,
                    timeSlot = session.timeSlot,
                    room = session.room,
                    sessionType = session.sessionType,
                    courseName = session.courseName,
                    section = session.section,
                    batch = session.batch,
                    department = session.department,
                    rank = session.rank,
                    colorCode = session.colorCode,
                    isCustom = true
                )

                // Check if this session already exists (same course, day, time, section)
                val isDuplicate = existingSessions.any { existing ->
                    existing.courseName.equals(newSession.courseName, ignoreCase = true) &&
                    existing.day == newSession.day &&
                    existing.timeSlot == newSession.timeSlot &&
                    existing.section == newSession.section
                }

                if (!isDuplicate) newSession else null
            }

            if (dashboardSessions.isNotEmpty()) {
                database.dashboardDao().insertDashboardSessions(dashboardSessions)
                Log.d(TAG, "addCustomCoursesToDashboard: Added ${dashboardSessions.size} new sessions (${sessions.size - dashboardSessions.size} duplicates skipped)")
            } else {
                Log.d(TAG, "addCustomCoursesToDashboard: All sessions already exist in dashboard")
            }
        }
    }

    /**
     * Refresh dashboard sessions to detect cancelled, shifted, or removed classes
     * Fetches fresh data from spreadsheet ONLY for courses currently in dashboard
     *
     * Handles three scenarios:
     * 1. CANCELLED: Cell contains "OS (CS-A) Cancelled" - display as cancelled
     * 2. SHIFTED: Class moved to different room - update with new room
     * 3. REMOVED: Cell cleared - class no longer appears in fresh data
     *
     * @param forceRefresh If true, clears cache and fetches fresh data. If false, uses cached data if available.
     * Returns updated sessions atomically (old sessions replaced only after new ones are ready)
     */
    suspend fun refreshDashboardSessions(
        currentSessions: List<com.example.fastable.data.models.DashboardSession>,
        forceRefresh: Boolean = true
    ): Result<List<com.example.fastable.data.models.DashboardSession>> {
        return withContext(Dispatchers.IO) {
            try {
                if (currentSessions.isEmpty()) {
                    return@withContext Result.success(currentSessions)
                }

                // Clear spreadsheet cache only if force refresh is requested
                if (forceRefresh) {
                    clearSpreadsheetCache()
                }

                // Fetch spreadsheet data (from cache or network)
                val spreadsheet = getSpreadsheet()
                    ?: return@withContext Result.failure(Exception("Failed to fetch spreadsheet"))

                Log.d(TAG, "refreshDashboardSessions: Fetched spreadsheet (forceRefresh=$forceRefresh), processing ${currentSessions.size} sessions")

                // Get unique course base names (without "Cancelled" suffix) from dashboard
                val courseBaseNames = currentSessions.map { session ->
                    session.courseName
                        .replace(" Cancelled", "", ignoreCase = true)
                        .replace("Cancelled", "", ignoreCase = true)
                        .trim()
                }.distinct()

                Log.d(TAG, "refreshDashboardSessions: Looking for ${courseBaseNames.size} unique courses: $courseBaseNames")

                // Create Course objects for lookup - include section info for better matching
                val coursesToLookup = currentSessions.map { session ->
                    val baseName = session.courseName
                        .replace(" Cancelled", "", ignoreCase = true)
                        .replace("Cancelled", "", ignoreCase = true)
                        .trim()
                    Course(
                        id = 0,
                        name = baseName,
                        section = session.section,
                        batch = session.batch,
                        department = session.department,
                        colorCode = session.colorCode,
                        fullEntry = baseName
                    )
                }.distinctBy { "${it.name}_${it.section}_${it.batch}" }

                // Get fresh sessions from spreadsheet
                val freshSessions = TimetableExtractor.getCustomTimetable(spreadsheet, coursesToLookup)
                Log.d(TAG, "refreshDashboardSessions: Found ${freshSessions.size} sessions from spreadsheet")

                // Build lookup map for old sessions: key -> session
                // Key format: "baseName_day_timeSlot_section" for precise matching
                val oldSessionsMap = currentSessions.associateBy { session ->
                    val baseName = session.courseName
                        .replace(" Cancelled", "", ignoreCase = true)
                        .replace("Cancelled", "", ignoreCase = true)
                        .trim()
                    "${baseName}_${session.day}_${session.timeSlot}_${session.section}"
                }


                val refreshedSessions = mutableListOf<com.example.fastable.data.models.DashboardSession>()
                val processedKeys = mutableSetOf<String>()

                // First, process all fresh sessions from spreadsheet
                freshSessions.forEach { freshSession ->
                    val baseName = freshSession.courseName
                        .replace(" Cancelled", "", ignoreCase = true)
                        .replace("Cancelled", "", ignoreCase = true)
                        .trim()
                    val key = "${baseName}_${freshSession.day}_${freshSession.timeSlot}_${freshSession.section}"

                    // Check if this was an existing session
                    val existingSession = oldSessionsMap[key]

                    val dashboardSession = com.example.fastable.data.models.DashboardSession(
                        day = freshSession.day,
                        timeSlot = freshSession.timeSlot,
                        room = freshSession.room,  // May be different if shifted
                        sessionType = freshSession.sessionType,
                        courseName = freshSession.courseName,  // May include "Cancelled"
                        section = freshSession.section,
                        batch = freshSession.batch,
                        department = freshSession.department,
                        rank = freshSession.rank,
                        colorCode = freshSession.colorCode,
                        isCustom = existingSession?.isCustom ?: true,
                        lastUpdated = System.currentTimeMillis()
                    )

                    refreshedSessions.add(dashboardSession)
                    processedKeys.add(key)

                    // Log changes detected
                    if (freshSession.courseName.contains("Cancelled", ignoreCase = true)) {
                        Log.d(TAG, "refreshDashboardSessions: CANCELLED detected - ${freshSession.courseName} on ${freshSession.day}")
                    } else if (existingSession != null && existingSession.room != freshSession.room) {
                        Log.d(TAG, "refreshDashboardSessions: SHIFTED detected - ${freshSession.courseName} moved from ${existingSession.room} to ${freshSession.room}")
                    }
                }

                // Check for sessions that were in old data but not in fresh data (REMOVED)
                currentSessions.forEach { oldSession ->
                    val baseName = oldSession.courseName
                        .replace(" Cancelled", "", ignoreCase = true)
                        .replace("Cancelled", "", ignoreCase = true)
                        .trim()
                    val key = "${baseName}_${oldSession.day}_${oldSession.timeSlot}_${oldSession.section}"

                    if (!processedKeys.contains(key)) {
                        // This session is no longer in the spreadsheet - it was REMOVED
                        Log.d(TAG, "refreshDashboardSessions: REMOVED detected - ${oldSession.courseName} on ${oldSession.day} at ${oldSession.timeSlot} no longer exists")
                        // We intentionally DON'T add it to refreshedSessions - it's been removed
                    }
                }

                Log.d(TAG, "refreshDashboardSessions: Processed ${refreshedSessions.size} sessions (was ${currentSessions.size})")

                // Atomically replace all sessions in database
                database.dashboardDao().clearAllSessions()
                database.dashboardDao().insertDashboardSessions(refreshedSessions)

                Log.d(TAG, "refreshDashboardSessions: Database updated successfully")

                Result.success(refreshedSessions)
            } catch (e: Exception) {
                Log.e(TAG, "refreshDashboardSessions: Failed", e)
                Result.failure(e)
            }
        }
    }
}

