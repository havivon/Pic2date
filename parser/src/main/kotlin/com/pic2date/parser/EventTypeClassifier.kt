package com.pic2date.parser

import com.pic2date.parser.model.EventType

/**
 * Classifies text into an [EventType] using weighted keyword scoring across
 * Hebrew and English signals. This approximates semantic understanding: many
 * synonyms per category plus contextual signals (a flight/PNR code, a clinic
 * name) rather than a single keyword.
 */
object EventTypeClassifier {

    private const val STRONG_WEIGHT = 3
    private const val WEAK_WEIGHT = 1

    // Pattern signals that strongly imply a type regardless of vocabulary.
    private val flightNumber = Regex("""\b[A-Z]{2}\s?\d{2,4}\b""")
    private val tableForN = Regex("""(table for|שולחן ל)\s*\d+""", RegexOption.IGNORE_CASE)

    data class Result(val type: EventType, val score: Int)

    fun classify(text: String): Result {
        val lower = text.lowercase()
        var best = EventType.GENERAL
        var bestScore = 0

        for (type in EventType.entries) {
            if (type == EventType.GENERAL) continue
            var score = 0
            type.strongKeywords.forEach { if (lower.contains(it.lowercase())) score += STRONG_WEIGHT }
            type.weakKeywords.forEach { if (lower.contains(it.lowercase())) score += WEAK_WEIGHT }

            // Contextual pattern boosts.
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
        return Result(best, bestScore)
    }
}
