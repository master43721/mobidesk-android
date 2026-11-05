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
- IMPLEMENTATION_PLAN.md
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


## 8. Implementation Evidence — 5 November 2026

**Base SHA:** `60b3152c84ec13a0f43ec59a983e968c0b5b3ff7`
**Head SHA:** `34b8f0012209d7c2a745435b943198aa8229203b`
**Implementation status:** COMPLETE — Narrow Hardening & Unit Pilot Suite Verified

### Modified / Created Files
1. `app/src/main/java/com/mobidesk/android/data/RecoveryStateStore.kt` (CREATED): Defines `AuthLevel` (`SIGNED_OUT`, `RECOVERY_ONLY`, `FULLY_AUTHENTICATED`), `RecoveryStage` (`NONE`, `CALLBACK_PENDING`, `PASSWORD_UPDATE_REQUIRED`, `INTERRUPTED`), `RecoveryStateStore` interface, and `SharedPreferencesRecoveryStateStore` implementing synchronous `.commit()`, bounded timestamp retention, and SHA-256 fingerprinting.
2. `app/src/main/java/com/mobidesk/android/data/MobiDeskRepositories.kt` (MODIFIED): Added `AuthRepository` and `WorkspaceRepository` interfaces; `SupabaseAuthRepository` implements `AuthRepository` with `SignOutScope.LOCAL` and `clearLocalSession()`; updated `AuthErrorMessages.safeMessage` with typed error code mapping and safe network/exception fallback without backend detail leakage.
3. `app/src/main/java/com/mobidesk/android/MobiDeskApplication.kt` (MODIFIED): Exposed `recoveryStateStore` lazy singleton.
4. `app/src/main/java/com/mobidesk/android/MainActivity.kt` (MODIFIED): Enforced exact path matching (`/recovery`), SHA-256 callback fingerprinting, replay defense, synchronous `CALLBACK_PENDING` write before calling `handleDeeplinks`, `PASSWORD_UPDATE_REQUIRED` on success, `INTERRUPTED` on error, and intent data neutralization.
5. `app/src/main/java/com/mobidesk/android/MobiDeskViewModel.kt` (MODIFIED): Added constructor injection test seams for `AuthRepository`, `WorkspaceRepository`, and `RecoveryStateStore`; implemented hard invariant gating workspace loading strictly on `FULLY_AUTHENTICATED`; implemented fail-closed interrupted recovery handling; added `cancelPasswordRecovery()` and `signOutDeviceOnly()`.
6. `app/src/main/java/com/mobidesk/android/ui/MobiDeskApp.kt` (MODIFIED): Added `RecoveryInterruptedScreen` composable, wired BackHandler to cancel recovery and reset routes, added device-only sign-out fallback button in `DashboardScreen` and `SettingsScreen`.
7. `app/src/test/java/com/mobidesk/android/AuthErrorMessagesTest.kt` (MODIFIED): Retained exact baseline tests FP01-BASE-01 and FP01-BASE-02; added tests for typed error codes, rate limits, expired flow state, and network failures (10 total tests).
8. `app/src/test/java/com/mobidesk/android/MobiDeskViewModelTest.kt` (CREATED): Full unit test suite covering FP01-CORE-01..24 scenarios using `FakeAuthRepository`, `WorkspaceRepositorySpy`, and `FakeRecoveryStateStore` (23 total tests).
9. `app/src/test/java/com/mobidesk/android/RecoveryStateStoreTest.kt` (CREATED): Unit tests verifying `SharedPreferencesRecoveryStateStore` marker persistence, SHA-256 fingerprinting, replay prevention, FIFO pruning, and time-bounded expiration (6 total tests). Total unit test suite: 39 passed, 0 failed.

### Verification Outcomes

#### Baseline Test Suite (TEST_PLAN.md § 2)
| ID | Test | Status | Evidence |
|---|---|---|---|
| FP01-BASE-01 | `AuthErrorMessagesTest.invalidCredentialsUseAStableSafeMessage` | PASS | Verified in test runner (6ms) |
| FP01-BASE-02 | `AuthErrorMessagesTest.backendDetailsAreNotShownToTheUser` | PASS | Verified in test runner (1ms) |
| FP01-BASE-03 | `MobiDeskSmokeTest.signInScreenDoesNotOfferFakePreviewAccess` | BLOCKED | Requires connected Android device/emulator (not available in container); verified by source inspection that no demo/signup affordance exists. |
| FP01-BASE-04 | `MobiDeskSmokeTest.passwordRecoveryRequestScreenOpensWithoutSendingAnEmail` | BLOCKED | Requires connected Android device/emulator. |

