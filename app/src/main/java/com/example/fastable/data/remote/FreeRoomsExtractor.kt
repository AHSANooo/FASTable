package com.example.fastable.data.remote

import android.util.Log
import com.example.fastable.data.models.FreeRoom
import com.example.fastable.data.models.SlotWithFreeRooms
import com.example.fastable.utils.SheetsHelper
import com.example.fastable.utils.TimeParser
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.RowData
import java.util.Calendar

/**
 * Extracts free rooms/labs from the timetable spreadsheet
 * A room is "free" during a time slot if no class is scheduled there
 */
object FreeRoomsExtractor {

    private const val TAG = "FreeRoomsExtractor"

    /**
     * Represents a time slot with its column range
     */
    private data class TimeSlotRange(
        val timeSlot: String,
        val startCol: Int,
        val endCol: Int  // exclusive
    )

    /**
     * Get free rooms for a specific day
     * @param spreadsheet The Google Sheets spreadsheet
     * @param day The day to check (Monday, Tuesday, etc.)
     * @return List of FreeRoom objects with their free time slots
     */
    fun getFreeRoomsForDay(spreadsheet: Spreadsheet, day: String): List<FreeRoom> {
        val freeRooms = mutableListOf<FreeRoom>()

        // Find the sheet for the specified day
        val sheet = spreadsheet.sheets?.find {
            it.properties?.title?.equals(day, ignoreCase = true) == true
        }

        if (sheet == null) {
            Log.e(TAG, "Sheet not found for day: $day")
            return emptyList()
        }

        val gridData = sheet.data?.getOrNull(0)?.rowData ?: return emptyList()
        if (gridData.size < 6) return emptyList()

        // Build time slot ranges (each time slot spans multiple columns)
        val timeSlotRanges = buildTimeSlotRanges(gridData)
        Log.d(TAG, "Found ${timeSlotRanges.size} time slot ranges for $day")
        timeSlotRanges.forEach { Log.d(TAG, "  ${it.timeSlot}: cols ${it.startCol}-${it.endCol}") }

        if (timeSlotRanges.isEmpty()) {
            Log.e(TAG, "No time slots found for $day")
            return emptyList()
        }

        // Find lab section start
        var labSectionStartRow: Int = -1
        var labTimeSlotRanges: List<TimeSlotRange> = emptyList()

        gridData.forEachIndexed { idx, row ->
            val values = row.values ?: return@forEachIndexed
            val firstCellValue = getFirstCellValue(values)
            if (firstCellValue.equals("Lab", ignoreCase = true)) {
                labSectionStartRow = idx
                labTimeSlotRanges = buildTimeSlotRangesFromRow(row)
                Log.d(TAG, "Lab section starts at row $idx with ${labTimeSlotRanges.size} time slots")
            }
        }

        // Map to store room -> (timeSlotIndex -> isOccupied)
        val roomOccupancy = mutableMapOf<String, MutableMap<Int, Boolean>>()
        val roomIsLab = mutableMapOf<String, Boolean>()

        // Process all data rows (skip header rows - usually first 5 rows)
        gridData.forEachIndexed { rowIdx, row ->
            if (rowIdx < 5) return@forEachIndexed  // Skip header rows

            val rowValues = row.values ?: return@forEachIndexed
            val isLabSection = labSectionStartRow > 0 && rowIdx > labSectionStartRow

            // Get room name from first column
            val room = getRoomName(rowValues)

            // Skip invalid room names
            if (room.isEmpty() || isHeaderRow(room)) {
                return@forEachIndexed
            }

            // Determine which time slot ranges to use
            val relevantRanges = if (isLabSection && labTimeSlotRanges.isNotEmpty()) {
                labTimeSlotRanges
            } else {
                timeSlotRanges
            }

            // Initialize room if not exists
            if (!roomOccupancy.containsKey(room)) {
                roomOccupancy[room] = mutableMapOf()
                roomIsLab[room] = isLabSection || room.lowercase().contains("lab")

                // Initialize all time slots as free
                relevantRanges.forEachIndexed { slotIdx, _ ->
                    roomOccupancy[room]!![slotIdx] = false
                }
            }

            // Get all cell values from this row
            val cellValues = getAllCellValues(rowValues)

            // For each time slot range, check if ANY cell in that range has course content
            relevantRanges.forEachIndexed { slotIdx, range ->
                // Check all columns in this time slot range
                for (col in range.startCol until range.endCol) {
                    val cellValue = cellValues.getOrNull(col) ?: ""
                    if (hasCourseContent(cellValue)) {
                        roomOccupancy[room]?.set(slotIdx, true)

                        // Also check for embedded time spans that might overlap other slots
                        val embeddedSpan = extractEmbeddedTimeSpan(cellValue)
                        if (embeddedSpan != null) {
                            // Mark all overlapping time slots as occupied
                            relevantRanges.forEachIndexed { otherSlotIdx, otherRange ->
                                val slotTimes = extractStartEndTimes(cleanTimeSlot(otherRange.timeSlot))
                                if (slotTimes != null) {
                                    val slotStart = timeToMinutes(slotTimes.first)
                                    val slotEnd = timeToMinutes(slotTimes.second)
                                    if (timeSlotsOverlap(slotStart, slotEnd, embeddedSpan.first, embeddedSpan.second)) {
                                        roomOccupancy[room]?.set(otherSlotIdx, true)
                                    }
                                }
                            }
                        }
                        break  // Found content in this time slot, no need to check other columns
                    }
                }
            }
        }

        // Now calculate free slots for each room
        roomOccupancy.forEach { (room, slotOccupancy) ->
            val isLab = roomIsLab[room] ?: false
            val relevantRanges = if (isLab && labTimeSlotRanges.isNotEmpty()) {
                labTimeSlotRanges
            } else {
                timeSlotRanges
            }

            // Get free time slots
            val freeTimeSlots = slotOccupancy
                .filter { !it.value }  // Not occupied
                .mapNotNull { relevantRanges.getOrNull(it.key)?.timeSlot }
                .map { cleanTimeSlot(it) }
                .filter { it.isNotEmpty() }

            if (freeTimeSlots.isNotEmpty()) {
                val mergedSlots = mergeConsecutiveSlots(freeTimeSlots)

                freeRooms.add(
                    FreeRoom(
                        roomName = room,
                        freeSlots = mergedSlots,
                        isLab = isLab
                    )
                )
            }
        }

        Log.d(TAG, "Found ${freeRooms.size} rooms with free slots for $day")

        // Sort rooms: Classrooms first, then Labs, alphabetically within each group
        // Also filter out any invalid room names that slipped through
        return freeRooms
            .filter { !isInvalidRoomName(it.roomName) }
            .sortedWith(compareBy({ it.isLab }, { it.roomName }))
    }

