package com.pic2date.parser

import com.pic2date.parser.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Mirrors a notes-app screenshot: the app's own timestamp header ("0 sec. ago
 * 7/2/26 23:25") must not be mistaken for the event's date or end time, and the
 * title should keep the user's full wording.
 */
class NoteScreenshotTest {

    private val today = LocalDate.of(2026, 7, 2)

    private val ocr = """
        0 sec. ago 7/2/26 23:25
        5.7.26

        יום הולדת לשני

        20:30
    """.trimIndent()

    @Test
    fun timestampHeaderDoesNotWin() {
        val e = EventParser.parse(ocr, today)
        assertEquals(LocalDate.of(2026, 7, 5), e.date)
        assertEquals(LocalTime.of(20, 30), e.time)
        // 23:25 belongs to the header timestamp — must not become the end time.
        assertNull(e.endTime)
    }

    @Test
    fun titleKeepsFullUserWording() {
        val e = EventParser.parse(ocr, today)
        assertEquals(EventType.BIRTHDAY, e.eventType)
        assertEquals("יום הולדת לשני", e.title)
    }

    @Test
    fun canonicalTitleWhenKeywordIsMidSentence() {
        // The keyword line here does not start with a keyword and is long —
        // fall back to the canonical type title.
        val e = EventParser.parse(
            "הזמנה לחתונה של דוד ושרה. יום חמישי 14.8.2026 בשעה 19:30. גן אירועים הגן הקסום, תל אביב",
            today,
        )
        assertEquals("חתונה", e.title)
    }
}
