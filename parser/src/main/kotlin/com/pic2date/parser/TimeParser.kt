package com.pic2date.parser

import java.time.LocalTime

/** A time (optionally a start–end range) found in text, with its range. */
data class TimeMatch(
    val time: LocalTime,
    val endTime: LocalTime? = null,
    val range: IntRange,
)

/**
 * Extracts a time from free text. Supports 24-hour ("19:30", "09:15"),
 * 12-hour with meridiem ("7:30 PM", "7 pm"), explicit ranges ("19:00-23:00")
 * and Hebrew lead-ins ("בשעה 19:30", "ב-21:00") which simply precede the digits.
 */
object TimeParser {

    // Range first so "19:00-23:00" is captured whole.
    private val range = Regex("""\b(\d{1,2}):(\d{2})\s*[-–—]\s*(\d{1,2}):(\d{2})\b""")

    // 19:30 / 7:30 pm / 09:15
    private val colon = Regex(
        """\b(\d{1,2}):(\d{2})\s*(a\.?m\.?|p\.?m\.?)?""",
        RegexOption.IGNORE_CASE,
    )

    // bare meridiem like "7 PM" / "11am" (require am/pm to avoid matching stray numbers)
    private val meridiem = Regex(
        """\b(\d{1,2})\s*(a\.?m\.?|p\.?m\.?)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(text: String): TimeMatch? {
        range.find(text)?.let { m ->
            val start = toTime(m.groupValues[1].toInt(), m.groupValues[2].toInt(), null)
            val end = toTime(m.groupValues[3].toInt(), m.groupValues[4].toInt(), null)
            if (start != null && end != null) return TimeMatch(start, end, m.range)
        }

        colon.find(text)?.let { m ->
            val t = toTime(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3])
            if (t != null) return TimeMatch(t, null, m.range)
        }

        meridiem.find(text)?.let { m ->
            val t = toTime(m.groupValues[1].toInt(), 0, m.groupValues[2])
            if (t != null) return TimeMatch(t, null, m.range)
        }
        return null
    }

    private fun toTime(hourIn: Int, minute: Int, meridiemRaw: String?): LocalTime? {
        if (minute !in 0..59) return null
        var hour = hourIn
        val mer = meridiemRaw?.lowercase()?.replace(".", "")
        when (mer) {
            "pm" -> if (hour in 1..11) hour += 12
            "am" -> if (hour == 12) hour = 0
        }
        if (hour !in 0..23) return null
        return LocalTime.of(hour, minute)
    }
}
