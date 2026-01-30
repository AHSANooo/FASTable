package com.example.fastable.data.models

/**
 * Data class representing a room with its free time slots
 */
data class FreeRoom(
    val roomName: String,
    val freeSlots: List<String>,  // List of free time slot strings
    val isLab: Boolean = false    // True if it's a lab room
) {
    fun getFreeSlotsText(): String {
        return if (freeSlots.isEmpty()) {
            "No free slots"
        } else {
            freeSlots.joinToString(", ")
        }
    }

    fun getRoomType(): String {
        return if (isLab) "Lab" else "Classroom"
    }
}

