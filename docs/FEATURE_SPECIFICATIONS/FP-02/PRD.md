# FP-02 PRD — Student Dashboard and Workspace

**Version:** 0.2  
**Date:** 5 November 2026  
**Parent:** MobiDesk PRD v1.2, owner-review draft  
**Status:** Feature-level owner review; parent amendment required for the Connect/freshness and assignment-consistency changes below.

## 1. Outcome

After FP-01 confirms a fully authenticated student session, MobiDesk shows the student's approved profile and exactly one valid assigned Cloud PC. The card presents lifecycle and RDP transport as separate observations, offers refresh, and offers a safe Connect action that always revalidates status before entering FP-05.

FP-02 is a Compose presentation/orchestration feature. It does not resolve assignments, probe VMs, issue grants, collect Windows credentials or open RDP.

## 2. In scope

- Student display name and optional approved college label.
- Exactly one Cloud PC card for a valid exactly-one assignment.
- Distinct lifecycle and RDP transport labels.
- Sample age and stale/unknown presentation.
- Manual Refresh status.
- Connect affordance and refresh-before-handoff orchestration.
- Loading, account-not-provisioned, assignment, status, session and recovery states.
- FP-01 Settings/sign-out delegation.
- Informational, non-interactive R2 Dock placeholder.
- Unit, Compose, repository-contract, accessibility and privacy tests.

## 3. Out of scope

No student VM picker, assignment write, power control, endpoint/host/IP/port/UUID display, service-role key, broker probe implementation, grant creation, Windows credential collection, NLA, WSS, FreeRDP, RDP session Activity, administrator UI or Dock streaming.

## 4. Functional requirements

| ID | Requirement | Acceptance |
|---|---|---|
| FP02-FR-001 | Workspace calls are gated by FP-01 FULLY_AUTHENTICATED for the current session. | No protected request during restore, signed-out, recovery-only or invalid session. |
| FP02-FR-002 | Show only the current student's approved profile fields. | Cross-user fixture and account-switch tests pass. |
| FP02-FR-003 | Show the approved college label only when nonblank. | Missing value is omitted; no hard-coded institution. |
| FP02-FR-004 | Render exactly one Cloud PC card only for FP-03 Assigned. | No list iteration, first-row fallback or duplicate card. |
| FP02-FR-005 | Never expose a target selector or infrastructure routing field. | UI, DTO, navigation and log scans pass. |
| FP02-FR-006 | Show lifecycle and RDP transport independently. | All API enums are separately mapped. |
| FP02-FR-007 | Show explicit copy for every lifecycle and transport enum. | No blank, generic green or inferred Ready state. |
| FP02-FR-008 | Determine freshness using monotonic receipt-age, not device wall time. | 15-second boundary and wall-clock-jump tests pass. |
| FP02-FR-009 | Expose Connect whenever the identity and assignment are valid and the last known status is not definitively negative, except during an in-flight request or server-directed cooldown. | Stale/missing/UNKNOWN/NOT_CHECKED remains actionable to request a fresh check; definitive negative state is disabled. |
| FP02-FR-010 | Every Connect tap requests fresh broker status and verifies assignmentTag before one no-argument FP-05 handoff. | No stale sample can directly hand off; only a matching fresh RUNNING + RDP_NEGOTIATION_OK result can proceed. |
| FP02-FR-011 | Manual Refresh obtains a new status sample and reports busy, cooldown and outcome. | Duplicate refresh is serialized and server Retry-After is honored. |
| FP02-FR-012 | A timeout, cancellation, 429, mismatch or failed refresh never preserves actionable old readiness. | State becomes indeterminate/non-actionable until a new successful check; retry policy is bounded. |
| FP02-FR-013 | Map the seven parent error codes to their exact English copy. | Snapshot tests equal PRD v1.2 §12 strings. |
| FP02-FR-014 | Provide explicit loading, no-profile, assignment, lifecycle, transport, stale and service-error presentation. | All enumerated states have reviewed copy. |
| FP02-FR-015 | Settings and sign-out delegate to FP-01. | No duplicate authentication or sign-out implementation. |
| FP02-FR-016 | Dock is an informational R2 placeholder only. | No interactive pairing, USB or display route. |
| FP02-FR-017 | Clear prior user's workspace/status on session change, sign-out or invalidation. | Old responses are ignored by session-generation checks. |
| FP02-FR-018 | Provide accessible status, action semantics, announcements, scaling and resource-based copy. | TalkBack, 1.3x and 1.5x font checks pass. |

## 5. Screen behavior

### S-02 Workspace

Show MobiDesk branding, approved greeting, optional college label, one Cloud PC card, lifecycle, RDP transport, last checked age, Refresh status, Connect and the R2 Dock placeholder. Settings and sign-out remain FP-01 actions.

### S-03 Cloud PC status

Show lifecycle, transport, sample age, probe duration if approved for display, freshness and Refresh status. Do not call TCP reachability a successful RDP sign-in. Windows NLA remains a later FP-05 step.

### S-07 Error and recovery

Use the exact parent error catalogue for AUTH-01, ASSIGN-01/02/03 and VM-01/02/03. State-description strings below are presentation copy, not new PRD error codes.

## 6. State copy

All strings are Android resources. English India (en-IN) is the reviewed R1 source locale. Telugu (te-IN) is not shipped until the translations are reviewed and approved; locale support must not be implemented through mixed inline strings.

