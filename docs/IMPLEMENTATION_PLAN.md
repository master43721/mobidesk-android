# MobiDesk — Gated Implementation Sequence

**Version:** 1.0 · **Date:** 3 November 2026
**Owner:** Project Owner; Lab IT consulted for guest-account and VM operations.

This file summarizes sequencing. Hour estimates, detailed dependencies and dates are authoritative in TASKS.md.

| Phase | Work | Exit evidence | Blocks next phase? |
|---|---|---|---|
| P0 / G0 — Baseline | Confirm canonical repo/stack; inspect source/Gradle/manifest/ABIs/gitlink/branches; preserve PRD v1.2 | BASELINE_AUDIT.md with [VERIFIED]/[UNKNOWN] facts; owner issues recorded | Stack/repo mismatch would stop work. None found. |
| P1 / G1 — Design freeze | Complete API/OpenAPI, threat model, TECHNICAL_SPEC, DESIGN, test strategy, traceability and decision register | Every PRD MUST traced; error catalogue and API schema reviewed; FR-SEC gap explicitly recorded | Yes: unresolved API/security contracts block implementation |
| P2 / G2 — Broker | JWT/session verification; assignment resolution; readiness; grant issue/consume; WSS relay; revocation; allowlist; rate/concurrency/bounds | Contract/negative tests, no public RDP, revocation <=30s, VM4 excluded | Yes: blocks client end-to-end RDP |
| P3 / G3 — Native client | Prove raw-stream transport and bridge; add coordinator, RdpSessionActivity, render target, credentials, cert validation, input and cleanup | Real Windows NLA/RDP session using exact pinned FreeRDP; secure bridge and full teardown | Yes: requires all critical FreeRDP spikes to pass |
| P4 / G4 — Device acceptance | RD-1 tests, 20 attempts, p90, 120s FPS workload, input latency, 20-cycle cleanup, recovery and negatives | All PRD NFR-PERF and security tests pass with reproducible private evidence | Yes: no pilot release before pass |
| P5 / G5 — Release/operations | Channel/key decision, signed artifact, static scan/SBOM, pin rotation, support, backup/restore, retention, incident and rollback | Owner go/no-go and Lab IT operational sign-off | Terminal release gate |
| R2 — Dock | Compare phone transcode vs Pi-native FreeRDP; key-backed pairing; external display/input UX | Comparative latency/FPS/power/thermal/recovery test; owner decision record | Not required for R1 |

## Mandatory Live Drill Acceptance Gate

Per `docs/MANDATORY_LIVE_DRILL_POLICY.md`, **every Feature Point (FP-01 through FP-28)** across all phases must satisfy the mandatory 9-stage sequence:
1. Implementation -> 2. Automated tests -> 3. Build verification -> 4. Independent implementation review -> 5. Live-drill preparation -> 6. Explicit live-drill handoff request to Owner -> 7. Owner live demonstration in target environment -> 8. Evidence capture & drill review -> 9. Explicit Owner acceptance.

No feature point may be marked complete or accepted merely because automated tests or code review passed. `LIVE_DRILL_STATUS: PASS` and recorded Project Owner sign-off are blocking exit criteria for every FP.

## Critical path
G1 contract/threat freeze -> G2 secure broker -> G3 FreeRDP transport/render/NLA -> G4 physical validation -> G5 distribution and operations.

## Stop conditions
- No raw RDP transport compatibility proven against pinned FreeRDP v3.32.0.
- LocalBridge peer identity cannot be authenticated.
- Certificate mismatch/unknown trust can be accepted.
- Broker cannot check active session_id or close relay within 30 seconds.
- Exactly-one assignment or VM allowlist cannot be enforced; VM4 is proposed before verification.
- RD-1 phone is not confirmed before physical performance acceptance.

Do not work around a stop condition by using a direct client-to-VM socket, accepting all certificates, trusting USB strings, adding a placeholder endpoint, or storing Windows credentials.


## College Administrator implementation track — owner-directed R1 scope

This is an additional R1 critical path, not an excuse to bypass the existing G0–G5 RDP gates.

1. **A0 — Authority and bootstrap:** freeze college_admin/student role source, bootstrap the single admin out-of-band, enroll MFA, and prove that role checks are server-side.
2. **A1 — Roster and invitation:** preserve the roster-gated Auth trigger; implement roster-first student invitation through a trusted server; prove the actual Supabase Kotlin 3.6.0 INVITE callback and email delivery.
3. **A2 — RLS and admin contract:** review schema/policies; freeze admin endpoints for account lifecycle, assignments, VM operations, sessions and audit. A proposed route name is not an implemented API.
4. **A3 — Administrator workspace:** add role-aware navigation, student directory/invitation, assignment view, server/VM health/actions and connection support screens.
5. **A4 — Privilege and operations acceptance:** run T-ADMIN-01..15; verify student isolation, admin action allowlist, session revocation, audit completeness and no secret in the APK.
6. **A5 — Controlled pilot:** only after invitation delivery, account role/RLS, host agent, action rollback, audit and RDP student acceptance gates pass.

**Stop conditions:** no admin screen may execute a privileged operation without a working trusted backend check; no admin Auth secret may be added to Android; no student signup setting is disabled until the replacement invite flow has passed; no host lifecycle action ships without a fixed allowlist, confirmation, timeout and rollback path.
