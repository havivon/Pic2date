package com.pic2date.parser

import java.time.LocalDate

/**
 * Parses Hebrew-calendar dates written in Hebrew — e.g. "ה' באלול תשפ"ו" —
 * and converts them to Gregorian [LocalDate]s. Israeli invitations very often
 * carry the Hebrew date in a clearer font than the stylized Gregorian one, so
 * this is a reliable second source for the event date.
 *
 * The arithmetic is the standard Dershowitz–Reingold Hebrew calendar
 * computation; the epoch constant is calibrated against known dates and the
 * conversion is cross-checked against pyluach in unit tests.
 */
object HebrewDateParser {

    // ---- Calendar arithmetic -------------------------------------------------

    /** epochDay(1 Tishrei of year y) = elapsedDays(y) + EPOCH_EPOCHDAY. */
    private const val EPOCH_EPOCHDAY = -2092591L

    fun isLeapYear(year: Int): Boolean = (7 * year + 1) % 19 < 7

    /** Days from the Hebrew epoch to Rosh Hashana of [year], with postponements. */
    private fun elapsedDays(year: Int): Long {
        val prev = (year - 1).toLong()
        val months = 235L * (prev / 19) + 12L * (prev % 19) + (7L * (prev % 19) + 1) / 19
        val parts = 204L + 793L * (months % 1080)
        val hours = 5L + 12L * months + 793L * (months / 1080) + parts / 1080
        var day = 1L + 29L * months + hours / 24
        val p = 1080L * (hours % 24) + parts % 1080
        if (p >= 19440L ||
            (day % 7 == 2L && p >= 9924L && !isLeapYear(year)) ||
            (day % 7 == 1L && p >= 16789L && isLeapYear(year - 1))
        ) {
            day += 1
        }
        if (day % 7 == 0L || day % 7 == 3L || day % 7 == 5L) day += 1
        return day
    }

    private fun yearLength(year: Int): Int = (elapsedDays(year + 1) - elapsedDays(year)).toInt()

    /**
     * Month numbering follows the sacred convention (Nisan = 1 … Elul = 6,
     * Tishrei = 7 … Adar = 12, Adar II = 13 in leap years), matching pyluach.
     * Returns the civil-order list (from Tishrei) of (monthNumber, length).
     */
    private fun civilMonths(year: Int): List<Pair<Int, Int>> {
        val length = yearLength(year)
        val longHeshvan = length % 10 == 5
        val shortKislev = length % 10 == 3
        val leap = isLeapYear(year)
        return buildList {
            add(7 to 30)                                  // Tishrei
            add(8 to if (longHeshvan) 30 else 29)         // Heshvan
            add(9 to if (shortKislev) 29 else 30)         // Kislev
            add(10 to 29)                                 // Tevet
            add(11 to 30)                                 // Shevat
            if (leap) {
                add(12 to 30)                             // Adar I
                add(13 to 29)                             // Adar II
            } else {
                add(12 to 29)                             // Adar
            }
            add(1 to 30)                                  // Nisan
            add(2 to 29)                                  // Iyar
            add(3 to 30)                                  // Sivan
            add(4 to 29)                                  // Tammuz
            add(5 to 30)                                  // Av
            add(6 to 29)                                  // Elul
        }
    }

    /** Converts a Hebrew date to Gregorian. Returns null for invalid input. */
    fun toGregorian(year: Int, month: Int, day: Int): LocalDate? {
        if (year !in 4000..7000 || day !in 1..30) return null
        val months = civilMonths(year)
        var offset = 0
        for ((m, len) in months) {
            if (m == month) {
                if (day > len) return null
                return LocalDate.ofEpochDay(EPOCH_EPOCHDAY + elapsedDays(year) + offset + day - 1)
            }
            offset += len
        }
        return null
    }

    // ---- Gematria ------------------------------------------------------------

    private val letterValues = mapOf(
        'א' to 1, 'ב' to 2, 'ג' to 3, 'ד' to 4, 'ה' to 5, 'ו' to 6, 'ז' to 7,
        'ח' to 8, 'ט' to 9, 'י' to 10, 'כ' to 20, 'ך' to 20, 'ל' to 30,
        'מ' to 40, 'ם' to 40, 'נ' to 50, 'ן' to 50, 'ס' to 60, 'ע' to 70,
        'פ' to 80, 'ף' to 80, 'צ' to 90, 'ץ' to 90, 'ק' to 100, 'ר' to 200,
        'ש' to 300, 'ת' to 400,
    )

    /** Sums letter values, ignoring geresh/gershayim punctuation. */
    fun gematria(raw: String): Int {
        var sum = 0
        for (c in raw) {
            val v = letterValues[c] ?: continue
            sum += v
        }
        return sum
    }

    // ---- Text parsing ----------------------------------------------------------

    private const val QUOTES = "\"'׳״"

    private val monthNumbers = listOf(
        "תשרי" to 7, "מרחשוון" to 8, "מרחשון" to 8, "חשוון" to 8, "חשון" to 8,
        "כסלו" to 9, "טבת" to 10, "שבט" to 11,
        "אדר א" to 12, "אדר ב" to 13, "אדר" to 12,
        "ניסן" to 1, "אייר" to 2, "סיוון" to 3, "סיון" to 3, "תמוז" to 4,
        "מנחם אב" to 5, "אב" to 5, "אלול" to 6,
    )

    private val monthAlt = monthNumbers.joinToString("|") { Regex.escape(it.first) }

    // day (digits or gematria) + optional ב prefix + month + year in gematria.
    private val pattern = Regex(
        """(?<![א-ת0-9])(\d{1,2}|[א-ת][$QUOTES]?[א-ת]?[$QUOTES]?)\s+ב?($monthAlt)(?:['׳]?)\s+([א-ת][$QUOTES]?[א-ת]{1,4}[$QUOTES]?[א-ת]?)(?![א-ת])""",
    )

    /** Finds the first Hebrew-calendar date in [text]; null when absent/invalid. */
    fun parse(text: String): DateMatch? {
        for (m in pattern.findAll(text)) {
            val dayToken = m.groupValues[1]
            val day = dayToken.toIntOrNull() ?: gematria(dayToken)
            if (day !in 1..30) continue

            var month = monthNumbers.first { it.first == m.groupValues[2] }.second

            var yearToken = m.groupValues[3]
            // Millennium prefix like ה'תשפ"ו — strip the leading ה geresh.
            if (yearToken.length > 2 && yearToken[0] == 'ה' && yearToken[1] in QUOTES) {
                yearToken = yearToken.substring(2)
            }
            var year = gematria(yearToken)
            if (year < 1000) year += 5000
            if (year !in 5500..6000) continue

            // Plain "אדר" in a leap year conventionally refers to Adar II.
            if (month == 12 && m.groupValues[2] == "אדר" && isLeapYear(year)) month = 13

            val date = toGregorian(year, month, day) ?: continue
            return DateMatch(date, m.range, hadExplicitYear = true)
        }
        return null
    }
}
