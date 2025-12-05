package com.example.fastable.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dashboard_courses")
data class DashboardCourse(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseName: String,
    val courseCode: String,
    val instructor: String,
    val room: String,
    val timeSlot: String,
    val day: String,
    val sessionType: String, // "Lecture" or "Lab"
    val sourceType: String, // "BATCH" or "CUSTOM"
    val batchName: String? = null, // Only for batch courses
    val section: String? = null // Only for batch courses
)

