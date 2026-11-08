# MobiDesk — APK Build, Artifact & Physical Device Installation Guide

**Date:** 8 November 2026
**Auditor / Lead Engineer:** Jules (AI Software Engineer)
**Governance Authority:** `AGENTS.md` & `docs/MANDATORY_LIVE_DRILL_POLICY.md`
**Target Architecture:** Android Phone (`arm64-v8a`, SDK 29–37)

---

## 1. APK Distribution & Binary Policy

In accordance with Section 4 of `AGENTS.md`:
- **No Binaries in Git History:** `.apk` and `.aab` files must **never be committed** directly into Git history.
- **Official Artifacts via GitHub Actions CI:** Automated builds produce APKs uploaded as verifiable GitHub Actions workflow run artifacts (retained for 14 to 90 days).
- **Local Testing Worktree:** Developers may drop test APKs into `executables-apks/` locally; this directory is strictly ignored by `.gitignore`.

---

## 2. GitHub Actions CI Build Workflow & Triggers

### 2.1 Workflow Details
- **Workflow File:** `.github/workflows/android-ci.yml`
- **Workflow Name:** `Android CI`
- **Runner Environment:** `ubuntu-24.04`
- **Branch Triggers:** Automatically runs on pushes and PRs to `main`, `fp-*`, `fp/**`, `feature/fp-*`, `step/fp-*`.
- **Manual Trigger:** Available via GitHub Actions UI (`workflow_dispatch`).

### 2.2 Build Pipeline Steps
The automated workflow executes:
1. Pinned FreeRDP submodule checkout (`third_party/FreeRDP`).
2. JDK 21 setup (`actions/setup-java@v4` with Temurin).
3. Linux native build prerequisites installation (`nasm`).
4. Android SDK 37 installation (`platforms;android-37.0`, `build-tools;37.0.0`, `ndk;29.0.13113456`, `cmake;4.1.2`).
5. Native dependency caching & hash verification (`scripts/fetch_freerdp_deps.py`).
6. Mandatory Live Drill Gate policy verification (`scripts/verify_live_drill_gate.py`).
7. JVM unit test execution & APK assembly:
   ```bash
   ./gradlew --no-daemon --stacktrace --max-workers=2 :app:testDebugUnitTest :app:assembleDebug
   ```
8. ZIP-integrity and APK signature verification with `apksigner`.
9. Artifact upload to GitHub Actions.

---

## 3. Recommended Branch & Revision for Physical Device Demonstration

To test the complete integrated foundation (FP-01 + FP-02 + FP-03 + FP-04):
- **Target Branch:** `fp-04-status-readiness`
- **Target Commit SHA:** `1cba5876ee163ef36d9e3cd505112fb24fb6a568`
- **Build Variant:** `debug` (`app-debug.apk`)
- **Signing Status:** Debug-signed with standard Android debug keystore.
  *(Note: This is a debug-signed test artifact for live drills, not a production release signed by the Owner's private release key).*

---

## 4. Downloading the Official CI Artifact

Once the GitHub Actions run completes for the branch commit:

### Option A: Using GitHub CLI (`gh`)
```bash
# Locate the run ID for the commit:
gh run list --workflow=android-ci.yml --commit=1cba5876ee163ef36d9e3cd505112fb24fb6a568

# Download the APK artifact using the run ID:
gh run download <RUN_ID> -n mobidesk-debug-1cba5876ee163ef36d9e3cd505112fb24fb6a568 --dir ./downloaded-apk
```

### Option B: Using GitHub Web UI
1. Navigate to `https://github.com/master43721/mobidesk-android/actions`.
2. Click on the workflow run for commit `1cba587` on branch `fp-04-status-readiness`.
3. Scroll down to the **Artifacts** section at the bottom of the summary page.
4. Download `mobidesk-debug-1cba5876ee163ef36d9e3cd505112fb24fb6a568.zip`.
5. Unzip to obtain `app-debug.apk`.

---

## 5. Artifact Integrity & SHA-256 Verification

Before installing on physical hardware, verify the file's SHA-256 checksum and debug signature:

```bash
# 1. Compute SHA-256 checksum
sha256sum ./downloaded-apk/app-debug.apk

# 2. Verify ZIP archive integrity
unzip -tq ./downloaded-apk/app-debug.apk

# 3. Verify APK signature
apksigner verify --verbose ./downloaded-apk/app-debug.apk
```

---

## 6. Physical Android Device Installation (via ADB)

1. Connect your physical Android phone via USB.
2. Enable USB Debugging in Android Developer Options.
3. Verify connection:
   ```bash
   adb devices
   ```
   *(Ensure your device appears with `device` status, not `unauthorized`).*

4. Install the debug APK:
   ```bash
   adb install -r -d ./downloaded-apk/app-debug.apk
   ```

5. Verify package installation:
   ```bash
   adb shell pm list packages | grep com.mobidesk.android
   # Output should display: package:com.mobidesk.android
   ```

6. Launch the application:
   ```bash
   adb shell monkey -p com.mobidesk.android -c android.intent.category.LAUNCHER 1
   ```

---

## 7. Staging Runtime Configuration Requirements

To exercise the interactive flows during the live demonstration:
1. **Supabase Staging Project:**
   - The app reads configuration from `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_PUBLISHABLE_KEY`.
   - In automated builds, these are populated at build time via `local.properties` or CI repository secrets.
   - **Crucial Rule:** Never use or pass Supabase `service_role` keys. Only publishable/anon keys are permitted.
2. **Student Test Account:**
   - Email: `student-qa@college.edu`
   - Password: Established staging password.
   - Assignment: 1 active row in staging `vms` table linked to the student profile.
3. **Session Broker Staging Endpoint:**
   - Broker base URL configured to point to staging `/v1/vm-status`.
