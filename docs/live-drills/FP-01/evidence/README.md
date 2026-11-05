# FP-01 Live-Drill Evidence Index

**Feature Package:** FP-01 — Authentication, Session State & Recovery
**Target Application:** `com.mobidesk.android`
**Current Live-Drill Status:** `LIVE_DRILL_STATUS: BLOCKED`
**Privacy Mandate:** NEVER store passwords, bearer tokens, recovery links, service keys, student personal data, or private connection details.

---

## Evidence Manifest

| Evidence ID | Scenario # | Description | Artifact Type | File Name | Redaction Verification | Status |
|:---:|:---:|:---|:---:|:---:|:---:|:---:|
| `EVD-01-01` | 1 | Fresh launch / splash screen | Redacted Screenshot | `evd-01-01-launch.png` | Verified: No sensitive system/device info | PENDING DRILL |
| `EVD-01-02` | 2 | Login screen UI layout | Redacted Screenshot | `evd-01-02-login.png` | Verified: Clean UI, no demo/signup shortcuts | PENDING DRILL |
| `EVD-01-03` | 3 | Invalid credentials safe error | Redacted Screenshot | `evd-01-03-error.png` | Verified: Safe generic copy; no stack traces | PENDING DRILL |
| `EVD-01-04` | 4–5 | Provisioned login & Dashboard entry | Redacted Video / Stills | `evd-01-04-login-success.mp4` | Verified: Test student email; tokens redacted | PENDING DRILL |
| `EVD-01-05` | 6 | Session persistence across reopen | Terminal / Screen Log | `evd-01-05-persistence.log` | Verified: No session token leakage in logcat | PENDING DRILL |
| `EVD-01-06` | 7–8 | Recovery deep-link callback | Redacted Screenshot | `evd-01-06-recovery-form.png` | Verified: Exact `/recovery` path; code redacted | PENDING DRILL |
| `EVD-01-07` | 9 | Recovery cancellation | Redacted Screenshot | `evd-01-07-cancel.png` | Verified: State cleared to SIGNED_OUT | PENDING DRILL |
| `EVD-01-08` | 11–12 | Sign-out and back navigation | Redacted Video | `evd-01-08-signout.mp4` | Verified: Session wiped; no back to workspace | PENDING DRILL |
| `EVD-01-09` | 13 | Credential & log security audit | Logcat excerpt | `evd-01-09-logcat-audit.txt` | Verified: Zero secrets/tokens emitted | PENDING DRILL |

---
*Note: Evidence artifacts will be populated following Project Owner execution of the live demonstration upon resolution of build blockers DEF-001 and DEF-002.*
