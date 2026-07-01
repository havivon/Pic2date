package com.pic2date.parser

import com.pic2date.parser.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** Cases that mirror messy real-world OCR output (extra spaces, mixed RTL/LTR). */
class OcrRobustnessTest {

    private val today = LocalDate.of(2026, 1, 1)

    @Test
    fun spacedDate() {
        // OCR frequently inserts spaces around separators.
        assertEquals(LocalDate.of(2026, 7, 2), DateParser.parse("2. 7. 26", today)?.date)
    }

    @Test
    fun spacedTime() {
        assertEquals(LocalTime.of(15, 0), TimeParser.parse("15 : 00")?.time)
    }

    @Test
    fun hebrewConcertWithDate() {
        val text = "הופעה\nבהיכל התרבות\n15:00 בשעה , 2.7.26"
        val e = EventParser.parse(text, today)
        assertEquals(EventType.CONCERT, e.eventType)
        assertEquals(LocalDate.of(2026, 7, 2), e.date)
        assertEquals(LocalTime.of(15, 0), e.time)
    }
}