    /**
     * Get free rooms organized by time slots for a specific day
     * This is more efficient as it groups rooms by slot rather than listing slots per room
     * @param spreadsheet The Google Sheets spreadsheet
     * @param day The day to check (Monday, Tuesday, etc.)
     * @return List of SlotWithFreeRooms, sorted by start time
     */
    fun getSlotWiseFreeRoomsForDay(spreadsheet: Spreadsheet, day: String): List<SlotWithFreeRooms> {
        val sheet = spreadsheet.sheets?.find {
            it.properties?.title?.equals(day, ignoreCase = true) == true
        }

        if (sheet == null) {
            Log.e(TAG, "Sheet not found for day: $day")
            return emptyList()
        }

        val gridData = sheet.data?.getOrNull(0)?.rowData ?: return emptyList()
        if (gridData.size < 6) return emptyList()

        // Build time slot ranges
        val timeSlotRanges = buildTimeSlotRanges(gridData)
        if (timeSlotRanges.isEmpty()) {
            Log.e(TAG, "No time slots found for $day")
            return emptyList()
        }

        // Find lab section start
        var labSectionStartRow = -1
        var labTimeSlotRanges: List<TimeSlotRange> = emptyList()

        gridData.forEachIndexed { idx, row ->
            val values = row.values ?: return@forEachIndexed
            val firstCellValue = getFirstCellValue(values)
            if (firstCellValue.equals("Lab", ignoreCase = true)) {
                labSectionStartRow = idx
                labTimeSlotRanges = buildTimeSlotRangesFromRow(row)
            }
        }

        // Map: slotIndex -> set of rooms occupied during ANY part of this slot
        val slotOccupiedRooms = mutableMapOf<Int, MutableSet<String>>()
        val slotOccupiedLabs = mutableMapOf<Int, MutableSet<String>>()

        // Track all rooms/labs that exist
        val allRooms = mutableSetOf<String>()
        val allLabs = mutableSetOf<String>()

        // Initialize slots
        timeSlotRanges.forEachIndexed { idx, _ ->
            slotOccupiedRooms[idx] = mutableSetOf()
            slotOccupiedLabs[idx] = mutableSetOf()
        }

        // Also initialize for lab time slots if different
        if (labTimeSlotRanges.isNotEmpty()) {
            labTimeSlotRanges.forEachIndexed { idx, _ ->
                if (!slotOccupiedLabs.containsKey(idx)) {
                    slotOccupiedLabs[idx] = mutableSetOf()
                }
            }
        }

        // Process all data rows
        gridData.forEachIndexed { rowIdx, row ->
            if (rowIdx < 5) return@forEachIndexed

            val rowValues = row.values ?: return@forEachIndexed
            val isLabSection = labSectionStartRow > 0 && rowIdx > labSectionStartRow
            val room = getRoomName(rowValues)

            if (room.isEmpty() || isHeaderRow(room)) return@forEachIndexed

            val isLab = isLabSection || room.lowercase().contains("lab")
            if (isLab) allLabs.add(room) else allRooms.add(room)

            val relevantRanges = if (isLabSection && labTimeSlotRanges.isNotEmpty()) {
                labTimeSlotRanges
            } else {
                timeSlotRanges
            }

            val cellValues = getAllCellValues(rowValues)

            relevantRanges.forEachIndexed { slotIdx, range ->
                for (col in range.startCol until range.endCol) {
                    val cellValue = cellValues.getOrNull(col) ?: ""
                    if (hasCourseContent(cellValue)) {
                        // Room is occupied in this slot
                        if (isLab) {
                            slotOccupiedLabs[slotIdx]?.add(room)
                        } else {
                            slotOccupiedRooms[slotIdx]?.add(room)
                        }
                        break
                    }
                }
            }
        }

        // Get current time for highlighting current/next slot
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        // Build result list
        val result = mutableListOf<SlotWithFreeRooms>()

        timeSlotRanges.forEachIndexed { slotIdx, range ->
            val cleanedSlot = cleanTimeSlot(range.timeSlot)
            val times = extractStartEndTimes(cleanedSlot)
            val startMinutes = times?.let { timeToMinutes(it.first) } ?: 0
            val endMinutes = times?.let { timeToMinutes(it.second) } ?: 0

            val freeRooms = allRooms.filter { room ->
                !(slotOccupiedRooms[slotIdx]?.contains(room) ?: false) && !isInvalidRoomName(room)
            }.sorted()

            val freeLabs = allLabs.filter { lab ->
                !(slotOccupiedLabs[slotIdx]?.contains(lab) ?: false) && !isInvalidRoomName(lab)
            }.sorted()

            result.add(
                SlotWithFreeRooms(
                    timeSlot = cleanedSlot,
                    startMinutes = startMinutes,
                    endMinutes = endMinutes,
                    freeRooms = freeRooms,
                    freeLabs = freeLabs,
                    isCurrentSlot = currentMinutes in startMinutes until endMinutes,
                    isNextSlot = false // Will be set later
                )
            )
        }

        // Sort by start time and mark next slot
        val sortedResult = result.sortedBy { it.startMinutes }
        val currentSlotIdx = sortedResult.indexOfFirst { it.isCurrentSlot }

        return sortedResult.mapIndexed { idx, slot ->
            slot.copy(isNextSlot = (currentSlotIdx >= 0 && idx == currentSlotIdx + 1) ||
                    (currentSlotIdx < 0 && slot.startMinutes > currentMinutes &&
                     sortedResult.none { it.startMinutes > currentMinutes && it.startMinutes < slot.startMinutes }))
        }
    }

