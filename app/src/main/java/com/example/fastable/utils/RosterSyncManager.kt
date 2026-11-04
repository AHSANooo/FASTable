package com.example.fastable.utils

import android.content.Context
import android.util.Log
import com.example.fastable.data.local.AppDatabase
import com.example.fastable.data.models.DashboardSession
import com.example.fastable.data.models.DefaultBatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Deliverable D3 - Concept 1 (R5, R4):
 * Roster-Synced Schedule Setup via Roll Number.
 *
 * Extracts the student's Roll Number from their authenticated university email
 * and automatically populates their dashboard timetable with their official section schedule
 * (prototype mock: BS CS Section E).
 */
object RosterSyncManager {

    private const val TAG = "RosterSyncManager"
    const val PREF_ROSTER_SYNCED = "roster_synced_roll_no"

    /**
     * Extracts a normalized Roll Number from an email.
     * Examples:
     *   "i230553@isb.nu.edu.pk" -> "23I-0553"
     *   "f210666@nu.edu.pk"     -> "21F-0666"
     *   "p220123@nu.edu.pk"     -> "22P-0123"
     *   Fallback or manual: formatted or default "23I-0553"
     */
    fun extractRollNoFromEmail(email: String?): String {
        if (email.isNullOrBlank()) return "23I-0553"

        val localPart = email.substringBefore("@").trim().lowercase(Locale.ROOT)

        // Pattern 1: e.g. "i230553" -> campus letter 'i', batch '23', roll '0553'
        val regex1 = Regex("^([a-z])(\\d{2})(\\d{3,5})$")
        val match1 = regex1.find(localPart)
        if (match1 != null) {
            val (campus, year, roll) = match1.destructured
            return "${year}${campus.uppercase(Locale.ROOT)}-$roll"
        }

        // Pattern 2: e.g. "23i-0553" or "23i0553"
        val regex2 = Regex("^(\\d{2})([a-z])-?(\\d{3,5})$")
        val match2 = regex2.find(localPart)
        if (match2 != null) {
            val (year, campus, roll) = match2.destructured
            return "${year}${campus.uppercase(Locale.ROOT)}-$roll"
        }

        return "23I-0553"
    }

