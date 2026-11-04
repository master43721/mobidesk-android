# FP-02 Technical Design — Student Dashboard and Workspace

**Version:** 0.1 — source-grounded Owner review draft  
**Date:** 4 November 2026  
**Status:** Target design; no application code changed by this document.

## 1. Design approach

Extend the existing student dashboard rather than replacing the app shell. Keep Compose, the current ViewModel-oriented style, the existing Supabase client and the feature boundaries already documented by the parent design.

FP-02 composes three already-owned inputs:
1. Fully authenticated student identity from FP-01.
2. Typed student profile/assignment outcome from FP-03.
3. VM lifecycle/RDP transport status from FP-04.

The screen must not query or calculate assignment authority itself, and it must not implement the status probe. The Connect button is a user-interface gate only; the broker must make the final authorization decision.

## 2. Verified current-main source baseline

Source snapshot inspected after documentation PR #5 was merged into `main` (4 Nov 2026). Re-fetch latest main before implementation.

| File | Current observed behavior | FP-02 consequence |
|---|---|---|
| `app/src/main/java/com/mobidesk/android/MobiDeskViewModel.kt` | Constructs `SupabaseAuthRepository` and `StudentWorkspaceRepository`; loads the workspace after a Supabase Authenticated event; stores profile/assignment result in `MobiDeskUiState`. | Keep network and state orchestration in ViewModel/repository, not Composables. Do not assume the Auth event alone is the final FP-01 gate; require the completed AuthLevel contract. |
| `app/src/main/java/com/mobidesk/android/data/MobiDeskRepositories.kt` | `StudentWorkspaceRepository.loadFor(userId)` selects `students(id,name,college)`, then selects RLS-filtered `vms(id,name)`; returns an account-not-provisioned result or a ready result containing a list. | Current data result is not yet the strong FP-03 exactly-one assignment contract. FP-02 must consume a typed safe result and never iterate arbitrary returned VM rows. |
| `app/src/main/java/com/mobidesk/android/ui/MobiDeskApp.kt` | Existing `DashboardScreen` shows welcome/name/college and a Cloud PC card; empty assignment message; a disabled "Remote session not configured" button; static "Remote desktop is not ready" notice; "Refresh assignment"; Settings and Sign out. | Evolve this screen to live status and real conditional Connect. Remove static/mock-like readiness copy only when the authoritative status adapter exists. |
| `MobiDeskApplication.kt` | Owns one configured Supabase client with Auth/PostgREST. | Continue using the same client and the caller's user session; never add a privileged key. |
| `docs/DESIGN.md` | S-02 Workspace, S-03 Cloud PC status detail, S-07 Error/recovery and S-09 Dock placeholder are already defined. | Implement the existing screen inventory and visual status vocabulary; do not invent a different flow. |
| `docs/API_CONTRACT.md` | Defines `GET /v1/vm-status`; bearer Supabase JWT; no body/query/target selector; separate lifecycle and transport enums; 1,500 ms probe budget; sample stale after 15 s. | FP-02 consumes the current contract; FP-04/server own the endpoint and probe. |

### 2.1 Current source gaps relevant to FP-02

- Current `StudentWorkspaceResult.Ready` carries a `List<VmSummary>`; the screen loops over every returned row. This does not enforce the product's exactly-one assignment rule. FP-03 must provide a typed result and tests; FP-02 must not choose the first row or display all rows.
- Current dashboard status content is static and remote session is permanently disabled. No live `StatusRepository`/broker status integration was observed in the Android path inspected.
- Current API contract requires the response not to expose VM ID/name/endpoint/hostname/port. Therefore the display label must arrive separately through a safe FP-03 presentation model.
- The checked-in code must be rechecked because the Project Owner reports FP-01 has been completed. The current GitHub main snapshot inspected here does not yet show those FP-01 source changes; see PROGRESS.md and OPEN_QUESTIONS.md.

## 3. Target component boundaries

```text
FP-01 Auth state
  └─ FULLY_AUTHENTICATED student session
       │
       ▼
StudentWorkspaceViewModel (FP-02 composition)
  ├─ WorkspaceRepository contract (data semantics implemented/owned by FP-03)
  │    ├─ Student display profile
  │    └─ AssignmentResolution (exactly zero/one/invalid)
  └─ StatusRepository contract (implementation/endpoint owned by FP-04)
       └─ GET /v1/vm-status (no target selector)
             │
             ▼
WorkspaceUiState
  ├─ Student header
  ├─ One Cloud PC card
  ├─ Lifecycle status
  ├─ RDP transport status
  ├─ sampledAt / freshness
  ├─ safe errors
  └─ Connect eligibility
       │
       └─ fresh eligible Connect → FP-05 Windows Credential Prompt
                                 → FP-06 grant/session authorization
```