    /**
     * Get currently available rooms (current slot + next slot only)
     * Efficient method that returns only the relevant slots
     */
    fun getCurrentlyAvailableRooms(spreadsheet: Spreadsheet, day: String): List<SlotWithFreeRooms> {
        val allSlots = getSlotWiseFreeRoomsForDay(spreadsheet, day)

        // Filter to only current and next slots
        return allSlots.filter { it.isCurrentSlot || it.isNextSlot }
    }

    /**
     * Build time slot ranges from the header row
     * Each time slot spans multiple columns until the next time slot starts
     */
    private fun buildTimeSlotRanges(gridData: List<RowData>): List<TimeSlotRange> {
        val ranges = mutableListOf<TimeSlotRange>()

        // Find the time row (row 5 in the image, 0-indexed = 4)
        for (rowIdx in 0 until minOf(10, gridData.size)) {
            val row = gridData[rowIdx]
            val rowValues = row.values ?: continue
            val firstCell = getFirstCellValue(rowValues)

            // Check if this is the time header row (starts with "Room" or has time patterns)
            val cellValues = getAllCellValues(rowValues)
            val timeColumns = mutableListOf<Pair<Int, String>>()

            cellValues.forEachIndexed { colIdx, value ->
                if (colIdx > 0 && value.matches(Regex(".*\\d{1,2}:\\d{2}.*"))) {
                    timeColumns.add(Pair(colIdx, value))
                }
            }

            if (timeColumns.size >= 3 || firstCell.lowercase().contains("room")) {
                // Build ranges from consecutive time columns
                for (i in timeColumns.indices) {
                    val (startCol, timeSlot) = timeColumns[i]
                    val endCol = if (i + 1 < timeColumns.size) {
                        timeColumns[i + 1].first
                    } else {
                        cellValues.size  // Last time slot extends to end
                    }
                    ranges.add(TimeSlotRange(timeSlot, startCol, endCol))
                }

                if (ranges.isNotEmpty()) {
                    Log.d(TAG, "Found time row at index $rowIdx")
                    break
                }
            }
        }

        return ranges
    }

