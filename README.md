# MobiDesk Android

MobiDesk is a native Kotlin/Jetpack Compose Android application for one college. The product model has a College Administrator workspace for provisioning students and managing approved server/VM/student connections, and a Student workspace for accessing only the student's assigned Windows Cloud PC. The current app implementation still provides the student sign-in/workspace foundation; the administrator workspace and provisioning/control-plane capabilities are proposed and not yet implemented; **native RDP session integration is not complete and no final APK exists**.

> **Current product target:** [`docs/MobiDesk_PRD_v1.2.pdf`](docs/MobiDesk_PRD_v1.2.pdf). It is an owner-review draft; approval is pending. Do not treat it as formally approved or claim a release gate has passed. See [`docs/PRD_TRACEABILITY.md`](docs/PRD_TRACEABILITY.md), [`docs/FREERDP_ANDROID_API_AUDIT.md`](docs/FREERDP_ANDROID_API_AUDIT.md), [`TEST_PLAN.md`](TEST_PLAN.md), [`OPEN_ITEMS.md`](OPEN_ITEMS.md), [`PROGRESS.md`](PROGRESS.md), and [`TODO.md`](TODO.md).

## Implemented

- Native Kotlin/Jetpack Compose app shell and student workspace states.
- Supabase email/password sign-in, persisted Auth session, sign-out, password-recovery request, and PKCE recovery callback flow.
- RLS-scoped profile and assigned-VM reads using narrow selected columns; no student self-registration. The College Administrator account is bootstrapped out-of-band; server-side role resolution is required before routing to the admin workspace.
- Official FreeRDP Android submodule pinned to v3.32.0 at `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1`, plus the Gradle compatibility overlay and native libraries for `arm64-v8a` and `x86_64`.
- App-manifest containment for unused exported FreeRDP UI entry points; the merged manifest was rebuilt and verified to contain zero exported FreeRDP components.

The app does **not** yet include the College Administrator workspace, roster-first student invitation workflow, admin APIs, or approved server/VM control plane. It also does **not** yet launch a remote desktop, request a conforming broker grant, show trusted live VM/RDP readiness, present separate Windows credentials, validate per-VM RDP certificate pins, or bridge FreeRDP to a secured WSS session. The pinned-source audit found C transport-I/O hooks but no raw socket/stream API in the checked-in Android Java wrapper. Packaged FreeRDP libraries alone are not RDP integration.

## Build and test status

The post-hardening local build passed `:app:testDebugUnitTest` (2 tests, 0 failures), compiled the pinned native FreeRDP code, and assembled the debug APK. The 217,307,804-byte APK passed merged-manifest review, ZIP-integrity and signature verification; SHA-256: `acd5a7a9e26d174652238439ddc342c5232064ca9d3be87e933fd84556062513`. It is debug-signed and **not** a final RDP product.

GitHub Actions workflow: [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml). The first run failed before Gradle because `android-actions/setup-android@v3` requested the retired SDK package `tools`. The workflow was corrected in the local worktree to use the SDK Manager already installed on the hosted runner; a green rerun and emulator instrumentation remain pending.

## Build locally

The pinned project uses the Gradle wrapper and SDK/NDK/CMake versions in `build.gradle`, with NASM required for the x86_64 OpenH264 native build. Android SDK licenses were accepted by the owner for this project. Keep real project settings in ignored `local.properties`; use only the Supabase publishable key in an Android client. Supabase Auth Admin invitations and all privileged operations must run on a trusted server; never use a service-role/secret key, Windows password, invitation token, host/SSH credential, tunnel token, or signing key in the app or repository. Students cannot self-register; a College Administrator creates their Auth account and sends an invitation to activate it.

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

The canonical repository `master43721/mobidesk-android` is the current development source. The older Google AI Studio workspace is a separate static copy, not a live GitHub mirror, and omitted the FreeRDP submodule. The current user direction authorizes local repository work and GitHub Actions; do not overwrite the canonical repository with that stale AI Studio copy. See docs/ADMIN_ACCOUNT_MODEL.md for the two-role account model and admin control boundaries.
