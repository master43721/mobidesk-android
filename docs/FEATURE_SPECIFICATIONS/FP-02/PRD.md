# FP-02 Product Requirements Document — Student Dashboard and Workspace

**Version:** 0.1 — source-grounded Owner review draft  
**Date:** 4 November 2026  
**Parent feature:** FP-02  
**Priority / release:** P0 / R1  
**Canonical repository:** `master43721/mobidesk-android`  
**Normative parent:** MobiDesk PRD v1.2 PDF  
**Status:** REVIEW DRAFT — proposed feature-level specification; not yet Owner-approved.

## 1. Executive summary

FP-02 provides the authenticated Student's single, truthful home screen in MobiDesk. It displays the student's approved identity fields, their one authorized Cloud PC, and the independently observed VM lifecycle/RDP readiness state. It provides a refresh action, exposes Connect only when the upstream authorization and readiness results permit it, and provides account/settings and sign-out access.

FP-02 is a **student workspace presentation and orchestration feature**. It does not create or change assignments, determine routable VM endpoints, power VMs on or off, authorize an RDP grant, collect Windows credentials, or implement the RDP session.

The dashboard consumes authoritative results from adjacent features:
- FP-01 supplies a fully authenticated student session.
- FP-03 resolves the student's own profile and exactly-one assignment, using the authenticated identity and RLS/server-side authorization.
- FP-04 supplies live VM lifecycle and RDP readiness.
- FP-05 owns the separate Windows guest credential prompt.
- FP-06 and later connection features own session authorization, one-use grant, relay and RDP connection.

## 2. Problem statement

The current student home screen displays the student's name/college and an assigned-VM summary. The checked-in implementation also shows an empty/no-assignment state, a disabled "Remote session not configured" action, a static service notice and a "Refresh assignment" action.

The product requires more than an assignment row:
- the workspace must display only the current student's approved profile and one valid Cloud PC;
- lifecycle and RDP readiness must be reported separately from assignment existence;
- a stale or unknown status must not appear ready;
- a Connect action must be disabled unless the assignment is valid and the latest status is fresh and ready;
- the student must never see internal VM routing data or another student's information.

The existing screen must evolve without duplicating the assignment resolver or broker status implementation owned by FP-03/FP-04.

## 3. Desired student outcome

A student who has completed FP-01 sign-in can:
1. See a welcome message using their approved display name.
2. See their college/institution label when that value is available and approved for display.
3. See one Cloud PC card with a student-safe display label.
4. Understand whether the Cloud PC is powered off, starting, powered on, stopping, unavailable, or unknown.
5. Understand whether the RDP service is not checked, unreachable, TCP-reachable only, or ready for Windows sign-in.
6. See when the status was sampled and whether it has become stale.
7. Refresh status manually and trigger a fresh readiness check before attempting to connect.
8. Select Connect only when the upstream assignment and status results say it is eligible; the action then hands off to FP-05.
9. Open account settings and sign out through FP-01.
10. See an informational Dock placeholder without any R1 dock streaming/pairing controls.

## 4. Parent PRD and feature boundary

### 4.1 Feature Inventory source

The current `docs/FEATURE_INVENTORY.md` defines FP-02 as:
- Student name and approved profile fields.
- College/institution label where available.
- One assigned Cloud PC card.
- Cloud PC display label, lifecycle summary and RDP readiness summary.
- Connect action when eligible.
- Refresh-status action.
- Loading, empty, stale and backend-error states.
- Settings/account entry and sign-out access.
- Informational Dock placeholder only; no R1 Dock streaming controls.

Its acceptance boundary says only the current student's profile and one valid assigned VM are shown; no hostname, IP, VM UUID, port or another student's data is exposed; no hard-coded "Online" status or fake assignment is allowed.

### 4.2 Adjacent feature ownership