#### Must-Run Core Pilot Suite (TEST_PLAN.md § 3)
| Test ID | Scenario | Status | Result / Notes |
|---|---|---|---|
| FP01-CORE-01 | Cold start without session | PASS | Restoring session flag active during Initializing, routes to SIGNED_OUT. |
| FP01-CORE-02 | Provisioned student login | PASS | Authenticated SDK event produces FULLY_AUTHENTICATED; workspace loads. |
| FP01-CORE-03 | Invalid credentials | PASS | Stable safe message "The email or password is incorrect." |
| FP01-CORE-04 | Session restore on restart | PASS / DEVICE GATED | Unit logic verified; physical device restart gated on hardware. |
| FP01-CORE-05 | Refresh failure / expired session | PASS | Clears recovery store, routes to SIGNED_OUT with expiration copy. |
| FP01-CORE-06 | Sign-out current session | PASS | SignOutScope.LOCAL used; clears state to SIGNED_OUT and wipes recovery store. |
| FP01-CORE-07 | Sign-out network failure | PASS | Shows honest error; signOutDeviceOnly() clears session locally with non-revocation disclaimer. |
| FP01-CORE-08 | Recovery callback begins | PASS | Marker CALLBACK_PENDING set; workspace spy proves 0 calls while RECOVERY_ONLY. |
| FP01-CORE-09 | Recovery cancel/back | PASS | Calls signOutCurrentSession, clears marker, 0 workspace calls. |
| FP01-CORE-10 | Recovery PKCE success | PASS | PASSWORD_UPDATE_REQUIRED sets RECOVERY_ONLY and displays recovery form; 0 workspace calls. |
| FP01-CORE-11 | Callback re-delivery / replay | PASS / DEVICE GATED | In-memory/durable fingerprint checks verify replay prevention; physical device gated. |
| FP01-CORE-12 | Process death in PASSWORD_UPDATE_REQUIRED | PASS / DEVICE GATED | Restores form if SDK session valid; clears stale state if NotAuthenticated. |
| FP01-CORE-13 | Process death in CALLBACK_PENDING | PASS | INTERRUPTED stage fails closed, clears local session, shows recovery interrupted screen. |
| FP01-CORE-14 | Password update success | PASS | Clears marker, publishes FULLY_AUTHENTICATED, loads workspace. |
| FP01-CORE-15 | FP-02 handoff gating | PASS | WorkspaceRepositorySpy confirms 0 calls across all RECOVERY_ONLY stages and validation errors. |
| FP01-CORE-16 | Unexpected callback URI | PASS | Exact match on scheme, host, and path in MainActivity; rejects unexpected URIs. |
| FP01-CORE-17 | Duplicate in-flight requests | PASS | Busy guard ignores duplicate submissions while isBusy == true. |
| FP01-CORE-18 | Auth repository injection | PASS | MobiDeskViewModel operates with fakes without Supabase client or Android runtime. |
| FP01-CORE-19 | `email_address_not_authorized` | PASS | Maps to actionable copy directing student to contact institution administrator. |
| FP01-CORE-20 | `over_email_send_rate_limit` | PASS | Maps to "Too many recovery emails have been sent. Please wait before requesting another." |
| FP01-CORE-21 | `over_request_rate_limit` | PASS | Maps to "Too many sign-in requests have been made. Please wait before retrying." |
| FP01-CORE-22 | `email_not_confirmed` | PASS | Maps to "Confirm your school email before signing in." |
| FP01-CORE-23 | Reset known vs unknown email | PASS | Produces identical generic acknowledgement preventing account enumeration. |
| FP01-CORE-24 | Missing Supabase configuration | PASS | Disables auth actions, displays safe configuration error. |

#### External Release Gates
- **Custom SMTP Delivery (TEST_PLAN.md § 4):** BLOCKED / EXTERNAL RELEASE GATE. Supabase custom SMTP or Send Email Hook must be configured with an external provider before real email delivery to non-organization student recipients can be verified.
- **Physical Device & Accessibility Testing (TEST_PLAN.md § 5, 6):** BLOCKED / EXTERNAL RELEASE GATE. Requires physical Android device with TalkBack enabled and URI collision test application.


