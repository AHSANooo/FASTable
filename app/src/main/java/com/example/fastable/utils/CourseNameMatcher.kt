package com.example.fastable.utils

import java.util.Locale

/**
 * Offline bidirectional mapping and normalization between official course catalog titles
 * (from Seating Plans) and abbreviated course titles used inside university Google Sheets cells.
 *
 * Ensures all courses fetched from the seating plan stay in the dashboard after refreshing.
 */
object CourseNameMatcher {

    data class CourseMapping(
        val canonicalName: String,
        val code: String,
        val sheetAliases: List<String>
    )

    private val MAPPINGS = listOf(
        CourseMapping(
            canonicalName = "AI Product Development",
            code = "AI4013",
            sheetAliases = listOf("ai prod develop", "ai product dev", "ai product development", "ai4013")
        ),
        CourseMapping(
            canonicalName = "AI-Led Transformation",
            code = "MG4057",
            sheetAliases = listOf("ai-led transformation", "at", "mg4057")
        ),
        CourseMapping(
            canonicalName = "Advanced Analysis of Algorithms",
            code = "CS5005",
            sheetAliases = listOf("advanced analysis of algorithms", "aaa", "cs5005")
        ),
        CourseMapping(
            canonicalName = "Advanced Artificial Intelligence",
            code = "AI5001",
            sheetAliases = listOf("advanced artificial intelligence", "aai", "ai5001")
        ),
        CourseMapping(
            canonicalName = "Advanced Operating Systems",
            code = "CS5009",
            sheetAliases = listOf("advanced operating systems", "aos", "cs5009")
        ),
        CourseMapping(
            canonicalName = "Advanced Quality Assurance",
            code = "SE5002",
            sheetAliases = listOf("advanced quality assurance", "aqa", "se5002")
        ),
        CourseMapping(
            canonicalName = "Advanced Software Architecture",
            code = "SE5003",
            sheetAliases = listOf("advanced software architecture", "asa", "se5003")
        ),
        CourseMapping(
            canonicalName = "Advanced Statistics",
            code = "DS2003",
            sheetAliases = listOf("advanced statistics", "as", "ds2003")
        ),
        CourseMapping(
            canonicalName = "Advanced Topics in Generative AI",
            code = "AI5004",
            sheetAliases = listOf("advanced topics in generative ai", "atga", "ai5004")
        ),
        CourseMapping(
            canonicalName = "Agentic AI",
            code = "CS5020",
            sheetAliases = listOf("adv agentic ai", "agentic ai", "cs5020")
        ),
        CourseMapping(
            canonicalName = "Agentic Artificial Intelligence",
            code = "AI4015",
            sheetAliases = listOf("adv agentic ai", "agentic ai", "agentic artificial intelligence", "ai4015")
        ),
        CourseMapping(
            canonicalName = "Applied Computer Vision",
            code = "CS5031",
            sheetAliases = listOf("applied computer vision", "acv", "cs5031")
        ),
        CourseMapping(
            canonicalName = "Applied Human Computer Interaction",
            code = "CS3014",
            sheetAliases = listOf("applied hci", "applied human computer interaction", "hci", "human computer interaction", "cs3014", "cs2007")
        ),
        CourseMapping(
            canonicalName = "Applied Information Security",
            code = "CY5005",
            sheetAliases = listOf("applied info sec", "applied infosec", "applied information security", "ais", "cy5005")
        ),
        CourseMapping(
            canonicalName = "Applied Physics",
            code = "NS1001",
            sheetAliases = listOf("physics", "phy", "ap", "applied physics", "ns1001")
        ),
        CourseMapping(
            canonicalName = "Applied Programming",
            code = "CS4002",
            sheetAliases = listOf("applied prog", "applied programming", "cs4002")
        ),
        CourseMapping(
            canonicalName = "Artificial Intelligence",
            code = "AI2002",
            sheetAliases = listOf("ai", "artificial intelligence", "ai2002")
        ),
        CourseMapping(
            canonicalName = "Arts Humanities and Technology",
            code = "SS1026",
            sheetAliases = listOf("arts humanities and technology", "aht", "ss1026")
        ),
        CourseMapping(
            canonicalName = "Blockchain Technologies and Applications",
            code = "CY4054",
            sheetAliases = listOf("blockchain", "blockchain technologies and applications", "cy4054")
        ),
        CourseMapping(
            canonicalName = "Blockchain and Cryptocurrency",
            code = "CS4049",
            sheetAliases = listOf("blockchain", "blockchain and cryptocurrency", "cs4049")
        ),
        CourseMapping(
            canonicalName = "Calculus and Analytical Geometry",
            code = "MT1003",
            sheetAliases = listOf("calculus", "calc", "mv calculus", "calculus and analytical geometry", "calculus & analytical geometry", "mt1003")
        ),
        CourseMapping(
            canonicalName = "Cloud Computing",
            code = "CS4075",
            sheetAliases = listOf("cloud comp", "cloud computing", "cs4081", "cs4075")
        ),
        CourseMapping(
            canonicalName = "Computer Architecture",
            code = "EE3009",
            sheetAliases = listOf("comp arch", "computer architecture", "ca", "cs3005", "ee3009")
        ),
        CourseMapping(
            canonicalName = "Computer Networks",
            code = "CS3001",
            sheetAliases = listOf("cn", "networks", "computer networks", "computer network", "cs3001")
        ),
        CourseMapping(
            canonicalName = "Computer Networks Lab",
            code = "CL3001",
            sheetAliases = listOf("cn lab", "computer networks lab", "networks lab", "cl3001")
        ),
        CourseMapping(
            canonicalName = "Computer Organization and Assembly Language",
            code = "EE2003",
            sheetAliases = listOf("coal", "computer organization and assembly language", "computer organization & assembly", "comp arch", "assembly", "assembly language", "ee2003")
        ),
        CourseMapping(
            canonicalName = "Cyber Security",
            code = "CY2004",
            sheetAliases = listOf("cyber sec", "cyber security", "cy2004")
        ),
        CourseMapping(
            canonicalName = "Data Analysis and Visualization",
            code = "DS3001",
            sheetAliases = listOf("data analysis and visualization", "dav", "ds3001")
        ),
        CourseMapping(
            canonicalName = "Data Science Tools and Techniques",
            code = "DS5002",
            sheetAliases = listOf("data science tools and techniques", "dstt", "ds5002")
        ),
        CourseMapping(
            canonicalName = "Data Structures",
            code = "CS2001",
            sheetAliases = listOf("data structures", "ds", "data structure", "cs2001")
        ),
        CourseMapping(
            canonicalName = "Data Structures and Algorithms",
            code = "CS2015",
            sheetAliases = listOf("data structures and algorithms", "dsa", "cs2001", "cs2015")
        ),
        CourseMapping(
            canonicalName = "Data Visualization",
            code = "CS5037",
            sheetAliases = listOf("data vis", "data visualization", "cs5037")
        ),
        CourseMapping(
            canonicalName = "Data Warehousing and Business Intellligence",
            code = "DS3003",
            sheetAliases = listOf("data warehousing and business intellligence", "dwbi", "ds3003")
        ),
        CourseMapping(
            canonicalName = "Database Systems",
            code = "CS2005",
            sheetAliases = listOf("db", "dbs", "database", "database systems", "database system", "cs2005")
        ),
        CourseMapping(
            canonicalName = "Database and Operating Systems",
            code = "CS5145",
            sheetAliases = listOf("database and operating systems", "dos", "cs5145")
        ),
        CourseMapping(
            canonicalName = "Deep Learning",
            code = "DS5006",
            sheetAliases = listOf("dl", "deep learning", "ai3003", "ds5006")
        ),
        CourseMapping(
            canonicalName = "Deep Learning for Perception",
            code = "CS4045",
            sheetAliases = listOf("deep learning for perception", "dlp", "cs4045")
        ),
        CourseMapping(
            canonicalName = "Design and Analysis of Algorithms",
            code = "CS2009",
            sheetAliases = listOf("daa", "algorithms", "algo", "design and analysis of algorithms", "design & analysis of algorithms", "cs2009")
        ),
        CourseMapping(
            canonicalName = "Digital Logic Design",
            code = "EE1005",
            sheetAliases = listOf("dld", "digital logic design", "digital logic", "ee1005")
        ),
        CourseMapping(
            canonicalName = "Digital Logic Design - Lab",
            code = "EL1005",
            sheetAliases = listOf("dld lab", "digital logic design lab", "digital logic design - lab", "el1005")
        ),
        CourseMapping(
            canonicalName = "Digital Sustainability",
            code = "MG4069",
            sheetAliases = listOf("digital sustainability", "ds", "mg4069")
        ),
        CourseMapping(
            canonicalName = "Discrete Structures",
            code = "CS1005",
            sheetAliases = listOf("discrete", "discrete structures", "discrete structure", "cs1005")
        ),
        CourseMapping(
            canonicalName = "Edge Computing and Intelligent Systems",
            code = "AI4019",
            sheetAliases = listOf("edge computing and intelligent systems", "ecis", "ai4019")
        ),
        CourseMapping(
            canonicalName = "Embedded Control of Robotics",
            code = "CS4095",
            sheetAliases = listOf("embedded control of robotics", "ecr", "cs4095")
        ),
        CourseMapping(
            canonicalName = "Empirical Software Engineering",
            code = "SE5032",
            sheetAliases = listOf("empirical software engineering", "ese", "se5032")
        ),
        CourseMapping(
            canonicalName = "Engineering AI based Software",
            code = "SE5008",
            sheetAliases = listOf("engineering ai based software", "eabs", "se5008")
        ),
        CourseMapping(
            canonicalName = "English Composition and Comprehension",
            code = "SS1004",
            sheetAliases = listOf("eng comp", "english comp", "english composition and comprehension", "english composition", "ss1011", "ss1004")
        ),
        CourseMapping(
            canonicalName = "Formal Methods in Software Engineering",
            code = "SE4033",
            sheetAliases = listOf("formal methods in software engineering", "fmse", "se4033")
        ),
        CourseMapping(
            canonicalName = "Functional English",
            code = "SS1012",
            sheetAliases = listOf("func eng", "eng", "english", "functional english", "ss1012")
        ),
        CourseMapping(
            canonicalName = "Fundamentals of Computer Vision",
            code = "CS4059",
            sheetAliases = listOf("fundamentals of computer vision", "fcv", "cs4059")
        ),
        CourseMapping(
            canonicalName = "Fundamentals of Data Visualization",
            code = "CS3012",
            sheetAliases = listOf("fundamentals of data visualization", "fdv", "cs3012")
        ),
        CourseMapping(
            canonicalName = "Fundamentals of Software Project Management",
            code = "CS4044",
            sheetAliases = listOf("fundamentals of software project management", "fspm", "cs4044")
        ),
        CourseMapping(
            canonicalName = "Game Design and Development",
            code = "CS4046",
            sheetAliases = listOf("game design and development", "gdd", "cs4046")
        ),
        CourseMapping(
            canonicalName = "Generative AI",
            code = "AI4009",
            sheetAliases = listOf("generative ai", "ga", "ai4009")
        ),
        CourseMapping(
            canonicalName = "Human Computer Interaction",
            code = "CS2007",
            sheetAliases = listOf("human computer interaction", "hci", "cs2007")
        ),
        CourseMapping(
            canonicalName = "Ideology and Constitution of Pakistan",
            code = "SS1013",
            sheetAliases = listOf("pak studies", "ideology", "ideology and constitution of pakistan", "ideology & constitution of pakistan", "civics", "ss1013")
        ),
        CourseMapping(
            canonicalName = "Information Assurance",
            code = "CY3003",
            sheetAliases = listOf("information assurance", "ia", "cy3003")
        ),
        CourseMapping(
            canonicalName = "Information Security",
            code = "CS3002",
            sheetAliases = listOf("info sec", "infosec", "info security", "is", "information security", "cs3002")
        ),
        CourseMapping(
            canonicalName = "Introduction to Computing Lab",
            code = "CL1000",
            sheetAliases = listOf("intro to comp lab", "intro to computing lab", "introduction to computing lab", "cl1000")
        ),
        CourseMapping(
            canonicalName = "Introduction to Data Science",
            code = "DS2001",
            sheetAliases = listOf("introduction to data science", "ids", "ds2001")
        ),
        CourseMapping(
            canonicalName = "Introduction to Software Engineering",
            code = "SE1001",
            sheetAliases = listOf("se", "software engineering", "introduction to software engineering", "intro to software engineering", "ise", "se1001")
        ),
        CourseMapping(
            canonicalName = "Islamic Studies/Ethics",
            code = "SS1007",
            sheetAliases = listOf("islamic studies/ethics", "is", "ss1007")
        ),
        CourseMapping(
            canonicalName = "Knowledge Representation and Reasoning",
            code = "AI3001",
            sheetAliases = listOf("knowledge representation and reasoning", "krr", "ai3001")
        ),
        CourseMapping(
            canonicalName = "Linear Algebra",
            code = "MT1004",
            sheetAliases = listOf("linear algebra", "la", "lin alg", "mt1004", "mt2005")
        ),
        CourseMapping(
            canonicalName = "MLOps",
            code = "CS4085",
            sheetAliases = listOf("mlops", "machine learning operations", "ml ops", "cs4085")
        ),
        CourseMapping(
            canonicalName = "Machine Learning",
            code = "AI3002",
            sheetAliases = listOf("ml", "machine learning", "ai3002")
        ),
        CourseMapping(
            canonicalName = "Machine Learning - Lab",
            code = "AL3002",
            sheetAliases = listOf("ml lab", "machine learning lab", "machine learning - lab", "al3002")
        ),
        CourseMapping(
            canonicalName = "Machine Learning for Cyber Security",
            code = "CY5002",
            sheetAliases = listOf("machine learning for cyber security", "mlcs", "cy5002")
        ),
        CourseMapping(
            canonicalName = "Mathematical Foundations of AI",
            code = "AI5002",
            sheetAliases = listOf("mathematical foundations of ai", "mfa", "ai5002")
        ),
        CourseMapping(
            canonicalName = "Mathematics for Computational Intelligence",
            code = "CS5144",
            sheetAliases = listOf("mathematics for computational intelligence", "mci", "cs5144")
        ),
        CourseMapping(
            canonicalName = "Methods in Business Research",
            code = "MG3040",
            sheetAliases = listOf("methods in business research", "mbr", "mg3040")
        ),
        CourseMapping(
            canonicalName = "Multiagent Systems and Game Theory",
            code = "AI4012",
            sheetAliases = listOf("multiagent systems and game theory", "msgt", "ai4012")
        ),
        CourseMapping(
            canonicalName = "Natural Language Processing",
            code = "DS5007",
            sheetAliases = listOf("nlp", "natural language processing", "ai3004", "ds5007")
        ),
        CourseMapping(
            canonicalName = "Object Oriented Programming",
            code = "CS1004",
            sheetAliases = listOf("oop", "obj oriented prog", "object oriented programming", "cs1004")
        ),
        CourseMapping(
            canonicalName = "Operating Systems",
            code = "CS2006",
            sheetAliases = listOf("os", "operating systems", "operating system", "cs2006")
        ),
        CourseMapping(
            canonicalName = "Pakistan Studies",
            code = "SS1015",
            sheetAliases = listOf("pak studies", "pakistan studies", "civics", "ss1013", "ss1015")
        ),
        CourseMapping(
            canonicalName = "Parallel and Distributed Computing",
            code = "CS3006",
            sheetAliases = listOf("pdc", "parallel and distributed computing", "parallel & distributed computing", "parallel computing", "cs3006")
        ),
        CourseMapping(
            canonicalName = "Process Mining and Simulation",
            code = "SE4009",
            sheetAliases = listOf("process mining and simulation", "pms", "se4009")
        ),
        CourseMapping(
            canonicalName = "Professional Practices in IT",
            code = "CS4001",
            sheetAliases = listOf("ppit", "ppit.", "professional practices in it", "professional practices", "cs4001")
        ),
        CourseMapping(
            canonicalName = "Programming Fundamentals",
            code = "CS1002",
            sheetAliases = listOf("pf", "prog fund", "programming fundamentals", "programming fundamental", "cs1002")
        ),
        CourseMapping(
            canonicalName = "Programming Fundamentals Lab",
            code = "CL1002",
            sheetAliases = listOf("pf lab", "programming fundamentals lab", "programming lab", "cl1002")
        ),
        CourseMapping(
            canonicalName = "Programming for AI",
            code = "AI2003",
            sheetAliases = listOf("programming for ai", "pa", "ai2003")
        ),
        CourseMapping(
            canonicalName = "Recent Advances in Deep Learning",
            code = "CS6018",
            sheetAliases = listOf("recent advances in deep learning", "radl", "cs6018")
        ),
        CourseMapping(
            canonicalName = "Research Methodology",
            code = "CS5001",
            sheetAliases = listOf("research methodology", "rm", "cs5001")
        ),
        CourseMapping(
            canonicalName = "Secure System Design",
            code = "CY5004",
            sheetAliases = listOf("secure system design", "ssd", "cy5004")
        ),
        CourseMapping(
            canonicalName = "Securing Cloud Computing",
            code = "CY5003",
            sheetAliases = listOf("securing cloud computing", "scc", "cy5003")
        ),
        CourseMapping(
            canonicalName = "Security Operations and Administration",
            code = "CY4048",
            sheetAliases = listOf("security operations and administration", "soa", "cy4048")
        ),
        CourseMapping(
            canonicalName = "Software Construction and Development",
            code = "SE3001",
            sheetAliases = listOf("scd", "software construction and development", "software construction & development", "se3001")
        ),
        CourseMapping(
            canonicalName = "Software Design and Analysis",
            code = "CS3004",
            sheetAliases = listOf("sda", "software design and analysis", "software design & analysis", "software design and architecture", "software design & architecture", "cs3004")
        ),
        CourseMapping(
            canonicalName = "Software Quality Engineering",
            code = "SE3002",
            sheetAliases = listOf("sqe", "software quality engineering", "software quality", "se3002")
        ),
        CourseMapping(
            canonicalName = "Software Re-Engineering",
            code = "SE4001",
            sheetAliases = listOf("software re-engineering", "sr", "se4001")
        ),
        CourseMapping(
            canonicalName = "Software for Mobile Devices",
            code = "CS4039",
            sheetAliases = listOf("software for mobile devices", "smd", "cs4039")
        ),
        CourseMapping(
            canonicalName = "Statistical Modelling",
            code = "MT2002",
            sheetAliases = listOf("stat modeling", "prob & stats", "statistical modelling", "statistical modeling", "mt2002")
        ),
        CourseMapping(
            canonicalName = "Statistical and Mathematical Methods for DS",
            code = "DS5003",
            sheetAliases = listOf("statistical and mathematical methods for ds", "smmd", "ds5003")
        ),
        CourseMapping(
            canonicalName = "Technical and Business Writing",
            code = "SS2012",
            sheetAliases = listOf("tbw", "tech & business writing", "technical and business writing", "technical & business writing", "exp writing", "ss2012", "ss2007")
        ),
        CourseMapping(
            canonicalName = "Understanding of Holy Quran II/Comparative Study of",
            code = "SS5016",
            sheetAliases = listOf("understanding of holy quran ii/comparative study of", "uhqis", "ss5016")
        ),
        CourseMapping(
            canonicalName = "Understanding of Holy Quran II/Introduction to Major",
            code = "SS1022",
            sheetAliases = listOf("understanding of holy quran ii/introduction to major", "uhqim", "ss1022")
        ),
        CourseMapping(
            canonicalName = "Understanding of Holy Quran II/Philosophy of Religions",
            code = "SS6008",
            sheetAliases = listOf("understanding of holy quran ii/philosophy of religions", "uhqir", "ss6008")
        ),
        CourseMapping(
            canonicalName = "Vulnerability Assessment and Reverse Engg",
            code = "CY3002",
            sheetAliases = listOf("vulnerability assessment and reverse engg", "vare", "cy3002")
        ),
        CourseMapping(
            canonicalName = "Web Programming",
            code = "CS4032",
            sheetAliases = listOf("web prog", "web programming", "cs3007", "cs4032")
        ),
    )

