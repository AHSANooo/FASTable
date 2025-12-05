package com.example.fastable.utils

import com.example.fastable.data.models.TimetableSession
import java.text.SimpleDateFormat
import java.util.*

object TimeParser {

    /**
     * Parse time slot and return milliseconds for sorting
     * Handles formats like "9:00 AM", "09:00-10:30", "9:00", etc.
     */
    fun parseTimeSlot(timeSlot: String): Long {
        if (timeSlot == "Unknown" || timeSlot.isEmpty()) return Long.MAX_VALUE

        // Extract first time token (HH:MM)
        val timeRegex = Regex("(\\d{1,2}:\\d{2})")
        val timeMatch = timeRegex.find(timeSlot) ?: return Long.MAX_VALUE

        val timeStr = timeMatch.value
        val parts = timeStr.split(":")
        if (parts.size != 2) return Long.MAX_VALUE

        val hour = parts[0].toIntOrNull() ?: return Long.MAX_VALUE
        val minute = parts[1].toIntOrNull() ?: return Long.MAX_VALUE

        // Check for AM/PM
        val ampmRegex = Regex("\\b(am|pm|AM|PM)\\b")
        val ampmMatch = ampmRegex.find(timeSlot)

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

        return (hour24 * 60 + minute).toLong()
    }

    /**
     * Parse embedded time from course entries like "Func Eng (SE) 09:00-10:45"
     * Returns Triple(cleanedName, timeSlot, hasEmbeddedTime)
     */
    fun parseEmbeddedTime(courseEntry: String): Triple<String, String, Boolean> {
        if (courseEntry.isEmpty()) return Triple(courseEntry, "Unknown", false)

        val timePattern = Regex("\\b(\\d{1,2}:\\d{2}(?:-\\d{1,2}:\\d{2})?)\\b")
        val timeMatch = timePattern.find(courseEntry)

        return if (timeMatch != null) {
            val timeSlot = timeMatch.value
            var cleanedEntry = courseEntry.replace(timePattern, "").trim()
            cleanedEntry = cleanedEntry.replace(Regex("\\s+"), " ").trim()
            if (cleanedEntry.endsWith("-")) {
                cleanedEntry = cleanedEntry.dropLast(1).trim()
            }
            Triple(cleanedEntry, timeSlot, true)
        } else {
            Triple(courseEntry, "Unknown", false)
        }
    }

    /**
     * Extract department from batch string (e.g., "BS-CS-1" -> "CS")
     */
    fun extractDepartmentFromBatch(batch: String): String {
        if (batch.isEmpty()) return ""

        // Handle dash-separated format
        if ("-" in batch) {
            val parts = batch.split("-")
            if (parts.size >= 2) return parts[1]
        }

        // Handle space-separated format like "BS CS (2023)"
        val tokens = Regex("\\b[A-Z]{2,4}\\b").findAll(batch).map { it.value }.toList()
        return tokens.firstOrNull { it != "BS" } ?: ""
    }

    /**
     * Extract year from batch string
     */
    fun extractYearFromBatch(batch: String): String {
        val yearRegex = Regex("(20\\d{2})")
        return yearRegex.find(batch)?.value ?: batch
    }

    /**
     * Clean room data
     */
    fun cleanRoomData(room: String): String {
        if (room.isEmpty()) return "Unknown"

        var cleaned = room.trim()

        // Remove common prefixes
        val prefixes = listOf("room", "room no", "room number", "location", "venue")
        for (prefix in prefixes) {
            if (cleaned.lowercase().startsWith(prefix)) {
                cleaned = cleaned.substring(prefix.length).trim()
            }
        }

        // Handle "No." prefix
        if (cleaned.lowercase().startsWith("no.")) {
            cleaned = cleaned.substring(3).trim()
        } else if (cleaned.lowercase().startsWith("no ")) {
            cleaned = cleaned.substring(3).trim()
        }

        // Remove extra punctuation
        cleaned = cleaned.trim('.', ',', ';', ':', ' ')

        return if (cleaned.isEmpty() || cleaned.isBlank()) "Unknown" else cleaned
    }

    /**
     * Normalize course name for comparison
     */
    fun normalizeCourse(name: String): String {
        if (name.isEmpty()) return ""

        var normalized = name.lowercase().trim()
        // Remove punctuation
        normalized = normalized.replace(Regex("[\\(\\)\\[\\]\\.,;:\\-]"), " ")
        // Remove lab/practical words
        normalized = normalized.replace(Regex("\\b(lab|lab session|practical|pract)\\b"), " ")
        // Collapse whitespace
        normalized = normalized.replace(Regex("\\s+"), " ").trim()

        return normalized
    }
}

