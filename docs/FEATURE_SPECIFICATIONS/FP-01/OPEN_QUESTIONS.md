# FP-01 Open Questions and Owner Decisions

**Version:** 0.2 — OWNER CRITIQUE REVISION  
**Date:** 4 November 2026

The v0.1 draft repeated routine defaults across several documents. This revision closes the choices that can be given a safe pilot default and keeps only owner-controlled policy and deployment decisions open.

## 1. Owner decisions

| ID | Question | Recommended default | Why it remains open |
|---|---|---|---|
| FP01-OQ-01 | Should public Supabase signup be disabled at the project level? | Yes, if all students are provisioned by institution roster/admin. | This is a live Auth project setting. Verify current value and ensure the approved admin provisioning path still works before changing it. |
| FP01-OQ-02 | Does college policy require MFA for the R1 pilot? | No additional MFA factor in FP-01 unless institution policy mandates it. | This is an institutional security policy, not a code preference. |
| FP01-OQ-03 | Which custom SMTP provider or Send Email Hook should deliver student email? | Choose an owner-controlled production-capable provider with a verified sender domain. | Provider account, sender domain, DNS records and SMTP credentials must be supplied/configured by the Owner. Secrets must never be sent in chat or committed. |
| FP01-OQ-04 | After a successful password update, should the recovery-established session be retained? | Yes; transition to FULLY_AUTHENTICATED only after updateUser succeeds. | Current implementation retains the session; Owner should approve this user experience. |
| FP01-OQ-05 | Is a device-local sign-out fallback acceptable when Supabase cannot confirm current-session revocation? | Yes, with explicit wording that remote revocation was not confirmed. | The fallback protects the device UI but cannot assert that the server revoked the refresh session. |
| FP01-OQ-06 | Is the existing PKCE custom URI acceptable for the pilot? | Retain current URI for pilot, with callback validation and competing-app review; assess HTTPS App Links before broad release. | Custom schemes can be claimed by another installed app; Owner accepts residual pilot risk or chooses verified App Links. |

## 2. Checks that are not product questions

These should be verified as project/configuration tasks rather than left as vague design unknowns:

| ID | Check | Current evidence | Required action |
|---|---|---|---|
| FP01-CFG-01 | Email provider enabled | Historical discovery says enabled. | Recheck in the live Supabase project. |
| FP01-CFG-02 | Email confirmation setting | Historical discovery says required. | Read current value and preserve unless Owner changes policy. |
| FP01-CFG-03 | Recovery redirect allow-list | Historical discovery reported exact URI added. | Recheck exact URI and remove broad/wildcard entries if any. |
| FP01-CFG-04 | OTP/recovery expiry | Not verified. | Read current value and record it; no new number invented here. |
| FP01-CFG-05 | Password min/strength/HIBP policy | Not verified. | Read current server policy; keep server authoritative. |
| FP01-CFG-06 | Auth request/email rate limits | Not verified. | Read values; ensure user-facing mapping handles returned codes. |
| FP01-CFG-07 | Session lifetime/refresh | Not verified. | Read current policy; do not change in FP-01 without approval. |
| FP01-CFG-08 | Auth session storage at rest in supabase-kt 3.6.0 | SDK stores serialized UserSession through SettingsSessionManager; encryption and Android backend not established by the SDK source inspected. | One short SDK/storage inspection and device test; decide whether a custom encrypted SessionManager is required. |
| FP01-CFG-09 | Recovery email end-to-end delivery | Historical audit reports custom SMTP disabled. | Must configure custom SMTP/Send Email Hook and receive a test email at a non-team address before recovery acceptance. |
| FP01-CFG-10 | Deployed GoTrue behavior for email_not_confirmed | Current upstream source checks password validity before the confirmation check; deployed server version not verified. | One integration test with valid password/unconfirmed email and invalid password/unconfirmed email. |

## 3. Technical questions with a short evidence task

| ID | Question | Minimum evidence needed |
|---|---|---|
| FP01-TQ-01 | Does the default Android Settings storage used by the SDK encrypt serialized session data at rest? | Inspect exact multiplatform-settings Android implementation resolved by Gradle; inspect app data behavior on a test device; record backup behavior. |
| FP01-TQ-02 | What happens if callback exchange is interrupted between SDK session import and app recovery-ready persistence? | Process-kill injection at the transition; verify recovery marker blocks FP-02 and safe restart path works. |
| FP01-TQ-03 | Does a competing app claiming the custom URI change Android callback routing on supported devices? | Install a test app with the same scheme/host on a physical test device; record chooser/default behavior and prove PKCE verification still rejects invalid callbacks. |

## 4. Closed by v0.2 defaults

These are no longer open questions unless the Owner objects:
- Email only, matching the current login UI.
- No signup screen in MobiDesk.
- No MFA/passkeys/social login in FP-01 pilot unless institutional policy requires them.
- Keep the existing email recovery channel.
- Keep the current successful-recovery session after update, then mark FULLY_AUTHENTICATED.
- Use SDK signOut LOCAL for current-session sign-out, not all devices.
- Support standard Android password manager/autofill without storing passwords in app data.
- Keep profile/assignment lookup outside FP-01; it is FP-02/FP-03.
- No new resend-confirmation UI in this FP.
- Keep existing session lifetime configuration; verify it, do not invent a new policy.

## 5. Review focus

Please respond only to OQ-01 through OQ-06 and any CFG/TQ item where the recommended treatment is wrong. Everything else has a proposed default to reduce review noise.
