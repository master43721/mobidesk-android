# MobiDesk — Integration-Readiness Audit Report

**Date:** 8 November 2026
**Auditor:** Jules (AI Software Engineer)
**Governance Authority:** `AGENTS.md` & `docs/MANDATORY_LIVE_DRILL_POLICY.md`
**Repository:** `master43721/mobidesk-android`
**Trunk Baseline:** `main` @ `417e7d63a7516f0f8247ac713301894d6c7b0b74`

---

## Executive Summary

This report establishes a rigorous, evidence-driven integration-readiness audit across five key active feature branches:
1. `fp-01-auth-hardening`
2. `fp-02-student-workspace`
3. `fp-02-ci-reproducible-native`
4. `fp-03-assignment-resolution`
5. `fp-04-status-readiness`

In strict adherence to `AGENTS.md` and `docs/MANDATORY_LIVE_DRILL_POLICY.md`, **no branch is marked complete or accepted merely because code compiles or unit tests pass**. A feature point is only complete when all 6 criteria of the Terminal Acceptance Invariant are fulfilled:
```text
FP Acceptance = (Implementation == PASS)
              ∧ (Automated Tests == PASS)
              ∧ (Build Verification == PASS)
              ∧ (Independent Code Review == PASS)
              ∧ (Physical Live Drill == PASS)
              ∧ (Project Owner Decision == ACCEPTED)
```

None of the audited branches have been merged into `main`. The findings below document the exact state of each branch.

---

## 1. Branch Audit: `fp-01-auth-hardening`

### 1.1 Exact Branch Name and Latest Commit SHA
- **Branch Name:** `fp-01-auth-hardening`
- **Latest Commit SHA:** `13c0916fe84726c75e1313cb1c5b13e8759622bd`
- **Commit Message:** `docs(fp-01): update live drill kit with 16 scenarios, expected CI artifact SHA, and blank verification lines`

### 1.2 Base Branch and Merge-Base SHA
- **Target / Upstream Base:** `main`
- **Merge-Base SHA with `origin/main`:** `1b2f0babd41ebdbc8cacbd1f6e2707114df403d6`
- **Divergence:** 2 commits behind `origin/main` (`1435bdc`, `417e7d6`), 8 commits ahead.
- **Tree Delta vs `origin/main`:** Commit `417e7d6` on `main` merged PR #15 (`docs/fp-01-live-drill-kit`), which cherry-picked the exact same live-drill documentation updates. Consequently, `git diff origin/main origin/fp-01-auth-hardening` is **empty** (0 files changed).

