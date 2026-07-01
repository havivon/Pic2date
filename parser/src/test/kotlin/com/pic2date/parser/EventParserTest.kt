package com.pic2date.parser

import com.pic2date.parser.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class EventParserTest {

    private val today = LocalDate.of(2026, 1, 1)

    @Test
    fun weddingExampleFromSpec() {
        val text = "Wedding invitation. David and Sarah invite you. " +
            "Thursday August 14th 2026 at 19:30. Jerusalem Convention Center."
        val e = EventParser.parse(text, today)

        assertEquals(EventType.WEDDING, e.eventType)
        assertEquals("Wedding", e.title)
        assertEquals(LocalDate.of(2026, 8, 14), e.date)
        assertEquals(LocalTime.of(19, 30), e.time)
        assertNotNull(e.location)
        assertTrue(e.location!!.contains("Jerusalem Convention Center"))
    }

    @Test
    fun doctorExampleFromSpec() {
        val text = "Doctor appointment on 07/09/2026 at 10:00. Herzl 25 Tel Aviv."
        val e = EventParser.parse(text, today)

        assertEquals(EventType.DOCTOR_APPOINTMENT, e.eventType)
        assertEquals("Doctor Appointment", e.title)
        assertEquals(LocalDate.of(2026, 9, 7), e.date)
        assertEquals(LocalTime.of(10, 0), e.time)
        assertEquals("Herzl 25 Tel Aviv", e.location)
    }

    @Test
    fun hebrewDateAndTime() {
        val text = "יום חמישי 14.8 בשעה 19:30"
        val e = EventParser.parse(text, today)

        assertTrue(e.isHebrew)
        assertEquals(LocalDate.of(2026, 8, 14), e.date)
        assertEquals(LocalTime.of(19, 30), e.time)
    }

    @Test
    fun hebrewWedding() {
        val text = "הזמנה לחתונה של דוד ושרה. יום חמישי 14.8.2026 בשעה 19:30. גן אירועים הגן הקסום, תל אביב"
        val e = EventParser.parse(text, today)

        assertEquals(EventType.WEDDING, e.eventType)
        assertEquals("חתונה", e.title)
        assertEquals(LocalDate.of(2026, 8, 14), e.date)
        assertEquals(LocalTime.of(19, 30), e.time)
        assertNotNull(e.location)
    }
}
