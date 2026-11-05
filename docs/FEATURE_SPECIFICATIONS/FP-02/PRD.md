# FP-02 PRD — Student Dashboard and Workspace

**Version:** 0.2 · **Date:** 5 November 2026  
**Parent:** MobiDesk PRD v1.2 (owner-review draft)  
**Status:** Feature-level owner review. Parent amendment is required for the Connect/freshness and assignment-consistency changes.

## 1. Outcome and scope

After FP-01 confirms a FULLY_AUTHENTICATED student session, MobiDesk shows approved identity, exactly one valid Cloud PC card, separate VM lifecycle and RDP transport, status age, manual refresh, a guarded Connect action, settings/sign-out delegation and an informational R2 Dock placeholder.

FP-02 is a Compose presentation/orchestration feature. It does not authorize assignments, probe VMs, issue grants, collect Windows credentials, perform NLA, open WSS/RDP or implement Dock streaming.

## 2. Functional requirements

| ID | Requirement | Acceptance |
|---|---|---|
| FP02-FR-001 | Gate protected reads on FP-01 FULLY_AUTHENTICATED for the current session generation. | No workspace/status request during restore, signed-out or recovery-only. |
| FP02-FR-002 | Show only the current student's approved profile fields. | Own-user and account-switch tests pass. |
| FP02-FR-003 | Show approved college label only when nonblank. | Missing value is omitted; no hard-coded institution. |
| FP02-FR-004 | Render exactly one Cloud PC card only for FP-03 Assigned. | No VM list or first-row fallback. |
| FP02-FR-005 | Never expose a target selector or routing field. | UI, DTO, navigation and log scans pass. |
| FP02-FR-006 | Show lifecycle and RDP transport independently. | All API enums are separately mapped. |
| FP02-FR-007 | Provide copy for every lifecycle/transport enum. | No blank, generic green or inferred Ready. |
| FP02-FR-008 | Calculate freshness from monotonic receipt-age, not phone wall time. | 15-second boundary and clock-jump tests pass. |
| FP02-FR-009 | Keep Connect available for missing/stale/indeterminate status when auth/assignment are valid and there is no known-negative state or temporary request/cooldown gate. | Stale/UNKNOWN/NOT_CHECKED can initiate a fresh check; known-negative states disable. |
| FP02-FR-010 | Every Connect tap obtains fresh broker status and checks assignmentTag before one no-argument FP-05 handoff. | No stale sample directly hands off; only matching, fresh RUNNING + RDP_NEGOTIATION_OK can proceed. |
| FP02-FR-011 | Manual Refresh obtains a new sample and reports busy/cooldown/outcome. | Duplicate refresh serialized and Retry-After honored. |
| FP02-FR-012 | Timeout, cancellation, 429, mismatch, failed refresh or definitive grant-level assignment invalidation never preserves actionable old readiness. | Failure becomes indeterminate; ASSIGN-03 clears card/status and triggers one paired reload; bounded user retry only. |
| FP02-FR-013 | Use the seven exact parent error strings. | Snapshot tests equal PRD v1.2 §12. |
| FP02-FR-014 | Cover loading, no-profile, assignment, lifecycle, transport, stale and service-error states. | All states have reviewed copy. |
| FP02-FR-015 | Settings and sign-out delegate to FP-01. | No duplicate auth implementation. |
| FP02-FR-016 | Dock is informational R2 placeholder only. | No pairing, USB or display route. |
| FP02-FR-017 | Clear previous-user workspace/status on session change/sign-out/invalidation. | Late responses from old session are ignored. |
| FP02-FR-018 | Provide accessible status/action semantics, announcements, scaling and resource-based copy. | TalkBack, 1.3x and 1.5x font checks pass. |

## 3. Screen behavior

S-02 shows brand, approved greeting, optional college label, exactly one Cloud PC card, lifecycle, RDP transport, last-checked age, Refresh status, Connect and Dock placeholder. Settings/sign-out remain FP-01 actions.

S-03 shows lifecycle, transport, sample age, probe duration if approved, freshness and Refresh status. TCP reachability is not RDP negotiation; RDP negotiation is not Windows NLA.