### 1.3 Files Changed Compared with `main`
- Total files changed: **0 files** (identical tree to `main@417e7d6`).
- Historical changes introduced by FP-01 (already present on `main` via PR #8 and `c414c73`):
  - `app/src/main/java/com/mobidesk/android/MobiDeskViewModel.kt` (discrete `AuthLevel`, recovery flows, offline sign-out)
  - `app/src/main/java/com/mobidesk/android/data/MobiDeskRepositories.kt` (`AuthRepository`, `SupabaseAuthRepository`)
  - `app/src/main/java/com/mobidesk/android/data/SharedPreferencesRecoveryStateStore.kt`
  - Unit tests: `AuthErrorMessagesTest.kt`, `RecoveryStateStoreTest.kt`, `MobiDeskApplicationTest.kt`, `MobiDeskViewModelTest.kt`

### 1.4 Unrelated Changes Evaluation
- **None.** Contains solely authentication hardening and live-drill kit documentation.

### 1.5 Implementation Status
- **Status:** **PASS (Implementation Complete)**.
- Full Supabase email/password sign-in, discrete `AuthLevel` (`SIGNED_OUT`, `RECOVERY_ONLY`, `FULLY_AUTHENTICATED`), PKCE deep-link callback handling (`com.mobidesk.android://auth-callback/recovery`), recovery state store, and local sign-out fallback are implemented.

### 1.6 Unit and Integration Test Results
- **Automated Tests:** **40/40 passed** (0 failures, 0 skipped).
  - `AuthErrorMessagesTest`: 10 passed
  - `MobiDeskApplicationTest`: 1 passed
  - `MobiDeskViewModelTest`: 23 passed
  - `RecoveryStateStoreTest`: 6 passed

### 1.7 Build and CI Status
- **Automated CI Workflow:** `.github/workflows/android-ci.yml`
- **Successful CI Run ID:** `42450424147` (commit `84bb6b7e507d3871848f3386c6ceab47d4442c05`).
- **CI Artifact SHA-256:** `450aa3be0b510d3847d9a83a16be8ee7a196d2474eda2f58ededbc76e68d5206`
- **Local Toolchain Verification:** Pinned Java 21, Gradle 9.6.1, Android SDK Platform 37, Build-Tools 37.0.0, CMake 4.1.2.

### 1.8 Independent Review Status and Evidence
- **Review Status:** **PASS (Automated Implementation Review Completed)**.
- Evidence documented in `docs/FEATURE_SPECIFICATIONS/FP-01/PROGRESS.md` and `docs/FEATURE_SPECIFICATIONS/FP-01/ANTIGRAVITY_REVIEW_PROMPT.md`.

### 1.9 Live-Drill Documentation and Evidence Status
- **Live Drill Status:** `LIVE_DRILL_STATUS: BLOCKED on NEG-03`
- **Owner Sign-off Status:** `UNACCEPTED (Awaiting physical demonstration on Android phone)`
- **Documentation Kit:** `docs/live-drills/FP-01/EXECUTION_REPORT.md` and `DEVICE_OR_ENVIRONMENT_CHECKLIST.md` contain 16 detailed physical scenarios with blank verification lines.
- **Blocker:** Physical staging credentials and tester phone details (`NEG-03`).

### 1.10 Dependencies on Other Feature Branches
- None. FP-01 is the architectural root foundation.

### 1.11 Known Conflicts, API Incompatibilities and Migration Risks
- None. The branch tree matches `main` identically.

---

## 2. Branch Audit: `fp-02-student-workspace` & `fp-02-ci-reproducible-native`

### 2.1 Exact Branch Name and Latest Commit SHA
- **Branch Names:** `fp-02-student-workspace` and `fp-02-ci-reproducible-native`
- **Latest Commit SHA:** `c532be1603908d559c8c9bf83379c03243129d76` (both branches point to this exact commit)
- **Commit Message:** `fix(ci): use upstream ExternalOpenH264 to preserve patch file resolution`

### 2.2 Base Branch and Merge-Base SHA
- **Base Branch:** Branch originated from `fp-01-auth-hardening` / `main`.
- **Merge-Base SHA with `origin/main`:** `1b2f0babd41ebdbc8cacbd1f6e2707114df403d6`
- **Divergence:** 2 commits behind `origin/main`, 18 commits ahead.

### 2.3 Files Changed Compared with `main`
- Total files changed: **51 files** (+3888 lines, -386 lines).
- Key files changed:
  - `.github/workflows/android-ci.yml`: Configurable `MOBIDESK_TARGET_ABIS`, native dependency caching, gate verification step.
  - `build.gradle`: Target ABI property resolution, CMake module path shadowing, native dependency cache directory wiring.
  - `app/build.gradle`: Added `compose-bom:2026.09.00` to `androidTestImplementation`.
  - `app/src/main/java/com/mobidesk/android/MobiDeskViewModel.kt`: Implemented workspace loading, monotonic age timer, Connect policy orchestration.
  - `app/src/main/java/com/mobidesk/android/data/ConnectPolicy.kt`: Connect button availability and RDP handoff eligibility pure functions.
  - `app/src/main/java/com/mobidesk/android/data/DefaultStatusRepository.kt`: Status client with 64 KB stream cap, monotonic clock, 429 Retry-After, timeout handling.
  - `app/src/main/java/com/mobidesk/android/data/StatusModels.kt`: `VmLifecycle`, `RdpTransport`, `VmStatusSample`, `ApiErrorResponse`.
  - `app/src/main/java/com/mobidesk/android/ui/MobiDeskApp.kt`: Complete Student Dashboard UI, Cloud PC card, status badges, strings alignment, R2 Dock placeholder.
  - `app/src/main/res/values/strings.xml`: 74 string resources covering all 11 lifecycle/transport states and parent error catalogue (`ASSIGN-01..03`, `VM-01..03`).
  - Unit tests: `ConnectPolicyTest.kt` (3 tests), `MobiDeskFp02Test.kt` (5 tests), `StatusRepositoryContractTest.kt` (7 tests).
  - Native scripts & shadows: `scripts/fetch_freerdp_deps.py`, `scripts/generate_cmake_shadows.py`, `cmake/freerdp/External*.cmake` (8 files).

### 2.4 Unrelated Changes Evaluation
- **Native CI Reproducibility:** Included `fetch_freerdp_deps.py` and CMake shadowing to prevent CI failures from upstream HTTP 504 timeouts (`libpng`). While authored under FP-02, this is critical infrastructure required across all builds.

### 2.5 Implementation Status
- **Status:** **PASS (Implementation Complete)**.
- Dashboard, Cloud PC card, lifecycle badges, monotonic clock age tracking, and Connect button state machine are fully implemented.

### 2.6 Unit and Integration Test Results
- **Automated Tests:** **55/55 passed** (0 failures, 0 skipped).
  - `AuthErrorMessagesTest`: 10 passed
  - `ConnectPolicyTest`: 3 passed
  - `MobiDeskApplicationTest`: 1 passed
  - `MobiDeskFp02Test`: 5 passed
  - `MobiDeskViewModelTest`: 23 passed
  - `RecoveryStateStoreTest`: 6 passed
  - `StatusRepositoryContractTest`: 7 passed

### 2.7 Build and CI Status
- **Automated CI Workflow:** `.github/workflows/android-ci.yml`
- **CI Run History:**
  - Push Run `42475965882` (commit `3393655`): Failed due to upstream `libpng` 504 timeout before reproducible dependency caching was committed in `8368a01`/`c532be1`.
  - PR #10 Run `42475969710`: Failed at gate policy step because PR targeted `main` with unexecuted live drill scenarios.
- **Local Build Status:** Local execution of `:app:testDebugUnitTest` passes 55/55 tests.

### 2.8 Independent Review Status and Evidence
- **Review Status:** **PASS (Automated Implementation Review Completed)**.
- Documented in `docs/FEATURE_SPECIFICATIONS/FP-02/PROGRESS.md` across Iterations 1, 2, and 3.

### 2.9 Live-Drill Documentation and Evidence Status
- **Live Drill Status:** `LIVE_DRILL_STATUS: READY FOR OWNER`
- **Owner Sign-off Status:** `UNACCEPTED (Awaiting physical demonstration)`
- **Documentation Kit:** `docs/live-drills/FP-02/EXECUTION_REPORT.md` and `DEVICE_OR_ENVIRONMENT_CHECKLIST.md`.

### 2.10 Dependencies on Other Feature Branches
- Depends on `fp-01-auth-hardening` (consumed directly).
- **Interface Coupling Note:** `fp-02-student-workspace` already contains the initial definitions of `StudentWorkspaceRepository.resolve()` and `DefaultStatusRepository`. Branches `fp-03` and `fp-04` were branched off `fp-02` to harden edge cases.

### 2.11 Known Conflicts, API Incompatibilities and Migration Risks
- PR #10 cannot merge into `main` because FP-01 has not completed its physical live drill, and `verify_live_drill_gate.py` prevents merging branches with `NOT RUN` live drill steps to `main`.

---

## 3. Branch Audit: `fp-03-assignment-resolution`

### 3.1 Exact Branch Name and Latest Commit SHA
- **Branch Name:** `fp-03-assignment-resolution`
- **Latest Commit SHA:** `aacc395f88a9eca410a1e463ce5c4ebfe8b89136`
- **Commit Message:** `feat(fp-03): implement assignment validation edge cases and add StudentAssignmentResolutionTest (61/61 tests pass)`

### 3.2 Base Branch and Merge-Base SHA
- **Base Branch:** `fp-02-student-workspace`
- **Merge-Base SHA with `fp-02-student-workspace`:** `c532be1603908d559c8c9bf83379c03243129d76`
- **Merge-Base SHA with `origin/main`:** `1b2f0babd41ebdbc8cacbd1f6e2707114df403d6`
- **Divergence vs `origin/main`:** 2 commits behind, 22 commits ahead.

### 3.3 Files Changed Compared with `main`
- Total files changed vs `main`: **53 files**.
- Specific files changed vs `fp-02-student-workspace` (3 files):
  - `app/src/main/java/com/mobidesk/android/data/MobiDeskRepositories.kt`:
    - Added guard: blank `userId` immediately returns `AccountNotProvisioned`.
    - Added guard in `StudentWorkspaceRepository.resolve()`: blank `vm.id` or blank `vm.name` fails closed as `AssignmentInvalid` with message `"Corrupt assignment data: VM identifier or name is blank"`.
  - `app/src/test/java/com/mobidesk/android/StudentAssignmentResolutionTest.kt`: Added 6 unit tests.
  - `docs/FEATURE_SPECIFICATIONS/FP-03/PROGRESS.md`: Recorded Iteration 2 gap analysis and test evidence.

### 3.4 Unrelated Changes Evaluation
- **None.** Scoped strictly to assignment resolution validation, tests, and documentation.

### 3.5 Implementation Status
- **Status:** **PASS (Implementation Complete)**.
- RLS profile reads, exactly-one active assignment rule, corrupt blank field rejection, zero-assignment (`NoAssignment`), multiple-assignment conflict (`AssignmentInvalid`), and synthetic `asg_tag_` derivation are implemented.

### 3.6 Unit and Integration Test Results
- **Automated Tests:** **61/61 passed** (0 failures, 0 skipped).
  - 55 tests from FP-02 suite + 6 tests in `StudentAssignmentResolutionTest`:
    - `testNullProfile_returnsAccountNotProvisioned`: PASSED
    - `testZeroAssignedVms_returnsNoAssignment`: PASSED
    - `testSingleValidVm_returnsAssignedWithDerivedTag`: PASSED
    - `testBlankVmId_failsClosedAsAssignmentInvalid`: PASSED
    - `testBlankVmName_failsClosedAsAssignmentInvalid`: PASSED
    - `testMultipleAssignedVms_failsClosedAsAssignmentConflict`: PASSED

### 3.7 Build and CI Status
- **Automated CI Workflow:** `.github/workflows/android-ci.yml`
- **CI Run History:**
  - Push Run `42475976994` (commit `045965e`): Failed due to upstream `libpng` 504 before reproducible cache fix.
  - PR #11 Run `42475980493` (targeting `fp-02`): Failed due to `libpng` 504.
  - Commit `aacc395` merged `c532be1` (which contains reproducible caching) into `fp-03`.

### 3.8 Independent Review Status and Evidence
- **Review Status:** **PASS (Automated Implementation Review Completed)**.
- Documented in `docs/FEATURE_SPECIFICATIONS/FP-03/PROGRESS.md`. Pure function `StudentWorkspaceRepository.resolve` verified for fail-closed edge cases.

### 3.9 Live-Drill Documentation and Evidence Status
- **Live Drill Status:** `LIVE_DRILL_STATUS: NOT REQUESTED` (Gated on staging broker / database integration).
- **Owner Sign-off Status:** `UNACCEPTED`

### 3.10 Dependencies on Other Feature Branches
- Depends on `fp-02-student-workspace` (from which it branched) and `fp-01-auth-hardening`.

### 3.11 Known Conflicts, API Incompatibilities and Migration Risks
- Merging cleanly into `fp-02-student-workspace`. `AssignedWorkspace` model is fully compatible.

---

## 4. Branch Audit: `fp-04-status-readiness`

### 4.1 Exact Branch Name and Latest Commit SHA
- **Branch Name:** `fp-04-status-readiness`
- **Latest Commit SHA:** `1cba5876ee163ef36d9e3cd505112fb24fb6a568`
- **Commit Message:** `test(gate): add test_real_kit_parser to verify_live_drill_gate.py to parse real EXECUTION_REPORT with blank sign-off`

### 4.2 Base Branch and Merge-Base SHA
- **Base Branch:** `fp-03-assignment-resolution`
- **Merge-Base SHA with `fp-03-assignment-resolution`:** `aacc395f88a9eca410a1e463ce5c4ebfe8b89136`
- **Merge-Base SHA with `origin/main`:** `1b2f0babd41ebdbc8cacbd1f6e2707114df403d6`
- **Divergence vs `origin/main`:** 2 commits behind, 27 commits ahead.

### 4.3 Files Changed Compared with `main`
- Total files changed vs `main`: **54 files**.
- Specific files changed vs `fp-03-assignment-resolution` (4 files):
  - `app/src/main/java/com/mobidesk/android/data/DefaultStatusRepository.kt`:
    - Added fast-fail on blank access token (`StatusResult.AuthExpired`).
    - Added explicit HTTP 403 Forbidden handling (`StatusResult.AuthExpired`).
  - `app/src/test/java/com/mobidesk/android/StatusRepositoryContractTest.kt`: Added 2 unit tests (fast-fail on blank token, 403 Forbidden).
  - `docs/FEATURE_SPECIFICATIONS/FP-04/PROGRESS.md`: Recorded Iteration 2 gap analysis and test evidence.
  - `scripts/verify_live_drill_gate.py`: Added `test_real_kit_parser()` negative test validating parser rejection of blank sign-off blocks.

### 4.4 Unrelated Changes Evaluation
- **Gate Test Hardening:** Includes a parser negative test in `scripts/verify_live_drill_gate.py`. This strengthens gate enforcement across all branches.

### 4.5 Implementation Status
- **Status:** **PASS (Implementation Complete)**.
- Decodes all 6 `VmLifecycle` and 5 `RdpTransport` states, handles 64 KB stream bounds, HTTP 429 Retry-After, blank token fast-fail, and HTTP 403 expiration.

### 4.6 Unit and Integration Test Results
- **Automated Tests:** **63/63 passed** (0 failures, 0 skipped).
  - 61 tests from FP-03 + 2 new contract tests:
    - `getVmStatus_blankAccessToken_failsImmediatelyAsAuthExpired`: PASSED
    - `getVmStatus_forbidden403_returnsAuthExpired`: PASSED

### 4.7 Build and CI Status
- **Automated CI Workflow:** `.github/workflows/android-ci.yml`
- **CI Run History:**
  - Push Run `42475987683` (commit `d7bbbe9`): Failed on `libpng` 504 before reproducible cache was merged.
  - PR #12 Run `42475992952` (targeting `fp-03`): Failed on `libpng` 504.
  - Latest commits `38e43fc` and `1cba587` include the native caching fix.

### 4.8 Independent Review Status and Evidence
- **Review Status:** **PASS (Automated Implementation Review Completed)**.
- Documented in `docs/FEATURE_SPECIFICATIONS/FP-04/PROGRESS.md`. Fast-fail and 403 handling confirmed.

### 4.9 Live-Drill Documentation and Evidence Status
- **Live Drill Status:** `LIVE_DRILL_STATUS: NOT REQUESTED` (Awaiting live staging broker).
- **Owner Sign-off Status:** `UNACCEPTED`

### 4.10 Dependencies on Other Feature Branches
- Consumes `fp-03-assignment-resolution`, `fp-02-student-workspace`, and `fp-01-auth-hardening`.

### 4.11 Known Conflicts, API Incompatibilities and Migration Risks
- Clean fast-forward / merge path from `fp-03` to `fp-04`. Fully compatible data contracts.

---

## 5. Summary Findings Across All Branches

| Metric / Check | `fp-01-auth-hardening` | `fp-02-student-workspace` | `fp-02-ci-reproducible-native` | `fp-03-assignment-resolution` | `fp-04-status-readiness` |
|:---|:---:|:---:|:---:|:---:|:---:|
| **Commit SHA** | `13c0916` | `c532be1` | `c532be1` | `aacc395` | `1cba587` |
| **Tree Diff vs `main`** | 0 files (Identical) | 51 files changed | 51 files changed | 53 files changed | 54 files changed |
| **Unit Tests Passed** | 40 / 40 | 55 / 55 | 55 / 55 | 61 / 61 | 63 / 63 |
| **Tests Failed / Skipped**| 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| **Automated Review** | PASS | PASS | PASS | PASS | PASS |
| **Live Drill Gate Status**| BLOCKED (NEG-03) | READY FOR OWNER | READY FOR OWNER | NOT REQUESTED | NOT REQUESTED |
| **Owner Acceptance** | UNACCEPTED | UNACCEPTED | UNACCEPTED | UNACCEPTED | UNACCEPTED |
| **Merger Ready to Main** | NO (Gated on Drill) | NO (Gated on Drill) | NO (Gated on Drill) | NO (Gated on Drill) | NO (Gated on Drill) |
