package com.pic2date.parser.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Result of analysing OCR text. All fields are nullable because a real-world
 * image may be missing any of them; the UI presents these as editable defaults.
 */
data class ParsedEvent(
    val title: String? = null,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    /** Optional explicit end time (e.g. "19:00 - 23:00"). */
    val endTime: LocalTime? = null,
    val location: String? = null,
    val phone: String? = null,
    val description: String? = null,
    val eventType: EventType = EventType.GENERAL,
    /** Whether the source text was detected as primarily Hebrew. */
    val isHebrew: Boolean = false,
    /** Raw OCR text the result was derived from. */
    val rawText: String = "",
    /** 0f..1f rough confidence based on how many key fields were found. */
    val confidence: Float = 0f,
) {
    val hasDate: Boolean get() = date != null
    val hasTime: Boolean get() = time != null
    val hasLocation: Boolean get() = !location.isNullOrBlank()
}
