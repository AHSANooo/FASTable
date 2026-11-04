package com.example.fastable

import com.example.fastable.adapters.CourseGroup
import com.example.fastable.data.models.Course
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrototypeV109FeaturesTest {

    @Test
    fun testCourseGroupingByDepartmentAndName() {
        val rawCourses = listOf(
            Course(id = 1, name = "PDC", department = "CS", section = "A", batch = "2023", colorCode = "", fullEntry = ""),
            Course(id = 2, name = "PDC", department = "CS", section = "B", batch = "2023", colorCode = "", fullEntry = ""),
            Course(id = 3, name = "PDC", department = "AI", section = "A", batch = "2023", colorCode = "", fullEntry = ""),
            Course(id = 4, name = "Info Sec", department = "CS", section = "A", batch = "2023", colorCode = "", fullEntry = ""),
            Course(id = 5, name = "Info Sec", department = "SE", section = "A", batch = "2023", colorCode = "", fullEntry = "")
        )

        // Group by (name, department)
        val groupedMap = rawCourses.groupBy { Pair(it.name.trim(), it.department.trim().uppercase()) }
        val groups = groupedMap.map { (key, sections) ->
            CourseGroup(
                courseName = key.first,
                department = key.second,
                sections = sections.sortedBy { it.section }
            )
        }

        // Verify total groups
        assertEquals(4, groups.size)

        // Find PDC in CS
        val pdcCS = groups.firstOrNull { it.courseName == "PDC" && it.department == "CS" }
        assertNotNull(pdcCS)
        assertEquals(2, pdcCS!!.sections.size)
        assertEquals("A", pdcCS.sections[0].section)
        assertEquals("B", pdcCS.sections[1].section)

        // Find PDC in AI
        val pdcAI = groups.firstOrNull { it.courseName == "PDC" && it.department == "AI" }
        assertNotNull(pdcAI)
        assertEquals(1, pdcAI!!.sections.size)

        // Filter by CS only
        val csOnly = groups.filter { it.department.equals("CS", ignoreCase = true) }
        assertEquals(2, csOnly.size)
        assertTrue(csOnly.all { it.department == "CS" })

        // Filter by AI only
        val aiOnly = groups.filter { it.department.equals("AI", ignoreCase = true) }
        assertEquals(1, aiOnly.size)
        assertEquals("PDC", aiOnly[0].courseName)
    }

    @Test
    fun testRelativeSyncTimeFormatting() {
        fun formatSyncTime(diffSeconds: Long): String {
            val diffMin = diffSeconds / 60
            val diffHour = diffMin / 60
            val diffDay = diffHour / 24

            return when {
                diffMin < 1 -> "Last synced just now"
                diffMin < 60 -> "Last synced ${diffMin}m ago"
                diffHour < 24 -> "Last synced ${diffHour}h ago"
                diffDay == 1L -> "Last synced yesterday"
                else -> "Last synced ${diffDay}d ago"
            }
        }

        assertEquals("Last synced just now", formatSyncTime(15))
        assertEquals("Last synced just now", formatSyncTime(59))
        assertEquals("Last synced 5m ago", formatSyncTime(300))
        assertEquals("Last synced 45m ago", formatSyncTime(2700))
        assertEquals("Last synced 1h ago", formatSyncTime(3600))
        assertEquals("Last synced 3h ago", formatSyncTime(10800))
        assertEquals("Last synced yesterday", formatSyncTime(86400))
        assertEquals("Last synced 2d ago", formatSyncTime(172800))
    }
}

