package com.pic2date.ui

import com.pic2date.R
import com.pic2date.parser.model.EventType

/** Maps a parser [EventType] to a localized string resource. */
fun EventType.labelRes(): Int = when (this) {
    EventType.WEDDING -> R.string.type_wedding
    EventType.FLIGHT -> R.string.type_flight
    EventType.DOCTOR_APPOINTMENT -> R.string.type_doctor
    EventType.MEETING -> R.string.type_meeting
    EventType.HOTEL_RESERVATION -> R.string.type_hotel
    EventType.RESTAURANT_RESERVATION -> R.string.type_restaurant
    EventType.CONCERT -> R.string.type_concert
    EventType.BIRTHDAY -> R.string.type_birthday
    EventType.SCHOOL_EVENT -> R.string.type_school
    EventType.CONFERENCE -> R.string.type_conference
    EventType.GENERAL -> R.string.type_general
}
