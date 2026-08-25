package com.example.fastable.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetable_sessions")
data class TimetableSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val day: String,
    val timeSlot: String,
    val room: String,
    val sessionType: String, // "Class" or "Lab"
    val courseName: String,
    val section: String,
    val batch: String,
    val department: String = "",
    val rank: Int = 0, // For sorting by time
    val colorCode: String = "",
    val isCustom: Boolean = false, // True if from custom timetable
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

    // Get display text for year (extracted from batch)
    fun getDisplayBatch(): String {
        val yearRegex = Regex("(20\\d{2})")
        val matchResult = yearRegex.find(batch)
        return matchResult?.value ?: batch
    }
}

