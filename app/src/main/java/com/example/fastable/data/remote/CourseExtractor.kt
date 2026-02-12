package com.example.fastable.data.remote

import android.util.Log
import com.example.fastable.data.models.Course
import com.example.fastable.utils.TimeParser
import com.example.fastable.utils.SheetsHelper
import com.google.api.services.sheets.v4.model.Spreadsheet

object CourseExtractor {

    private const val TAG = "CourseExtractor"

    /**
     * Extract all courses from spreadsheet
     */
    fun extractAllCourses(spreadsheet: Spreadsheet): List<Course> {
        Log.d(TAG, "=== extractAllCourses START ===")
        val courses = mutableListOf<Course>()
        val dayKeywords = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

        // Extract batch colors first
        val batchColors = TimetableExtractor.extractBatchColors(spreadsheet)
        Log.d(TAG, "Batch colors extracted: ${batchColors.size} colors found")
        batchColors.forEach { (color, batch) ->
            Log.d(TAG, "  Color: $color -> Batch: $batch")
        }

        Log.d(TAG, "Total sheets in spreadsheet: ${spreadsheet.sheets?.size}")
        spreadsheet.sheets?.forEach { sheet ->
            val sheetTitle = sheet.properties?.title
            Log.d(TAG, "Sheet found: $sheetTitle")
        }

        spreadsheet.sheets?.forEach { sheet ->
            val sheetName = sheet.properties?.title ?: return@forEach
            // Use partial matching - sheet name must contain one of the day keywords
            val matchedDay = dayKeywords.firstOrNull { day -> sheetName.contains(day, ignoreCase = true) }
            if (matchedDay == null) {
                Log.d(TAG, "Skipping non-timetable sheet: $sheetName")
                return@forEach
            }

            // Use the normalized day name
            val normalizedDayName = matchedDay

            Log.d(TAG, "Processing timetable sheet: $sheetName (normalized: $normalizedDayName)")

            val gridData = sheet.data?.getOrNull(0)?.rowData
            if (gridData == null) {
                Log.w(TAG, "No grid data for sheet: $sheetName")
                return@forEach
            }

            Log.d(TAG, "Grid has ${gridData.size} rows")

            // Process rows starting from row 5
            gridData.drop(5).forEach { row ->
                val rowValues = row.values ?: return@forEach

                // rowValues is a collection with 1 ArrayList containing all cells
                rowValues.forEach { cellElement ->
                    if (cellElement is ArrayList<*>) {
                        // Iterate through ALL cells in the ArrayList
                        cellElement.forEach { cell ->
                            val cellColor = SheetsHelper.getBackgroundColor(cell)

                            // Check if this cell has a course
                            if (cellColor in batchColors && cellColor.isNotEmpty()) {
                                val courseEntry = SheetsHelper.getFormattedValue(cell) ?: ""

                                if (courseEntry.isNotEmpty()) {
                                    val batch = batchColors[cellColor] ?: ""
                                    val courseInfo = parseCourseEntry(courseEntry, batch)

                                    if (courseInfo != null) {
                                        val updatedCourse = courseInfo.copy(
                                            colorCode = cellColor,
                                            day = normalizedDayName
                                        )

                                        // Check if course already exists
                                        if (!courses.any { it.getCourseKey() == updatedCourse.getCourseKey() }) {
                                            courses.add(updatedCourse)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Fallback: treat as single cell
                        val cellColor = SheetsHelper.getBackgroundColor(cellElement)

                        if (cellColor in batchColors && cellColor.isNotEmpty()) {
                            val courseEntry = SheetsHelper.getFormattedValue(cellElement) ?: ""

                            if (courseEntry.isNotEmpty()) {
                                val batch = batchColors[cellColor] ?: ""
                                val courseInfo = parseCourseEntry(courseEntry, batch)

                                if (courseInfo != null) {
                                    val updatedCourse = courseInfo.copy(
                                        colorCode = cellColor,
                                        day = normalizedDayName
                                    )

                                    if (!courses.any { it.getCourseKey() == updatedCourse.getCourseKey() }) {
                                        courses.add(updatedCourse)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return courses.sortedWith(compareBy({ it.name }, { it.department }, { it.section }))
    }

    /**
     * Parse course entry to extract information
     */
    private fun parseCourseEntry(courseEntry: String, batch: String): Course? {
        if (courseEntry.isEmpty()) return null

        // Extract department from batch
        val department = TimeParser.extractDepartmentFromBatch(batch)

        // Extract section from course entry
        var section = ""
        var courseName = courseEntry

        // Handle group patterns like "(CS-A,G-1)"
        val groupWithSectionPattern = Regex("\\($department-([A-Z]),\\s*G-\\d+\\)")
        val groupSectionMatch = groupWithSectionPattern.find(courseEntry)

        if (groupSectionMatch != null) {
            section = groupSectionMatch.groupValues[1]
            courseName = courseEntry.replace(Regex("$department-[A-Z],"), "$department,")
        } else {
            // Standard section patterns
            val sectionPatterns = listOf(
                Regex("\\($department-([A-Z])\\)"),
                Regex("-([A-Z])\\b"),
                Regex("\\(([A-Z])\\)"),
                Regex("\\s([A-Z])\\s")
            )

            for (pattern in sectionPatterns) {
                val match = pattern.find(courseEntry)
                if (match != null) {
                    section = match.groupValues[1]
                    courseName = courseEntry.replace(pattern, "").trim()
                    break
                }
            }
        }

        // Clean up course name
        courseName = courseName.replace("()", "").trim()
        if (courseName.endsWith("-")) {
            courseName = courseName.dropLast(1).trim()
        }

        // Parse embedded time and remove it from name
        val (cleanName, _, _) = TimeParser.parseEmbeddedTime(courseName)
        courseName = cleanName

        return Course(
            name = courseName,
            department = department,
            section = section,
            batch = batch,
            colorCode = "",
            fullEntry = courseEntry,
            day = "",
            isSelected = false
        )
    }

    /**
     * Search courses with filters
     */
    fun searchCourses(
        courses: List<Course>,
        query: String = "",
        department: String = "",
        batch: String = ""
    ): List<Course> {
        var filtered = courses

        // Filter by department
        if (department.isNotEmpty()) {
            filtered = filtered.filter { it.department == department }
        }

        // Filter by batch
        if (batch.isNotEmpty()) {
            filtered = filtered.filter { it.batch == batch }
        }

        // Filter by search query
        if (query.isNotEmpty()) {
            val queryLower = query.lowercase()
            filtered = filtered.filter {
                queryLower in it.name.lowercase() ||
                queryLower in it.department.lowercase() ||
                queryLower in it.section.lowercase()
            }
        }

        return filtered.sortedWith(compareBy({ it.name.lowercase() }, { it.department }, { it.section }))
    }

    /**
     * Get unique departments
     */
    fun getDepartments(courses: List<Course>): List<String> {
        return courses.map { it.department }.distinct().sorted()
    }

    /**
     * Get unique batches
     */
    fun getBatches(courses: List<Course>): List<String> {
        return courses.map { it.batch }.distinct().sorted()
    }
}

