package com.example.fastable.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.fastable.Home
import com.example.fastable.R
import com.example.fastable.data.local.AppDatabase
import com.example.fastable.data.models.DashboardSession
import com.example.fastable.data.models.NotificationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service to detect and notify about schedule changes
 * Compares old dashboard sessions with new ones and sends notifications
 */
object ScheduleChangeDetector {

    private const val TAG = "ScheduleChangeDetector"
    private const val CHANNEL_ID = "schedule_changes"
    private const val CHANNEL_NAME = "Schedule Changes"

    /**
     * Compare old and new sessions and send notifications for changes
     */
    fun detectAndNotifyChanges(
        context: Context,
        oldSessions: List<DashboardSession>,
        newSessions: List<DashboardSession>
    ) {
        Log.d(TAG, "=== SCHEDULE CHANGE DETECTION STARTED ===")
        Log.d(TAG, "Old sessions count: ${oldSessions.size}")
        Log.d(TAG, "New sessions count: ${newSessions.size}")

        // Log old sessions
        Log.d(TAG, "--- OLD SESSIONS ---")
        oldSessions.forEachIndexed { index, session ->
            Log.d(TAG, "Old[$index]: ${session.courseName} | ${session.day} | ${session.timeSlot} | ${session.room}")
        }

        // Log new sessions
        Log.d(TAG, "--- NEW SESSIONS ---")
        newSessions.forEachIndexed { index, session ->
            Log.d(TAG, "New[$index]: ${session.courseName} | ${session.day} | ${session.timeSlot} | ${session.room}")
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val changes = detectChanges(oldSessions, newSessions)

                if (changes.isNotEmpty()) {
                    Log.d(TAG, "=== CHANGES DETECTED: ${changes.size} ===")
                    changes.forEachIndexed { index, change ->
                        Log.d(TAG, "Change[$index]: Type=${change.type}, Course=${change.courseName}")
                        Log.d(TAG, "  Message: ${change.message}")
                        sendNotification(context, change)
                        saveNotificationToDatabase(context, change)
                    }
                } else {
                    Log.d(TAG, "=== NO SCHEDULE CHANGES DETECTED ===")
                }
                Log.d(TAG, "=== SCHEDULE CHANGE DETECTION COMPLETED ===")
            } catch (e: Exception) {
                Log.e(TAG, "=== ERROR DETECTING CHANGES ===", e)
                Log.e(TAG, "Error message: ${e.message}")
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            }
        }
    }

    /**
     * Detect changes between old and new sessions
     */
    private fun detectChanges(
        oldSessions: List<DashboardSession>,
        newSessions: List<DashboardSession>
    ): List<ScheduleChange> {
        Log.d(TAG, "--- DETECTING CHANGES ---")
        val changes = mutableListOf<ScheduleChange>()

        // Check for cancelled classes
        Log.d(TAG, "Checking for CANCELLED classes...")
        var cancelledCount = 0
        oldSessions.forEach { oldSession ->
            Log.v(TAG, "Checking old session: ${oldSession.courseName} on ${oldSession.day}")

            // First check if there's a cancelled version in new sessions
            val cancelledSession = newSessions.find {
                val isSameDay = it.day.equals(oldSession.day, ignoreCase = true)
                val hasCancelledKeyword = it.courseName.contains("Cancelled", ignoreCase = true)

                // Clean both names for comparison
                val cleanNewName = it.courseName
                    .replace("Cancelled", "", ignoreCase = true)
                    .replace("-", "")
                    .replace("(", "")
                    .replace(")", "")
                    .trim()

                val cleanOldName = oldSession.courseName
                    .replace("-", "")
                    .replace("(", "")
                    .replace(")", "")
                    .trim()

                // Debug logging
                if (hasCancelledKeyword) {
                    Log.d(TAG, "    Found session with 'Cancelled': ${it.courseName}")
                    Log.d(TAG, "      Clean new name: '$cleanNewName'")
                    Log.d(TAG, "      Clean old name: '$cleanOldName'")
                    Log.d(TAG, "      Same day? $isSameDay (${it.day} vs ${oldSession.day})")
                    Log.d(TAG, "      Names match? ${cleanNewName.contains(cleanOldName, ignoreCase = true)}")
                }

                isSameDay && hasCancelledKeyword && cleanNewName.contains(cleanOldName, ignoreCase = true)
            }

            if (cancelledSession != null) {
                cancelledCount++
                Log.w(TAG, "  ⚠️ CANCELLED DETECTED: ${oldSession.courseName}")
                Log.w(TAG, "    Old: ${oldSession.courseName} | ${oldSession.day} | ${oldSession.timeSlot}")
                Log.w(TAG, "    New: ${cancelledSession.courseName}")

                changes.add(
                    ScheduleChange(
                        type = ChangeType.CANCELLED,
                        courseName = oldSession.courseName,
                        oldSession = oldSession,
                        newSession = cancelledSession,
                        message = "${oldSession.courseName} on ${oldSession.day} at ${oldSession.timeSlot} has been cancelled"
                    )
                )
            } else {
                // Only check for exact match if not cancelled
                val matchingNew = newSessions.find {
                    isSameCourse(it, oldSession)
                }

                if (matchingNew != null) {
                    Log.v(TAG, "  ✓ Match found: ${matchingNew.courseName}")
                } else {
                    Log.v(TAG, "  No match found, might be removed from schedule")
                }
            }
        }

        Log.d(TAG, "Cancelled classes found: $cancelledCount")

        // Check for rescheduled classes
        Log.d(TAG, "Checking for RESCHEDULED classes...")
        var rescheduledCount = 0
        newSessions.forEach { newSession ->
            if (newSession.courseName.contains("ReSch", ignoreCase = true) ||
                newSession.courseName.contains("Reschedule", ignoreCase = true)) {

                Log.d(TAG, "  Found ReSch marker in: ${newSession.courseName}")

                // Extract original course name by removing ReSch markers and extra spaces
                val originalCourseName = newSession.courseName
                    .replace("ReSch", "", ignoreCase = true)
                    .replace("Reschedule", "", ignoreCase = true)
                    .replace("-", "")
                    .replace("(", "")
                    .replace(")", "")
                    .trim()

                Log.d(TAG, "    Extracted original name: $originalCourseName")

                val oldSession = oldSessions.find {
                    val cleanOldName = it.courseName
                        .replace("-", "")
                        .replace("(", "")
                        .replace(")", "")
                        .trim()
                    cleanOldName.contains(originalCourseName, ignoreCase = true) ||
                    originalCourseName.contains(cleanOldName, ignoreCase = true)
                }

                if (oldSession != null) {
                    rescheduledCount++
                    Log.w(TAG, "  ⚠️ RESCHEDULE DETECTED: ${oldSession.courseName}")
                    Log.w(TAG, "    Old: ${oldSession.day} | ${oldSession.timeSlot} | ${oldSession.room}")
                    Log.w(TAG, "    New: ${newSession.day} | ${newSession.timeSlot} | ${newSession.room}")

                    val changeDetails = buildRescheduleMessage(oldSession, newSession)
                    Log.d(TAG, "    Change details: $changeDetails")

                    changes.add(
                        ScheduleChange(
                            type = ChangeType.RESCHEDULED,
                            courseName = oldSession.courseName,
                            oldSession = oldSession,
                            newSession = newSession,
                            message = changeDetails
                        )
                    )
                } else {
                    Log.w(TAG, "    ⚠️ No matching old session found for: $originalCourseName")
                }
            }
        }
        Log.d(TAG, "Rescheduled classes found: $rescheduledCount")

        // Check for time/room changes without ReSch marker
        Log.d(TAG, "Checking for MODIFIED classes (time/room/type changes)...")
        var modifiedCount = 0
        oldSessions.forEach { oldSession ->
            Log.v(TAG, "Comparing: ${oldSession.courseName} on ${oldSession.day}")

            val matchingNew = newSessions.find {
                isSameCourse(it, oldSession)
            }

            if (matchingNew != null && !matchingNew.courseName.contains("ReSch", ignoreCase = true)) {
                Log.v(TAG, "  Found matching session: ${matchingNew.courseName}")

                if (hasSignificantChange(oldSession, matchingNew)) {
                    modifiedCount++
                    Log.w(TAG, "  ⚠️ MODIFICATION DETECTED: ${oldSession.courseName}")
                    Log.w(TAG, "    Time changed: ${oldSession.timeSlot != matchingNew.timeSlot} (${oldSession.timeSlot} → ${matchingNew.timeSlot})")
                    Log.w(TAG, "    Room changed: ${oldSession.room != matchingNew.room} (${oldSession.room} → ${matchingNew.room})")
                    Log.w(TAG, "    Type changed: ${oldSession.sessionType != matchingNew.sessionType} (${oldSession.sessionType} → ${matchingNew.sessionType})")

                    val changeDetails = buildChangeMessage(oldSession, matchingNew)
                    Log.d(TAG, "    Change details: $changeDetails")

                    changes.add(
                        ScheduleChange(
                            type = ChangeType.MODIFIED,
                            courseName = oldSession.courseName,
                            oldSession = oldSession,
                            newSession = matchingNew,
                            message = changeDetails
                        )
                    )
                } else {
                    Log.v(TAG, "  ✓ No significant changes")
                }
            }
        }
        Log.d(TAG, "Modified classes found: $modifiedCount")
        Log.d(TAG, "--- DETECTION COMPLETE: Total changes = ${changes.size} ---")

        return changes
    }

    /**
     * Check if two sessions represent the same course
     */
    private fun isSameCourse(s1: DashboardSession, s2: DashboardSession): Boolean {
        return s1.courseName.equals(s2.courseName, ignoreCase = true) &&
               s1.day.equals(s2.day, ignoreCase = true)
    }

    /**
     * Check if there's a significant change between sessions
     */
    private fun hasSignificantChange(old: DashboardSession, new: DashboardSession): Boolean {
        return old.timeSlot != new.timeSlot ||
               old.room != new.room ||
               old.sessionType != new.sessionType
    }

    /**
     * Build reschedule message
     */
    private fun buildRescheduleMessage(old: DashboardSession, new: DashboardSession): String {
        val changes = mutableListOf<String>()

        if (old.day != new.day) {
            changes.add("day changed from ${old.day} to ${new.day}")
        }
        if (old.timeSlot != new.timeSlot) {
            changes.add("time changed from ${old.timeSlot} to ${new.timeSlot}")
        }
        if (old.room != new.room) {
            changes.add("room changed from ${old.room} to ${new.room}")
        }

        val courseName = old.courseName
        return if (changes.isNotEmpty()) {
            "$courseName has been rescheduled: ${changes.joinToString(", ")}"
        } else {
            "$courseName has been rescheduled"
        }
    }

    /**
     * Build change message for modified sessions
     */
    private fun buildChangeMessage(old: DashboardSession, new: DashboardSession): String {
        val changes = mutableListOf<String>()

        if (old.timeSlot != new.timeSlot) {
            changes.add("Time: ${old.timeSlot} → ${new.timeSlot}")
        }
        if (old.room != new.room) {
            changes.add("Room: ${old.room} → ${new.room}")
        }
        if (old.sessionType != new.sessionType) {
            changes.add("Type: ${old.sessionType} → ${new.sessionType}")
        }

        return "${old.courseName} on ${old.day}: ${changes.joinToString(", ")}"
    }

    /**
     * Send notification to user
     */
    private fun sendNotification(context: Context, change: ScheduleChange) {
        Log.d(TAG, "--- SENDING NOTIFICATION ---")
        Log.d(TAG, "Type: ${change.type}")
        Log.d(TAG, "Course: ${change.courseName}")
        Log.d(TAG, "Message: ${change.message}")

        createNotificationChannel(context)

        val intent = Intent(context, Home::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = when (change.type) {
            ChangeType.CANCELLED -> "Class Cancelled"
            ChangeType.RESCHEDULED -> "Class Rescheduled"
            ChangeType.MODIFIED -> "Schedule Updated"
        }

        Log.d(TAG, "Notification title: $title")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(change.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(change.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = change.courseName.hashCode()
        notificationManager.notify(notificationId, notification)

        Log.d(TAG, "✓ Notification sent with ID: $notificationId")
    }

    /**
     * Save notification to database
     */
    private suspend fun saveNotificationToDatabase(context: Context, change: ScheduleChange) {
        Log.d(TAG, "--- SAVING NOTIFICATION TO DATABASE ---")
        Log.d(TAG, "Course: ${change.courseName}")

        try {
            val notificationItem = NotificationItem(
                title = when (change.type) {
                    ChangeType.CANCELLED -> "Class Cancelled"
                    ChangeType.RESCHEDULED -> "Class Rescheduled"
                    ChangeType.MODIFIED -> "Schedule Updated"
                },
                message = change.message,
                courseName = change.courseName,
                timeSlot = change.oldSession?.timeSlot ?: "",
                room = change.oldSession?.room ?: "",
                timestamp = System.currentTimeMillis()
            )

            AppDatabase.getDatabase(context).notificationDao()
                .insertNotification(notificationItem)

            Log.d(TAG, "✓ Notification saved to database")

            // Clean up old notifications (keep only last 5 days)
            val fiveDaysAgo = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L)
            AppDatabase.getDatabase(context).notificationDao()
                .deleteOldNotifications(fiveDaysAgo)

            Log.d(TAG, "Cleaned up old notifications (5+ days old)")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error saving notification to database", e)
            Log.e(TAG, "Error message: ${e.message}")
        }
    }

    /**
     * Create notification channel
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for schedule changes and updates"
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Data classes
     */
    data class ScheduleChange(
        val type: ChangeType,
        val courseName: String,
        val oldSession: DashboardSession?,
        val newSession: DashboardSession?,
        val message: String
    )

    enum class ChangeType {
        CANCELLED,
        RESCHEDULED,
        MODIFIED
    }
}
