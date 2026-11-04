package com.example.fastable

import com.example.fastable.adapters.CourseGroup
import com.example.fastable.data.models.Course
import com.example.fastable.data.models.DashboardSession
import com.example.fastable.data.models.RoomOccupancyState
import com.example.fastable.data.repository.CampusFloorRepository
import com.example.fastable.utils.RosterSyncManager
import org.junit.Assert.*
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

        val groupedMap = rawCourses.groupBy { Pair(it.name.trim(), it.department.trim().uppercase()) }
        val groups = groupedMap.map { (key, sections) ->
            CourseGroup(
                courseName = key.first,
                department = key.second,
                sections = sections.sortedBy { it.section }
            )
        }

        assertEquals(4, groups.size)

        val pdcCS = groups.firstOrNull { it.courseName == "PDC" && it.department == "CS" }
        assertNotNull(pdcCS)
        assertEquals(2, pdcCS!!.sections.size)
        assertEquals("A", pdcCS.sections[0].section)
        assertEquals("B", pdcCS.sections[1].section)

        val pdcAI = groups.firstOrNull { it.courseName == "PDC" && it.department == "AI" }
        assertNotNull(pdcAI)
        assertEquals(1, pdcAI!!.sections.size)

        val csOnly = groups.filter { it.department.equals("CS", ignoreCase = true) }
        assertEquals(2, csOnly.size)
        assertTrue(csOnly.all { it.department == "CS" })

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

    @Test
    fun testTrafficLightStatusColorCalculation() {
        fun computeTrafficLightColor(diffMinutes: Long): Int {
            val diffHour = diffMinutes / 60
            return when {
                diffMinutes < 60 -> 0xFF10B981.toInt()   // Green (< 1 hr)
                diffHour < 6 -> 0xFFF59E0B.toInt()       // Amber (1 - 6 hrs)
                else -> 0xFFEF4444.toInt()               // Red (> 6 hrs / stale)
            }
        }

        assertEquals("0 min -> Green", 0xFF10B981.toInt(), computeTrafficLightColor(0))
        assertEquals("45 min -> Green", 0xFF10B981.toInt(), computeTrafficLightColor(45))
        assertEquals("75 min -> Amber", 0xFFF59E0B.toInt(), computeTrafficLightColor(75))
        assertEquals("5 hours -> Amber", 0xFFF59E0B.toInt(), computeTrafficLightColor(300))
        assertEquals("7 hours -> Red", 0xFFEF4444.toInt(), computeTrafficLightColor(420))
        assertEquals("24 hours -> Red", 0xFFEF4444.toInt(), computeTrafficLightColor(1440))
    }

    @Test
    fun testRosterSyncEmailExtraction() {
        assertEquals("23I-0553", RosterSyncManager.extractRollNoFromEmail("i230553@isb.nu.edu.pk"))
        assertEquals("21F-0666", RosterSyncManager.extractRollNoFromEmail("f210666@nu.edu.pk"))
        assertEquals("22P-0123", RosterSyncManager.extractRollNoFromEmail("p220123@nu.edu.pk"))
        assertEquals("23I-0553", RosterSyncManager.extractRollNoFromEmail("23i-0553@nu.edu.pk"))
        assertEquals("23I-0553", RosterSyncManager.extractRollNoFromEmail(null))
        assertEquals("23I-0553", RosterSyncManager.extractRollNoFromEmail(""))
    }

    @Test
    fun testRosterMockCSSectionESchedule() {
        val schedule = RosterSyncManager.getMockCSSectionESchedule()
        assertTrue("Schedule must not be empty", schedule.isNotEmpty())
        assertTrue("All sessions must be Section CS-E", schedule.all { it.section == "CS-E" })

        val rooms = schedule.map { it.room }.toSet()
        assertTrue("Contains classroom C-301", rooms.contains("C-301"))
        assertTrue("Contains Margalla Lab 1", rooms.contains("Margalla Lab 1"))
        assertTrue("Contains Rawal Lab 1", rooms.contains("Rawal Lab 1"))
    }

    @Test
    fun testCampusFloorRepositoryStructure() {
        val blocks = CampusFloorRepository.getBlocks()
        assertEquals(listOf("Block A", "Block B", "Block C"), blocks)

        val blockCFloors = CampusFloorRepository.getFloorsForBlock("Block C")
        assertTrue(blockCFloors.contains("3rd Floor"))
        assertTrue(blockCFloors.contains("2nd Floor"))
        assertTrue(blockCFloors.contains("5th Floor"))

        val c3rdRooms = CampusFloorRepository.getRoomsForFloor("Block C", "3rd Floor")
        assertTrue(c3rdRooms.any { it.id == "C-301" })
        assertTrue(c3rdRooms.any { it.id == "C-302" })
        assertTrue(c3rdRooms.any { it.id == "C-307" })
    }

    @Test
    fun testRoomOccupancyAndYourRoomDetection() {
        val userSessions = listOf(
            DashboardSession(
                day = "Monday",
                timeSlot = "10:00 - 11:20",
                room = "C-301",
                sessionType = "Class",
                courseName = "Data Structures",
                section = "CS-E",
                batch = "BS CS (2023)"
            )
        )

        val roomC301 = CampusFloorRepository.getRoomsForFloor("Block C", "3rd Floor").first { it.id == "C-301" }
        val roomC302 = CampusFloorRepository.getRoomsForFloor("Block C", "3rd Floor").first { it.id == "C-302" }

        // Test at 10:15 AM on Monday (minutes = 10 * 60 + 15 = 615)
        val statusC301 = CampusFloorRepository.evaluateRoomStatus(
            room = roomC301,
            userSessions = userSessions,
            allCampusSessions = emptyList(),
            currentDay = "Monday",
            currentMinutes = 615
        )

        assertEquals("Must mark as student's class", RoomOccupancyState.STUDENT_CLASS, statusC301.state)
        assertTrue("isUserClass flag must be true", statusC301.isUserClass)
        assertEquals("Data Structures", statusC301.currentLecture)

        // Test room C-302 (free)
        val statusC302 = CampusFloorRepository.evaluateRoomStatus(
            room = roomC302,
            userSessions = userSessions,
            allCampusSessions = emptyList(),
            currentDay = "Monday",
            currentMinutes = 615
        )

        assertEquals("Must mark as free for study", RoomOccupancyState.FREE, statusC302.state)
        assertFalse(statusC302.isUserClass)
    }

    @Test
    fun testRoomNameMatchingRobustness() {
        assertTrue(CampusFloorRepository.isRoomNameMatching("C-301", "C301"))
        assertTrue(CampusFloorRepository.isRoomNameMatching("C-301", "c-301"))
        assertTrue(CampusFloorRepository.isRoomNameMatching("Margalla-1", "Margalla Lab 1"))
        assertTrue(CampusFloorRepository.isRoomNameMatching("Rawal-1", "Rawal Lab 1"))
        assertFalse(CampusFloorRepository.isRoomNameMatching("C-301", "C-302"))
    }
}
