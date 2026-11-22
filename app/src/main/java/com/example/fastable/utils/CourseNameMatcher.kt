package com.example.fastable.utils

import java.util.Locale

/**
 * Intelligent bidirectional matcher between formal course catalog titles (from Seating Plans)
 * and abbreviated course titles used inside university Google Sheets cells.
 *
 * Example Matches:
 *  - "Professional Practices in IT" <-> "PPIT (CS-E)"
 *  - "Applied Human Computer Interaction" <-> "Applied HCI (CS-E)"
 *  - "Information Security" <-> "Info Sec (CS-E)"
 *  - "MLOps" <-> "MLOps (CS-B)"
 *  - "Computer Organization and Assembly Language" <-> "COAL (CS-A)"
 */
object CourseNameMatcher {

    private val ALIAS_GROUPS = listOf(
        listOf("applied human computer interaction", "applied hci", "human computer interaction", "hci", "cs3014", "cs2007"),
        listOf("professional practices in it", "professional practices", "ppit", "cs4001"),
        listOf("information security", "info sec", "infosec", "is", "cs3002"),
        listOf("mlops", "machine learning operations", "cs4085"),
        listOf("data structures", "ds", "cs2001"),
        listOf("data structures lab", "ds lab", "cl2001"),
        listOf("linear algebra", "la", "mt1004", "mt2005"),
        listOf("computer organization and assembly language", "computer organization & assembly", "coal", "assembly language", "assembly", "ee2003"),
        listOf("assembly language lab", "coal lab", "assembly lab", "el2003"),
        listOf("discrete structures", "discrete", "cs1005"),
        listOf("operating systems", "os", "cs2006"),
        listOf("operating systems lab", "os lab", "cl2006"),
        listOf("database systems", "db", "dbs", "database", "cs2005"),
        listOf("database systems lab", "db lab", "cl2005"),
        listOf("computer networks", "cn", "networks", "cs3001"),
        listOf("computer networks lab", "cn lab", "cl3001"),
        listOf("parallel and distributed computing", "parallel & distributed computing", "pdc", "cs3006"),
        listOf("design and analysis of algorithms", "design & analysis of algorithms", "daa", "algorithms", "algo", "cs2009"),
        listOf("programming fundamentals", "pf", "prog fund", "cs1002"),
        listOf("programming fundamentals lab", "pf lab", "cl1002"),
        listOf("object oriented programming", "oop", "obj oriented prog", "cs1004"),
        listOf("object oriented programming lab", "oop lab", "cl1004"),
        listOf("technical and business writing", "technical & business writing", "tbw", "tech & business writing", "tech and business writing", "ss2012", "ss2007"),
        listOf("calculus and analytical geometry", "calculus & analytical geometry", "calculus", "calc", "mt1003"),
        listOf("digital logic design", "dld", "ee1005"),
        listOf("digital logic design lab", "dld lab", "el1005"),
        listOf("artificial intelligence", "ai", "ai2002"),
        listOf("machine learning", "ml", "ai3002"),
        listOf("machine learning lab", "ml lab", "al3002"),
        listOf("cyber security", "cyber sec", "cy2004"),
        listOf("software construction and development", "software construction & development", "scd", "se3001"),
        listOf("software quality engineering", "sqe", "se3002"),
        listOf("functional english", "func eng", "eng", "english", "ss1012"),
        listOf("ideology and constitution of pakistan", "ideology & constitution of pakistan", "pak studies", "ideology", "ss1013"),
        listOf("applied physics", "physics", "phy", "ns1001")
    )

    /**
     * Determines whether a cell entry from Google Sheets matches the requested course.
     */
    fun matches(cellText: String, courseName: String, courseSection: String = "", courseDept: String = ""): Boolean {
        if (cellText.isBlank() || courseName.isBlank()) return false

        val cellClean = normalizeText(cellText)
        val targetClean = normalizeText(courseName)

        // 1. Direct equality or substring containment
        val isNameMatch = when {
            cellClean == targetClean -> true
            cellClean.contains(targetClean) || targetClean.contains(cellClean) -> true
            else -> {
                // 2. Check alias groups
                ALIAS_GROUPS.any { group ->
                    val targetInGroup = group.any { alias -> targetClean.contains(alias) || alias.contains(targetClean) }
                    if (targetInGroup) {
                        group.any { alias ->
                            // Word boundary matching for short acronyms like "ds", "os", "cn" to prevent false positives
                            if (alias.length <= 3) {
                                Regex("\\b${Regex.escape(alias)}\\b").containsMatchIn(cellClean)
                            } else {
                                cellClean.contains(alias)
                            }
                        }
                    } else false
                }
            }
        }

        if (!isNameMatch) return false

        // Distinguish Lab vs Theory
        val cellIsLab = cellClean.contains("lab")
        val targetIsLab = targetClean.contains("lab")
        if (cellIsLab != targetIsLab) return false

        // 3. Check section if provided
        if (courseSection.isNotBlank()) {
            return matchesSection(cellText, courseSection, courseDept)
        }

        return true
    }

    /**
     * Matches section identifiers (e.g. "CS-E", "BCS-7E", "E") against cell text.
     */
    fun matchesSection(cellText: String, courseSection: String, courseDept: String = ""): Boolean {
        if (courseSection.isBlank()) return true

        // Extract section letter (e.g. "E" from "BCS-7E" or "CS-E")
        val secLetter = Regex("([A-Za-z])$").find(courseSection.trim())?.groupValues?.get(1)?.uppercase(Locale.ROOT)
            ?: courseSection.trim().uppercase(Locale.ROOT)

        val dept = if (courseDept.isNotBlank()) courseDept.uppercase(Locale.ROOT)
                   else Regex("^([A-Za-z]{2})").find(courseSection.trim())?.groupValues?.get(1)?.uppercase(Locale.ROOT) ?: ""

        val patterns = mutableListOf(
            "($dept-$secLetter)",
            "(${secLetter})",
            "-$secLetter)",
            "-$secLetter ",
            "-$secLetter,",
            " $secLetter)",
            ",$secLetter)",
            "(${courseSection.uppercase(Locale.ROOT)})"
        )
        if (dept.isNotEmpty()) {
            patterns.add("($dept-$secLetter")
            patterns.add("$dept-$secLetter")
        }

        val upperCell = cellText.uppercase(Locale.ROOT)
        return patterns.any { upperCell.contains(it) }
    }

    private fun normalizeText(text: String): String {
        return text.lowercase(Locale.ROOT)
            .replace(Regex("\\([^)]*\\)"), " ") // Strip parenthetical sections
            .replace(Regex("[^a-z0-9\\s]"), " ") // Strip punctuation
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
