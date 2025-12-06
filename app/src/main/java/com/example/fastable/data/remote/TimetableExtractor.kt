package com.example.fastable.data.remote

import android.util.Log
import com.example.fastable.data.models.TimetableSession
import com.example.fastable.data.models.Course
import com.example.fastable.utils.TimeParser
import com.example.fastable.utils.SheetsHelper
import com.google.api.services.sheets.v4.model.*

object TimetableExtractor {

    private const val TAG = "TimetableExtractor"

    fun extractBatchColors(spreadsheet: Spreadsheet): Map<String, String> {
        val batchColors = mutableMapOf<String, String>()
        val timetableSheets = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

        spreadsheet.sheets?.forEach { sheet ->
            val sheetName = sheet.properties?.title ?: return@forEach
            if (sheetName !in timetableSheets) return@forEach

            val gridData = sheet.data?.getOrNull(0)?.rowData ?: return@forEach

            for (rowIdx in 0 until minOf(4, gridData.size)) {
                val rowData = gridData[rowIdx].values ?: continue
                val cellList = rowData.toList()

                cellList.forEach { cellElement ->
                    if (cellElement is ArrayList<*>) {
                        cellElement.forEach { cell ->
                            val value = SheetsHelper.getFormattedValue(cell)
                            val cellColor = SheetsHelper.getBackgroundColor(cell)

                            if (value != null && value.contains("BS", ignoreCase = true)) {
                                if (cellColor.isNotEmpty() && cellColor != "1.001.001.00") {
                                    batchColors[cellColor] = value.trim()
                                }
                            }
                        }
                    } else {
                        val value = SheetsHelper.getFormattedValue(cellElement)
                        val cellColor = SheetsHelper.getBackgroundColor(cellElement)

                        if (value != null && value.contains("BS", ignoreCase = true)) {
                            if (cellColor.isNotEmpty() && cellColor != "1.001.001.00") {
                                batchColors[cellColor] = value.trim()
                            }
                        }
                    }
                }
            }
        }

        Log.d(TAG, "Extracted ${batchColors.size} batches")
        return batchColors
    }

    private fun findRoomColumn(gridData: List<RowData>): Int {
        val roomKeywords = listOf("room", "rooms", "room no", "room number", "location", "venue")

        for (rowIdx in 0 until minOf(10, gridData.size)) {
            val rowValues = gridData[rowIdx].values ?: continue
            rowValues.forEachIndexed { colIdx, cell ->
                val cellValue = SheetsHelper.getFormattedValue(cell)?.lowercase() ?: ""
                if (roomKeywords.any { it in cellValue }) {
                    return colIdx
                }
            }
        }

        return 0
    }

    private fun buildTimeColRank(gridData: List<RowData>): Pair<RowData?, Map<Int, Int>> {
        var timeRow: RowData? = null
        var startCol = 0

        for (i in 0 until minOf(10, gridData.size)) {
            val rowValues = gridData[i].values ?: continue
            if (rowValues.isNotEmpty()) {
                val firstCellValue = SheetsHelper.getFormattedValue(SheetsHelper.getCellAt(rowValues, 0))
                val firstCell = firstCellValue?.lowercase() ?: ""
                if ("room" in firstCell) {
                    timeRow = gridData[i]
                    startCol = 1
                    break
                }
            }
        }

        if (timeRow == null && gridData.size > 4) {
            timeRow = gridData[4]
            startCol = 0
        }

        val colRank = mutableMapOf<Int, Int>()
        timeRow?.values?.forEach { cellElement ->
            if (cellElement is ArrayList<*>) {
                // Iterate through ALL cells in the ArrayList
                cellElement.forEachIndexed { colIdx, cell ->
                    val formattedVal = SheetsHelper.getFormattedValue(cell)
                    if (colIdx >= startCol && !formattedVal.isNullOrBlank()) {
                        colRank[colIdx] = colRank.size
                    }
                }
            } else {
                // Fallback
                timeRow.values?.toList()?.forEachIndexed { colIdx, cell ->
                    val formattedVal = SheetsHelper.getFormattedValue(cell)
                    if (colIdx >= startCol && !formattedVal.isNullOrBlank()) {
                        colRank[colIdx] = colRank.size
                    }
                }
            }
        }

        return Pair(timeRow, colRank)
    }

