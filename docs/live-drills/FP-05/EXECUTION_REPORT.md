# FP-05..11 FreeRDP Native Pipeline & Rendering Live-Drill Execution Report

**Feature Package:** FP-05 through FP-11 — Native FreeRDP Remote Desktop Pipeline  
**Application / Subsystem:** `app` + `freeRDPCore`  
**Target Repository:** `master43721/mobidesk-android`  
**Handoff Branch:** `fp-05-freerdp-pipeline`  
**Exact Source Commit (HEAD SHA):** `[Recorded from Git HEAD]`  
**Current Live-Drill Status:** `LIVE_DRILL_STATUS: READY FOR OWNER`  
**Owner Sign-off Status:** UNACCEPTED (Awaiting physical demonstration)  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md`  

---

## 1. Executive Summary

This feature package establishes the complete end-to-end FreeRDP pipeline connecting the Android client to an assigned Windows guest VM:
1. **FP-05:** Dedicated Windows Guest Credential Prompt with mutable `CharArray` storage and zeroization.
2. **FP-06:** Broker Session Authorization and Single-Use Grant (`POST /v1/session-grants`).
3. **FP-07:** Secure RFC 6455 Binary WebSocket Relay (`GET /v1/rdp`).
4. **FP-08:** Protected FreeRDP Local Transport Bridge (`127.0.0.1:<ephemeral>` with anti-hijack listener closure).
5. **FP-09:** Native FreeRDP Engine and RDP Protocol Integration (NLA CredSSP handshake).
6. **FP-10:** Strict Leaf Certificate SHA-256 Fingerprint Pinning Verification.
7. **FP-11:** Dedicated Native RDP Session Activity (`FLAG_SECURE`, `SessionView`, `ScrollView2D`, floating toolbar).

Per the Terminal Acceptance Invariant, full acceptance requires the Project Owner's physical live drill sign-off on target hardware.

---

## 2. Numbered Scenarios & Acceptance Criteria

| Step # | Scenario & Action | Acceptance Criteria (Expected Result) | Result |
|:---:|:---|:---|:---:|
| **1** | Windows Guest Credential Prompt | Tap "Connect" on ready Cloud PC. In-session modal prompts for Windows lab username and password. Explicitly explains credentials differ from Supabase login. | NOT RUN |
| **2** | Credential Memory Zeroization | Submit or dismiss dialog. Mutable `CharArray` buffers are wiped with zeros (`\u0000`). No passwords in JVM strings or memory dumps. | NOT RUN |
| **3** | Single-Use Grant Issuance | App requests session grant from broker (`POST /v1/session-grants`). Broker returns single-use token, 60s TTL, and target VM SHA-256 certificate pin. | NOT RUN |
| **4** | WSS Relay Connection | App establishes RFC 6455 WebSocket connection with single-use bearer grant. Upgrades to 101 Switching Protocols. | NOT RUN |
| **5** | Protected Bridge Initialization | `LocalRdpBridge` starts loopback listener on dynamic port, accepts exactly 1 connection from FreeRDP, and immediately closes listener (anti-hijack). | NOT RUN |
| **6** | FreeRDP Engine Handshake | Pinned FreeRDP connects over local bridge; negotiates X.224, MCS channels, and executes NLA CredSSP authentication. | NOT RUN |
| **7** | Strict Certificate Fingerprint Verification | Server presents X.509 leaf certificate. `RdpCertificateVerifier` checks SHA-256 against session grant pin. Matching pin returns `2` (`ACCEPT_TEMPORARY`); mismatch aborts with `0` (`REJECT`). | NOT RUN |
| **8** | Native Desktop Viewport & FLAG_SECURE | `RdpSessionActivity` opens in immersive fullscreen. `FLAG_SECURE` blocks screenshots and task switcher previews. Decoded desktop bitmap renders in `SessionView`. | NOT RUN |
| **9** | Touch & Input Forwarding | Relative pointer, tap-to-click, and soft keyboard inputs transmit cursor and key events to remote VM. | NOT RUN |
| **10** | Deterministic Clean Teardown | User exits session. All sockets, relay streams, and worker threads terminate cleanly. Held credentials and session data zeroed. | NOT RUN |

---

## 3. Evidence Checklist & Redaction Protocol

- **Screenshots:**
  - `<date>-fp05-step01-cred-dialog.png`
  - `<date>-fp10-step07-cert-verified.png`
  - `<date>-fp11-step08-desktop-session.png`
  - `<date>-fp11-step10-cleanup.png`
- **SHA-256 Manifest:** Recorded in `docs/live-drills/FP-05/evidence/manifest.sha256`.
- **Redaction Protocol:**
  - [ ] Zero passwords or NLA hashes in device logcat.
  - [ ] Zero internal hostnames or IP addresses in student UI.

---

## 4. Owner Sign-Off Block

```text
================================================================================
                    PROJECT OWNER LIVE-DRILL SIGN-OFF
================================================================================
Feature Point:           FP-05..11 — FreeRDP Pipeline & Desktop Rendering
Commit SHA:              [Recorded by Owner from Git HEAD]
APK SHA-256:             [Recorded from installed APK]
Device Model & OS:       [Recorded by Owner]
All Scenarios Passed:    [YES / NO]
Evidence Manifest Valid: [YES / NO]
Owner Decision:          [ ACCEPTED / REJECTED ]
Signed by:               [Pending Owner Signature]
Date:                    [Awaiting Physical Demonstration]
================================================================================
```
