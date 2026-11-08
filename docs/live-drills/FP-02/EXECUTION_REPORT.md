# FP-02 Live-Drill Execution Report & Kit

**Feature Package:** FP-02 — Student Dashboard and Workspace  
**Application ID:** `com.mobidesk.android`  
**Target Repository:** `master43721/mobidesk-android`  
**Handoff Branch:** `fp-02-student-workspace` (commit `6992036b7c67d67d87324da2af283627d8c7673e`)  
**Current Live-Drill Status:** `LIVE_DRILL_STATUS: READY FOR OWNER`  
**Owner Sign-off Status:** UNACCEPTED (Awaiting physical demonstration on Android phone)  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md`  

---

## 1. Executive Summary

FP-02 provides the Student Workspace screen: greeting, authenticated user identifier, single Cloud PC card, live lifecycle and transport state rendering, monotonic receipt-age 15-second freshness indicator, refresh cooldown, and Connect gating with on-tap fresh verification per AMD-01.

All 55 automated unit tests pass on branch tip `6992036b7c67d67d87324da2af283627d8c7673e`. Automated CI Run 42450446908 completed with SUCCESS. Per the Terminal Acceptance Invariant, FP-02 remains unaccepted until the Project Owner executes this physical live drill on target hardware.

---

## 2. Preconditions & Prerequisites (Ordered)

1. **Staging Supabase Project (not production):**
   - Active staging project with student test accounts and verified VM assignment metadata.
   - `local.properties` contains `SUPABASE_PUBLISHABLE_KEY` and `SUPABASE_URL`.
2. **Physical Android Device:**
   - Physical phone model and Android version recorded by Owner (SDK 29–37).
   - USB debugging enabled.
3. **Staging Broker Endpoint:**
   - Deployed Session Broker exposing `GET /v1/vm-status` accessible from device network.

---

## 3. Exact Installation & Verification Steps

```bash
# 1. Download the CI-built debug APK artifact from GitHub Actions
gh run download 42450446908 -n mobidesk-debug-6992036b7c67d67d87324da2af283627d8c7673e --dir /tmp/mobidesk-fp02

# 2. Verify APK SHA-256 hash against CI build record
sha256sum /tmp/mobidesk-fp02/app-debug.apk

# 3. Connect physical phone via ADB and install
adb install -r /tmp/mobidesk-fp02/app-debug.apk

# 4. Verify installed package on device
adb shell pm list packages | grep com.mobidesk.android
```

---

## 4. Numbered Scenarios & Acceptance Criteria

| Step # | Scenario & Action | Acceptance Criteria (Expected Result) | Result |
|:---:|:---|:---|:---:|
| **1** | Authenticate into Workspace | Sign in with staging QA student account. Workspace displays student greeting, user identifier, and exactly one assigned Cloud PC card. | NOT RUN |
| **2** | Inspect Cloud PC card content | Card displays safe label (e.g. "Cloud PC (win-01)"). Shows NO VM UUID, private IP, MAC address, host node, or raw port. | NOT RUN |
| **3** | Initial live status observation | Status badge renders observed lifecycle (`RUNNING`, `STOPPED`, `STARTING`, `UNKNOWN`) and transport state. Matches broker API response. | NOT RUN |
| **4** | Monotonic freshness countdown | Watch freshness timer count from 15 seconds down to 0. Verifies age is derived from monotonic elapsed time, not device wall clock. | NOT RUN |
| **5** | Stale status transition | At expiry (15s elapsed), status transitions to "Stale". Connect button remains AVAILABLE per AMD-01 (ready to trigger a fresh check on tap). | NOT RUN |
| **6** | Manual Refresh & Cooldown | Tap "Refresh". Status is re-probed from broker; timestamp/freshness resets. Refresh button enters brief cooldown to prevent spam. | NOT RUN |
| **7** | Connect gate on fresh positive | When lifecycle is `RUNNING` and transport is `RDP_NEGOTIATION_OK`, tap Connect. App requests fresh status and initiates connection sequence. | NOT RUN |
| **8** | Connect gate on fresh negative | When lifecycle is `STOPPED` or transport is `TCP_UNREACHABLE`, Connect button is disabled with explanatory copy. | NOT RUN |
| **9** | Rate limit handling (HTTP 429) | Trigger rapid status checks to exceed rate limit. Broker returns 429 with `Retry-After`. App honors cooldown with safe `ASSIGN-02` copy. | NOT RUN |
| **10** | Assignment tag consistency | Verify card and status share identical opaque `assignmentTag`. No mismatched revisions rendered. | NOT RUN |

---

## 5. Evidence Checklist & Redaction Protocol

- **Screenshots:** Capture and name:
  - `<date>-fp02-step01-workspace.png`
  - `<date>-fp02-step03-fresh-status.png`
  - `<date>-fp02-step05-stale-status.png`
  - `<date>-fp02-step06-refreshed.png`
  - `<date>-fp02-step09-ratelimit.png`
- **SHA-256 Manifest:** Record each capture in `docs/live-drills/FP-02/evidence/manifest.sha256`.
- **Redaction Checklist:**
  - [ ] No private IP addresses or internal hostnames visible.
  - [ ] No student personal emails or IDs exposed.
  - [ ] No tokens or bearer credentials in logcat.

---

## 6. Abort & Rollback Procedure

If any step fails or an unexpected exception occurs:
1. Immediately halt the drill.
2. Record the defect in `docs/live-drills/FP-02/DEFECTS.md`.
3. Uninstall the test APK: `adb uninstall com.mobidesk.android`.
4. Transition `LIVE_DRILL_STATUS` to `FAIL` or `BLOCKED`.

---

## 7. Owner Sign-Off Block

```text
================================================================================
                    PROJECT OWNER LIVE-DRILL SIGN-OFF
================================================================================
Feature Point:           FP-02 — Student Dashboard and Workspace
Commit SHA:              6992036b7c67d67d87324da2af283627d8c7673e
APK SHA-256:             [Recorded from installed APK]
Device Model & OS:       [Recorded by Owner]
All Scenarios Passed:    [YES / NO]
Evidence Manifest Valid: [YES / NO]
Owner Decision:          [ ACCEPTED / REJECTED ]

Signed by:               __________________________________
Date:                    __________________________________
================================================================================
```
