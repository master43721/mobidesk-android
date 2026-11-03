# PRD v1.2 — intake and implementation baseline

**Status:** Owner-review draft. The source document says Project Owner approval is still required. It is stored here unchanged as [`MobiDesk_PRD_v1.2.pdf`](MobiDesk_PRD_v1.2.pdf). Until approval is recorded, treat the requirements below as the proposed R1 target; do not call them formally approved or deploy against an unreviewed interpretation.

**Scope:** R1 is a student Android client for one assigned Windows Cloud PC. R2 dock/external-monitor functionality is explicitly deferred. The product is native Kotlin/Jetpack Compose, uses the existing Supabase Auth/RLS model, and must not let the client choose a VM or endpoint. Supabase passwords are never Windows credentials.

## Repository baseline inspected on 2026-11-03

- Canonical repository: private `master43721/mobidesk-android`; branch `main` was clean at commit `4986006` when first inspected. PRD documentation was later pushed as `cbcdea6`, and the first CI workflow as `a6c7282`; current CI/manifest/audit fixes are pending validation and push.
- Android shell, Supabase Auth/session recovery and RLS-scoped student/VM reads exist. The dashboard does not yet launch a remote desktop or query a trusted live-status service.
- FreeRDP Android is a git submodule pinned to v3.32.0, commit `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1`. Existing build documentation reports native libraries for `arm64-v8a` and `x86_64`; that is not proof of an RDP session.
- A fresh local build after manifest hardening passed 2 JVM unit tests, compiled FreeRDP native code and assembled a debug APK. The merged manifest contains zero exported FreeRDP components. The APK passed ZIP and signature verification; its SHA-256 and size are recorded in `PROGRESS.md`. It is a debug build, not an RDP/emulator/release acceptance artifact.
- A GitHub Actions workflow now exists. Its first run (`41585309927`) failed before Gradle because the setup action requested the retired `tools` SDK package. The workflow is locally corrected to use the SDK Manager present on the hosted runner; the rerun is pending.
- The FreeRDP source audit is complete: see [`FREERDP_ANDROID_API_AUDIT.md`](FREERDP_ANDROID_API_AUDIT.md). Native C `rdpTransportIo` hooks exist, but the checked-in Android Java wrapper exposes no raw socket/stream injection method. RD Gateway WebSocket uses HTTP upgrade and RD Gateway framing; it is not a transparent raw-RDP relay. A native/JNI transport spike is a prerequisite, not an optional UI task.
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
4. Use the completed pinned-source audit; build a native/JNI transport spike and prove protected byte-stream composition with FreeRDP TLS/NLA and Android lifecycle before app session implementation. Stop if it cannot meet the threat model.
5. Implement a MobiDesk-owned session coordinator and non-exported session Activity, protected local transport, separate volatile Windows-credential prompt, certificate pinning, lifecycle cleanup, safe input controls and privacy-safe telemetry.
6. Run broker/unit/instrumentation and negative authorization tests, then physical-device and controlled-network performance/security gates.
7. Produce the Owner-signed release only after G0–G5 evidence is recorded. A compile, debug APK or mock is not R1 completion.

See [`TEST_PLAN.md`](../TEST_PLAN.md), [`OPEN_ITEMS.md`](../OPEN_ITEMS.md), [`PROGRESS.md`](../PROGRESS.md), and [`TODO.md`](../TODO.md) for actionable evidence and gate status.
