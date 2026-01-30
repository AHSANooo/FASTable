package com.example.fastable.data.remote

import android.util.Log
import com.example.fastable.data.models.FreeRoom
import com.example.fastable.utils.SheetsHelper
import com.example.fastable.utils.TimeParser
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.RowData

/**
 * Extracts free rooms/labs from the timetable spreadsheet
 * A room is "free" during a time slot if no class is scheduled there
 */
object FreeRoomsExtractor {

    private const val TAG = "FreeRoomsExtractor"

    // Standard time slots used in university timetable
    private val STANDARD_TIME_SLOTS = listOf(
        "8:30-10:00",
        "10:00-11:30",
        "11:30-1:00",
        "1:00-2:30",
        "2:30-4:00",
        "4:00-5:30"
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

        // Build time column mapping
        val timeSlotsByColumn = extractTimeSlots(gridData)
        Log.d(TAG, "Found ${timeSlotsByColumn.size} time columns for $day")

        // Find lab time row for lab sections
        var labTimeRow: RowData? = null
        var labTimeRowIndex: Int = -1

        gridData.forEachIndexed { idx, row ->
            val values = row.values
            val firstCellValue = SheetsHelper.getFormattedValue(SheetsHelper.getCellAt(values, 0))
            if (firstCellValue != null && firstCellValue.equals("Lab", ignoreCase = true)) {
                labTimeRow = row
                labTimeRowIndex = idx
                Log.d(TAG, "Lab row found at index: $idx")
            }
        }

        // Extract lab time slots if lab row exists
        val labTimeSlotsByColumn = if (labTimeRow != null) {
            extractTimeSlotsFromRow(labTimeRow)
        } else {
            emptyMap()
        }

        // Map to store room -> occupied time slots
        val roomOccupancy = mutableMapOf<String, MutableSet<String>>()
        val roomIsLab = mutableMapOf<String, Boolean>()

        // Process all data rows (skip header rows)
        gridData.drop(5).forEachIndexed { relIdx, row ->
            val rowIdx = relIdx + 5
            val isLabSection = labTimeRowIndex > 0 && rowIdx > labTimeRowIndex

            val rowValues = row.values ?: return@forEachIndexed

            // Get room name from first column
            val roomCellValue = SheetsHelper.getFormattedValue(SheetsHelper.getCellAt(rowValues, 0))
            var room = roomCellValue?.trim() ?: ""
            room = TimeParser.cleanRoomData(room)

            // Skip if room is empty, just whitespace, or is a header
            if (room.isEmpty() || room.isBlank() || room.equals("Lab", ignoreCase = true) ||
                room.equals("Room", ignoreCase = true) || room.equals("Rooms", ignoreCase = true)) {
                return@forEachIndexed
            }

            // Initialize room if not exists
            if (!roomOccupancy.containsKey(room)) {
                roomOccupancy[room] = mutableSetOf()
                roomIsLab[room] = isLabSection || room.contains("Lab", ignoreCase = true)
            }

            // Check each column for occupied slots
            rowValues.forEach { cellElement ->
                if (cellElement is ArrayList<*>) {
                    cellElement.forEachIndexed { colIdx, cell ->
                        val cellValue = SheetsHelper.getFormattedValue(cell)?.trim() ?: ""

                        // Cell is occupied if it has non-empty content (not just whitespace)
                        if (cellValue.isNotEmpty() && cellValue.isNotBlank() && colIdx > 0) {
                            // Get time slot for this column
                            val timeSlot = if (isLabSection && labTimeSlotsByColumn.containsKey(colIdx)) {
                                labTimeSlotsByColumn[colIdx]
                            } else {
                                timeSlotsByColumn[colIdx]
                            }

                            if (timeSlot != null) {
                                roomOccupancy[room]?.add(timeSlot)
                            }
                        }
                    }
                }
            }
        }

        // Now calculate free slots for each room
        val allTimeSlots = (timeSlotsByColumn.values + labTimeSlotsByColumn.values)
            .filter { it.isNotEmpty() && it.contains(":") }
            .toSet()
            .sortedBy { parseTimeForSorting(it) }

        Log.d(TAG, "All time slots found: $allTimeSlots")

        roomOccupancy.forEach { (room, occupiedSlots) ->
            val freeSlots = allTimeSlots.filter { it !in occupiedSlots }

            if (freeSlots.isNotEmpty()) {
                freeRooms.add(
                    FreeRoom(
                        roomName = room,
                        freeSlots = freeSlots,
                        isLab = roomIsLab[room] ?: false
                    )
                )
            }
        }

        // Sort rooms: Labs first, then by room name
        return freeRooms.sortedWith(compareBy({ !it.isLab }, { it.roomName }))
    }

    /**
     * Extract time slots from the time row (usually row 4 or 5)
     */
    private fun extractTimeSlots(gridData: List<RowData>): Map<Int, String> {
        val timeSlots = mutableMapOf<Int, String>()

        // Find time row (look for row with time patterns like "8:30" or "10:00")
        for (i in 0 until minOf(10, gridData.size)) {
            val rowValues = gridData[i].values ?: continue
            var foundTimeInRow = false

            rowValues.forEach { cellElement ->
                if (cellElement is ArrayList<*>) {
                    cellElement.forEachIndexed { colIdx, cell ->
                        val value = SheetsHelper.getFormattedValue(cell)?.trim() ?: ""
                        if (value.contains(":") && value.matches(Regex(".*\\d+:\\d+.*"))) {
                            timeSlots[colIdx] = value
                            foundTimeInRow = true
                        }
                    }
                }
            }

            if (foundTimeInRow && timeSlots.size >= 3) {
                Log.d(TAG, "Found time row at index $i with ${timeSlots.size} slots")
                break
            }
        }

        return timeSlots
    }

    /**
     * Extract time slots from a specific row (for lab times)
     */
    private fun extractTimeSlotsFromRow(row: RowData): Map<Int, String> {
        val timeSlots = mutableMapOf<Int, String>()

        row.values?.forEach { cellElement ->
            if (cellElement is ArrayList<*>) {
                cellElement.forEachIndexed { colIdx, cell ->
                    val value = SheetsHelper.getFormattedValue(cell)?.trim() ?: ""
                    if (value.contains(":") && value.matches(Regex(".*\\d+:\\d+.*"))) {
                        timeSlots[colIdx] = value
                    }
                }
            }
        }

        return timeSlots
    }

    /**
     * Parse time string for sorting (convert to minutes from midnight)
     */
    private fun parseTimeForSorting(timeSlot: String): Int {
        val timeRegex = Regex("(\\d{1,2}):(\\d{2})")
        val match = timeRegex.find(timeSlot) ?: return Int.MAX_VALUE

        val hour = match.groupValues[1].toIntOrNull() ?: return Int.MAX_VALUE
        val minute = match.groupValues[2].toIntOrNull() ?: 0

        // Assume times before 8 are PM (like 1:00 = 13:00)
        val adjustedHour = if (hour < 8) hour + 12 else hour

        return adjustedHour * 60 + minute
    }
}