    /**
     * Returns the official BS CS Section E weekly courses for prototype verification.
     */
    fun getMockCSSectionESchedule(): List<DashboardSession> {
        val now = System.currentTimeMillis()
        val batch = "BS CS (2023)"
        val section = "CS-E"
        val dept = "CS"

        return listOf(
            // Monday
            DashboardSession(
                day = "Monday",
                timeSlot = "10:00 - 11:20",
                room = "C-301",
                sessionType = "Class",
                courseName = "Data Structures",
                section = section,
                batch = batch,
                department = dept,
                rank = 1,
                colorCode = "#E05638",
                isCustom = false,
                lastUpdated = now
            ),
            DashboardSession(
                day = "Monday",
                timeSlot = "11:30 - 12:50",
                room = "C-304",
                sessionType = "Class",
                courseName = "Linear Algebra",
                section = section,
                batch = batch,
                department = dept,
                rank = 2,
                colorCode = "#3B82F6",
                isCustom = false,
                lastUpdated = now
            ),
            DashboardSession(
                day = "Monday",
                timeSlot = "01:00 - 02:20",
                room = "C-307",
                sessionType = "Class",
                courseName = "Technical & Business Writing",
                section = section,
                batch = batch,
                department = dept,
                rank = 3,
                colorCode = "#8B5CF6",
                isCustom = false,
                lastUpdated = now
            ),

            // Tuesday
            DashboardSession(
                day = "Tuesday",
                timeSlot = "08:30 - 09:50",
                room = "C-302",
                sessionType = "Class",
                courseName = "Computer Organization & Assembly",
                section = section,
                batch = batch,
                department = dept,
                rank = 1,
                colorCode = "#10B981",
                isCustom = false,
                lastUpdated = now
            ),
            DashboardSession(
                day = "Tuesday",
                timeSlot = "10:00 - 11:20",
                room = "C-305",
                sessionType = "Class",
                courseName = "Discrete Structures",
                section = section,
                batch = batch,
                department = dept,
                rank = 2,
                colorCode = "#F59E0B",
                isCustom = false,
                lastUpdated = now
            ),
            DashboardSession(
                day = "Tuesday",
                timeSlot = "11:30 - 02:20",
                room = "Margalla Lab 1",
                sessionType = "Lab",
                courseName = "Data Structures Lab",
                section = section,
                batch = batch,
                department = dept,
                rank = 3,
                colorCode = "#E05638",
                isCustom = false,
                lastUpdated = now
            ),

            // Wednesday
            DashboardSession(
                day = "Wednesday",
                timeSlot = "10:00 - 11:20",
                room = "C-301",
                sessionType = "Class",
                courseName = "Data Structures",
                section = section,
                batch = batch,
                department = dept,
                rank = 1,
                colorCode = "#E05638",
                isCustom = false,
                lastUpdated = now
            ),
            DashboardSession(
                day = "Wednesday",
                timeSlot = "11:30 - 12:50",
                room = "C-304",
                sessionType = "Class",
                courseName = "Linear Algebra",
                section = section,
                batch = batch,
                department = dept,
                rank = 2,
                colorCode = "#3B82F6",
                isCustom = false,
                lastUpdated = now
            ),
            DashboardSession(
                day = "Wednesday",
                timeSlot = "01:00 - 02:20",
                room = "C-307",
                sessionType = "Class",
                courseName = "Technical & Business Writing",
                section = section,
                batch = batch,
                department = dept,
                rank = 3,
                colorCode = "#8B5CF6",
                isCustom = false,
                lastUpdated = now
            ),

            // Thursday
            DashboardSession(
                day = "Thursday",
                timeSlot = "08:30 - 09:50",
                room = "C-302",
                sessionType = "Class",
                courseName = "Computer Organization & Assembly",
                section = section,
                batch = batch,
                department = dept,
                rank = 1,
                colorCode = "#10B981",
                isCustom = false,
                lastUpdated = now
            ),
            DashboardSession(
                day = "Thursday",
                timeSlot = "10:00 - 12:50",
                room = "Rawal Lab 1",
                sessionType = "Lab",
                courseName = "Assembly Language Lab",
                section = section,
                batch = batch,
                department = dept,
                rank = 2,
                colorCode = "#10B981",
                isCustom = false,
                lastUpdated = now
            ),

            // Friday
            DashboardSession(
                day = "Friday",
                timeSlot = "08:30 - 09:50",
                room = "C-305",
                sessionType = "Class",
                courseName = "Discrete Structures",
                section = section,
                batch = batch,
                department = dept,
                rank = 1,
                colorCode = "#F59E0B",
                isCustom = false,
                lastUpdated = now
            )
        )
    }

    /**
     * Executes Roster Sync for the given roll number:
     * 1. Replaces dashboard sessions in Room DB with CS Section E schedule.
     * 2. Sets default batch to "BS CS (2023)" Section "CS-E".
     * 3. Records persistent sync timestamp.
     * 4. Attempts native Google Calendar auto-sync.
     */
    suspend fun syncRosterForStudent(context: Context, rollNo: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val sessions = getMockCSSectionESchedule()

                // Replace old batch sessions in Room DB
                db.dashboardDao().deleteBatchSessions()
                db.dashboardDao().insertDashboardSessions(sessions)

                // Save default batch metadata
                db.defaultBatchDao().setDefaultBatch(
                    DefaultBatch(
                        batchName = "BS CS (2023)",
                        section = "CS-E"
                    )
                )

                // Save sync preferences
                val prefs = context.getSharedPreferences("fastable_timetable_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString(PREF_ROSTER_SYNCED, rollNo)
                    .putLong("timetable_last_sync_timestamp", System.currentTimeMillis())
                    .apply()

                // Optional calendar sync if permission granted
                try {
                    if (PermissionManager.hasCalendarPermission(context)) {
                        GoogleCalendarSyncManager.syncCurrentWeek(context, sessions)
                    }
                } catch (calEx: Exception) {
                    Log.w(TAG, "Calendar auto-sync deferred: ${calEx.message}")
                }

                Log.d(TAG, "Roster sync successful for $rollNo: inserted ${sessions.size} sessions")
                Result.success(sessions.size)
            } catch (e: Exception) {
                Log.e(TAG, "Roster sync failed for $rollNo", e)
                Result.failure(e)
            }
        }
    }
}