    private val canonicalCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    private val canonicalByNameMap by lazy {
        MAPPINGS.associateBy { normalizeText(it.canonicalName) }
    }

    private val exactAliasMap by lazy {
        val map = mutableMapOf<Pair<String, Boolean>, String>()
        for (m in MAPPINGS) {
            val isLab = m.canonicalName.contains("lab", ignoreCase = true)
            for (a in m.sheetAliases) {
                val norm = normalizeText(a)
                if (!map.containsKey(Pair(norm, isLab))) {
                    map[Pair(norm, isLab)] = m.canonicalName
                }
            }
        }
        map
    }

    /**
     * Maps ANY variation, abbreviation, or sheet cell text into the canonical Seating Plan course title.
     * Examples:
     *   "Info Sec" -> "Information Security"
     *   "PPIT" -> "Professional Practices in IT"
     *   "Applied HCI" -> "Applied Human Computer Interaction"
     *   "MLOps" -> "MLOps"
     */
    fun getCanonicalName(rawNameOrAlias: String): String {
        if (rawNameOrAlias.isBlank()) return rawNameOrAlias
        canonicalCache[rawNameOrAlias]?.let { return it }

        val clean = normalizeText(rawNameOrAlias)

        // 1. Direct exact match with canonicalName
        val directMatch = canonicalByNameMap[clean]
        if (directMatch != null) {
            canonicalCache[rawNameOrAlias] = directMatch.canonicalName
            return directMatch.canonicalName
        }

        val targetIsLab = clean.contains("lab")

        // 2. Exact match with sheet aliases
        val exactAliasMatch = exactAliasMap[Pair(clean, targetIsLab)]
        if (exactAliasMatch != null) {
            canonicalCache[rawNameOrAlias] = exactAliasMatch
            return exactAliasMatch
        }

        // 3. Match with longest matching alias contained within clean
        val cleanWords = clean.split(" ").filter { it.isNotBlank() }.toSet()
        val candidateMatches = MAPPINGS.filter { mapping ->
            val mappingIsLab = mapping.canonicalName.contains("lab", ignoreCase = true)
            if (targetIsLab != mappingIsLab) return@filter false

            mapping.sheetAliases.any { alias ->
                val normAlias = normalizeText(alias)
                if (normAlias.length <= 3) {
                    cleanWords.contains(normAlias)
                } else {
                    clean.contains(normAlias)
                }
            }
        }

        if (candidateMatches.isNotEmpty()) {
            val bestMatch = candidateMatches.maxByOrNull { mapping ->
                mapping.sheetAliases.filter { alias ->
                    val normAlias = normalizeText(alias)
                    if (normAlias.length <= 3) cleanWords.contains(normAlias)
                    else clean.contains(normAlias)
                }.maxOfOrNull { it.length } ?: 0
            }
            if (bestMatch != null) {
                canonicalCache[rawNameOrAlias] = bestMatch.canonicalName
                return bestMatch.canonicalName
            }
        }

        // Fallback: return trimmed original (without section parenthesis)
        val fallback = rawNameOrAlias.replace(Regex("\\([^)]*\\)"), "").trim()
        canonicalCache[rawNameOrAlias] = fallback
        return fallback
    }

