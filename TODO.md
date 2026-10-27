# MobiDesk Android — Live Checklist

## Phase 0 — Discovery and baseline
- [x] Confirm no current MobiDesk working copy was provided in the task workspace; inspect the account for an existing GitHub project before creating a new one.
- [x] Create new private repository `master43721/mobidesk-android`; leave public `master43721/mobidesk-updated` untouched per the user's clean-start instruction.
- [x] Confirm baseline build environment: Java 21 is present; Flutter, Dart, Gradle, Android SDK/ADB/emulator, and Android Studio executable are absent. `/dev/kvm` is unavailable.
- [x] Inspect official FreeRDP Android project. Pin candidate upstream release v3.32.0 at commit `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1`; its official Android client includes Java UI, FreeRDP JNI/native libraries, and Gradle/CMake build files.
- [ ] Establish a successful reproducible Android build in this environment.
- [ ] Establish whether SDK Emulator can boot without hardware acceleration; if not, obtain a real emulator-capable environment rather than claiming emulator verification.

## Phase 1 — Product specification and UI/UX
- [ ] Implement the screen inventory/navigation from `plan.md`: splash/session restoration; sign-in and recovery/error; student dashboard and assigned Cloud PC; VM status and connection preparation; native desktop; reconnect/disconnected/failure; settings/profile; safe exit; role-protected admin dashboard.
- [ ] Ensure only trusted backend assignment data can drive a student's VM. Client-supplied VM IDs/roles are not authorization.
- [ ] Ensure admin actions are server-authorized and audited; hiding a button is not the boundary.
- [ ] Keep unknown/offline/unconfigured status explicit; never present historical/sample data as live.
- [ ] Implement only desktop toolbar controls with a real verified behavior; handle touch, keyboard, sticky modifiers, clipboard, scaling, session exit, and lifecycle according to the upstream engine's supported APIs.

## Phase 2 — Native Android foundation
- [ ] Create app structure, theme, navigation, configuration boundaries, accessible reusable components, loading/empty/offline/error states, and app identity.
- [ ] Keep all service credentials out of APK/source/logs. Public client configuration may use documented public values only; privileged secrets remain server-side.
- [ ] Add build/test automation and a repeatable debug APK build.
- [ ] Build and install on an actual Android Emulator before claiming emulator validation.

## Phase 3 — Authentication and assignment authorization
- [ ] Integrate Supabase Auth, session restore/logout/recovery, trusted profile/role retrieval, and assignment lookup after the user supplies the actual project configuration/schema.
- [ ] Confirm row-level security; test anonymous access, cross-student reads, self-promotion, and unauthorized assignment changes.
- [ ] Do not put a Supabase service-role key in the APK.

## Phase 4 — Student/admin dashboards
- [ ] Student sees only their server-authorized assigned VM and a real backend status.
- [ ] Represent online, offline, starting, stopped, unknown, and error distinctly; add refresh and useful failure handling.
- [ ] Admin sees authorized inventory/actions only after trusted role/API enforcement exists; expand inventory without hardcoding only five VMs.

## Phase 5 — Secure VM control API
- [ ] Define authenticated status, power, assignment, connection-ticket, audit, and error contracts with the actual API owner.
- [ ] Verify user identity server-side; authorize each VM and action from trusted records.
- [ ] Allowlist identifiers/actions; do not execute shell commands from unsanitized client parameters.
- [ ] Add audit logging and safe errors; integrate only after endpoint and contract are supplied/verified.

## Phase 6 — FreeRDP feasibility and secure connection
- [ ] Vendor/pin FreeRDP Android source with license notices and retain the upstream native/JNI build.
- [ ] Build the native Android client for the required ABIs; document licensing/dependencies.
- [ ] Launch a minimal native session on a controlled, reachable test VM.
- [ ] Integrate Kotlin-to-FreeRDP/session launch through a narrow boundary and verify lifecycle/rendering cleanup.
- [ ] Verify external Android reachability, TLS/server identity, authentication, one-time/expiring connection details, and assignment enforcement.
- [ ] Verify transport without Tailscale and without public TCP/3389; Cloudflare HTTPS alone is not assumed to support this.

## Phase 7 — Native desktop interaction
- [ ] Verify display sizing/scaling/orientation and dynamic resolution where supported.
- [ ] Verify touch/direct-click and trackpad/pointer behaviors and coordinate mapping.
- [ ] Verify soft/hardware keyboard, text composition, special keys, and shortcuts.
- [ ] Implement OFF → LATCHED → LOCKED sticky modifier states with safe cancellation/reset.
- [ ] Verify clipboard separately; evaluate audio and other channels separately.
- [ ] Measure session startup, frame rate, latency, CPU, memory, and rendering stability on representative hardware; do not claim target performance without measurements.

## Phase 8 — Reliability and security
- [ ] Handle connecting, connected, disconnecting, disconnected, failed, retry/reconnect, expired ticket, background/foreground, lock, orientation, and cleanup.
- [ ] Review TLS validation, ticket/credential storage, logging, clipboard privacy, and logout/session termination.
- [ ] Test unreachable VM, denied credentials, network loss, backend error, and unauthorized assignment; failures must not broaden access.

## Phase 9 — End-to-end and device tests
- [ ] Verify auth, assignment, status, power operations, real native RDP, input, keyboard, scaling, clipboard, and supported audio on real configured services.
- [ ] Verify network path from an external Android device without public RDP exposure.
- [ ] Attempt cross-student VM access and unauthorized admin actions.
- [ ] Record Android version/device coverage and performance measurements.

## Phase 10 — Release
- [ ] Set production endpoints/config/version and use an owner-controlled signing key; do not publish or claim production readiness before security review.
- [ ] Build actual APK/AAB, install it, and smoke-test on a physical device.
- [ ] Document setup, operational prerequisites, supported capabilities, known limitations, and troubleshooting.
- [ ] Produce final completion report distinguishing implemented, built, isolated-tested, end-to-end-verified, incomplete, and unverified features.

## Current status snapshot

- **Verified complete:** private repository created; official FreeRDP Android docs and v3.32.0 source inspected; environment/toolchain baseline recorded.
- **Implemented but not tested:** none yet.
- **Partially implemented:** none yet.
- **Not started:** MobiDesk Android shell/screens, Supabase/API integration, actual FreeRDP library integration/build, tests.
- **Blocked:** real authentication/VM control/desktop session until project credentials/schema/API contract and a safe reachable test endpoint are available.
- **Requires external verification:** secure RDP transport, server-side authorization, real VM status/actions, physical-device session performance, production signing.
