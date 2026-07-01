package com.pic2date.parser

import com.pic2date.parser.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Guards against confident-looking detections that were never really in the text. */
class FalsePositiveTest {

    private val today = LocalDate.of(2026, 1, 1)

    @Test
    fun substringsDoNotFalselyClassify() {
        // "live"(olive), "show"(shower), "tour"(detour), "band"(bandana) appear only
        // as substrings -> must stay GENERAL, not Concert.
        val e = EventParser.parse("Olive shower in the detour with a bandana", today)
        assertEquals(EventType.GENERAL, e.eventType)
    }

    @Test
    fun realConcertStillDetected() {
        val e = EventParser.parse("Live concert tonight", today)
        assertEquals(EventType.CONCERT, e.eventType)
    }

    @Test
    fun hebrewPrefixKeywordDetected() {
        // "להופעה" (to the concert) should still classify as concert.
        val e = EventParser.parse("כרטיס להופעה", today)
        assertEquals(EventType.CONCERT, e.eventType)
    }
}
