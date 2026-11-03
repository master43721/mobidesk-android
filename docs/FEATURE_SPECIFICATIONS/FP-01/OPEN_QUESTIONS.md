# FP-01 Open Questions — Owner Review

**Version:** 0.1 — REVIEW DRAFT  
**Date:** 3 November 2026  
**Purpose:** Get product decisions before converting FP-01 from draft into an implementation-ready specification.

## 1. Decisions required

| ID | Question | Current evidence | Recommended starting point | Your decision |
|---|---|---|---|---|
| FP01-OQ-01 | Should students sign in with school email only, or also roll number/username? | Current UI asks for school email. | Keep school email only for pilot unless school identity policy requires another identifier. | OPEN |
| FP01-OQ-02 | Should public signup be disabled in the Supabase project? | App has no signup screen. Historical project audit reported provider signup enabled. | Disable public signup if all users must be roster/institution provisioned; verify admin provisioning path first. | BLOCKING |
| FP01-OQ-03 | Should email confirmation remain required? | Historical project audit reported it was required. | Keep required for institution-controlled email accounts unless Owner/Lab IT has a documented alternative. | OPEN |
| FP01-OQ-04 | Should unconfirmed, unknown-email and wrong-password errors be indistinguishable? | Current code has a separate email-not-confirmed message. | Use generic external wording to reduce account enumeration. | OPEN |
| FP01-OQ-05 | After a password reset, should the app keep the recovery-established session or sign the student out and require a fresh sign-in? | Current implementation keeps the recovery session and returns to authenticated UI. | Keep session if Supabase confirms password update and session validity; require fresh sign-in only if policy demands it. | OPEN |
| FP01-OQ-06 | Is custom SMTP/email sender configured and verified for pilot? | Historical audit reported custom SMTP disabled and delivery unverified. | Require a successful end-to-end test email before claiming recovery is production-ready. | BLOCKING |
| FP01-OQ-07 | Should FP-01 include MFA, passkeys or social sign-in? | Not present in current app/feature inventory. | Exclude from pilot unless institution policy requires MFA. | OPEN |
| FP01-OQ-08 | Should the app use the existing custom URI scheme or migrate recovery to verified HTTPS Android App Links? | Existing code uses custom scheme and PKCE. | Keep current URI for staging while completing a hijack/verification review; make production decision before release. | OPEN |
| FP01-OQ-09 | What sign-out scope is required: local device only, current session, or all devices? | Current code calls signOut() with no explicit scope; pinned SDK default is not verified. | Current device/current session by default; explicit "sign out all devices" should be a separate capability if needed. | OPEN |
| FP01-OQ-10 | What Auth session lifetime and maximum inactivity policy should the college enforce? | Supabase config values not inspected in this draft. | Use an Owner/Lab IT policy; do not invent numbers. | OPEN |
| FP01-OQ-11 | What password minimum and complexity policy should be used? | Current UI checks length >=8; live Supabase policy is unknown. | Supabase server policy must be authoritative; align copy and validation after measuring live settings. | OPEN |
| FP01-OQ-12 | Should password managers/autofill be supported? | Current fields use email/password keyboard types; Autofill semantics not tested. | Support Android password managers without adding MobiDesk password persistence. | OPEN |
| FP01-OQ-13 | What exact phrase should the app use: "school account", "college account" or institution-specific wording? | Current UI uses "school email" and "school account". | Use "college account" if that is the institution's normal student language. | OPEN |
| FP01-OQ-14 | What support contact should be shown for account not provisioned or recovery failures? | No support address/helpdesk value established in FP-01 source. | Use owner-approved college IT contact, not a guessed email address. | OPEN |
| FP01-OQ-15 | Should account-not-provisioned be part of FP-01 or FP-02? | Current app displays it after a student profile lookup. | FP-02; FP-01 ends at authenticated identity. | RECOMMENDED |
| FP01-OQ-16 | Should FP-01 provide explicit "resend email confirmation" action? | Not implemented in current app. | Exclude unless student support data shows it is needed. | OPEN |

## 2. Technical questions that require evidence, not preference

| ID | Question | Required evidence |
|---|---|---|
| FP01-TQ-01 | How does Supabase Kotlin SDK 3.6.0 persist and protect Auth session material on Android? | Inspect pinned artifact/source and storage backend; document location, encryption and backup behavior. |
| FP01-TQ-02 | What is the default effect of client.auth.signOut() in SDK 3.6.0? | Source/API inspection plus test of local session removal and server-side session invalidation. |
| FP01-TQ-03 | Does handleDeeplinks enforce PKCE state/verifier and replay rejection exactly as assumed? | Test valid, malformed, expired, wrong-state and replayed callbacks. |
| FP01-TQ-04 | Is the exact recovery redirect allowed in the live Supabase project and does the email template preserve it? | Read-only dashboard verification and real staging email. |
| FP01-TQ-05 | Does the current UI reveal account existence through error text or timing? | Comparative tests using synthetic known, unknown, unconfirmed and wrong-password accounts. |
| FP01-TQ-06 | Can Auth state be separated cleanly from workspace loading without breaking the current student route? | ViewModel/repository interface tests and Compose navigation tests. |

## 3. Please critique these aspects of the draft

1. **Feature boundary:** Should account provisioning and confirmation/resend actions belong here or remain outside FP-01?
2. **User journey:** Is email/password plus email recovery enough for your institution?
3. **Security:** Do you agree with generic account errors and disabling public signup?
4. **Session policy:** What should happen on sign-out and after password recovery?
5. **Architecture:** Do you agree that FP-01 should stop at Authenticated and FP-02 should load/verify the student profile?
6. **Acceptance:** Are the test cases sufficient for calling FP-01 complete?

Please answer by OQ ID or write free-form corrections. This v0.1 is designed to be edited based on your critique.
