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
            if (timeSlot == "Unknown") return Long.MAX_VALUE

            val timeRegex = Regex("(\\d{1,2}:\\d{2})")
            val timeMatch = timeRegex.find(timeSlot) ?: return Long.MAX_VALUE

            val timeStr = timeMatch.value
            val ampmRegex = Regex("\\b(am|pm|AM|PM)\\b")
            val ampmMatch = ampmRegex.find(timeSlot)

            return try {
                val parts = timeStr.split(":")
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()

                val hour24 = if (ampmMatch != null) {
                    val ampm = ampmMatch.value.uppercase()
                    when {
                        ampm == "PM" && hour != 12 -> hour + 12
                        ampm == "AM" && hour == 12 -> 0
                        else -> hour
                    }
                } else {
                    hour
                }

                (hour24 * 60 + minute).toLong()
            } catch (e: Exception) {
                Long.MAX_VALUE
            }
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

