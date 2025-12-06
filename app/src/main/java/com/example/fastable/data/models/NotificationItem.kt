package com.example.fastable.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val courseName: String,
    val timeSlot: String,
    val room: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

