# FP-01 Live-Drill Execution Report & Kit

**Feature Package:** FP-01 — Authentication, Session State & Recovery  
**Application ID:** `com.mobidesk.android`  
**Target Repository:** `master43721/mobidesk-android`  
**Handoff Branch:** `fp-01-auth-hardening` (tracking staging candidate `84bb6b7e507d3871848f3386c6ceab47d4442c05` on `main`)  
**Expected CI Artifact SHA-256:** `450aa3be0b510d3847d9a83a16be8ee7a196d2474eda2f58ededbc76e68d5206` (from CI Run 42450424147)  
**Current Live-Drill Status:** `LIVE_DRILL_STATUS: BLOCKED on NEG-03 (staging Supabase, SMTP, tester phone details)`  
**Owner Sign-off Status:** UNACCEPTED (Awaiting physical demonstration on Android phone)  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md`  

---

## 1. Executive Summary

FP-01 hardens the authentication foundation: email/password sign-in, session restoration, discrete `AuthLevel` (`SIGNED_OUT`, `RECOVERY_ONLY`, `FULLY_AUTHENTICATED`), fail-closed password recovery via custom URI PKCE callback, local sign-out fallback, and strict redaction of credentials and tokens.

All 40 automated unit tests pass on branch tip `84bb6b7e507d3871848f3386c6ceab47d4442c05`. Automated CI Run 42450424147 completed with SUCCESS. Per the Terminal Acceptance Invariant, FP-01 cannot be marked complete or accepted until the Project Owner executes this physical live drill on target hardware.

---

## 2. Preconditions & Prerequisites (Ordered)

1. **Staging Supabase Project (not production):**
   - Active staging project with student test accounts provisioned.
   - `local.properties` contains `SUPABASE_PUBLISHABLE_KEY` and `SUPABASE_URL`.
   - Custom SMTP configured (the Supabase built-in mailer cannot deliver to non-team test addresses).
2. **Physical Android Device:**
   - Physical phone model and Android version recorded by Owner (SDK 29–37).
   - USB debugging enabled (or ability to install via browser/drive).
3. **Network Connection:** Active Wi-Fi or cellular network connecting to the staging Supabase project.

---

## 3. Exact Installation & Verification Steps

Execute the following commands to obtain the verified CI artifact and install it on the physical phone:

```bash
# 1. Download the CI-built debug APK artifact from GitHub Actions
gh run download 42450424147 -n mobidesk-debug-84bb6b7e507d3871848f3386c6ceab47d4442c05 --dir /tmp/mobidesk-fp01

# 2. Verify APK SHA-256 hash against CI build record
sha256sum /tmp/mobidesk-fp01/app-debug.apk
# Expected: 450aa3be0b510d3847d9a83a16be8ee7a196d2474eda2f58ededbc76e68d5206

# 3. Connect physical phone via ADB and install
adb install -r /tmp/mobidesk-fp01/app-debug.apk

