# FP-01 Supabase Configuration and Operational Contract

**Version:** 0.1 — REVIEW DRAFT  
**Date:** 3 November 2026  
**Scope:** Existing MobiDesk Supabase project Auth settings used by Android FP-01.  
**Status:** Configuration inventory/checklist only. No Supabase setting has been changed by this draft.

## 1. Purpose

List the project-side settings and integrations FP-01 depends on. An Android UI implementation cannot by itself guarantee that account registration is restricted, recovery email is deliverable or password policy is strong. These are Supabase project-level responsibilities.

## 2. Verified Android integration

From the canonical repository snapshot inspected for this draft:

- Gradle uses Supabase BOM 3.6.0 with auth-kt and postgrest-kt.
- Ktor Android client is 3.4.3.
- MobiDeskApplication reads SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY from local.properties and exposes them as BuildConfig constants.
- Only a client-safe publishable/anon key is intended for Android.
- Auth is configured with FlowType.PKCE.
- Current callback scheme is com.mobidesk.android.
- Current callback host is auth-callback.
- Current recovery URI is com.mobidesk.android://auth-callback/recovery.
- MainActivity calls handleDeeplinks for an incoming VIEW intent after scheme/host checks.

This verifies app code configuration. It does not verify the current live Auth dashboard configuration.

## 3. Historical project evidence — not current confirmation

docs/SUPABASE_DISCOVERY.md records a read-only snapshot dated 27 October 2026:

- Email/password Auth was enabled.
- Email confirmation was required.
- New-user signup was reported enabled at that time.
- The roster-trigger provisioning logic accepted identities matching the institution roster; the discovery report described a trigger that only provisions a student profile when the new Auth email matches a roster record.
- A recovery redirect URI com.mobidesk.android://auth-callback/recovery was reported as added to the allowed Redirect URLs.
- Custom SMTP was reported disabled at that time.
- Actual recovery-email delivery was not verified.

These are **REPORTED historical facts**, not statements about the live project on 3 November. Recheck every item before FP-01 acceptance. Do not assume historical row counts or Auth settings are current.

## 4. Required Auth configuration review

| Setting | Desired condition for FP-01 | Current evidence | Action |
|---|---|---|---|
| Email provider | Enabled | Historical discovery says enabled; live value UNKNOWN | Recheck read-only. |
| Email confirmation | Institution policy approved; normally require confirmation for provisioned email | Historical discovery says required | Recheck and owner approve. |
| Public signups | Must align with roster/invitation model; recommendation is disable public signup if users are provisioned administratively | Historical discovery reported enabled | BLOCKING owner decision and live verification. |
| Roster provisioning | Auth identity maps to an approved student roster; no app-supplied role/identity metadata grants access | Historical discovery described a roster-gated trigger | Verify current trigger/function/migrations with owner. |
| Recovery redirect | Exact callback URI allowed, no broad wildcard | Historical discovery reported exact URI added | Recheck exact allow-list. |
| Recovery email template | Uses correct redirect variable/route and does not direct to a stale site | Not verified | Review in staging. |
| Custom SMTP | Production sender with tested deliverability | Historical discovery reported custom SMTP disabled | Configure/verify before real pilot recovery. |
| Password policy | Server policy approved and matches UX guidance | Current setting UNKNOWN | Inspect and record; client minimum is not authoritative. |
| Leaked password protection | Enable where available/appropriate to project plan | Historical discovery reported disabled | Owner review. |
| Auth rate limits / abuse protection | Reasonable rate limits and abuse controls for email/password and recovery | Current values UNKNOWN | Inspect and test; do not invent numeric settings. |
| OTP/recovery expiry | Owner-approved bounded expiry | Current value UNKNOWN | Inspect and record. |
| Session lifetime/refresh | Matches college access policy | Current values UNKNOWN | Inspect and record. |
| Auth logs/retention | Appropriate access and retention | Current values UNKNOWN | Owner review. |

## 5. Account provisioning and signup policy

The Android app has no sign-up screen. This is not equivalent to disabling provider-level signup.

Two candidate policies:

**Option A — Disable public signup (recommended if all accounts are institution-provisioned).**
- Only existing/invited/provisioned users can authenticate.
- Account creation is performed through an owner-controlled provisioning process.
- Reduces public account creation and roster-trigger attack surface.
- Requires owner/admin provisioning to be documented.

**Option B — Keep provider signup enabled with a hardened roster gate.**
- Supabase trigger/server-side policy must reject identities not matching an approved roster.
- The trigger must be transaction-safe, race-safe and protected against metadata-based role escalation.
- Auth users created without a student profile must not gain application/VM authorization.
- Must be tested with non-roster, duplicate and already-provisioned emails.
- Owner must explicitly accept operational and abuse risk.

FP-01 does not call signUp from Android under either option. Select the project-level policy separately.

## 6. Redirect and email flow

Required redirect URI currently present in app build configuration:
- com.mobidesk.android://auth-callback/recovery

Checklist:
1. Recheck the exact URI in Supabase Redirect URLs.
2. Ensure the request's redirectUrl matches the allowed URI exactly.
3. Review email templates: when using a custom redirect URL, confirm the template uses the correct Supabase redirect variable and preserves the app callback path.
4. Open links from the actual mail client on supported Android device(s).
5. Test a valid link, expired link, replay, wrong callback and app cold-start.
6. Confirm the SDK processes PKCE/OTP material; app code must not treat callback query values as identity proof.
7. Decide whether custom scheme is accepted for pilot or a verified HTTPS Android App Link is required for production.
8. Verify a production email sender. A successful API response does not prove email arrival.

## 7. Key handling and environments

- SUPABASE_URL and publishable/anon key may be included as public client configuration.
- Never include service_role, secret API keys, DB passwords, SMTP credentials or signing keys in Android.
- Keep local.properties ignored and do not commit actual project credentials.
- Use separate development/staging/production project configuration where practical.
- Test builds must clearly identify their environment without including credentials.
- Release APK/AAB must be scanned for test endpoints and secret patterns.

## 8. Supabase owner verification checklist

- [ ] Email provider state verified.
- [ ] Email confirmation policy verified.
- [ ] Signup policy selected and verified.
- [ ] Roster trigger/provisioning path inspected and tested.
- [ ] Recovery redirect URI exact-match verified.
- [ ] Recovery email template reviewed.
- [ ] SMTP sender configured and test email received.
- [ ] Password minimum/strength and leaked-password policy approved.
- [ ] Auth rate limits/abuse settings reviewed.
- [ ] OTP/recovery expiry reviewed.
- [ ] Session/refresh policy reviewed.
- [ ] Auth logs/retention and operator access reviewed.
- [ ] No server-side secret is present in the Android project.

## 9. Official references reviewed for this draft

- Supabase Kotlin initialization, PKCE and Android deep links: https://supabase.com/docs/reference/kotlin/initializing
- Supabase Kotlin email/password sign-in: https://supabase.com/docs/reference/kotlin/auth-signinwithpassword
- Supabase Kotlin password reset: https://supabase.com/docs/reference/kotlin/auth-resetpasswordforemail
- Supabase redirect URLs and mobile deep links: https://supabase.com/docs/guides/auth/redirect-urls
- Supabase general Auth configuration (including allow new users to sign up): https://supabase.com/docs/guides/auth/general-configuration
- Supabase password strength and leaked-password protection: https://supabase.com/docs/guides/auth/password-security
- Supabase production security checklist: https://supabase.com/docs/guides/security/production-checklist

These links describe platform behavior. They are not evidence of the live MobiDesk project settings.
