package com.pic2date.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Locale-aware display formatting for dates and times. */
object DateTimeFormat {

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    fun date(date: LocalDate?, locale: Locale = Locale.getDefault()): String =
        date?.format(dateFormatter.withLocale(locale)) ?: ""

    fun time(time: LocalTime?): String =
        time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""
}
