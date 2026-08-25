package com.example.fastable.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for dashboard/home screen timetable sessions
 * Represents courses shown on the main dashboard
 */
@Entity(tableName = "dashboard_sessions")
data class DashboardSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val day: String,
    val timeSlot: String,
    val room: String,
    val sessionType: String, // "Class" or "Lab"
    val courseName: String,
    val section: String = "",
    val batch: String = "",
    val department: String = "",
    val rank: Int = 0,
    val colorCode: String = "",
    val isCustom: Boolean = false, // True if from custom timetable, false if from batch
    val lastUpdated: Long = System.currentTimeMillis()
) {
    // Parse and extract start time for sorting
    fun getStartTimeMillis(): Long {
        return parseTimeSlot(timeSlot)
    }

    companion object {
        private fun parseTimeSlot(timeSlot: String): Long {
            return com.example.fastable.utils.TimeParser.parseTimeSlot(timeSlot)
        }
    }

    // Convert to TimetableSession for display
    fun toTimetableSession(): TimetableSession {
        return TimetableSession(
            id = id,
            day = day,
            timeSlot = timeSlot,
            room = room,
            sessionType = sessionType,
            courseName = courseName,
            section = section,
            batch = batch,
            department = department,
            rank = rank,
            colorCode = colorCode,
            isCustom = isCustom,
            lastUpdated = lastUpdated
        )
    }
}

