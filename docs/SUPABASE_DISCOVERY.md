# Supabase discovery — MobiDesk

> **Historical snapshot (2026-10-27).** Counts and assignment statements in this file predate a later 2026-10-30 assignment update. The last dated audit recorded five student assignments; PRD v1.2 says only four RDP endpoints are approved and VM4 is unverified. Re-query the live project and reconcile the approved server allowlist before any R1 test. See [`BASELINE_AUDIT.md`](BASELINE_AUDIT.md) and [`OPEN_ITEMS.md`](OPEN_ITEMS.md). No assignment should be inferred from this historical count.

## Project and historical data inventory (2026-10-27)

The connected Supabase project is **MobiDesk**, project ID `kthfhjwltcpjhjemugwm`, at `https://kthfhjwltcpjhjemugwm.supabase.co`. The current metadata shows 7 Auth users, 7 roster rows, 7 student-profile rows, 1 VM row, and 1 student-to-VM assignment. I counted rows only; I did not select names, emails, roll numbers, hostnames, VM names, connection IDs, or other row contents.

The public schema contains `students`, `vms`, and `student_vm_access`. The private schema contains `student_roster`. RLS is enabled on all four tables. `students` has `id`, `name`, `roll_no`, `college`, `guacamole_base_url`, and `created_at`; its primary key is also a foreign key to `auth.users.id`. `vms` has `id`, `name`, `guac_connection_id`, `hostname`, and `created_at`. `student_vm_access` links a `student_id` and `vm_id`, with a `granted_at` timestamp. The private roster is keyed by `roll_no`; its generated email is derived from that value and it tracks `auth_user_id` and `provisioned_at`.

## Identity provisioning and RLS

The existing `private.handle_new_mobidesk_student()` Auth trigger is `SECURITY DEFINER` with an empty search path. It provisions a public student only if the new Auth email matches a roster email and the roster's `auth_user_id` is empty or already matches the new user. It sets the display name from user metadata or the roll number, defaults the college to `RVRJC`, inserts `public.students`, and links the roster row. It does not create an administrator role. This trigger remains the only verified student-provisioning path; the Android app does not expose public sign-up or trust user metadata for authorization.

The current public SELECT policies are:

- `students`: authenticated users can read only the row whose `id = auth.uid()`.
- `student_vm_access`: authenticated users can read only assignments whose `student_id = auth.uid()`.
- `vms`: authenticated users can read only VM rows for which an assignment exists with `student_id = auth.uid()`.

The `private.student_roster` table has RLS enabled and no policies, so it is not directly readable through client roles. Authenticated clients have SELECT privileges on the public student and assignment data. VM access is granted at the column level for `id`, `name`, and `guac_connection_id`; the `hostname` column is not granted. The app intentionally requests only `students(id,name,college)` and `vms(id,name)`, and never asks for roll numbers, Guacamole fields, or hostnames.

Metadata inspection confirmed the app's selected `students.id/name/college` and `vms.id/name` fields are `NOT NULL`; no application row values were read for this check.

## Existing migrations and server-side capabilities

The applied migration history is:

- `20261018183621 init_mobidesk_student_vm_schema`
- `20261018183959 auto_create_student_profiles_from_auth`
- `20261019025014 restrict_student_profiles_to_roster`
- `20261019025920 align_student_roster_to_created_auth_accounts`
- `20261020113212 grant_authenticated_student_vm_reads`

There are currently no Supabase Edge Functions. The database contains no VM status field or verified power-control/session-ticket API. No schema migration or database-row mutation was applied; the only project-side change was adding the Android recovery redirect URL.

## Auth and email settings (read-only)

Email/password Auth is enabled, new-user signup is currently allowed, and email confirmation is required. Phone Auth, anonymous sign-in, and manual identity linking are disabled. The app will provide sign-in and password recovery only; it will not offer self-registration because accounts must be roster-provisioned.

The Auth URL Configuration page initially listed one allowed redirect, the existing V0 callback `https://v0.app/chat/api/supabase/redirect/vcVoBEfr2U2`, and a localhost development Site URL. After confirming the Android callback scheme and wiring PKCE recovery handling, I added exactly `com.mobidesk.android://auth-callback/recovery` to the Redirect URLs allowlist and verified that Supabase reported “Successfully added 1 URL.” The existing callback and Site URL were not changed. The SMTP Settings page shows custom SMTP disabled. No email credentials or templates were read or changed. Actual recovery-email delivery remains unverified; production delivery may require the project owner to configure a supported SMTP sender.

## Security/performance advisors

The current Security Advisor reports: (1) `private.student_roster` has RLS enabled without policies (informational and fail-closed); (2) authenticated roles can discover the three RLS-protected public tables in the GraphQL schema; and (3) leaked-password protection is disabled. RLS still limits row reads, but the owner should review GraphQL exposure and consider enabling leaked-password protection. The advisor's remediation pages are [RLS without policy](https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy), [GraphQL schema exposure](https://supabase.com/docs/guides/database/database-linter?lint=0027_pg_graphql_authenticated_table_exposed), and [leaked-password protection](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection).

The Performance Advisor reports one unused index, `student_vm_access_vm_id_idx`. It is informational; I did not remove or change it.

## Android client implementation references

The stable `supabase-kt` 3.6.0 tag pins Kotlin 2.3.21 and Ktor 3.4.3, matching the current MobiDesk Gradle Kotlin version. Newer 3.7/3.8 tags require Kotlin 2.4.0 and Ktor 3.5.1, so the implementation pins 3.6.0 to avoid an unverified toolchain upgrade. The app uses the modern Supabase publishable key only; it does not use a service-role key.

The Kotlin reference supports `supabase.auth.signInWith(Email) { email = ...; password = ... }`, a `sessionStatus` flow with `Initializing`, `Authenticated`, `RefreshFailure`, and `NotAuthenticated` states, and Android deep-link handling via `supabase.handleDeeplinks(intent)`. The 3.6.0 Auth defaults automatically load and save sessions and handle Android lifecycle refresh. Password recovery is requested with `resetPasswordForEmail(email, redirectUrl)` and completed after callback with `updateUser { password = ... }`.

The client queries use explicit column lists. The VM query relies on the existing RLS policy to expose only the signed-in student's assigned VM rows. The database has no authoritative live status field, and the client will not infer online/offline status. Supabase's Kotlin guidance requires Data API table grants and RLS policies to be configured together.

References:

- [supabase-kt 3.6.0 version catalog](https://raw.githubusercontent.com/supabase/supabase-kt/3.6.0/gradle/libs.versions.toml)
- [Supabase Kotlin initialization and session handling](https://supabase.com/docs/reference/kotlin/initializing)
- [Supabase Kotlin email/password sign-in](https://supabase.com/docs/reference/kotlin/auth-signinwithpassword)
- [Supabase Kotlin password reset](https://supabase.com/docs/reference/kotlin/auth-resetpasswordforemail)
- [Supabase Auth redirect URLs](https://supabase.com/docs/guides/auth/redirect-urls)
- [Supabase Kotlin PostgREST filters](https://supabase.com/docs/reference/kotlin/using-filters)
