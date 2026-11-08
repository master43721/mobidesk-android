# Proposed Amendment AMD-03 — Pilot Sessions Foreground-Only & Background Continuity Deferral

**Parent:** MobiDesk PRD v1.2 (dated 3 November 2026)  
**Proposed version:** v1.2.1 clarification or successor v1.3  
**Date:** 8 November 2026  
**Owner decision:** APPROVED FOR PILOT (Owner directive 2026-11-08) — Pilot sessions are strictly foreground-only; background continuity deferred.

---

## 1. Problem and Context

MobiDesk PRD v1.2 §6.5 and §6.7, alongside `FEATURE_INVENTORY.md` §7 (`FP-14 — RDP Session Lifecycle, Background Continuity and Cleanup`), specify that active RDP sessions persist when the student moves the MobiDesk Android application into the background, switching to a dedicated Android foreground service with persistent notification, reconnect countdown timers, and battery optimization exception prompts.

However, modern Android platforms (API levels 29 through 37) enforce aggressive foreground service type declarations (`dataSync`, `mediaProjection`, `connectedDevice`), OEM battery saver kills, memory trimming, and background network slicing. Attempting to ensure robust background RDP streaming across heterogeneous OEM hardware during the pilot introduces unpredictable connection drops, resource leakage, and high implementation complexity ahead of proving the basic foreground RDP pipeline.

---

## 2. Proposed Normative Wording

### 2.1 Foreground-Only Session Scope for Pilot
> For the R1 student pilot, active RDP sessions are strictly foreground-only:
> 1. An active RDP session requires that the MobiDesk session Activity remains in the foreground (`RESUMED` or visible `STARTED` state).
> 2. When the user navigates away from the application, switches apps, returns to the Android home screen, or minimizes the session, the application initiates an orderly, graceful session disconnect.
> 3. The disconnect releases the WebSocket connection to the broker, closes local socket/file descriptor resources, tears down the FreeRDP context, and securely erases ephemeral in-memory Windows credentials.
> 4. Background continuity services, persistent foreground notification services for background streaming, and background reconnect grace timers specified under FP-14 are deferred to post-pilot releases.

### 2.2 Reconnection Behavior
> When the student returns to MobiDesk after an app switch or pause:
> 1. The application presents the student workspace card with current Cloud PC status.
> 2. A fresh status probe is performed.
> 3. Tapping Connect requests a fresh one-use session grant from the broker and launches a new foreground session to the assigned Windows Cloud PC.
> 4. Windows session state on the remote VM remains intact per standard Windows RDP behavior (disconnected session resumes upon re-authentication with student guest credentials).

---

## 3. Impact Analysis

| Component | Original PRD v1.2 (FP-14) | Revised R1 Pilot Behavior (AMD-03) |
|:---|:---|:---|
| **Android Session Lifecycle** | Persistent foreground service across backgrounding | Session cleanly closed on activity `onStop()` / background |
| **Notification Management** | Required sticky ongoing notification for background session | Standard foreground activity; notification only during active session if required by OS |
| **Broker Resource Release** | Held active during background grace period (up to 120s) | Immediate WebSocket close frees broker relay slot and VM socket |
| **Credentials in Memory** | Retained in process memory while backgrounded | Cleared immediately on disconnect upon leaving foreground |
| **Battery & Data Impact** | High background consumption risks OEM kill | Zero background network or CPU consumption |

---

## 4. Security & Robustness Benefits

1. **Deterministic Cleanup:** Immediate session termination when backgrounded guarantees that orphaned WebSocket connections or TCP relay streams do not linger on the broker or VM.
2. **Reduced Credential Exposure Window:** Windows guest credentials in memory are discarded immediately upon backgrounding rather than sitting in memory in a backgrounded process.
3. **Audit Precision:** Every session disconnect maps directly to an explicit user departure, simplifying audit records and live drill verification.

---

## 5. Traceability and Decisions

- Parent PRD v1.2 text is preserved; this amendment records the approved pilot scope.
- Recorded in `docs/dev-loop/NEGOTIABLE_REGISTER.md` as NEG-08 (`ACCEPTED`, 2026-11-08).
- `docs/dev-loop/OWNER_DECISIONS_ROUND2.md` indexes AMD-03 as owner-confirmed.
