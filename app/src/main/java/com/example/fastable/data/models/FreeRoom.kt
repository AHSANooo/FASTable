package com.example.fastable.data.models

/**
 * Data class representing a room with its free time slots
 */
data class FreeRoom(
    val roomName: String,
    val freeSlots: List<String>,  // List of free time slot strings (already merged)
    val isLab: Boolean = false    // True if it's a lab room
) {
    /**
     * Get free slots as bullet points (each slot on a new line)
     */
    fun getFreeSlotsText(): String {
        return if (freeSlots.isEmpty()) {
            "Fully occupied"
        } else {
            freeSlots.joinToString("\n") { "• $it" }
        }
    }

    /**
     * Get simple display text without bullet points
     */
    fun getFreeSlotsSimple(): String {
        return if (freeSlots.isEmpty()) {
            "Fully occupied"
        } else {
            freeSlots.joinToString("\n")
        }
    }

    fun getRoomType(): String {
        return if (isLab) "Lab" else "Classroom"
    }
}

