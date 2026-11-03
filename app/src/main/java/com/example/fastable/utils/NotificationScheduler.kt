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
    private const val COMMON_NOTIFICATION_TAG = "fastable_notifications"

    // Canonical time bounds for class notifications (08:00 AM to 05:15 PM)
    const val MIN_CLASS_HOUR = 8
    const val MIN_CLASS_MINUTE = 0
    const val MAX_CUTOFF_HOUR = 17
    const val MAX_CUTOFF_MINUTE = 15

    const val MIN_CLASS_MINUTES_OF_DAY = 8 * 60 // 480 (08:00 AM)
    const val MAX_CUTOFF_MINUTES_OF_DAY = 17 * 60 + 15 // 1035 (05:15 PM)

    /**
     * Check if given calendar timestamp falls strictly within the allowed notification window:
     * - Monday through Saturday (Sunday is strictly blocked)
     * - Between 08:00 AM (480 mins) and 05:15 PM (1035 mins) inclusive
     */
    fun isWithinAllowedNotificationWindow(calendar: Calendar = Calendar.getInstance()): Boolean {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY) {
            return false
        }

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val currentMinutes = hour * 60 + minute

        return currentMinutes in MIN_CLASS_MINUTES_OF_DAY..MAX_CUTOFF_MINUTES_OF_DAY
    }

    /**
     * Schedule notifications for all dashboard sessions
     * Skips cancelled classes - no notifications for them
     */
    fun scheduleNotificationsForSessions(context: Context, sessions: List<DashboardSession>) {
        logDebug(TAG, "Scheduling notifications for ${sessions.size} sessions")

        // Cancel all existing notifications first
        cancelAllNotifications(context)

        // Schedule new notifications (skip cancelled classes)
        var scheduledCount = 0
        sessions.forEach { session ->
            if (!session.courseName.contains("Cancelled", ignoreCase = true)) {
                scheduleNotificationForSession(context, session)
                scheduledCount++
            }
        }

        logDebug(TAG, "Scheduled $scheduledCount notifications (${sessions.size - scheduledCount} cancelled classes skipped)")
    }

    /**
     * Schedule notifications for a single session (15 minutes before only)
     */
    fun scheduleNotificationForSession(context: Context, session: DashboardSession) {
        val sessionTime = getSessionTimeInMillis(session.day, session.timeSlot)

        if (sessionTime == null) {
            logWarn(TAG, "Could not parse or validate time for session: ${session.courseName}")
            return
        }

        val currentTime = System.currentTimeMillis()

        // Schedule 15-minute reminder only
        val fifteenMinBefore = sessionTime - (15 * 60 * 1000)

        // Strict boundary check: trigger time must fall inside allowed notification window (480..1035, Mon-Sat)
        val triggerCalendar = Calendar.getInstance().apply {
            timeInMillis = fifteenMinBefore
        }
        if (!isWithinAllowedNotificationWindow(triggerCalendar)) {
            logDebug(TAG, "Skipping notification for ${session.courseName} - trigger time is outside allowed window")
            return
        }

        if (fifteenMinBefore > currentTime) {
            scheduleNotification(
                context,
                session,
                fifteenMinBefore - currentTime,
                15,
                "${session.id}_15"
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
            .addTag(COMMON_NOTIFICATION_TAG) // Common tag for bulk cancellation
            .addTag("$WORK_TAG_PREFIX$uniqueId") // Unique tag for individual cancellation
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

        logDebug(TAG, "Scheduled notification for ${session.courseName} - $minutesBefore min before")
    }

    /**
     * Get session time in milliseconds from day and timeSlot.
     * Strictly rejects sessions on Sunday or outside allowed class hours (08:00 AM - 05:15 PM, 480..1035).
     */
    fun getSessionTimeInMillis(day: String, timeSlot: String): Long? {
        try {
            // Strictly reject Sunday
            if (day.equals("Sunday", ignoreCase = true)) {
                logDebug(TAG, "Skipping notification for $day - Sunday is not an allowed class day")
                return null
            }

            val totalMinutes = TimeParser.parseTimeSlot(timeSlot)
            if (totalMinutes == Long.MAX_VALUE) return null

            // Reject sessions starting before 08:00 AM (480) or after 05:15 PM (1035)
            if (totalMinutes < MIN_CLASS_MINUTES_OF_DAY || totalMinutes > MAX_CUTOFF_MINUTES_OF_DAY) {
                logDebug(TAG, "Skipping notification for $timeSlot ($totalMinutes mins) - outside allowed class hours (08:00 - 17:15)")
                return null
            }

            val hour = (totalMinutes / 60).toInt()
            val minute = (totalMinutes % 60).toInt()

            // Get current calendar
            val calendar = Calendar.getInstance()
            val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            // Convert day name to Calendar day constant
            val targetDayOfWeek = when (day.trim().lowercase().replaceFirstChar { it.uppercase() }) {
                "Monday" -> Calendar.MONDAY
                "Tuesday" -> Calendar.TUESDAY
                "Wednesday" -> Calendar.WEDNESDAY
                "Thursday" -> Calendar.THURSDAY
                "Friday" -> Calendar.FRIDAY
                "Saturday" -> Calendar.SATURDAY
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
            logError(TAG, "Error parsing time: ${e.message}")
            return null
        }
    }

    /**
     * Cancel all scheduled notifications
     * Uses a common tag for all notifications to enable bulk cancellation
     */
    fun cancelAllNotifications(context: Context) {
        // Cancel by the common tag that all notifications share
        WorkManager.getInstance(context).cancelAllWorkByTag(COMMON_NOTIFICATION_TAG)
        logDebug(TAG, "Cancelled all scheduled notifications")
    }

    /**
     * Cancel notification for a specific session
     */
    fun cancelNotificationForSession(context: Context, sessionId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("${WORK_TAG_PREFIX}${sessionId}_15")
        logDebug(TAG, "Cancelled notifications for session: $sessionId")
    }

    /**
     * Cancel all notifications for sessions and reschedule only for the provided list
     * This ensures that removed courses no longer receive notifications
     *
     * @param context The application context
     * @param currentSessions The current list of dashboard sessions that should receive notifications
     */
    fun syncNotificationsWithDashboard(context: Context, currentSessions: List<DashboardSession>) {
        logDebug(TAG, "Syncing notifications with dashboard - ${currentSessions.size} sessions")

        // Cancel ALL existing notifications
        cancelAllNotifications(context)

        // Schedule new notifications only for current sessions
        currentSessions.forEach { session ->
            // Skip cancelled classes
            if (!session.courseName.contains("Cancelled", ignoreCase = true)) {
                scheduleNotificationForSession(context, session)
            }
        }

        logDebug(TAG, "Notifications synced - scheduled for ${currentSessions.size} sessions")
    }

    /**
     * Reschedule all notifications (call this when dashboard sessions change)
     */
    fun rescheduleNotifications(context: Context, sessions: List<DashboardSession>) {
        logDebug(TAG, "Rescheduling notifications")
        scheduleNotificationsForSessions(context, sessions)
    }

    private fun logDebug(tag: String, msg: String) {
        try {
            Log.d(tag, msg)
        } catch (_: RuntimeException) {
            // Unmocked Log in JVM unit tests
        }
    }

    private fun logWarn(tag: String, msg: String) {
        try {
            Log.w(tag, msg)
        } catch (_: RuntimeException) {
            // Unmocked Log in JVM unit tests
        }
    }

    private fun logError(tag: String, msg: String, tr: Throwable? = null) {
        try {
            if (tr != null) Log.e(tag, msg, tr) else Log.e(tag, msg)
        } catch (_: RuntimeException) {
            // Unmocked Log in JVM unit tests
        }
    }
}

