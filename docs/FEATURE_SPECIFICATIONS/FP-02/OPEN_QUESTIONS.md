# FP-02 Open Questions and Required Evidence

**Version:** 0.2 · **Date:** 5 November 2026

| ID | Open item | Recommended closure | Status |
|---|---|---|---|
| OQ-01 | Is the FP-03 display label safe? | Owner/FP-03 approve exact source field and examples; do not assume raw vms.name is safe | OPEN — FP-03 |
| OQ-02 | Where is completed FP-01 AuthLevel source commit? | Owner reports FP-01 complete. Inspected main ViewModel still observes SessionStatus.Authenticated and lacks FULLY_AUTHENTICATED; identify/merge exact implementation and test recovery-only | OPEN — integration blocker |
| OQ-03 | FP-03 typed assignment and tag contract? | Freeze zero/one/invalid model, safe label and shared resolver tag; test rotation on reassignment | OPEN — FP-03 |
| OQ-04 | Is FP-04 status endpoint deployed at approved HTTPS URL? | Verify JWT, request-fresh sample, tag, safe response and 429/Retry-After | OPEN — FP-04 |
| OQ-05 | Which clock determines freshness? | Monotonic elapsedRealtime receipt-age + conservative request RTT; sampledAt display only | CLOSED — parent/API approval pending |
| OQ-06 | Is periodic polling required? | No polling. Load on entry, manual Refresh, fresh request on Connect | CLOSED — parent approval pending |
| OQ-07 | Exact missing-profile copy? | FP-03 confirms typed result; use safe resource-backed account-not-provisioned state | OPEN — FP-03/UX |
| OQ-08 | Missing college label behavior? | Omit null/blank label | CLOSED |
| OQ-09 | Does FP-05 expose a stable no-argument callback? | Confirm destination/callback and navigation test | OPEN — FP-05 |
| OQ-10 | Persist status freshness across process recreation? | No; reload on re-entry | CLOSED — safe default |
| OQ-11 | Is assignmentTag approved in parent/API? | Approve PRD amendment/API v1.1; FP-03/FP-04 use one canonical resolver | OPEN — Owner/G1 |
| OQ-12 | When is Telugu copy approved? | R1 reviewed en-IN; te-IN only with fluent reviewer sign-off | OPEN — localization |

## Non-negotiable boundaries

No VM picker, assignment write, student power control, infrastructure identifiers, fake Ready, TCP-as-RDP inference, stale/mismatched handoff or student-side grant authorization. Dock is informational only in R1.

Close an item only with decision owner/date, source commit or API revision, relevant tests, affected FR/CORE IDs and parent-document update.
