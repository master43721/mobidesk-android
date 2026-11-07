#!/usr/bin/env bash
# MobiDesk Verification Script (Toolchain check, JVM tests, lint, assemble)
set -euo pipefail

OUTPUT_DIR="${1:-build/reports}"
mkdir -p "$OUTPUT_DIR"
OUT_FILE="$OUTPUT_DIR/verification-report.json"

TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

JDK_AVAILABLE=false
JDK_VERSION=""
if command -v java >/dev/null 2>&1; then
    JDK_AVAILABLE=true
    JDK_VERSION=$(java -version 2>&1 | head -n 1)
fi

SDK_AVAILABLE=false
ANDROID_SDK_PATH="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -n "$ANDROID_SDK_PATH" ] && [ -d "$ANDROID_SDK_PATH" ]; then
    SDK_AVAILABLE=true
fi

if [ "$JDK_AVAILABLE" = false ] || [ "$SDK_AVAILABLE" = false ]; then
    cat <<EOF > "$OUT_FILE"
{
  "timestamp": "$TIMESTAMP",
  "toolchain": {
    "jdkAvailable": $JDK_AVAILABLE,
    "jdkVersion": "$JDK_VERSION",
    "androidSdkAvailable": $SDK_AVAILABLE,
    "androidHome": "$ANDROID_SDK_PATH",
    "gradlewAvailable": true
  },
  "verification": {
    "status": "BLOCKED_MISSING_TOOLCHAIN",
    "totalTests": 0,
    "passed": 0,
    "failed": 0,
    "skipped": 0,
    "apkBuilt": false
  }
}
EOF
    cat "$OUT_FILE"
    exit 1
fi

echo "Toolchain verified. Executing build verification..."
./gradlew --no-daemon --stacktrace :app:testDebugUnitTest :app:lintDebug :app:assembleDebug

TOTAL=0
FAILED=0
SKIPPED=0

if python3 -c 'import sys; sys.exit(0)' >/dev/null 2>&1; then
    eval "$(python3 -c '
import glob, xml.etree.ElementTree as ET
total, failed, skipped = 0, 0, 0
for xml_file in glob.glob("app/build/test-results/testDebugUnitTest/*.xml"):
    try:
        tree = ET.parse(xml_file)
        root = tree.getroot()
        total += int(root.attrib.get("tests", 0))
        failed += int(root.attrib.get("failures", 0)) + int(root.attrib.get("errors", 0))
        skipped += int(root.attrib.get("skipped", 0))
    except Exception:
        pass
passed = total - failed - skipped
print(f"TOTAL={total}\nPASSED_COUNT={passed}\nFAILED={failed}\nSKIPPED={skipped}")
')"
else
    PASSED_COUNT=0
fi

APK_BUILT=false
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    APK_BUILT=true
fi

STATUS="SUCCESS"
if [ "$FAILED" -gt 0 ] || [ "$APK_BUILT" = false ]; then
    STATUS="FAILED"
fi

cat <<EOF > "$OUT_FILE"
{
  "timestamp": "$TIMESTAMP",
  "toolchain": {
    "jdkAvailable": true,
    "jdkVersion": "$JDK_VERSION",
    "androidSdkAvailable": true,
    "androidHome": "$ANDROID_SDK_PATH",
    "gradlewAvailable": true
  },
  "verification": {
    "status": "$STATUS",
    "totalTests": $TOTAL,
    "passed": $PASSED_COUNT,
    "failed": $FAILED,
    "skipped": $SKIPPED,
    "apkBuilt": $APK_BUILT
  }
}
EOF

cat "$OUT_FILE"
if [ "$STATUS" != "SUCCESS" ]; then
    exit 1
fi
exit 0
