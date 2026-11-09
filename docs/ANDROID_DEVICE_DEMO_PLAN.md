# MobiDesk — Physical Android Device Demonstration Plan

**Document Version:** 1.0
**Date:** 8 November 2026
**Auditor / Lead Engineer:** Jules (AI Software Engineer)
**Target Hardware:** Physical Android Handset (SDK 29–37, arm64-v8a)
**Governance Authority:** `AGENTS.md` & `docs/MANDATORY_LIVE_DRILL_POLICY.md`
**Candidate Revision:** `fp-04-status-readiness` @ commit `1cba5876ee163ef36d9e3cd505112fb24fb6a568`

---

## 1. Demonstration Scope & Honest System Boundaries

> [!IMPORTANT]
> **Honest Engineering Boundaries:**
> This demonstration is specifically designed to prove the **foundation stack** (FP-01, FP-02, FP-03, FP-04) running natively on physical hardware.
>
> - **WHAT IS DEMONSTRATED:**
>   1. Real Supabase student authentication, discrete session levels, token storage in Android Keystore encrypted preferences, PKCE deep-link password recovery, and offline sign-out.
>   2. Real RLS-scoped student profile and VM assignment resolution enforcing the strict single-assignment invariant without exposing private VM hostnames, internal IPs, or database identifiers.
>   3. Live VM lifecycle and RDP readiness status presentation with 64 KB stream protection, monotonic clock freshness evaluation (15-second expiration), and manual refresh cooldown.
>   4. Dynamic Connect button availability gating based on authoritative readiness and staleness.
>
> - **WHAT IS NOT YET INCLUDED (DO NOT CLAIM AS WORKING):**
>   - **Remote Desktop Session Launch:** FP-05 (guest credential prompt), FP-06 (one-time broker grant), FP-07 (WSS relay), and FP-08..11 (FreeRDP protocol negotiation and native desktop rendering) are not yet integrated into this APK. Tapping "Connect" triggers handoff eligibility evaluation and navigates to the connect transition seam, but will not establish a live Windows RDP desktop stream.
>   - **R2 Dock Streaming:** The Dock section is purely an informational placeholder per PRD v1.2.

---

## 2. Hardware and Environment Prerequisites

1. **Target Android Smartphone:**
   - Architecture: `arm64-v8a` (standard modern 64-bit Android phone).
   - Android OS: Android 10 through Android 15 (API 29–35, or preview 36/37).
   - USB Debugging enabled (Settings -> Developer Options -> USB Debugging: ON).
2. **Workstation / Laptop:**
   - Android SDK Platform-Tools installed (`adb` available in `$PATH`).
   - USB-C cable connecting phone to workstation.
3. **Staging Supabase Project:**
   - Active Supabase instance with database migrations applied (`students`, `vms`).
   - Exactly one student record provisioned with an assigned VM.
   - Project URL and Publishable Key ready.
4. **Staging Status Broker Service (Optional / Staging):**
   - Session broker service running on reachable local network IP or Cloudflare tunnel providing `GET /v1/vm-status`.
   - If broker is unreachable, app must be observed handling network timeout gracefully (`ASSIGN-02`).

---

## 3. Step-by-Step Live Demonstration Scenarios

### Scenario Block A: Authentication, Security & Recovery (FP-01)

| Step # | Action / User Interaction | Expected System Reaction | Pass Criteria |
|:---:|:---|:---|:---:|
| **A.1** | **Cold Launch:** Tap the MobiDesk icon on the phone home screen. | App boots cleanly without flashing unauthenticated screens; displays brand lockup and enters Sign-in screen. | Splash loads, no crashes, email/password fields visible. |
| **A.2** | **Invalid Credentials:** Enter `test@college.edu` and incorrect password `WrongPassword123!`; tap "Sign in". | Form shows generic safe error: "The email or password is incorrect." | Zero Supabase internal codes or stack traces leaked. |
| **A.3** | **Valid Authentication:** Enter provisioned staging student credentials; tap "Sign in". | Shows progress indicator, authenticates with Supabase Auth, transitions to Student Dashboard. | Lands on authenticated student workspace. |
| **A.4** | **Force Close & Persistence:** Swipe MobiDesk away from Android Recents, then relaunch from home screen. | App restores valid session from Keystore-backed encrypted storage and lands directly on Student Dashboard without flashing login. | No login prompt shown; session restored. |
| **A.5** | **Password Recovery Request:** From Sign-in, tap "Forgot password", enter test email, tap "Send recovery link". | Safe notice appears: "If an account exists for this email, password reset instructions have been sent." | Account enumeration prevented. |
| **A.6** | **PKCE Recovery Callback:** Open recovery link on device (`com.mobidesk.android://auth-callback/recovery?code=...`). | Deep link opens app directly to Password Update screen. Sets `AuthLevel.RECOVERY_ONLY`. | Workspace is NOT loaded. |
| **A.7** | **Process Death on Recovery:** While on Password Update screen, kill process via ADB (`adb shell am kill com.mobidesk.android`) and relaunch. | App fails closed to `RecoveryInterruptedScreen`, clears local tokens, requires restart from Login. | Interrupted recovery state handled safely. |
| **A.8** | **Offline Sign-Out:** While signed in, enable Airplane Mode on phone. Tap "Sign out". | App executes local sign-out fallback, wipes Keystore encrypted tokens, and resets UI to `SIGNED_OUT`. | User is safely signed out locally even without internet. |

