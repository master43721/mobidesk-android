# Platform notes and source references

> **Historical notes.** The 2026-10-27 build/toolchain facts below remain evidence for that build, but the old claim that the server was not inspected is superseded by the read-only 2026-11-03 inventory in [`BASELINE_AUDIT.md`](BASELINE_AUDIT.md). Current product scope is in [`MobiDesk_PRD_v1.2.pdf`](MobiDesk_PRD_v1.2.pdf), whose cover still carries a pending-approval label; the Project Owner's 3 Nov 2026 instruction declares v1.2 the final decided baseline.

## Google AI Studio workspace

The imported project URL is [MobiDesk in AI Studio](https://aistudio.google.com/apps/33ada769-49b3-4db4-88fc-c2aace7546d4?showAssistant=true&showPreview=true). AI Studio states imported code does not remain synced to GitHub. Its import copied the repository tree but omitted the `third_party/FreeRDP` Git submodule.

The first full-project Gradle attempt failed during configuration because `:freeRDPCore` pointed to the missing submodule directory. AI Studio later made workspace-only fallback adjustments to run a **shell-only build**. Its saved report says the fallback APK compilation succeeded and was queued for streaming preview; Robolectric Compose coverage ran, while `connectedDebugAndroidTest` could not attach to the streaming emulator. The native FreeRDP engine was unavailable in that fallback, and no downloadable artifact was found in the AI Studio Artifacts panel.

Therefore, the AI Studio link shows an older UI prototype, not the current GitHub source, not a working native RDP app, and not proof of end-to-end backend access. The current source has Supabase Auth and RLS-scoped workspace queries, which are not in the static AI Studio copy. The canonical source is `master43721/mobidesk-android`.

Official pages:
- [Google AI Studio Build mode](https://ai.google.dev/gemini-api/docs/aistudio-build-mode)
- [Google AI Studio Android app workflow](https://ai.google.dev/gemini-api/docs/aistudio-android)

## FreeRDP Android client

The selected upstream source is FreeRDP v3.32.0, commit `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1`. Its Android implementation combines a Java GUI, FreeRDP native libraries/dependencies, and JNI bindings. MobiDesk pins it as a submodule and uses a MobiDesk-owned Gradle overlay so the submodule itself remains unchanged.

The successfully assembled MobiDesk debug APK includes `libfreerdp-android.so` and `libfreerdp-client3.so` for `arm64-v8a` and `x86_64`. These libraries are packaged but are not yet called by the MobiDesk UI; there is no completed connection-ticket-to-session flow. Preserve upstream licensing and file-specific notices, including MPL 2.0 notices on some Android client files.

- [FreeRDP source](https://github.com/FreeRDP/FreeRDP)
- [Pinned Android build instructions](https://github.com/FreeRDP/FreeRDP/blob/3.32.0/docs/README.android)
- [Android client overview](https://freerdp-freerdp.mintlify.app/clients/android)
- [Android build guide](https://freerdp-freerdp.mintlify.app/building/android)

## Android SDK, APK, and emulator

The user explicitly approved accepting Google's Android SDK/NDK/CMake license terms. The Sandbox toolchain was installed, including the API 37 compile platform, pinned Build Tools/NDK/CMake, JDK 21, platform-tools, emulator, and an API 36 Google APIs x86_64 system image. NASM was added after FreeRDP's x86_64 OpenH264 build reported the missing assembler.

The Kotlin app and FreeRDP Java core compiled; JVM unit tests passed; `:app:assembleDebug` completed successfully. The resulting debug APK at `app/build/outputs/apk/debug/app-debug.apk` is 207,696,333 bytes; ZIP integrity and APK Signature Scheme v2 verification passed. It uses the standard Android debug certificate and is not a Play-ready release.

A Pixel 6 API 36 x86_64 AVD was created for a software-only run because `/dev/kvm` is unavailable. The user asked to stop tests while its boot result was pending. The emulator and boot waiter were stopped, and no app install, emulator screen check, Android instrumentation test, or emulator boot result is claimed. No test was resumed after the user's stop request.

- [Official Android Studio download/checksum page](https://developer.android.com/studio)
- [Command-line tools release notes](https://developer.android.com/tools/releases/cmdline-tools)
- [Android Emulator command-line guide](https://developer.android.com/studio/run/emulator-commandline)
- [Android Emulator acceleration guide](https://developer.android.com/studio/run/emulator-acceleration)

## Supabase app integration

Current schema, RLS, Auth settings, client-key handling, recovery allowlist, and migration findings are recorded in [`SUPABASE_DISCOVERY.md`](SUPABASE_DISCOVERY.md). The project has the Android recovery redirect allowlist entry; its Site URL, Auth provider/signup configuration, SMTP settings, schema, and data rows were not changed. The default Auth client URL and publishable key used to compile local debug output were kept in ignored `local.properties`, not committed. Real account login and recovery-email delivery were not exercised.

## Server access

The Sandbox joined the user's Tailscale network in userspace mode and could ping the advertised Ubuntu peer. The SSH attempt stopped before authentication because strict host-key verification did not find a known ED25519 key. The user requested stopping further testing. The Ubuntu server was not inspected or modified; no SSH key, password, or command was sent to it.
