package com.pic2date.model

import androidx.annotation.StringRes
import com.pic2date.R

/** Preset reminder choices offered on the confirm screen. */
enum class ReminderOption(val minutes: Int?, @StringRes val labelRes: Int) {
    NONE(null, R.string.reminder_none),
    TEN_MINUTES(10, R.string.reminder_10_min),
    THIRTY_MINUTES(30, R.string.reminder_30_min),
    ONE_HOUR(60, R.string.reminder_1_hour),
    ONE_DAY(24 * 60, R.string.reminder_1_day);

    companion object {
        /** Returns the preset matching [minutes], or null if it is a custom value. */
        fun fromMinutes(minutes: Int?): ReminderOption? =
            entries.firstOrNull { it.minutes == minutes }
    }
}