| Capability | Owning feature | FP-02 responsibility |
|---|---|---|
| Sign-in, session restore, sign-out and AuthLevel | FP-01 | Consume only the fully authenticated student state; present account actions |
| Profile and assignment query/RLS; exactly-one active assignment; invalid/ambiguous assignment handling | FP-03 | Render the typed result; never choose an assignment |
| VM lifecycle and RDP readiness probe/status API | FP-04 | Render status and sample age; request refresh through the status interface |
| Windows username/password/domain prompt and NLA entry | FP-05 | Hand off Connect to the credential prompt when eligible |
| Grant creation/consume, relay, native RDP and cleanup | FP-06 onward | No grant/relay/RDP implementation in FP-02 |
| Admin student, VM and assignment management | FP-23..FP-28 / Owner-directed account scope | No student-side administration controls |
| Dock pairing/rendering | R2 | Informational placeholder only |

### 4.3 Parent requirements

- FR-AUTH-02 requires profile/assignment reads to use the caller's user token and RLS. FP-02 must consume the current authenticated student's data; FP-03 owns assignment resolution and its authorization invariants.
- FR-VM-01's exactly-one assignment condition is enforced by FP-03. FP-02 must fail closed on any non-assigned or ambiguous result.
- FR-VM-04 and FR-VM-05 define live lifecycle/transport state, probe and freshness; FP-04 owns the authoritative implementation. FP-02 displays those results and applies the same Connect gate.
- FP-02 must not claim the completion of FR-VM-02/03 grant routing, FR-SES, FR-GUEST or FR-RDP requirements.

The parent PDF remains normative. Do not change its requirement wording or IDs in this feature package.

## 5. Scope

### Included
- Student workspace entry after FP-01 exposes a valid `FULLY_AUTHENTICATED` state.
- Student header with approved display name and optional college/institution label.
- A single student-safe Cloud PC summary card from the typed FP-03 assignment result.
- Separate VM lifecycle and RDP transport readiness status presentation.
- Sample timestamp/age and stale indication.
- Initial status load, explicit refresh and refresh-before-Connect.
- Connect eligibility calculation from valid assignment + lifecycle + transport + freshness + authenticated student state.
- Loading, no-profile, no-assignment, invalid-assignment, unknown, stale, not-ready, service-error and retry states.
- Student account/settings entry and sign-out action routed through FP-01.
- R2 Dock informational placeholder.
- Unit, Compose/UI, contract-adapter and accessibility tests.

### Excluded
- Student sign-up, invitation activation or Auth account creation.
- Student profile or assignment writes, reassignment, VM picker or multiple-VM switching.
- Assignment cardinality/authorization implementation owned by FP-03.
- VM start/stop/restart, host/VM administration or power controls.
- Implementation of `GET /v1/vm-status`, the broker probe, endpoint allowlist or host/libvirt integration owned by FP-04/server work.
- Session-grant issuance, grant storage, WebSocket relay, Windows credential prompt, NLA, FreeRDP, RDP renderer, keyboard/pointer and session lifecycle.
- Admin workspace or Admin APIs.
- Dock pairing, external-display streaming or USB/AOA controls.
- Display of hostnames, IPs, ports, VM UUIDs, Guacamole IDs/URLs, internal service addresses, guest usernames/passwords, JWTs or grants.

## 6. User journey

1. Android launches and FP-01 restores/checks the Supabase session.
2. Until FP-01 publishes `FULLY_AUTHENTICATED`, FP-02 does not issue profile, assignment or status requests.
3. FP-02 requests the current student's workspace snapshot from the FP-03-owned repository contract.
4. If the account/profile or assignment is missing/invalid, FP-02 shows the exact PRD error/state and does not render a usable Connect action.
5. For one valid assignment, FP-02 obtains lifecycle/RDP readiness from the FP-04 status interface.
6. The dashboard displays the Cloud PC label, both status dimensions and sample age.
7. If the sample is stale, unknown, or not ready, Connect stays disabled.
8. When the student taps Connect while eligible, FP-02 first requests a fresh status. If the fresh result is still eligible, FP-02 invokes the FP-05 credential-prompt handoff. If not, it remains on the workspace and shows the returned state/error.
9. FP-06 and the broker independently revalidate assignment/session/readiness before issuing a grant. The dashboard's Connect gate is UX, not an authorization boundary.
10. Settings/sign-out are delegated to FP-01. Dock remains informational.

