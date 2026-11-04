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
                FloorRoom("A-01", "Classroom A-01", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("A-02", "Classroom A-02", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("A-03", "Classroom A-03", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("A-04", "Classroom A-04", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("A-AUD", "Main Auditorium", "Block A", floor, RoomType.FACILITY, "West Wing", 300),
                FloorRoom("A-ONESTOP", "One Stop Student Center", "Block A", floor, RoomType.FACILITY, "North Hallway", 30),
                FloorRoom("A-ACAD", "Manager Academics Office", "Block A", floor, RoomType.OFFICE, "North Hallway", 10),
                FloorRoom("A-ADMIN", "Admin & Accounts Office", "Block A", floor, RoomType.OFFICE, "North Hallway", 15)
            )
            "1st Floor" -> listOf(
                FloorRoom("A-101", "Classroom A-101", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("A-102", "Classroom A-102", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("A-103", "Classroom A-103", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("A-104", "Classroom A-104", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("A-CONF", "Conference Hall", "Block A", floor, RoomType.FACILITY, "West Wing", 80),
                FloorRoom("A-SEM", "Seminar Hall", "Block A", floor, RoomType.FACILITY, "West Wing", 100),
                FloorRoom("A-105", "Faculty Offices A-105", "Block A", floor, RoomType.OFFICE, "North Hallway", 20),
                FloorRoom("A-108", "Faculty Offices A-108", "Block A", floor, RoomType.OFFICE, "North Hallway", 20)
            )
            "2nd Floor" -> listOf(
                FloorRoom("A-201", "Classroom A-201", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("A-202", "Classroom A-202", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("A-203", "Classroom A-203", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("A-210", "Classroom A-210", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 45),
                FloorRoom("A-211", "Classroom A-211", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 45),
                FloorRoom("Khyber-1", "Khyber Lab 1", "Block A", floor, RoomType.LAB, "North Hallway", 40),
                FloorRoom("Khyber-2", "Khyber Lab 2", "Block A", floor, RoomType.LAB, "North Hallway", 40),
                FloorRoom("Khyber-3", "Khyber Lab 3", "Block A", floor, RoomType.LAB, "North Hallway", 40),
                FloorRoom("Khyber-4", "Khyber Lab 4", "Block A", floor, RoomType.LAB, "North Hallway", 40),
                FloorRoom("CALL-1", "CALL Lab 1 (A-213)", "Block A", floor, RoomType.LAB, "West Wing", 35),
                FloorRoom("CALL-2", "CALL Lab 2 (A-214)", "Block A", floor, RoomType.LAB, "West Wing", 35)
            )
            "3rd Floor" -> listOf(
                FloorRoom("A-301", "Classroom A-301", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("A-302", "Classroom A-302", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("A-303", "Classroom A-303", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("A-309", "Classroom A-309", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 45),
                FloorRoom("A-310", "Classroom A-310", "Block A", floor, RoomType.CLASSROOM, "South Hallway", 45),
                FloorRoom("Mehran-1", "Mehran Lab 1", "Block A", floor, RoomType.LAB, "North Hallway", 40),
                FloorRoom("Karakoram-1", "Karakoram Lab 1", "Block A", floor, RoomType.LAB, "West Wing", 35),
                FloorRoom("Karakoram-2", "Karakoram Lab 2", "Block A", floor, RoomType.LAB, "West Wing", 35),
                FloorRoom("CALL-3", "CALL Lab 3", "Block A", floor, RoomType.LAB, "North Hallway", 35)
            )
            else -> emptyList()
        }
    }

    private fun getBlockBRooms(floor: String): List<FloorRoom> {
        return when (floor) {
            "Ground Floor" -> listOf(
                FloorRoom("MARS-Lab", "MARS Lab (B-15)", "Block B", floor, RoomType.LAB, "North Corridor", 40),
                FloorRoom("B-16", "Classroom B-16", "Block B", floor, RoomType.CLASSROOM, "North Corridor", 45),
                FloorRoom("Power-Lab", "Power Systems Lab (B-22)", "Block B", floor, RoomType.LAB, "North Corridor", 40),
                FloorRoom("Machine-Lab", "Electric Machine Lab (B-23)", "Block B", floor, RoomType.LAB, "North Corridor", 40),
                FloorRoom("B-26", "Classroom B-26", "Block B", floor, RoomType.CLASSROOM, "West Corridor", 45),
                FloorRoom("B-27", "Classroom B-27", "Block B", floor, RoomType.CLASSROOM, "West Corridor", 45),
                FloorRoom("B-28", "Classroom B-28", "Block B", floor, RoomType.CLASSROOM, "West Corridor", 45)
            )
            "1st Floor" -> listOf(
                FloorRoom("B-213", "Classroom B-213", "Block B", floor, RoomType.CLASSROOM, "North Corridor", 45),
                FloorRoom("B-214", "Classroom B-214", "Block B", floor, RoomType.CLASSROOM, "North Corridor", 45),
                FloorRoom("Micro-Lab", "Microprocessor Lab (B-218)", "Block B", floor, RoomType.LAB, "North Corridor", 40),
                FloorRoom("DLD-Lab", "Digital Logic Design (DLD) Lab", "Block B", floor, RoomType.LAB, "South Corridor", 40),
                FloorRoom("B-215", "Classroom B-215", "Block B", floor, RoomType.CLASSROOM, "South Corridor", 45),
                FloorRoom("B-216", "Classroom B-216", "Block B", floor, RoomType.CLASSROOM, "South Corridor", 45),
                FloorRoom("Rawal-3", "Rawal Lab 3 (B-232)", "Block B", floor, RoomType.LAB, "West Corridor", 40)
            )
            "2nd Floor" -> listOf(
                FloorRoom("B-115", "Classroom B-115", "Block B", floor, RoomType.CLASSROOM, "North Corridor", 45),
                FloorRoom("Maker-Lab", "Maker Lab (B-116)", "Block B", floor, RoomType.LAB, "North Corridor", 35),
                FloorRoom("Electronic-Lab", "Electronic Lab (B-122B)", "Block B", floor, RoomType.LAB, "North Corridor", 35),
                FloorRoom("Physics-Lab", "Physics Lab (B-119)", "Block B", floor, RoomType.LAB, "South Corridor", 40),
                FloorRoom("Circuit-Lab", "Circuit Lab (B-123B)", "Block B", floor, RoomType.LAB, "South Corridor", 40),
                FloorRoom("RFCS-Lab", "RFCS Lab", "Block B", floor, RoomType.LAB, "West Corridor", 35),
                FloorRoom("Stationary", "Stationary & Book Shop", "Block B", floor, RoomType.FACILITY, "South Corridor", 20)
            )
            else -> emptyList()
        }
    }

    private fun getBlockCRooms(floor: String): List<FloorRoom> {
        return when (floor) {
            "Basement 1" -> listOf(
                FloorRoom("CAFE-MAIN", "Cafeteria Main Hall", "Block C", floor, RoomType.FACILITY, "Center Area", 250),
                FloorRoom("CAFE-1", "Cafe Counter 1 & 2", "Block C", floor, RoomType.FACILITY, "West Wing", 40),
                FloorRoom("BADMINTON", "Indoor Badminton Court", "Block C", floor, RoomType.FACILITY, "South Area", 50),
                FloorRoom("FACULTY-CAFE", "Faculty Sitting Area", "Block C", floor, RoomType.FACILITY, "South Area", 30)
            )
            "Ground Floor" -> listOf(
                FloorRoom("LIB-MAIN", "Central Library", "Block C", floor, RoomType.FACILITY, "Central Hall", 200),
                FloorRoom("LIB-STUDY-1", "Library Study Room 1-3", "Block C", floor, RoomType.CLASSROOM, "North Wing", 25),
                FloorRoom("LIB-STUDY-2", "Library Study Room 4-6", "Block C", floor, RoomType.CLASSROOM, "North Wing", 25),
                FloorRoom("LIB-STUDY-3", "Library Study Room 7-9", "Block C", floor, RoomType.CLASSROOM, "North Wing", 25),
                FloorRoom("LIB-MEDIA", "Digital Media Room", "Block C", floor, RoomType.LAB, "West Wing", 30)
            )
            "1st Floor" -> listOf(
                FloorRoom("C-105", "Learning Resource Center (C-105)", "Block C", floor, RoomType.FACILITY, "LRC Lobby", 60),
                FloorRoom("C-104", "Career Services Office", "Block C", floor, RoomType.OFFICE, "Center Corridor", 20),
                FloorRoom("C-109", "MEDC Conference Room", "Block C", floor, RoomType.FACILITY, "North Hallway", 40),
                FloorRoom("C-110", "MEDC Seminar Hall", "Block C", floor, RoomType.FACILITY, "North Hallway", 80)
            )
            "2nd Floor" -> listOf(
                FloorRoom("Margalla-1", "Margalla Lab 1 (C-209)", "Block C", floor, RoomType.LAB, "North Hallway", 45),
                FloorRoom("Margalla-2", "Margalla Lab 2 (C-210)", "Block C", floor, RoomType.LAB, "North Hallway", 45),
                FloorRoom("Margalla-3", "Margalla Lab 3 (C-211)", "Block C", floor, RoomType.LAB, "North Hallway", 45),
                FloorRoom("Margalla-4", "Margalla Lab 4 (C-212)", "Block C", floor, RoomType.LAB, "North Hallway", 45),
                FloorRoom("C-213", "Academic Office (Flex/Slate)", "Block C", floor, RoomType.OFFICE, "East Wing", 20),
                FloorRoom("C-201", "HOD CS Office", "Block C", floor, RoomType.OFFICE, "South Wing", 15)
            )
            "3rd Floor" -> listOf(
                // North Hallway
                FloorRoom("C-307", "Classroom C-307", "Block C", floor, RoomType.CLASSROOM, "North Hallway", 50),
                FloorRoom("C-308", "Classroom C-308", "Block C", floor, RoomType.CLASSROOM, "North Hallway", 50),
                FloorRoom("C-309", "Classroom C-309", "Block C", floor, RoomType.CLASSROOM, "North Hallway", 50),
                FloorRoom("C-310", "Classroom C-310", "Block C", floor, RoomType.CLASSROOM, "North Hallway", 50),
                FloorRoom("C-311", "Classroom C-311", "Block C", floor, RoomType.CLASSROOM, "North Hallway", 50),
                // South Hallway
                FloorRoom("C-301", "Classroom C-301", "Block C", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("C-302", "Classroom C-302", "Block C", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("C-303", "Classroom C-303", "Block C", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("C-304", "Classroom C-304", "Block C", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("C-305", "Classroom C-305", "Block C", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("C-306", "Girls Common Room (GCR)", "Block C", floor, RoomType.FACILITY, "North Hallway", 40)
            )
            "4th Floor" -> listOf(
                // North Hallway
                FloorRoom("C-406", "Classroom C-406", "Block C", floor, RoomType.CLASSROOM, "North Hallway", 50),
                FloorRoom("C-407", "Classroom C-407", "Block C", floor, RoomType.CLASSROOM, "North Hallway", 50),
                FloorRoom("C-408", "Classroom C-408", "Block C", floor, RoomType.CLASSROOM, "North Hallway", 50),
                FloorRoom("C-409", "Classroom C-409", "Block C", floor, RoomType.CLASSROOM, "North Hallway", 50),
                FloorRoom("C-410", "Classroom C-410", "Block C", floor, RoomType.CLASSROOM, "North Hallway", 50),
                // South Hallway
                FloorRoom("C-401", "Classroom C-401", "Block C", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("C-402", "Classroom C-402", "Block C", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("C-403", "Classroom C-403", "Block C", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("C-404", "Classroom C-404", "Block C", floor, RoomType.CLASSROOM, "South Hallway", 50),
                FloorRoom("C-405", "Classroom C-405", "Block C", floor, RoomType.CLASSROOM, "South Hallway", 50)
            )
            "5th Floor" -> listOf(
                FloorRoom("Rawal-1", "Rawal Lab 1 (C-509)", "Block C", floor, RoomType.LAB, "North Hallway", 45),
                FloorRoom("Rawal-2", "Rawal Lab 2 (C-510)", "Block C", floor, RoomType.LAB, "North Hallway", 45),
                FloorRoom("GPU-Lab", "GPU Research Lab (C-511)", "Block C", floor, RoomType.LAB, "North Hallway", 40),
                FloorRoom("Rawal-4", "Rawal Lab 4 (C-512)", "Block C", floor, RoomType.LAB, "North Hallway", 45),
                FloorRoom("C-513", "Graduate Academic Office", "Block C", floor, RoomType.OFFICE, "East Wing", 20),
                FloorRoom("C-501", "HOD CS Office", "Block C", floor, RoomType.OFFICE, "South Wing", 15)
            )
            else -> emptyList()
        }
    }

    /**
     * Evaluates whether a room is currently Free, Occupied, or matching the user's enrolled class.
     */
    fun evaluateRoomStatus(
        room: FloorRoom,
        userSessions: List<DashboardSession>,
        allCampusSessions: List<DashboardSession> = emptyList(),
        currentDay: String,
        currentMinutes: Int
    ): RoomStatusInfo {
        // 1. Check if matching student's enrolled class today
        val userMatch = userSessions.find { session ->
            session.day.equals(currentDay, ignoreCase = true) &&
                    isRoomNameMatching(room.id, session.room)
        }

        if (userMatch != null) {
            val (startMin, endMin) = parseTimeSlotMinutes(userMatch.timeSlot)
            val isCurrentTime = currentMinutes in startMin..endMin || (startMin - currentMinutes in 0..20)
            return RoomStatusInfo(
                room = room,
                state = RoomOccupancyState.STUDENT_CLASS,
                currentLecture = userMatch.courseName,
                timeSlot = userMatch.timeSlot,
                details = "Section: ${userMatch.section} (${userMatch.sessionType})",
                freeUntil = null,
                isUserClass = true
            )
        }

        // 2. Check if occupied by another scheduled university class
        val campusMatch = allCampusSessions.find { session ->
            session.day.equals(currentDay, ignoreCase = true) &&
                    isRoomNameMatching(room.id, session.room)
        }

        if (campusMatch != null) {
            val (startMin, endMin) = parseTimeSlotMinutes(campusMatch.timeSlot)
            if (currentMinutes in startMin..endMin || (currentMinutes < 8 * 60 + 30)) {
                return RoomStatusInfo(
                    room = room,
                    state = RoomOccupancyState.OCCUPIED,
                    currentLecture = campusMatch.courseName,
                    timeSlot = campusMatch.timeSlot,
                    details = "Section: ${campusMatch.section} (${campusMatch.sessionType})",
                    freeUntil = null,
                    isUserClass = false
                )
            }
        }

        // 3. Otherwise: Room is FREE for study
        return RoomStatusInfo(
            room = room,
            state = RoomOccupancyState.FREE,
            currentLecture = null,
            timeSlot = null,
            details = "Ideal spot for quiet study or group discussion",
            freeUntil = "Free for study",
            isUserClass = false
        )
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

