package com.example.fastable.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.fastable.data.models.NotificationItem
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationItem>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentNotifications(limit: Int = 50): Flow<List<NotificationItem>>

    @Insert
    suspend fun insertNotification(notification: NotificationItem)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Long)

    @Query("DELETE FROM notifications WHERE timestamp < :cutoffTime")
    suspend fun deleteOldNotifications(cutoffTime: Long)

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>
}

