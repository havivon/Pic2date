package com.pic2date.parser

import com.pic2date.parser.model.EventType

/**
 * Classifies text into an [EventType] using weighted keyword scoring across
 * Hebrew and English signals. This approximates semantic understanding: many
 * synonyms per category plus contextual signals (a flight/PNR code, a clinic
 * name) rather than a single keyword.
 *
 * Keywords are matched on word boundaries (not raw substrings) so short words
 * like "live" do not match inside "olive". Hebrew one/two-letter prefixes
 * (ל, ב, ה, ו, …) are allowed, so "להופעה" still matches "הופעה". A minimum
 * score is required before leaving [EventType.GENERAL], to avoid confident-
 * looking detections from a single incidental word.
 */
object EventTypeClassifier {

    private const val STRONG_WEIGHT = 3
    private const val WEAK_WEIGHT = 1

    /** Below this, we stay GENERAL rather than claim a specific type. */
    private const val MIN_SCORE = STRONG_WEIGHT

    // Pattern signals that strongly imply a type regardless of vocabulary.
    private val flightNumber = Regex("""\b[A-Z]{2}\s?\d{2,4}\b""")
    private val tableForN = Regex("""(table for|שולחן ל)\s*\d+""", RegexOption.IGNORE_CASE)

    // Cache of boundary-aware matchers, one per keyword.
    private val keywordMatchers = HashMap<String, Regex>()

    private fun matcher(keyword: String): Regex = keywordMatchers.getOrPut(keyword) {
        val escaped = Regex.escape(keyword.lowercase())
        // No letter/digit immediately before (allowing up to two Hebrew prefix
        // letters), and no letter/digit immediately after.
        Regex("""(?<![\p{L}\p{N}])[א-ת]{0,2}$escaped(?![\p{L}\p{N}])""")
    }

    private fun contains(text: String, keyword: String): Boolean =
        matcher(keyword).containsMatchIn(text)

    data class Result(val type: EventType, val score: Int)

    fun classify(text: String): Result {
        val lower = text.lowercase()
        var best = EventType.GENERAL
        var bestScore = 0

        for (type in EventType.entries) {
            if (type == EventType.GENERAL) continue
            var score = 0
            type.strongKeywords.forEach { if (contains(lower, it)) score += STRONG_WEIGHT }
            type.weakKeywords.forEach { if (contains(lower, it)) score += WEAK_WEIGHT }

            when (type) {
                EventType.FLIGHT -> if (flightNumber.containsMatchIn(text)) score += STRONG_WEIGHT
                EventType.RESTAURANT_RESERVATION -> if (tableForN.containsMatchIn(text)) score += STRONG_WEIGHT
                else -> Unit
            }

            if (score > bestScore) {
                bestScore = score
                best = type
            }
        }

        return if (bestScore >= MIN_SCORE) Result(best, bestScore) else Result(EventType.GENERAL, 0)
    }
}
