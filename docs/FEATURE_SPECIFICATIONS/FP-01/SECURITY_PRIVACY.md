# FP-01 Security and Privacy Requirements

**Version:** 0.2 — OWNER CRITIQUE REVISION  
**Date:** 4 November 2026  
**Scope:** Student authentication and account recovery.

## 1. Security objectives

1. Supabase Auth remains the credential/session authority.
2. Android includes no service-role key or privileged backend secret.
3. Passwords are transient and never stored/logged by MobiDesk.
4. A session created through password recovery is RECOVERY_ONLY until the new password is successfully set.
5. Recovery cancellation, process death and callback re-delivery cannot create an unintended normal authenticated workspace.
6. Sign-out failures have an explicit safe device-local exit.
7. Recovery mail is operational for synthetic non-team students before it is accepted for a pilot.
8. Auth success never bypasses FP-02/FP-03 student profile and VM authorization.

## 2. Verified defects and risk

| ID | Finding | Impact | Required mitigation |
|---|---|---|---|
| SEC-01 | SDK Authenticated state is imported before recovery success callback runs. | ViewModel may load workspace while recovery is pending. | Durable recovery marker and RECOVERY_ONLY AuthLevel gate. |
| SEC-02 | Recovery Back/cancel only clears UI flag. | Recovery session remains authenticated and can enter dashboard. | Cancel signs out the recovery session; local clear fallback. |
| SEC-03 | Recovery state only lives in ViewModel memory. | Process death restores SDK session as ordinary Authenticated. | Persist recovery stage outside ViewModel; fail closed on interrupted callback. |
| SEC-04 | Activity callback handling has no durable de-duplication. | Recreation may re-submit consumed PKCE code. | Store pending/consumed one-way callback fingerprint and stage; never store raw code. |
| SEC-05 | Sign-out network failure leaves authenticated state active. | User has no exit path and current local session stays usable. | Offer explicit device-local clearSession action; explain remote revoke not confirmed. |

## 3. Recovery state and authorization

A recovery link authenticates possession of email access but does not prove that the user has completed the intended password reset. Treat the resulting SDK session as RECOVERY_ONLY.

Required controls:
- Persist CALLBACK_PENDING before starting asynchronous PKCE exchange.
- Do not call FP-02 profile/workspace repository while CALLBACK_PENDING or PASSWORD_UPDATE_REQUIRED.
- Persist PASSWORD_UPDATE_REQUIRED before showing the password form.
- Only successful updateUser clears the recovery marker and changes AuthLevel to FULLY_AUTHENTICATED.
- Cancel/back signs out and clears local session state.
- Process death during callback pending does not replay the code; it shows interrupted recovery and requests a fresh link.
- Process death after password-update-ready restores the recovery form if the SDK session is still valid.
- A failed password update stays RECOVERY_ONLY.

Do not save the raw code, access/refresh token, PKCE verifier or full recovery URL in the app recovery marker.

## 4. Sign-out security

Pinned supabase-kt 3.6.0 defaults to SignOutScope.LOCAL: revoke the current Auth session and clear it locally after successful server response. It does not sign the user out from all devices.

If the server request fails:
- Do not keep the app trapped in the authenticated dashboard.
- Offer retry and explicit device-local clearSession.
- If device-local clear is chosen, clear all local Auth/recovery UI state.
- Show that remote revocation could not be confirmed.
- Future MobiDesk broker operations must independently check active session_id/authorization; a locally cleared client session is not proof that a copied access token is immediately invalidated.

## 5. Session storage

The pinned supabase-kt 3.6.0 default config auto-loads and auto-saves Auth session state. AuthImpl defaults to SettingsSessionManager, which serializes UserSession to JSON and writes it through the multiplatform Settings abstraction.

The code inspected does not prove the exact Android Settings backing implementation or encryption-at-rest behavior for the dependency graph resolved by this app. This is a bounded SDK/dependency inspection task. Do not claim the session is encrypted at rest until proven. The app's manifest has allowBackup=false; verify the merged manifest as part of release testing.

