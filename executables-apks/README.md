# MobiDesk Executables & Test APK Directory

This directory is reserved for local testing and physical live drill execution. 

Compiled binaries (`.apk`, `.aab`) in this directory are intentionally ignored by `.gitignore` to prevent repository bloat and avoid committing unverified binaries to GitHub.

---

## 1. Naming Convention for Local Test APKs

When building or dropping an APK here for testing, use the standard format:
```text
fp-<FP_NUMBER>-<BUILD_TYPE>-v<VERSION>.apk
```

**Examples:**
- `fp-01-debug-v1.0.apk`
- `fp-02-debug-v0.1.apk`

---

## 2. Generating SHA-256 Checksum for Live Drill Reports

Per `docs/MANDATORY_LIVE_DRILL_POLICY.md`, every live drill execution report must record the exact SHA-256 of the tested APK.

**PowerShell (Windows):**
```powershell
Get-FileHash -Path executables-apks/fp-01-debug-v1.0.apk -Algorithm SHA256
```

**Bash / Linux / macOS:**
```bash
sha256sum executables-apks/fp-01-debug-v1.0.apk
```

---

## 3. Direct Installation to Connected Android Device

To install the APK directly to a physical device connected via USB or Wi-Fi:
```powershell
adb install -r executables-apks/fp-01-debug-v1.0.apk
```

To build directly into this folder using Gradle:
```powershell
./gradlew assembleDebug
# Copy to executables-apks
Copy-Item app/build/outputs/apk/debug/app-debug.apk executables-apks/fp-01-debug-v1.0.apk
```
