# FP-01 Implementation Plan — Student Authentication and Account Recovery

**Version:** 0.1 — REVIEW DRAFT  
**Date:** 3 November 2026  
**Repository:** master43721/mobidesk-android  
**Implementation status:** Planning only. No app source changes are part of this documentation draft.

## 1. Delivery approach

Implement FP-01 as a bounded authentication capability. Preserve the existing working sign-in/recovery path where tests prove it correct; refactor only where the current Auth/workspace coupling prevents a clean feature boundary or a measurable acceptance criterion.

Do not start by rewriting all authentication classes. First verify exact Supabase Kotlin 3.6.0 behavior and capture the current test/build baseline. All implementation changes happen in small, reviewable commits within the canonical repository.

## 2. Work packages

| Phase | Work package | Main output | Exit gate |
|---|---|---|---|
| FP01-G0 | Requirements and current-state freeze | Owner-approved FP-01 PRD, exact parent PRD traceability, live Supabase config facts | Scope and security decisions closed |
| FP01-G1 | SDK and platform verification | SDK storage/sign-out/deep-link findings; redirect threat decision | No critical SDK behavior assumed |
| FP01-G2 | Auth boundary and state model | Auth repository/coordinator/UI state separated from workspace reads | FP-01 can hand Authenticated state to FP-02 without loading VM/profile |
| FP01-G3 | Sign-in and session lifecycle | Sign-in, restore, refresh-failure and sign-out flows with tests | Cold start, process recreation, invalid credentials and sign-out pass |
| FP01-G4 | Recovery end-to-end | Generic reset request, verified deep link, password update and failure UX | Real staging email/callback/password update tests pass |
| FP01-G5 | Security and accessibility | Credential/logging checks, account-enumeration behavior, deep-link negative tests, accessibility review | No critical security/privacy test fails |
| FP01-G6 | Feature acceptance | Final traceability, CI/build/test report and owner sign-off | All MUST acceptance criteria have evidence |

## 3. Task breakdown

### G0 — Scope and baseline

| Task | Work | Evidence/status |
|---|---|---|
| FP01-001 | Confirm exact FP-01 boundary and parent FR-AUTH-01/02 traceability against PRD v1.2 | BLOCKED — Owner review |
| FP01-002 | Capture canonical source commit, Gradle/SDK/dependency versions and current app tests | Initial source audit recorded in TECHNICAL_DESIGN.md; rerun before implementation |
| FP01-003 | Recheck live Supabase Auth configuration without modifying it | UNKNOWN — requires owner-authorized project inspection |
| FP01-004 | Decide public signup, email confirmation, MFA requirement and user-facing error policy | OPEN — DECISIONS.md / OPEN_QUESTIONS.md |

### G1 — SDK and callback spikes

| Task | Work | Evidence/status |
|---|---|---|
| FP01-005 | Inspect supabase-kt 3.6.0 session persistence implementation and Android storage backend | NOT STARTED |
| FP01-006 | Verify signOut() default scope and local/server revocation behavior | NOT STARTED |
| FP01-007 | Verify PKCE callback verification, state handling and replay behavior | NOT STARTED |
| FP01-008 | Review custom URI redirect hijack risk and decide custom scheme vs verified HTTPS App Links | OPEN |
| FP01-009 | Test recovery email delivery and confirm exact redirect/template behavior in staging | BLOCKED by project/email configuration verification |

### G2 — Auth boundary

| Task | Work | Evidence/status |
|---|---|---|
| FP01-010 | Define app-owned Auth state model and operation results | DESIGN PROPOSED |
| FP01-011 | Separate AuthRepository/Coordinator responsibility from StudentWorkspaceRepository | NOT STARTED |
| FP01-012 | Make FP-02 consume authenticated identity via a clear handoff/interface | NOT STARTED |
| FP01-013 | Ensure no password/token/callback value enters UI state, SavedState or logs | NOT STARTED |

### G3 — Sign-in and session lifecycle

| Task | Work | Evidence/status |
|---|---|---|
| FP01-014 | Preserve/complete email-password sign-in with trimmed email and unchanged password | Existing path verified in source; tests incomplete |
| FP01-015 | Implement deterministic initial session restoration routing | Existing flow present; cold-start process tests missing |
| FP01-016 | Test refresh success/failure and Authenticated/NotAuthenticated transitions | NOT STARTED |
| FP01-017 | Implement/verify sign-out local state clearing and chosen server revocation semantics | Existing signOut call present; semantics unverified |
| FP01-018 | Normalize safe error mapping with approved enumeration policy | Existing mapper present; email-not-confirmed behavior requires decision |
| FP01-019 | Ensure password fields are transient and cleared at terminal state | Existing sign-in password uses remember, not rememberSaveable; recovery path needs teardown tests |

### G4 — Account recovery

