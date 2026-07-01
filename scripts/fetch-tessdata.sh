#!/usr/bin/env bash
#
# Downloads the Tesseract "fast" trained data for Hebrew and English into the
# app's assets so OCR works fully offline. These files are a few MB each and are
# intentionally NOT committed to git; run this once before building (CI does it
# automatically).
#
# Usage: scripts/fetch-tessdata.sh
set -euo pipefail

DEST="$(cd "$(dirname "$0")/.." && pwd)/app/src/main/assets/tessdata"
BASE_URL="https://github.com/tesseract-ocr/tessdata_fast/raw/main"
LANGS=("eng" "heb")

mkdir -p "$DEST"

for lang in "${LANGS[@]}"; do
    target="$DEST/${lang}.traineddata"
    if [[ -f "$target" && -s "$target" ]]; then
        echo "✓ ${lang}.traineddata already present"
        continue
    fi
    echo "↓ Downloading ${lang}.traineddata ..."
    curl -fSL "${BASE_URL}/${lang}.traineddata" -o "$target"
done

echo "Done. Trained data in: $DEST"