## 6. Email enumeration and error handling

Do not blindly replace every Auth error with the same generic message.

- invalid_credentials should remain generic for unknown email/wrong password.
- email_not_confirmed is a distinct Auth code. Current upstream GoTrue code checks whether the supplied password is valid before returning this error. Therefore a specific confirmation message can be useful without revealing the state to someone who does not know the password. Verify against the deployed project version before release.
- Recovery request should use the same generic acknowledgement for known and unknown email. Supabase documents that resetPasswordForEmail does not reveal whether an account exists.
- email_address_not_authorized and rate-limit codes are operational errors and need actionable messages.

Do not expose raw provider exceptions, session IDs, authorization headers, callback codes or query values.

## 7. Email delivery security and operational blocker

The historical SUPABASE_DISCOVERY.md snapshot dated 27 October 2026 reported custom SMTP disabled.

Supabase's documented built-in hosted email service:
- only sends to email addresses belonging to members of the Supabase organization;
- is limited to two emails per hour project-wide in the documented Confirm email configuration;
- is best-effort and not intended for production student email delivery.

Therefore, until custom SMTP or a Send Email Hook is configured and tested, password recovery and email-confirmation delivery to ordinary student recipients must be treated as non-functional for pilot acceptance. Do not tell students a recovery message was delivered merely because the API call succeeded.

See SUPABASE_CONFIGURATION.md for the required configuration sequence. No SMTP secret belongs in GitHub, this document or chat.

## 8. Required controls

| Asset | Control |
|---|---|
| Email/password | Password transient; email omitted from telemetry. |
| Access/refresh session | Supabase SDK owns it; verify storage backend and backup policy. |
| Recovery callback | Exact scheme/host/path, SDK PKCE verification, one-time processing, no raw code persistence. |
| Recovery marker | Stage, time and one-way fingerprint only; no raw token/code/URL. |
| AuthLevel | RECOVERY_ONLY cannot invoke workspace/profile fetch. |
| Sign-out | Explicit LOCAL scope; clearSession fallback; honest remote-revocation copy. |
| Build config | Publishable/anon key only; no service-role/secret key. |
| Diagnostics | Opaque attempt ID, event class and result only; no PII/secrets. |
| Mail sender | Custom SMTP/Send Email Hook; provider secrets stored only in Supabase secret configuration. |

## 9. Residual risks and blockers

- BLOCKING: exact Android Settings backend and encryption at rest not proven.
- BLOCKING: custom SMTP/Send Email Hook not configured/verified for student recipients.
- BLOCKING: process-death/callback replay/recovery-cancel fixes not implemented or tested.
- OPEN: public signup setting and institution provisioning policy must be confirmed.
- OPEN: custom scheme collision risk must be assessed on the actual device.
- OPEN: MFA requirement must be checked with institutional policy.

## 10. References

- supabase-kt 3.6.0 signOut and LOCAL default: https://github.com/supabase/supabase-kt/blob/3.6.0/Auth/src/commonMain/kotlin/io/github/jan/supabase/auth/Auth.kt
- supabase-kt 3.6.0 AuthImpl: https://github.com/supabase/supabase-kt/blob/3.6.0/Auth/src/commonMain/kotlin/io/github/jan/supabase/auth/AuthImpl.kt
- supabase-kt 3.6.0 Android deep link handler: https://github.com/supabase/supabase-kt/blob/3.6.0/Auth/src/androidMain/kotlin/io/github/jan/supabase/auth/Android.kt
- Supabase Auth error codes: https://supabase.com/docs/guides/auth/debugging/error-codes
- Supabase built-in email service and SMTP: https://supabase.com/docs/guides/auth/auth-smtp
- Supabase email flow testing: https://supabase.com/docs/guides/auth/testing-auth-flows
- Supabase rate limits: https://supabase.com/docs/guides/auth/rate-limits
