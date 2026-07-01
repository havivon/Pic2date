package com.pic2date.parser.model

/**
 * Semantic category of a detected event. Each type carries weighted keyword
 * signals (Hebrew + English) used by [com.pic2date.parser.EventTypeClassifier]
 * and a default human-readable title used when no explicit title is found.
 */
enum class EventType(
    val englishTitle: String,
    val hebrewTitle: String,
    /** Strong signals — a single hit is highly indicative. */
    val strongKeywords: List<String>,
    /** Weak signals — contribute to scoring but are less decisive. */
    val weakKeywords: List<String> = emptyList(),
) {
    WEDDING(
        englishTitle = "Wedding",
        hebrewTitle = "חתונה",
        strongKeywords = listOf(
            "wedding", "חתונה", "להינשא", "נישואין", "חופה", "groom", "bride",
            "save the date", "מזמינים אתכם לחתונה",
        ),
        weakKeywords = listOf("invite you", "מזמינים", "celebrate", "נשמח לראותכם"),
    ),
    FLIGHT(
        englishTitle = "Flight",
        hebrewTitle = "טיסה",
        strongKeywords = listOf(
            "flight", "טיסה", "boarding", "departure", "gate", "טרמינל", "terminal",
            "boarding pass", "כרטיס עלייה למטוס", "el al", "אל על", "ryanair",
        ),
        weakKeywords = listOf("airport", "שדה תעופה", "seat", "מושב", "pnr"),
    ),
    DOCTOR_APPOINTMENT(
        englishTitle = "Doctor Appointment",
        hebrewTitle = "תור לרופא",
        strongKeywords = listOf(
            "doctor", "appointment", "תור", "רופא", "ד\"ר", "ד״ר", "dr.", "clinic",
            "מרפאה", "קופת חולים", "כללית", "מכבי", "מאוחדת", "לאומית", "בדיקה",
        ),
        weakKeywords = listOf("medical", "רפואי", "checkup", "התור שלך"),
    ),
    MEETING(
        englishTitle = "Meeting",
        hebrewTitle = "פגישה",
        strongKeywords = listOf(
            "meeting", "פגישה", "ישיבה", "sync", "zoom", "google meet", "teams call",
            "conference call", "שיחת ועידה", "מפגש",
        ),
        weakKeywords = listOf("agenda", "סדר יום", "call", "discussion", "דיון"),
    ),
    HOTEL_RESERVATION(
        englishTitle = "Hotel Reservation",
        hebrewTitle = "הזמנת מלון",
        strongKeywords = listOf(
            "hotel", "מלון", "check-in", "check in", "checkin", "check-out", "reservation",
            "booking", "הזמנה", "צ'ק אין", "לינה", "אכסניה", "booking.com",
        ),
        weakKeywords = listOf("nights", "לילות", "room", "חדר", "guest"),
    ),
    RESTAURANT_RESERVATION(
        englishTitle = "Restaurant Reservation",
        hebrewTitle = "הזמנת מסעדה",
        strongKeywords = listOf(
            "restaurant", "מסעדה", "table for", "שולחן ל", "reservation for", "dinner reservation",
            "הזמנת מקום", "סועדים", "diners",
        ),
        weakKeywords = listOf("table", "שולחן", "menu", "תפריט", "ארוחה"),
    ),
    CONCERT(
        englishTitle = "Concert",
        hebrewTitle = "הופעה",
        strongKeywords = listOf(
            "concert", "הופעה", "show", "מופע", "live", "tour", "tickets", "כרטיסים",
            "doors open", "פתיחת דלתות", "festival", "פסטיבל",
        ),
        weakKeywords = listOf("stage", "במה", "band", "להקה", "venue"),
    ),
    BIRTHDAY(
        englishTitle = "Birthday Party",
        hebrewTitle = "יום הולדת",
        strongKeywords = listOf(
            "birthday", "יום הולדת", "יומולדת", "turning", "b-day", "bday", "חוגג", "חוגגת",
        ),
        weakKeywords = listOf("party", "מסיבה", "celebrate", "חוגגים"),
    ),
    SCHOOL_EVENT(
        englishTitle = "School Event",
        hebrewTitle = "אירוע בית ספר",
        strongKeywords = listOf(
            "school", "בית ספר", "בית הספר", "כיתה", "class", "parents meeting", "אסיפת הורים",
            "גן ילדים", "kindergarten", "מורה", "teacher", "מסיבת סיום", "graduation",
        ),
        weakKeywords = listOf("pupils", "תלמידים", "lesson", "שיעור"),
    ),
    CONFERENCE(
        englishTitle = "Conference",
        hebrewTitle = "כנס",
        strongKeywords = listOf(
            "conference", "כנס", "summit", "כינוס", "registration", "הרשמה", "keynote",
            "webinar", "וובינר", "expo", "תערוכה", "workshop", "סדנה",
        ),
        weakKeywords = listOf("speaker", "מרצה", "session", "מושב", "track"),
    ),

    /** Fallback when no specific type can be confidently determined. */
    GENERAL(
        englishTitle = "Event",
        hebrewTitle = "אירוע",
        strongKeywords = emptyList(),
        weakKeywords = emptyList(),
    );

    fun title(hebrew: Boolean): String = if (hebrew) hebrewTitle else englishTitle
}
