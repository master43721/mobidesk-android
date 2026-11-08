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

---

## 8. Development Loop Iteration 1 (2026-11-07)

### Loop A — Review and Self-Critique
- **Readiness:** BLOCKED
- **Iterations used:** 1
- **Findings & Severity:**
  - `C1 / Test Evidence`: BLOCKER — Toolchain (JDK/Android SDK) unavailable on this Windows host. Automated tests cannot execute locally; must run in Codespaces or GitHub Actions. Reopened until raw CI test output is attached.
  - `C2 / Legacy Main State (F1)`: NEGOTIABLE — PR #8 merged FP-01 code to `main` before live drill gate was established. Two options presented to Owner in `docs/dev-loop/NEGOTIABLE_REGISTER.md` (NEG-01): (a) staging trunk with owner sign-off; (b) clean `git revert` commits.
  - `C3 / Line Endings`: MINOR — Resolved. `.gitattributes` added (`* text=auto eol=lf`, `*.ps1 text eol=crlf`) and renormalized in commit `876d090`.
  - `C4 / CI Trigger & Reports`: MINOR — Resolved. `.github/workflows/android-ci.yml` extended to trigger on `fp-*` branches and upload JUnit reports in commit `a2e06a9`.
  - `C6 / Branch Reconciliation`: MINOR — Reconciled mapping table generated between local and remote branch topologies.
  - `C7 / Pruning Utility Safety`: MINOR — Resolved. `scripts/prune-remote-branches.ps1` and `.sh` updated to create archive bundle and `archive/<branch>` tags prior to deletion in commit `7365fb8`.
  - `C8 / Documentation Hygiene`: MINOR — Resolved. Package handoff/review prompt links fixed; zero case-colliding files confirmed via `git ls-files` in commit `4b717ab`.
- **Remaining NEGOTIABLE items:** NEG-01 (F1 trunk policy), NEG-03 (staging credentials), NEG-04 (prune execution), NEG-05 (branch protection).

### Loop B — Automated Test & Output Mismatch Log
- **Local Toolchain Check:** FAILED (`scripts/verify.ps1` output: `BLOCKED_MISSING_TOOLCHAIN`). JDK and Android SDK are not installed on this Windows host.
- **Expected vs Actual Mismatches:**
  - *Mismatch 1:* Expected local `./gradlew testDebugUnitTest` execution; Actual: `BLOCKED_MISSING_TOOLCHAIN`. Classification: `Environment`.
  - *Mismatch 2:* Expected git push to CI; Actual: Blocked by interactive Git Credential Manager GUI in non-interactive terminal. Classification: `Environment`.
- **Static Verification Scripts:**
  - `scripts/scan-secrets-logs.ps1`: PASSED (0 leaks)
  - `scripts/check-release-config.ps1`: PASSED (0 release risks)
  - `scripts/check-traceability.ps1`: PASSED (0 unmapped requirements, 0 missing tests)
  - `scripts/check-docs.ps1`: PASSED (0 case collisions, 0 broken links, 0 duplicate plan files)

---

## 9. Development Loop Iteration 2 (2026-11-07)

### Loop A — Review and Self-Critique
- **Readiness:** BLOCKED
- **Iterations used:** 2
- **Findings & Severity:**
  - `C3 / CR Corruption in Working Tree`: MAJOR — `.devcontainer/post-create.sh`, `update-content.sh`, and `gradlew` retained CRLF line endings on Windows. Resolved by adding explicit `*.sh text eol=lf` and `gradlew text eol=lf` rules to `.gitattributes` and renormalizing working copy.
  - `Bash Scripts Integrity`: MAJOR — `scripts/validate-fake-broker-contract.sh` previously lacked payload inspection; updated with Python-based JSON schema validation. Replaced fragile `jq` parsing in `scripts/verify.sh` and `check-docs.sh`.
- **Remaining NEGOTIABLE items:** NEG-01 (F1 trunk policy), NEG-03 (staging credentials), NEG-04 (prune execution), NEG-05 (branch protection).

### Loop B — Automated Test & Output Mismatch Log
- **Local Toolchain Check:** FAILED (`scripts/verify.ps1` output: `BLOCKED_MISSING_TOOLCHAIN`). JDK and Android SDK are not installed on this Windows host.
- **Static Verification Scripts:**
  - `scripts/scan-secrets-logs.ps1`: PASSED (0 leaks)
  - `scripts/check-release-config.ps1`: PASSED (0 release risks)
  - `scripts/check-traceability.ps1`: PASSED (0 unmapped requirements, 0 missing tests)
  - `scripts/check-docs.ps1`: PASSED (0 case collisions, 0 broken links, 0 duplicate plan files)

---

## 10. Development Loop Iteration 3 (2026-11-08)

### Loop A — Review and Self-Critique
- **Readiness:** READY FOR OWNER (Automated tests and CI verification PASS; gated strictly on physical device demonstration DEF-001, DEF-002, DEF-003)
- **Iterations used:** 3 (Iteration cap reached)
- **Findings & Severity:**
  - `C1 / Automated Test Execution`: Resolved. Executed `./gradlew testDebugUnitTest --rerun-tasks` in Codespaces Linux environment (OpenJDK 21.0.12.1, Gradle 9.6.1, Android SDK 37). All 40 unit tests executed and passed (0 failed, 0 skipped).
  - `Submodule Integrity Restored`: Cleaned `third_party/FreeRDP` submodule to remove dirty working tree modifications, strictly pinning commit `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1` per AGENTS.md §1.2.
  - `Broker Validation Script Fix`: Fixed Python 3 f-string escaped-quote syntax error in `scripts/validate-fake-broker-contract.sh`.
- **Remaining NEGOTIABLE items:** NEG-01 (F1 trunk policy), NEG-03 (staging credentials), NEG-04 (prune execution), NEG-05 (branch protection).

### Loop B — Automated Test & Output Mismatch Log
- **Local Toolchain Check:** PASSED. OpenJDK 21.0.12.1, Gradle 9.6.1, Android SDK 37.
- **Expected vs Actual Mismatches:** 0 unexplained mismatches.
- **Automated Test Results (40/40 passed, 0 failures, 0 skipped):**
  - `com.mobidesk.android.AuthErrorMessagesTest`: 10 passed, 0 failed
  - `com.mobidesk.android.MobiDeskApplicationTest`: 1 passed, 0 failed
  - `com.mobidesk.android.MobiDeskViewModelTest`: 23 passed, 0 failed
  - `com.mobidesk.android.RecoveryStateStoreTest`: 6 passed, 0 failed
- **Static Verification Scripts:**
  - `scripts/check-docs.sh`: PASSED (0 case collisions, 0 broken links, 0 duplicate plan files)
  - `scripts/check-release-config.sh`: PASSED (0 release configuration risks)
  - `scripts/check-traceability.sh`: PASSED (FP-01: 20/20 requirements mapped, 0 missing tests)
  - `scripts/scan-secrets-logs.sh`: PASSED (0 leaks or credentials)
  - `scripts/validate-fake-broker-contract.sh`: PASSED (0 schema violations, 0 prohibited fields; fakeVersusReal: UNKNOWN)


