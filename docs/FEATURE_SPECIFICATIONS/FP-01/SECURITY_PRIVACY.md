# FP-01 Security and Privacy Requirements

**Version:** 0.1 — REVIEW DRAFT  
**Date:** 3 November 2026  
**Scope:** Authentication and account recovery only.

## 1. Security objectives

1. Only the approved Supabase Auth provider verifies student credentials.
2. Client code contains no service-role key or privileged backend secret.
3. Passwords are transient and are never stored or logged by MobiDesk.
4. Authenticated UI is shown only after the pinned SDK confirms the session.
5. Recovery callbacks are validated through the configured URI and Supabase PKCE/OTP handling.
6. Account-existence information is not unnecessarily disclosed.
7. Session persistence, token storage and sign-out semantics are documented from exact SDK behavior.
8. Authentication success does not bypass student-profile and VM authorization checks in later FPs.

## 2. Assets and trust boundaries

### Assets

- Student email/password during user entry and Auth request.
- Supabase access and refresh tokens/session data.
- PKCE verifier/state and recovery callback values.
- Supabase project URL and publishable/anon key.
- Authenticated user identity and session status.
- Recovery email delivery and redirect configuration.
- App/UI state indicating authenticated status.

### Trust boundaries

- Android UI and app process: trusted to protect ordinary app data but not treated as a security authority.
- Supabase Auth: authoritative credential/session verifier.
- Supabase Auth configuration: owner-controlled provider, signup, confirmation, redirect, password and rate settings.
- Android deep-link resolver: external OS routing; incoming URI must be treated as untrusted until SDK validation succeeds.
- FP-02: separately validates student profile/eligibility after FP-01 Auth success.

## 3. Threats and controls

| Threat | Risk | Required control |
|---|---|---|
| Service-role key embedded in APK | Critical privilege compromise | Use only publishable/anon client key; secret scan source, BuildConfig and APK. |
| Password saved in UI state or disk | Account takeover | Password is transient Compose state, not rememberSaveable/DataStore/preferences/database; clear at terminal state. |
| Password accidentally logged | Account takeover | No password interpolation in logs, exception messages, analytics or crash context. |
| Stale Auth state shown after refresh failure | Unauthorized-looking UI / later access confusion | One authoritative session-state flow; clear protected UI on RefreshFailure/NotAuthenticated. |
| Token persistence misunderstood | Theft from device storage | Inspect exact supabase-kt 3.6.0 storage implementation; document storage location, encryption and backup behavior. |
| Custom-scheme callback hijack | Recovery code/session interception | PKCE, exact scheme/host/path validation, SDK state verification, redirect allow-list; assess verified HTTPS App Links. |
| Recovery link replay/expiry | Unauthorized reset | Rely on SDK/provider single-use/expiry validation; prove with integration tests. |
| User enumeration | Targeted account discovery | Generic sign-in/recovery messages; equivalent recovery acknowledgement for existing/non-existing accounts. |
| Public signup bypasses roster provisioning | Unauthorized Auth accounts | Recheck project signup setting and roster trigger. Prefer disabling public signup if invitations/provisioning are the approved path; require explicit Owner/Lab IT approval. |
| Weak or leaked password | Account compromise | Configure server-side password policy and leaked-password protection where subscription/configuration supports it; client-side minimum is not authoritative. |
| Mail delivery failure | Student cannot recover | Verify custom SMTP/domain/template and actual delivery in staging; don't report email delivered based on client response alone. |
| Race between account switch and requests | Wrong account state shown | Cancel/ignore stale operations by attempt identity; serialize Auth state transitions. |

## 4. Credential handling requirements

### Email

- Treat email as personal information.
- Trim outer whitespace before submission.
- Do not log it or attach it to telemetry.
- Avoid client-side domain allowlists unless the Owner explicitly approves the institution's full domain/alias policy. Server-side provisioning remains authoritative.

### Password

- Pass password unchanged; do not trim or normalize it.
- Never save password in persistent UI state, SavedStateHandle, DataStore, SharedPreferences, database, file, clipboard or analytics.
- Do not put password in navigation arguments, Intent, Bundle, URI or ViewModel observable state.
- Clear app-owned transient values after success, cancellation and terminal failure as safely possible.
- Do not claim forensic zeroization of immutable JVM Strings or framework copies. Minimize copies and document framework behavior.
- Never add a remember-password feature.

### Access/refresh session and PKCE material

- Keep handling inside the Supabase SDK except where the SDK requires a callback.
- Never log or display raw tokens, full callback URLs, callback codes or PKCE values.
- Do not manually parse JWT claims to establish an authenticated user when the SDK session stream is available.
- Inspect exact SDK persistence and encryption behavior before release.
- Ensure app backup policy does not unintentionally export Auth secrets. Manifest currently sets allowBackup=false; verify merged manifest and Android-version behavior.

## 5. Sign-in and account enumeration

Current AuthErrorMessages maps invalid credentials separately and maps email-not-confirmed to a specific message. The current recovery success message is generic. These are verified current implementation behaviors, not the final security policy.

**Proposed policy for Owner approval:**
- Unknown email, incorrect password and unconfirmed email should not produce externally distinguishable account-existence messages.
- Recovery request should always display the same generic acknowledgement.
- Do not expose whether an account is on the roster or provisioned.
- Use generic timing/error behavior where practical; provider-side rate limiting remains required.
- Do not disable email confirmation merely to simplify sign-in.

## 6. Supabase project controls

These are owner-controlled project settings, not Android-code switches:

- Email/password provider enablement.
- Public signup enablement/disablement.
- Email confirmation policy.
- Roster/invitation provisioning restrictions.
- Password minimum/strength and leaked-password protection.
- Auth rate limits / abuse protection / CAPTCHA if selected.
- OTP/recovery-link expiry.
- Allowed redirect URI list.
- Email templates and sender/SMTP configuration.
- Auth audit logs/retention.
- Session lifetime and refresh-token reuse policy.

No project settings are changed by this document. Verify live state before acceptance.

## 7. Privacy-safe diagnostics

Permitted:
- Opaque attempt ID.
- Event class.
- Duration.
- Safe result class.
- App and SDK version.
- Coarse network class where justified.

Forbidden:
- Email, password, refresh/access token, session ID, recovery code, PKCE verifier/state, callback query, full callback URL, raw exception, authorization header, IP address or device identifier unless separately justified and approved.

## 8. Release security checklist

- [ ] No service-role/secret key in source, Gradle, local properties tracked by Git, BuildConfig, APK or logs.
- [ ] Password not persisted or logged.
- [ ] SDK Auth storage behavior and backup treatment verified.
- [ ] PKCE and deep-link state validation tested.
- [ ] Exact redirect allow-list reviewed.
- [ ] No self-signup UI and project signup policy approved.
- [ ] Generic non-enumerating error policy tested.
- [ ] Provider password and rate-limiting policy approved.
- [ ] Recovery email sender/domain/template verified.
- [ ] Auth sign-out and session lifetime semantics documented.
- [ ] Authenticated identity does not bypass FP-02 profile/VM authorization.

## 9. Residual risks / blockers

- **BLOCKING:** exact session storage behavior for supabase-kt 3.6.0 has not been verified.
- **BLOCKING:** signOut default scope and effect on other sessions has not been verified.
- **BLOCKING:** current live Supabase signup, confirmation, password, rate and SMTP configuration has not been rechecked.
- **UNKNOWN:** whether the custom URI scheme is sufficient or verified HTTPS App Links are required.
- **OPEN:** exact non-enumerating error wording and policy need Owner decision.
