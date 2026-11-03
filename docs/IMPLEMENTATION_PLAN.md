# MobiDesk — Implementation Plan

**Version:** 1.2 · **Date:** 3 November 2026 · **Owner:** Project Owner.

| Phase | Work | Exit criteria | Dependencies |
|---|---|---|---|
| P0 — Freeze | Confirm canonical repo, inspect main, pin FreeRDP and preserve source-of-truth rules | Commit/gitlink recorded; clean reviewed baseline | Owner |
| P1 — Prove integration | FreeRDP transport, certificate callback, native Activity/surface and local bridge spikes | Each spike has reproducible test evidence; incompatible designs rejected early | P0 |
| P2 — Broker foundation | Identity/session validation, assignment resolution, VM status and safe errors | Cross-student, none/multiple assignment and stale status tests pass | P0, owner decisions |
| P3 — Grant and relay | One-use grants, WSS relay, allowlist, rate/concurrency, revocation and cleanup | Contract/security tests pass; no public RDP exposure | P1, P2 |
| P4 — Android RDP session | Credential prompt, coordinator, native Activity, FreeRDP adapter, input and error flow | Approved test VM opens real NLA session; cleanup tests pass | P1, P3 |
| P5 — Hardening | Certificate rotation, bridge-hijack tests, session revocation, logging/privacy and fault injection | PRD G5 security gates pass | P3, P4 |
| P6 — Performance/pilot | Physical device measurements, user cohort, operator runbook and rollback | G4 gates pass; owner signs G6 go/no-go | P5, device decision |
| P7 — Dock R2 | Compare phone transcode and Pi-native FreeRDP; key-based pairing | Decision record backed by FPS/latency/power/thermal/recovery results | R1 evidence |

## Execution rules
- Keep chunks small and documentation/source changes separately reviewable.
- Do not edit app code while a prerequisite integration spike is unresolved.
- For each implementation task, record changed files, test command, result, evidence and rollback.
- No changes to VM power state, server firewall, certificates, student assignments or guest accounts without explicit approval.
- A compile is not end-to-end acceptance; release requires measured physical-device evidence.
