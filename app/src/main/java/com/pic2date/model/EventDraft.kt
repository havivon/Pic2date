package com.pic2date.model

import com.pic2date.parser.model.EventType
import com.pic2date.parser.model.ParsedEvent
import java.time.LocalDate
import java.time.LocalTime

/** Mutable, user-editable representation of an event shown on the confirm screen. */
data class EventDraft(
    val title: String = "",
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val endTime: LocalTime? = null,
    val location: String = "",
    val notes: String = "",
    /** Minutes before the event for a reminder; null means no reminder. */
    val reminderMinutes: Int? = 60,
    val phone: String? = null,
    val eventType: EventType = EventType.GENERAL,
    val rawText: String = "",
) {
    val hasLocation: Boolean get() = location.isNotBlank()

    companion object {
        fun from(parsed: ParsedEvent): EventDraft = EventDraft(
            title = parsed.title.orEmpty(),
            date = parsed.date,
            time = parsed.time,
            endTime = parsed.endTime,
            location = parsed.location.orEmpty(),
            notes = "",
            reminderMinutes = 60,
            phone = parsed.phone,
            eventType = parsed.eventType,
            rawText = parsed.rawText,
        )
    }
}