## 9. Senior Review Resolution & Gradle Verification — 5 November 2026

Following the Independent Senior Review, all requested P1–P3 code and build changes have been implemented and verified:

### 1. Standard Gradle Build & Test Pipeline Restored (P1 Resolution)
- Removed direct `@aar` classifier issue on `sqlcipher-android` in `gradle/freerdpcore.gradle`.
- Added `testOptions { unitTests.returnDefaultValues = true }` and `kotlinx-coroutines-test` in `app/build.gradle`.
- Verified standard Gradle execution: `./gradlew :app:testDebugUnitTest` executed and passed cleanly:
  - `AuthErrorMessagesTest`: 10 passed, 0 failed (100% success rate)
  - `MobiDeskViewModelTest`: 23 passed, 0 failed (100% success rate)
  - `RecoveryStateStoreTest`: 6 passed, 0 failed (100% success rate)
  - **Total: 39 passed, 0 failed, 39 total.**
  - Official HTML report generated at `app/build/reports/tests/testDebugUnitTest/index.html`.

### 2. Session Storage Encryption at Rest Implemented (P2 Resolution)
- Added `androidx.security:security-crypto:1.1.0` to `app/build.gradle`.
- Implemented `createEncryptedSessionManager()` in `MobiDeskApplication.kt` using `androidx.security.crypto.EncryptedSharedPreferences` and `MasterKey` (AES256_GCM key scheme + AES256_SIV key encryption + AES256_GCM value encryption), wrapped in `SharedPreferencesSettings` for Supabase `SettingsSessionManager`.
- Fulfills FP01-CFG-08 and PRD §9 item 8: session tokens and refresh tokens are encrypted at rest with hardware Keystore-backed encryption.

### 3. Repository Hygiene & Test Harness Clean Up (P3 Resolution)
- Completely removed ad-hoc `test-runner/` harness and associated shims.
- All 39 unit tests are housed in standard Gradle source directories (`app/src/test/java/com/mobidesk/android/`) and run directly through the standard `./gradlew :app:testDebugUnitTest` command.
- Cleaned `.gitignore` to maintain proper repository hygiene.


## 10. Mandatory Live-Drill Gate Status & Reconciliation — 5 November 2026

Per `docs/MANDATORY_LIVE_DRILL_POLICY.md`, automated unit tests and senior code reviews are necessary prerequisites, but do not complete FP-01. Physical demonstration on an Android phone is a mandatory, blocking acceptance gate.

- **Current `LIVE_DRILL_STATUS`:** `BLOCKED`
- **Owner Acceptance Status:** `UNACCEPTED` (Human physical demonstration pending)
- **Active Blockers Documented:**
  1. **DEF-001:** FreeRDP ExternalFFmpeg CMake linker memory exhaustion during `make -j` link of `libavcodec.so.62` on container with ~8 GB RAM and 0 swap. Proposed resolution: bound compilation parallelism or configure swap. Awaiting Owner approval.
  2. **DEF-002:** `local.properties` contains `SUPABASE_URL` but is missing `SUPABASE_PUBLISHABLE_KEY`. Live drill requires key insertion before building the testable APK.
  3. **QA Account:** Provisioning of dedicated staging QA student account and confirmation of email delivery path.
- **Scenarios Catalogued:** 13 physical scenarios defined in `docs/live-drills/FP-01/DEVICE_OR_ENVIRONMENT_CHECKLIST.md` (all currently `NOT RUN` pending APK build and test account).
- **Evidence Directory:** `docs/live-drills/FP-01/` (`EXECUTION_REPORT.md`, `DEVICE_OR_ENVIRONMENT_CHECKLIST.md`, `DEFECTS.md`).
- **Next Required Actions:**
  1. Owner approves CMake build parameter adjustment to resolve DEF-001.
  2. Configure Supabase publishable key in `local.properties` (DEF-002).
  3. Build debug APK and record artifact SHA-256.
  4. Transition `LIVE_DRILL_STATUS` to `READY FOR OWNER`.
  5. Send explicit live-drill handoff request to Project Owner and STOP.
  6. Project Owner performs live demonstration on physical Android phone.
  7. Audit evidence and record written Owner acceptance.
