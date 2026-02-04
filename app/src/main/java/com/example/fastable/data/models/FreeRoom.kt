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

/**
 * Data class representing a time slot with rooms free during that entire slot
 */
data class SlotWithFreeRooms(
    val timeSlot: String,           // e.g., "8:30 - 10:00"
    val startMinutes: Int,          // For sorting
    val endMinutes: Int,            // For current slot detection
    val freeRooms: List<String>,    // Rooms free for the entire slot
    val freeLabs: List<String>,     // Labs free for the entire slot
    val isCurrentSlot: Boolean = false,  // Highlight if current
    val isNextSlot: Boolean = false      // Highlight if next
) {
    fun getRoomCount(): Int = freeRooms.size
    fun getLabCount(): Int = freeLabs.size
    fun getTotalCount(): Int = freeRooms.size + freeLabs.size
}
