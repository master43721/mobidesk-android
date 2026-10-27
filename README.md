# MobiDesk Android

A native Kotlin/Jetpack Compose Android client for students to sign in and see only their assigned Windows Cloud PC. The app uses the verified MobiDesk Supabase project and its existing roster provisioning/RLS model. The official FreeRDP Android core is pinned as a git submodule; remote desktop access is not enabled until a trusted control API and secure transport are implemented.

## Current implementation status

The source now includes Supabase email/password sign-in, automatic session restore, sign-out, password-reset request and PKCE recovery deep-link handling. The signed-in app queries only `students(id,name,college)` and RLS-filtered `vms(id,name)`. It shows honest loading, account-not-provisioned, no-assignment, and backend-error states. The app has no self-registration, admin dashboard, VM status/power actions, connection-ticket endpoint, or live RDP session flow.

**No APK has been built and no tests or Android Emulator run has succeeded yet.** The current sandbox has no Android SDK, Gradle, Kotlin compiler, ADB, Android Studio, or `/dev/kvm`. Installing Google's Android toolchain requires explicit authorization to accept the Android SDK/NDK/CMake terms; no such acceptance or installation has been performed. See [`TODO.md`](TODO.md) for the live phase checklist.

## Supabase configuration

The connected project is `kthfhjwltcpjhjemugwm` (`https://kthfhjwltcpjhjemugwm.supabase.co`). The existing student provisioning trigger and RLS policies are already present. The only project-side change made was adding `com.mobidesk.android://auth-callback/recovery` to the Auth Redirect URLs allowlist. The existing Site URL, provider/signup settings, email templates, and SMTP settings were left unchanged. Custom SMTP is disabled, so actual student recovery email delivery still needs verification.

For local builds, copy `local.properties.example` to `local.properties` and set the project's `SUPABASE_URL` and **publishable** `SUPABASE_PUBLISHABLE_KEY`. The file is ignored by Git. Never place a Supabase service-role key, SMTP credential, Windows password, or signing key in the app, Git, logs, or chat.

See [`docs/SUPABASE_DISCOVERY.md`](docs/SUPABASE_DISCOVERY.md) for verified table columns, RLS policies, Auth settings, migration history, and advisor findings. No database schema or data-row changes were made.

## Build and test (once the Android toolchain is available)

Clone with the native dependency included:

```bash
git clone --recurse-submodules https://github.com/master43721/mobidesk-android.git
cd mobidesk-android
cp local.properties.example local.properties
# Edit local.properties with the project's URL and publishable key.
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

The debug APK is expected at `app/build/outputs/apk/debug/app-debug.apk` after a successful build. On a provisioned Android Emulator, run:

```bash
./gradlew connectedDebugAndroidTest
```

The pinned FreeRDP native module also requires the official Android SDK platform/build tools, NDK and CMake versions declared in the Gradle configuration. Package licenses must be accepted through the owner's authorized Android development environment; this repository does not auto-accept them. APK output and tests remain unverified until those commands actually pass.

## FreeRDP and remote access

`third_party/FreeRDP` is pinned to official FreeRDP v3.32.0 and retains upstream licensing. At this point the MobiDesk app only declares the upstream `freeRDPCore` module; it does not yet launch a remote session. A production connection still requires an authenticated server-side API to verify assignment, control VM power, audit requests, and issue short-lived connection details. The native RDP transport, TLS/certificate behavior, and access from external Android networks must be verified separately. Public TCP/3389 exposure is not acceptable, and Cloudflare HTTPS is not assumed to carry raw RDP.

## Google AI Studio

The AI Studio app is a separate, static import, not a live GitHub workspace. The imported copy omits the FreeRDP git submodule, so its first full-project build failed. AI Studio later built a fallback **UI-only shell APK** and queued it for the streaming preview; its saved report says Robolectric UI coverage ran, while `connectedDebugAndroidTest` could not attach to the streaming emulator. The fallback has no native FreeRDP engine, no working RDP session, and no downloadable artifact in the AI Studio Artifacts panel. It also predates the Supabase Auth changes in this repository. Re-importing from GitHub will still omit the submodule unless handled separately; the canonical source is this repository.

- Private source repository: https://github.com/master43721/mobidesk-android
- Earlier AI Studio import: https://aistudio.google.com/apps/33ada769-49b3-4db4-88fc-c2aace7546d4?showAssistant=true&showPreview=true
