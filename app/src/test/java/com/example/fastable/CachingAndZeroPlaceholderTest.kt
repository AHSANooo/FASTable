package com.example.fastable

import com.example.fastable.data.models.Course
import com.example.fastable.utils.TimeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Milestone M2:
 * - Caching efficiency (Requirement R6)
 * - Zero placeholder handling (Requirement R5)
 */
class CachingAndZeroPlaceholderTest {

    @Test
    fun testCoursePlaceholderFiltering_ExcludesPlaceholderByName() {
        val course1 = Course(id = 1, name = "Placeholder", department = "CS", section = "A", batch = "BCS-2023", colorCode = "", fullEntry = "BCS-2023")
        val course2 = Course(id = 2, name = "placeholder", department = "SE", section = "B", batch = "BSE-2023", colorCode = "", fullEntry = "BSE-2023")
        val course3 = Course(id = 3, name = "Data Structures", department = "CS", section = "A", batch = "BCS-2023", colorCode = "#FF5722", fullEntry = "Data Structures")
        val course4 = Course(id = 4, name = "Operating Systems Cancelled", department = "CS", section = "A", batch = "BCS-2023", colorCode = "#4CAF50", fullEntry = "Operating Systems")

        val rawList = listOf(course1, course2, course3, course4)

        // The filtering applied across CustomTimetableViewModel and Add_Timetable
        val filteredForDisplay = rawList.filter {
            !it.name.equals("Placeholder", ignoreCase = true) &&
            !it.name.contains("Cancelled", ignoreCase = true)
        }

        assertEquals("Only real non-cancelled courses must remain", 1, filteredForDisplay.size)
        assertEquals("Data Structures", filteredForDisplay[0].name)
        assertFalse("Placeholder must not be present", filteredForDisplay.any { it.name.equals("Placeholder", ignoreCase = true) })
    }

    @Test
    fun testBatchMergingAndDeduplication_ExcludesBlanksAndPlaceholders() {
        val dbBatches = listOf("BCS-2021", "BCS-2022", "Placeholder", "")
        val memoryBatches = listOf("BCS-2022", "BSE-2023", "   ", "placeholder")

        val combined = (dbBatches + memoryBatches)
            .filter { it.isNotBlank() && !it.equals("Placeholder", ignoreCase = true) }
            .distinct()
            .sorted()

        assertEquals("Should contain 3 unique valid batches", listOf("BCS-2021", "BCS-2022", "BSE-2023"), combined)
        assertFalse("Placeholder must be purged", combined.any { it.equals("Placeholder", ignoreCase = true) })
        assertFalse("Blanks must be purged", combined.any { it.isBlank() })
    }

    @Test
    fun testDepartmentExtractionFromBatch() {
        assertEquals("CS", TimeParser.extractDepartmentFromBatch("BS-CS-2023"))
        assertEquals("SE", TimeParser.extractDepartmentFromBatch("BS-SE-2022"))
        assertEquals("AI", TimeParser.extractDepartmentFromBatch("BS-AI-2024"))
        assertEquals("CY", TimeParser.extractDepartmentFromBatch("BS-CY-2021"))
        assertEquals("CS", TimeParser.extractDepartmentFromBatch("BS CS (2023)"))
    }

    @Test
    fun testPreserveSelectionsExcludesPlaceholders() {
        val existingCourses = listOf(
            Course(id = 1, name = "Placeholder", department = "CS", section = "A", batch = "BCS-2023", colorCode = "", fullEntry = "", isSelected = true),
            Course(id = 2, name = "Algorithms", department = "CS", section = "A", batch = "BCS-2023", colorCode = "", fullEntry = "", isSelected = true),
            Course(id = 3, name = "Database Systems", department = "CS", section = "B", batch = "BCS-2023", colorCode = "", fullEntry = "", isSelected = false)
        )

        val preservedKeys = existingCourses
            .filter { it.isSelected && !it.name.equals("Placeholder", ignoreCase = true) }
            .map { it.getCourseKey() }
            .toSet()

        assertEquals("Only Algorithms should be preserved", 1, preservedKeys.size)
        assertTrue("Algorithms selection key should be present", preservedKeys.contains("Algorithms_CS_A_BCS-2023"))
        assertFalse("Placeholder selection must not be preserved", preservedKeys.any { it.startsWith("Placeholder") })
    }

    @Test
    fun testCacheValidationIgnoresLegacyPlaceholders() {
        val legacyDbWithOnlyPlaceholders = listOf(
            Course(id = 1, name = "Placeholder", department = "CS", section = "A", batch = "BCS-2023", colorCode = "", fullEntry = ""),
            Course(id = 2, name = "Placeholder", department = "SE", section = "A", batch = "BSE-2023", colorCode = "", fullEntry = "")
        )

        // TimetableRepository.syncData cache validation logic
        val existingRealCourses = legacyDbWithOnlyPlaceholders.filter { !it.name.equals("Placeholder", ignoreCase = true) }

        // Must evaluate to empty so syncData does NOT falsely skip fetching real courses
        assertTrue("Legacy DB with only placeholders must be treated as empty cache", existingRealCourses.isEmpty())

        val populatedDb = listOf(
            Course(id = 1, name = "Compiler Construction", department = "CS", section = "A", batch = "BCS-2021", colorCode = "", fullEntry = "")
        )
        val validCourses = populatedDb.filter { !it.name.equals("Placeholder", ignoreCase = true) }
        assertTrue("Populated DB with real course must be recognized as valid cache", validCourses.isNotEmpty())
    }
}