| API dimension/state | Student label | Supporting text |
|---|---|---|
| Lifecycle STOPPED | Powered off | Your Cloud PC is powered off. Contact the lab administrator. |
| Lifecycle STARTING | Starting | Your Cloud PC is starting or is not ready yet. |
| Lifecycle RUNNING | Powered on | Power is on. Remote Desktop readiness is shown separately. |
| Lifecycle STOPPING | Shutting down | Your Cloud PC is shutting down. Try again when it is ready. |
| Lifecycle ERROR | Needs attention | Your Cloud PC needs attention. Contact the lab administrator. |
| Lifecycle UNKNOWN | Power status unavailable | We could not confirm your Cloud PC power state. Refresh status to try again. |
| Transport NOT_CHECKED | Checking connection | The remote desktop connection has not been checked yet. |
| Transport TCP_UNREACHABLE | Cannot reach remote desktop service | Your Cloud PC cannot be reached right now. |
| Transport TCP_REACHABLE | Service responding | The service responded, but RDP negotiation is not ready. |
| Transport RDP_NEGOTIATION_OK | Ready for Windows sign-in | RDP negotiation succeeded. Your Windows credentials are still required. |
| Transport UNKNOWN | Connection status unavailable | We could not verify the remote desktop service. Refresh status to try again. |

Freshness copy:
- 0–4 seconds upper-bound age: “Updated just now”.
- 5–14 seconds: “Updated {n} seconds ago”.
- At or beyond 15 seconds: “Status needs refreshing”.
- While no sample exists: “Status not checked yet”.
- During an active request: “Checking status…”.

The relative age is computed from monotonic elapsed time, not by subtracting sampledAt from the phone clock. sampledAt is shown only as a formatted observation timestamp.

## 7. Parent error catalogue (unchanged)

| Code | Exact copy |
|---|---|
| AUTH-01 | Your sign-in session expired. Sign in again. |
| ASSIGN-01 | No Cloud PC is assigned to your account. Contact the lab administrator. |
| ASSIGN-02 | We could not verify your assigned Cloud PC. Try again. |
| ASSIGN-03 | Your Cloud PC assignment needs administrator review. |
| VM-01 | Your Cloud PC is powered off. Contact the lab administrator. |
| VM-02 | Your Cloud PC is starting or is not ready yet. |
| VM-03 | Your Cloud PC cannot be reached right now. |

## 8. Assignment consistency contract

FP-03's assigned workspace presentation and FP-04's broker status response MUST contain the same opaque assignmentTag for the same authenticated subject and assignment revision. The tag is generated by a shared trusted assignment resolver, rotates whenever the active assignment or its relevant version changes, and contains no readable student ID, VM ID, VM name or endpoint. It is equality-only metadata, not an authorization credential or target selector.

FP-02 combines a Cloud PC label and status only when the tags match. On mismatch it discards the combined snapshot and reloads profile/assignment and status together once. A second mismatch maps to the existing ASSIGN-02 copy and blocks handoff. No mismatched snapshot is displayed as one coherent Cloud PC.

## 9. Connect policy — single normative rule for FP-02

The Connect button is an action to check and connect, not a reusable authorization granted by a previously displayed status.

**Button availability**
- The student must be FULLY_AUTHENTICATED and have one valid FP-03 Assigned result.
- Keep Connect available when status is missing, stale, lifecycle UNKNOWN, transport UNKNOWN or transport NOT_CHECKED. These are indeterminate states in which a fresh user-initiated check is useful.
- Disable Connect when the latest observed status contains a definitive negative: lifecycle STOPPED, STARTING, STOPPING or ERROR; or transport TCP_UNREACHABLE or TCP_REACHABLE.
- Disable while a status request is in flight, the session/assignment gate is invalid, or the broker Retry-After cooldown is active.
- The separate Refresh status action remains available whenever its local/server cooldown permits.

**Tap behavior**
1. Serialize the tap and obtain the current FP-01 access token.
2. Request GET /v1/vm-status with no body, query parameter or target selector.
3. Enforce the 8-second client deadline and honor cancellation/429 cooldown.
4. Confirm the response assignmentTag equals the currently displayed FP-03 assignmentTag. On mismatch, discard and execute the one-time paired reload in §8.
5. Compute fresh age from the monotonic receipt clock. A successful result is fresh only while its conservative age upper-bound is less than staleAfterSeconds.
6. Hand off to FP-05 exactly once, with no VM ID, assignment selector, endpoint, credential or grant argument, only when lifecycle is RUNNING, transport is RDP_NEGOTIATION_OK, tag matches, the sample is fresh, AuthLevel is still FULLY_AUTHENTICATED, and no refresh/cooldown is active.
7. Otherwise remain in the workspace and render the exact state/error copy.

No cached sample can directly authorize the handoff. The broker and FP-06 independently revalidate assignment and readiness during grant issuance. This UI rule is not a security boundary.

## 10. Data and API boundary

- Workspace/profile data comes from the FP-03 typed result and current user's RLS context.
- Android StatusRepository is implemented and owned by FP-02. It calls the FP-04 endpoint with the current Supabase access JWT.
- FP-04 owns GET /v1/vm-status, server-side assignment derivation, status probe, assignmentTag and rate-limit responses.
- Status DTO allowlist: lifecycle, rdpTransport, sampledAt, probeDurationMs, staleAfterSeconds, requestId and assignmentTag. No VM identity, name or routable field is allowed.
- The parent API contract remains a reviewed proposal until G1 approval; the API addendum in v0.2 records the required changes.

## 11. Acceptance gates

FP-02 is accepted only after: FP-01 AuthLevel integration is verified; FP-03 typed exactly-one result and safe label are integrated; FP-04 status endpoint and assignmentTag contract are deployed in an approved test environment; the client unit/Compose/contract tests pass; privacy scans pass; TalkBack/font-scale review passes; and the FP-05 no-argument handoff is verified. No mock status, fake Ready, hard-coded broker URL or client-side assignment selector is permitted.
