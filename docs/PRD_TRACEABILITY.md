# PRD v1.2 — intake and implementation baseline

**Status:** Owner-review draft. The source document says Project Owner approval is still required. It is stored here unchanged as [`MobiDesk_PRD_v1.2.pdf`](MobiDesk_PRD_v1.2.pdf). Until approval is recorded, treat the requirements below as the proposed R1 target; do not call them formally approved or deploy against an unreviewed interpretation.

**Scope:** R1 is a student Android client for one assigned Windows Cloud PC. R2 dock/external-monitor functionality is explicitly deferred. The product is native Kotlin/Jetpack Compose, uses the existing Supabase Auth/RLS model, and must not let the client choose a VM or endpoint. Supabase passwords are never Windows credentials.

## Repository baseline inspected on 2026-11-03

- Canonical repository: private `master43721/mobidesk-android`; branch `main` was clean at commit `4986006` when inspected.
- Android shell, Supabase Auth/session recovery and RLS-scoped student/VM reads exist. The dashboard does not yet launch a remote desktop or query a trusted live-status service.
- FreeRDP Android is a git submodule pinned to v3.32.0, commit `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1`. Existing build documentation reports that the native libraries are packaged for `arm64-v8a` and `x86_64`; that is not proof of an RDP session.
- The 2026-10-27 build report records a 207,696,333-byte debug-signed APK, unit-test/build success and no emulator, physical-device, real-login or RDP acceptance. It is a historical development artifact, not the final APK.
- No GitHub Actions workflow existed at the time of this audit.
- A separate local broker prototype exists outside this repository. A read-only audit found 23 passing local unit tests, but the prototype is not deployed or checked into this repository and is not PRD-conformant: it lacks `/v1/vm-status`, active Supabase-session validation, periodic in-session revocation checks, and stable TLS/WSS ingress; it returns VM ID/name in its grant response and its issuance throttling is not the PRD's 5-per-minute policy. Do not expose or deploy it as-is.

## External baseline checked read-only on 2026-11-03

- Tailscale SSH authenticated as `administrator` on `mobilab-server`; the host reports Ubuntu 22.04.5 LTS, 24 logical CPUs, 15 GiB RAM and about 6 GiB available at inspection.
- Libvirt showed `win-01` through `win-05` running; `linux-01` and `win_10-lab` were shut off. No MobiDesk broker service was active and no listener on its prototype port was observed.
- Host `xrdp.service` was active and a host TCP/3389 listener was bound to a wildcard address. Public reachability was **not** established. UFW status could not be read because this account has no passwordless sudo. Do not disable or alter xrdp; it may be the owner's GUI access path. Verify firewall/upstream reachability before any release claim.
- A `cloudflared` process and Guacamole-related listener were observed, but no permanent MobiDesk WSS route was verified. A Quick Tunnel is not a stable production hostname.
- No server, VM, firewall, tunnel or Supabase configuration was changed during this inventory.

## Material reconciliation issue — fail closed

The last dated Supabase assignment audit (2026-10-30) records five student-to-VM assignments across the five Windows VM catalog entries. This PRD instead says only four endpoints are currently approved and specifically says VM4 remains unverified and must be excluded until verified. These facts must be reconciled with the owner before any grant can route to VM4 or any account is reassigned. Do not silently alter the assignment table, substitute a VM, or expose a guessed endpoint. Re-read current Supabase rows and verify the exact approved endpoint allowlist before end-to-end testing.

## R1 implementation sequence

1. Record Owner approval of this PRD and freeze the exact repository commit/submodule.
2. Complete `API_CONTRACT.md`, `SECURITY_THREAT_MODEL.md`, and `TEST_PLAN.md`; reconcile assignment and server-readiness facts.
3. Implement broker identity/session checks, unique assignment lookup, live lifecycle plus bounded RDP readiness, one-time grant, allowlist-only relay, and revocation during an active relay.
4. Inspect the pinned FreeRDP v3.32.0 Android source for its actual session Activity, render surface, credentials, certificate callback, and raw-stream transport APIs. Do not infer intent extras or equate Microsoft RD Gateway WebSocket with the custom raw-byte relay.
5. Implement a MobiDesk-owned session coordinator and native session Activity, a protected local transport, a separate volatile Windows-credential prompt, certificate pinning, lifecycle cleanup, safe input controls and privacy-safe telemetry.
6. Run broker/unit/instrumentation and negative authorization tests, then physical-device and controlled-network performance/security gates.
7. Produce the owner-signed release only after G0–G5 evidence is recorded. A compile, debug APK or mock is not R1 completion.

See [`TEST_PLAN.md`](../TEST_PLAN.md), [`OPEN_ITEMS.md`](../OPEN_ITEMS.md), [`PROGRESS.md`](../PROGRESS.md), and [`TODO.md`](../TODO.md) for actionable evidence and gate status.