## 7. Functional requirements

| ID | Requirement | Priority | Acceptance evidence |
|---|---|---:|---|
| FP02-FR-001 | FP-02 MUST be reachable only after FP-01 confirms a valid fully authenticated student session. | MUST | Auth-gate integration test; no workspace request while restoring, signed out or recovery-only |
| FP02-FR-002 | The workspace MUST display only the current authenticated student's approved display name and approved profile fields. | MUST | Repository contract/RLS tests and UI assertions |
| FP02-FR-003 | The college/institution label MUST be displayed only when the approved value is present; missing optional value MUST not break the workspace. | MUST | Null/blank profile UI tests |
| FP02-FR-004 | The workspace MUST display at most one student-safe Cloud PC card, sourced from the FP-03 typed assignment result. | MUST | Exactly-one, none, ambiguous and invalid assignment tests |
| FP02-FR-005 | FP-02 MUST NOT choose the first item from a list, display multiple assignments, or permit the student to select/change an assigned VM. | MUST | Multi-row negative test and UI tree inspection |
| FP02-FR-006 | FP-02 MUST NOT infer VM lifecycle or RDP readiness from an assignment row or a cached/hard-coded value. | MUST | Assignment-only fixture displays status as checking/unknown; source inspection |
| FP02-FR-007 | The workspace MUST display VM lifecycle and RDP transport as separate dimensions using the parent vocabulary. | MUST | Enum-to-label tests for every defined value |
| FP02-FR-008 | The UI MUST display status sample age/freshness and MUST treat a sample as stale at the server-provided `staleAfterSeconds` boundary (15 seconds in the current contract). | MUST | Boundary/fake-clock tests |
| FP02-FR-009 | The UI MUST keep Connect disabled unless the session is fully authenticated, the FP-03 assignment is valid, lifecycle is RUNNING, RDP transport is RDP_NEGOTIATION_OK, and status is fresh. | MUST | Exhaustive Connect eligibility truth table |
| FP02-FR-010 | On Connect, FP-02 MUST request/reuse only a freshly verified status result and hand off to FP-05 only when the fresh result remains eligible. | MUST | Stale-to-ready and ready-to-not-ready transition tests |
| FP02-FR-011 | FP-02 MUST provide a manual refresh-status action and visible loading/refresh feedback. | MUST | Button/action and state tests |
| FP02-FR-012 | A failed refresh MUST NOT leave an old positive status displayed as current or enable Connect from stale data. | MUST | Refresh error/stale cache tests |
| FP02-FR-013 | FP-02 MUST map assignment and VM failures to the exact PRD §12 error code/copy/retry behavior; raw backend detail MUST NOT reach the student. | MUST | Error mapping snapshot/UI tests |
| FP02-FR-014 | Loading, empty, no-profile, no-assignment, ambiguous-assignment, stale, unknown, not-ready and backend-error states MUST be explicit and non-confusing. | MUST | Compose state coverage |
| FP02-FR-015 | FP-02 MUST expose account/settings and sign-out actions without reimplementing FP-01 authentication behavior. | MUST | Navigation and delegate tests |
| FP02-FR-016 | FP-02 MUST display only an informational R2 Dock placeholder; there MUST be no functional R1 dock action. | MUST | UI assertions; no dock route/action |
| FP02-FR-017 | FP-02 MUST clear prior workspace/status data when the Auth subject/session changes or signs out. | MUST | Account-switch/sign-out state reset tests |
| FP02-FR-018 | All student-visible text and controls MUST remain accessible with TalkBack, semantic labels and supported font scaling. | MUST | Accessibility checklist/device evidence |

## 8. Dashboard data and presentation contract

### 8.1 Student profile

Display only approved, user-safe fields. The current client selects `students(id,name,college)`; the user ID is an internal repository key and is not shown. The current project documents `name` and `college` as available fields, but their current live values and suitability for student-facing presentation must be rechecked by FP-03 before acceptance.

