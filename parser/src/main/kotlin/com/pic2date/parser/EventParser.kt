package com.pic2date.parser

import com.pic2date.parser.model.EventType
import com.pic2date.parser.model.ParsedEvent
import java.time.LocalDate

/**
 * Top-level entry point. Turns raw OCR text into a best-effort [ParsedEvent]
 * by combining the date, time, location, contact and type extractors. Fully
 * offline and deterministic.
 *
 * Usage: `EventParser.parse(ocrText)`
 */
object EventParser {

    fun parse(rawText: String, today: LocalDate = LocalDate.now()): ParsedEvent {
        val text = rawText.trim()
        if (text.isEmpty()) return ParsedEvent(rawText = rawText)

        val isHebrew = TextLang.isPrimarilyHebrew(text)
        val dateMatch = DateParser.parse(text, today)
        val timeMatch = TimeParser.parseCombined(text)
        val typeResult = EventTypeClassifier.classify(text)
        val location = LocationExtractor.extract(text)
        val phone = ContactExtractor.extractPhone(text)

        val title = resolveTitle(text, typeResult.type, isHebrew, dateMatch, timeMatch, location)

        val confidence = computeConfidence(dateMatch != null, timeMatch != null, location != null, typeResult.type)

        return ParsedEvent(
            title = title,
            date = dateMatch?.date,
            time = timeMatch?.time,
            endTime = timeMatch?.endTime,
            location = location,
            phone = phone,
            description = null,
            eventType = typeResult.type,
            isHebrew = isHebrew,
            rawText = rawText,
            confidence = confidence,
        )
    }

    private fun resolveTitle(
        text: String,
        type: EventType,
        isHebrew: Boolean,
        dateMatch: DateMatch?,
        timeMatch: TimeMatch?,
        location: String?,
    ): String {
        if (type != EventType.GENERAL) {
            // Prefer the user's own wording when a short line *starts with* the
            // type keyword — "יום הולדת לשני" beats the generic "יום הולדת".
            val keywordLine = text.lines()
                .map { it.trim() }
                .firstOrNull { line ->
                    line.length in 2..60 && type.strongKeywords.any { kw ->
                        kw !in scheduleLabelKeywords &&
                            (
                                line.startsWith(kw, ignoreCase = true) ||
                                    // Allow a single Hebrew prefix letter (ל/ב/ה…).
                                    (TextLang.isHebrewChar(line.first()) && line.drop(1).startsWith(kw))
                                )
                    }
                }
            return keywordLine ?: type.title(isHebrew)
        }

        // For a generic event, use the first meaningful line as the title.
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val firstMeaningful = lines.firstOrNull { line ->
            val isDate = dateMatch != null && line.contains(dateText(text, dateMatch))
            val hasLetters = line.count { it.isLetter() } >= 2
            hasLetters && line != location && !looksLikeOnlyDateTime(line)
        }
        val candidate = firstMeaningful ?: type.title(isHebrew)
        return candidate.take(60).trim()
    }

    /**
     * Classification keywords that are schedule/section labels, not event names —
     * a line starting with one must not become the title ("חופה וקידושין" is the
     * ceremony slot on a wedding invitation, not the event's name).
     */
    private val scheduleLabelKeywords = setOf(
        "חופה", "קבלת פנים", "save the date", "boarding", "departure", "gate",
        "check-in", "check in", "checkin", "check-out", "doors open", "פתיחת דלתות",
    )

    private fun dateText(full: String, match: DateMatch): String =
        full.substring(match.range)

    private val onlyDateTime = Regex("""^[\d\s:./\-–]+$""")

    private fun looksLikeOnlyDateTime(line: String): Boolean = onlyDateTime.matches(line)

    private fun computeConfidence(
        hasDate: Boolean,
        hasTime: Boolean,
        hasLocation: Boolean,
        type: EventType,
    ): Float {
        var c = 0f
        if (hasDate) c += 0.4f
        if (hasTime) c += 0.3f
        if (hasLocation) c += 0.15f
        if (type != EventType.GENERAL) c += 0.15f
        return c.coerceIn(0f, 1f)
    }
}
