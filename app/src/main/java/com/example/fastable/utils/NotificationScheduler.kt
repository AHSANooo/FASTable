package com.example.fastable.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.fastable.data.models.DashboardSession
import com.example.fastable.services.NotificationWorker
import java.util.*
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"
    private const val WORK_TAG_PREFIX = "course_notification_"

    /**
     * Schedule notifications for all dashboard sessions
     */
    fun scheduleNotificationsForSessions(context: Context, sessions: List<DashboardSession>) {
        Log.d(TAG, "Scheduling notifications for ${sessions.size} sessions")

        // Cancel all existing notifications first
        cancelAllNotifications(context)

        // Schedule new notifications
        sessions.forEach { session ->
            scheduleNotificationForSession(context, session)
        }
    }

    /**
     * Schedule notifications for a single session (10 and 20 minutes before)
     */
    fun scheduleNotificationForSession(context: Context, session: DashboardSession) {
        val sessionTime = getSessionTimeInMillis(session.day, session.timeSlot)

        if (sessionTime == null) {
            Log.w(TAG, "Could not parse time for session: ${session.courseName}")
            return
        }

        val currentTime = System.currentTimeMillis()

        // Schedule 20-minute reminder
        val twentyMinBefore = sessionTime - (20 * 60 * 1000)
        if (twentyMinBefore > currentTime) {
            scheduleNotification(
                context,
                session,
                twentyMinBefore - currentTime,
                20,
                "${session.id}_20"
            )
        }

        // Schedule 10-minute reminder
        val tenMinBefore = sessionTime - (10 * 60 * 1000)
        if (tenMinBefore > currentTime) {
            scheduleNotification(
                context,
                session,
                tenMinBefore - currentTime,
                10,
                "${session.id}_10"
            )
        }
    }

    private fun scheduleNotification(
        context: Context,
        session: DashboardSession,
        delayMillis: Long,
        minutesBefore: Int,
        uniqueId: String
    ) {
        val data = Data.Builder()
            .putString(NotificationWorker.KEY_COURSE_NAME, session.courseName)
            .putString(NotificationWorker.KEY_TIME_SLOT, session.timeSlot)
            .putString(NotificationWorker.KEY_ROOM, session.room)
            .putInt(NotificationWorker.KEY_MINUTES_BEFORE, minutesBefore)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("$WORK_TAG_PREFIX$uniqueId")
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "$WORK_TAG_PREFIX$uniqueId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

        Log.d(TAG, "Scheduled notification for ${session.courseName} - $minutesBefore min before")
    }

    // Valid notification hours (7:00 AM to 5:15 PM in 24-hour format)
    private const val MIN_NOTIFICATION_HOUR = 7
    private const val MAX_NOTIFICATION_HOUR = 17
    private const val MAX_NOTIFICATION_MINUTE = 15

    /**
     * Get session time in milliseconds from day and timeSlot
     */
    private fun getSessionTimeInMillis(day: String, timeSlot: String): Long? {
        try {
            // Parse timeSlot like "8:30-9:50" or "8:30am-9:50am" or "01:00-02:20"
            val timeRegex = Regex("(\\d{1,2}):(\\d{2})")
            val timeMatch = timeRegex.find(timeSlot) ?: return null

            var hour = timeMatch.groupValues[1].toInt()
            val minute = timeMatch.groupValues[2].toInt()

            // Check for explicit AM/PM markers
            val hasAmPm = timeSlot.lowercase().contains("am") || timeSlot.lowercase().contains("pm")
            val isPm = timeSlot.lowercase().contains("pm")
            val isAm = timeSlot.lowercase().contains("am")

            // Convert to 24-hour format
            if (hasAmPm) {
                // Explicit AM/PM marker
                if (isPm && hour != 12) {
                    hour += 12
                } else if (isAm && hour == 12) {
                    hour = 0
                }
            } else {
                // University schedule convention (no AM/PM marker):
                // 8, 9, 10, 11 are AM (morning classes start at 8:30)
                // 12 is noon (PM)
                // 1, 2, 3, 4, 5, 6, 7 are PM (afternoon classes)
                if (hour in 1..7) {
                    hour += 12  // 1:00 -> 13:00, 2:00 -> 14:00, etc.
                }
                // 8-11 remain as is (AM)
                // 12 remains as is (noon/PM)
            }

            // Final validation: skip notifications for times outside university hours (7:00 AM - 5:15 PM)
            // After conversion, valid hours are 7-17
            if (hour < MIN_NOTIFICATION_HOUR || hour > MAX_NOTIFICATION_HOUR) {
                Log.d(TAG, "Skipping notification for $timeSlot (hour=$hour) - outside valid hours")
                return null
            }
            if (hour == MAX_NOTIFICATION_HOUR && minute > MAX_NOTIFICATION_MINUTE) {
                Log.d(TAG, "Skipping notification for $timeSlot - after 5:15 PM")
                return null
            }

            // Get current calendar
            val calendar = Calendar.getInstance()
            val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            // Convert day name to Calendar day constant
            val targetDayOfWeek = when (day) {
                "Monday" -> Calendar.MONDAY
                "Tuesday" -> Calendar.TUESDAY
                "Wednesday" -> Calendar.WEDNESDAY
                "Thursday" -> Calendar.THURSDAY
                "Friday" -> Calendar.FRIDAY
                "Saturday" -> Calendar.SATURDAY
                "Sunday" -> Calendar.SUNDAY
                else -> return null
            }

            // Set to target day
            val daysToAdd = (targetDayOfWeek - currentDayOfWeek + 7) % 7
            calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)

            // Set time
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            // If the time has already passed today and it's the same day, schedule for next week
            if (daysToAdd == 0 && calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }

            return calendar.timeInMillis
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time: ${e.message}")
            return null
        }
    }

    /**
     * Cancel all scheduled notifications
     */
    fun cancelAllNotifications(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG_PREFIX)
        Log.d(TAG, "Cancelled all scheduled notifications")
    }

    /**
     * Cancel notification for a specific session
     */
    fun cancelNotificationForSession(context: Context, sessionId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag("${WORK_TAG_PREFIX}${sessionId}_10")
        WorkManager.getInstance(context).cancelAllWorkByTag("${WORK_TAG_PREFIX}${sessionId}_20")
        Log.d(TAG, "Cancelled notifications for session: $sessionId")
    }

    /**
     * Reschedule all notifications (call this when dashboard sessions change)
     */
    fun rescheduleNotifications(context: Context, sessions: List<DashboardSession>) {
        Log.d(TAG, "Rescheduling notifications")
        scheduleNotificationsForSessions(context, sessions)
    }
}