| Task | Work | Evidence/status |
|---|---|---|
| FP01-020 | Verify recovery-request input validation and generic acknowledgement | Existing code present; backend equivalence test missing |
| FP01-021 | Verify exact redirect URI allow-list and email template | Historical allow-list update reported; live recheck required |
| FP01-022 | Harden callback route validation and SDK deep-link invocation | Existing scheme/host/path handling present; negative tests missing |
| FP01-023 | Verify recovery session before enabling password update | Existing state gate present; full integration test missing |
| FP01-024 | Reconcile client minimum-password check with current Supabase policy | Existing client checks 8 characters; live provider policy unknown |
| FP01-025 | Test update success, provider rejection and session outcome after reset | NOT STARTED |

### G5 — Security and accessibility

| Task | Work | Evidence/status |
|---|---|---|
| FP01-026 | Ensure generic non-enumerating sign-in/recovery behavior if approved | NOT STARTED |
| FP01-027 | Verify no service-role key or backend secret in source/APK | Existing code uses publishable key; automated scan still required |
| FP01-028 | Verify session/token storage at rest and backup exclusions | SDK storage audit required; manifest backup disabled is verified |
| FP01-029 | Test malformed, wrong-scheme, wrong-host, wrong-path, expired and replayed callbacks | NOT STARTED |
| FP01-030 | Accessibility pass for keyboard, TalkBack and error announcements | NOT STARTED |
| FP01-031 | Verify no secret/PII/callback query in Logcat, crash data or analytics | NOT STARTED |

### G6 — Acceptance

| Task | Work | Evidence/status |
|---|---|---|
| FP01-032 | Run unit tests and Compose UI tests | NOT STARTED |
| FP01-033 | Run staging Supabase sign-in/session/recovery integration tests | BLOCKED by staging test account/email |
| FP01-034 | Run physical-device cold start and recovery callback tests | NOT STARTED |
| FP01-035 | Update FP-01 PROGRESS.md with test run/build/source evidence | NOT STARTED |
| FP01-036 | Owner review and acceptance | NOT STARTED |

## 4. Proposed implementation file map

Current paths are shown as existing files. Proposed additions are suggestions only.

| Path | Proposed treatment |
|---|---|
| app/src/main/java/com/mobidesk/android/MobiDeskApplication.kt | Preserve single configured Supabase client; no Auth feature logic in Application. |
| app/src/main/java/com/mobidesk/android/MainActivity.kt | Keep deep-link entry point; make callback validation explicit/testable. |
| app/src/main/java/com/mobidesk/android/MobiDeskViewModel.kt | Refactor or split Auth/session state from workspace repository orchestration. |
| app/src/main/java/com/mobidesk/android/data/MobiDeskRepositories.kt | Separate SupabaseAuthRepository from StudentWorkspaceRepository if needed. |
| app/src/main/java/com/mobidesk/android/ui/MobiDeskApp.kt | Keep Compose UI state rendering; isolate Auth forms and recovery navigation. |
| app/src/test/java/com/mobidesk/android/AuthErrorMessagesTest.kt | Expand error mapping and non-enumeration tests. |
| app/src/androidTest/java/com/mobidesk/android/MobiDeskSmokeTest.kt | Expand UI tests; avoid real Supabase dependency in UI smoke tests. |
| Proposed app/src/main/java/com/mobidesk/android/auth/ | Optional Auth domain/state/coordinator package; confirm with repository style before creating. |
| Proposed app/src/test/.../auth/ | Auth state machine, validation and error tests. |
| Proposed app/src/androidTest/.../auth/ | Callback and UI integration tests. |

Do not create all proposed classes/files automatically. The final package structure should be agreed during FP01-G2.

## 5. Dependencies and sequencing

- G0 blocks implementation because requirements, live provider configuration and account-provisioning policy are unresolved.
- G1 blocks callback/recovery hardening because exact SDK behavior must be verified.
- G2 is required before claiming a clean FP-01/FP-02 feature boundary.
- G3 and G4 can share implementation infrastructure but should remain separate acceptance groups.
- G5 security tests are mandatory before FP01-G6.
- No Supabase database schema migration is expected for FP-01.
- Any Supabase Dashboard change requires explicit Owner approval and a recorded before/after configuration result.
- Keep dependency versions pinned unless a tested defect requires a separate upgrade.

## 6. Definition of done

FP-01 is not complete until:
- Every MUST requirement in PRD.md is mapped to one or more passing test IDs.
- Valid sign-in and session restoration work on the target Android reference device.
- Sign-out semantics are known and the observed behavior matches the approved policy.
- Password recovery works end-to-end with a real staging email and verified redirect.
- Negative callback, expired link, replay and account-enumeration tests pass.
- No secrets/credentials are persisted or logged.
- The FP-01-to-FP-02 boundary is explicit.
- CI, build, unit/UI/integration test evidence is linked in PROGRESS.md.
- Owner approves the final PRD/design.

No hour estimate or delivery date is assigned in this v0.1 plan. Estimate after review and after the SDK/recovery spikes reveal the true scope.
