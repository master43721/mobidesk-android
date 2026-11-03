# MobiDesk — Verification and Acceptance Test Plan

**Version:** 1.2 · **Date:** 3 November 2026.

## Test principles
Use synthetic/test accounts and controlled VMs. Do not test with student production credentials or expose student rows. Each pass records commit, build, device/OS, test data version, procedure/command and evidence location.

## Identity and assignment
- Valid student signs in and restores only their own session.
- Student reads only their own profile and assignment through RLS.
- Anonymous, expired and revoked access is denied.
- Cross-student reads and altered VM destination requests are denied.
- Zero or multiple assignments fail closed; no default VM appears.
- No self-registration or client-side assignment mutation exists.

## Status and broker
- Status derives authenticated assignment and returns no hostname/IP/port.
- Lifecycle and RDP readiness are separate.
- Probe timeout <=1500ms; status stale after 15s.
- TCP reachability is not reported as NLA success.
- Invalid/revoked token, ambiguous assignment, rate and capacity limits fail safely.

## Grant and relay
- Verify grant entropy, 60s expiry, atomic single use, hash-at-rest and replay/race behaviour.
- Grant is bound to user, active Auth session and assigned VM.
- Assignment/session revocation is checked <=15s; active relay closes <=30s.
- Broker reaches only configured private endpoints; app cannot override destination.
- Relay enforces size limits, backpressure, idle/total timeout and concurrency.
- No public TCP/3389 exposure is found.

## RDP, credentials and bridge
- Prove FreeRDP v3.32.0 WSS/raw transport using exact pinned APIs and framing.
- Windows NLA uses separate guest credentials; failures re-prompt safely.
- Credentials absent from logs, crash reports, analytics, clipboard and persistent storage.
- RDP CA/pin validates; bad/missing/rotated pin fails closed.
- Local bridge rejects unauthorized connections; listener accepts one connection then closes.
- Disconnect, app destruction, network loss, grant expiry and auth revocation release native buffers, sockets, relay and callbacks.

## Android interaction and privacy
- Connect states/errors are accessible and truthful.
- Touch, pointer, keyboard, modifier locks, scrolling, rotation and resize tested.
- FLAG_SECURE protects normal screenshots and recent-task previews.
- Root policy and exception path verified on test devices.
- Background/screen lock follows foreground-only R1 teardown rules.

## Performance / release gates
Execute PERFORMANCE_PLAN.md and report sample count, p50 and p90. G0-G6 in PRD.md are mandatory. Emulator-only tests do not satisfy physical-device RDP/performance acceptance. A successful build does not satisfy a live RDP test.