    fun getBatchTimetable(
        spreadsheet: Spreadsheet,
        userBatch: String,
        userSection: String
    ): List<TimetableSession> {
        val batchColors = extractBatchColors(spreadsheet)

        // Normalize batch name - remove parentheses and spaces for matching
        val normalizeBatch = { batch: String ->
            batch.replace("(", "").replace(")", "").replace(" ", "").uppercase()
        }

        val normalizedUserBatch = normalizeBatch(userBatch)

        val targetColor = batchColors.entries.firstOrNull {
            val normalized = normalizeBatch(it.value)
            normalized == normalizedUserBatch
        }?.key

        if (targetColor == null) {
            Log.e(TAG, "No matching color found for batch: $userBatch")
            batchColors.forEach { (_, batch) ->
                Log.e(TAG, "     - $batch (normalized: ${normalizeBatch(batch)})")
            }
            return emptyList()
        }

        val sessions = mutableListOf<TimetableSession>()
        val timetableSheets = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

        spreadsheet.sheets?.forEach { sheet ->
            val sheetName = sheet.properties?.title ?: return@forEach
            if (sheetName !in timetableSheets) return@forEach

            val gridData = sheet.data?.getOrNull(0)?.rowData ?: return@forEach
            if (gridData.size < 6) return@forEach


            val roomColumn = 0
            val (timeRow, colRank) = buildTimeColRank(gridData)
            Log.d(TAG, "    Time columns: ${colRank.size}")

            var labTimeRow: RowData? = null
            var labTimeRowIndex: Int? = null

            gridData.forEachIndexed { idx, row ->
                val values = row.values
                val firstCellValue = SheetsHelper.getFormattedValue(SheetsHelper.getCellAt(values, 0))
                if (firstCellValue != null && firstCellValue.equals("Lab", ignoreCase = true)) {
                    labTimeRow = row
                    labTimeRowIndex = idx
                    Log.d(TAG, "    Lab row found at index: $idx")
                }
            }

            var cellsChecked = 0
            var cellsMatched = 0
            var sessionsFound = 0

            gridData.drop(5).forEachIndexed { relIdx, row ->
                val rowIdx = relIdx + 6
                val isLab = labTimeRowIndex != null && rowIdx >= labTimeRowIndex + 1

                val rowValues = row.values ?: return@forEachIndexed
                val roomCellValue = SheetsHelper.getFormattedValue(SheetsHelper.getCellAt(rowValues, roomColumn))
                var room = roomCellValue ?: "Unknown"
                room = TimeParser.cleanRoomData(room)

                // rowValues is a collection with 1 ArrayList containing all cells
                rowValues.forEach { cellElement ->
                    if (cellElement is ArrayList<*>) {
                        // Iterate through ALL cells in the ArrayList
                        cellElement.forEachIndexed { colIdx, cell ->
                            cellsChecked++
                            val cellColor = SheetsHelper.getBackgroundColor(cell)

                            if (cellColor == targetColor) {
                                cellsMatched++
                                val classEntry = SheetsHelper.getFormattedValue(cell) ?: ""

                                if (classEntry.isNotEmpty()) {
                                    val dept = TimeParser.extractDepartmentFromBatch(userBatch)
                                    val sectionPatterns = listOf(
                                        if (dept.isNotEmpty()) "($dept-$userSection)" else null,
                                        "-$userSection)",
                                        "-$userSection ",
                                        "-$userSection,",
                                        "($userSection)",
                                        " $userSection)",
                                        " $userSection,",
                                        " $userSection "
                                    ).filterNotNull()

                                    val sectionMatch = sectionPatterns.any { pattern ->
                                        pattern in classEntry
                                    }

                                    if (sectionMatch) {
                                        sessionsFound++
                                        // ...existing time/course extraction logic...
                                        val (cleanEntry, embeddedTime, hasEmbeddedTime) = TimeParser.parseEmbeddedTime(classEntry)

                                        val timeSlot = if (hasEmbeddedTime) {
                                            embeddedTime
                                        } else if (isLab && labTimeRow != null) {
                                            // For labs, get time from the Lab row itself
                                            val labTimeValues = labTimeRow.values
                                            if (labTimeValues != null) {
                                                val labTimeList = labTimeValues.toList()
                                                if (labTimeList.isNotEmpty() && labTimeList[0] is ArrayList<*>) {
                                                    val labTimeCellArray = labTimeList[0] as ArrayList<*>

                                                    // Collect all time values from merged cells
                                                    val timeValues = mutableListOf<String>()
                                                    for (i in colIdx until minOf(colIdx + 10, labTimeCellArray.size)) {
                                                        val timeValue = SheetsHelper.getFormattedValue(labTimeCellArray[i])
                                                        if (timeValue != null && timeValue.isNotEmpty() &&
                                                            timeValue.contains(":")) {
                                                            timeValues.add(timeValue)
                                                        } else if (timeValues.isNotEmpty()) {
                                                            // Stop when we hit empty or non-time cell
                                                            break
                                                        }
                                                    }

                                                    // Build time range
                                                    if (timeValues.size >= 2) {
                                                        "${timeValues.first()}-${timeValues.last()}"
                                                    } else if (timeValues.isNotEmpty()) {
                                                        timeValues.first()
                                                    } else {
                                                        "Unknown"
                                                    }
                                                } else {
                                                    SheetsHelper.getFormattedValue(SheetsHelper.getCellAt(labTimeValues, colIdx)) ?: "Unknown"
                                                }
                                            } else {
                                                "Unknown"
                                            }
                                        } else {
                                            // For regular classes, use the time row
                                            val timeValues = timeRow?.values
                                            val timeCell = if (timeValues != null) {
                                                val timeList = timeValues.toList()
                                                if (timeList.isNotEmpty() && timeList[0] is ArrayList<*>) {
                                                    val timeCellArray = timeList[0] as ArrayList<*>
                                                    if (colIdx < timeCellArray.size) {
                                                        SheetsHelper.getFormattedValue(timeCellArray[colIdx])
                                                    } else null
                                                } else {
                                                    SheetsHelper.getFormattedValue(SheetsHelper.getCellAt(timeValues, colIdx))
                                                }
                                            } else null
                                            timeCell ?: "Unknown"
                                        }

                                        var courseName = if (hasEmbeddedTime) cleanEntry else classEntry
                                        sectionPatterns.forEach { pattern ->
                                            courseName = courseName.replace(pattern, "").trim()
                                        }
                                        courseName = courseName.replace("()", "").trim()
                                        if (courseName.endsWith("-")) {
                                            courseName = courseName.dropLast(1).trim()
                                        }

                                        val rank = colRank[colIdx] ?: 999

                                        // Determine session type: check if course name contains "Lab" or if it's in lab section
                                        val sessionType = if (isLab || courseName.contains("Lab", ignoreCase = true)) "Lab" else "Class"

                                        sessions.add(
                                            TimetableSession(
                                                day = sheetName,
                                                timeSlot = timeSlot,
                                                room = room,
                                                sessionType = sessionType,
                                                courseName = courseName,
                                                section = userSection,
                                                batch = userBatch,
                                                department = dept,
                                                rank = rank,
                                                colorCode = cellColor,
                                                isCustom = false
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (sessions.isEmpty()) {
            Log.w(TAG, "❌ NO SESSIONS FOUND!")
            Log.w(TAG, "   This could mean:")
            Log.w(TAG, "   1. Section '$userSection' doesn't exist for batch '$userBatch'")
            Log.w(TAG, "   2. The section format in the spreadsheet is different")
            Log.w(TAG, "   3. The color mapping is incorrect")
        }

        Log.d(TAG, "=== getBatchTimetable END: Found ${sessions.size} sessions ===")
        return sessions.sortedWith(compareBy({ it.day }, { it.rank }, { it.getStartTimeMillis() }))
    }

    fun getCustomTimetable(
        spreadsheet: Spreadsheet,
        selectedCourses: List<Course>
    ): List<TimetableSession> {
        if (selectedCourses.isEmpty()) return emptyList()

        val sessions = mutableListOf<TimetableSession>()
        val batchColors = extractBatchColors(spreadsheet)
        val timetableSheets = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

        spreadsheet.sheets?.forEach { sheet ->
            val sheetName = sheet.properties?.title ?: return@forEach
            if (sheetName !in timetableSheets) return@forEach

            val gridData = sheet.data?.getOrNull(0)?.rowData ?: return@forEach
            if (gridData.size < 6) return@forEach

            val (timeRow, colRank) = buildTimeColRank(gridData)

            var labTimeRow: RowData? = null
            var labTimeRowIndex: Int? = null

            gridData.forEachIndexed { idx, row ->
                val values = row.values
                val firstCellValue = SheetsHelper.getFormattedValue(SheetsHelper.getCellAt(values, 0))
                if (firstCellValue != null && firstCellValue.equals("Lab", ignoreCase = true)) {
                    labTimeRow = row
                    labTimeRowIndex = idx
                }
            }

            gridData.drop(5).forEachIndexed { relIdx, row ->
                val rowIdx = relIdx + 6
                val isLab = labTimeRowIndex != null && rowIdx >= labTimeRowIndex + 1

                val rowValues = row.values ?: return@forEachIndexed
                val roomCellValue = SheetsHelper.getFormattedValue(SheetsHelper.getCellAt(rowValues, 0))
                var room = roomCellValue ?: "Unknown"
                room = TimeParser.cleanRoomData(room)

                // rowValues is a collection with 1 ArrayList containing all cells
                rowValues.forEach { cellElement ->
                    if (cellElement is ArrayList<*>) {
                        // Iterate through ALL cells in the ArrayList
                        cellElement.forEachIndexed { colIdx, cell ->
                            val classEntry = SheetsHelper.getFormattedValue(cell) ?: ""
                            if (classEntry.isEmpty()) return@forEachIndexed

                            val cellColor = SheetsHelper.getBackgroundColor(cell)

                            selectedCourses.forEach { selectedCourse ->
                                if (matchesSelectedCourse(classEntry, selectedCourse, cellColor, batchColors)) {
                                    val (cleanEntry, embeddedTime, hasEmbeddedTime) = TimeParser.parseEmbeddedTime(classEntry)

                                    val timeSlot = if (hasEmbeddedTime) {
                                        embeddedTime
                                    } else if (isLab && labTimeRow != null) {
                                        // For labs, get time from the Lab row itself
                                        val labTimeValues = labTimeRow.values
                                        if (labTimeValues != null) {
                                            val labTimeList = labTimeValues.toList()
                                            if (labTimeList.isNotEmpty() && labTimeList[0] is ArrayList<*>) {
                                                val labTimeCellArray = labTimeList[0] as ArrayList<*>

                                                // Collect all time values from merged cells
                                                val timeValues = mutableListOf<String>()
                                                for (i in colIdx until minOf(colIdx + 10, labTimeCellArray.size)) {
                                                    val timeValue = SheetsHelper.getFormattedValue(labTimeCellArray[i])
                                                    if (timeValue != null && timeValue.isNotEmpty() &&
                                                        timeValue.contains(":")) {
                                                        timeValues.add(timeValue)
                                                    } else if (timeValues.isNotEmpty()) {
                                                        // Stop when we hit empty or non-time cell
                                                        break
                                                    }
                                                }

                                                // Build time range
                                                if (timeValues.size >= 2) {
                                                    "${timeValues.first()}-${timeValues.last()}"
                                                } else if (timeValues.isNotEmpty()) {
                                                    timeValues.first()
                                                } else {
                                                    "Unknown"
                                                }
                                            } else {
                                                SheetsHelper.getFormattedValue(SheetsHelper.getCellAt(labTimeValues, colIdx)) ?: "Unknown"
                                            }
                                        } else {
                                            "Unknown"
                                        }
                                    } else {
                                        // For regular classes, use the time row
                                        val timeValues = timeRow?.values
                                        val timeCell = if (timeValues != null) {
                                            val timeList = timeValues.toList()
                                            if (timeList.isNotEmpty() && timeList[0] is ArrayList<*>) {
                                                val timeCellArray = timeList[0] as ArrayList<*>
                                                if (colIdx < timeCellArray.size) {
                                                    SheetsHelper.getFormattedValue(timeCellArray[colIdx])
                                                } else null
                                            } else {
                                                SheetsHelper.getFormattedValue(SheetsHelper.getCellAt(timeValues, colIdx))
                                            }
                                        } else null
                                        timeCell ?: "Unknown"
                                    }

                                    val courseName = if (hasEmbeddedTime) cleanEntry else selectedCourse.name
                                    val rank = colRank[colIdx] ?: 999

                                    // Determine session type: check if course name contains "Lab" or if it's in lab section
                                    val sessionType = if (isLab || courseName.contains("Lab", ignoreCase = true)) "Lab" else "Class"

                                    val session = TimetableSession(
                                        day = sheetName,
                                        timeSlot = timeSlot,
                                        room = room,
                                        sessionType = sessionType,
                                        courseName = courseName,
                                        section = selectedCourse.section,
                                        batch = selectedCourse.batch,
                                        department = selectedCourse.department,
                                        rank = rank,
                                        colorCode = cellColor,
                                        isCustom = true
                                    )

                                    if (!sessions.any { isSimilarSession(it, session) }) {
                                        sessions.add(session)
                                    }
                                }
                            }
                        }
                    } else {
                        // Fallback for non-ArrayList cells
                        rowValues.toList().forEachIndexed { colIdx, cell ->
                            val classEntry = SheetsHelper.getFormattedValue(cell) ?: ""
                            if (classEntry.isEmpty()) return@forEachIndexed

                            val cellColor = SheetsHelper.getBackgroundColor(cell)

                            selectedCourses.forEach { selectedCourse ->
                                if (matchesSelectedCourse(classEntry, selectedCourse, cellColor, batchColors)) {
                                    val (cleanEntry, embeddedTime, hasEmbeddedTime) = TimeParser.parseEmbeddedTime(classEntry)

                                    val timeSlot = if (hasEmbeddedTime) {
                                        embeddedTime
                                    } else if (isLab && labTimeRow != null) {
                                        // For labs, get time from the Lab row itself
                                        val labTimeValues = labTimeRow.values
                                        val timeCell = SheetsHelper.getFormattedValue(SheetsHelper.getCellAt(labTimeValues, colIdx))
                                        timeCell ?: "Unknown"
                                    } else {
                                        // For regular classes
                                        val timeValues = timeRow?.values
                                        val timeCell = SheetsHelper.getFormattedValue(SheetsHelper.getCellAt(timeValues, colIdx))
                                        timeCell ?: "Unknown"
                                    }

                                    val courseName = if (hasEmbeddedTime) cleanEntry else selectedCourse.name
                                    val rank = colRank[colIdx] ?: 999

                                    // Determine session type: check if course name contains "Lab" or if it's in lab section
                                    val sessionType = if (isLab || courseName.contains("Lab", ignoreCase = true)) "Lab" else "Class"

                                    val session = TimetableSession(
                                        day = sheetName,
                                        timeSlot = timeSlot,
                                        room = room,
                                        sessionType = sessionType,
                                        courseName = courseName,
                                        section = selectedCourse.section,
                                        batch = selectedCourse.batch,
                                        department = selectedCourse.department,
                                        rank = rank,
                                        colorCode = cellColor,
                                        isCustom = true
                                    )

                                    if (!sessions.any { isSimilarSession(it, session) }) {
                                        sessions.add(session)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return sessions.sortedWith(compareBy({ it.day }, { it.rank }, { it.getStartTimeMillis() }))
    }

    private fun matchesSelectedCourse(
        classEntry: String,
        selectedCourse: Course,
        cellColor: String,
        batchColors: Map<String, String>
    ): Boolean {
        val (cleanEntry, _, hasEmbeddedTime) = TimeParser.parseEmbeddedTime(classEntry)
        val entryToMatch = if (hasEmbeddedTime) cleanEntry else classEntry

        if (selectedCourse.name.lowercase() !in entryToMatch.lowercase()) {
            return false
        }

        if ("lab" !in selectedCourse.name.lowercase() && "lab" in entryToMatch.lowercase()) {
            return false
        }

        val dept = selectedCourse.department
        val section = selectedCourse.section
        val sectionPatterns = listOf(
            if (dept.isNotEmpty()) "($dept-$section)" else null,
            "-$section)",
            "-$section ",
            "($section)",
            " $section)"
        ).filterNotNull()

        if (!sectionPatterns.any { it in classEntry }) {
            return false
        }

        val courseBatch = selectedCourse.batch
        val expectedColor = batchColors.entries.firstOrNull { it.value == courseBatch }?.key
        if (expectedColor != null && cellColor != expectedColor) {
            return false
        }

        return true
    }

    private fun isSimilarSession(session1: TimetableSession, session2: TimetableSession): Boolean {
        return session1.day == session2.day &&
                session1.timeSlot == session2.timeSlot &&
                session1.courseName.lowercase() == session2.courseName.lowercase() &&
                session1.section == session2.section
    }
}
