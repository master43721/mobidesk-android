# Antigravity Implementation Handoff — FP-02 Student Dashboard and Workspace

**Repository:** `master43721/mobidesk-android`  
**Feature:** FP-02 — Student Dashboard and Workspace  
**Specification branch:** `docs/fp-02-student-dashboard-workspace`  
**Implementation branch:** create a new branch from latest `main` after this specification is reviewed/approved.  
**Status:** Owner review draft. Do not begin production implementation until the open cross-feature contracts are verified.

## 1. Task

Implement FP-02 as the Student's single home screen using the existing native Kotlin + Jetpack Compose app. Preserve the visual structure and the owner-approved product boundary. Do not redesign the application or implement adjacent features.

Read first:
- `docs/MobiDesk_PRD_v1.2.pdf`
- `docs/FEATURE_INVENTORY.md`
- `docs/PRD.md`
- `docs/DESIGN.md`
- `docs/API_CONTRACT.md`
- `docs/TECHNICAL_SPEC.md`
- `docs/SECURITY_THREAT_MODEL.md`
- `docs/TEST_PLAN.md`
- `docs/TRACEABILITY_CHECK.md`
- all files in `docs/FEATURE_SPECIFICATIONS/FP-02/`

PRD v1.2 is normative. The feature package decomposes FP-02 but does not silently modify the parent PRD.

## 2. Hard ownership boundaries

- **FP-01:** Sign-in/session restore/AuthLevel/sign-out. FP-02 enters only on the confirmed fully authenticated student state.
- **FP-03:** Current-student profile and exactly-one assignment resolution under RLS/trusted server logic. FP-02 consumes the typed result; do not reimplement the resolver.
- **FP-04:** Broker `GET /v1/vm-status`, host/VM probe, lifecycle/transport status and freshness. FP-02 consumes the status result; do not create another endpoint or guess status.
- **FP-05:** Windows credentials/domain prompt and NLA. FP-02 only hands off Connect.
- **FP-06+:** Grant, session authorization, WSS relay, FreeRDP and session lifecycle. Out of FP-02 scope.
- **FP-23..FP-28:** Admin operations. No Admin or student VM-management controls in this student feature.
- **R2:** Dock remains informational only.

## 3. Critical current-main findings to recheck

The current-main source inspected for this draft showed:
- `MobiDeskViewModel.kt` constructs Supabase auth/workspace repositories and calls workspace load for any Supabase Authenticated event.
- `MobiDeskRepositories.kt` returns a profile plus a list of RLS-filtered VMs; it does not establish exactly-one semantics.
- `DashboardScreen` displays every assigned VM row, a permanently disabled "Remote session not configured" button, static non-ready notice and "Refresh assignment".
- No live StatusRepository/broker API client was observed in the student workspace source.

Re-fetch current main before implementing. The Project Owner reports FP-01 completed, but the current GitHub main source snapshot used to prepare this feature package did not yet show the FP-01 AuthLevel changes. Do not bypass FP-01; obtain the merged implementation commit/evidence.

## 4. Required implementation

### A. Consume authenticated state

- Accept workspace entry only after FP-01 reports `FULLY_AUTHENTICATED` for the current session.
- Do not use Supabase SessionStatus.Authenticated as the only gate.
- Do not run profile/assignment/status calls during SIGNED_OUT, RECOVERY_ONLY, initialization or session invalidation.
- Clear cached workspace/status immediately when the user/session changes or signs out.

### B. Consume exactly-one assignment result

- Use FP-03's reviewed typed result; do not read `assignedVms.firstOrNull()` and do not iterate arbitrary rows.
- Only one `Assigned` outcome may produce the Cloud PC card.
- No assignment shows exact ASSIGN-01 copy.
- Invalid/ambiguous assignment shows exact ASSIGN-03 or contract-defined state.
- Temporary verification failure shows ASSIGN-02.
- Use only a safe student-facing Cloud PC label. Do not assume the existing `vms.name` is safe before FP-03/Owner verifies it.
- Do not expose userId, VM ID/UUID, hostname, IP, port, Guac fields or another student's data.

### C. Consume live status

