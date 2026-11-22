package com.example.fastable.data.repository

import com.example.fastable.data.models.*
import java.util.Calendar
import java.util.Locale

/**
 * Repository containing architectural floor plans for Block A, Block B, and Block C (FAST ISB)
 * with live occupancy calculation and "Your Room" detection.
 */
object CampusFloorRepository {

    fun getBlocks(): List<String> = listOf("Block A", "Block B", "Block C")

    fun getFloorsForBlock(block: String): List<String> {
        return when (block) {
            "Block A" -> listOf("Ground Floor", "1st Floor", "2nd Floor", "3rd Floor")
            "Block B" -> listOf("Ground Floor", "1st Floor", "2nd Floor")
            "Block C" -> listOf("Basement 1", "Ground Floor", "1st Floor", "2nd Floor", "3rd Floor", "4th Floor", "5th Floor")
            else -> listOf("Ground Floor", "1st Floor")
        }
    }

    fun getRoomsForFloor(block: String, floor: String): List<FloorRoom> {
        return when (block) {
            "Block A" -> getBlockARooms(floor)
            "Block B" -> getBlockBRooms(floor)
            "Block C" -> getBlockCRooms(floor)
            else -> emptyList()
        }
    }

    private fun getBlockARooms(floor: String): List<FloorRoom> {
        return when (floor) {
            "Ground Floor" -> listOf(
                FloorRoom("A-01", "Classroom A-01", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("A-02", "Classroom A-02", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("A-03", "Classroom A-03", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("A-04", "Classroom A-04", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("A-AUD", "Main Auditorium", "Block A", floor, RoomType.FACILITY, "West Wing"),
                FloorRoom("A-ONESTOP", "One Stop Student Center", "Block A", floor, RoomType.FACILITY, "North Hallway"),
                FloorRoom("A-ACAD", "Manager Academics Office", "Block A", floor, RoomType.OFFICE, "North Hallway"),
                FloorRoom("A-ADMIN", "Admin & Accounts Office", "Block A", floor, RoomType.OFFICE, "North Hallway")
            )
            "1st Floor" -> listOf(
                FloorRoom("A-101", "Classroom A-101", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("A-102", "Classroom A-102", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("A-103", "Classroom A-103", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("A-104", "Classroom A-104", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("A-CONF", "Conference Hall", "Block A", floor, RoomType.FACILITY, "West Wing"),
                FloorRoom("A-SEM", "Seminar Hall", "Block A", floor, RoomType.FACILITY, "West Wing"),
                FloorRoom("A-105", "Faculty Offices A-105", "Block A", floor, RoomType.OFFICE, "North Hallway"),
                FloorRoom("A-108", "Faculty Offices A-108", "Block A", floor, RoomType.OFFICE, "North Hallway")
            )
            "2nd Floor" -> listOf(
                FloorRoom("A-201", "Classroom A-201", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("A-202", "Classroom A-202", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("A-203", "Classroom A-203", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("A-210", "Classroom A-210", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("A-211", "Classroom A-211", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("Khyber-1", "Khyber Lab 1", "Block A", floor, RoomType.LAB, "North Hallway"),
                FloorRoom("Khyber-2", "Khyber Lab 2", "Block A", floor, RoomType.LAB, "North Hallway"),
                FloorRoom("Khyber-3", "Khyber Lab 3", "Block A", floor, RoomType.LAB, "North Hallway"),
                FloorRoom("Khyber-4", "Khyber Lab 4", "Block A", floor, RoomType.LAB, "North Hallway"),
                FloorRoom("CALL-1", "CALL Lab 1 (A-213)", "Block A", floor, RoomType.LAB, "West Wing"),
                FloorRoom("CALL-2", "CALL Lab 2 (A-214)", "Block A", floor, RoomType.LAB, "West Wing")
            )
            "3rd Floor" -> listOf(
                FloorRoom("A-301", "Classroom A-301", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("A-302", "Classroom A-302", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("A-303", "Classroom A-303", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("A-309", "Classroom A-309", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("A-310", "Classroom A-310", "Block A", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("Mehran-1", "Mehran Lab 1", "Block A", floor, RoomType.LAB, "North Hallway"),
                FloorRoom("Karakoram-1", "Karakoram Lab 1", "Block A", floor, RoomType.LAB, "West Wing"),
                FloorRoom("Karakoram-2", "Karakoram Lab 2", "Block A", floor, RoomType.LAB, "West Wing"),
                FloorRoom("CALL-3", "CALL Lab 3", "Block A", floor, RoomType.LAB, "North Hallway")
            )
            else -> emptyList()
        }
    }

    private fun getBlockBRooms(floor: String): List<FloorRoom> {
        return when (floor) {
            "Ground Floor" -> listOf(
                FloorRoom("MARS-Lab", "MARS Lab (B-15)", "Block B", floor, RoomType.LAB, "North Corridor"),
                FloorRoom("B-16", "Classroom B-16", "Block B", floor, RoomType.CLASSROOM, "North Corridor"),
                FloorRoom("Power-Lab", "Power Systems Lab (B-22)", "Block B", floor, RoomType.LAB, "North Corridor"),
                FloorRoom("Machine-Lab", "Electric Machine Lab (B-23)", "Block B", floor, RoomType.LAB, "North Corridor"),
                FloorRoom("B-26", "Classroom B-26", "Block B", floor, RoomType.CLASSROOM, "West Corridor"),
                FloorRoom("B-27", "Classroom B-27", "Block B", floor, RoomType.CLASSROOM, "West Corridor"),
                FloorRoom("B-28", "Classroom B-28", "Block B", floor, RoomType.CLASSROOM, "West Corridor")
            )
            "1st Floor" -> listOf(
                FloorRoom("B-213", "Classroom B-213", "Block B", floor, RoomType.CLASSROOM, "North Corridor"),
                FloorRoom("B-214", "Classroom B-214", "Block B", floor, RoomType.CLASSROOM, "North Corridor"),
                FloorRoom("Micro-Lab", "Microprocessor Lab (B-218)", "Block B", floor, RoomType.LAB, "North Corridor"),
                FloorRoom("DLD-Lab", "Digital Logic Design (DLD) Lab", "Block B", floor, RoomType.LAB, "South Corridor"),
                FloorRoom("B-215", "Classroom B-215", "Block B", floor, RoomType.CLASSROOM, "South Corridor"),
                FloorRoom("B-216", "Classroom B-216", "Block B", floor, RoomType.CLASSROOM, "South Corridor"),
                FloorRoom("Rawal-3", "Rawal Lab 3 (B-232)", "Block B", floor, RoomType.LAB, "West Corridor")
            )
            "2nd Floor" -> listOf(
                FloorRoom("B-115", "Classroom B-115", "Block B", floor, RoomType.CLASSROOM, "North Corridor"),
                FloorRoom("Maker-Lab", "Maker Lab (B-116)", "Block B", floor, RoomType.LAB, "North Corridor"),
                FloorRoom("Electronic-Lab", "Electronic Lab (B-122B)", "Block B", floor, RoomType.LAB, "North Corridor"),
                FloorRoom("Physics-Lab", "Physics Lab (B-119)", "Block B", floor, RoomType.LAB, "South Corridor"),
                FloorRoom("Circuit-Lab", "Circuit Lab (B-123B)", "Block B", floor, RoomType.LAB, "South Corridor"),
                FloorRoom("RFCS-Lab", "RFCS Lab", "Block B", floor, RoomType.LAB, "West Corridor"),
                FloorRoom("Stationary", "Stationary & Book Shop", "Block B", floor, RoomType.FACILITY, "South Corridor")
            )
            else -> emptyList()
        }
    }

    private fun getBlockCRooms(floor: String): List<FloorRoom> {
        return when (floor) {
            "Basement 1" -> listOf(
                FloorRoom("CAFE-MAIN", "Cafeteria Main Hall", "Block C", floor, RoomType.FACILITY, "Center Area"),
                FloorRoom("CAFE-1", "Cafe Counter 1 & 2", "Block C", floor, RoomType.FACILITY, "West Wing"),
                FloorRoom("BADMINTON", "Indoor Badminton Court", "Block C", floor, RoomType.FACILITY, "South Area"),
                FloorRoom("FACULTY-CAFE", "Faculty Sitting Area", "Block C", floor, RoomType.FACILITY, "South Area")
            )
            "Ground Floor" -> listOf(
                FloorRoom("LIB-MAIN", "Central Library", "Block C", floor, RoomType.FACILITY, "Central Hall"),
                FloorRoom("LIB-STUDY-1", "Library Study Room 1-3", "Block C", floor, RoomType.CLASSROOM, "North Wing"),
                FloorRoom("LIB-STUDY-2", "Library Study Room 4-6", "Block C", floor, RoomType.CLASSROOM, "North Wing"),
                FloorRoom("LIB-STUDY-3", "Library Study Room 7-9", "Block C", floor, RoomType.CLASSROOM, "North Wing"),
                FloorRoom("LIB-MEDIA", "Digital Media Room", "Block C", floor, RoomType.LAB, "West Wing")
            )
            "1st Floor" -> listOf(
                FloorRoom("C-105", "Learning Resource Center (C-105)", "Block C", floor, RoomType.FACILITY, "LRC Lobby"),
                FloorRoom("C-104", "Career Services Office", "Block C", floor, RoomType.OFFICE, "Center Corridor"),
                FloorRoom("C-109", "MEDC Conference Room", "Block C", floor, RoomType.FACILITY, "North Hallway"),
                FloorRoom("C-110", "MEDC Seminar Hall", "Block C", floor, RoomType.FACILITY, "North Hallway")
            )
            "2nd Floor" -> listOf(
                FloorRoom("Margalla-1", "Margalla Lab 1 (C-209)", "Block C", floor, RoomType.LAB, "North Hallway"),
                FloorRoom("Margalla-2", "Margalla Lab 2 (C-210)", "Block C", floor, RoomType.LAB, "North Hallway"),
                FloorRoom("Margalla-3", "Margalla Lab 3 (C-211)", "Block C", floor, RoomType.LAB, "North Hallway"),
                FloorRoom("Margalla-4", "Margalla Lab 4 (C-212)", "Block C", floor, RoomType.LAB, "North Hallway"),
                FloorRoom("C-213", "Academic Office (Flex/Slate)", "Block C", floor, RoomType.OFFICE, "East Wing"),
                FloorRoom("C-201", "HOD CS Office", "Block C", floor, RoomType.OFFICE, "South Wing")
            )
            "3rd Floor" -> listOf(
                // North Hallway
                FloorRoom("C-307", "Classroom C-307", "Block C", floor, RoomType.CLASSROOM, "North Hallway"),
                FloorRoom("C-308", "Classroom C-308", "Block C", floor, RoomType.CLASSROOM, "North Hallway"),
                FloorRoom("C-309", "Classroom C-309", "Block C", floor, RoomType.CLASSROOM, "North Hallway"),
                FloorRoom("C-310", "Classroom C-310", "Block C", floor, RoomType.CLASSROOM, "North Hallway"),
                FloorRoom("C-311", "Classroom C-311", "Block C", floor, RoomType.CLASSROOM, "North Hallway"),
                // South Hallway
                FloorRoom("C-301", "Classroom C-301", "Block C", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("C-302", "Classroom C-302", "Block C", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("C-303", "Classroom C-303", "Block C", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("C-304", "Classroom C-304", "Block C", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("C-305", "Classroom C-305", "Block C", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("C-306", "Girls Common Room (GCR)", "Block C", floor, RoomType.FACILITY, "North Hallway")
            )
            "4th Floor" -> listOf(
                // North Hallway
                FloorRoom("C-406", "Classroom C-406", "Block C", floor, RoomType.CLASSROOM, "North Hallway"),
                FloorRoom("C-407", "Classroom C-407", "Block C", floor, RoomType.CLASSROOM, "North Hallway"),
                FloorRoom("C-408", "Classroom C-408", "Block C", floor, RoomType.CLASSROOM, "North Hallway"),
                FloorRoom("C-409", "Classroom C-409", "Block C", floor, RoomType.CLASSROOM, "North Hallway"),
                FloorRoom("C-410", "Classroom C-410", "Block C", floor, RoomType.CLASSROOM, "North Hallway"),
                // South Hallway
                FloorRoom("C-401", "Classroom C-401", "Block C", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("C-402", "Classroom C-402", "Block C", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("C-403", "Classroom C-403", "Block C", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("C-404", "Classroom C-404", "Block C", floor, RoomType.CLASSROOM, "South Hallway"),
                FloorRoom("C-405", "Classroom C-405", "Block C", floor, RoomType.CLASSROOM, "South Hallway")
            )
            "5th Floor" -> listOf(
                FloorRoom("Rawal-1", "Rawal Lab 1 (C-509)", "Block C", floor, RoomType.LAB, "North Hallway"),
                FloorRoom("Rawal-2", "Rawal Lab 2 (C-510)", "Block C", floor, RoomType.LAB, "North Hallway"),
                FloorRoom("GPU-Lab", "GPU Research Lab (C-511)", "Block C", floor, RoomType.LAB, "North Hallway"),
                FloorRoom("Rawal-4", "Rawal Lab 4 (C-512)", "Block C", floor, RoomType.LAB, "North Hallway"),
                FloorRoom("C-513", "Graduate Academic Office", "Block C", floor, RoomType.OFFICE, "East Wing"),
                FloorRoom("C-501", "HOD CS Office", "Block C", floor, RoomType.OFFICE, "South Wing")
            )
            else -> emptyList()
        }
    }

    /**
     * Evaluates whether a room is currently Free, Occupied, or matching the user's enrolled class.
     * Incorporates both ongoing and upcoming lecture context so rooms show rich schedule info.
     */
    fun evaluateRoomStatus(
        room: FloorRoom,
        userSessions: List<DashboardSession>,
        allCampusSessions: List<DashboardSession> = emptyList(),
        currentDay: String,
        currentMinutes: Int
    ): RoomStatusInfo {
        // Effective day: on weekend (Sunday or Saturday), preview Monday's full schedule
        val dayToEvaluate = if (currentDay.equals("Sunday", ignoreCase = true) || currentDay.equals("Saturday", ignoreCase = true)) {
            "Monday"
        } else {
            currentDay
        }

        // 1. Check if matching student's enrolled class today
        val userMatchesToday = userSessions.filter { session ->
            session.day.equals(dayToEvaluate, ignoreCase = true) &&
                    isRoomNameMatching(room.id, session.room)
        }

        if (userMatchesToday.isNotEmpty()) {
            val ongoing = userMatchesToday.find { session ->
                val (startMin, endMin) = parseTimeSlotMinutes(session.timeSlot)
                currentMinutes in startMin..endMin || (startMin - currentMinutes in 0..15)
            }
            val activeSession = ongoing ?: userMatchesToday.first()
            val isLiveNow = ongoing != null

            return RoomStatusInfo(
                room = room,
                state = RoomOccupancyState.STUDENT_CLASS,
                currentLecture = activeSession.courseName,
                timeSlot = activeSession.timeSlot,
                details = if (isLiveNow) "★ Ongoing Now: ${activeSession.courseName} (${activeSession.timeSlot})"
                          else "★ Today at ${activeSession.timeSlot.split("-").first().trim()}: ${activeSession.courseName}",
                freeUntil = null,
                isUserClass = true
            )
        }

        // 2. Check scheduled university campus classes for this room
        val campusPool = if (allCampusSessions.isNotEmpty()) allCampusSessions else getCampusStandardSessions(dayToEvaluate)
        val roomClassesToday = campusPool.filter { session ->
            session.day.equals(dayToEvaluate, ignoreCase = true) &&
                    isRoomNameMatching(room.id, session.room)
        }.sortedBy { parseTimeSlotMinutes(it.timeSlot).first }

        if (roomClassesToday.isNotEmpty()) {
            // Check if currently ongoing
            val ongoing = roomClassesToday.find { session ->
                val (startMin, endMin) = parseTimeSlotMinutes(session.timeSlot)
                currentMinutes in startMin..endMin
            }

            if (ongoing != null) {
                val (_, endMin) = parseTimeSlotMinutes(ongoing.timeSlot)
                val endFormatted = formatMinutes(endMin)
                return RoomStatusInfo(
                    room = room,
                    state = RoomOccupancyState.OCCUPIED,
                    currentLecture = ongoing.courseName,
                    timeSlot = ongoing.timeSlot,
                    details = "Ongoing (${ongoing.timeSlot}) • Free at $endFormatted",
                    freeUntil = endFormatted,
                    isUserClass = false
                )
            }

            // Check if upcoming today
            val upcoming = roomClassesToday.find { session ->
                val (startMin, _) = parseTimeSlotMinutes(session.timeSlot)
                startMin > currentMinutes
            }

            if (upcoming != null) {
                val (startMin, _) = parseTimeSlotMinutes(upcoming.timeSlot)
                val startFormatted = formatMinutes(startMin)
                return RoomStatusInfo(
                    room = room,
                    state = RoomOccupancyState.FREE,
                    currentLecture = null,
                    timeSlot = upcoming.timeSlot,
                    details = "Free Now • Next: ${upcoming.courseName} at $startFormatted",
                    freeUntil = startFormatted,
                    isUserClass = false
                )
            }

            // If all classes for today finished
            val lastClass = roomClassesToday.last()
            return RoomStatusInfo(
                room = room,
                state = RoomOccupancyState.FREE,
                currentLecture = null,
                timeSlot = null,
                details = "Free for study • Classes finished (Last: ${lastClass.courseName})",
                freeUntil = "Open for study",
                isUserClass = false
            )
        }

        // 3. Otherwise: Room is free all day
        return RoomStatusInfo(
            room = room,
            state = RoomOccupancyState.FREE,
            currentLecture = null,
            timeSlot = null,
            details = "Vacant study spot • Quiet area",
            freeUntil = "Open all day",
            isUserClass = false
        )
    }

    private fun formatMinutes(minutes: Int): String {
        var h = minutes / 60
        val m = minutes % 60
        val ampm = if (h >= 12) "PM" else "AM"
        if (h > 12) h -= 12
        if (h == 0) h = 12
        return String.format(Locale.ROOT, "%02d:%02d %s", h, m, ampm)
    }

    /**
     * Standard university scheduled sessions across campus rooms for baseline occupancy.
     */
    fun getCampusStandardSessions(day: String): List<DashboardSession> {
        val now = System.currentTimeMillis()
        val s = mutableListOf<DashboardSession>()

        fun addSession(room: String, slot: String, course: String, sec: String) {
            s.add(
                DashboardSession(
                    day = day,
                    timeSlot = slot,
                    room = room,
                    sessionType = if (course.contains("Lab", ignoreCase = true)) "Lab" else "Class",
                    courseName = course,
                    section = sec,
                    batch = "BS CS (2023)",
                    department = "CS",
                    rank = 1,
                    colorCode = "#3B82F6",
                    isCustom = false,
                    lastUpdated = now
                )
            )
        }

        // Block C Classrooms & Labs
        addSession("C-301", "08:30 - 09:50", "Operating Systems", "CS-A")
        addSession("C-301", "10:00 - 11:20", "Data Structures", "CS-E")
        addSession("C-301", "11:30 - 12:50", "Design & Analysis of Algorithms", "CS-B")
        addSession("C-301", "01:00 - 02:20", "Parallel & Distributed Computing", "CS-C")

        addSession("C-302", "08:30 - 09:50", "Computer Organization & Assembly", "CS-E")
        addSession("C-302", "10:00 - 11:20", "Applied Human Computer Interaction", "CS-E")
        addSession("C-302", "01:00 - 02:20", "Artificial Intelligence", "AI-A")

        addSession("C-303", "10:00 - 11:20", "Discrete Structures", "CS-B")
        addSession("C-303", "11:30 - 12:50", "Software Construction & Development", "SE-A")

        addSession("C-304", "08:30 - 09:50", "Programming Fundamentals", "CS-A")
        addSession("C-304", "11:30 - 12:50", "Linear Algebra", "CS-E")
        addSession("C-304", "01:00 - 02:20", "Information Security", "CS-E")

        addSession("C-305", "08:30 - 09:50", "Discrete Structures", "CS-E")
        addSession("C-305", "10:00 - 11:20", "Machine Learning", "AI-B")

        addSession("C-401", "10:00 - 11:20", "MLOps", "CS-B")
        addSession("C-401", "11:30 - 12:50", "Cloud Computing", "CS-A")

        addSession("C-402", "08:30 - 09:50", "Database Systems", "CS-B")
        addSession("C-402", "01:00 - 02:20", "Deep Learning", "AI-A")

        addSession("C-406", "10:00 - 11:20", "Generative AI", "AI-A")
        addSession("C-407", "11:30 - 12:50", "Multiagent Systems", "AI-B")

        addSession("Rawal Lab 1", "10:00 - 12:50", "Assembly Language Lab", "CS-E")
        addSession("Rawal Lab 1", "01:00 - 03:50", "Operating Systems Lab", "CS-A")
        addSession("Margalla Lab 1", "01:00 - 03:50", "Data Structures Lab", "CS-E")
        addSession("Khyber Lab 1", "10:00 - 12:50", "Database Systems Lab", "CS-B")

        // Block A Classrooms & Labs
        addSession("A-101", "08:30 - 09:50", "Technical & Business Writing", "SE-A")
        addSession("A-101", "10:00 - 11:20", "Software Quality Engineering", "SE-A")
        addSession("A-201", "11:30 - 12:50", "Formal Methods in SE", "SE-B")
        addSession("Khyber-1", "01:00 - 03:50", "Programming Fundamentals Lab", "SE-A")

        // Block B
        addSession("B-16", "10:00 - 11:20", "Digital Logic Design", "CS-C")
        addSession("B-26", "11:30 - 12:50", "Electric Machine Analysis", "EE-A")

        return s
    }

    fun isRoomNameMatching(roomPattern: String, sessionRoom: String): Boolean {
        fun normalize(s: String): String {
            return s.lowercase(Locale.ROOT)
                .replace("classroom", "")
                .replace("lab", "")
                .replace("room", "")
                .replace("-", "")
                .replace(" ", "")
                .trim()
        }

        val norm1 = normalize(roomPattern)
        val norm2 = normalize(sessionRoom)

        if (norm1.isNotEmpty() && norm1 == norm2) return true
        if (norm1.isNotEmpty() && norm2.isNotEmpty() && (norm2.contains(norm1) || norm1.contains(norm2))) return true

        return false
    }

    private fun parseTimeSlotMinutes(slot: String): Pair<Int, Int> {
        val tokens = Regex("(\\d{1,2}:\\d{2})").findAll(slot).map { it.value }.toList()
        if (tokens.isEmpty()) return Pair(8 * 60 + 30, 9 * 60 + 50)

        fun toMin(t: String, isEnd: Boolean): Int {
            val p = t.split(":")
            var h = p[0].toIntOrNull() ?: 8
            val m = p.getOrNull(1)?.toIntOrNull() ?: 0
            if (h in 1..7) h += 12
            else if (h == 8 && isEnd) h += 12
            return h * 60 + m
        }

        val start = toMin(tokens[0], false)
        val end = if (tokens.size >= 2) toMin(tokens[1], true) else start + 80
        return Pair(start, end)
    }
}

