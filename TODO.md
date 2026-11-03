# MobiDesk R1 — Live Checklist

**Target:** [`docs/MobiDesk_PRD_v1.2.pdf`](docs/MobiDesk_PRD_v1.2.pdf), currently an owner-review draft pending approval. R1 is native Android + secured broker + assigned Windows VM. R2 dock support is deferred. Keep each completed engineering milestone committed and pushed, and record its evidence in [`PROGRESS.md`](PROGRESS.md).

## G0 — Product and baseline freeze

- [x] Create private canonical repo `master43721/mobidesk-android`; leave public `mobidesk-updated` untouched.
- [x] Pin official FreeRDP Android source as a submodule at v3.32.0, commit `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1`.
- [x] Record the owner-provided PRD v1.2 in `docs/MobiDesk_PRD_v1.2.pdf` and map its requirements to tests.
- [ ] Project Owner approves the PRD/architecture; record approval and exact repo/submodule baseline before treating it as normative.
- [ ] Reconcile the last-reported five student assignments with the PRD's four currently approved endpoints and VM4 exclusion. Re-read live Supabase state; do not reassign users or route to an unverified VM.

## Android foundation and CI

- [x] Build native Kotlin/Jetpack Compose shell with Supabase Auth, session restoration, sign-out, recovery, and RLS-scoped profile/assignment reads.
- [x] Keep self-registration, role inference by email, fake VM entries, and direct VM power controls out of the app.
- [x] Install the owner-approved Android toolchain; historical build report records successful Kotlin/FreeRDP-core compilation, JVM unit tests, debug APK assembly, ZIP and debug-signature verification.
- [ ] Add GitHub Actions for the pinned submodule, exact SDK/NDK/CMake, JVM tests, debug build, and artifact upload; label the artifact as development-only.
- [ ] Run Android instrumentation tests on a booted emulator. This is now authorized but has not yet been demonstrated in this continuation.
- [ ] Re-run local tests/build and record current result in `PROGRESS.md`; do not infer it from the October report.
- [ ] Perform an authorized live Supabase test and anonymous/cross-student RLS-negative tests without collecting a password or exposing row contents.
- [ ] Verify password recovery email delivery; custom SMTP is disabled in the last recorded settings snapshot.

## G1 — Contract and threat-model freeze

- [ ] Record Owner approval of `API_CONTRACT.md` and `SECURITY_THREAT_MODEL.md` before implementing production behavior.
- [ ] `GET /v1/vm-status`: no client VM ID; authenticated; separate lifecycle and RDP transport readiness; sample timestamp; stale after 15 seconds; timeout is UNKNOWN.
- [ ] `POST /v1/session-grants`: authenticated empty body; no VM ID/name/host/IP/port; server rechecks assignment/readiness/session; random ≥256-bit one-use grant, ≤60-second TTL, bound to Supabase user/session and server-resolved target.
- [ ] Choose and review server-side active Supabase `session_id` lookup; check at issuance and during relay. Do not include service-role/database credentials in the APK.
- [ ] Approve per-VM RDP certificate pin/internal-CA lifecycle and protected local FreeRDP-to-WSS transport.

## G2 — Broker readiness

- [ ] Bring the existing out-of-repo broker prototype into the canonical repository only after review and contract alignment. It is not deployed or conformant today.
- [ ] Implement status/readiness probe, approved server-side allowlist, generic failures, bounded request/message queues, 5-per-minute/user rate limit, one active session/user and bounded pilot concurrency.
- [ ] Implement in-session assignment/session rechecks at least every 15 seconds and disconnect within 30 seconds after revocation.
- [ ] Verify only Owner-approved Windows endpoints; keep VM4 fail-closed until its endpoint/OS is explicitly verified and approved.
- [ ] Obtain a stable owner-controlled HTTPS/WSS route. Do not use the temporary Guacamole Quick Tunnel as the MobiDesk endpoint or expose the broker port directly.
- [ ] Review host xrdp TCP/3389 binding and firewall/upstream access without disrupting the owner's GUI; ensure no public guest TCP/3389 path.
- [ ] Deploy only after the PRD, threat model, access route, session revocation and rollback plan are approved; verify as a dedicated least-privilege service.

