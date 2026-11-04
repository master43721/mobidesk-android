# FP-02 Open Questions and Owner Decisions

**Version:** 0.1  
**Date:** 4 November 2026

Unknowns remain explicit. The feature may implement only safe behavior while a question is open; it must not fill gaps using assumptions or historical data.

## 1. Owner decisions / confirmations

| ID | Question | Recommended default | Why it remains open / closure evidence | Status |
|---|---|---|---|---|
| FP02-OQ-01 | Is the current `vms.name` value approved as the student-facing Cloud PC label? | No raw database VM name unless Owner/FP-03 confirms every value is safe; use a separate approved display label where needed. | Review current values/semantics without exposing private endpoint metadata; document the source field and safe examples. | OPEN |
| FP02-OQ-02 | Which exact FP-01 implementation commit/branch is complete and accepted? | FP-02 must require FP-01's `FULLY_AUTHENTICATED` contract and must not use the raw SDK Authenticated event as a substitute. | Current GitHub main was inspected after PR #5 merge; it still shows the old `MobiDeskViewModel` flow. PR #3 remains open and its body describes a docs-only FP-01 draft. Provide/merge implementation commit and acceptance evidence. | OPEN — INTEGRATION BLOCKER |
| FP02-OQ-03 | What exact typed assignment result will FP-03 expose to FP-02? | One typed state for not-provisioned, no assignment, exactly-one assigned, needs-review/ambiguous and verification-unavailable. | Agree DTO/domain model, label semantics, cardinality, error mapping and tests with FP-03; do not duplicate resolver logic in FP-02. | OPEN — CONTRACT DEPENDENCY |
| FP02-OQ-04 | Is the broker's `GET /v1/vm-status` deployed and reachable at an approved stable HTTPS base URL? | FP-02 consumes the existing contract only; no hard-coded server endpoint or alternate route. | Verify broker process, TLS/DNS, external route, bearer JWT validation and response shape. SSH access alone does not close this item. | OPEN — FP-04 / SERVER DEPENDENCY |
| FP02-OQ-05 | What clock/freshness method is authoritative for `sampledAt` on devices with clock skew? | Fail closed if sample time cannot be trusted; use testable clock logic and align with FP-04 response semantics. | Agree whether response Date header/monotonic age or another verified approach is needed; do not invent a new field unilaterally. | OPEN |
| FP02-OQ-06 | Is periodic status polling required while S-02 is visible? | No periodic polling; load on entry, explicit refresh and before Connect. | Owner approval if a different cadence is needed; include battery/network and stale-state tests. | OWNER REVIEW — PROPOSED DEFAULT |
| FP02-OQ-07 | What is the desired user state when the profile row is missing but Auth is valid? | Show a safe account-not-provisioned state and allow sign-out; no placeholder student identity. | Confirm current FP-03 error result and exact user copy; preserve RLS/roster provisioning boundary. | OPEN |
| FP02-OQ-08 | Should a missing college label be omitted or replaced with a fixed institution name? | Omit it. Do not hard-code a college label. | Owner may approve a fixed label only if backed by product policy and current profile contract. | OWNER REVIEW — PROPOSED DEFAULT |
| FP02-OQ-09 | Does FP-05 provide a stable navigation callback/route for Connect? | FP-02 emits one no-argument Connect handoff after fresh readiness; FP-05 owns credential fields and validation. | Confirm destination name and navigation mechanism when FP-05 package is frozen. | OPEN — FEATURE DEPENDENCY |
| FP02-OQ-10 | Is the current assignment/status response safe to cache across Activity recreation? | Do not persist status across process death; reload/mark unknown on re-entry unless FP-04 freshness can be proven. | Agree with FP-04; test stale cached state and session/assignment changes. | OPEN |

## 2. Non-negotiable known boundaries (not open questions)

- No student VM picker or reassignment action.
- No student VM power controls.
- No endpoint/host/IP/port/UUID exposure.
- No "Online" state inferred from assignment existence or TCP_REACHABLE.
- Connect is disabled when the assignment is absent/ambiguous, Auth is not fully authenticated, lifecycle is not RUNNING, transport is not RDP_NEGOTIATION_OK, status is stale/unknown or refresh is in progress.
- The broker independently rechecks authorization; UI eligibility is not a security control.
- Dock remains R2 informational placeholder.

## 3. Historical evidence warning

`docs/SUPABASE_DISCOVERY.md` is dated 27 October 2026 and explicitly warns that its assignment counts predate a later update. Do not use its row counts, assignment count or VM list as current truth. Re-query the live schema and policies under FP-03 with least privilege and approved access.

## 4. Closure format

Close an item only by adding:
- Owner/date decision or verified implementation evidence.
- Exact repository commit, schema/API contract revision or test report.
- Affected FP-02 requirement IDs and tests.
- Any required updates to PRD, technical design, decisions and implementation plan.
