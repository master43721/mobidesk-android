# MobiDesk Android

Native Android client for authorized student access to assigned Windows Cloud PCs. This repository is a clean implementation; the existing public `mobidesk-updated` repository is intentionally left untouched.

## Current status

The current source and architecture decisions are tracked in [`plan.md`](plan.md) and [`TODO.md`](TODO.md). No MobiDesk Supabase project configuration, trusted VM control API, VM assignment records, test RDP endpoint, or approved external RDP transport has been supplied. The application must not represent historical VM mappings or placeholder data as live state.

The planned desktop engine is the official FreeRDP Android implementation, pinned at v3.32.0, built through its Android JNI/CMake/Gradle project. Its source and licenses will be incorporated as a pinned dependency before implementation is considered complete.

## Security boundaries

- Students must see/access only the assignment verified for their authenticated identity.
- Administrative VM actions must be independently authorized server-side and audited.
- No privileged backend key, permanent Windows password, or private signing key belongs in the APK or Git.
- The app must not depend on public TCP/3389. A server-issued, short-lived connection ticket and verified native RDP transport are required before real desktop access is enabled.
- `Unknown`, `Not configured`, or error status must not be relabeled as live data.

## Build and test

Android SDK/Gradle setup is not yet present in the current sandbox. Build commands will be documented and verified when the project toolchain and Android emulator are provisioned. Do not claim an APK, emulator test, or end-to-end remote desktop session until the corresponding command/runtime succeeds.
