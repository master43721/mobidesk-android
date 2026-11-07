# FP-01 Progress and Evidence

**Version:** 0.2 — OWNER CRITIQUE REVISION  
**Snapshot:** 4 November 2026  
**Repository:** master43721/mobidesk-android  
**Documentation provenance branch:** feature/fp-01-authentication-draft (stale; 39 commits behind current main at comparison time). **Current handoff branch:** docs/fp-01-current-main-handoff  
**Implementation changes:** None. This branch contains documentation drafts only.

## 1. Current status

| Workstream | Status | Evidence |
|---|---|---|
| Canonical app source inspected | DONE — read-only | MainActivity, MobiDeskApplication, MobiDeskViewModel, MobiDeskRepositories, MobiDeskApp, manifest, Gradle and existing tests. |
| Parent PRD traceability | CORRECTED IN V0.2 | FR-AUTH-01 maps to FP-01; FR-AUTH-02 profile/assignment RLS is FP-02/FP-03. |
| Supabase SDK sign-out | VERIFIED AGAINST 3.6.0 TAG | Default SignOutScope.LOCAL; current session only. SDK clears local session on successful logout; remote errors may throw before clear. |
| Supabase SDK PKCE ordering | VERIFIED AGAINST 3.6.0 TAG | Code exchange imports session/sets Authenticated before Android helper invokes onSessionSuccess. |
| Recovery cancel defect | VERIFIED IN APP SOURCE | Current cancel only clears a UI boolean. v0.2 defines sign-out/clearSession correction. |
| Recovery process-death gap | VERIFIED IN APP SOURCE | No durable recovery marker currently present. v0.2 defines CALLBACK_PENDING/PASSWORD_UPDATE_REQUIRED. |
| Callback de-duplication | DESIGN RISK VERIFIED; RUNTIME TEST NOT RUN | onCreate/onNewIntent route to handler; no durable guard found. |
| Sign-out failure exit | VERIFIED IN APP SOURCE | Current catch displays error but no local sign-out action. v0.2 defines fallback. |
| AuthRepository seam | GAP VERIFIED | Current ViewModel constructs repository directly. v0.2 specifies lightweight interface/factory seam. |
| Auth session storage | PARTIALLY VERIFIED | SDK serializes UserSession through SettingsSessionManager. Exact Android backing/encryption unresolved. |
| Supabase email delivery | BLOCKING — HISTORICAL PROJECT CONFIG + CURRENT PLATFORM DOCS | Historical audit reported custom SMTP disabled. Built-in email only reaches organization team members and is rate-limited. Custom SMTP/Send Email Hook required. |
| Live Auth project settings | NOT RECHECKED | Signup, confirm, redirect, expiry, password and rate-limit values need read-only verification. |
| Baseline tests | VERIFIED IN SOURCE; NOT RUN IN THIS REVISION | Two unit tests and two Compose smoke tests are named in TEST_PLAN.md. |
| Full FP-01 acceptance | NOT RUN | No app code changed and no tests/build were run for v0.2 documentation revision. |

## 2. v0.1 critique disposition

| Critique | v0.2 correction |
|---|---|
| Parent FR-AUTH-01/02 mapping wrong | Corrected. FR-AUTH-01 is FP-01; FR-AUTH-02 belongs FP-02/FP-03. |
| Recovery cancellation leaves session signed in | Added mandatory sign-out on recovery cancel/back, with local clear fallback. |
| Recovery state lost on process death | Added durable recovery marker and AuthLevel. |
| Callback may replay on Activity recreation | Added exact-path validation and pending/consumed callback de-duplication; runtime tests remain required. |
| Sign-out default scope was answerable | Verified default LOCAL against exact 3.6.0 tag; removed as an unknown. |
| Custom SMTP issue is a known blocker | Marked student recovery non-functional for ordinary student addresses until provider configuration and delivery tests pass. |
| Auth architecture over-designed | Removed proposed AuthSessionCoordinator/AuthDeepLinkRouter; use current ViewModel plus AuthRepository interface and minimal AuthLevel. |
| No repository injection seam | Explicit AuthRepository interface/factory seam added to design and tasks. |
| MUST + proposal contradiction | Removed; all FRs are requirements. Owner questions are separate. |
| Account enumeration policy too broad | Keep email_not_confirmed actionable; test actual deployed GoTrue behavior. Generic reset acknowledgement remains. |
| Missing rate-limit UX | Added specific email_address_not_authorized, over_email_send_rate_limit and over_request_rate_limit errors. |
| Too many tests/no mandatory subset | Defined four existing baseline tests and 24 must-run core pilot cases. |
| Staging project assumed | No hosted staging is assumed. Use fake repository for unit/UI and local Supabase + Mailpit for provider tests where practical; hosted SMTP integration requires Owner-approved environment. |
| No estimates/dates | Added task estimates and 4–13 November planning window. |

## 3. Existing baseline tests — names verified

- app/src/test/java/com/mobidesk/android/AuthErrorMessagesTest.kt: invalidCredentialsUseAStableSafeMessage
- app/src/test/java/com/mobidesk/android/AuthErrorMessagesTest.kt: backendDetailsAreNotShownToTheUser
- app/src/androidTest/java/com/mobidesk/android/MobiDeskSmokeTest.kt: signInScreenDoesNotOfferFakePreviewAccess
- app/src/androidTest/java/com/mobidesk/android/MobiDeskSmokeTest.kt: passwordRecoveryRequestScreenOpensWithoutSendingAnEmail

The last test only opens the reset screen. It does not prove email delivery.

## 4. v0.2 documents

- README.md
- PRD.md
- TECHNICAL_DESIGN.md
- PLAN.md
- TEST_PLAN.md
- SECURITY_PRIVACY.md
- SUPABASE_CONFIGURATION.md
- DECISIONS.md
- OPEN_QUESTIONS.md
- TRACEABILITY.md
- PROGRESS.md
- REVISION_NOTES.md

## 5. No-change statement

No Android source, Gradle, manifest, CI, Supabase project setting, database schema, Auth user, email template, SMTP setting, server, VM or FreeRDP submodule was changed. No build or test result is claimed by this documentation revision.

## 6. Next step

Owner reviews OQ-01 through OQ-06 and the proposed recovery/AuthLevel behavior. After critique, reconcile the package and publish v1.0. Implementation begins only after the Owner approves the final FP-01 package.


## 7. Owner direction and current-main handoff — 4 November 2026

- The Project Owner has directed preparation of a complete FP-01 implementation handoff for Antigravity.
- The current account-model decision is incorporated: students do not self-register; College Administrator provisions student Auth identities and sends activation invitations. Invitation dispatch/activation is separately owned and not part of FP-01.
- The historical `feature/fp-01-authentication-draft` branch is 33 commits ahead / 39 behind current main and has diverged. Do not use it as the implementation base.
- A fresh handoff branch was based on current main; preparation SHA: `60b3152c84ec13a0f43ec59a983e968c0b5b3ff7`. Antigravity must re-fetch main before creating its own implementation branch.
- No Android source, Gradle file, Supabase live setting, database, SMTP configuration or live account was changed by this documentation update.
- No tests/build are claimed passed by this documentation update. SMTP delivery and physical-device recovery remain external release gates.
