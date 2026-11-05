# FP-02 Open Questions and Required Evidence

**Version:** 0.2 · **Date:** 5 November 2026

Only unresolved external facts/approvals remain here. Closed design choices live in DECISIONS.md.

| ID | Open item | Recommended closure | Status |
|---|---|---|---|
| OQ-01 | Is the selected FP-03 display label safe for students? | FP-03/Owner approve the exact source field and examples; no raw vms.name by assumption | OPEN — FP-03 |
| OQ-02 | Where is the completed FP-01 AuthLevel implementation commit? | Owner reports FP-01 complete. Inspected main MobiDeskViewModel still observes SessionStatus.Authenticated and does not expose FULLY_AUTHENTICATED; identify/merge the correct commit and run recovery-only tests | OPEN — INTEGRATION BLOCKER |
| OQ-03 | Exact FP-03 typed assignment model and assignmentTag resolver? | Publish model contract, zero/one/invalid cases, safe label and resolver source; tests prove stable equality for the same revision and rotation on reassignment | OPEN — FP-03 |
| OQ-04 | Is FP-04 GET /v1/vm-status deployed at approved HTTPS base URL? | Test TLS, current JWT, request-fresh sample, no private response fields, matching assignmentTag and error/429 behavior | OPEN — FP-04 |
| OQ-05 | What clock is used for freshness? | Closed in v0.2: Android elapsedRealtime receipt-age plus conservative request RTT; sampledAt display only; no cached prior sample | CLOSED — API amendment approval remains |
| OQ-06 | Is periodic polling required? | Closed in v0.2: no periodic polling; load on entry, manual refresh and fresh request on Connect | CLOSED — parent amendment approval remains |
| OQ-07 | User copy for missing profile? | FP-03 confirms typed result; use resource-backed safe account-not-provisioned state without placeholder identity | OPEN — FP-03/UX |
| OQ-08 | College label missing behavior? | Closed: omit null/blank label | CLOSED |
| OQ-09 | Does FP-05 expose stable no-argument navigation callback? | Confirm destination/callback name and navigation test | OPEN — FP-05 |
| OQ-10 | May status survive Activity/process recreation? | Do not persist status freshness across process death; reload on re-entry | CLOSED — safe default |
| OQ-11 | Is assignmentTag part of the parent/API contract? | Approve PRD amendment and API_CONTRACT v1.1 draft; FP-03 and FP-04 must use one canonical resolver | OPEN — OWNER/G1 |
| OQ-12 | When is Telugu te-IN copy approved? | R1 English en-IN; add reviewed Telugu translations as a scoped follow-up with a fluent reviewer | OPEN — LOCALISATION |

## Non-negotiable known boundaries

- No student VM selector, assignment mutation or VM power control.
- No endpoint, hostname, IP, port, VM UUID or Guacamole metadata.
- No “Online” inference from assignment existence or TCP_REACHABLE.
- No stale or mismatched sample may be handed to FP-05.
- UI availability is not authorization; grant-time server checks remain mandatory.
- Dock stays informational for R1.

## Evidence format

Close an item only with the decision owner/date, source commit or API revision, relevant test report, affected FR/CORE IDs and any required parent-document update.
