# MobiDesk Android

MobiDesk is a native Kotlin/Jetpack Compose student client for accessing one assigned Windows Cloud PC. The app currently implements the sign-in/workspace foundation; **native RDP session integration is not complete and no final APK exists**.

> **Current product target:** [`docs/MobiDesk_PRD_v1.2.pdf`](docs/MobiDesk_PRD_v1.2.pdf). It is an owner-review draft; approval is pending. Do not treat it as formally approved or claim a release gate has passed. See [`docs/PRD_TRACEABILITY.md`](docs/PRD_TRACEABILITY.md), [`TEST_PLAN.md`](TEST_PLAN.md), [`OPEN_ITEMS.md`](OPEN_ITEMS.md), [`PROGRESS.md`](PROGRESS.md), and [`TODO.md`](TODO.md).

## Implemented

- Native Kotlin/Jetpack Compose app shell and student workspace states.
- Supabase email/password sign-in, persisted Auth session, sign-out, password-recovery request, and PKCE recovery callback flow.
- RLS-scoped profile and assigned-VM reads using narrow selected columns; no self-registration and no client-selected role.
- Official FreeRDP Android submodule pinned to v3.32.0 at `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1`, plus the Gradle compatibility overlay and native libraries in the historically verified debug APK for `arm64-v8a` and `x86_64`.

The app does **not** yet launch a remote desktop, request a conforming broker grant, show trusted live VM/RDP readiness, present separate Windows credentials, validate per-VM RDP certificate pins, or bridge FreeRDP to a secured WSS session. Packaged FreeRDP libraries alone are not RDP integration.

## Existing development artifact

The historical build report records a debug APK at `app/build/outputs/apk/debug/app-debug.apk`, built on 2026-10-27:

- Package `com.mobidesk.android`; version `0.2.0` (version code `1`).
- Minimum SDK 29; compile/target SDK 37.
- Size 207,696,333 bytes (about 198 MiB).
- Signed with the Sandbox Android debug key, not an owner-controlled release key.

This is an installable development build, not the final R1 product or a Play release. Its earlier build/test evidence and limitations are in [`docs/ANDROID_BUILD_REPORT.md`](docs/ANDROID_BUILD_REPORT.md). Do not infer a current build result from that historical report.

## Build and test locally

The pinned project uses the Gradle wrapper and the SDK/NDK/CMake versions in `build.gradle`, with NASM required for the x86_64 OpenH264 native build. Android SDK licenses were accepted by the owner for this project. Keep real project settings in ignored `local.properties`; use only the Supabase publishable key in an Android client. Never use a service-role key, Windows password, tunnel token, or signing key in the app or repository.

```bash
git clone --recurse-submodules https://github.com/master43721/mobidesk-android.git
cd mobidesk-android
cp local.properties.example local.properties
# Set only the Supabase URL and publishable key in local.properties.
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

On a booted emulator, run `./gradlew :app:connectedDebugAndroidTest`. Emulator tests do not replace the PRD's physical-device R1 acceptance. Build and test evidence must be recorded in `PROGRESS.md` and linked to the exact source revision.

## Server and transport status

A read-only Tailscale SSH inventory was completed on 2026-11-03. The server is Ubuntu 22.04.5, five Windows guests were running, and no MobiDesk broker service was deployed. A host xrdp TCP/3389 listener was bound to a wildcard address; public reachability and firewall state were not established. Preserve the existing GUI access path and do not change firewall/RDP services without a reviewed change plan. No server, VM, tunnel, or Supabase configuration was modified during that inventory.

A separate local broker prototype exists outside this repository. It is not deployed or release-ready; its gaps are documented in [`docs/PRD_TRACEABILITY.md`](docs/PRD_TRACEABILITY.md). A Cloudflare Quick Tunnel is temporary and is not an acceptable stable WSS endpoint. Public TCP/3389, a client-selected VM/host/port, guessed status, and Supabase-password reuse are prohibited.

## Source of truth

The private repository `master43721/mobidesk-android` is the current development source. The older Google AI Studio workspace is a separate static copy, not a live GitHub mirror, and omitted the FreeRDP submodule. The current user direction authorizes local repository work and GitHub Actions; do not overwrite the canonical repository with that stale AI Studio copy.
