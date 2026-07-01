package com.pic2date.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.pic2date.model.EventDraft
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone

/**
 * Writes events to the device calendar provider. Requires READ_CALENDAR /
 * WRITE_CALENDAR. Falls back to an ACTION_INSERT intent (no permission needed)
 * when direct insertion is not possible.
 */
class CalendarRepository(context: Context) {

    private val appContext = context.applicationContext

    sealed interface SaveResult {
        data class Success(val eventId: Long) : SaveResult
        data object NoCalendar : SaveResult
        data class Error(val cause: Throwable) : SaveResult
    }

    /** Inserts the event and its reminder. Returns the new event id on success. */
    fun insert(draft: EventDraft): SaveResult {
        val calendarId = findWritableCalendarId() ?: return SaveResult.NoCalendar
        return try {
            val (start, end, allDay) = computeTimes(draft)
            val tz = if (allDay) "UTC" else TimeZone.getDefault().id

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, draft.title.ifBlank { "Event" })
                put(CalendarContract.Events.DTSTART, start)
                put(CalendarContract.Events.DTEND, end)
                put(CalendarContract.Events.EVENT_TIMEZONE, tz)
                if (allDay) put(CalendarContract.Events.ALL_DAY, 1)
                if (draft.location.isNotBlank()) {
                    put(CalendarContract.Events.EVENT_LOCATION, draft.location)
                }
                val description = buildDescription(draft)
                if (description.isNotBlank()) {
                    put(CalendarContract.Events.DESCRIPTION, description)
                }
                if (draft.reminderMinutes != null) {
                    put(CalendarContract.Events.HAS_ALARM, 1)
                }
            }

            val uri = appContext.contentResolver
                .insert(CalendarContract.Events.CONTENT_URI, values)
                ?: return SaveResult.Error(IllegalStateException("Insert returned null"))
            val eventId = ContentUris.parseId(uri)

            draft.reminderMinutes?.let { addReminder(eventId, it) }
            SaveResult.Success(eventId)
        } catch (e: SecurityException) {
            SaveResult.Error(e)
        } catch (e: Exception) {
            SaveResult.Error(e)
        }
    }

    private fun addReminder(eventId: Long, minutes: Int) {
        val values = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, minutes)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        appContext.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
    }

    /** Picks a calendar the user can write to, preferring a primary Google one. */
    private fun findWritableCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.VISIBLE,
        )
        appContext.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection, null, null, null,
        )?.use { cursor ->
            var best: Long? = null
            var bestScore = Int.MIN_VALUE
            val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val accessCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
            val primaryCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)
            val typeCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
            val visibleCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.VISIBLE)

            while (cursor.moveToNext()) {
                val access = cursor.getInt(accessCol)
                if (access < CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) continue
                var score = 0
                if (cursor.getInt(primaryCol) == 1) score += 4
                if (cursor.getInt(visibleCol) == 1) score += 1
                if (cursor.getString(typeCol) == "com.google") score += 2
                if (score > bestScore) {
                    bestScore = score
                    best = cursor.getLong(idCol)
                }
            }
            return best
        }
        return null
    }

    /** ACTION_INSERT intent — opens the calendar app's editor, no permission needed. */
    fun buildInsertIntent(draft: EventDraft): Intent {
        val (start, end, allDay) = computeTimes(draft)
        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, draft.title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, allDay)
            if (draft.location.isNotBlank()) {
                putExtra(CalendarContract.Events.EVENT_LOCATION, draft.location)
            }
            val description = buildDescription(draft)
            if (description.isNotBlank()) {
                putExtra(CalendarContract.Events.DESCRIPTION, description)
            }
        }
    }

    private fun buildDescription(draft: EventDraft): String = buildString {
        if (draft.notes.isNotBlank()) append(draft.notes)
        if (!draft.phone.isNullOrBlank()) {
            if (isNotEmpty()) append('\n')
            append(draft.phone)
        }
    }.trim()

    private data class Times(val start: Long, val end: Long, val allDay: Boolean)

    private fun computeTimes(draft: EventDraft): Times {
        val date = draft.date ?: java.time.LocalDate.now()
        val time = draft.time
        return if (time == null) {
            // All-day events use UTC midnight per CalendarContract.
            val start = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            Times(start, end, allDay = true)
        } else {
            val zone = ZoneId.systemDefault()
            val startDt = date.atTime(time).atZone(zone)
            var endDt = draft.endTime?.let { date.atTime(it).atZone(zone) }
                ?: startDt.plusHours(1)
            if (!endDt.isAfter(startDt)) endDt = endDt.plusDays(1)
            Times(startDt.toInstant().toEpochMilli(), endDt.toInstant().toEpochMilli(), allDay = false)
        }
    }

    /** ACTION_VIEW intent for an existing event (used by the success screen). */
    fun buildViewIntent(eventId: Long): Intent {
        val uri: Uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return Intent(Intent.ACTION_VIEW).setData(uri)
    }
}
