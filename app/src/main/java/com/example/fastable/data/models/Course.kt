package com.example.fastable.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val department: String,
    val section: String,
    val batch: String,
    val colorCode: String,
    val fullEntry: String,
    val day: String = "",
    val isSelected: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    // Generate unique identifier for course matching
    fun getCourseKey(): String {
        return "${name}_${department}_${section}_${batch}"
    }

    // Extract year from batch
    fun getYear(): String {
        val yearRegex = Regex("(20\\d{2})")
        val matchResult = yearRegex.find(batch)
        return matchResult?.value ?: batch
    }

    // Format for display
    fun getDisplayText(): String {
        val year = getYear()
        val parts = listOf(name, department, section, year).filter { it.isNotEmpty() }
        return parts.joinToString(" ")
    }
}

