# MobiDesk — Branch Compatibility Matrix

**Date:** 8 November 2026
**Auditor:** Jules (AI Software Engineer)
**Governance Authority:** `AGENTS.md` & `docs/MANDATORY_LIVE_DRILL_POLICY.md`
**Scope:** Architectural and contractual compatibility across `main`, `fp-01-auth-hardening`, `fp-02-student-workspace`, `fp-03-assignment-resolution`, and `fp-04-status-readiness`.

---

## 1. Overview of Contract Intersections

The student application relies on four primary contractual boundaries between features:
1. **Authentication Boundary (FP-01 -> All):** Discrete `AuthLevel` (`SIGNED_OUT`, `RECOVERY_ONLY`, `FULLY_AUTHENTICATED`) and access token retrieval (`currentAccessToken()`).
2. **Assignment Resolution Boundary (FP-03 -> FP-02):** `StudentWorkspaceResult.Assigned` providing `AssignedWorkspace(displayLabel, assignmentTag)`.
3. **VM Status & Readiness Boundary (FP-04 -> FP-02):** `DefaultStatusRepository.getVmStatus(accessToken)` yielding `VmStatusSample` with `assignmentTag` matching.
4. **Connect Decision Boundary (FP-02 Core):** `ConnectPolicy.isConnectAvailable()` and `ConnectPolicy.isHandoffEligible()` orchestrating button availability and RDP handoff gating.

---

## 2. Compatibility Analysis Matrix

| Feature Point / Branch | `main` (`417e7d6`) | `fp-01-auth-hardening` (`13c0916`) | `fp-02-student-workspace` (`c532be1`) | `fp-03-assignment-resolution` (`aacc395`) | `fp-04-status-readiness` (`1cba587`) |
|:---|:---:|:---:|:---:|:---:|:---:|
| **`main` (`417e7d6`)** | N/A | **Identical (100% Tree Match)** | **Incompatible Baseline:** Lacks `compose-bom:2026.09.00` in androidTest, lacks `StatusRepository`, lacks strings.xml | **Incompatible Baseline:** Lacks `StudentWorkspaceResult.Assigned` / `asg_tag` | **Incompatible Baseline:** Lacks status fast-fail and 63-test suite |
| **`fp-01-auth-hardening`** | **Identical (100% Tree Match)** | N/A | **Compatible (Direct Parent):** FP-02 consumes `AuthLevel` and `AuthRepository` seamlessly | **Compatible:** Consumes `AuthLevel.FULLY_AUTHENTICATED` guard | **Compatible:** Consumes `AuthRepository.currentAccessToken()` |
| **`fp-02-student-workspace`** | **Requires Integration:** Merge blocked by Live Drill Gate | **Compatible:** Fully consumes FP-01 contracts | N/A | **Fully Compatible:** `fp-03` extends `StudentWorkspaceRepository.resolve` without breaking signature | **Fully Compatible:** `fp-04` extends `DefaultStatusRepository` without breaking signature |
| **`fp-03-assignment-resolution`** | **Requires Integration:** Merge blocked by Live Drill Gate | **Compatible:** Fully consumes FP-01 contracts | **Direct Derivative:** Extends FP-02 with blank field guards and 6 additional unit tests | N/A | **Direct Derivative:** `fp-04` directly inherits and includes `fp-03` |
| **`fp-04-status-readiness`** | **Requires Integration:** Merge blocked by Live Drill Gate | **Compatible:** Fully consumes FP-01 contracts | **Direct Derivative:** Includes all FP-02 and FP-03 changes | **Direct Derivative:** Builds directly on `fp-03` | N/A |

---

## 3. Data Model & Contract Lineage

### 3.1 `AuthLevel` & Session Contract (FP-01)
- **Defined in:** `app/src/main/java/com/mobidesk/android/MobiDeskViewModel.kt`
- **Definition:**
  ```kotlin
  enum class AuthLevel {
      SIGNED_OUT,
      RECOVERY_ONLY,
      FULLY_AUTHENTICATED
  }
  ```