## G3 — Native Android session

- [ ] Inspect the exact pinned FreeRDP v3.32.0 Android source for its real Activity, render-surface, credential, certificate callback and raw-stream transport APIs. Do not guess extras or equate RD Gateway WebSocket with the custom raw-RDP WSS relay.
- [ ] Complete the RDPEDISP/surface attach-detach/rotation spike and record the R2 reconnect-or-migration decision.
- [ ] Implement a MobiDesk-owned non-exported `RdpSessionActivity`, render-target abstraction, session coordinator and status/grant clients.
- [ ] Prompt for separate Windows credentials **before grant issuance**; keep them in volatile memory only and clear them on every success/failure/cancel path.
- [ ] Prove a protected, process/session-owned local byte-stream bridge (socketpair preferred; Unix socket with peer UID next; authenticated one-use local fallback only after security proof).
- [ ] Validate the exact VM certificate pin at the RDP/TLS handshake; missing/mismatch fails closed. Add NLA and safe errors.
- [ ] Implement touch/trackpad, pointer, keyboard/modifiers, Ctrl+Alt+Delete, `FLAG_SECURE`, lifecycle cleanup, bounded reconnect, and privacy-safe telemetry.

## G4 — Physical R1 acceptance

- [ ] Use the nominated physical Pixel 6 / Android 16 / API 36 or an Owner-approved replacement; emulator results do not substitute.
- [ ] Prove correct assignment only, cross-user/expired/replay/unmapped/revoked denials, and immediate cleanup.
- [ ] Complete Windows NLA on a real approved guest using separate Windows credentials and approved certificate pins.
- [ ] Measure the PRD's 20 controlled attempts, first-frame p90, 120-second FPS workload, input p90, reliability, recovery and 20-cycle cleanup gates on controlled Wi-Fi.
- [ ] Verify no credential/token/grant/endpoint/screen data leaks into disk, logs, crash reports, telemetry or URLs.

## G5 — Release and operations

- [ ] Define backup/restore, retention, semester-end deletion, support, maintenance and rollback procedures.
- [ ] Build reproducibly with production-safe config; no demo/mock fallback, trust-all certificate policy, cleartext, or test endpoint.
- [ ] Sign with the Owner-controlled key/upload key. Record signer certificate, source commit, artifact SHA-256, device/test evidence, and distribution route.
- [ ] Deliver only the exact signed APK that passed physical R1 acceptance; a debug APK or successful compile is not product completion.

## Current snapshot

- Canonical repo was clean on `main` at `4986006` before this PRD intake update; no GitHub Actions workflow existed then.
- Historical 2026-10-27 artifact: `app/build/outputs/apk/debug/app-debug.apk`, package `com.mobidesk.android`, version `0.2.0`/code `1`, target SDK 37, min SDK 29, about 198 MiB, debug-signed. It is not final.
- Supabase's last dated audit (2026-10-30) recorded five assignments; the PRD says only four RDP endpoints are approved. Live state and the VM4 discrepancy must be rechecked before connecting.
- Server access is now authorized and a read-only Tailscale SSH inventory was performed on 2026-11-03. The host is Ubuntu 22.04.5; five Windows guests were running and two other domains were off. The broker is not deployed. Host xrdp listens on wildcard TCP/3389; public reachability/firewall were not verified. No server configuration was changed.
- A local broker prototype outside this repo had 23 passing local unit tests in its last review, but is not deployed or PRD-conformant. See `docs/PRD_TRACEABILITY.md` and `OPEN_ITEMS.md`.
- No emulator/physical-device RDP test, stable MobiDesk WSS route, owner-approved RDP pin inventory, Windows pilot credentials, or owner-signed release artifact is evidenced yet.
