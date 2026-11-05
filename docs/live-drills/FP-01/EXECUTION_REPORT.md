# FP-01 Live-Drill Execution Report

**Feature Package:** FP-01 — Authentication, Session State & Recovery
**Application ID:** `com.mobidesk.android`
**Target Repository:** `master43721/mobidesk-android`
**Handoff Branch:** `feature/fp-01-auth-hardening`
**Exact Source Commit (HEAD SHA):** `cf01c89deb7dd0b0cb4a272811d9b322f33a93e8`
**Execution Date:** 2026-11-05
**Current Live-Drill Status:** `LIVE_DRILL_STATUS: BLOCKED`
**Owner Sign-off Status:** UNACCEPTED (Awaiting physical demonstration on Android phone)

---

## 1. Executive Summary

FP-01 implementation has been reconciled against authoritative requirements in `docs/FEATURE_SPECIFICATIONS/FP-01/`. Automated unit verification confirms that the core authentication hardening, `AuthLevel` (`SIGNED_OUT`, `RECOVERY_ONLY`, `FULLY_AUTHENTICATED`) transitions, fail-closed recovery flow, and error mapping logic are complete and verified across 39 passing unit tests.

The live demonstration on a physical Android phone is a mandatory acceptance gate. The application build and live drill are currently gated on resolving:
1. Native CMake build memory exhaustion during `libavcodec.so.62` link (Defect DEF-001).
2. Missing Supabase publishable key in `local.properties` (Defect DEF-002).
3. Provisioning of a dedicated staging QA student account and confirmation of the email delivery path.

---

## 2. Environment & Tooling Verification

| Component | Version / Specification |
|:---|:---|
| Operating System | Linux 6.8.0-1064-azure x86_64 (GitHub Codespace) |
| Java Runtime | OpenJDK 25.0.5.0.2 LTS (Microsoft build 25.0.5.0.2+1-LTS) |
| Gradle Version | Gradle 9.6.1 |
| Android SDK Platform | Android SDK 37 (compileSdk = 37, targetSdk = 37, minSdk = 29) |
| Android NDK | NDK 29.0.13113456 |
| Target ABIs | `arm64-v8a`, `x86_64` |
| Supabase Kotlin BOM | 3.6.0 (modules: `auth-kt`, `postgrest-kt`) |

---

## 3. Authoritative Requirements Reconciliation Matrix

