package com.pic2date.parser

import com.pic2date.parser.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** Mirrors a real Hebrew wedding invitation used in manual testing. */
class WeddingInvitationTest {

    private val today = LocalDate.of(2026, 1, 1)

    @Test
    fun pipeSeparatedDate() {
        assertEquals(LocalDate.of(2026, 8, 18), DateParser.parse("18 | 08 | 26", today)?.date)
    }

    @Test
    fun twoTimesBecomeStartAndEnd() {
        val m = TimeParser.parseCombined("חופה וקידושין\n20:30\nקבלת פנים\n19:30")
        assertEquals(LocalTime.of(19, 30), m?.time)
        assertEquals(LocalTime.of(20, 30), m?.endTime)
    }

    @Test
    fun fullInvitation() {
        val text = """
            בס"ד
            יעל מאור
            18 | 08 | 26
            שמחים ונרגשים להזמין אתכם לחגוג עימנו את
            יום חתונתנו ביום שלישי ה' באלול תשפ"ו
            חופה וקידושין
            20:30
            קבלת פנים
            19:30
            גן אירועים "אלכסנדר"
            המסיק 2 עמק חפר
            נשמח לראותכם
        """.trimIndent()

        val e = EventParser.parse(text, today)

        assertEquals(EventType.WEDDING, e.eventType)
        assertEquals("חתונה", e.title)
        assertEquals(LocalDate.of(2026, 8, 18), e.date)
        assertEquals(LocalTime.of(19, 30), e.time)
        assertEquals(LocalTime.of(20, 30), e.endTime)
        assertNotNull(e.location)
        assertTrue(e.location!!.contains("אירועים"))
    }
}
