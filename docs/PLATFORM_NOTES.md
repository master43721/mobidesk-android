# Platform notes and source references

## Google AI Studio workspace

The imported project URL is [MobiDesk in AI Studio](https://aistudio.google.com/apps/33ada769-49b3-4db4-88fc-c2aace7546d4?showAssistant=true&showPreview=true). AI Studio states imported code does not remain synced to GitHub. Its import copied the repository tree but omitted the `third_party/FreeRDP` Git submodule.

The first full-project Gradle attempt failed during configuration because `:freeRDPCore` pointed to the missing submodule directory. AI Studio then made workspace-only fallback adjustments to run a **shell-only build** while keeping remote access explicitly unavailable. The saved workspace report says the fallback APK compilation succeeded and was queued for its streaming Android preview; its Robolectric Compose smoke coverage ran instead of Gradle-connected Android tests. The report says `connectedDebugAndroidTest` could not attach to the streaming emulator, the native FreeRDP engine was unavailable, and the AI Studio Artifacts panel showed no artifact to download.

Therefore this link shows an Android UI prototype, not the current GitHub source, not a working native RDP app, and not proof of end-to-end backend access. The current local/GitHub source has since gained real Supabase Auth and RLS-scoped workspace queries; those changes are **not** in the static AI Studio copy. The canonical source is `master43721/mobidesk-android`.

Official pages:
- [Google AI Studio Build mode](https://ai.google.dev/gemini-api/docs/aistudio-build-mode)
- [Google AI Studio Android app workflow](https://ai.google.dev/gemini-api/docs/aistudio-android)

## FreeRDP Android client

The selected upstream source is FreeRDP v3.32.0, commit `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1`. Its Android implementation combines a Java GUI, FreeRDP native libraries/dependencies, and JNI bindings. The upstream Android project requires Android SDK, NDK/CMake, and Gradle tooling. Preserve the upstream repository license and file-specific notices, including MPL 2.0 notices on some Android client files.

- [FreeRDP source](https://github.com/FreeRDP/FreeRDP)
- [Pinned Android build instructions](https://github.com/FreeRDP/FreeRDP/blob/3.32.0/docs/README.android)
- [Android client overview](https://freerdp-freerdp.mintlify.app/clients/android)
- [Android build guide](https://freerdp-freerdp.mintlify.app/building/android)

## Android SDK and emulator

The local sandbox contains Java but no Android SDK, Gradle, Kotlin compiler, ADB, Android Studio, or `/dev/kvm`. A software emulator might still be possible after SDK setup, but it has not been attempted locally. The user has not approved accepting Google Android SDK/NDK/CMake license terms, so no local toolchain packages were installed and no terms were accepted. Do not run an SDK build or emulator installation that accepts those licenses until that approval is explicit.

- [Android Studio and command-line tools](https://developer.android.com/studio)
- [SDK Manager and package licensing](https://developer.android.com/tools/sdkmanager)
- [Emulator command line](https://developer.android.com/studio/run/emulator-commandline)

## Supabase app integration

Current schema, RLS, Auth settings, client-key handling, recovery allowlist, and migration findings are recorded in [`SUPABASE_DISCOVERY.md`](SUPABASE_DISCOVERY.md). The project has an Android recovery redirect allowlist entry; its Site URL, Auth provider/signup configuration, SMTP settings, schema, and data rows were not modified.