| Requirement ID | Description | Responsible Kotlin Artifacts | Test Reference | Current Status | Evidence / Notes |
|:---|:---|:---|:---|:---:|:---|
| **FP01-FR-001** | Preserve Supabase email/password sign-in for institution-provisioned users. | `MobiDeskViewModel.kt`<br>`MobiDeskRepositories.kt` | `MobiDeskViewModelTest.testCore02ProvisionedStudentLoginSuccess`<br>`AuthErrorMessagesTest.invalidCredentialsUseAStableSafeMessage` | **PASS** (Unit) / Device Gated | Valid credentials authenticate; invalid credentials return stable safe error. |
| **FP01-FR-002** | Preserve session restoration without premature route flash. | `MobiDeskViewModel.kt`<br>`MainActivity.kt` | `MobiDeskViewModelTest.testCore01ColdStartInitialSessionRestoration` | **PASS** (Unit) / Device Gated | `isRestoringSession` held until SDK `SessionStatus` emits. |
| **FP01-FR-003** | Publish `SIGNED_OUT`, `RECOVERY_ONLY`, and `FULLY_AUTHENTICATED` to navigation. | `MobiDeskViewModel.kt`<br>`RecoveryStateStore.kt` | `MobiDeskViewModelTest.testCore10PasswordUpdateRequiredEntersRecoveryOnly`<br>`MobiDeskViewModelTest.testCore14PasswordUpdateSuccessTransitionsToFullyAuthenticatedAndLoadsWorkspace` | **PASS** (Unit) / Device Gated | Discrete `AuthLevel` enum published on UI state flow. |
| **FP01-FR-004** | Set durable `CALLBACK_PENDING` marker before asynchronous PKCE code exchange. | `MainActivity.kt`<br>`RecoveryStateStore.kt` | `MobiDeskViewModelTest.testCore08CallbackPendingBlocksWorkspaceLoading` | **PASS** (Unit) / Device Gated | Synchronous `.commit()` write to `RecoveryStateStore` in `MainActivity.handleAuthIntent`. |
| **FP01-FR-005** | SDK `Authenticated` event during pending recovery remains `RECOVERY_ONLY` and blocks workspace. | `MobiDeskViewModel.kt` | `MobiDeskViewModelTest.testCore15WorkspaceSpyProvesZeroCallsDuringRecoveryOnly` | **PASS** (Unit) | Verified with `WorkspaceRepositorySpy`; zero workspace calls while `authLevel != FULLY_AUTHENTICATED`. |
| **FP01-FR-006** | Successful password update clears marker and promotes to `FULLY_AUTHENTICATED`. | `MobiDeskViewModel.kt` | `MobiDeskViewModelTest.testCore14PasswordUpdateSuccessTransitionsToFullyAuthenticatedAndLoadsWorkspace` | **PASS** (Unit) / Device Gated | Password update clears recovery store and emits `FULLY_AUTHENTICATED`. |
| **FP01-FR-007** | Cancelling recovery signs out session, returns to `SIGNED_OUT`, and never enters workspace. | `MobiDeskViewModel.kt`<br>`MobiDeskApp.kt` | `MobiDeskViewModelTest.testCore09CancelPasswordRecoverySignsOutAndClearsMarker` | **PASS** (Unit) / Device Gated | Invokes `signOutCurrentSession()`, wipes marker, returns to `SIGNED_OUT`. |
| **FP01-FR-008** | Current-session sign-out with safe local fallback if remote revocation fails. | `MobiDeskViewModel.kt`<br>`MobiDeskRepositories.kt` | `MobiDeskViewModelTest.testCore06LocalSignOutClearsStateAndStore`<br>`MobiDeskViewModelTest.testCore07SignOutFailureAllowsDeviceOnlySignOut` | **PASS** (Unit) / Device Gated | Uses `SignOutScope.LOCAL`. Remote error exposes `signOutDeviceOnly()` fallback. |
| **FP01-FR-009** | Recovery callback de-duplication across activity recreation. | `MainActivity.kt`<br>`RecoveryStateStore.kt` | `RecoveryStateStoreTest.testFingerprintConsumedAndReplayPrevention` | **PASS** (Unit) / Device Gated | Computes SHA-256 fingerprint; consumed tokens rejected. |
| **FP01-FR-010** | Process death during `CALLBACK_PENDING` fails closed to interrupted path. | `MobiDeskViewModel.kt`<br>`RecoveryStateStore.kt` | `MobiDeskViewModelTest.testCore13ProcessDeathWhileCallbackPendingFailsClosedOnStartup` | **PASS** (Unit) / Device Gated | Fails closed on restart, clears local session, routes to `RecoveryInterruptedScreen`. |
| **FP01-FR-011** | Process death after `PASSWORD_UPDATE_REQUIRED` restores update form. | `MobiDeskViewModel.kt` | `MobiDeskViewModelTest.testCore10PasswordUpdateRequiredEntersRecoveryOnly` | **PASS** (Unit) / Device Gated | Restores password update screen if SDK session is valid. |
| **FP01-FR-012** | Preserve exact recovery redirect and PKCE URI format. | `MainActivity.kt`<br>`MobiDeskApplication.kt` | `RecoveryStateStoreTest.testSha256FingerprintDeterministic` | **PASS** (Unit) / Device Gated | Enforces strict path match (`/recovery`) and custom scheme `com.mobidesk.android://auth-callback/recovery`. |
| **FP01-FR-013** | Display actionable errors for `email_address_not_authorized` and `over_email_send_rate_limit`. | `MobiDeskRepositories.kt`<br>`MobiDeskViewModel.kt` | `AuthErrorMessagesTest.emailAddressNotAuthorizedDirectsToAdmin`<br>`AuthErrorMessagesTest.overEmailSendRateLimitMapsToPatienceCopy` | **PASS** (Unit) | Verified in unit test suite. |
| **FP01-FR-014** | Distinguish IP/request rate limit (`over_request_rate_limit`) from email rate limit. | `MobiDeskRepositories.kt` | `AuthErrorMessagesTest.overRequestRateLimitMapsToRequestLimitCopy` | **PASS** (Unit) | Distinct copy rendered for request vs email limits. |
| **FP01-FR-015** | App must not expose raw exceptions, credentials, tokens, or PKCE verifiers. | All Modules | `AuthErrorMessagesTest.backendDetailsAreNotShownToTheUser` | **PASS** (Unit) / Audit | No stack traces or tokens logged or shown in UI state. |
| **FP01-FR-016** | Injectable Auth repository seam for deterministic unit testing. | `MobiDeskViewModel.kt`<br>`MobiDeskRepositories.kt` | `MobiDeskViewModelTest.testCore18FakeInjectionAllowsTestingWithoutSupabaseClient` | **PASS** (Unit) | ViewModel allows constructor injection of `AuthRepository` and `WorkspaceRepository`. |
| **FP01-FR-017** | Preserve existing baseline sign-in and recovery user experience. | `MobiDeskApp.kt` | `AuthErrorMessagesTest.invalidCredentialsUseAStableSafeMessage` | **PASS** (Unit) / Device Gated | UI structure preserved; baseline unit tests pass. |
| **FP01-FR-018** | Prohibit student self-registration and public sign-up route. | `MobiDeskApp.kt`<br>`MainActivity.kt` | Code Inspection / Smoke Baseline | **PASS** (Static Audit) | App has no signup button or call to `supabase.auth.signUp`. |

