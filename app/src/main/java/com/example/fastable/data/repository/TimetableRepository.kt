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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TimetableRepository(private val context: Context) {

    private val TAG = "TimetableRepository"
    private val database = AppDatabase.getDatabase(context)
    private val courseDao = database.courseDao()
    private val timetableDao = database.timetableDao()
    private val sheetsService = GoogleSheetsService(context)

    private var lastSyncTime: Long = 0
    private val SYNC_INTERVAL = 30 * 1000 // 30 seconds - very aggressive
    fun getLastSyncTimestamp(): Long {
        val prefs = context.getSharedPreferences("fastable_timetable_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("last_refresh_timestamp", 0L)
    }

    fun saveLastSyncTimestamp(timestamp: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences("fastable_timetable_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_refresh_timestamp", timestamp).apply()
        lastSyncTime = timestamp
    }

    init {
        val prefs = context.getSharedPreferences("fastable_timetable_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("cached_batches", null)
        if (!saved.isNullOrEmpty()) {
            _cachedBatchesFlow.value = saved.toList().sorted()
        }
    }

    companion object {
        // Mutex to synchronize all database writes across threads and workers
        val syncMutex = Mutex()

        // Flow to provide batch names extracted during syncBatchesOnly()
        private val _cachedBatchesFlow = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())

        // SHARED cache across all TimetableRepository instances
        // This prevents multiple simultaneous fetches from different workers/activities
        @Volatile
        private var cachedSpreadsheet: com.google.api.services.sheets.v4.model.Spreadsheet? = null
        @Volatile
        private var cacheTime: Long = 0

        // Cache duration: 2 hours for normal use
        // This significantly reduces network calls while still keeping data relatively fresh
        private const val CACHE_DURATION = 2 * 60 * 60 * 1000L // 2 hours

        // Minimum cache age before allowing force refresh (prevents rapid re-fetches)
        // 2 minutes - fast enough for quick refreshes but prevents spam
        private const val MIN_CACHE_AGE_FOR_REFRESH = 2 * 60 * 1000L // 2 minutes

        // Mutex to prevent race conditions during fetch
        private val fetchMutex = Mutex()

        /**
         * Check if cache is fresh enough (less than 5 minutes old)
         * If so, don't allow force refresh to save bandwidth
         */
        fun isCacheFresh(): Boolean {
            val age = System.currentTimeMillis() - cacheTime
            return cachedSpreadsheet != null && age < MIN_CACHE_AGE_FOR_REFRESH
        }

        /**
         * Get cache age in seconds (for logging)
         */
        fun getCacheAgeSeconds(): Long {
            return (System.currentTimeMillis() - cacheTime) / 1000
        }

        /**
         * Clear the shared spreadsheet cache (forces a fresh fetch on next use)
         */
        fun clearSharedCache() {
            Log.d("TimetableRepository", "Clearing shared spreadsheet cache")
            cachedSpreadsheet = null
            cacheTime = 0
            TimetableExtractor.clearCache()
        }
    }

    /**
     * Prefetch spreadsheet in background (call on app start)
     */
    suspend fun prefetchSpreadsheet() {
        if (cachedSpreadsheet != null) {
            return // Already cached
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
        clearSharedCache()
    }

    /**
     * Get spreadsheet from cache or fetch new
     * Uses shared cache and mutex to prevent redundant fetches
     */
    suspend fun getSpreadsheet(forceRefresh: Boolean = false): com.google.api.services.sheets.v4.model.Spreadsheet? {
        if (forceRefresh) {
            clearSpreadsheetCache()
        }
        val currentTime = System.currentTimeMillis()

        // Return cached if still valid (fast path, no lock needed)
        val cached = cachedSpreadsheet
        if (!forceRefresh && cached != null && (currentTime - cacheTime) < CACHE_DURATION) {
            Log.d(TAG, "Using cached spreadsheet (age: ${(currentTime - cacheTime) / 1000}s)")
            return cached
        }

        // Acquire lock to fetch (only one fetch at a time across all instances)
        return fetchMutex.withLock {
            // Double-check after acquiring lock (another thread might have fetched)
            val cachedAfterLock = cachedSpreadsheet
            val timeAfterLock = System.currentTimeMillis()
            if (cachedAfterLock != null && (timeAfterLock - cacheTime) < CACHE_DURATION) {
                Log.d(TAG, "Cache was populated while waiting for lock")
                return@withLock cachedAfterLock
            }

            // Fetch new
            Log.d(TAG, "Fetching fresh spreadsheet from Google Sheets...")
            val startTime = System.currentTimeMillis()
            val spreadsheet = sheetsService.fetchSpreadsheet()
            val fetchTime = System.currentTimeMillis() - startTime

            if (spreadsheet != null) {
                Log.d(TAG, "Spreadsheet fetched successfully in ${fetchTime}ms")
                cachedSpreadsheet = spreadsheet
                cacheTime = System.currentTimeMillis()
            } else {
                Log.e(TAG, "Failed to fetch spreadsheet after ${fetchTime}ms")
            }
            spreadsheet
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

                // Clean up any legacy placeholder courses from previous app versions
                courseDao.deletePlaceholderCourses()

                // Cache batch names in memory and persistent preferences without inserting fake courses
                val prefs = context.getSharedPreferences("fastable_timetable_prefs", Context.MODE_PRIVATE)
                prefs.edit().putStringSet("cached_batches", batchNames.toSet()).apply()
                _cachedBatchesFlow.value = batchNames
                Log.d(TAG, "FAST SYNC: Cached ${batchNames.size} batch names without inserting placeholder courses")

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
                // PERFORMANCE: Check if we have courses first - skip sync if we do (ignore legacy placeholder rows)
                val existingCourses = courseDao.getAllCoursesOnce().filter { !it.name.equals("Placeholder", ignoreCase = true) }
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

                // Preserve selection state from existing courses (ignoring legacy placeholders)
                val existingSelections = courseDao.getAllCoursesOnce()
                    .filter { it.isSelected && !it.name.equals("Placeholder", ignoreCase = true) }
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
                saveLastSyncTimestamp(currentTime)
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
     * Get batches (combines batch names from Room courses and cached batch headers)
     */
    fun getBatches(): Flow<List<String>> {
        return kotlinx.coroutines.flow.combine(
            courseDao.getAllBatches(),
            _cachedBatchesFlow
        ) { dbBatches, memBatches ->
            (dbBatches + memBatches).filter { it.isNotBlank() && !it.equals("Placeholder", ignoreCase = true) }.distinct().sorted()
        }
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
     * Get snapshot of all dashboard sessions directly from database (one-shot)
     */
    suspend fun getDashboardSessionsOnce(): List<com.example.fastable.data.models.DashboardSession> {
        return withContext(Dispatchers.IO) {
            database.dashboardDao().getAllDashboardSessionsOnce()
        }
    }

    fun getActiveRollNo(): String {
        val prefs = context.getSharedPreferences("fastable_timetable_prefs", Context.MODE_PRIVATE)
        return prefs.getString(com.example.fastable.utils.RosterSyncManager.PREF_ROSTER_SYNCED, "")?.trim() ?: ""
    }

    fun getRemovedCourses(): Set<String> {
        val prefs = context.getSharedPreferences("fastable_timetable_prefs", Context.MODE_PRIVATE)
        val rollNo = getActiveRollNo()
        val key = if (rollNo.isNotBlank()) "removed_courses_${rollNo}" else "removed_courses_canonical"
        return prefs.getStringSet(key, emptySet()) ?: emptySet()
    }

    fun addRemovedCourse(canonicalName: String) {
        val prefs = context.getSharedPreferences("fastable_timetable_prefs", Context.MODE_PRIVATE)
        val rollNo = getActiveRollNo()
        val key = if (rollNo.isNotBlank()) "removed_courses_${rollNo}" else "removed_courses_canonical"
        val current = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(canonicalName.lowercase(java.util.Locale.ROOT))
        prefs.edit().putStringSet(key, current).apply()
        val global = prefs.getStringSet("removed_courses_canonical", emptySet())?.toMutableSet() ?: mutableSetOf()
        global.add(canonicalName.lowercase(java.util.Locale.ROOT))
        prefs.edit().putStringSet("removed_courses_canonical", global).apply()
    }

    fun clearRemovedCourse(canonicalName: String) {
        val prefs = context.getSharedPreferences("fastable_timetable_prefs", Context.MODE_PRIVATE)
        val rollNo = getActiveRollNo()
        val key = if (rollNo.isNotBlank()) "removed_courses_${rollNo}" else "removed_courses_canonical"
        val current = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.remove(canonicalName.lowercase(java.util.Locale.ROOT))
        prefs.edit().putStringSet(key, current).apply()
        val global = prefs.getStringSet("removed_courses_canonical", emptySet())?.toMutableSet() ?: mutableSetOf()
        global.remove(canonicalName.lowercase(java.util.Locale.ROOT))
        prefs.edit().putStringSet("removed_courses_canonical", global).apply()
    }

    fun clearAllRemovedCourses() {
        val prefs = context.getSharedPreferences("fastable_timetable_prefs", Context.MODE_PRIVATE)
        val rollNo = getActiveRollNo()
        if (rollNo.isNotBlank()) {
            prefs.edit().remove("removed_courses_${rollNo}").apply()
        }
        prefs.edit().remove("removed_courses_canonical").apply()
    }

    /**
     * Delete an entire course across all days of the week from the dashboard,
     * and persist to removed courses set to permanently prevent it from respawning on refresh.
     */
    suspend fun deleteCourseFromDashboard(session: com.example.fastable.data.models.DashboardSession): List<com.example.fastable.data.models.DashboardSession> {
        return syncMutex.withLock {
            withContext(Dispatchers.IO) {
                val cleanTarget = session.courseName
                    .replace(" Cancelled", "", ignoreCase = true)
                    .replace("Cancelled", "", ignoreCase = true)
                    .trim()
                val targetCanonical = com.example.fastable.utils.CourseNameMatcher.getCanonicalName(cleanTarget)

                // Permanently blacklist from auto-refresh for this student
                addRemovedCourse(targetCanonical)

                val allSessions = database.dashboardDao().getAllDashboardSessionsOnce()
                val sessionsToDelete = allSessions.filter { existing ->
                    val existingClean = existing.courseName
                        .replace(" Cancelled", "", ignoreCase = true)
                        .replace("Cancelled", "", ignoreCase = true)
                        .trim()
                    val existingCanonical = com.example.fastable.utils.CourseNameMatcher.getCanonicalName(existingClean)
                    existingCanonical.equals(targetCanonical, ignoreCase = true) ||
                        existing.courseName.equals(session.courseName, ignoreCase = true) ||
                        existingClean.equals(cleanTarget, ignoreCase = true)
                }

                val idsToDelete = sessionsToDelete.map { it.id }.filter { it > 0 }
                if (idsToDelete.isNotEmpty()) {
                    database.dashboardDao().deleteSessionsByIds(idsToDelete)
                }
                database.dashboardDao().deleteSessionsByExactCourseName(session.courseName)
                if (cleanTarget != session.courseName) {
                    database.dashboardDao().deleteSessionsByExactCourseName(cleanTarget)
                }

                val remaining = database.dashboardDao().getAllDashboardSessionsOnce()
                Log.d(TAG, "deleteCourseFromDashboard: Removed course $targetCanonical (${sessionsToDelete.size} sessions across week). ${remaining.size} sessions remaining.")
                remaining
            }
        }
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
                dashboardSessions.forEach { session ->
                    val clean = session.courseName.replace(" Cancelled", "", ignoreCase = true).trim()
                    clearRemovedCourse(com.example.fastable.utils.CourseNameMatcher.getCanonicalName(clean))
                }
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
     * @param forceRefresh If true, clears cache and fetches fresh data (unless cache is very fresh).
     *                     If false, uses cached data if available.
     * Returns updated sessions atomically (old sessions replaced only after new ones are ready)
     */
    suspend fun refreshDashboardSessions(
        currentSessions: List<com.example.fastable.data.models.DashboardSession>,
        forceRefresh: Boolean = true
    ): Result<List<com.example.fastable.data.models.DashboardSession>> {
        return syncMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val removedCourses = getRemovedCourses()
                    val baseSessions = if (currentSessions.isNotEmpty()) currentSessions else database.dashboardDao().getAllDashboardSessionsOnce()
                    val activeSessions = baseSessions.filter { session ->
                        val clean = session.courseName
                            .replace(" Cancelled", "", ignoreCase = true)
                            .replace("Cancelled", "", ignoreCase = true)
                            .trim()
                        val canonical = com.example.fastable.utils.CourseNameMatcher.getCanonicalName(clean).lowercase(java.util.Locale.ROOT)
                        !removedCourses.contains(canonical)
                    }

                    if (activeSessions.isEmpty()) {
                        Log.d(TAG, "refreshDashboardSessions: No active sessions to refresh")
                        database.dashboardDao().clearAllSessions()
                        saveLastSyncTimestamp(System.currentTimeMillis())
                        return@withContext Result.success(emptyList())
                    }

                    // Smart cache clearing: only clear if force refresh AND cache is not too fresh
                    // This prevents rapid re-fetches when user spams the refresh button
                    if (forceRefresh && !isCacheFresh()) {
                        clearSpreadsheetCache()
                        Log.d(TAG, "refreshDashboardSessions: Cache cleared (age: ${getCacheAgeSeconds()}s)")
                    } else if (forceRefresh && isCacheFresh()) {
                        Log.d(TAG, "refreshDashboardSessions: Using fresh cache (age: ${getCacheAgeSeconds()}s) - skipping network fetch")
                    }

                    // Fetch spreadsheet data (from cache or network)
                    val spreadsheet = getSpreadsheet()
                        ?: return@withContext Result.failure(Exception("Failed to fetch spreadsheet"))

                    Log.d(TAG, "refreshDashboardSessions: Fetched spreadsheet (forceRefresh=$forceRefresh), processing ${activeSessions.size} sessions")

                    // Get unique course base names (without "Cancelled" suffix) from active dashboard sessions
                    val courseBaseNames = activeSessions.map { session ->
                        val clean = session.courseName
                            .replace(" Cancelled", "", ignoreCase = true)
                            .replace("Cancelled", "", ignoreCase = true)
                            .trim()
                        com.example.fastable.utils.CourseNameMatcher.getCanonicalName(clean)
                    }.distinct()

                    Log.d(TAG, "refreshDashboardSessions: Looking for ${courseBaseNames.size} unique courses: $courseBaseNames")

                    // Create Course objects for lookup - using canonical course names and section info for optimal matching
                    val coursesToLookup = activeSessions.map { session ->
                        val clean = session.courseName
                            .replace(" Cancelled", "", ignoreCase = true)
                            .replace("Cancelled", "", ignoreCase = true)
                            .trim()
                        val canonicalName = com.example.fastable.utils.CourseNameMatcher.getCanonicalName(clean)
                        Course(
                            id = 0,
                            name = canonicalName,
                            section = session.section,
                            batch = session.batch,
                            department = session.department,
                            colorCode = session.colorCode,
                            fullEntry = canonicalName
                        )
                    }.distinctBy { "${it.name}_${it.section}_${it.batch}" }

                    // Get fresh sessions from spreadsheet
                    val freshSessions = TimetableExtractor.getCustomTimetable(spreadsheet, coursesToLookup)
                    Log.d(TAG, "refreshDashboardSessions: Found ${freshSessions.size} sessions from spreadsheet")

                    // SAFEGUARD: If 0 sessions returned from spreadsheet, NEVER wipe out the dashboard!
                    if (freshSessions.isEmpty() && activeSessions.isNotEmpty()) {
                        Log.w(TAG, "refreshDashboardSessions: 0 fresh sessions found from spreadsheet, retaining ${activeSessions.size} existing sessions to prevent dashboard wipeout")
                        saveLastSyncTimestamp(System.currentTimeMillis())
                        return@withContext Result.success(activeSessions)
                    }

                    // Build lookup map for old sessions: key -> session
                    // Key format: "canonicalName_day_normalizedTimeSlot_section"
                    val oldSessionsMap = activeSessions.associateBy { session ->
                        val clean = session.courseName
                            .replace(" Cancelled", "", ignoreCase = true)
                            .replace("Cancelled", "", ignoreCase = true)
                            .trim()
                        val canonicalName = com.example.fastable.utils.CourseNameMatcher.getCanonicalName(clean)
                        val normTime = com.example.fastable.utils.TimeParser.normalizeTimeSlot(session.timeSlot)
                        "${canonicalName}_${session.day}_${normTime}_${session.section}"
                    }

                    val refreshedSessions = mutableListOf<com.example.fastable.data.models.DashboardSession>()

                    // Filter spreadsheet sessions against removed courses
                    val validFreshSessions = freshSessions.filterNot { freshSession ->
                        val freshClean = freshSession.courseName.replace(" Cancelled", "", ignoreCase = true).replace("Cancelled", "", ignoreCase = true).trim()
                        val freshCanonical = com.example.fastable.utils.CourseNameMatcher.getCanonicalName(freshClean).lowercase(java.util.Locale.ROOT)
                        removedCourses.contains(freshCanonical)
                    }

                    // Track which courses were found in the spreadsheet
                    val coursesFoundInSpreadsheet = validFreshSessions.map { session ->
                        val freshClean = session.courseName.replace(" Cancelled", "", ignoreCase = true).replace("Cancelled", "", ignoreCase = true).trim()
                        com.example.fastable.utils.CourseNameMatcher.getCanonicalName(freshClean).lowercase(java.util.Locale.ROOT)
                    }.toSet()

                    // Process all fresh sessions from spreadsheet
                    validFreshSessions.forEach { freshSession ->
                        val freshClean = freshSession.courseName
                            .replace(" Cancelled", "", ignoreCase = true)
                            .replace("Cancelled", "", ignoreCase = true)
                            .trim()
                        val freshCanonical = com.example.fastable.utils.CourseNameMatcher.getCanonicalName(freshClean)
                        val normTime = com.example.fastable.utils.TimeParser.normalizeTimeSlot(freshSession.timeSlot)
                        val exactKey = "${freshCanonical}_${freshSession.day}_${normTime}_${freshSession.section}"

                        // Check if matching an existing session (exact slot or shifted slot for this course on same day)
                        val existingSession = oldSessionsMap[exactKey] ?: activeSessions.firstOrNull { old ->
                            val oldClean = old.courseName.replace(" Cancelled", "", ignoreCase = true).replace("Cancelled", "", ignoreCase = true).trim()
                            val oldCanonical = com.example.fastable.utils.CourseNameMatcher.getCanonicalName(oldClean)
                            oldCanonical.equals(freshCanonical, ignoreCase = true) &&
                                    old.day.equals(freshSession.day, ignoreCase = true) &&
                                    old.sessionType.equals(freshSession.sessionType, ignoreCase = true)
                        }

                        val isCancelled = freshSession.courseName.contains("Cancelled", ignoreCase = true)
                        val finalCourseName = if (isCancelled) "$freshCanonical Cancelled" else freshCanonical

                        val dashboardSession = com.example.fastable.data.models.DashboardSession(
                            day = freshSession.day,
                            timeSlot = normTime,
                            room = freshSession.room,  // May be updated if room shifted
                            sessionType = freshSession.sessionType,
                            courseName = finalCourseName,
                            section = freshSession.section,
                            batch = freshSession.batch,
                            department = freshSession.department,
                            rank = freshSession.rank,
                            colorCode = existingSession?.colorCode ?: freshSession.colorCode,
                            isCustom = existingSession?.isCustom ?: true,
                            lastUpdated = System.currentTimeMillis()
                        )

                        refreshedSessions.add(dashboardSession)

                        if (existingSession != null) {
                            if (isCancelled) {
                                Log.d(TAG, "refreshDashboardSessions: CANCELLED detected - $finalCourseName on ${freshSession.day}")
                            } else if (existingSession.room != freshSession.room) {
                                Log.d(TAG, "refreshDashboardSessions: SHIFTED detected - $finalCourseName moved from ${existingSession.room} to ${freshSession.room}")
                            }
                        }
                    }

                    // Reconcile: If Google Sheets found sessions for a course, the spreadsheet sessions
                    // completely replace all old/mock sessions for that course across the entire week.
                    // We ONLY retain an old session if its course was NEVER found in the spreadsheet at all (e.g. custom offline course).
                    activeSessions.forEach { oldSession ->
                        val oldClean = oldSession.courseName
                            .replace(" Cancelled", "", ignoreCase = true)
                            .replace("Cancelled", "", ignoreCase = true)
                            .trim()
                        val oldCanonical = com.example.fastable.utils.CourseNameMatcher.getCanonicalName(oldClean)
                        val canonicalLower = oldCanonical.lowercase(java.util.Locale.ROOT)

                        if (!coursesFoundInSpreadsheet.contains(canonicalLower)) {
                            Log.d(TAG, "refreshDashboardSessions: Retaining offline course session (not in spreadsheet) - ${oldSession.courseName} on ${oldSession.day} at ${oldSession.timeSlot}")
                            val normOldSession = oldSession.copy(
                                timeSlot = com.example.fastable.utils.TimeParser.normalizeTimeSlot(oldSession.timeSlot)
                            )
                            refreshedSessions.add(normOldSession)
                        } else {
                            Log.d(TAG, "refreshDashboardSessions: Course $oldCanonical updated from spreadsheet - dropping old session on ${oldSession.day}")
                        }
                    }

                    // Deduplicate refreshedSessions by (canonicalName, day, normalizedTimeSlot, section)
                    val finalSessions = refreshedSessions.distinctBy { session ->
                        val clean = session.courseName.replace(" Cancelled", "", ignoreCase = true).replace("Cancelled", "", ignoreCase = true).trim()
                        val canonical = com.example.fastable.utils.CourseNameMatcher.getCanonicalName(clean)
                        val normTime = com.example.fastable.utils.TimeParser.normalizeTimeSlot(session.timeSlot)
                        "${canonical.lowercase(java.util.Locale.ROOT)}_${session.day.lowercase(java.util.Locale.ROOT)}_${normTime}_${session.section.lowercase(java.util.Locale.ROOT)}"
                    }

                    Log.d(TAG, "refreshDashboardSessions: Processed ${finalSessions.size} sessions (was ${activeSessions.size})")

                    // Atomically replace all sessions in database
                    database.dashboardDao().clearAllSessions()
                    database.dashboardDao().insertDashboardSessions(finalSessions)

                    // Query database to return rows with valid auto-generated SQLite primary keys (id > 0)
                    val freshSavedSessions = database.dashboardDao().getAllDashboardSessionsOnce()
                    Log.d(TAG, "refreshDashboardSessions: Database updated successfully with ${freshSavedSessions.size} sessions")

                    saveLastSyncTimestamp(System.currentTimeMillis())
                    Result.success(freshSavedSessions)
                } catch (e: Exception) {
                    Log.e(TAG, "refreshDashboardSessions: Failed", e)
                    Result.failure(e)
                }
            }
        }
    }
}