- **Cross-Branch Compatibility:**
  - `main`: Present.
  - `fp-01`: Present.
  - `fp-02`, `fp-03`, `fp-04`: Retained and used as non-bypassable guard for workspace and status loading (`authLevel == AuthLevel.FULLY_AUTHENTICATED`).
- **Verdict:** **100% Compatible across all branches.**

### 3.2 Student Assignment & `AssignedWorkspace` Contract (FP-03)
- **Defined in:** `app/src/main/java/com/mobidesk/android/data/MobiDeskRepositories.kt`
- **Definition Evolution:**
  - *In `main`:* `StudentWorkspaceResult.Ready(profile, assignedVms: List<VmSummary>)`. Permitted multiple VMs or raw list.
  - *In `fp-02`:* Introduced `StudentWorkspaceResult.Assigned(profile, AssignedWorkspace(displayLabel, assignmentTag))`, `NoAssignment`, `AssignmentInvalid`. Kept deprecated `Ready` for backwards compatibility.
  - *In `fp-03`:* Hardened `resolve()`: Blank `userId` returns `AccountNotProvisioned`; blank `vm.id` or blank `vm.name` fails closed as `AssignmentInvalid`.
  - *In `fp-04`:* Inherits `fp-03`'s hardened `resolve()` unchanged.
- **Verdict:** **`fp-04` contains the most robust, backwards-compatible, and secure implementation of FP-03.**

### 3.3 VM Status Response & Sample Contract (FP-04)
- **Defined in:** `app/src/main/java/com/mobidesk/android/data/StatusModels.kt` and `DefaultStatusRepository.kt`
- **Definition Evolution:**
  - *In `main`:* Does not exist.
  - *In `fp-02`:* Introduced `VmLifecycle` (6 states), `RdpTransport` (5 states), `VmStatusResponse`, `VmStatusSample` with monotonic age upper-bound calculation.
  - *In `fp-03`:* Unchanged from `fp-02`.
  - *In `fp-04`:* Hardened `DefaultStatusRepository`: Blank access token fast-fails as `StatusResult.AuthExpired`; HTTP 403 maps to `StatusResult.AuthExpired`.
- **Verdict:** **`fp-04` contains the definitive implementation of FP-04.**

### 3.4 Connect Button & Handoff Policy Contract (FP-02)
- **Defined in:** `app/src/main/java/com/mobidesk/android/data/ConnectPolicy.kt`
- **Invariants:**
  1. `isConnectAvailable()`: Requires `FULLY_AUTHENTICATED`, `isAssigned`, no in-flight request, no active cooldown. If sample is fresh and known-negative (`STOPPED`, `STARTING`, `STOPPING`, `ERROR`, `TCP_UNREACHABLE`, `TCP_REACHABLE`), Connect is disabled. If sample is stale (>15s) or null, Connect is enabled to allow manual refresh.
  2. `isHandoffEligible()`: Requires `FULLY_AUTHENTICATED`, `isAssigned`, matching non-blank `assignmentTag`, fresh sample (age < 15s), `lifecycle == RUNNING`, and `rdpTransport == RDP_NEGOTIATION_OK`.
- **Verdict:** **100% Identical in `fp-02`, `fp-03`, and `fp-04`. Fully compatible.**

---

## 4. Compilation & Toolchain Compatibility

1. **Jetpack Compose BOM Version:**
   - `build.gradle` and `app/build.gradle` on `fp-02`, `fp-03`, and `fp-04` use `androidx.compose:compose-bom:2026.09.00` across both `implementation` and `androidTestImplementation`.
2. **Native Dependency Caching (`scripts/fetch_freerdp_deps.py`):**
   - Present on `fp-02-ci-reproducible-native`, `fp-02-student-workspace`, `fp-03-assignment-resolution`, and `fp-04-status-readiness`.
   - Resolves all CI network timeout risks.
3. **Gate Policy Enforcement (`scripts/verify_live_drill_gate.py`):**
   - Present on all branches. `fp-04` includes enhanced real kit parser validation.