---

### Scenario Block B: Student Assignment Resolution (FP-03)

| Step # | Action / User Interaction | Expected System Reaction | Pass Criteria |
|:---:|:---|:---|:---:|
| **B.1** | **Authorized Single Assignment:** Sign in with student account assigned exactly one VM (`win-01`). | Student Dashboard displays student name, college name, and Cloud PC card with VM display label (`win-01`). | VM display label shown; internal VM UUID, IP address, and RDP port are completely hidden. |
| **B.2** | **Unassigned Student Account:** Sign in with student account that has zero rows in `vms`. | App displays safe unassigned screen: "No Cloud PC is assigned to your account. Contact the lab administrator." | Fails closed with `ASSIGN-01`; no connect affordance shown. |
| **B.3** | **Conflict / Corrupt Data:** Simulate duplicate assignment in database. | App displays safe error screen: "Your Cloud PC assignment needs administrator review." | Fails closed with `ASSIGN-03`; multi-assignment rejected. |

---

### Scenario Block C: Live Status Readiness & Monotonic Freshness (FP-04)

| Step # | Action / User Interaction | Expected System Reaction | Pass Criteria |
|:---:|:---|:---|:---:|
| **C.1** | **Initial Status Probe:** Upon entering dashboard, app requests status. | Shows "Checking status…". When broker returns 200, displays power state (e.g. "Powered on") and transport state (e.g. "Ready for Windows sign-in"). | Badges reflect exact JSON payload without inventing states. |
| **C.2** | **Monotonic Freshness Aging:** Observe Cloud PC card without touching screen for 15 seconds. | Timestamp updates dynamically: "Updated just now" -> "Updated 6 seconds ago" -> "Status needs refreshing" at the 15-second mark. | Zero network requests sent during aging (proves no-polling policy). |
| **C.3** | **Manual Refresh & Cooldown:** Tap "Refresh status". Immediately attempt to tap it again. | First tap refreshes status. Second tap is blocked while cooldown timer is active (or shows button in busy state). | Enforces 429 rate-limit protection. |
| **C.4** | **Power Off / Unreachable State:** Simulate VM stopped or port unreachable. | Lifecycle badge displays "Powered off" (`STOPPED`) or "Cannot reach remote desktop service" (`TCP_UNREACHABLE`). | Safe copy from strings.xml displayed. |

---

### Scenario Block D: Connect Availability Orchestration (FP-02)

| Step # | Action / User Interaction | Expected System Reaction | Pass Criteria |
|:---:|:---|:---|:---:|
| **D.1** | **Connect Gating on Known-Negative:** Status is fresh (<15s) and VM is `STOPPED`. | "Connect" button is visually disabled. | Student cannot trigger a futile connection attempt. |
| **D.2** | **Connect Availability on Stale:** Status is stale (>15s) and VM was previously `STOPPED`. | "Connect" button becomes enabled. | Tapping Connect allows user to trigger a fresh status check before connecting. |
| **D.3** | **Connect Availability on Ready:** Status is fresh (<15s), lifecycle is `RUNNING`, and transport is `RDP_NEGOTIATION_OK`. | "Connect" button is enabled with primary highlight styling. | Reflects full connection eligibility. |

---

## 4. Evidence Capture and Logging Instructions

During execution of the above scenarios, the Project Owner records:
1. **Redacted Screenshots:**
   - `step-a1-login.png`
   - `step-a3-dashboard.png`
   - `step-a7-interrupted.png`
   - `step-c1-status-ready.png`
   - `step-c2-status-stale.png`
2. **Logcat Capture (Sensitive Data Leakage Audit):**
   ```bash
   adb logcat -d | grep -E "(password|access_token|refresh_token|code_verifier|supabase_key)"
   ```
   *Pass Criteria:* Output must be completely empty (0 leaked secrets).
3. **Sign-off Completion:**
   Record observations in `docs/live-drills/FP-01/EXECUTION_REPORT.md` and `docs/live-drills/FP-02/EXECUTION_REPORT.md`.
