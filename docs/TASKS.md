# MobiDesk — Delivery Tasks

**Version:** 1.2 · **Snapshot:** 3 November 2026  
Owner for every project decision and acceptance: **Project Owner**. Lab IT is consulted on guest accounts, VM operations, certificates and lab networking.

| ID | Task | Acceptance / evidence | Status |
|---|---|---|---|
| MD-001 | Ratify PRD v1.2 | Owner records approval or requested amendments | OWNER REVIEW |
| MD-002 | Freeze canonical repository and pinned FreeRDP version | Confirm only master43721/mobidesk-android is implementation source; pin remains reviewed | VERIFIED BASELINE |
| MD-003 | Confirm distribution and signing model | Play / institutional MDM / direct; key custody and release workflow documented | OPEN |
| MD-004 | Select physical reference Android device | Model, OS/API, ABI recorded; device available for acceptance | OPEN |
| MD-005 | Confirm VM lifecycle/hosting model | Host location, persistent/ephemeral, semester reset and deletion rules approved | OPEN |
| MD-006 | Approve Windows guest identity model | Accounts, reset, lockout, expiry, student support and offboarding documented with Lab IT | OPEN |
| MD-007 | Verify Supabase identity and RLS | Own-profile/assignment and cross-student negative tests pass | NOT STARTED |
| MD-008 | Specify and implement broker readiness endpoint | Lifecycle/readiness separated; freshness and timeouts tested | NOT STARTED |
| MD-009 | Implement grant authorization | No client vm_id; active session check; exactly one assignment; one-use 60s grant | NOT STARTED |
| MD-010 | Implement WSS raw relay | Pinned FreeRDP transport compatibility proven; private upstream only | BLOCKED BY SPIKE |
| MD-011 | Implement revocation/relay termination | <=15s recheck and <=30s termination demonstrated | NOT STARTED |
| MD-012 | Define certificate inventory/rotation | CA or per-VM pin; mismatch fails closed; rollback tested | NOT STARTED |
| MD-013 | Implement RdpSessionActivity | Native FreeRDP surface, lifecycle and input contract tested | NOT STARTED |
| MD-014 | Secure local bridge | Descriptor/Unix socket preferred; fallback one-use authenticated listener; hijack test passes | NOT STARTED |
| MD-015 | Implement Windows credential prompt | Separate credentials, volatile buffers, log/crash redaction, teardown tests | NOT STARTED |
| MD-016 | Add secure session UX and error mapping | All documented errors have honest retry/help behaviour | NOT STARTED |
| MD-017 | Add privacy-safe stage telemetry | Required timing fields, no secrets/PII/screen data | NOT STARTED |
| MD-018 | Run G4 performance plan | All reference-device gates pass with measurement record | NOT STARTED |
| MD-019 | Run security verification | IDOR, replay, revocation, cert, bridge, rate/DoS, root and cleanup tests pass | NOT STARTED |
| MD-020 | R2 dock spike | Compare Pi FreeRDP and phone transcode using latency/FPS/power/thermal criteria | DEFERRED |
| MD-021 | Pilot go/no-go | Owner signs G0-G6 evidence and rollback readiness | NOT STARTED |

Do not mark work complete based on a document, compile or unit test alone. Link each completed task to a reproducible evidence artifact.
