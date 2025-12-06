package com.example.fastable.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fastable.Home
import com.example.fastable.R
import com.example.fastable.data.local.AppDatabase
import com.example.fastable.data.models.NotificationItem

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "NotificationWorker"
        const val KEY_COURSE_NAME = "courseName"
        const val KEY_TIME_SLOT = "timeSlot"
        const val KEY_ROOM = "room"
        const val KEY_MINUTES_BEFORE = "minutesBefore"
    }

    override suspend fun doWork(): Result {
        val courseName = inputData.getString(KEY_COURSE_NAME) ?: return Result.failure()
        val timeSlot = inputData.getString(KEY_TIME_SLOT) ?: return Result.failure()
        val room = inputData.getString(KEY_ROOM) ?: return Result.failure()
        val minutesBefore = inputData.getInt(KEY_MINUTES_BEFORE, 10)

        val title = "Course Reminder"
        val message = "$courseName starts in $minutesBefore minutes at $room"

        // Save to database
        saveNotificationToDatabase(title, message, courseName, timeSlot, room)

        // Show notification
        showNotification(title, message, courseName)

        return Result.success()
    }

    private suspend fun saveNotificationToDatabase(
        title: String,
        message: String,
        courseName: String,
        timeSlot: String,
        room: String
    ) {
        try {
            val notificationItem = NotificationItem(
                title = title,
                message = message,
                courseName = courseName,
                timeSlot = timeSlot,
                room = room,
                timestamp = System.currentTimeMillis()
            )
            AppDatabase.getDatabase(applicationContext).notificationDao()
                .insertNotification(notificationItem)

            // Delete notifications older than 5 days
            val fiveDaysAgo = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L)
            AppDatabase.getDatabase(applicationContext).notificationDao()
                .deleteOldNotifications(fiveDaysAgo)

            Log.d(TAG, "Notification saved to database")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notification: ${e.message}")
        }
    }

    private fun showNotification(title: String, message: String, courseName: String) {
        createNotificationChannel()

        val intent = Intent(applicationContext, Home::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, FCMService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(courseName.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FCMService.CHANNEL_ID,
                FCMService.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming courses"
                enableVibration(true)
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

