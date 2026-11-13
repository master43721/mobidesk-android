#!/usr/bin/env bash
set -euo pipefail

echo "Waiting for emulator boot completion..."
adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'
echo "Emulator boot complete!"

echo "=== Step 1: Install Candidate APK ==="
adb install -r -t candidate-apks/app-debug.apk
echo "Candidate APK install successful!"

echo "=== Step 2: Verify Package in PackageManager ==="
adb shell pm list packages | grep "com.mobidesk.android"
adb shell dumpsys package com.mobidesk.android | grep -E "versionName|versionCode|targetSdk"

echo "=== Step 3: Install Instrumentation Test APK ==="
adb install -r -t candidate-apks/app-debug-androidTest.apk
echo "Test APK install successful!"

echo "=== Step 4: Launch MobiDesk MainActivity ==="
adb shell am start -n com.mobidesk.android/.MainActivity
sleep 5

echo "=== Step 5: Capture Initial Emulator Screenshot ==="
adb exec-out screencap -p > emulator-smoke.png || true

echo "=== Step 6: Run Instrumented Feature Tests ==="
set +e
adb shell am instrument -w -r com.mobidesk.android.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tee instrumentation-results.txt
test_exit_code=$?
set -e

echo "=== Step 7: Post-Test Evidence Capture ==="
adb exec-out screencap -p > emulator-after-tests.png || true
adb logcat -d > emulator-logcat.txt || true

echo "=== Test Summary Output ==="
cat instrumentation-results.txt

if [ $test_exit_code -ne 0 ] || grep -q "FAILURES!!!" instrumentation-results.txt; then
  echo "FATAL: Instrumented tests reported failures!"
  exit 1
fi
echo "All instrumented tests passed on emulator!"