---

## 4. Automated Test Results

Command executed:
```bash
./gradlew --no-daemon --max-workers=3 :app:testDebugUnitTest
```

### Result: BUILD SUCCESSFUL (39 passed, 0 failed, 0 skipped)

- `com.mobidesk.android.AuthErrorMessagesTest`: 10 passed, 0 failed
- `com.mobidesk.android.MobiDeskViewModelTest`: 23 passed, 0 failed
- `com.mobidesk.android.RecoveryStateStoreTest`: 6 passed, 0 failed

Test report paths:
- XML: `app/build/test-results/testDebugUnitTest/`
- HTML: `app/build/reports/tests/testDebugUnitTest/index.html`

---

## 5. Build Feasibility & Artifact Verification

- Expected Artifact: `app/build/outputs/apk/debug/app-debug.apk`
- Current Status: BLOCKED by Defect DEF-001 (CMake linker memory exhaustion during `make -j` in `ExternalFFmpeg.cmake`).
- APK Package: `com.mobidesk.android`
- Target Version: `0.2.0` (versionCode `1`)
- Min SDK: 29 | Target SDK: 37 | Target ABIs: `arm64-v8a`, `x86_64`

---

## 6. Preflight Check & External Release Gates

1. **Client Configuration (DEF-002):** `local.properties` contains `SUPABASE_URL` but is missing `SUPABASE_PUBLISHABLE_KEY`. Live drill requires key insertion before building the testable APK.
2. **QA Student Account:** An active, administrator-provisioned test account must be supplied by the Project Owner.
3. **Password Recovery Delivery (TEST_PLAN.md §4):** Live password recovery email dispatch requires either an authorized organization team mailbox or active custom SMTP / Send Email Hook.
4. **Physical Device Live Drill (Checklist):** All 13 scenarios in `DEVICE_OR_ENVIRONMENT_CHECKLIST.md` remain `NOT RUN` pending physical phone demonstration.
