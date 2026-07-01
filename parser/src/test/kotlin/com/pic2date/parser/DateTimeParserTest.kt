package com.pic2date.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DateTimeParserTest {

    private val today = LocalDate.of(2026, 1, 1)

    @Test
    fun numericSlash() {
        assertEquals(LocalDate.of(2026, 8, 12), DateParser.parse("12/08/2026", today)?.date)
    }

    @Test
    fun numericDot() {
        assertEquals(LocalDate.of(2026, 8, 12), DateParser.parse("12.08.2026", today)?.date)
    }

    @Test
    fun englishMonthFirst() {
        assertEquals(LocalDate.of(2026, 8, 12), DateParser.parse("August 12 2026", today)?.date)
    }

    @Test
    fun englishDayFirstAbbrev() {
        assertEquals(LocalDate.of(2026, 8, 12), DateParser.parse("12 Aug 2026", today)?.date)
    }

    @Test
    fun partialDateResolvesToNearestFuture() {
        // No year given; relative to 2026-01-01 the next 14 Aug is in 2026.
        assertEquals(LocalDate.of(2026, 8, 14), DateParser.parse("Thursday 14.8 at 19:30", today)?.date)
    }

    @Test
    fun americanOrderWhenFirstTokenIsMonth() {
        // 13 cannot be a month, so 08/13 must be Aug 13.
        assertEquals(LocalDate.of(2026, 8, 13), DateParser.parse("08/13/2026", today)?.date)
    }

    @Test
    fun time24h() {
        assertEquals(LocalTime.of(19, 30), TimeParser.parse("19:30")?.time)
        assertEquals(LocalTime.of(9, 15), TimeParser.parse("09:15")?.time)
        assertEquals(LocalTime.of(21, 0), TimeParser.parse("21:00")?.time)
    }

    @Test
    fun time12hMeridiem() {
        assertEquals(LocalTime.of(19, 30), TimeParser.parse("7:30 PM")?.time)
        assertEquals(LocalTime.of(7, 0), TimeParser.parse("7 AM")?.time)
        assertEquals(LocalTime.of(0, 0), TimeParser.parse("12 AM")?.time)
        assertEquals(LocalTime.of(12, 0), TimeParser.parse("12 PM")?.time)
    }

    @Test
    fun timeRange() {
        val m = TimeParser.parse("19:00 - 23:00")
        assertEquals(LocalTime.of(19, 0), m?.time)
        assertEquals(LocalTime.of(23, 0), m?.endTime)
    }

    @Test
    fun noTime() {
        assertNull(TimeParser.parse("no clock here, just 12.08.2026"))
    }
}
