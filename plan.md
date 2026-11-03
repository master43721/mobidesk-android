# MobiDesk Android — Implementation Plan

## Current goal

> **Historical plan, not current release authority.** The owner supplied `docs/MobiDesk_PRD_v1.2.pdf` on 2026-11-03; it is an owner-review draft pending approval. Some environment/test statements below are stale. Use `PROGRESS.md`, `BASELINE_AUDIT.md`, `TEST_PLAN.md`, and `TODO.md` for current evidence. The current user has authorized local repository work and GitHub Actions.

Deliver a polished Android app for students to sign in, see only their assigned Windows Cloud PC, and later open a native FreeRDP desktop session through an authorized control plane. Keep the control path (identity, assignments, VM actions, tickets) separate from the desktop data path (RDP graphics/input). Do not display sample accounts or imply a VM is live when the service cannot verify it.

## Prior architecture proposal (historical; current approval pending)

- **Android app shell:** Kotlin + Jetpack Compose, chosen over Flutter because the user explicitly allowed any suitable framework and the app must host FreeRDP's native Android Activity/JNI stack. This keeps Android lifecycle and RDP session integration direct.
- **Authentication/data:** Supabase Auth + PostgREST, using only the project's publishable client key in the Android client and existing RLS policies for identity-scoped reads.
- **Provisioning:** Existing Auth trigger creates a student profile only when the email matches the private institution roster. The app offers sign-in and password recovery, not self-registration or client-selected roles.
- **RDP engine:** Pin official FreeRDP v3.32.0 as a git submodule and reuse its `freeRDPCore` Android implementation. No RDP protocol is written from scratch.
- **Future VM control plane:** A trusted server-side API must authorize each status/power/session request and issue a short-lived connection ticket. No service-role key, permanent Windows password, client-supplied VM ID, or public TCP/3389 exposure.
- **Desktop transport:** Must be separately verified for real external Android networks; an HTTPS Cloudflare Tunnel is not assumed to transport raw RDP.

## Verified Supabase baseline

Project `kthfhjwltcpjhjemugwm` contains `students`, `vms`, `student_vm_access`, and private `student_roster` tables, with RLS enabled. Metadata counts are 7 Auth users, 7 roster rows, 7 student rows, 1 VM row, and 1 assignment; no row contents were read. Existing RLS policies scope student rows by `auth.uid()`, assignments by `student_id = auth.uid()`, and VM rows through the signed-in user's assignment. The auth trigger is roster-gated and does not create an admin role.

The app requests only `students(id,name,college)` and assigned `vms(id,name)`. It does not request `roll_no`, hostnames, Guacamole fields, or VM status. Supabase has no live status field and currently has no Edge Functions or verified power/control/session-ticket API. The one Supabase project change so far is adding the recovery deep link `com.mobidesk.android://auth-callback/recovery` to the redirect allowlist; no schema or data rows were changed. See `docs/SUPABASE_DISCOVERY.md` for exact policies, grants, Auth settings, migrations, and advisor findings.

## App screens and behavior

1. Session restoration → sign-in or authenticated workspace.
2. School email/password sign-in, safe errors, password reset request, and PKCE recovery callback/new-password screen.
3. Student dashboard populated from the signed-in student's RLS-scoped profile and VM assignment.
4. Explicit no-profile, no-assignment, loading, retry, and backend-error states.
5. Account/settings and sign-out.
6. Remote session remains visibly disabled until a trusted control endpoint and expiring ticket exist.
7. No administrator route is exposed until the project has a verified admin-role source and server-side enforcement.

## Security decisions

- Do not add an Android sign-up flow; existing provisioning is roster-controlled.
- Do not read or mutate other students' rows; use the current Supabase session and existing RLS.
- Use explicit selected columns only; do not fetch hostnames or Guacamole metadata into Android.
- Use PKCE for mobile recovery and validate the app scheme/host. Only enter password-reset mode after successful session exchange.
- Never log passwords, tokens, connection tickets, or raw backend errors.
- Keep `local.properties` ignored by Git. It may contain the Supabase URL and publishable key, never a service-role key.
- Leave remote connect/start controls disabled until the API, authorization, ticket expiry, certificate validation, and transport are verified.

## Testing and build requirements

Source tests exist for safe authentication error handling and Compose smoke coverage for sign-in/recovery screens. **They have not run.** The sandbox currently has no Android SDK, Gradle, Kotlin compiler, ADB, Android Studio, or `/dev/kvm`. No SDK/NDK license acceptance or installation has been approved. Therefore there is no locally built APK and no emulator result yet. Do not report compile, test, emulator, or device success until it is actually measured.

## Remaining product work

- Build the project with the authorized Android SDK/NDK/CMake toolchain; resolve compile/native issues.
- Run Gradle unit tests, instrumentation tests, install on an Android Emulator, and test the UI.
- Validate live Supabase login and recovery with an authorized account; verify email delivery (custom SMTP is currently disabled).
- Add explicit cross-student/anonymous RLS checks without exposing row data.
- Define the administrator-role model if the separate admin dashboard remains in scope.
- Design/deploy an authenticated VM control API with status, power, connection-ticket, audit, and error contracts.
- Build and launch the FreeRDP Android session only from authorized short-lived tickets; verify TLS/certificate behavior and external RDP transport.
- Test real Windows sessions, touch/pointer, keyboard, modifiers, scaling, clipboard, reconnect, background/foreground, and performance on representative devices.
- Produce a signed release APK only with an owner-controlled signing key and final security review.

## Decision log

- **2026-10-27:** User authorized a clean start and selected the new private repo `master43721/mobidesk-android`; the separate public `mobidesk-updated` repo remains untouched.
- **2026-10-27:** User allowed the agent to choose a non-Flutter implementation. Selected Kotlin/Jetpack Compose for a direct Android host around FreeRDP's Kotlin/Java/JNI Activity.
- **2026-10-27:** Pinned official FreeRDP source to v3.32.0 commit `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1`.
- **2026-10-27:** Confirmed the connected Supabase schema, existing student provisioning trigger, row counts, RLS, and Auth configuration without reading student or VM row values.
- **2026-10-27:** Added the MobiDesk Android password-recovery deep link to Supabase's Redirect URLs allowlist; no provider setting, Site URL, SMTP setting, database schema, or row was changed.
- **2026-10-27:** Integrated Supabase Auth/session/recovery and student/assignment repository code in the app source. Build and runtime behavior remain unverified until a licensed Android toolchain is approved and used.