Do not add roll number, roster data, administrator data or private fields merely because a column exists. Do not use email as a substitute for display name unless an Owner-approved design decision explicitly permits it.

### 8.2 Cloud PC label

The feature requires a student-safe display label. The current database/client references `vms.name`, but the documents do not prove that every current value is an approved student-facing label. FP-02 must consume a safe presentation label supplied by FP-03. It must not expose raw VM UUID, hostname, IP, RDP port, Guacamole connection ID, host name or routing configuration.

### 8.3 Lifecycle state

Use the parent vocabulary:

| State | Student-facing treatment | Connect |
|---|---|---|
| STOPPED | Powered off; show the exact VM-01 message/action | Disabled |
| STARTING | Starting/not ready; show VM-02 message | Disabled |
| RUNNING | Powered on; this alone is not sufficient | Depends on RDP readiness and freshness |
| STOPPING | Shutting down/not ready | Disabled |
| ERROR | Needs attention; safe error treatment | Disabled |
| UNKNOWN | Status unavailable; show sample age | Disabled |

### 8.4 RDP transport state

Use the parent vocabulary:

| State | Student-facing treatment | Connect |
|---|---|---|
| NOT_CHECKED | Checking connection | Disabled |
| TCP_UNREACHABLE | Cannot reach remote desktop service; use VM-03 mapping where returned | Disabled |
| TCP_REACHABLE | Remote desktop service responds; explain that this is port reachability only | Disabled |
| RDP_NEGOTIATION_OK | Ready for Windows sign-in; NLA has not yet succeeded | Eligible only with RUNNING + fresh valid assignment |
| UNKNOWN | Status unavailable | Disabled |

Do not call TCP_REACHABLE "Online", "Ready" or "Connected". RDP_NEGOTIATION_OK does not mean Windows guest credentials have passed NLA.

### 8.5 Status response

FP-02 consumes the existing broker contract:
- `GET /v1/vm-status`
- Bearer Supabase access JWT.
- No body, query parameters or student/VM/endpoint selector.
- Lifecycle, transport, `sampledAt`, `probeDurationMs`, `staleAfterSeconds` and opaque `requestId`.
- The status response does not return VM ID, display name, endpoint, hostname or port.

The current API contract specifies a 1,500 ms server probe budget and a 15-second sample freshness window. FP-04/server implementation owns this contract. FP-02 must not create a competing endpoint or invent response fields.

## 9. Connect eligibility

Connect is enabled only when every condition is true:

```text
FP-01 AuthLevel == FULLY_AUTHENTICATED
AND FP-03 AssignmentState == ASSIGNED_EXACTLY_ONE
AND lifecycle == RUNNING
AND rdpTransport == RDP_NEGOTIATION_OK
AND statusSampleIsFresh == true
AND no refresh/error/terminal state is active
```

On tap:
1. Disable repeat taps and request a fresh status.
2. Re-evaluate the complete condition.
3. If still eligible, hand off to the FP-05 Windows credential prompt.
4. Otherwise stay in FP-02 and show the current safe state/error.
5. FP-06/broker still rechecks Auth session, assignment and readiness; FP-02 does not authorize a session.

No assignment ID, VM ID, hostname, IP, port or endpoint is passed as a client-selected target to FP-05/FP-06.

## 10. Exact PRD error presentation

Use exact English copy from PRD v1.2 / `docs/DESIGN.md §8`. Do not paraphrase, concatenate or replace it with raw server text.

| Code | Exact English copy | FP-02 behavior |
|---|---|---|
| ASSIGN-01 | No Cloud PC is assigned to your account. Contact the lab administrator. | Persistent empty assignment state; no Connect; no automatic retry |
| ASSIGN-02 | We could not verify your assigned Cloud PC. Try again. | Retry status/verification within bounded policy |
| ASSIGN-03 | Your Cloud PC assignment needs administrator review. | Show safe review state; no retry or VM choice |
| VM-01 | Your Cloud PC is powered off. Contact the lab administrator. | Powered-off lifecycle; no client power control |
| VM-02 | Your Cloud PC is starting or is not ready yet. | Show pending/not-ready; retry status after backoff |
| VM-03 | Your Cloud PC cannot be reached right now. | Show unavailable; bounded refresh; no Connect |
| AUTH-01 | Your sign-in session expired. Sign in again. | Delegate re-authentication to FP-01 |

