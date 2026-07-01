# Tesseract trained data

This folder must contain the Tesseract "fast" trained data used for offline OCR:

- `eng.traineddata`
- `heb.traineddata`

These binaries are **not** committed to the repository. Populate them before
building:

```bash
scripts/fetch-tessdata.sh
```

The CI workflow (`.github/workflows/android.yml`) runs this automatically before
assembling the APK. If the files are absent, the app still builds and falls back
to ML Kit (Latin-only) OCR at runtime.