# 4. Verify installed package on device
adb shell pm list packages | grep com.mobidesk.android
```

---

## 4. Numbered Scenarios & Acceptance Criteria (16 Scenarios)

| Step # | Scenario & Action | Acceptance Criteria (Expected Result) | Owner Verification |
|:---:|:---|:---|:---:|
| **1** | Fresh installation and artifact checksum match | Compare SHA-256 hash of CI APK against expected `450aa3be...`. Install APK: `adb install -r /tmp/app-debug.apk`. Launch app. | Hash matches exactly (`450aa3be...`). App boots cleanly without crashing, displays splash/branding, enters Sign-in screen. | [         ] |
| **2** | Unauthenticated state inspection | Inspect fresh launch interface. | Displays "Sign in", email and password inputs, forgot password link. No preview/demo shortcuts, no signup affordances, no fake bypass. | [         ] |
| **3** | Invalid credentials & safe error handling | Enter valid format email and wrong password; tap "Sign in". | Form displays generic, stable notice: "The email or password is incorrect." No backend stack traces or internal Supabase details leaked. | [         ] |
| **4** | Valid QA account authentication | Enter provisioned staging student credentials; tap "Sign in". | Progress indicator displays during busy state, authenticates successfully without error; transitions to `AuthLevel.FULLY_AUTHENTICATED`. | [         ] |
| **5** | Session persistence after force-close | Force-close the application (swipe away from Android Recents), then tap app icon to reopen. | Initializing session restoration executes without premature signed-out route flash; restores session and lands directly on authenticated Dashboard. | [         ] |
| **6** | Password recovery request & safe notice | From login screen, tap "Forgot password", enter test email, tap "Send recovery link". | Generic safe acknowledgement displays: "If an account exists for this email, password reset instructions have been sent." (Account enumeration prevention). | [         ] |
| **7** | Deep-link callback & password update form | Tap recovery link in email on device (`com.mobidesk.android://auth-callback/recovery?code=<TEST_CODE>`). | App opens, validates exact `/recovery` path, sets `RECOVERY_ONLY` level. Recovery update form renders. Workspace is NOT loaded. | [         ] |
| **8** | Process death while on recovery form | While password update form is open, execute process death:<br>`adb shell am kill com.mobidesk.android`<br>Relaunch app from launcher. | App detects interrupted recovery state, fails closed to `RecoveryInterruptedScreen`, clears local session, requires restart from Login. | [         ] |
| **9** | Duplicate / re-delivered recovery callback | While on password update form, simulate duplicate deep link intent:<br>`adb shell am start -W -a android.intent.action.VIEW -d "com.mobidesk.android://auth-callback/recovery?code=<TEST_CODE>" com.mobidesk.android` | App identifies duplicate/consumed callback fingerprint, ignores re-delivery without re-triggering PKCE exchange or crashing. | [         ] |
| **10** | Stray recovery callback while signed in | While signed in as authenticated student (`FULLY_AUTHENTICATED`), execute stray callback:<br>`adb shell am start -W -a android.intent.action.VIEW -d "com.mobidesk.android://auth-callback/recovery?code=<STRAY_CODE>" com.mobidesk.android` | App ignores stray callback; student remains signed in. Stray callback MUST NOT sign the user out or demote to recovery. | [         ] |
| **11** | Expired / replayed link rejection | Attempt to re-trigger callback with already-consumed or expired token:<br>`adb shell am start -W -a android.intent.action.VIEW -d "com.mobidesk.android://auth-callback/recovery?code=<TEST_CODE>" com.mobidesk.android` | Fails closed with actionable safe error; does not promote to recovery form or leak backend token details. | [         ] |
| **12** | Screen rotation during recovery form | While on password update form, rotate device orientation:<br>`adb shell settings put system user_rotation 1`<br>`adb shell settings put system user_rotation 0` | UI retains entered password form state across configuration change without losing `RECOVERY_ONLY` level or resetting. | [         ] |
| **13** | Recovery cancellation / Back navigation | On password update screen, tap Cancel or Android system Back button. | Calls `signOutCurrentSession()`, clears recovery store, resets state to `SIGNED_OUT`, returns to Login. Workspace is never exposed. | [         ] |
| **14** | Offline sign-out | While signed in, simulate network drop:<br>`adb shell svc wifi disable && adb shell svc data disable`<br>Tap "Sign out".<br>Restore network:<br>`adb shell svc wifi enable && adb shell svc data enable` | `SignOutScope.LOCAL` successfully clears local encrypted store, wipes tokens, and returns to `SIGNED_OUT` screen even when remote Supabase is unreachable. | [         ] |
| **15** | Authenticated session sign-out | In authenticated Dashboard/Settings with network connected, tap "Sign out". | Local tokens wiped, remote session revoked, UI state resets to `SIGNED_OUT`, Back button cannot re-enter workspace. | [         ] |
| **16** | Credential security & logcat verification | Inspect logcat output across all flows:<br>`adb logcat -d \| grep -E "(password\|access_token\|refresh_token\|code_verifier)"` | Zero passwords, access tokens, refresh tokens, PKCE verifiers, or raw callback URLs appear in device logcat. | [         ] |

---

## 5. Evidence Checklist & Redaction Protocol

- **Screenshots:** Capture and name:
  - `<date>-fp01-step01-launch.png`
  - `<date>-fp01-step03-error.png`
  - `<date>-fp01-step04-authenticated.png`
  - `<date>-fp01-step07-recovery.png`
  - `<date>-fp01-step08-interrupted.png`
  - `<date>-fp01-step15-signout.png`
- **SHA-256 Manifest:** Record each capture in `docs/live-drills/FP-01/evidence/manifest.sha256`.
- **Redaction Checklist:**
  - [ ] No real student names or production email addresses.
  - [ ] No passwords or OTP codes shown in screenshots.
  - [ ] No Supabase JWTs, refresh tokens, or service-role keys in text or logs.
  - [ ] No internal server IP addresses or VM hostnames visible.

---

## 6. Abort & Rollback Procedure

If any step fails or an unexpected exception occurs:
1. **Immediately halt the drill.** Do not proceed to subsequent steps.
2. Record the defect in `docs/live-drills/FP-01/DEFECTS.md` with full reproduction steps.
3. Uninstall the test APK: `adb uninstall com.mobidesk.android`.
4. Transition `LIVE_DRILL_STATUS` to `FAIL` or `BLOCKED`.

---

## 7. Owner Sign-Off Block

```text
================================================================================
                    PROJECT OWNER LIVE-DRILL SIGN-OFF
================================================================================
Feature Point:           FP-01 — Authentication, Session State & Recovery
Commit SHA:              84bb6b7e507d3871848f3386c6ceab47d4442c05
Expected APK SHA-256:    450aa3be0b510d3847d9a83a16be8ee7a196d2474eda2f58ededbc76e68d5206
Actual APK SHA-256:      [Recorded from installed APK]
Device Model & OS:       [Recorded by Owner]
All Scenarios Passed:    [YES / NO]
Evidence Manifest Valid: [YES / NO]
Owner Decision:          [ ACCEPTED / REJECTED ]

Signed by:               __________________________________
Date:                    __________________________________
================================================================================
```
