# FP-01 Physical Device Live-Drill Checklist

**Feature Package:** FP-01 — Authentication, Session State & Recovery  
**Application ID:** `com.mobidesk.android`  
**Base Commit:** `84bb6b7e507d3871848f3386c6ceab47d4442c05`  
**Expected CI Artifact SHA-256:** `450aa3be0b510d3847d9a83a16be8ee7a196d2474eda2f58ededbc76e68d5206` (from CI Run 42450424147)  
**LIVE_DRILL_STATUS:** BLOCKED on NEG-03 (staging Supabase, SMTP, tester phone details)  
**Owner Sign-off Status:** UNACCEPTED (Awaiting physical demonstration on Android phone)  
**Operator:** Master43721 (Owner)  
**Verification Agent:** Antigravity  

---

## Live-Drill Protocol & Stop Rules

1. Each scenario requires real physical touch and navigation on the tester's Android phone.
2. Emulators, unit tests, and mock screenshots do not satisfy live-drill verification.
3. Steps are confirmed interactively one stage at a time.
4. Pass requires physical verification by the human tester.

---

## Preflight Environment & Physical Device Verification

| Environment Field | Value / Verification Requirement | Verified by Tester |
|:---|:---|:---:|
| **Physical Phone Model** | *(e.g. Google Pixel 8, Samsung Galaxy S23)* | [ ] |
| **Android OS Version & API** | Android API 29–37 *(e.g. Android 14 / API 34)* | [ ] |
| **Display & Font Scale** | Standard / 100% (or specify custom scaling) | [ ] |
| **Target Build Variant** | Debug APK (`app-debug.apk`) | [ ] |
| **Target ABI** | `arm64-v8a` (or `x86_64`) | [ ] |
| **Installation Command** | `adb install -r /path/to/downloaded/app-debug.apk` | [ ] |
| **APK Checksum Comparison** | `sha256sum app-debug.apk` matches `450aa3be...` | [ ] |
| **Network Condition** | Wi-Fi or Cellular with active Internet access | [ ] |
| **Backend Environment** | Supabase Staging (`kthfhjwltcpjhjemugwm.supabase.co`) | [ ] |

---

## Scenarios Matrix (16 Scenarios)

| Scenario # | Scenario Description | Required Input / Action / Exact ADB Command | Expected Result | Owner Verification |
|:---:|:---|:---|:---|:---:|
| **1** | Fresh installation and artifact checksum match | Compare SHA-256 hash of CI APK against expected `450aa3be...`. Install APK: `adb install -r /tmp/app-debug.apk`. Launch app. | Hash matches exactly. App boots cleanly without crashing, displays splash/branding, enters Sign-in screen. | [         ] |
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
