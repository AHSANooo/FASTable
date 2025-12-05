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
    private val SYNC_INTERVAL = 5 * 60 * 1000 // 5 minutes

    // Cache the spreadsheet to avoid repeated API calls
    private var cachedSpreadsheet: com.google.api.services.sheets.v4.model.Spreadsheet? = null
    private var cacheTime: Long = 0
    private val CACHE_DURATION = 10 * 60 * 1000 // 10 minutes

    /**
     * Get spreadsheet from cache or fetch new
     */
    private suspend fun getSpreadsheet(): com.google.api.services.sheets.v4.model.Spreadsheet? {
        val currentTime = System.currentTimeMillis()

        // Return cached if still valid
        if (cachedSpreadsheet != null && (currentTime - cacheTime) < CACHE_DURATION) {
            return cachedSpreadsheet
        }

        // Fetch new
        val spreadsheet = sheetsService.fetchSpreadsheet()
        if (spreadsheet != null) {
            cachedSpreadsheet = spreadsheet
            cacheTime = currentTime
        }
        return spreadsheet
    }

    /**
     * Sync data from Google Sheets
     */
    suspend fun syncData(forceRefresh: Boolean = false): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()
                if (!forceRefresh && (currentTime - lastSyncTime) < SYNC_INTERVAL) {
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
                val courses = CourseExtractor.extractAllCourses(spreadsheet)
                courseDao.deleteAllCourses()
                courseDao.insertCourses(courses)

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
}
