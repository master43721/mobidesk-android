# FP-03 Student-to-VM Assignment Resolution — Progress & Iteration Record

**Feature Point:** FP-03 (Student-to-VM Assignment Resolution and Authorization)  
**Branch:** `fp-03-assignment-resolution`  
**Base:** `fp-02-student-workspace` @ `ee3f7970db26f8ee8dfceea4353e58095f04fcc7`  
**Status Label:** **IN PROGRESS (UNACCEPTED BASE: Staging candidate waiting for upstream FP-01/02 Live Drill acceptance)**  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md` and `AGENTS.md`  

---

## 1. Audit Gap Table (FR -> Existing Code -> Existing Test -> Gap)

| Functional Requirement (FR) | PRD v1.2 / Spec Ref | Existing Android Code (`fp-02`) | Existing Automated Tests | Identified Client Gap |
|:---|:---|:---|:---|:---|
| **FP03-FR-01 (Authenticated Gating)** | PRD §5.1 / FR-AUTH-01 | `MobiDeskViewModel.kt` lines 270–290 (`authLevel == FULLY_AUTHENTICATED`) | `MobiDeskViewModelTest` (tests 1–6) | **NONE (Complete).** Workspace and assignments are inaccessible under unauthenticated or recovery states. |
| **FP03-FR-02 (RLS Profile & Assignment Query)** | PRD §5.1 / FR-AUTH-02 | `StudentWorkspaceRepository.loadFor()` lines 74–90 | `MobiDeskFp02Test` / `MobiDeskViewModelTest` | **NONE (Complete).** Direct client queries use user session token; zero service-role keys. |
| **FP03-FR-03 (Exactly-One Rule)** | PRD §5.1 / FR-VM-01 | `StudentWorkspaceRepository.loadFor()` lines 91–105 | `MobiDeskViewModelTest` (valid assignment scenario) | **NONE (Complete).** Single assigned VM maps to `StudentWorkspaceResult.Assigned`. |
| **FP03-FR-04 (Zero Assignment Fail-Closed)** | PRD §5.1 / FR-VM-02 | `StudentWorkspaceRepository.loadFor()` line 92 | `MobiDeskViewModelTest` (empty VM list -> ASSIGN-01 copy) | **NONE (Complete).** Maps to `StudentWorkspaceResult.NoAssignment` with `error_assign_01`. |
| **FP03-FR-05 (Multiple / Duplicate Assignment Fail-Closed)** | PRD §5.1 / FR-VM-03 | `StudentWorkspaceRepository.loadFor()` lines 101–104 | `MobiDeskViewModelTest` (conflict -> ASSIGN-03 copy) | **NONE (Complete).** Maps to `StudentWorkspaceResult.AssignmentConflict` with `error_assign_03`. |
| **FP03-FR-06 (No VM ID / Host / IP / Port in Client Requests)** | PRD §5.1 / SEC-AUTH-02 | `StudentWorkspaceRepository.kt`, `DefaultStatusRepository.kt` | `validate-fake-broker-contract.sh`, `scan-secrets-logs.sh` | **NONE (Complete).** No request parameters contain VM identifiers or network endpoints. |
| **FP03-FR-07 (AssignmentTag Binding)** | PRD-1.2-AMD-01 | `AssignedWorkspace.assignmentTag`, `ConnectPolicy.isHandoffEligible` | `ConnectPolicyTest`, `StatusRepositoryContractTest` | **NONE (Complete).** `assignmentTag` binds Cloud PC card to VM status probe. |

**Verdict:** The entire client-side assignment resolution logic and fail-closed state machines were designed and integrated cleanly into the repository seams during FP-02. No duplicate client code is required. Server-side allowlist and mapping live in `session-broker` (completed on branch `broker/catch-up-v1.1`).

---

## 2. Development Loop Iteration 1 (2026-11-08)

### Loop A — Review and Adversary Self-Critique
- **Readiness:** IN PROGRESS (UNACCEPTED BASE)
- **Findings & Severity:** 0 BLOCKER, 0 MAJOR, 0 MINOR.
- **Reviewer Check:** Verified all 7 FRs have direct test coverage in `MobiDeskViewModelTest` and `MobiDeskFp02Test`.
- **Adversary Check:** Verified zero client-side VM destination parameters, zero service-role keys, and fail-closed handling on corrupt or duplicate rows.
- **Remaining NEGOTIABLE items:** NEG-01 through NEG-09 as recorded in `docs/dev-loop/NEGOTIABLE_REGISTER.md`.

### Loop B — Automated Test Execution
- **Task Executed:** `./gradlew :app:testDebugUnitTest`
- **Result:** 55/55 tests passed (0 failures, 0 skipped).
