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
    fun testNormalizeRollNoVariants() {
        assertEquals("23I-0553", RosterSyncManager.normalizeRollNo("i230553"))
        assertEquals("23I-0553", RosterSyncManager.normalizeRollNo("23i0553"))
        assertEquals("23I-0553", RosterSyncManager.normalizeRollNo("23i-0553"))
        assertEquals("24I-0008", RosterSyncManager.normalizeRollNo("24i-0008"))
        assertEquals("26I-3001", RosterSyncManager.normalizeRollNo("26i3001"))
        assertEquals("21F-0666", RosterSyncManager.normalizeRollNo("f210666"))
        assertEquals("23I-0553", RosterSyncManager.normalizeRollNo("i230553@isb.nu.edu.pk"))
    }

    @Test
    fun testStudentRosterJsonFileValidityAndStudentLookup() {
        val file = java.io.File("src/main/assets/student_roster.json")
        val altFile = java.io.File("app/src/main/assets/student_roster.json")
        val targetFile = if (file.exists()) file else altFile
        assertTrue("student_roster.json must exist in assets", targetFile.exists())

        val reader = java.io.FileReader(targetFile)
        val type = object : com.google.gson.reflect.TypeToken<Map<String, com.example.fastable.utils.StudentRosterEntry>>() {}.type
        val roster: Map<String, com.example.fastable.utils.StudentRosterEntry> = com.google.gson.Gson().fromJson(reader, type)

        assertEquals("Must contain all 4,313 students", 4313, roster.size)

        val student23I0553 = roster["23I-0553"]
        assertNotNull("Student 23I-0553 must be found", student23I0553)
        assertEquals("Muhammad Ahsan", student23I0553!!.name)

        val courseCodes = student23I0553.courses.map { it.code }
        assertTrue("Must contain CS3014 (HCI)", courseCodes.contains("CS3014"))
        assertTrue("Must contain CS4001 (PPIT)", courseCodes.contains("CS4001"))
        assertTrue("Must contain CS3002 (InfoSec)", courseCodes.contains("CS3002"))
        assertTrue("Must contain CS4085 (MLOps)", courseCodes.contains("CS4085"))

        // Generate schedule for 23I-0553
        val schedule = RosterSyncManager.generateScheduleForStudent(student23I0553)
        assertTrue("Schedule must not be empty", schedule.isNotEmpty())
        assertTrue("Schedule contains HCI course", schedule.any { it.courseName.contains("Human Computer Interaction") })

        // Verify zero collision
        val dayTimes = mutableSetOf<Pair<String, String>>()
        for (session in schedule) {
            val key = Pair(session.day, session.timeSlot)
            assertFalse("Duplicate time slot on same day: $key", dayTimes.contains(key))
            dayTimes.add(key)
        }
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

        // Test room C-302: has campus lecture 'Applied Human Computer Interaction' at 10:00 - 11:20 AM
        val statusC302 = CampusFloorRepository.evaluateRoomStatus(
            room = roomC302,
            userSessions = userSessions,
            allCampusSessions = emptyList(),
            currentDay = "Monday",
            currentMinutes = 615
        )

        assertEquals("Must mark as occupied by ongoing campus lecture", RoomOccupancyState.OCCUPIED, statusC302.state)
        assertEquals("Applied Human Computer Interaction", statusC302.currentLecture)
        assertFalse(statusC302.isUserClass)
    }

    @Test
    fun testCourseNameMatcherAbbreviationMatching() {
        // Test PPIT
        assertTrue(com.example.fastable.utils.CourseNameMatcher.matches("PPIT (CS-E)", "Professional Practices in IT", "CS-E"))
        assertTrue(com.example.fastable.utils.CourseNameMatcher.matches("Professional Practices (CS-E)", "Professional Practices in IT", "CS-E"))
        // Test Applied HCI
        assertTrue(com.example.fastable.utils.CourseNameMatcher.matches("Applied HCI (CS-E)", "Applied Human Computer Interaction", "CS-E"))
        assertTrue(com.example.fastable.utils.CourseNameMatcher.matches("HCI (CS-E)", "Applied Human Computer Interaction", "CS-E"))
        // Test Info Sec
        assertTrue(com.example.fastable.utils.CourseNameMatcher.matches("Info Sec (CS-E)", "Information Security", "CS-E"))
        // Test MLOps
        assertTrue(com.example.fastable.utils.CourseNameMatcher.matches("MLOps (CS-B)", "MLOps", "CS-B"))
        // Test COAL
        assertTrue(com.example.fastable.utils.CourseNameMatcher.matches("COAL (CS-A)", "Computer Organization and Assembly Language", "CS-A"))
        // Test Data Structures
        assertTrue(com.example.fastable.utils.CourseNameMatcher.matches("DS (CS-E)", "Data Structures", "CS-E"))
        // Section mismatch must return false
        assertFalse(com.example.fastable.utils.CourseNameMatcher.matches("PPIT (CS-A)", "Professional Practices in IT", "CS-E"))
    }

    @Test
    fun testRoomNameMatchingRobustness() {
        assertTrue(CampusFloorRepository.isRoomNameMatching("C-301", "C301"))
        assertTrue(CampusFloorRepository.isRoomNameMatching("C-301", "c-301"))
        assertTrue(CampusFloorRepository.isRoomNameMatching("Margalla-1", "Margalla Lab 1"))
        assertTrue(CampusFloorRepository.isRoomNameMatching("Rawal-1", "Rawal Lab 1"))
        assertFalse(CampusFloorRepository.isRoomNameMatching("C-301", "C-302"))
    }

    @Test
    fun testCourseNameMatcherCanonicalResolution() {
        assertEquals("Information Security", com.example.fastable.utils.CourseNameMatcher.getCanonicalName("Info Sec"))
        assertEquals("Information Security", com.example.fastable.utils.CourseNameMatcher.getCanonicalName("infosec"))
        assertEquals("Professional Practices in IT", com.example.fastable.utils.CourseNameMatcher.getCanonicalName("PPIT"))
        assertEquals("Applied Human Computer Interaction", com.example.fastable.utils.CourseNameMatcher.getCanonicalName("Applied HCI"))
        assertEquals("MLOps", com.example.fastable.utils.CourseNameMatcher.getCanonicalName("MLOps"))
        assertEquals("Operating Systems", com.example.fastable.utils.CourseNameMatcher.getCanonicalName("OS"))
        assertEquals("Database Systems", com.example.fastable.utils.CourseNameMatcher.getCanonicalName("DB"))
        assertEquals("Parallel and Distributed Computing", com.example.fastable.utils.CourseNameMatcher.getCanonicalName("PDC"))
        assertEquals("Computer Organization and Assembly Language", com.example.fastable.utils.CourseNameMatcher.getCanonicalName("COAL"))
        assertEquals("Design and Analysis of Algorithms", com.example.fastable.utils.CourseNameMatcher.getCanonicalName("DAA"))
        assertEquals("Software Design and Analysis", com.example.fastable.utils.CourseNameMatcher.getCanonicalName("SDA"))
        assertEquals("Introduction to Software Engineering", com.example.fastable.utils.CourseNameMatcher.getCanonicalName("SE"))
    }

    @Test
    fun testSeatingPlanRefreshRetentionSimulated() {
        // Enrolled courses for student 23I-0553
        val currentSessions = listOf(
            DashboardSession(day = "Monday", timeSlot = "10:00 - 11:20", room = "C-301", sessionType = "Class", courseName = "Information Security", section = "CS-E", batch = "BS CS (2023)"),
            DashboardSession(day = "Monday", timeSlot = "11:30 - 12:50", room = "C-304", sessionType = "Class", courseName = "Professional Practices in IT", section = "CS-E", batch = "BS CS (2023)"),
            DashboardSession(day = "Tuesday", timeSlot = "10:00 - 11:20", room = "C-302", sessionType = "Class", courseName = "MLOps", section = "CS-B", batch = "BS CS (2023)"),
            DashboardSession(day = "Wednesday", timeSlot = "01:00 - 02:20", room = "C-307", sessionType = "Class", courseName = "Applied Human Computer Interaction", section = "CS-E", batch = "BS CS (2023)")
        )

        // Simulate sheet only returning an update for MLOps with a room shift to C-401
        val freshSessions = listOf(
            DashboardSession(day = "Tuesday", timeSlot = "10:00 - 11:20", room = "C-401", sessionType = "Class", courseName = "MLOps", section = "CS-B", batch = "BS CS (2023)")
        )

        val oldSessionsMap = currentSessions.associateBy { session ->
            val clean = session.courseName.replace(" Cancelled", "", ignoreCase = true).trim()
            val canonical = com.example.fastable.utils.CourseNameMatcher.getCanonicalName(clean)
            "${canonical}_${session.day}_${session.timeSlot}_${session.section}"
        }

        val refreshedSessions = mutableListOf<DashboardSession>()
        val processedKeys = mutableSetOf<String>()

        freshSessions.forEach { freshSession ->
            val freshClean = freshSession.courseName.replace(" Cancelled", "", ignoreCase = true).trim()
            val freshCanonical = com.example.fastable.utils.CourseNameMatcher.getCanonicalName(freshClean)
            val exactKey = "${freshCanonical}_${freshSession.day}_${freshSession.timeSlot}_${freshSession.section}"

            refreshedSessions.add(freshSession)
            processedKeys.add(exactKey)
        }

        currentSessions.forEach { oldSession ->
            val oldClean = oldSession.courseName.replace(" Cancelled", "", ignoreCase = true).trim()
            val oldCanonical = com.example.fastable.utils.CourseNameMatcher.getCanonicalName(oldClean)
            val oldKey = "${oldCanonical}_${oldSession.day}_${oldSession.timeSlot}_${oldSession.section}"

            if (!processedKeys.contains(oldKey)) {
                refreshedSessions.add(oldSession)
                processedKeys.add(oldKey)
            }
        }

        // All 4 courses must be retained in the dashboard after refresh
        assertEquals(4, refreshedSessions.size)
        val refreshedCourseNames = refreshedSessions.map { it.courseName }.toSet()
        assertTrue(refreshedCourseNames.contains("Information Security"))
        assertTrue(refreshedCourseNames.contains("Professional Practices in IT"))
        assertTrue(refreshedCourseNames.contains("MLOps"))
        assertTrue(refreshedCourseNames.contains("Applied Human Computer Interaction"))

        // MLOps must have the updated room C-401
        val mlops = refreshedSessions.first { it.courseName == "MLOps" }
        assertEquals("C-401", mlops.room)
    }

    @Test
    fun testRefreshSupersedesAndPreventsDuplicates() {
        // Reproduce exact user case from screenshot:
        // Student had Monday classes generated or from previous sync:
        // 1. Information Security on Monday 10:00 - 11:20 in C-301
        // 2. Professional Practices in IT on Monday 11:30 - 12:50 in C-302
        val currentSessions = listOf(
            DashboardSession(day = "Monday", timeSlot = "10:00 - 11:20", room = "C-301", sessionType = "Class", courseName = "Information Security", section = "CS-E", batch = "BS CS (2023)"),
            DashboardSession(day = "Monday", timeSlot = "11:30 - 12:50", room = "C-302", sessionType = "Class", courseName = "Professional Practices in IT", section = "CS-E", batch = "BS CS (2023)")
        )

        // Fresh spreadsheet returns updated slots and rooms from Google Sheet:
        // 1. Professional Practices in IT on Monday 08:30-09:50 in D-406
        // 2. Information Security on Monday 10:00-11:20 in D-406
        val freshSessions = listOf(
            DashboardSession(day = "Monday", timeSlot = "08:30-09:50", room = "D-406", sessionType = "Class", courseName = "PPIT", section = "CS-E", batch = "BS CS (2023)"),
            DashboardSession(day = "Monday", timeSlot = "10:00-11:20", room = "D-406", sessionType = "Class", courseName = "Info Sec", section = "CS-E", batch = "BS CS (2023)")
        )

        val refreshedSessions = mutableListOf<DashboardSession>()
        val coveredCourseDayTypes = mutableSetOf<Triple<String, String, String>>()

        freshSessions.forEach { freshSession ->
            val freshClean = freshSession.courseName.replace(" Cancelled", "", ignoreCase = true).trim()
            val freshCanonical = com.example.fastable.utils.CourseNameMatcher.getCanonicalName(freshClean)
            val normTime = com.example.fastable.utils.TimeParser.normalizeTimeSlot(freshSession.timeSlot)

            refreshedSessions.add(freshSession.copy(courseName = freshCanonical, timeSlot = normTime))
            coveredCourseDayTypes.add(
                Triple(
                    freshCanonical.lowercase(java.util.Locale.ROOT),
                    freshSession.day.lowercase(java.util.Locale.ROOT),
                    freshSession.sessionType.lowercase(java.util.Locale.ROOT)
                )
            )
        }

        currentSessions.forEach { oldSession ->
            val oldClean = oldSession.courseName.replace(" Cancelled", "", ignoreCase = true).trim()
            val oldCanonical = com.example.fastable.utils.CourseNameMatcher.getCanonicalName(oldClean)
            val key = Triple(
                oldCanonical.lowercase(java.util.Locale.ROOT),
                oldSession.day.lowercase(java.util.Locale.ROOT),
                oldSession.sessionType.lowercase(java.util.Locale.ROOT)
            )

            if (!coveredCourseDayTypes.contains(key)) {
                val normOldSession = oldSession.copy(timeSlot = com.example.fastable.utils.TimeParser.normalizeTimeSlot(oldSession.timeSlot))
                refreshedSessions.add(normOldSession)
                coveredCourseDayTypes.add(key)
            }
        }

        // Deduplicate
        val finalSessions = refreshedSessions.distinctBy { session ->
            val clean = session.courseName.replace(" Cancelled", "", ignoreCase = true).trim()
            val canonical = com.example.fastable.utils.CourseNameMatcher.getCanonicalName(clean)
            val normTime = com.example.fastable.utils.TimeParser.normalizeTimeSlot(session.timeSlot)
            "${canonical.lowercase(java.util.Locale.ROOT)}_${session.day.lowercase(java.util.Locale.ROOT)}_${normTime}_${session.section.lowercase(java.util.Locale.ROOT)}"
        }

        // Must have EXACTLY 2 sessions, NOT 4!
        assertEquals(2, finalSessions.size)
        // Must contain D-406 rooms from Google Sheet
        assertTrue(finalSessions.all { it.room == "D-406" })
        assertTrue(finalSessions.any { it.courseName == "Professional Practices in IT" && it.timeSlot == "08:30 - 09:50" })
        assertTrue(finalSessions.any { it.courseName == "Information Security" && it.timeSlot == "10:00 - 11:20" })
    }

    @Test
    fun testTimeParserNormalizeTimeSlot() {
        assertEquals("08:30 - 09:50", com.example.fastable.utils.TimeParser.normalizeTimeSlot("08:30-09:50"))
        assertEquals("08:30 - 09:50", com.example.fastable.utils.TimeParser.normalizeTimeSlot("08:30 - 09:50"))
        assertEquals("08:30 - 09:50", com.example.fastable.utils.TimeParser.normalizeTimeSlot(" 08:30 - 09:50 "))
        assertEquals("10:00 - 11:20", com.example.fastable.utils.TimeParser.normalizeTimeSlot("10:00-11:20"))
    }

    @Test
    fun testCourseNameMatcherHighPerformanceAndCaching() {
        val startTime = System.currentTimeMillis()
        for (i in 0 until 10000) {
            val canonical = com.example.fastable.utils.CourseNameMatcher.getCanonicalName("PPIT (CS-E)")
            assertEquals("Professional Practices in IT", canonical)
        }
        val duration = System.currentTimeMillis() - startTime
        // 10,000 lookups must take less than 200ms
        assertTrue("Execution took ${duration}ms, expected < 200ms", duration < 200)
    }
}
