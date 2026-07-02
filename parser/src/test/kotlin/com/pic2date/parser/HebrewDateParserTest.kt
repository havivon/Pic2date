package com.pic2date.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Calendar conversion vectors were generated with pyluach (months use the
 * sacred numbering: Nisan=1 … Elul=6, Tishrei=7 … Adar=12, Adar II=13).
 */
class HebrewDateParserTest {

    @Test
    fun conversionMatchesPyluach() {
        val vectors = listOf(
            Triple(5786, 6, 5) to LocalDate.of(2026, 8, 18),   // 5 Elul 5786 (the invitation)
            Triple(5786, 7, 1) to LocalDate.of(2025, 9, 23),   // Rosh Hashana 5786
            Triple(5784, 12, 10) to LocalDate.of(2024, 2, 19), // Adar I, leap year
            Triple(5784, 13, 14) to LocalDate.of(2024, 3, 24), // Adar II (Purim), leap year
            Triple(5785, 12, 14) to LocalDate.of(2025, 3, 14), // Adar, regular year
            Triple(5786, 1, 15) to LocalDate.of(2026, 4, 2),   // 15 Nisan (Pesach)
            Triple(5787, 7, 1) to LocalDate.of(2026, 9, 12),   // Rosh Hashana 5787
            Triple(5750, 4, 20) to LocalDate.of(1990, 7, 13),
            Triple(5800, 9, 25) to LocalDate.of(2039, 12, 12),
            Triple(5786, 5, 9) to LocalDate.of(2026, 7, 23),   // 9 Av
            Triple(5789, 12, 1) to LocalDate.of(2029, 2, 16),
            Triple(5787, 11, 2) to LocalDate.of(2027, 1, 10),
        )
        for ((hebrew, expected) in vectors) {
            val (y, m, d) = hebrew
            assertEquals("$d/$m/$y", expected, HebrewDateParser.toGregorian(y, m, d))
        }
    }

    @Test
    fun gematria() {
        assertEquals(5, HebrewDateParser.gematria("ה'"))
        assertEquals(14, HebrewDateParser.gematria("י\"ד"))
        assertEquals(15, HebrewDateParser.gematria("ט\"ו"))
        assertEquals(786, HebrewDateParser.gematria("תשפ\"ו"))
    }

    @Test
    fun invitationPhrase() {
        val m = HebrewDateParser.parse("יום חתונתנו ביום שלישי ה' באלול תשפ\"ו")
        assertEquals(LocalDate.of(2026, 8, 18), m?.date)
    }

    @Test
    fun digitsDayAndGershayimQuotes() {
        // Unicode gershayim, digit day, millennium prefix.
        assertEquals(
            LocalDate.of(2026, 8, 18),
            HebrewDateParser.parse("5 באלול ה\"תשפ״ו")?.date,
        )
        assertEquals(
            LocalDate.of(2026, 4, 2),
            HebrewDateParser.parse("ט״ו בניסן תשפ״ו")?.date,
        )
    }

    @Test
    fun plainTextDoesNotMatch(): Unit {
        assertNull(HebrewDateParser.parse("ניפגש מחר בבוקר בבית הקפה"))
        assertNull(HebrewDateParser.parse("שלום אבי, מה שלומך"))
    }

    @Test
    fun endToEndThroughEventParser() {
        // No Gregorian date at all — only the Hebrew one.
        val e = EventParser.parse(
            "הזמנה לחתונה\nיום שלישי ה' באלול תשפ\"ו\nבשעה 19:30",
            LocalDate.of(2026, 1, 1),
        )
        assertEquals(LocalDate.of(2026, 8, 18), e.date)
    }
}