S-07 uses exact parent error copy. No endpoint, stack trace or raw backend error is shown.

## 4. Complete state copy

All strings are Android resources. R1 source locale is reviewed English India (en-IN). Telugu (te-IN) is a follow-up requiring review by a fluent reviewer; do not ship unreviewed or mixed-language copy.

| API state | Student label | Supporting text |
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

Freshness copy: 0–4s upper-bound age = “Updated just now”; 5–14s = “Updated {n} seconds ago”; >=15s = “Status needs refreshing”; no sample = “Status not checked yet”; in flight = “Checking status…”. Relative age comes from monotonic elapsed time. sampledAt is formatted as an observation timestamp only.

## 5. Assignment consistency

FP-03 assigned workspace presentation and FP-04 broker status response MUST contain the same opaque assignmentTag for the same authenticated subject and assignment revision. The tag is produced by a shared trusted assignment resolver, rotates on assignment/version change, contains no readable user/VM/endpoint data and is equality-only—not a selector, credential or authorization token.

FP-02 combines a label and status only when tags match. On mismatch, discard the pair and reload both once. A second mismatch maps to existing ASSIGN-02 copy and blocks handoff. Never show card A with status B.

## 6. Connect policy — single normative rule

The Connect button is a fresh-check action, not an authorization stored by a previously displayed sample.

**Availability**
- Student must be FULLY_AUTHENTICATED and have one valid FP-03 Assigned result.
- Keep Connect available when status is missing, stale, lifecycle UNKNOWN, transport UNKNOWN or NOT_CHECKED. These are indeterminate and benefit from a fresh user-initiated check.
- Disable for a **fresh** definitive negative: lifecycle STOPPED, STARTING, STOPPING or ERROR; transport TCP_UNREACHABLE or TCP_REACHABLE. Once any sample reaches the stale boundary, its former positive or negative state is indeterminate and Connect becomes available as a fresh-check action.
- Disable while a request is in flight, auth/assignment is invalid, or the broker Retry-After cooldown is active. Manual Refresh remains available when its cooldown permits.

**Tap**
1. Serialize the tap and obtain the current FP-01 access token.
2. GET /v1/vm-status with no body, query or target selector.
3. Enforce the 8s client deadline and honor cancellation/429 cooldown.
4. Verify response assignmentTag equals the current FP-03 tag. Mismatch triggers the one-time paired reload.
5. Evaluate monotonic age upper-bound; fresh iff it is less than staleAfterSeconds.
6. Call FP-05 exactly once, with no VM ID, selector, endpoint, credential or grant argument, only when AuthLevel remains FULLY_AUTHENTICATED, assignment is valid, tag matches, status is fresh, lifecycle RUNNING and transport RDP_NEGOTIATION_OK.
7. Otherwise remain on workspace and render safe state/error copy.

No cached sample directly authorizes handoff. FP-06/broker independently revalidates assignment and readiness at grant issuance. The UI is not a security boundary. If FP-06 or the broker later returns ASSIGN-03 or another definitive assignment-invalid result, FP-02 MUST immediately clear the Cloud PC card and paired status, cancel outstanding status/workspace requests, suppress Connect, and reload the assignment/status pair once. It MUST NOT retry a grant or continue using the previous assignment. A repeated invalid result remains in the safe assignment-review state.

## 7. Parent error catalogue (unchanged)

- AUTH-01: Your sign-in session expired. Sign in again.
- ASSIGN-01: No Cloud PC is assigned to your account. Contact the lab administrator.
- ASSIGN-02: We could not verify your assigned Cloud PC. Try again.
- ASSIGN-03: Your Cloud PC assignment needs administrator review.
- VM-01: Your Cloud PC is powered off. Contact the lab administrator.
- VM-02: Your Cloud PC is starting or is not ready yet.
- VM-03: Your Cloud PC cannot be reached right now.

## 8. Acceptance

Require verified FP-01 AuthLevel; FP-03 typed exactly-one result/safe label/tag; FP-04 status endpoint/tag/429 contract in an approved environment; client unit/Compose/contract tests; privacy and accessibility review; and FP-05 no-argument handoff. No mock Ready, fake VM, hard-coded broker URL or client-side target selector.
