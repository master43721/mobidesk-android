# MobiDesk Android — Build and Delivery Report

> **Historical report (2026-10-27).** Its debug-build evidence remains valid for that source revision, but its statements about server access and stopped testing are no longer current. See [`PROGRESS.md`](PROGRESS.md) and [`TASKS.md`](TASKS.md) for the latest state. It does not claim a working RDP session or final release.

**Report date:** 2026-10-27

**Status:** Debug APK built and artifact-verified; remote-desktop product integration is incomplete.

## Executive summary

A native Kotlin/Jetpack Compose Android prototype has been created in the private `master43721/mobidesk-android` repository. It includes Supabase email/password authentication, session restoration, password recovery, and a student workspace that uses the existing RLS-protected Supabase records. The official FreeRDP Android core is pinned as a Git submodule, and its native libraries are packaged in the APK.

The Android build completed successfully and produced an installable **debug-signed APK**. ZIP integrity and APK signature verification passed. Kotlin compilation and the JVM unit-test task passed. The user requested stopping emulator testing while a software-only AVD boot was pending; consequently there was no emulator app install, instrumentation test, live-account sign-in, or RDP session test. This artifact is a development build, not a completed remote-desktop product or Play Store release.

## APK artifact

- **Local build output (not committed):** `app/build/outputs/apk/debug/app-debug.apk`. The generated APK was intentionally not stored in the repository.
- **Package:** `com.mobidesk.android`
- **Version:** `0.2.0` (version code `1`)
- **Minimum SDK:** 29
- **Compile SDK / target SDK:** 37 / 37
- **Size:** 207,696,333 bytes (about 198.0 MiB)
- **SHA-256:** `51145594144e21abed199f5db43096ae3398cf6817d520c4ff7fbbd0ee101dee`
- **Signing:** Android debug certificate; APK Signature Scheme v2 verified
- **Debug certificate SHA-256:** `18c4dd6cc212a27521d0813ef1ec30781769645a71c0e2369d1d89da8c7948e2`
- **FreeRDP engine ABIs included:** `arm64-v8a` and `x86_64`

The APK is signed using the Android debug key generated in this Sandbox. That is not an owner-controlled release key; do not treat the artifact as a production/Play Store release. The APK is a build output and is not committed to GitHub.

## What is implemented

### Android client

The app includes a native Kotlin/Jetpack Compose shell and MobiDesk visual system; school-account email/password sign-in; persistent Android Auth sessions; sign-out; password-reset request; and PKCE recovery deep-link handling. It presents loading, unprovisioned-account, no-assignment, backend-error, settings, and recovery states. It has no self-registration or fake/sample VM records. Android defaults include HTTPS-only app traffic and non-exported FreeRDP session activities.

### Supabase integration

The app queries the signed-in student's own `students(id,name,college)` row and the RLS-filtered assigned `vms(id,name)` rows, selecting only those columns. Account provisioning remains controlled by the existing private student-roster trigger; the app does not provision accounts itself. Schema discovery counted 7 Auth users, 7 roster rows, 7 student profiles, 1 VM, and 1 assignment without reading user identifiers or personal rows.

The only Supabase project change was adding `com.mobidesk.android://auth-callback/recovery` to the Auth Redirect URLs allowlist. No schema or data-row changes, provider/signup changes, Site URL changes, SMTP changes, or Edge Functions were made. Local builds use the project URL and non-privileged publishable key from ignored `local.properties`; no service-role key is in the Android source or repository.

### FreeRDP

The project pins upstream FreeRDP v3.32.0 at commit `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1` as a submodule. It builds the FreeRDP Java core and packages `libfreerdp-android.so` and `libfreerdp-client3.so` for `arm64-v8a` and `x86_64`. The upstream source remains unmodified; an MobiDesk-owned Gradle overlay handles Android Gradle Plugin compatibility.

The MobiDesk UI does **not** yet invoke the FreeRDP session Activity or pass it an authorized connection configuration. Having the engine in the APK is not the same as having working RDP access.

## Build and verification performed

1. Kotlin app and FreeRDP Java-core compilation completed successfully.
2. JVM unit-test task `:app:testDebugUnitTest` completed successfully.
3. Native debug APK task `:app:assembleDebug` reported `BUILD SUCCESSFUL` after NASM was installed to satisfy the x86_64 OpenH264 native build.
4. APK ZIP integrity check completed with no corrupt entries.
5. `apksigner verify --verbose` verified the APK signature (v2).
6. Package metadata was inspected: `com.mobidesk.android`, version `0.2.0`, min SDK 29, target SDK 37.
7. APK contents were inspected and the FreeRDP Android/client shared libraries were found for `arm64-v8a` and `x86_64`.

## Tests not performed

A Pixel 6 API 36 x86_64 AVD was created. Because this Sandbox has no `/dev/kvm`, a software-only boot attempt was started. The user then instructed us to stop testing. The boot waiter and emulator service were stopped. No emulator boot result is claimed, and no app install, Compose instrumentation test, real Supabase login/recovery, physical-device test, or RDP connection was performed.

## What is not complete

The APK does **not** yet provide end-to-end Cloud PC access:

- The connected Supabase project has no trusted VM control API or Edge Functions.
- The Android client does not launch the FreeRDP `SessionActivity`.
- There is no short-lived RDP connection-ticket endpoint or secure VM credential handoff.
- There are no live VM status/start/stop endpoints, audit endpoint, or server-enforced admin role.
- No secure external RDP transport or certificate model has been selected and verified. Public TCP/3389 exposure is not an acceptable fallback.
- Real student-account sign-in and assignment reads have not been tested. Recovery email delivery is unverified because custom SMTP is disabled.
- The user's Ubuntu server was not inspected or modified. Tailscale peer reachability was verified, but SSH stopped at strict host-key verification before authentication; the user asked to stop further server testing.
- No Android Emulator UI/instrumentation or physical-device interaction tests were completed.
- No owner-controlled production signing key or release artifact exists.

## Source repository and AI Studio

- **Canonical private source repository:** <https://github.com/master43721/mobidesk-android>. The Android source, build-compatibility overlay, project notes, and report are synchronized there; the APK build output is not committed.
- **Earlier AI Studio import:** <https://aistudio.google.com/apps/33ada769-49b3-4db4-88fc-c2aace7546d4?showAssistant=true&showPreview=true>. This is a static older UI-only copy; it omits the FreeRDP submodule and predates the current Supabase Auth implementation. It is not the source of this APK.

## Recommended remaining work to finish the product

1. Obtain authorized, trusted read-only access to the Ubuntu host and inventory the VM hypervisor, Windows test VM, firewall, and existing RDP/gateway services.
2. Agree on a server API that validates Supabase JWTs, reads assignments from trusted data, enforces per-user authorization, audits operations, and issues short-lived connection tickets.
3. Decide the RDP transport and certificate model; keep RDP off the public Internet and verify reachability from Android networks.
4. Wire the ticket flow to the native FreeRDP session and implement session lifecycle, reconnection, display scaling, touch/pointer, keyboard, clipboard, and optional audio behavior.
5. Verify actual sign-in, RLS isolation, password recovery, instrumentation, and session behavior on a controlled emulator/VM and a physical Android device, when testing is authorized.
6. Build a release signed with an owner-controlled key only after server/security and device acceptance tests pass.

The phased server checklist is in [`SERVER_IMPLEMENTATION_PLAN.md`](SERVER_IMPLEMENTATION_PLAN.md); remaining work is tracked in [`TASKS.md`](TASKS.md).
