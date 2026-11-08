# FP-01 Live-Drill Execution Report & Kit

**Feature Package:** FP-01 — Authentication, Session State & Recovery  
**Application ID:** `com.mobidesk.android`  
**Target Repository:** `master43721/mobidesk-android`  
**Handoff Branch:** `fp-01-auth-hardening` (tracking staging candidate `84bb6b7e507d3871848f3386c6ceab47d4442c05` on `main`)  
**Current Live-Drill Status:** `LIVE_DRILL_STATUS: READY FOR OWNER`  
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

# 3. Connect physical phone via ADB and install
adb install -r /tmp/mobidesk-fp01/app-debug.apk

# 4. Verify installed package on device
adb shell pm list packages | grep com.mobidesk.android
```

---

## 4. Numbered Scenarios & Acceptance Criteria

| Step # | Scenario & Action | Acceptance Criteria (Expected Result) | Result |
|:---:|:---|:---|:---:|
| **1** | Clean launch from launcher icon | App launches smoothly, displays splash/branding, enters Sign-in screen. No crashes, no route flash. | NOT RUN |
| **2** | Inspect unauthenticated UI | Shows email/password fields, "Sign in" button, "Forgot password" link. No self-registration, demo bypass, or student data. | NOT RUN |
| **3** | Submit invalid credentials | Enter test email + wrong password. UI displays stable safe error: *"The email or password is incorrect."* Zero tokens or stack traces in UI/logcat. | NOT RUN |
| **4** | Valid student sign-in | Enter staging student credentials. Authenticates successfully; transitions to `AuthLevel.FULLY_AUTHENTICATED`. | NOT RUN |
| **5** | Session persistence test | Force-close app (swipe from Recents), re-launch. Restores session without route flash; returns directly to authenticated state. | NOT RUN |
| **6** | Password recovery request | Tap "Forgot password", enter test email. Shows safe notice: *"If an account exists for this email, password reset instructions have been sent."* | NOT RUN |
| **7** | Password recovery callback | Open recovery email on device, tap reset link (`com.mobidesk.android://auth-callback/recovery?...`). App opens, validates `/recovery`, sets `RECOVERY_ONLY`. Password update form displays; workspace is NOT loaded. | NOT RUN |
| **8** | Cancel recovery flow | On password update form, tap Cancel or Android Back. State transitions to `SIGNED_OUT`, recovery store cleared, login screen restored. | NOT RUN |
| **9** | Sign out | In authenticated session, tap Sign out. Session wiped locally (`SignOutScope.LOCAL`), UI resets to `SIGNED_OUT`, Android Back cannot navigate forward. | NOT RUN |
| **10** | Security & Logcat check | Inspect `adb logcat -d`. Proves zero passwords, tokens, PKCE verifiers, or recovery links appear in log. | NOT RUN |

---

## 5. Evidence Checklist & Redaction Protocol

- **Screenshots:** Capture and name:
  - `<date>-fp01-step01-launch.png`
  - `<date>-fp01-step03-error.png`
  - `<date>-fp01-step04-authenticated.png`
  - `<date>-fp01-step07-recovery.png`
  - `<date>-fp01-step09-signout.png`
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
APK SHA-256:             [Recorded from installed APK]
Device Model & OS:       [Recorded by Owner]
All Scenarios Passed:    [YES / NO]
Evidence Manifest Valid: [YES / NO]
Owner Decision:          [ ACCEPTED / REJECTED ]

Signed by:               __________________________________
Date:                    __________________________________
================================================================================
```
