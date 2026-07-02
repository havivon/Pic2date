---
name: ocr-lab
description: Reproduce and debug Pic2Date's on-device OCR locally on a test image, without building the app. Use whenever a scan gives bad/missing text, before changing OcrService/TesseractOcrEngine/parser, or to tune preprocessing (DPI, PSM, grayscale, scaling).
---

# OCR Lab — reproduce the phone's OCR pipeline locally

Pic2Date's OCR quality bugs are almost never guessable from code. This lab
reproduces the exact on-device pipeline (same engine family, same trained data)
on a desktop, so a fix can be measured on the failing image *before* pushing a
build.

## Golden rules (learned the hard way)

1. **Never ship an OCR/preprocessing change that was not first verified here**
   on a real failing image. The build-install-scan loop through the user's
   phone takes ~10 minutes per attempt; the lab takes seconds.
2. **Change one variable at a time** (DPI, PSM, scaling, color) and diff the
   text output.
3. **Finish by feeding the raw lab OCR output through `EventParser`** (see
   step 5) — good-looking text that doesn't parse is still a failure.

## Setup (once per machine)

```bash
apt-get install -y tesseract-ocr                       # engine (5.x, like tesseract4android)
scripts/fetch-tessdata.sh                              # heb+eng into app assets
# OR extract the data from a built APK for exact parity:
unzip -o -j Pic2Date.apk "assets/tessdata/*" -d /tmp/tessdata
```

## Reproducing the app pipeline

The app pipeline is: decode (cap 3300px) → grayscale (no resize!) →
Tesseract `heb+eng`, `PSM_AUTO`, `user_defined_dpi=150`.

```bash
TD=app/src/main/assets/tessdata   # or /tmp/tessdata
python3 - <<'EOF'                 # grayscale exactly like the app
from PIL import Image
Image.open("failing.jpg").convert("L").save("gray.png")
EOF
tesseract gray.png - -l heb+eng --psm 3 --dpi 150      # PSM 3 == PSM_AUTO
```

## Known findings (do not re-learn these)

- **Any bilinear resize destroys thin Hebrew strokes.** Even a 1.03x upscale
  collapsed a full invitation to 4 lines. Grayscale only; no scaling.
- **DPI drives page-layout analysis.** Android Bitmaps have no DPI, so
  Tesseract guesses. Measured on invitation photos: 150 best (kept the Hebrew
  date line intact), 70/200 OK but split columns, 300 catastrophic. The app
  pins `user_defined_dpi=150`.
- **`PSM_AUTO_OSD` is forbidden**: its page-level script detection can decide
  the page is Latin and transliterate Hebrew glyphs ("D1'1 1JNJ1NN" = יום
  חתונתנו). Orientation is handled by the rotation vote in
  `TesseractOcrEngine` instead.
- **Decorative photos** (card on fabric) break PSM 3 column detection; the
  Hebrew-calendar date ("ה' באלול תשפ"ו") often survives when the Gregorian
  digits don't — the parser converts it (`HebrewDateParser`).

## Step 5 — always validate end-to-end

Paste the lab's raw OCR text into a scratch JUnit test and print the parse:

```kotlin
val e = EventParser.parse(rawOcrText, LocalDate.now())
println("type=${e.eventType} title=${e.title} date=${e.date} time=${e.time}-${e.endTime} loc=${e.location}")
```

Run parser tests with `./gradlew :parser:test` (pure JVM — works even where
the Android SDK is unavailable; if Google Maven is blocked, copy
`parser/src` into a standalone Kotlin-JVM Gradle project first).

## Sweep helper

`scripts/ocr-lab.sh <image>` runs the standard matrix (dpi 70/150/300 ×
psm 3/4) and prints a scored summary.
