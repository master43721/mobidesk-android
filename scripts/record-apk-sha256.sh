#!/usr/bin/env bash
# MobiDesk APK SHA-256 and Integrity Recorder (Bash)
set -euo pipefail

APK_PATH="${1:-app/build/outputs/apk/debug/app-debug.apk}"
OUTPUT_DIR="${2:-build/reports}"
mkdir -p "$OUTPUT_DIR"
OUT_FILE="$OUTPUT_DIR/apk-sha256.json"

TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

if [ ! -f "$APK_PATH" ]; then
    cat <<EOF > "$OUT_FILE"
{
  "timestamp": "$TIMESTAMP",
  "apkFound": false,
  "apkPath": "$APK_PATH",
  "status": "NOT_BUILT"
}
EOF
else
    SHA256=$(sha256sum "$APK_PATH" | awk '{print $1}')
    SIZE=$(stat -c %s "$APK_PATH" 2>/dev/null || stat -f %z "$APK_PATH")
    cat <<EOF > "$OUT_FILE"
{
  "timestamp": "$TIMESTAMP",
  "apkFound": true,
  "apkPath": "$APK_PATH",
  "sha256": "$SHA256",
  "sizeBytes": $SIZE,
  "status": "RECORDED"
}
EOF
fi

cat "$OUT_FILE"
exit 0
