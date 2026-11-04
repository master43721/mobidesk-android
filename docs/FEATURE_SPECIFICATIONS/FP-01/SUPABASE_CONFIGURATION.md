# FP-01 Supabase Configuration and Email Delivery Plan

**Version:** 0.2 — OWNER CRITIQUE REVISION  
**Date:** 4 November 2026  
**Status:** Configuration checklist and required operational action. No live project setting was changed by this document revision.

## 1. Why this is a release prerequisite

The historical SUPABASE_DISCOVERY.md snapshot dated 27 October 2026 reported:
- Email/password provider enabled.
- Email confirmation required.
- Provider-level signup enabled.
- The recovery redirect URI had been added to the allow-list.
- Custom SMTP disabled.
- Actual recovery-email delivery unverified.

These values are historical and must be rechecked. However, Supabase's current documentation states that the hosted built-in email service only sends to organization team-member addresses and is currently limited to two emails/hour project-wide in the documented Confirm email configuration. It is best-effort and not intended for production.

**Disposition:** If custom SMTP remains disabled, recovery/confirmation email delivery to ordinary student email addresses is non-functional for pilot purposes. FP-01 recovery cannot pass release acceptance until custom SMTP or a Send Email Hook is configured and a real message reaches a synthetic non-team recipient.

## 2. Current Android integration — verified from source

- Supabase BOM 3.6.0.
- auth-kt and postgrest-kt.
- Ktor Android 3.4.3.
- SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY read from local.properties and injected into BuildConfig.
- PKCE enabled.
- Current recovery URI: com.mobidesk.android://auth-callback/recovery.
- MainActivity checks VIEW action, scheme and host; current implementation uses a loose pathSegments.contains("recovery") check which FP-01 will tighten.
- Only publishable/anon key may be in the Android client. No service-role/secret key.

This is evidence of app code, not live dashboard configuration.

## 3. Required project settings review

| Setting | Desired policy | Last evidence | Required action |
|---|---|---|---|
| Email/password provider | Enabled | Historical discovery says enabled | Read current value. |
| Email confirmation | Keep required unless Owner changes institution policy | Historical discovery says required | Read current value; do not disable just to avoid mail setup. |
| Public signup | Disable if institution roster/admin is the only approved provisioning path | Historical discovery reported enabled | Owner confirms provisioning model, then read/change project setting under approved action. |
| Roster provisioning | Server-side/admin process only; client metadata cannot grant student access | Historical discovery described roster-gated Auth trigger | Reinspect live trigger and test. |
| Recovery redirect | Exact approved URI, no wildcard | Historical discovery reported exact URI added | Recheck current allow-list and exact redirect. |
| Recovery template | Correct recovery link and redirect path | Not inspected | Review template and send test. |
| Custom SMTP / Send Email Hook | Required for non-team student recipient delivery | Historical discovery reported SMTP disabled | Configure before recovery release. |
| Sender domain | Verified and authorized for provider | Unknown | Select domain and complete provider DNS verification. |
| Password policy | Supabase server setting is authoritative | Unknown | Read min length, required characters and leaked-password protection. |
| OTP/recovery expiry | Bounded and documented | Unknown | Read current value; use it for recovery marker retention. |
| Email/request rate limits | Documented and mapped to errors | Unknown | Read project values; test returned errors. |
| Session lifetime/refresh | Institution-approved policy | Unknown | Read and record; no client override. |
| Auth logs/retention | Operator access and retention controlled | Unknown | Review. |

## 4. Configure custom SMTP or Send Email Hook

### Owner inputs required

1. Select an email provider that supports transactional email and SMTP or a Supabase Send Email Hook.
2. Choose the approved sender domain/address.
3. Complete provider account verification and domain DNS records (SPF/DKIM and any provider-required records).
4. Obtain SMTP host, port, username and password, or the hook secret/configuration.
5. Store credentials only in Supabase project secret/configuration settings; never in Android, GitHub, docs or chat.
6. Configure production mail templates and redirect.
7. Confirm provider quotas, retry/bounce handling and sender reputation.

The provider/domain/credentials are not specified in the repository. Do not invent them or select a paid provider without the Owner's decision.

### Verification procedure

- Send a confirmation email to a synthetic recipient not belonging to the Supabase organization.
- Send a password-recovery email to the same controlled mailbox.
- Verify the exact recovery URI survives the email template.
- Open the link on a supported physical Android device.
- Confirm the SDK PKCE flow succeeds and the password can be updated.
- Verify the new password works and the old password fails.
- Exercise email rate-limit and recipient-authorization errors.
- Record only provider name, sender domain, test timestamp, message outcome and redacted error class. Do not store email content, codes or tokens in public docs.

**Acceptance gate:** recovery remains BLOCKED until a non-team synthetic mailbox receives both the required test mail and recovery email and the flow completes end to end.

## 5. Rate-limit handling

The pinned supabase-kt 3.6.0 AuthErrorCode enum includes:
- EmailAddressNotAuthorized — email_address_not_authorized
- OverEmailSendRateLimit — over_email_send_rate_limit
- OverRequestRateLimit — over_request_rate_limit

Required student copy:
- email_address_not_authorized: explain email delivery is not enabled for this address and point to approved college support.
- over_email_send_rate_limit: wait before requesting another email.
- over_request_rate_limit: too many Auth requests; wait before retrying.

Do not map all three to a generic “try again” message. Do not show a fabricated countdown unless the provider supplies a reliable retry duration.

## 6. Signup policy

The app has no signup route; that does not disable public provider signup.

Recommended if institution provisioning is mandatory:
- disable public signup in Supabase Auth;
- retain Owner/admin roster provisioning;
- verify existing provisioned accounts still sign in;
- verify unapproved addresses cannot create a usable student identity;
- ensure Auth users without a student profile cannot enter FP-02/FP-03 workspace authorization.

The Owner must confirm the account-provisioning model before this setting is changed. No project setting is altered by this documentation PR.

## 7. Environment and secrets policy

- Use only publishable/anon key in Android.
- Keep local.properties untracked.
- Never store service-role keys, DB password, SMTP password, Send Email Hook secret or signing keys in GitHub.
- Use synthetic accounts only in test environments.
- Do not send provider credentials through chat.
- A hosted Supabase staging project is not currently evidenced. Unit/UI tests must use a fake Auth repository. Provider integration requires an Owner-approved isolated local or hosted environment.

## 8. Official references

- Built-in email limitations and custom SMTP: https://supabase.com/docs/guides/auth/auth-smtp
- Testing email flows and two-emails/hour built-in service: https://supabase.com/docs/guides/auth/testing-auth-flows
- Auth error codes: https://supabase.com/docs/guides/auth/debugging/error-codes
- Auth rate limits: https://supabase.com/docs/guides/auth/rate-limits
- Password policy: https://supabase.com/docs/guides/auth/password-security
- Redirect URLs: https://supabase.com/docs/guides/auth/redirect-urls