No Compose component calls Supabase PostgREST or Ktor directly. No UI component holds a VM route, endpoint or secret.

## 4. Screen composition

### S-02 Workspace

Retain the existing brand/header and greeting structure:
- MobiDesk branding and "Student workspace".
- Settings icon/button.
- Welcome plus approved student display name.
- College label only when available.
- One Cloud PC card.
- Lifecycle and RDP transport summaries, sample age/freshness.
- Connect or disabled/not-ready action.
- Refresh status.
- Informational Dock placeholder.
- Sign out.

Do not show an assignment list or selection control. Do not display infrastructure fields.

### S-03 Cloud PC status detail

Use the existing parent screen definition:
- Separate lifecycle label and RDP transport label.
- Last sampled time or age.
- Fresh/stale indication.
- Refresh status action.
- Exact PRD error message where a code is returned.
- Explicit explanation that TCP reachability is not a completed Windows sign-in.

S-03 can be a dedicated Compose destination or a focused detail panel consistent with the existing navigation style. Choose the smallest pattern that matches the current codebase; do not introduce a new navigation library just for FP-02.

### S-07 Error/recovery

Use the exact PRD §12 English messages from `docs/DESIGN.md §8`. Error states include no assignment, assignment review needed, assignment verification failure, powered off, starting/not ready and unreachable. Retry behavior must match the parent contract.

### S-09 Dock placeholder

Display the existing R2 informational text only. No button, route or pairing control.

## 5. Domain presentation contracts

These are screen-facing contracts, not a mandate to create an elaborate new package hierarchy.

### 5.1 Workspace profile

```kotlin
data class StudentHeader(
    val displayName: String,
    val collegeLabel: String?
)
```

The authenticated subject is passed from FP-01 to the repository; it is not shown or chosen by the screen.

### 5.2 Assignment presentation

FP-02 consumes a typed outcome from FP-03. Suggested shape for review:

```kotlin
sealed interface AssignmentPresentation {
    data object ProfileUnavailable : AssignmentPresentation
    data object NoAssignment : AssignmentPresentation
    data class Assigned(val cloudPcLabel: String) : AssignmentPresentation
    data object NeedsAdministratorReview : AssignmentPresentation
    data object VerificationUnavailable : AssignmentPresentation
}
```

The real FP-03 model may use the project's existing sealed-result style. Requirements:
- Exactly one `Assigned` value at most.
- No raw VM ID, VM UUID, hostname, IP, port or Guacamole fields.
- Zero assignments maps to the PRD's ASSIGN-01 state.
- Ambiguous/inactive/unmapped state maps to ASSIGN-03 or the exact FP-03 contract error.
- Verification/transient read failure maps to ASSIGN-02.
- FP-02 does not implement the assignment query, uniqueness constraint or server authorization.

### 5.3 Status presentation

Use the exact status enums from `API_CONTRACT.md §4`:

```kotlin
enum class VmLifecycle {
    STOPPED, STARTING, RUNNING, STOPPING, ERROR, UNKNOWN
}

enum class RdpTransport {
    NOT_CHECKED, TCP_UNREACHABLE, TCP_REACHABLE,
    RDP_NEGOTIATION_OK, UNKNOWN
}
```

The actual transport DTO/repository belongs to FP-04. Do not duplicate enum definitions if shared parent domain models already exist.

The response also supplies `sampledAt`, `probeDurationMs`, `staleAfterSeconds` and `requestId`. Treat requestId as opaque internal diagnostic metadata, not a user-facing VM identifier.

### 5.4 Workspace UI state

Suggested small, deterministic state:
- session/loading state;
- profile/assignment presentation;
- optional readiness response;
- status refresh in progress;
- stale flag/sample age;
- safe PRD error code/message;
- derived `canConnect`.

Do not store credentials, JWT, grant, VM endpoint or raw server exception in UI state.

## 6. Data flow

### Initial workspace entry

1. Confirm FP-01 `FULLY_AUTHENTICATED` student session.
2. Clear any data associated with a prior subject/session.
3. Load profile and assignment through the FP-03 repository contract, using the current authenticated user context and RLS.
4. If profile/assignment is not a single valid assignment, render the typed error/empty state and do not request Connect.
5. If exactly one approved assignment is returned, obtain current status through FP-04's StatusRepository, whose broker request uses the current user's Supabase access JWT and no target selector.
6. Compose S-02 from the student-safe profile, assignment label and status response.

### Manual status refresh

