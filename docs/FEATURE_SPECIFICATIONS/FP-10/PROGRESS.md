# FP-10 RDP Server Certificate Identity — Progress & Iteration Record

**Feature Point:** FP-10 (RDP Server Certificate Identity and Verification)  
**Branch:** `fp-05-freerdp-pipeline`  
**Status Label:** **IN PROGRESS (UNACCEPTED BASE: Staging candidate waiting for physical Live Drill gate)**  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md` and `AGENTS.md`  

---

## 1. Audit Gap Table (FR -> Implementation -> Tests -> Resolution)

| Functional Requirement (FR) | PRD v1.2 / Spec Ref | Android Implementation | Automated Tests | Gap & Resolution |
|:---|:---|:---|:---|:---|
| **FP10-FR-01 (Strict Leaf SHA-256 Pinning)** | PRD §6.7 / API Contract v1.1 §5 | `RdpCertificateVerifier.kt` | `RdpCertificateVerifierTest.exactMatchingFingerprint_returnsAcceptTemporary` | **Complete.** Validates presented certificate fingerprint against pinned SHA-256 delivered in session grant. |
| **FP10-FR-02 (FreeRDP Status Code Mapping)** | FreeRDP API Audit §3 | `RdpCertificateVerifier.kt:52-54` | `RdpCertificateVerifierTest` | **Complete.** Returns `2` (`ACCEPT_TEMPORARY`) on match, `0` (`REJECT`) on mismatch. |
| **FP10-FR-03 (Format Normalization)** | PRD §6.7 | `RdpCertificateVerifier.normalizeFingerprint()` | `RdpCertificateVerifierTest.matchingFingerprint_withColonsAndUpperCase_normalizesAndAccepts`<br>`RdpCertificateVerifierTest.matchingFingerprint_withDashesAndSpaces_normalizesAndAccepts` | **Complete.** Normalizes colons, dashes, spaces, and uppercase hex characters to standard lowercase 64-char string. |
| **FP10-FR-04 (Mismatched Fingerprint Fail-Closed)** | PRD §6.7 | `RdpCertificateVerifier.verify()` | `RdpCertificateVerifierTest.mismatchedFingerprint_failsClosedWithReject` | **Complete.** Any fingerprint divergence immediately returns `REJECT` (0). |
| **FP10-FR-05 (Null, Blank or Invalid Length Fail-Closed)** | Robustness / Defensive | `RdpCertificateVerifier.verify()` | `RdpCertificateVerifierTest.nullOrBlankFingerprint_failsClosed`<br>`RdpCertificateVerifierTest.invalidLengthFingerprint_failsClosed` | **Complete.** Missing, empty, or non-64-char hashes fail closed without exception. |
| **FP10-FR-06 (No Accept-All Bypass UI)** | PRD §6.7 | `RdpCertificateVerifier.kt`, `RdpSessionActivity.kt` | `scan-secrets-logs.sh` | **Complete.** Zero "continue anyway" or trust-on-first-use prompt exposed to user. |
