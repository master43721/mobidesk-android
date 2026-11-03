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
