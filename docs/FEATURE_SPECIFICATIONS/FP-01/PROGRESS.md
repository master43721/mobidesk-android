# FP-01 Progress and Evidence

**Version:** 0.1 — REVIEW DRAFT  
**Snapshot:** 3 November 2026  
**Repository:** master43721/mobidesk-android  
**Branch:** feature/fp-01-authentication-draft  
**Implementation changes:** None. This branch contains specification drafts only.

## 1. Current status

| Workstream | Status | Evidence / missing evidence |
|---|---|---|
| Repository/source inspection | DONE — read-only | MainActivity, MobiDeskApplication, MobiDeskViewModel, MobiDeskRepositories, MobiDeskApp, manifest, Gradle and existing tests inspected. |
| Supabase SDK baseline | VERIFIED IN SOURCE | BOM 3.6.0; auth-kt; postgrest-kt; Ktor 3.4.3. |
| Email/password sign-in path | PRESENT IN SOURCE | SupabaseAuthRepository.signIn; no live integration test was run for this draft. |
| Session status observation | PRESENT IN SOURCE | MobiDeskViewModel observes Initializing, Authenticated, RefreshFailure and NotAuthenticated. |
| Sign-out path | PRESENT IN SOURCE; SEMANTICS UNKNOWN | client.auth.signOut() called without explicit scope; pinned SDK effect not tested. |
| Password recovery request | PRESENT IN SOURCE | resetPasswordForEmail with configured recovery URI; no actual email delivery test in this draft. |
| Recovery deep link | PRESENT IN SOURCE | MainActivity checks VIEW/scheme/host and calls handleDeeplinks; negative callback tests are missing. |
| Password update | PRESENT IN SOURCE | updateUser password path and minimum 8-char client check; provider policy not rechecked. |
| Auth/workspace separation | GAP | MobiDeskViewModel triggers workspace load after Authenticated. Design proposes FP-01/FP-02 boundary. |
| Error mapping | PARTIAL | Two unit tests exist. Current mapper exposes a distinct unconfirmed-email message; Owner policy pending. |
| UI tests | BASIC SMOKE BASELINE | Existing test verifies no preview/sign-up path and reset screen opens. It does not send a recovery email. |
| Security storage audit | UNKNOWN | Exact SDK 3.6.0 storage/encryption and backup behavior not inspected. |
| Live Supabase config | HISTORICAL ONLY | SUPABASE_DISCOVERY.md dated 27 Oct 2026; recheck required. |
| Full end-to-end acceptance | NOT RUN | No FP-01 integrated test suite or physical-device recovery test executed for this draft. |

## 2. Existing tests found

- app/src/test/java/com/mobidesk/android/AuthErrorMessagesTest.kt — two unit tests.
- app/src/androidTest/java/com/mobidesk/android/MobiDeskSmokeTest.kt — two Compose smoke tests.

The smoke test named passwordRecoveryRequestScreenOpensWithoutSendingAnEmail intentionally confirms navigation only; it is not proof of email delivery or password recovery.

## 3. Draft artefacts created

- README.md
- PRD.md
- TECHNICAL_DESIGN.md
- IMPLEMENTATION_PLAN.md
- TEST_PLAN.md
- SECURITY_PRIVACY.md
- SUPABASE_CONFIGURATION.md
- DECISIONS.md
- OPEN_QUESTIONS.md
- PROGRESS.md

## 4. Review log

| Date | Activity | Result |
|---|---|---|
| 3 Nov 2026 | Read-only audit of canonical repository authentication implementation | Existing happy-path sign-in and recovery code confirmed; several security/integration proofs remain unknown. |
| 3 Nov 2026 | Created FP-01 package v0.1 | Awaiting Project Owner critique. |

## 5. No-change statement

No app Kotlin source, Gradle, manifest, CI, Supabase project configuration, database schema, email template, server, VM or FreeRDP submodule was changed. No test/build run is claimed by this document-only draft.

## 6. Next step

Wait for Project Owner critique of PRD.md, DECISIONS.md and OPEN_QUESTIONS.md. Apply accepted changes consistently across technical design, implementation plan, security and test artefacts. Only then produce FP-01 v1.0 and derive implementation tasks.
