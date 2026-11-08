# SPK-01 FreeRDP Transport Spike Live-Drill Execution Report & Kit

**Feature Package:** SPK-01 — FreeRDP Native Transport I/O Integration Spike  
**Branch:** `spike/freerdp-transport` (Android repo; spike branch, never merges to main)  
**Target Hardware:** Physical Android Phone + Throwaway Windows VM (NLA enabled)  
**Current Live-Drill Status:** `LIVE_DRILL_STATUS: IN PROGRESS`  
**Owner Sign-off Status:** UNACCEPTED (Awaiting physical demonstration)  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md`  

---

## 1. Executive Summary

SPK-01 investigates and proves whether the pinned FreeRDP submodule (`d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1`) can connect over a custom I/O relay (WebSocket binary byte stream via file descriptor / socketpair bridge) with NLA authentication, strict certificate pinning, and zero credential leakage in logs.

Plumbing proof without Windows has been verified via `scripts/test_spk01_plumbing.py` (proves socketpair duplex bytes, UDS peer-UID verification, loopback hijack failure, and WebSocket framing to mock RDP target).

> **Important Boundary Notice:** The automated plumbing proof proves transport byte relay framing, buffering, and socketpair duplex data flow only. It does NOT prove NLA authentication, CredSSP exchange, or physical Android display rendering.

Per the Terminal Acceptance Invariant, the spike outcome cannot be marked GO or ACCEPTED until demonstrated on physical hardware by the Project Owner.

---

## 2. Preconditions & Prerequisites (Ordered)

1. **Throwaway Windows VM:**
   - Dedicated Windows VM instance with Network Level Authentication (NLA) enabled on TCP 3389.
   - Separate test guest credentials (username / domain / password).
   - Known SHA-256 leaf certificate fingerprint.
2. **Physical Android Device:**
   - Physical phone model and Android version recorded by Owner (SDK 29–37).
   - USB debugging enabled.
3. **Session Broker Instance:**
   - Running broker instance with WebSocket relay `/v1/rdp` routing to Windows VM.

---

## 3. Exact Installation & Verification Steps

```bash
# 1. Obtain test APK built from spike/freerdp-transport branch
gh run download <SPIKE_RUN_ID> -n mobidesk-spike-debug -d /tmp/mobidesk-spk01

# 2. Verify APK SHA-256 checksum
sha256sum /tmp/mobidesk-spk01/app-debug.apk

# 3. Install test APK on physical phone via ADB
adb install -r /tmp/mobidesk-spk01/app-debug.apk

# 4. Clear logcat before test run
adb logcat -c
```

---

## 4. Numbered Scenarios & Acceptance Criteria

| Step # | Scenario & Action | Acceptance Criteria (Expected Result) | Result |
|:---:|:---|:---|:---:|
| **1** | Bridge Socket Initialization | Adapter establishes local connected file descriptor / socketpair bridge before connect. No exposed unauthenticated loopback port. | VERIFIED (Plumbing script test 1 & 2 pass; hijack test confirms loopback rejection) |
| **2** | WebSocket Relay Attachment | Android client connects to broker `/v1/rdp` with single-use grant; bi-directional byte stream pipes to local bridge fd. | VERIFIED (Plumbing script test 4 pass; proves bytes only) |
| **3** | Strict Certificate Fingerprint Verification | Remote Windows VM presents X.509 leaf certificate. Client validates fingerprint against pinned SHA-256 from session grant. Matching cert proceeds; mismatched cert immediately aborts connection (4405). | NOT RUN (Requires physical VM) |
| **4** | Separate NLA Credential Prompt | In-session modal prompts for Windows guest credentials. Entered credentials passed directly to FreeRDP `OnAuthenticate`; not transmitted to broker or Supabase. | NOT RUN (Requires physical VM) |
| **5** | Interactive Desktop Session | FreeRDP graphics update stream renders Windows desktop bitmap on device display; touch/mouse input events reach VM. | NOT RUN (Requires physical VM) |
| **6** | Surface Lifecycle (Attach / Detach) | Rotate screen or minimize app. Native graphics handle surface change; MS-RDPEDISP display update succeeds or recovers cleanly. | NOT RUN |
| **7** | Clean Session Teardown | User exits session or disconnects. FreeRDP thread exits, local socketpair fds close, memory freed, zero lingering background threads. | NOT RUN |
| **8** | Memory & Logcat Redaction Audit | Inspect device logcat. Verifies zero passwords, NLA hashes, tokens, or private VM IPs appear in device logs. | NOT RUN |

---

## 5. Evidence Checklist & Redaction Protocol

- **Evidence Files:**
  - `<date>-spk01-step03-cert-valid.png`
  - `<date>-spk01-step03-cert-rejected.png`
  - `<date>-spk01-step05-desktop.png`
  - `<date>-spk01-step07-cleanup.log`
  - `<date>-spk01-step08-logcat-audit.txt`
- **SHA-256 Manifest:** Recorded in `docs/live-drills/SPK-01/evidence/manifest.sha256`.
- **Redaction Checklist:**
  - [ ] No Windows guest passwords in logs or screenshots.
  - [ ] No private desktop contents or student files.
  - [ ] No internal IP addresses.

---

## 6. Abort & Rollback Procedure

If crash, freeze, or certificate bypass occurs:
1. Immediately stop the drill.
2. Record defect in `docs/live-drills/SPK-01/DEFECTS.md`.
3. Uninstall test APK: `adb uninstall com.mobidesk.android`.
4. Transition `LIVE_DRILL_STATUS` to `FAIL` or `BLOCKED`.

---

## 7. Owner Sign-Off Block

```text
================================================================================
                    PROJECT OWNER LIVE-DRILL SIGN-OFF
================================================================================
Spike Package:           SPK-01 — FreeRDP Native Transport I/O Spike
Commit SHA:              [Recorded by Owner from spike/freerdp-transport HEAD]
All Scenarios Passed:    [YES / NO]
Spike Verdict:           [ GO (Adopt FreeRDP) / NO-GO (Pivot Transport) ]
Owner Decision:          [ ACCEPTED / REJECTED ]

Signed by:               __________________________________
Date:                    __________________________________
================================================================================
```
