# Pic2Date

Turn any picture into a calendar event.

Pic2Date is a native Android app that reads an image containing an invitation,
appointment, reservation, ticket, screenshot, WhatsApp message or email
confirmation, extracts the event details with on-device OCR + a smart parser,
and creates a calendar event — in **Hebrew and English**.

---

## Features

- **Image input** — take a photo, pick from the gallery, or **share an image
  straight from another app** (WhatsApp, Gmail, browser, screenshots). Handles
  JPG, PNG, screenshots and single-page PDFs.
- **Offline OCR** — Tesseract (Hebrew + English in one pass) on-device, with an
  ML Kit (Latin) fallback. No network required.
- **Smart event extraction** — a deterministic parser detects:
  - Title, date and time (many formats, incl. `12/08/2026`, `August 12 2026`,
    `12 Aug 2026`, `Thursday 14.8 at 19:30`, `יום חמישי 14.8 בשעה 19:30`).
  - Location / venue, phone number.
  - **Event type** (wedding, flight, doctor appointment, meeting, hotel,
    restaurant, concert, birthday, school event, conference) via weighted,
    multi-signal keyword scoring in both languages.
- **Confirmation screen** — every field is editable, with native date/time
  pickers and reminder options (10 min / 30 min / 1 hour / 1 day / custom).
- **Calendar integration** — writes directly to the device calendar (Google
  Calendar included) with the chosen reminder.
- **Navigation** — a *Navigate* button opens Google Maps for a detected address.
- **Standard Android / Google design** — Material 3, clean white UI, light and
  fast.

## Minimal permissions

`CAMERA`, `READ_CALENDAR`, `WRITE_CALENDAR`. Gallery uses the Android Photo
Picker, so **no storage permission** is required.

---

## Project structure

```
Pic2Date/
├── app/                     Android application (Jetpack Compose, Material 3)
│   ├── src/main/java/com/pic2date/
│   │   ├── MainActivity.kt         camera / gallery / share intents + splash
│   │   ├── ocr/                    OcrService + ML Kit engine (pluggable)
│   │   ├── calendar/               CalendarRepository (insert + reminders)
│   │   ├── model/                  EventDraft, ReminderOption
│   │   ├── ui/                     Compose screens + navigation + theme
│   │   └── util/                   image decoding, maps, formatting
│   └── src/main/assets/tessdata/   trained data (fetched, not committed)
└── parser/                  Pure-Kotlin event-extraction engine (JVM, unit-tested)
    └── src/main/kotlin/com/pic2date/parser/
        ├── EventParser.kt          orchestrator: text -> ParsedEvent
        ├── DateParser.kt           numeric / English / Hebrew dates
        ├── TimeParser.kt           24h / 12h / ranges
        ├── LocationExtractor.kt    venue / address heuristics
        ├── EventTypeClassifier.kt  semantic-ish type detection
        └── ContactExtractor.kt     phone numbers
```

The extraction logic lives in a **pure-Kotlin module** (`:parser`) so it is
fast, deterministic and unit-tested independently of Android.

---

## Building

### Requirements
- JDK 17
- Android SDK (compileSdk 35)

### Build the APK
```bash
# 1. Fetch the Hebrew + English OCR data into app assets (a few MB).
scripts/fetch-tessdata.sh

# 2. Assemble.
./gradlew :app:assembleDebug
# APK -> app/build/outputs/apk/debug/app-debug.apk
```

If the trained data is not fetched, the app still builds and falls back to ML
Kit (Latin) OCR at runtime.

### Run the parser tests
```bash
./gradlew :parser:test
```

### CI

`.github/workflows/android.yml` sets up the JDK + Android SDK, runs the parser
tests, builds the APK and uploads it as the `pic2date-debug-apk` artifact on
every push.

---

## OCR & Hebrew note

Hebrew is a first-class language here. Google ML Kit's on-device text
recognition covers Latin, Chinese, Devanagari, Japanese and Korean — **not
Hebrew** — so Pic2Date uses **Tesseract** (`heb+eng`) as the primary on-device
engine (via the `com.github.adaptech-cz.Tesseract4Android` JitPack artifact),
with ML Kit kept as a fast Latin fallback. Both run entirely offline.

The trained data (`heb.traineddata`, `eng.traineddata`, Tesseract "fast" models)
is fetched into `app/src/main/assets/tessdata/` at build time by
`scripts/fetch-tessdata.sh` (CI does this automatically) and is not committed.

## Parser examples (verified by unit tests)

| Input | Title | Date | Time | Location |
|-------|-------|------|------|----------|
| `Wedding invitation. … Thursday August 14th 2026 at 19:30. Jerusalem Convention Center.` | Wedding | 2026-08-14 | 19:30 | Jerusalem Convention Center |
| `Doctor appointment on 07/09/2026 at 10:00. Herzl 25 Tel Aviv.` | Doctor Appointment | 2026-09-07 | 10:00 | Herzl 25 Tel Aviv |
| `יום חמישי 14.8 בשעה 19:30` | — | 14 Aug | 19:30 | — |