Use only the approved FP-04 implementation of `GET /v1/vm-status`:
- Authorization: current Supabase access JWT.
- No body, query parameters, VM selector or endpoint selector.
- Read lifecycle enum, RDP transport enum, `sampledAt`, `probeDurationMs`, `staleAfterSeconds` and opaque `requestId`.
- Do not expose private target values.
- Do not infer RUNNING or Ready from the assignment row.

Map lifecycle:
- STOPPED: powered off; exact VM-01 copy.
- STARTING: starting/not ready; VM-02.
- RUNNING: powered on; not sufficient alone.
- STOPPING: shutting down; not ready.
- ERROR: needs attention; no Connect.
- UNKNOWN: unavailable; no Connect.

Map transport:
- NOT_CHECKED: checking.
- TCP_UNREACHABLE: unreachable; VM-03 when returned.
- TCP_REACHABLE: port responds only; not Ready.
- RDP_NEGOTIATION_OK: RDP protocol negotiation ready; Windows NLA remains pending.
- UNKNOWN: unavailable; no Connect.

### D. Connect gating

Connect may be enabled only when all are true:
- FP-01 fully authenticated student.
- FP-03 returns exactly one valid assignment.
- Lifecycle RUNNING.
- Transport RDP_NEGOTIATION_OK.
- Sample fresh (15-second limit in the current contract).
- No refresh/error state.

On tap:
1. Debounce repeated taps.
2. Force a fresh status request.
3. Re-evaluate eligibility.
4. If still ready, call FP-05's no-argument/approved handoff.
5. Do not include target selector, VM ID, endpoint, guest credential or grant.
6. Broker/FP-06 remains authoritative and rechecks assignment/readiness.

### E. Refresh and states

- Load on entering workspace.
- Provide manual Refresh status.
- Refresh again before Connect.
- No periodic poll unless Owner approves.
- A refresh failure makes previous positive status stale/non-actionable.
- Implement loading, no profile, no assignment, needs review, assignment unverified, STOPPED, STARTING, STOPPING, ERROR, UNKNOWN, TCP_UNREACHABLE, TCP_REACHABLE, stale, status service error and ready states.
- Use exact PRD §12 English error copy.

### F. Settings and Dock

- Retain Account Settings and delegate sign-out to FP-01.
- No second sign-out implementation.
- Display the parent S-09 R2 Dock placeholder only; no working Dock control.

## 5. Required test set

Implement/run all FP02-CORE-01..30 from `TEST_PLAN.md`. This includes:
- AuthGate/RECOVERY_ONLY integration.
- Profile and assignment ownership.
- Zero/one/duplicate/invalid assignment outcomes.
- All lifecycle and transport state mappings.
- 15-second stale boundary and refresh failure.
- Full Connect truth table and refresh-before-handoff.
- No target selector in broker request.
- Account switch/sign-out cache clearing.
- Dock placeholder inertness.
- Exact PRD copy and retry behavior.
- TalkBack/font scaling and no sensitive data in UI/logs.

Run existing app unit/Compose tests as well. Report exact task names, test names, report paths and device/emulator. A test not run is NOT RUN, not PASS.

## 6. Stop conditions

Stop Connect integration and report BLOCKED if:
- FP-01 AuthLevel is missing/unverified.
- FP-03 exact-one contract is missing.
- Student-safe Cloud PC display label is not agreed.
- FP-04 status endpoint/client is missing or not authenticated.
- Broker base URL/TLS route is not approved.
- Status freshness cannot be validated.

Do not replace a missing dependency with mocked live UI data, a fake "Online" status, a hard-coded endpoint or a client-side VM selector.

## 7. Final report

Return:
- implementation branch/base SHA;
- changed files and exact ownership;
- FP02-FR-001..018 status with test evidence;
- all CORE-01..30 test results;
- current status endpoint/environment evidence;
- freshness implementation and Connect truth table;
- accessibility/security/static scan evidence;
- unresolved FP-01/FP-03/FP-04/FP-05 dependencies;
- screenshots/test reports from synthetic data only;
- updated `docs/FEATURE_SPECIFICATIONS/FP-02/PROGRESS.md`.

Do not claim FP-02 complete while any MUST requirement or dependency gate remains BLOCKED/NOT RUN.
