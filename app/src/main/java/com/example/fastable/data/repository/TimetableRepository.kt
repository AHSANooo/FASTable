package com.example.fastable.data.repository

import android.content.Context
import android.util.Log
import com.example.fastable.data.local.AppDatabase
import com.example.fastable.data.models.Course
import com.example.fastable.data.models.TimetableSession
import com.example.fastable.data.remote.CourseExtractor
import com.example.fastable.data.remote.GoogleSheetsService
import com.example.fastable.data.remote.TimetableExtractor
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
     */
    suspend fun getBatchTimetable(batch: String, section: String): Result<List<TimetableSession>> {
        return withContext(Dispatchers.IO) {
            try {
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

                // Get sessions for this batch
                Log.d(TAG, "setDefaultBatch: Calling getBatchTimetable...")
                val sessions = getBatchTimetable(batch, section).getOrNull() ?: emptyList()
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
     * Refresh dashboard sessions to detect cancelled classes
     * Fetches fresh data from spreadsheet for existing courses in dashboard
     * Returns updated sessions atomically (old sessions are replaced only after new are ready)
     */
    suspend fun refreshDashboardSessions(currentSessions: List<com.example.fastable.data.models.DashboardSession>): Result<List<com.example.fastable.data.models.DashboardSession>> {
        return withContext(Dispatchers.IO) {
            try {
                if (currentSessions.isEmpty()) {
                    return@withContext Result.success(currentSessions)
                }

                // Fetch fresh spreadsheet data
                val spreadsheet = getSpreadsheet()
                    ?: return@withContext Result.failure(Exception("Failed to fetch spreadsheet"))

                Log.d(TAG, "refreshDashboardSessions: Fetched spreadsheet, processing ${currentSessions.size} sessions")

                // Get unique course names (base names without "Cancelled" suffix)
                val courseBaseNames = currentSessions.map { session ->
                    session.courseName.replace(" Cancelled", "", ignoreCase = true).trim()
                }.distinct()

                Log.d(TAG, "refreshDashboardSessions: Looking for courses: $courseBaseNames")

                // Create Course objects for lookup
                val coursesToLookup = courseBaseNames.map { courseName ->
                    Course(
                        id = 0,
                        name = courseName,
                        section = "",
                        batch = "",
                        department = "",
                        colorCode = "",
                        fullEntry = courseName
                    )
                }

                // Get fresh sessions from spreadsheet
                val freshSessions = TimetableExtractor.getCustomTimetable(spreadsheet, coursesToLookup)
                Log.d(TAG, "refreshDashboardSessions: Found ${freshSessions.size} sessions from spreadsheet")

                // Build a map of existing sessions for quick lookup
                val existingSessionsMap = currentSessions.associateBy { session ->
                    "${session.courseName.replace(" Cancelled", "", ignoreCase = true).trim()}_${session.day}_${session.section}"
                }

                // Process fresh sessions - detect cancelled ones
                val refreshedSessions = mutableListOf<com.example.fastable.data.models.DashboardSession>()
                val processedKeys = mutableSetOf<String>()

                freshSessions.forEach { freshSession ->
                    val baseName = freshSession.courseName.replace(" Cancelled", "", ignoreCase = true).trim()
                    val key = "${baseName}_${freshSession.day}_${freshSession.section}"


                    val dashboardSession = com.example.fastable.data.models.DashboardSession(
                        day = freshSession.day,
                        timeSlot = freshSession.timeSlot,
                        room = freshSession.room,
                        sessionType = freshSession.sessionType,
                        courseName = freshSession.courseName, // Keep as-is (may include "Cancelled")
                        section = freshSession.section,
                        batch = freshSession.batch,
                        department = freshSession.department,
                        rank = freshSession.rank,
                        colorCode = freshSession.colorCode,
                        isCustom = existingSessionsMap[key]?.isCustom ?: true
                    )

                    refreshedSessions.add(dashboardSession)
                    processedKeys.add(key)
                }

                Log.d(TAG, "refreshDashboardSessions: Processed ${refreshedSessions.size} sessions, ${processedKeys.size} unique keys")

                // Atomically replace all sessions in database
                // This ensures no empty state and no duplicates
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