    /**
     * Determines whether a cell entry from Google Sheets matches the requested course.
     */
    fun matches(cellText: String, courseName: String, courseSection: String = "", courseDept: String = ""): Boolean {
        if (cellText.isBlank() || courseName.isBlank()) return false

        val cellCanonical = getCanonicalName(cellText)
        val targetCanonical = getCanonicalName(courseName)

        // Compare canonical representations
        val isNameMatch = cellCanonical.equals(targetCanonical, ignoreCase = true) ||
                cellText.contains(targetCanonical, ignoreCase = true) ||
                (targetCanonical.contains("Human Computer Interaction") && cellCanonical.contains("Human Computer Interaction"))

        if (!isNameMatch) return false

        // Distinguish Lab vs Theory
        val cellIsLab = cellText.contains("lab", ignoreCase = true) || cellCanonical.contains("lab", ignoreCase = true)
        val targetIsLab = courseName.contains("lab", ignoreCase = true) || targetCanonical.contains("lab", ignoreCase = true)
        if (cellIsLab != targetIsLab) return false

        // Section check if provided
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

        val targetSemester = Regex("(\\d+)").find(courseSection.trim())?.groupValues?.get(1)

        val dept = if (courseDept.isNotBlank()) courseDept.uppercase(Locale.ROOT)
        else Regex("^([A-Za-z]{2})").find(courseSection.trim())?.groupValues?.get(1)?.uppercase(Locale.ROOT) ?: ""

        val upperCell = cellText.uppercase(Locale.ROOT)

        // Strict department isolation: If department is known, reject if cell explicitly specifies a different department
        if (dept.isNotEmpty()) {
            val allDepts = listOf("CS", "AI", "SE", "DS", "CY", "EE")
            val otherDepts = allDepts.filter { it != dept }
            for (other in otherDepts) {
                if (Regex("B?$other-\\d*$secLetter").containsMatchIn(upperCell)) {
                    return false
                }
            }

            // Semester conflict check: If target specifies semester (e.g. 7 from "CS-7B"), reject if cell specifies a different semester (e.g. "CS-5B")
            if (targetSemester != null) {
                val cellSemMatch = Regex("B?$dept-(\\d+)$secLetter").find(upperCell)
                    ?: Regex("\\((\\d+)$secLetter\\)").find(upperCell)
                if (cellSemMatch != null && cellSemMatch.groupValues[1] != targetSemester) {
                    return false
                }
            }

            // Department matches: e.g. CS-E, BCS-7E, BCS-E, CS-7B
            val deptRegex = Regex("(?:\\b|\\()B?$dept-\\d*$secLetter(?:\\b|\\)|,)")
            if (deptRegex.containsMatchIn(upperCell)) {
                return true
            }
        }

        // Check semester mismatch for generic pattern as well
        if (targetSemester != null) {
            val genericSemMatch = Regex("(?:\\b|\\(|,)\\s*(\\d+)$secLetter\\s*(?:\\)|,)").find(upperCell)
            if (genericSemMatch != null && genericSemMatch.groupValues[1] != targetSemester) {
                return false
            }
        }

        // Generic section patterns: (E), (7E), ,E), etc.
        val genericRegex = Regex("(?:\\b|\\(|,)\\s*\\d*$secLetter\\s*(?:\\)|,)")
        if (genericRegex.containsMatchIn(upperCell)) {
            return true
        }

        val exactPattern = "(${courseSection.uppercase(Locale.ROOT)})"
        return upperCell.contains(exactPattern)
    }

    private fun normalizeText(text: String): String {
        return text.lowercase(Locale.ROOT)
            .replace(Regex("\\([^)]*\\)"), " ") // Strip parenthetical sections
            .replace(Regex("[^a-z0-9\\s]"), " ") // Strip punctuation
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
