package com.pic2date.parser

/**
 * Heuristically extracts a venue or address line. It first honours explicit
 * labels ("Location:", "כתובת:"), then falls back to scoring each line for
 * venue / street / city signals in Hebrew and English.
 */
object LocationExtractor {

    private val venueKeywords = listOf(
        "convention center", "convention centre", "conference center", "center", "centre",
        "hall", "hotel", "garden", "gardens", "stadium", "arena", "theater", "theatre",
        "club", "auditorium", "ballroom", "venue", "campus", "pavilion",
        "אולם", "אולמי", "גן אירועים", "גני אירועים", "מרכז", "היכל", "מלון",
        "אצטדיון", "מועדון", "תיאטרון", "אודיטוריום", "קמפוס", "בית",
    )

    private val streetWords = listOf(
        "street", "st.", "road", "rd.", "ave", "avenue", "blvd", "boulevard", "lane", "drive",
        "רחוב", "רח'", "שדרות", "שד'", "דרך", "סמטת",
    )

    private val cities = listOf(
        "tel aviv", "jerusalem", "haifa", "beer sheva", "be'er sheva", "rishon", "netanya",
        "ashdod", "ramat gan", "herzliya", "raanana", "kfar saba", "holon", "bat yam",
        "modiin", "eilat", "tiberias", "nazareth", "ashkelon", "rehovot", "petah tikva",
        "תל אביב", "תל-אביב", "ירושלים", "חיפה", "באר שבע", "ראשון לציון", "נתניה",
        "אשדוד", "רמת גן", "הרצליה", "רעננה", "כפר סבא", "חולון", "בת ים", "מודיעין",
        "אילת", "טבריה", "נצרת", "אשקלון", "רחובות", "פתח תקווה", "גבעתיים", "כרמיאל",
    )

    private val labels = Regex(
        """^\s*(location|venue|address|where|place|כתובת|מיקום|מקום|כתובת:|איפה)\s*[:\-–]?\s*""",
        RegexOption.IGNORE_CASE,
    )

    /** Lines starting with these are almost never an address. */
    private val nonLocationPrefixes = Regex(
        """^\s*(date|time|when|tel|phone|טלפון|תאריך|שעה)\b""",
        RegexOption.IGNORE_CASE,
    )

    // Split on line breaks and on sentence punctuation followed by whitespace.
    // The lookbehind keeps dotted dates ("12.08.2026") intact because they have
    // no whitespace after the dot.
    private val segmenter = Regex("""[\n\r]+|(?<=[.!?|•])\s+""")

    fun extract(text: String): String? {
        val lines = text.split(segmenter).map { it.trim() }.filter { it.isNotBlank() }

        // 1. Explicit label wins.
        for (line in lines) {
            val m = labels.find(line)
            if (m != null) {
                val rest = line.substring(m.range.last + 1).trim()
                if (rest.isNotBlank()) return rest
            }
        }

        // 2. Score remaining lines.
        var best: String? = null
        var bestScore = 0
        for (line in lines) {
            if (nonLocationPrefixes.containsMatchIn(line)) continue
            val score = scoreLine(line)
            if (score > bestScore) {
                bestScore = score
                best = line
            }
        }
        return if (bestScore >= 2) clean(best!!) else null
    }

    private fun scoreLine(line: String): Int {
        val lower = line.lowercase()
        var score = 0
        if (venueKeywords.any { lower.contains(it) }) score += 3
        if (cities.any { lower.contains(it) }) score += 2
        if (streetWords.any { lower.contains(it) }) score += 2
        // A street/venue line usually carries a house number.
        if (score > 0 && line.any { it.isDigit() }) score += 1
        // Very long lines are usually prose, not an address.
        if (line.length > 70) score -= 1
        return score
    }

    private fun clean(line: String): String =
        line.replace(labels, "")
            .replace(Regex("""^\s*(at|in|ב)\s+""", RegexOption.IGNORE_CASE), "")
            .trim()
            .trimEnd('.', ',', ';')
}
