package com.example.fastable.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.fastable.data.models.Course
import com.example.fastable.data.models.TimetableSession
import com.example.fastable.data.models.DashboardSession
import com.example.fastable.data.models.DefaultBatch
import com.example.fastable.data.models.UserProfile
import com.example.fastable.data.models.NotificationItem

@Database(
    entities = [Course::class, TimetableSession::class, DashboardSession::class, DefaultBatch::class, UserProfile::class, NotificationItem::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun timetableDao(): TimetableDao
    abstract fun dashboardDao(): DashboardDao
    abstract fun defaultBatchDao(): DefaultBatchDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fastable_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

