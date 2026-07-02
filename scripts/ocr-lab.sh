#!/usr/bin/env bash
#
# OCR lab sweep: reproduce Pic2Date's on-device OCR on a test image across a
# small DPI x PSM matrix and print a scored summary (Hebrew letters count
# double, Latin letters and digits once). See .claude/skills/ocr-lab/SKILL.md.
#
# Usage: scripts/ocr-lab.sh <image> [tessdata-dir]
set -euo pipefail

IMG="${1:?usage: scripts/ocr-lab.sh <image> [tessdata-dir]}"
TD="${2:-$(dirname "$0")/../app/src/main/assets/tessdata}"
OUT="$(mktemp -d)"

command -v tesseract >/dev/null || { echo "tesseract not installed (apt-get install -y tesseract-ocr)"; exit 1; }
[ -s "$TD/heb.traineddata" ] || { echo "missing $TD/heb.traineddata — run scripts/fetch-tessdata.sh"; exit 1; }

# Grayscale exactly like the app (no resize).
python3 - "$IMG" "$OUT/gray.png" <<'EOF'
import sys
from PIL import Image
Image.open(sys.argv[1]).convert("L").save(sys.argv[2])
EOF

score() {
    python3 - "$1" <<'EOF'
import sys
t = open(sys.argv[1], encoding="utf-8").read()
s = sum(2 if 'א' <= c <= 'ת' else 1 if c.isalnum() else 0 for c in t)
print(s)
EOF
}

echo "== OCR lab sweep on $IMG =="
BEST=""; BEST_SCORE=-1
for dpi in 70 150 300; do
    for psm in 3 4; do
        f="$OUT/d${dpi}_p${psm}"
        TESSDATA_PREFIX="$TD" tesseract "$OUT/gray.png" "$f" -l heb+eng --psm "$psm" --dpi "$dpi" 2>/dev/null
        s=$(score "$f.txt")
        lines=$(grep -cve '^\s*$' "$f.txt" || true)
        echo "dpi=$dpi psm=$psm  score=$s  lines=$lines"
        if [ "$s" -gt "$BEST_SCORE" ]; then BEST_SCORE=$s; BEST="$f.txt (dpi=$dpi psm=$psm)"; fi
    done
done
echo ""
echo "== best: $BEST =="
cat "${BEST%% *}"
echo ""
echo "(outputs kept in $OUT)"
