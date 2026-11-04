package com.example.fastable.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import com.example.fastable.data.models.DashboardSession
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Handles synchronizing current week FASTable course sessions to Google Calendar
 * using the Android native CalendarContract provider.
 */
object GoogleCalendarSyncManager {

    private const val TAG = "GoogleCalendarSync"
    private const val PREFS_NAME = "fastable_timetable_prefs"
    private const val KEY_CALENDAR_SYNC_ENABLED = "is_calendar_sync_enabled"
    private const val FASTABLE_EVENT_TAG = "[FASTable Timetable Session]"

    fun isCalendarSyncEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_CALENDAR_SYNC_ENABLED, false)
    }

    fun setCalendarSyncEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_CALENDAR_SYNC_ENABLED, enabled).apply()
    }

    /**
     * Query available calendars on device, prioritizing Google account calendars.
     */
    fun getPrimaryGoogleCalendarId(context: Context): Long? {
        if (!PermissionManager.hasCalendarPermission(context)) {
            Log.w(TAG, "getPrimaryGoogleCalendarId: Calendar permission not granted")
            return null
        }

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )

        var fallbackCalendarId: Long? = null

        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE} = 1",
                null,
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                    val accountType = it.getString(it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)) ?: ""
                    val isPrimary = it.getInt(it.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY))

                    if (fallbackCalendarId == null) {
                        fallbackCalendarId = id
                    }

                    // Prioritize Google accounts
                    if (accountType.equals("com.google", ignoreCase = true)) {
                        if (isPrimary == 1) {
                            return id
                        }
                        fallbackCalendarId = id
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying calendars: ${e.message}", e)
        }

        return fallbackCalendarId
    }

    /**
     * Synchronize the current week's classes to the primary device Google Calendar.
     */
    fun syncCurrentWeek(context: Context, sessions: List<DashboardSession>): Result<Int> {
        if (!PermissionManager.hasCalendarPermission(context)) {
            Log.w(TAG, "syncCurrentWeek: Missing calendar permissions")
            return Result.failure(SecurityException("Calendar permissions not granted"))
        }

        val calendarId = getPrimaryGoogleCalendarId(context)
            ?: return Result.failure(IllegalStateException("No writable Google calendar found on this device"))

        return try {
            val (weekStartMillis, weekEndMillis) = getCurrentWeekWindowMillis()

            // Delete previous FASTable events in current week window
            deleteCurrentWeekFASTableEvents(context, calendarId, weekStartMillis, weekEndMillis)

            var insertedCount = 0
            val validSessions = sessions.filter {
                !it.courseName.contains("Cancelled", ignoreCase = true) &&
                !it.courseName.equals("Placeholder", ignoreCase = true) &&
                it.courseName.isNotBlank()
            }

            for (session in validSessions) {
                val timeRange = parseStartAndEndTime(session.day, session.timeSlot)
                if (timeRange != null) {
                    val (startMillis, endMillis) = timeRange

                    val values = ContentValues().apply {
                        put(CalendarContract.Events.CALENDAR_ID, calendarId)
                        put(CalendarContract.Events.TITLE, "${session.courseName} (${session.section})")
                        put(
                            CalendarContract.Events.DESCRIPTION,
                            "FAST NUCES Schedule\nRoom: ${session.room}\nSection: ${session.section}\nType: ${session.sessionType}\n$FASTABLE_EVENT_TAG"
                        )
                        put(CalendarContract.Events.EVENT_LOCATION, session.room)
                        put(CalendarContract.Events.DTSTART, startMillis)
                        put(CalendarContract.Events.DTEND, endMillis)
                        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                        put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                    }

                    val uri: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                    if (uri != null) {
                        insertedCount++
                    }
                }
            }

            Log.d(TAG, "syncCurrentWeek: Successfully synced $insertedCount sessions to calendar ID $calendarId")
            Result.success(insertedCount)
        } catch (e: Exception) {
            Log.e(TAG, "syncCurrentWeek failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun deleteCurrentWeekFASTableEvents(
        context: Context,
        calendarId: Long,
        startMillis: Long,
        endMillis: Long
    ) {
        try {
            val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND " +
                    "${CalendarContract.Events.DTSTART} >= ? AND " +
                    "${CalendarContract.Events.DTSTART} <= ? AND " +
                    "${CalendarContract.Events.DESCRIPTION} LIKE ?"
            val selectionArgs = arrayOf(
                calendarId.toString(),
                startMillis.toString(),
                endMillis.toString(),
                "%$FASTABLE_EVENT_TAG%"
            )
            val deleted = context.contentResolver.delete(
                CalendarContract.Events.CONTENT_URI,
                selection,
                selectionArgs
            )
            Log.d(TAG, "Deleted $deleted existing FASTable calendar events for current week")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning previous calendar events: ${e.message}", e)
        }
    }

    /**
     * Returns Pair(Monday 00:00:00, Saturday 23:59:59) for current week
     */
    private fun getCurrentWeekWindowMillis(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        // If Sunday, treat upcoming Monday as current week
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.DAY_OF_MONTH, 5) // Monday + 5 = Saturday
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    /**
     * Calculates millisecond timestamps for start and end time of a session in current week.
     */
    private fun parseStartAndEndTime(dayName: String, timeSlot: String): Pair<Long, Long>? {
        val targetDayOfWeek = when (dayName.lowercase(Locale.ROOT)) {
            "monday" -> Calendar.MONDAY
            "tuesday" -> Calendar.TUESDAY
            "wednesday" -> Calendar.WEDNESDAY
            "thursday" -> Calendar.THURSDAY
            "friday" -> Calendar.FRIDAY
            "saturday" -> Calendar.SATURDAY
            else -> return null
        }

        val cal = Calendar.getInstance()
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        cal.set(Calendar.DAY_OF_WEEK, targetDayOfWeek)

        // Parse time tokens (e.g. "08:30 - 10:00" or "08:30")
        val tokens = Regex("(\\d{1,2}:\\d{2})").findAll(timeSlot).map { it.value }.toList()
        if (tokens.isEmpty()) return null

        val startTime = parseHourMinute(tokens[0], isEndTime = false) ?: return null
        val endTime = if (tokens.size >= 2) {
            parseHourMinute(tokens[1], isEndTime = true) ?: Pair(startTime.first + 1, startTime.second + 20)
        } else {
            // Default 80 minutes lecture length
            var endH = startTime.first + 1
            var endM = startTime.second + 20
            if (endM >= 60) {
                endH += 1
                endM -= 60
            }
            Pair(endH, endM)
        }

        cal.set(Calendar.HOUR_OF_DAY, startTime.first)
        cal.set(Calendar.MINUTE, startTime.second)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMillis = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, endTime.first)
        cal.set(Calendar.MINUTE, endTime.second)
        val endMillis = cal.timeInMillis

        return Pair(startMillis, endMillis)
    }

    private fun parseHourMinute(token: String, isEndTime: Boolean): Pair<Int, Int>? {
        val parts = token.split(":")
        if (parts.size != 2) return null
        var hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null

        // Convert to 24 hour based on university hours (08:00 AM - 08:00 PM)
        if (hour in 1..7) {
            hour += 12 // PM
        } else if (hour == 8 && isEndTime) {
            hour += 12 // 8:05 PM evening class end
        }

        return Pair(hour, minute)
    }
}