    /**
     * Build time slot ranges from a specific row (for lab section)
     */
    private fun buildTimeSlotRangesFromRow(row: RowData): List<TimeSlotRange> {
        val ranges = mutableListOf<TimeSlotRange>()
        val cellValues = getAllCellValues(row.values ?: return emptyList())
        val timeColumns = mutableListOf<Pair<Int, String>>()

        cellValues.forEachIndexed { colIdx, value ->
            if (colIdx > 0 && value.matches(Regex(".*\\d{1,2}:\\d{2}.*"))) {
                timeColumns.add(Pair(colIdx, value))
            }
        }

        for (i in timeColumns.indices) {
            val (startCol, timeSlot) = timeColumns[i]
            val endCol = if (i + 1 < timeColumns.size) {
                timeColumns[i + 1].first
            } else {
                cellValues.size
            }
            ranges.add(TimeSlotRange(timeSlot, startCol, endCol))
        }

        return ranges
    }

    /**
     * Get all cell values from row as a list indexed by column
     */
    private fun getAllCellValues(rowValues: MutableCollection<Any>): List<String> {
        val values = mutableListOf<String>()
        rowValues.forEach { cellElement ->
            if (cellElement is ArrayList<*>) {
                cellElement.forEach { cell ->
                    values.add(SheetsHelper.getFormattedValue(cell)?.trim() ?: "")
                }
            }
        }
        return values
    }

    /**
     * Get the first cell value from row values
     */
    private fun getFirstCellValue(rowValues: MutableCollection<Any>): String {
        rowValues.forEach { cellElement ->
            if (cellElement is ArrayList<*> && cellElement.isNotEmpty()) {
                return SheetsHelper.getFormattedValue(cellElement[0])?.trim() ?: ""
            }
        }
        return ""
    }

    /**
     * Get room name from row values (first column)
     */
    private fun getRoomName(rowValues: MutableCollection<Any>): String {
        val rawValue = getFirstCellValue(rowValues)
        return TimeParser.cleanRoomData(rawValue.trim())
    }

    /**
     * Check if this is a header row (not a room)
     */
    private fun isHeaderRow(value: String): Boolean {
        val headerKeywords = listOf("lab", "room", "rooms", "time", "slot", "venue", "location", "unknown", "map", "classrooms", "classrooms/labs")
        val lowerValue = value.lowercase().trim()
        return headerKeywords.any { lowerValue == it } ||
               lowerValue.contains("classrooms") ||
               lowerValue.contains("unknown") ||
               (value.contains(":") && !value.any { it.isLetter() })
    }

    /**
     * Check if room name is invalid and should be filtered out
     */
    private fun isInvalidRoomName(roomName: String): Boolean {
        val invalidNames = listOf("unknown", "map", "classrooms", "classrooms/labs", "location", "rooms", "labs")
        val lowerName = roomName.lowercase().trim()
        return lowerName.isEmpty() ||
               invalidNames.any { lowerName == it || lowerName.contains(it) } ||
               lowerName.contains("classrooms") ||
               lowerName.contains("unknown") ||
               lowerName.startsWith("map")
    }

