# FP-04 Live VM Lifecycle and RDP Readiness — Progress & Iteration Record

**Feature Point:** FP-04 (Live VM Lifecycle and RDP Readiness)  
**Branch:** `fp-04-status-readiness`  
**Base:** `fp-03-assignment-resolution` @ `6a80d4dc0cf797b62266aca4e889b90abd51a323`  
**Status Label:** **IN PROGRESS (UNACCEPTED BASE: Staging candidate waiting for upstream FP-01/02 Live Drill acceptance)**  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md` and `AGENTS.md`  

---

## 1. Audit Gap Table (FR -> Existing Code -> Existing Test -> Gap)

| Functional Requirement (FR) | PRD v1.2 / Spec Ref | Existing Android Code (`fp-02`) | Existing Automated Tests | Identified Client Gap |
|:---|:---|:---|:---|:---|
| **FP04-FR-01 (Broker-Owned Lifecycle Status)** | PRD §5.1 / FR-VM-04 | `VmLifecycle` enum (STOPPED, STARTING, RUNNING, STOPPING, ERROR, UNKNOWN) | `StatusRepositoryContractTest`, `MobiDeskFp02Test` | **NONE (Complete).** All 6 lifecycle states represented and decoded from JSON. |
| **FP04-FR-02 (Separate RDP Transport Status)** | PRD §5.1 / FR-VM-04 | `RdpTransport` enum (NOT_CHECKED, TCP_UNREACHABLE, TCP_REACHABLE, RDP_NEGOTIATION_OK, UNKNOWN) | `StatusRepositoryContractTest`, `MobiDeskFp02Test` | **NONE (Complete).** All 5 transport states decoded distinctly from lifecycle. |
| **FP04-FR-03 (Student Copy Coverage for 11 States)** | PRD §4 / §12 / FP-02 §7 | `MobiDeskApp.kt` lines 1034–1068, `strings.xml` | `MobiDeskFp02Test` | **NONE (Complete).** Exact 1:1 string resources for all 11 lifecycle and transport states matching PRD catalogue. |
| **FP04-FR-04 (Bounded Stream & Fail-Closed Overflow)** | API Contract v1.1 | `DefaultStatusRepository.readBounded()` lines 137–152 (64 KB cap) | `StatusRepositoryContractTest.responseExceedingByteLimitFailsClosed` | **NONE (Complete).** Throws `ProtocolException` on overflow -> returns `StatusResult.Error`. |
| **FP04-FR-05 (Client Request Timeout)** | PRD §5.1 / Spec | `DefaultStatusRepository` connectTimeout=8000ms, readTimeout=8000ms | `StatusRepositoryContractTest.socketTimeoutReturnsFailClosedError` | **NONE (Complete).** 8,000 ms budget comfortably exceeds server 1,500 ms probe budget. |
| **FP04-FR-06 (Monotonic Clock Freshness Tracking)** | PRD-1.2-AMD-01 | `MonotonicClock`, `SystemMonotonicClock`, `VmStatusSample.isFresh()` | `ConnectPolicyTest`, `MobiDeskFp02Test` | **NONE (Complete).** Freshness based exclusively on device monotonic receipt age (15s limit). |
| **FP04-FR-07 (HTTP 429 & Retry-After Handling)** | API Contract v1.1 | `DefaultStatusRepository` lines 84–109 | `StatusRepositoryContractTest.rateLimit429WithRetryAfterHeader` | **NONE (Complete).** Extracts Retry-After header / body, respects cooldown, uses `ASSIGN-02` error copy. |
| **FP04-FR-08 (Refresh Cooldown & No Polling)** | PRD §5.1 | `MobiDeskViewModel` cooldown timer logic | `MobiDeskFp02Test`, `MobiDeskViewModelTest` | **NONE (Complete).** Zero automatic polling loops; manual refresh enforced with cooldown. |
| **FP04-FR-09 (Connect Policy Freshness & Handoff)** | PRD-1.2-AMD-01 | `ConnectPolicy.isConnectAvailable()`, `ConnectPolicy.isHandoffEligible()` | `ConnectPolicyTest` (all 14 test cases) | **NONE (Complete).** Stale status allows Connect tap to trigger fresh probe; handoff requires RUNNING + RDP_NEGOTIATION_OK. |

**Verdict:** Client-side status repository, HTTP client, bounded reader, monotonic timer, and contract test suite are complete and verified against OpenAPI/Contract v1.1. Server-side `GET /v1/vm-status` probe is implemented on `session-broker` branch `broker/catch-up-v1.1`.

---

## 2. Development Loop Iteration 1 (2026-11-08)

### Loop A — Review and Adversary Self-Critique
- **Readiness:** IN PROGRESS (UNACCEPTED BASE)
- **Findings & Severity:** 0 BLOCKER, 0 MAJOR, 0 MINOR.
- **Reviewer Check:** Verified contract test suite exercises status DTO parsing, 429 handling, socket timeout, byte overflow, and auth expiration.
- **Adversary Check:** Verified no client-side destination tampering, monotonic time cannot be skewed by wall-clock changes, and zero IP/hostname leakage.
- **Remaining NEGOTIABLE items:** NEG-01 through NEG-09.

### Loop B — Automated Test Execution
- **Task Executed:** `./gradlew :app:testDebugUnitTest`
- **Result:** 55/55 tests passed (0 failures, 0 skipped).
