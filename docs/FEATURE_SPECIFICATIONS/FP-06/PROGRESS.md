# FP-06 Broker Session Authorization and One-Time Grant — Progress & Iteration Record

**Feature Point:** FP-06 (Broker Session Authorization and One-Time Grant)  
**Branch:** `fp-05-freerdp-pipeline`  
**Status Label:** **IN PROGRESS (UNACCEPTED BASE: Staging candidate waiting for physical Live Drill gate)**  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md` and `AGENTS.md`  

---

## 1. Audit Gap Table (FR -> Implementation -> Tests -> Resolution)

| Functional Requirement (FR) | PRD v1.2 / Spec Ref | Android Implementation | Automated Tests | Gap & Resolution |
|:---|:---|:---|:---|:---|
| **FP06-FR-01 (Single-Use Grant Issuance)** | PRD §5.2 / API Contract v1.1 §5 | `DefaultSessionGrantRepository.kt` (`POST /v1/session-grants`) | `SessionGrantRepositoryTest.successfulResponse_parsesGrantAndCertificatePin` | **Complete.** Authenticates with Supabase access JWT bearer token; receives single-use grant. |
| **FP06-FR-02 (Empty Request Body Rule)** | API Contract v1.1 §5 | `DefaultSessionGrantRepository.kt:48-52` | `SessionGrantRepositoryTest.successfulResponse_parsesGrantAndCertificatePin` | **Complete.** Strictly transmits empty JSON `{}` without any VM selector or credentials. |
| **FP06-FR-03 (Monotonic 60s Expiration Tracking)** | PRD §5.2 / API Contract v1.1 §5 | `SessionGrant.isExpired()`, `MonotonicClock` | `SessionGrantRepositoryTest.grantExpiration_monotonicAgeVerification` | **Complete.** Measures grant validity against monotonic elapsed clock rather than handset wall clock. |
| **FP06-FR-04 (Rate-Limit 429 & Retry-After)** | API Contract v1.1 §5 | `DefaultSessionGrantRepository.kt:95-103` | `SessionGrantRepositoryTest.rateLimited429_parsesRetryAfter` | **Complete.** Extracts `Retry-After` header and error payload, returning structured `SessionGrantResult.RateLimited`. |
| **FP06-FR-05 (401/403 Fail-Closed to AuthExpired)** | API Contract v1.1 §2 | `DefaultSessionGrantRepository.kt:104-106` | `SessionGrantRepositoryTest.unauthorized401_returnsAuthExpired`<br>`SessionGrantRepositoryTest.forbidden403_returnsAuthExpired` | **Complete.** Maps 401 and 403 to `AuthExpired` to cleanly reset authentication state. |
| **FP06-FR-06 (Bounded Stream & Buffer Safety)** | PRD §9 / Bounded I/O | `DefaultSessionGrantRepository.readBounded()` | `SessionGrantRepositoryTest` | **Complete.** 64 KiB read boundary prevents memory exhaustion on malformed server responses. |
