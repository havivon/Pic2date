package com.pic2date.parser

import java.time.DateTimeException
import java.time.LocalDate

/** A date found in text, together with the character range it occupied. */
data class DateMatch(
    val date: LocalDate,
    val range: IntRange,
    val hadExplicitYear: Boolean,
)

/**
 * Extracts a calendar date from free OCR text. Supports numeric forms
 * (dd/mm/yyyy, dd.mm.yyyy, dd-mm-yy, partial dd.mm) and month-name forms in
 * both English ("August 12 2026", "12 Aug 2026") and Hebrew
 * ("12 באוגוסט 2026"). When the year is missing it resolves to the nearest
 * upcoming occurrence relative to [today].
 */
object DateParser {

    private val englishMonths: Map<String, Int> = buildMap {
        val full = listOf(
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december",
        )
        full.forEachIndexed { i, name ->
            put(name, i + 1)
            put(name.substring(0, 3), i + 1) // jan, feb, ...
        }
        put("sept", 9)
    }

    private val hebrewMonths: Map<String, Int> = mapOf(
        "ינואר" to 1, "פברואר" to 2, "מרץ" to 3, "מרס" to 3, "אפריל" to 4,
        "מאי" to 5, "יוני" to 6, "יולי" to 7, "אוגוסט" to 8, "ספטמבר" to 9,
        "אוקטובר" to 10, "נובמבר" to 11, "דצמבר" to 12,
    )

    private val enMonthAlt = englishMonths.keys.sortedByDescending { it.length }.joinToString("|")
    private val heMonthAlt = hebrewMonths.keys.sortedByDescending { it.length }.joinToString("|")

    // "August 12 2026" / "Aug 12th, 2026" / "August 12"
    private val monthFirstEn = Regex(
        """\b($enMonthAlt)\.?\s+(\d{1,2})(?:st|nd|rd|th)?(?:[,\s]+(\d{4}))?\b""",
        RegexOption.IGNORE_CASE,
    )

    // "12 August 2026" / "12th Aug" / "12 Aug, 2026"
    private val dayFirstEn = Regex(
        """\b(\d{1,2})(?:st|nd|rd|th)?\s+(?:of\s+)?($enMonthAlt)\.?(?:[,\s]+(\d{4}))?\b""",
        RegexOption.IGNORE_CASE,
    )

    // "12 באוגוסט 2026" / "12 אוגוסט" / "12 לאוגוסט"
    private val dayFirstHe = Regex(
        """(\d{1,2})\s+(?:ב|ל)?($heMonthAlt)(?:\s+(\d{4}))?""",
    )

    // Numeric: 12/08/2026, 12.08.26, 12-8, 14.8, and stylized "18 | 08 | 26"
    // (invitations often use pipes). Tolerates small spaces that OCR often
    // inserts around separators ("2. 7. 26").
    private val numeric = Regex(
        """\b(\d{1,2})\s{0,3}[./\-|]\s{0,3}(\d{1,2})(?:\s{0,3}[./\-|]\s{0,3}(\d{2,4}))?\b""",
    )

    fun parse(text: String, today: LocalDate = LocalDate.now()): DateMatch? {
        val candidates = mutableListOf<DateMatch>()

        monthFirstEn.findAll(text).forEach { m ->
            val month = englishMonths[m.groupValues[1].lowercase()] ?: return@forEach
            val day = m.groupValues[2].toInt()
            val year = m.groupValues[3].toIntOrNull()
            build(day, month, year, m.range, today)?.let { candidates += it }
        }
        dayFirstEn.findAll(text).forEach { m ->
            val day = m.groupValues[1].toInt()
            val month = englishMonths[m.groupValues[2].lowercase()] ?: return@forEach
            val year = m.groupValues[3].toIntOrNull()
            build(day, month, year, m.range, today)?.let { candidates += it }
        }
        dayFirstHe.findAll(text).forEach { m ->
            val day = m.groupValues[1].toInt()
            val month = hebrewMonths[m.groupValues[2]] ?: return@forEach
            val year = m.groupValues[3].toIntOrNull()
            build(day, month, year, m.range, today)?.let { candidates += it }
        }
        numeric.findAll(text).forEach { m ->
            val a = m.groupValues[1].toInt()
            val b = m.groupValues[2].toInt()
            val (day, month) = decideDayMonth(a, b) ?: return@forEach
            val year = m.groupValues[3].toIntOrNull()?.let { if (it < 100) 2000 + it else it }
            build(day, month, year, m.range, today)?.let { candidates += it }
        }
        // Hebrew-calendar dates ("ה' באלול תשפ"ו") carry an explicit year.
        HebrewDateParser.parse(text)?.let { candidates += it }

        if (candidates.isEmpty()) return null
        // Prefer the candidate that appears earliest in the text; among ties
        // (overlapping matches) prefer the one carrying an explicit year.
        return candidates
            .sortedWith(compareBy({ it.range.first }, { !it.hadExplicitYear }))
            .first()
    }

    private fun build(
        day: Int,
        month: Int,
        year: Int?,
        range: IntRange,
        today: LocalDate,
    ): DateMatch? {
        if (month !in 1..12 || day !in 1..31) return null
        return try {
            if (year != null) {
                DateMatch(LocalDate.of(year, month, day), range, hadExplicitYear = true)
            } else {
                var d = LocalDate.of(today.year, month, day)
                if (d.isBefore(today)) d = LocalDate.of(today.year + 1, month, day)
                DateMatch(d, range, hadExplicitYear = false)
            }
        } catch (e: DateTimeException) {
            null
        }
    }

    /** Resolve ambiguous "a/b" into (day, month), defaulting to day-first. */
    private fun decideDayMonth(a: Int, b: Int): Pair<Int, Int>? = when {
        a > 31 || b > 31 -> null
        a > 12 && b <= 12 -> a to b          // a must be the day
        b > 12 && a <= 12 -> b to a          // mm/dd form
        else -> a to b                       // ambiguous -> day-first
    }
}
