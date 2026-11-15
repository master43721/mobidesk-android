# FP-03 Student-to-VM Assignment Resolution — Progress & Iteration Record

**Feature Point:** FP-03 (Student-to-VM Assignment Resolution and Authorization)  
**Branch:** `fp-03-assignment-resolution`  
**Base:** `fp-02-student-workspace` @ `c532be1603908d559c8c9bf83379c03243129d76`  
**Status Label:** **IN PROGRESS (UNACCEPTED BASE: Staging candidate waiting for upstream FP-01/02 Live Drill acceptance)**  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md` and `AGENTS.md`  

---

## 1. Audit Gap Table (FR -> Existing Code -> Existing Test -> Gap & Resolution)

| Functional Requirement (FR) | PRD v1.2 / Spec Ref | Existing Android Code (`fp-02`) | Existing Automated Tests | Identified Client Gap & Resolution |
|:---|:---|:---|:---|:---|
| **FP03-FR-01 (Authenticated Gating)** | PRD §5.1 / FR-AUTH-01 | `MobiDeskViewModel.kt:270-290` (`authLevel == FULLY_AUTHENTICATED`) | `MobiDeskViewModelTest` (tests 1–6) | **NONE (Complete).** Workspace and assignments are inaccessible under unauthenticated or recovery states. |
| **FP03-FR-02 (RLS Profile & Assignment Query)** | PRD §5.1 / FR-AUTH-02 | `StudentWorkspaceRepository.loadFor()` lines 74–90 | `MobiDeskFp02Test` / `MobiDeskViewModelTest` | **NONE (Complete).** Direct client queries use user session token; zero service-role keys. |
| **FP03-FR-03 (Exactly-One Rule)** | PRD §5.1 / FR-VM-01 | `StudentWorkspaceRepository.resolve()` lines 95–130 | `StudentAssignmentResolutionTest.testSingleValidVm_returnsAssignedWithDerivedTag` | **NONE (Complete).** Single assigned VM maps to `StudentWorkspaceResult.Assigned`. |
| **FP03-FR-04 (Zero Assignment Fail-Closed)** | PRD §5.1 / FR-VM-02 | `StudentWorkspaceRepository.resolve()` line 105 | `StudentAssignmentResolutionTest.testZeroAssignedVms_returnsNoAssignment` | **NONE (Complete).** Maps to `StudentWorkspaceResult.NoAssignment` with `error_assign_01`. |
| **FP03-FR-05 (Multiple / Duplicate Assignment Fail-Closed)** | PRD §5.1 / FR-VM-03 | `StudentWorkspaceRepository.resolve()` lines 122–126 | `StudentAssignmentResolutionTest.testMultipleAssignedVms_failsClosedAsAssignmentConflict` | **NONE (Complete).** Maps to `StudentWorkspaceResult.AssignmentInvalid` with `error_assign_03`. |
| **FP03-FR-06 (Corrupt Blank VM Record Fail-Closed)** | PRD §5.1 / Robustness | `StudentWorkspaceRepository.resolve()` lines 107–113 | `StudentAssignmentResolutionTest.testBlankVmId_failsClosedAsAssignmentInvalid`<br>`StudentAssignmentResolutionTest.testBlankVmName_failsClosedAsAssignmentInvalid` | **GAP RESOLVED.** Blank VM id or blank VM name now explicitly fails closed as `AssignmentInvalid` instead of fabricating malformed tags. |
| **FP03-FR-07 (Blank User Identifier Guard)** | PRD §5.1 / Boundary | `StudentWorkspaceRepository.loadFor()` lines 76–78 | `StudentAssignmentResolutionTest.testNullProfile_returnsAccountNotProvisioned` | **GAP RESOLVED.** Guard against blank `userId` avoids corrupt downstream queries. |
| **FP03-FR-08 (No VM ID / Host / IP / Port in Client Requests)** | PRD §5.1 / SEC-AUTH-02 | `StudentWorkspaceRepository.kt`, `DefaultStatusRepository.kt` | `validate-fake-broker-contract.sh`, `scan-secrets-logs.sh` | **NONE (Complete).** No request parameters contain VM identifiers or network endpoints. |
| **FP03-FR-09 (AssignmentTag Binding)** | PRD-1.2-AMD-01 | `AssignedWorkspace.assignmentTag`, `ConnectPolicy.isHandoffEligible` | `ConnectPolicyTest`, `StatusRepositoryContractTest` | **NONE (Complete).** `assignmentTag` binds Cloud PC card to VM status probe. |

---

## 2. Development Loop Iteration 2 (2026-11-08)

### Loop A — Independent REVIEWER & ADVERSARY Critique
- **Reviewer Check:**
  - Evaluated `StudentWorkspaceRepository.resolve` pure function seam.
  - Verified that blank id or name in `VmSummary` cannot slip into `AssignedWorkspace` or generate empty tags (`asg_tag_`).
  - Verified Compose BOM version alignment in `build.gradle` (using BOM 2026.09.00 consistently).
- **Adversary Check:**
  - Attack scenario: Malicious Supabase row with empty string `""` or whitespace `" "` as VM id/name.
  - Mitigation: `vm.id.isBlank() || vm.name.isBlank()` check immediately triggers `StudentWorkspaceResult.AssignmentInvalid`, halting connect and handoff.
  - Attack scenario: Empty or blank user UUID query.
  - Mitigation: Immediately returns `AccountNotProvisioned` without database execution.

### Loop B — Automated Test Execution
- **Task Executed:** `./gradlew :app:testDebugUnitTest`
- **Result:** **61/61 tests passed** (0 failures, 0 skipped).
- **New Tests Added (6 tests):** `com.mobidesk.android.StudentAssignmentResolutionTest`
  - `testNullProfile_returnsAccountNotProvisioned`
  - `testZeroAssignedVms_returnsNoAssignment`
  - `testSingleValidVm_returnsAssignedWithDerivedTag`
  - `testBlankVmId_failsClosedAsAssignmentInvalid`
  - `testBlankVmName_failsClosedAsAssignmentInvalid`
  - `testMultipleAssignedVms_failsClosedAsAssignmentConflict`
