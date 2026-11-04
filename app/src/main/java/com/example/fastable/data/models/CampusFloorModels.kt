package com.example.fastable.data.models

enum class RoomType {
    CLASSROOM,
    LAB,
    OFFICE,
    FACILITY
}

enum class RoomOccupancyState {
    FREE,          // Green: Vacant study space
    OCCUPIED,      // Red: Ongoing lecture or lab
    STUDENT_CLASS  // Blue/Gold: ★ "Your Room" matching the student's active class!
}

data class FloorRoom(
    val id: String,                 // e.g. "C-301", "Margalla Lab 1"
    val displayName: String,        // e.g. "C-301"
    val block: String,              // "Block A", "Block B", "Block C"
    val floor: String,              // e.g. "3rd Floor"
    val type: RoomType,             // CLASSROOM, LAB, OFFICE, FACILITY
    val wing: String = "Main Corridor", // e.g. "North Hallway", "South Hallway"
    val capacity: Int = 45          // Approximate seating capacity
)

data class RoomStatusInfo(
    val room: FloorRoom,
    val state: RoomOccupancyState,
    val currentLecture: String? = null,
    val timeSlot: String? = null,
    val details: String? = null,
    val freeUntil: String? = "5:15 PM",
    val isUserClass: Boolean = false
)

data class CorridorSection(
    val wingName: String,
    val rooms: List<RoomStatusInfo>
)