1. Set refresh-in-progress.
2. Disable Connect while refresh is running.
3. Request new status through StatusRepository.
4. On success replace the previous status and freshness baseline.
5. On failure never reuse a prior positive status as current; mark stale/unavailable and keep Connect disabled.
6. Keep profile/assignment information only if the current Auth subject/session remains the same.

### Connect tap

1. Debounce/serialize repeated taps.
2. Request fresh status.
3. Re-evaluate full Connect eligibility.
4. If eligible, hand off without a VM selector to FP-05.
5. Otherwise remain in dashboard and display the current exact parent status/error.
6. Broker/FP-06 revalidates authorization and readiness; no client-side flag is trusted by the server.

## 7. Freshness and polling

- The current contract sets `staleAfterSeconds = 15`.
- A sample at/after its stale boundary must not permit Connect.
- A failed refresh cannot leave the old green/Ready state actionable.
- Proposed R1 behavior: load status on workspace entry, refresh when the student asks, and always refresh before Connect.
- No periodic background polling is specified by the existing documents. Do not add it without an Owner decision.
- Use a testable clock abstraction for freshness boundary tests. If device/server clock skew prevents reliable interpretation of `sampledAt`, keep Connect disabled and surface the issue; record a follow-up with FP-04 rather than inventing a new API field inside FP-02.

## 8. Connect eligibility function

```kotlin
fun canConnect(
    isFullyAuthenticated: Boolean,
    assignment: AssignmentPresentation,
    lifecycle: VmLifecycle?,
    transport: RdpTransport?,
    isStatusFresh: Boolean,
    isRefreshing: Boolean
): Boolean =
    isFullyAuthenticated &&
    assignment is AssignmentPresentation.Assigned &&
    lifecycle == VmLifecycle.RUNNING &&
    transport == RdpTransport.RDP_NEGOTIATION_OK &&
    isStatusFresh &&
    !isRefreshing
```

This function is UI gating only. The broker remains the final authorization authority.

## 9. Error mapping

Use exact English PRD v1.2 strings:
- ASSIGN-01: "No Cloud PC is assigned to your account. Contact the lab administrator."
- ASSIGN-02: "We could not verify your assigned Cloud PC. Try again."
- ASSIGN-03: "Your Cloud PC assignment needs administrator review."
- VM-01: "Your Cloud PC is powered off. Contact the lab administrator."
- VM-02: "Your Cloud PC is starting or is not ready yet."
- VM-03: "Your Cloud PC cannot be reached right now."
- AUTH-01: "Your sign-in session expired. Sign in again."

Never synthesize a new PRD code, change the exact English copy, or show low-level backend exception content. Use returned contract code where available; do not guess between ASSIGN-02 and VM-03.

## 10. Security and data handling

- Profile and assignment reads use the current student's user JWT/RLS path; no service-role key.
- Do not pass caller-selected `userId`, `vmId`, hostname or endpoint from UI events.
- Broker status call carries the Supabase access JWT and no selector/query/body.
- Only approved profile/label fields enter Compose.
- Do not map or retain Guacamole URL/connection ID, endpoint, hostname, IP or port.
- Do not log student identifiers, JWTs, response bodies, request headers or raw exception details.
- Clear workspace/status state on sign-out, Auth subject change and session invalidation.
- Use opaque IDs only in safe diagnostics; do not render internal broker request ID as a VM or assignment identity.
- Student screens have no VM start/stop/restart, assignment change or arbitrary network action.
- Server-side checks remain authoritative even when Connect is enabled.

## 11. UI accessibility

- Give lifecycle and transport labels meaningful text, not icon/color-only indicators.
- Announce loading/refresh completion and errors once.
- Provide meaningful labels for Refresh, Connect and Settings.
- Preserve predictable TalkBack focus order.
- Support 1.3x and 1.5x font scaling without hiding the Cloud PC state or primary action.
- Do not show tiny timestamp-only freshness; pair it with a text state such as "Updated just now" / "Status needs refreshing", with wording approved during UX review.

## 12. Dependencies and unresolved technical questions

- FP-01 current-main implementation and exact AuthLevel handoff needs verification.
- FP-03 must supply typed one-assignment result and student-safe display label.
- FP-04 must supply StatusRepository and working `GET /v1/vm-status`.
- Current `vms.name` label safety is not verified.
- `sampledAt` clock-skew semantics need agreement with FP-04.
- No broker base URL is selected in the checked-in API/operations docs; configuration must come from an approved build/environment config, not a literal private endpoint in source.

## 13. Not part of FP-02

No schema migrations, RLS policy modifications, broker endpoint implementation, libvirt/VM action, assignment mutation, grant, guest credential, FreeRDP or session-lifecycle code. Those changes must be planned and reviewed under their owning FPs.
