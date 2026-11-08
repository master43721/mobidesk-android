# FP-04 Live VM Lifecycle and RDP Readiness — Progress & Iteration Record

**Feature Point:** FP-04 (Live VM Lifecycle and RDP Readiness)  
**Branch:** `fp-04-status-readiness`  
**Base:** `fp-03-assignment-resolution` @ `aacc3952c42d3aa05047ae9bc6ee7e12760cb5cf`  
**Status Label:** **IN PROGRESS (UNACCEPTED BASE: Staging candidate waiting for upstream FP-01/02 Live Drill acceptance)**  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md` and `AGENTS.md`  

---

## 1. Audit Gap Table (FR -> Existing Code -> Existing Test -> Gap & Resolution)

| Functional Requirement (FR) | PRD v1.2 / Spec Ref | Existing Android Code (`fp-02`) | Existing Automated Tests | Identified Client Gap & Resolution |
|:---|:---|:---|:---|:---|
| **FP04-FR-01 (Broker-Owned Lifecycle Status)** | PRD §5.1 / FR-VM-04 | `VmLifecycle` enum (STOPPED, STARTING, RUNNING, STOPPING, ERROR, UNKNOWN) | `StatusRepositoryContractTest`, `MobiDeskFp02Test` | **NONE (Complete).** All 6 lifecycle states represented and decoded from JSON. |
| **FP04-FR-02 (Separate RDP Transport Status)** | PRD §5.1 / FR-VM-04 | `RdpTransport` enum (NOT_CHECKED, TCP_UNREACHABLE, TCP_REACHABLE, RDP_NEGOTIATION_OK, UNKNOWN) | `StatusRepositoryContractTest`, `MobiDeskFp02Test` | **NONE (Complete).** All 5 transport states decoded distinctly from lifecycle. |
| **FP04-FR-03 (Student Copy Coverage for 11 States)** | PRD §4 / §12 / FP-02 §7 | `MobiDeskApp.kt` lines 1034–1068, `strings.xml` | `MobiDeskFp02Test` | **NONE (Complete).** Exact 1:1 string resources for all 11 lifecycle and transport states matching PRD catalogue. |
| **FP04-FR-04 (Bounded Stream & Fail-Closed Overflow)** | API Contract v1.1 | `DefaultStatusRepository.readBounded()` lines 137–152 (64 KB cap) | `StatusRepositoryContractTest.getVmStatus_oversizedPayloadFailsClosed` | **NONE (Complete).** Throws `ProtocolException` on overflow -> returns `StatusResult.Error`. |
| **FP04-FR-05 (Client Request Timeout)** | PRD §5.1 / Spec | `DefaultStatusRepository` connectTimeout=8000ms, readTimeout=8000ms | `StatusRepositoryContractTest.getVmStatus_timeoutReturnsError` | **NONE (Complete).** 8,000 ms budget comfortably exceeds server 1,500 ms probe budget. |
| **FP04-FR-06 (Monotonic Clock Freshness Tracking)** | PRD-1.2-AMD-01 | `MonotonicClock`, `SystemMonotonicClock`, `VmStatusSample.isFresh()` | `ConnectPolicyTest`, `MobiDeskFp02Test` | **NONE (Complete).** Freshness based exclusively on device monotonic receipt age (15s limit). |
| **FP04-FR-07 (HTTP 429 & Retry-After Handling)** | API Contract v1.1 | `DefaultStatusRepository` lines 84–109 | `StatusRepositoryContractTest.getVmStatus_rateLimitedWithRetryAfterHeaderAndBody` | **NONE (Complete).** Extracts Retry-After header / body, respects cooldown, uses `ASSIGN-02` error copy. |
| **FP04-FR-08 (Refresh Cooldown & No Polling)** | PRD §5.1 | `MobiDeskViewModel` cooldown timer logic | `MobiDeskFp02Test`, `MobiDeskViewModelTest` | **NONE (Complete).** Zero automatic polling loops; manual refresh enforced with cooldown. |
| **FP04-FR-09 (Connect Policy Freshness & Handoff)** | PRD-1.2-AMD-01 | `ConnectPolicy.isConnectAvailable()`, `ConnectPolicy.isHandoffEligible()` | `ConnectPolicyTest` (all 14 test cases) | **NONE (Complete).** Stale status allows Connect tap to trigger fresh probe; handoff requires RUNNING + RDP_NEGOTIATION_OK. |
| **FP04-FR-10 (Blank Access Token Fast-Fail)** | PRD §5.1 / Boundary | `DefaultStatusRepository.getVmStatus()` lines 33–35 | `StatusRepositoryContractTest.getVmStatus_blankAccessToken_failsImmediatelyAsAuthExpired` | **GAP RESOLVED.** Blank access tokens fail closed immediately as `AuthExpired` without firing futile HTTP requests. |
| **FP04-FR-11 (HTTP 403 Forbidden Fail-Closed)** | Contract v1.1 / Auth | `DefaultStatusRepository.getVmStatus()` lines 116–119 | `StatusRepositoryContractTest.getVmStatus_forbidden403_returnsAuthExpired` | **GAP RESOLVED.** HTTP 403 maps distinctly to non-transient `AuthExpired` rather than generic transient error. |

---

## 2. Development Loop Iteration 2 (2026-11-08)

### Loop A — Independent REVIEWER & ADVERSARY Critique
- **Reviewer Check:**
  - Evaluated `DefaultStatusRepository` fast-fail on blank access tokens and explicit 403 handling.
  - Independent review of `app/build.gradle` Compose BOM change (`androidTestImplementation platform('androidx.compose:compose-bom:2026.09.00')`): APPROVED. It aligns androidTest dependencies with runtime BOM version 2026.09.00 without impacting release APK size or runtime behavior.
- **Adversary Check:**
  - Attack scenario: Empty string passed as session token to status repository.
  - Mitigation: Evaluated before URL connection is opened; returns `StatusResult.AuthExpired` without sending unauthenticated requests.
  - Attack scenario: Revoked or unauthorized session returning HTTP 403.
  - Mitigation: Maps to `AuthExpired`, preventing misleading "Try again" loops.

### Loop B — Automated Test Execution
- **Task Executed:** `./gradlew :app:testDebugUnitTest`
- **Result:** **63/63 tests passed** (0 failures, 0 skipped).
- **New Tests Added (2 tests):** `com.mobidesk.android.StatusRepositoryContractTest`
  - `getVmStatus_blankAccessToken_failsImmediatelyAsAuthExpired`
  - `getVmStatus_forbidden403_returnsAuthExpired`