    /**
     * Check if a cell contains actual course content
     */
    private fun hasCourseContent(cellValue: String): Boolean {
        if (cellValue.isEmpty() || cellValue.isBlank()) return false
        if (cellValue.length < 2) return false
        if (cellValue.matches(Regex("^\\d+$"))) return false

        val emptyIndicators = listOf("-", "—", "–", "n/a", "na", "nil", "none", "empty", "free")
        if (emptyIndicators.any { cellValue.lowercase() == it }) return false

        // Must contain letters to be a course name
        return cellValue.any { it.isLetter() }
    }

    /**
     * Extract embedded time span from course text
     */
    private fun extractEmbeddedTimeSpan(cellValue: String): Pair<Int, Int>? {
        val timePattern = Regex("(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})")
        val match = timePattern.find(cellValue) ?: return null

        val startMinutes = timeToMinutes(match.groupValues[1])
        val endMinutes = timeToMinutes(match.groupValues[2])

        return if (startMinutes > 0 && endMinutes > 0) {
            Pair(startMinutes, endMinutes)
        } else null
    }

    /**
     * Check if two time ranges overlap
     */
    private fun timeSlotsOverlap(start1: Int, end1: Int, start2: Int, end2: Int): Boolean {
        return start1 < end2 && start2 < end1
    }

    /**
     * Clean time slot string
     */
    private fun cleanTimeSlot(timeSlot: String): String {
        return timeSlot
            .replace(Regex("\\s*\\(.*?\\)"), "")
            .replace(Regex("inc\\.?\\s*\\d+\\s*min\\.?\\s*break", RegexOption.IGNORE_CASE), "")
            .replace(Regex("including.*break", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*-\\s*"), " - ")
            .trim()
    }

    /**
     * Merge consecutive time slots
     */
    private fun mergeConsecutiveSlots(slots: List<String>): List<String> {
        if (slots.isEmpty()) return emptyList()
        if (slots.size == 1) return slots

        data class TimeSlot(val start: String, val end: String, val startMinutes: Int, val endMinutes: Int)

        val parsedSlots = slots.mapNotNull { slot ->
            val times = extractStartEndTimes(slot)
            if (times != null) {
                TimeSlot(times.first, times.second, timeToMinutes(times.first), timeToMinutes(times.second))
            } else null
        }.sortedBy { it.startMinutes }

        if (parsedSlots.isEmpty()) return slots
        if (parsedSlots.size == 1) return listOf("${parsedSlots[0].start} - ${parsedSlots[0].end}")

        val mergedSlots = mutableListOf<String>()
        var currentStart = parsedSlots[0].start
        var currentEnd = parsedSlots[0].end
        var currentEndMins = parsedSlots[0].endMinutes

        for (i in 1 until parsedSlots.size) {
            val nextSlot = parsedSlots[i]
            if (nextSlot.startMinutes <= currentEndMins + 15) {
                if (nextSlot.endMinutes > currentEndMins) {
                    currentEnd = nextSlot.end
                    currentEndMins = nextSlot.endMinutes
                }
            } else {
                mergedSlots.add("$currentStart - $currentEnd")
                currentStart = nextSlot.start
                currentEnd = nextSlot.end
                currentEndMins = nextSlot.endMinutes
            }
        }
        mergedSlots.add("$currentStart - $currentEnd")

        return mergedSlots
    }

    /**
     * Extract start and end times from a time slot string
     */
    private fun extractStartEndTimes(slot: String): Pair<String, String>? {
        val timeRegex = Regex("(\\d{1,2}:\\d{2})")
        val matches = timeRegex.findAll(slot).toList()
        return when {
            matches.size >= 2 -> Pair(matches[0].value, matches[1].value)
            matches.size == 1 -> Pair(matches[0].value, matches[0].value)
            else -> null
        }
    }

    /**
     * Convert time string to minutes from midnight
     */
    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        if (parts.size != 2) return 0
        val hour = parts[0].toIntOrNull() ?: return 0
        val minute = parts[1].toIntOrNull() ?: 0
        val adjustedHour = if (hour < 8) hour + 12 else hour
        return adjustedHour * 60 + minute
    }
}
