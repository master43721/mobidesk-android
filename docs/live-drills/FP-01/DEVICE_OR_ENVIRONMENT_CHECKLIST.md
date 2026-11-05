# FP-01 Physical Device Live-Drill Checklist

**Feature Package:** FP-01 — Authentication, Session State & Recovery
**Target Application:** `com.mobidesk.android`
**Base Commit:** `cf01c89deb7dd0b0cb4a272811d9b322f33a93e8`
**Execution Status:** NOT RUN — Human Verification Pending
**LIVE_DRILL_STATUS:** BLOCKED (Scenarios: NOT RUN — Physical verification pending build & config resolution)
**Operator:** Master43721 (Owner)
**Verification Agent:** Antigravity

---

## Live-Drill Protocol & Stop Rules

1. Each scenario requires real physical touch and navigation on the tester's Android phone.
2. Emulators, unit tests, and mock screenshots do not satisfy live-drill verification.
3. Steps are confirmed interactively one stage at a time.
4. Pass requires physical verification by the human tester.

---

## Scenarios Matrix

| Scenario # | Scenario Description | Required Input / Action | Expected Result | Result | Evidence / Notes |
|:---:|:---|:---|:---|:---:|:---|
| **1** | Fresh installation and first launch | Install debug APK via ADB or file download on physical Android device; tap application icon to launch. | App boots cleanly without crashing, displays splash/branding, enters Sign-in screen. | NOT RUN | Gated on APK build & install. |
| **2** | Unauthenticated state and login screen | Inspect fresh launch interface. | Displays "Sign in", email and password inputs, forgot password link. No preview/demo shortcuts, no signup affordances, no fake bypass. | NOT RUN | Requirement FP01-FR-001, FP01-FR-018. |
| **3** | Invalid credentials & safe error handling | Enter valid format email and wrong password; tap "Sign in". | Form displays generic, stable notice: "The email or password is incorrect." No backend stack traces or internal Supabase details leaked. | NOT RUN | Requirement FP01-FR-001, FP01-FR-015. |
| **4** | Valid QA account authentication | Enter provisioned QA account email and password; tap "Sign in". | Progress indicator displays during busy state, authenticates successfully without error. | NOT RUN | Requirement FP01-FR-001. |
| **5** | `FULLY_AUTHENTICATED` transition & workspace boundary | Observe state transition after valid sign-in. | App establishes `AuthLevel.FULLY_AUTHENTICATED`. Navigates to Dashboard/Workspace with active student session. | NOT RUN | Requirement FP01-FR-003, FP01-FR-005. |
| **6** | Session persistence after closing and reopening | Force-close the application (swipe away from Android Recents), then tap app icon to reopen. | Initializing session restoration executes without premature signed-out route flash; restores session and lands directly on authenticated Dashboard. | NOT RUN | Requirement FP01-FR-002. |
| **7** | Password recovery request & email dispatch | From login screen, tap "Forgot password", enter QA account email, tap "Send recovery link". | Generic safe acknowledgement displays: "If an account exists for this email, password reset instructions have been sent." (Account enumeration prevention). | NOT RUN | External Gate: Custom SMTP / org mailbox. |
| **8** | Deep-link callback & password update form | Tap recovery link in email on device (`com.mobidesk.android://auth-callback/recovery?...`). | App opens, validates exact `/recovery` path, sets `RECOVERY_ONLY` level. Recovery update form renders. Workspace is NOT loaded. | NOT RUN | Requirement FP01-FR-004, FP01-FR-010. |
| **9** | Recovery cancellation / Back navigation | On password update screen, tap Cancel or Android system Back button. | Calls `signOutCurrentSession()`, clears recovery store, resets state to `SIGNED_OUT`, returns to Login. Workspace is never exposed. | NOT RUN | Requirement FP01-FR-007. |
| **10** | Session expiry / refresh failure handling | Simulate session invalidation or revocation. | App detects expired session, clears stored session, routes safely to `SIGNED_OUT` with clear notification. | NOT RUN | Requirement FP01-FR-003. |
| **11** | Sign-out and clearing of session state | In authenticated Dashboard/Settings, tap "Sign out". | Executes `signOut(SignOutScope.LOCAL)`. Wipes local tokens, resets UI state to `SIGNED_OUT`, navigates to Login screen. | NOT RUN | Requirement FP01-FR-008. |
| **12** | App reopening and Back navigation after sign-out | Press Android system Back button; force-close and reopen app after sign-out. | Android Back does not navigate forward to protected Dashboard. Reopening app opens Login screen. No stale session restored. | NOT RUN | Requirement FP01-FR-002, FP01-FR-008. |
| **13** | Credential security & logging integrity | Inspect device logcat and application UI during and after all flows. | No passwords, access tokens, refresh tokens, PKCE verifiers, or raw callback URLs are logged or displayed. | NOT RUN | Requirement FP01-FR-015. |
