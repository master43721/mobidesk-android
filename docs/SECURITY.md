# MobiDesk security boundaries

> **Status note (2026-11-03):** This file records the earlier Android/security baseline. The code has since been built, but the native RDP session, broker, WSS transport and physical security tests remain incomplete. The owner-review draft [`MobiDesk_PRD_v1.2.pdf`](MobiDesk_PRD_v1.2.pdf) and current [`TEST_PLAN.md`](../TEST_PLAN.md) define the proposed release requirements; approval is pending.

## Earlier implementation baseline (compiled historically; RDP still incomplete)

The Android app uses the MobiDesk Supabase URL and a publishable client key only. Auth uses email/password and PKCE recovery deep links; session persistence and refresh follow the pinned `supabase-kt` Android defaults. The app provides no public self-registration and does not trust client-supplied roles. The existing Auth trigger provisions only accounts matching the private institution roster.

After sign-in, the app selects only `students(id,name,college)` filtered by the authenticated user ID and `vms(id,name)` under the project's existing RLS policies. It does not request roll numbers, VM hostnames, Guacamole fields, or status. The database policy definitions were inspected, but anonymous/cross-student behavior has not yet been exercised with a real Android client/JWT. No app build or runtime test has passed yet.

The app does not start/stop VMs and keeps the remote-session button disabled. No server-side control API, live status endpoint, connection-ticket issuance, admin-role model, or verified external RDP transport currently exists. The FreeRDP module is included as a dependency, but MobiDesk does not yet launch it.

## Required before enabling remote access

1. Verify an authenticated server-side control API that validates Supabase access tokens and derives identity/role from trusted records.
2. Require a server-side assignment check for every status, power, and session-ticket request; never treat a client VM ID or hidden UI button as authorization.
3. Allowlist operations and VM identifiers; audit privileged operations and return safe errors.
4. Issue narrowly scoped, short-lived connection details only after authorization. Never embed a service-role key, permanent Windows password, or long-lived RDP credential in the APK.
5. Verify the Android network route, TLS certificate validation, endpoint identity, expiry/revocation behavior, and a test VM. Do not expose public TCP/3389 or assume an HTTPS tunnel supports arbitrary native TCP.
6. Launch the non-exported FreeRDP activity only from a verified in-app authorization result. Confirm certificate validation is not silently bypassed.
7. Avoid logging tokens, passwords, clipboard payloads, hostnames, or ticket material. Clear temporary connection details after use and on disconnect/logout.
8. Define a trusted admin-role source and API policy before shipping any administrator dashboard or actions.

## Supabase project observations

The project has RLS enabled on the student, assignment, VM, and private roster tables. The current student/assignment/VM policies scope reads using `auth.uid()` and assignment relationships. The app selects a narrow subset of columns. Review the Security Advisor findings about GraphQL schema discoverability and disabled leaked-password protection. The private roster has RLS enabled with no policies and is intentionally inaccessible to client roles.

Password recovery deep-link allowlisting is configured for `com.mobidesk.android://auth-callback/recovery`. Custom SMTP is disabled, so actual recovery email delivery is not yet confirmed. The project currently allows Auth signup, but this Android app does not expose sign-up; the roster trigger and RLS remain the account/assignment gates. Do not change provider/signup/SMTP policy without an explicit operational decision.

Until the required control-plane, transport, build, and device evidence exists, MobiDesk must not be described as production-ready or as having a working RDP session.
