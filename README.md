# MobiDesk Android

MobiDesk is a native Kotlin/Jetpack Compose Android client prototype for students to sign in and see their assigned Windows Cloud PC. It uses the verified MobiDesk Supabase project and its existing roster provisioning/RLS model. The official FreeRDP Android core is pinned as a submodule and its native libraries are built into the debug APK, but the app does **not** yet launch a remote desktop session.

## Current delivered build

A debug APK was built successfully on 2026-10-27 at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

- Package: `com.mobidesk.android`
- Version: `0.2.0` (version code `1`)
- Minimum Android SDK: 29
- Compile/target SDK: 37
- APK size: 207,696,333 bytes (about 198.0 MiB)
- Signature: Android debug certificate; verified with `apksigner` (APK Signature Scheme v2)
- FreeRDP native engine libraries included for `arm64-v8a` and `x86_64`

This is an installable development/debug artifact, **not** a production or Play Store release. It is not signed with an owner-controlled release key. The APK is a build output and is not committed to Git.

## Implemented app behavior

The source includes Supabase email/password sign-in, persisted Android Auth sessions, sign-out, password-reset request, and PKCE recovery deep-link handling. When signed in, it selects only `students(id,name,college)` and the RLS-filtered assigned `vms(id,name)`. The UI contains loading, not-provisioned, no-assignment, backend-error, account/settings, and recovery states. Self-registration is intentionally absent; student provisioning remains controlled by the existing roster-trigger function.

The Kotlin application and FreeRDP Java core compiled successfully. The JVM unit-test task passed. APK ZIP integrity and debug signature verification passed. The user asked to stop testing while a software-only Android Emulator boot attempt was in progress, so no APK install, Android instrumentation run, emulator UI test, live Supabase sign-in, or real RDP session test is claimed.

See [`TODO.md`](TODO.md) for phase-by-phase status and [`docs/ANDROID_BUILD_REPORT.md`](docs/ANDROID_BUILD_REPORT.md) for the artifact metadata, evidence, limitations, and completion plan.

## Supabase configuration

The connected project is `kthfhjwltcpjhjemugwm` (`https://kthfhjwltcpjhjemugwm.supabase.co`). The existing student provisioning trigger and RLS policies were inspected. The only project-side change was adding `com.mobidesk.android://auth-callback/recovery` to the Auth Redirect URLs allowlist. Site URL, provider/signup settings, email templates, SMTP settings, schema, and data rows were left unchanged. Custom SMTP is disabled, so actual student recovery email delivery has not been verified.

For local builds, copy `local.properties.example` to `local.properties` and set `SUPABASE_URL` and the **publishable** `SUPABASE_PUBLISHABLE_KEY`. The file is ignored by Git. Never put a Supabase service-role key, SMTP credential, Windows password, or signing key in the Android app, Git, logs, or chat.

See [`docs/SUPABASE_DISCOVERY.md`](docs/SUPABASE_DISCOVERY.md) for the verified columns, RLS policies, Auth settings, migrations, and advisor findings. No database DDL or data-row changes were made.

## Build and test

Clone with the native dependency included:

```bash
git clone --recurse-submodules https://github.com/master43721/mobidesk-android.git
cd mobidesk-android
cp local.properties.example local.properties
# Edit local.properties with this project's URL and publishable key.
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

The debug APK output is `app/build/outputs/apk/debug/app-debug.apk`. The pinned FreeRDP module requires the Android SDK/Build Tools, NDK, and CMake versions declared in Gradle, plus NASM for its x86_64 OpenH264 build. The build does not auto-accept Android SDK licenses. On a provisioned emulator, Android instrumentation tests can be run with:

```bash
./gradlew connectedDebugAndroidTest
```

## FreeRDP and remote access — incomplete

`third_party/FreeRDP` is pinned to official FreeRDP v3.32.0 and retains upstream licensing. The app declares the upstream `freeRDPCore` module and the debug APK contains its native shared libraries, but MobiDesk does not yet invoke `SessionActivity` or pass it an authorized connection configuration.

A production connection still requires a trusted server-side API to authenticate the Supabase user, verify VM assignment, authorize/audit power actions, and issue a short-lived connection ticket. VM status/power endpoints, ticket issuance, secure transport, certificate behavior, session lifecycle/input/accessibility, external Android network reachability, and physical-device RDP tests are not implemented or verified. Public TCP/3389 exposure is not acceptable; Cloudflare HTTPS is not assumed to carry raw RDP.

The user's Ubuntu server was not inspected or modified. Tailscale peer connectivity was verified, but SSH host-key verification failed before authentication; the user asked to stop further server testing. See [`docs/SERVER_IMPLEMENTATION_PLAN.md`](docs/SERVER_IMPLEMENTATION_PLAN.md) for the staged server plan.

## Google AI Studio

The AI Studio workspace is a separate static import, not a live GitHub workspace. Its imported copy omitted the FreeRDP git submodule and later produced a UI-only fallback shell; that copy predates the Supabase Auth implementation and is not the APK documented here. The canonical project is the private GitHub repository: <https://github.com/master43721/mobidesk-android>.