The server error code is authoritative. Do not guess between ASSIGN-02 and VM-03 when the contract provides an error code. Preserve the PRD's retry policy.

## 11. Non-functional requirements

- Preserve the parent Android/Kotlin/Compose/Supabase compatibility baseline.
- No network request runs on the main thread.
- UI state remains deterministic and testable with fake repositories and a fake clock.
- Status samples are visibly fresh/stale; no silent use of stale positive results.
- Do not introduce arbitrary performance targets not found in the parent PRD.
- Use meaningful accessibility labels and do not rely on color alone for lifecycle/readiness.
- No credentials, tokens, grant, endpoint or internal infrastructure fields are present in UI state, Intent arguments, SavedState or logs.
- A failed refresh or account change must clear or visibly invalidate prior status.
- Avoid status polling unless an Owner-reviewed policy is approved; initial load, explicit refresh and refresh-before-Connect are the proposed R1 behavior.

## 12. Dependencies and blockers

| Dependency | Owner | Required evidence |
|---|---|---|
| FP-01 AuthLevel/session handoff is complete and merged | FP-01 | Current main exposes a verified fully-authenticated student state; tests prove recovery-only cannot enter workspace |
| FP-03 typed profile/assignment result and exactly-one semantics | FP-03 | RLS/contract tests; zero/one/multiple/inactive/missing states; safe display label |
| FP-04 live status API and client repository | FP-04 / broker | `GET /v1/vm-status` deployed at a stable approved HTTPS endpoint; enum/error/freshness integration tests |
| FP-05 credential-prompt route | FP-05 | FP-02 Connect handoff target exists; no guest credential enters FP-02 |
| Supabase RLS and display-field verification | FP-03 / Project Owner | Live schema/policy verification; no historical discovery snapshot treated as current |
| Student-facing label approval | Project Owner | Confirm `vms.name` is safe or supply a dedicated display label through FP-03 |

The server being reachable by SSH is not proof that the broker service or the authenticated status endpoint is deployed and externally reachable.

## 13. Acceptance criteria

FP-02 is DONE when:
1. Entry is gated by FP-01 fully authenticated student state.
2. Only the current student's approved profile values are displayed.
3. Only one valid, safe-label Cloud PC card is displayed.
4. Zero, duplicate, inactive, missing and unverified assignment states fail closed with exact parent messages.
5. Lifecycle and RDP transport are distinct and correctly mapped.
6. Status age/freshness is visible and stale at the contract boundary.
7. Connect is disabled for every state other than RUNNING + RDP_NEGOTIATION_OK + fresh status + valid assignment + fully authenticated session.
8. Connect refreshes status before handoff to FP-05.
9. API/network failures never preserve a false positive Ready/Connect state.
10. Settings/sign-out are correctly delegated to FP-01.
11. Dock remains informational and inert for R1.
12. All FP02 tests pass at their specified level; external integration/device blockers are separately recorded.
13. Diff contains no assignment resolver, VM power action, grant, guest credential or RDP transport implementation that belongs to another FP.
14. Documentation traceability and PROGRESS.md reflect actual evidence.

## 14. Owner review required

Review the open items in `OPEN_QUESTIONS.md`, especially:
- whether the current `vms.name` values are approved student-facing Cloud PC labels;
- the exact FP-03 assignment-result contract;
- whether status refresh occurs only on initial load/manual refresh/Connect (recommended) or requires an approved polling policy;
- the stable broker status endpoint availability;
- the FP-01 implementation commit/acceptance evidence that will be the FP-02 dependency.

No open question is silently treated as settled by this draft.
