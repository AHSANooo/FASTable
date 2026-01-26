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
                    // University schedule: 8, 9, 10, 11 are AM; 12, 1, 2, 3, 4, 5, 6, 7 are PM
                    // Classes start at 8:30 AM and go until evening
                    when {
                        hour in 8..11 -> hour  // Morning classes (8:30, 9:00, 10:00, 11:00 are AM)
                        hour == 12 -> 12       // 12:00 is PM (noon)
                        hour in 1..7 -> hour + 12  // Afternoon classes (1:00, 2:00, 2:30 etc. are PM)
                        else -> hour
                    }
                }

                (hour24 * 60 + minute).toLong()
            } catch (e: Exception) {
                Long.MAX_VALUE
            }
        }
    }

    // Get display text for year (extracted from batch)
    fun getDisplayBatch(): String {
        val yearRegex = Regex("(20\\d{2})")
        val matchResult = yearRegex.find(batch)
        return matchResult?.value ?: batch
    }
}

