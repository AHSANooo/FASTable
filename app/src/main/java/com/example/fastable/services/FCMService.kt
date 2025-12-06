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
import com.example.fastable.data.models.NotificationItem
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FCMService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID = "course_reminders"
        const val CHANNEL_NAME = "Course Reminders"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token: $token")
        // Store token in SharedPreferences for future use
        getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        message.data.isNotEmpty().let {
            val title = message.data["title"] ?: "Course Reminder"
            val body = message.data["body"] ?: ""
            val courseName = message.data["courseName"] ?: ""
            val timeSlot = message.data["timeSlot"] ?: ""
            val room = message.data["room"] ?: ""

            // Save to database
            saveNotificationToDatabase(title, body, courseName, timeSlot, room)

            // Show notification
            showNotification(title, body)
        }

        message.notification?.let {
            showNotification(it.title ?: "Course Reminder", it.body ?: "")
        }
    }

    private fun saveNotificationToDatabase(
        title: String,
        message: String,
        courseName: String,
        timeSlot: String,
        room: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
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
                Log.d(TAG, "Notification saved to database")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification: ${e.message}")
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        createNotificationChannel()

        val intent = Intent(this, Home::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming courses"
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

