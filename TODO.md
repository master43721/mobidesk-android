# MobiDesk Android — Live Checklist

## Phase 0 — Discovery and baseline
- [x] Inspect the existing workspace/GitHub account; keep public `master43721/mobidesk-updated` untouched.
- [x] Create private repository `master43721/mobidesk-android`.
- [x] Pin upstream FreeRDP v3.32.0 at `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1` as a submodule and inspect its Android core.
- [x] Inspect connected Supabase schema, migrations, triggers, RLS, grants, Auth settings, and advisors without selecting personal rows.
- [x] Confirm counts only: 7 Auth users, 7 roster rows, 7 student profiles, 1 VM, 1 assignment.
- [x] Add `com.mobidesk.android://auth-callback/recovery` to the Supabase Redirect URLs allowlist; preserve existing Site URL/provider/SMTP settings.
- [x] Document findings in `docs/SUPABASE_DISCOVERY.md` and `docs/PLATFORM_NOTES.md`.

## Phase 1 — Product specification and UI/UX
- [x] Choose native Kotlin/Jetpack Compose as the Android shell after the user authorized framework choice; keep FreeRDP as the native RDP engine.
- [x] Define screen structure and truthful loading, no-profile, no-assignment, and unconfigured-session states.
- [x] Remove the demo dashboard/preview path so no sample VM can be mistaken for a real assignment.
- [ ] Implement a role-protected admin dashboard only after an authoritative admin role and server-side authorization are defined.

## Phase 2 — Native Android foundation
- [x] Add Android project shell, MobiDesk theme/brand assets, Compose app flow, and internal FreeRDP module dependency.
- [x] Add local-only Supabase config template and ignore `local.properties`; do not commit the public key or any privileged key.
- [x] Keep FreeRDP session Activities non-exported and app cleartext traffic disabled.
- [ ] Compile the app and resolve build/static errors with an authorized Android SDK/NDK/CMake toolchain.
- [ ] Build/install the APK on a real Android Emulator; no emulator verification has occurred yet.

## Phase 3 — Authentication and assignment authorization
- [x] Integrate Supabase email/password sign-in with PKCE Auth configuration and default Android session persistence.
- [x] Add sign-out, safe user-facing errors, password-reset request, and deep-link recovery/password update.
- [x] Gate recovery UI on a successfully exchanged PKCE session callback.
- [x] Query the signed-in user's own `students(id,name,college)` row and assigned `vms(id,name)` only; rely on verified RLS and explicit column selection.
- [x] Keep app registration unavailable; provisioning remains controlled by the existing roster-trigger function.
- [ ] Compile, run tests, and verify sign-in/profile reads using an authorized test account without collecting its password.
- [ ] Verify recovery email delivery; custom SMTP is currently disabled, and default Supabase mail delivery may be limited.
- [ ] Test anonymous and cross-student reads with real Auth/RLS evidence.

## Phase 4 — Student/admin dashboards
- [x] Implement a live student workspace screen with server-backed profile and assigned-VM list states.
- [x] Show no assignment honestly and avoid inferred online/offline status.
- [ ] Add live VM status only after a trusted status endpoint exists.
- [ ] Add start/stop actions only after the control API authorizes and audits those operations.
- [ ] Add admin inventory only after a trusted admin-role model and server enforcement exist.

## Phase 5 — Secure VM control API
- [ ] Define authenticated VM status, power, assignment, short-lived connection-ticket, audit, and error contracts with the API owner.
- [ ] Verify user identity server-side and authorize every VM/action from trusted data.
- [ ] Allowlist IDs/actions; avoid unsanitized shell commands.
- [ ] Deploy and security-test the API. Supabase currently has no Edge Functions/control API.

## Phase 6 — FreeRDP feasibility and secure connection
- [x] Pin the official FreeRDP Android source and declare the `freeRDPCore` dependency.
- [ ] Build the native client for supported phone/emulator ABIs.
- [ ] Launch a minimal session on a controlled, reachable test VM.
- [ ] Pass only a valid, expiring server-issued ticket into the native client; verify lifecycle, rendering, certificate checks, and cleanup.
- [ ] Verify Android network reachability without Tailscale or public TCP/3389; do not assume Cloudflare HTTPS carries native RDP.

## Phase 7 — Native desktop interaction
- [ ] Verify display scaling/orientation/dynamic resolution.
- [ ] Verify direct-touch, trackpad/pointer mapping, right-click, and scrolling.
- [ ] Verify soft/hardware keyboard, text composition, scancodes, and shortcuts.
- [ ] Implement and test OFF → LATCHED → LOCKED modifier states with safe reset.
- [ ] Verify clipboard and separately assess audio/other RDP channels.
- [ ] Measure startup latency, frame rate, CPU/memory, and reconnect behavior on representative hardware.

## Phase 8 — Reliability and security
- [ ] Handle connecting/connected/disconnecting/disconnected/failed/expired-ticket states, network loss, process recreation, and cleanup.
- [ ] Review Auth session storage, TLS validation, ticket/credential logging, clipboard privacy, and logout behavior.
- [ ] Test unreachable VM, denied credentials, backend outage, expired ticket, and unauthorized assignment.

## Phase 9 — End-to-end and device tests
- [ ] Run unit and Android instrumentation tests; the code exists but has not been run.
- [ ] Verify real Supabase Auth, assignments, and recovery on a test Android Emulator.
- [ ] Verify actual Windows RDP, input, keyboard, scaling, clipboard, and supported channels on a physical device.
- [ ] Verify external network transport and attempt cross-student/admin authorization tests.

## Phase 10 — Release
- [ ] Build actual APK/AAB with the authorized toolchain.
- [ ] Use an owner-controlled signing key and production-safe endpoints.
- [ ] Install and smoke-test a physical-device build; record tested Android versions/device coverage.
- [ ] Document capabilities, requirements, operational setup, and limitations.

## Current status snapshot

- **Implemented in source, not compiled:** native Compose shell; Supabase email/password auth and session restoration; password recovery callback; RLS-scoped current-student/VM queries; honest empty/error states; tests authored.
- **Supabase project:** project and existing schema verified; one recovery redirect added. No DDL or data-row changes. No admin role, live VM status, control API, or Edge Functions found.
- **Build/test:** not run. The sandbox has no Android SDK, Gradle, Kotlin compiler, ADB, Android Studio, or `/dev/kvm`; no Android SDK/NDK license acceptance has been granted.
- **AI Studio:** imported workspace is a separate static copy and is not live-synced to GitHub; its prior native build was blocked by the omitted FreeRDP submodule. It does not represent these current source changes until re-imported.
- **Still incomplete for product release:** VM control API, actual FreeRDP session launch/ticket flow, secure external RDP transport, admin authorization, live status/power, emulator/physical device tests, owner-signed release artifact.
