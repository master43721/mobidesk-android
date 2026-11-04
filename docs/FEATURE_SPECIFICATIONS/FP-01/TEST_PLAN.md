# FP-01 Test Plan — Focused Pilot Acceptance

**Version:** 0.2 — OWNER CRITIQUE REVISION  
**Date:** 4 November 2026  
**Status:** Test cases are proposed; none is claimed to have been run in this revision.

## 1. Testing strategy

The v0.1 catalogue was too broad and assumed a staging project that is not evidenced. This version separates:
1. The four named existing baseline tests.
2. A compact must-run pilot suite for the defects and FP-01 MUST requirements.
3. Additional security/accessibility checks that remain release checks but are not counted as unit test cases.

Unit/ViewModel/UI tests use a fake Auth repository and must not require a staging Supabase project. Provider integration and real email tests require an Owner-approved isolated test environment.

## 2. Existing baseline regression suite — mandatory

These exact tests already exist in the repository:

| Baseline ID | Current test | Purpose |
|---|---|---|
| FP01-BASE-01 | AuthErrorMessagesTest.invalidCredentialsUseAStableSafeMessage | Existing invalid-credential message regression. |
| FP01-BASE-02 | AuthErrorMessagesTest.backendDetailsAreNotShownToTheUser | Raw backend details do not leak into user message. |
| FP01-BASE-03 | MobiDeskSmokeTest.signInScreenDoesNotOfferFakePreviewAccess | Sign-in remains visible; no fake preview/demo or signup flow. |
| FP01-BASE-04 | MobiDeskSmokeTest.passwordRecoveryRequestScreenOpensWithoutSendingAnEmail | Reset request route opens; this does not prove email delivery. |

The baseline command group is:
- app unit test task: :app:testDebugUnitTest
- Compose instrumentation task: :app:connectedDebugAndroidTest

A test run counts as baseline PASS only when all four named tests are present in the test report and pass. A Gradle task that was not run is NOT RUN, not PASS.

## 3. Must-run core pilot suite

| Test ID | Scenario | Expected result | Level |
|---|---|---|---|
| FP01-CORE-01 | First launch with no Auth session | Wait for initialization, then show sign-in; no flash. | ViewModel/UI |
| FP01-CORE-02 | Valid provisioned email/password | Authenticated SDK event produces FULLY_AUTHENTICATED. | Fake + isolated Auth integration |
| FP01-CORE-03 | Invalid email/password | Safe generic invalid-credential behavior; no account data exposed. | Unit + integration |
| FP01-CORE-04 | Session restore after process restart | Valid SDK session routes without premature sign-in UI. | Device |
| FP01-CORE-05 | Refresh failure/expired session | Route to SIGNED_OUT and clear protected UI. | Fake + integration |
| FP01-CORE-06 | Default sign-out | Verify LOCAL/current-session behavior and local state clear on success. | SDK contract/integration |
| FP01-CORE-07 | Sign-out network failure | Show honest error; explicit device-local clear exits Authenticated UI. | Fake + integration |
| FP01-CORE-08 | Recovery callback begins | Persist CALLBACK_PENDING before asynchronous SDK exchange; Authenticated event remains RECOVERY_ONLY. | Unit/ViewModel |
| FP01-CORE-09 | Recovery cancel/back | Sign out current recovery session, clear marker and do not load workspace. | UI/ViewModel |
| FP01-CORE-10 | Valid recovery callback | SDK PKCE exchange succeeds; marker becomes PASSWORD_UPDATE_REQUIRED before recovery form. | Isolated integration/device |
| FP01-CORE-11 | Activity recreation/re-delivery of same callback | Code is not exchanged a second time; no spurious error or workspace route. | Instrumentation/device |
| FP01-CORE-12 | Process death while PASSWORD_UPDATE_REQUIRED | Restore recovery form if SDK session remains valid. | Device |
| FP01-CORE-13 | Process death while CALLBACK_PENDING | Do not replay code or treat session as FULLY_AUTHENTICATED; clear partial recovery session and request a new link. | Device |
| FP01-CORE-14 | Password update success/failure | Success clears marker and transitions to FULLY_AUTHENTICATED; failure stays RECOVERY_ONLY. | Fake + isolated integration |
| FP01-CORE-15 | FP-02 handoff | Workspace repository is called only for FULLY_AUTHENTICATED, never RECOVERY_ONLY. | Unit |
| FP01-CORE-16 | Unexpected callback scheme/host/path | Callback ignored/rejected; no recovery session or workspace access. | Unit + instrumentation |
| FP01-CORE-17 | Duplicate Auth requests/taps | One in-flight operation; no duplicate reset/password update. | Unit/UI |
| FP01-CORE-18 | Auth repository injection | ViewModel test constructs with fake; no production Supabase client required. | Unit |
| FP01-CORE-19 | email_address_not_authorized | Show specific delivery-not-configured message, not generic failure. | Unit + staging |
| FP01-CORE-20 | over_email_send_rate_limit | Tell student to wait before requesting another recovery email. | Unit + staging |
| FP01-CORE-21 | over_request_rate_limit | Tell student to wait before making further Auth requests. | Unit + staging |
| FP01-CORE-22 | email_not_confirmed order | Compare valid and invalid password for an unconfirmed synthetic account; use exact deployed behavior for copy. | Isolated Auth integration |
| FP01-CORE-23 | Recovery request known vs unknown email | App acknowledgement is the same; provider response must not reveal whether a user exists. | Unit + staging |
| FP01-CORE-24 | Missing Supabase URL/publishable key | Auth actions disabled; no demo/fake bypass. | Unit/UI |

## 4. SMTP and provider integration gate

Student recovery is NOT ACCEPTED until all of the following are true:
1. Custom SMTP or Send Email Hook is configured for the project.
2. Sender domain/provider is verified and credentials are stored only in Supabase configuration.
3. A test message reaches a synthetic recipient who is not a member of the Supabase organization.
4. A recovery link uses the exact approved redirect URI.
5. Link opens the app on a supported physical Android device.
6. PKCE exchange completes, password update succeeds and new password can sign in.
7. Email send throttling and recipient authorization errors are mapped to actionable messages.

Supabase's built-in email provider is not sufficient for real student recovery in the documented hosted configuration. See SUPABASE_CONFIGURATION.md.

## 5. Measurable accessibility check

Manual device check with TalkBack enabled:
- 100% of actionable controls on sign-in/reset/recovery screens have non-empty, meaningful accessible names.
- Focus order follows the visual interaction order: email, password, visibility control, Sign in, Forgot password; analogous order for reset/recovery screens.
- Busy state and error/success message are announced once and remain available in the accessibility tree.
- Password visibility toggle announces Show password/Hide password.
- Font scaling at 1.3x and 1.5x does not hide the primary action or make recovery input impossible.
- Record device model/API, tester, steps and result.

## 6. Custom URI collision check

This is a controlled device experiment, not a unit test:
1. Install MobiDesk on a test device.
2. Install a small test app that declares the same custom scheme/host.
3. Trigger a synthetic recovery callback.
4. Record Android resolver/default handling.
5. Confirm MobiDesk accepts the session only after the Supabase SDK's PKCE verifier exchange succeeds.
6. Confirm the competing app cannot use the callback to obtain a session without the verifier.
7. Decide whether current custom URI is acceptable for pilot or verified HTTPS App Links are required.

Do not claim that custom URI scheme ownership is exclusive to MobiDesk.

## 7. Storage/backup and privacy checks

- Inspect resolved supabase-kt 3.6.0 session storage backend.
- Confirm app allowBackup=false in merged manifest.
- Confirm no email/password/refresh token/access token/PKCE code/full callback URL in Logcat, crash output or analytics.
- Confirm recovery marker stores stage/time/fingerprint only, not raw callback code or URL.
- Confirm no password in SavedState, preferences or database.
- Record storage-encryption finding separately; do not mark encrypted at rest without evidence.

## 8. Release gate

FP-01 pilot pass requires:
- Four baseline tests pass.
- All must-run core tests applicable to the selected test environment pass.
- Isolated Auth integration tests pass for valid/invalid login, session restore, sign-out and confirmation behavior.
- SMTP email delivery and full recovery callback succeed.
- Physical-device callback recreation/process-death cases pass.
- TalkBack and URI collision checks are reviewed.
- No critical credential/session disclosure.
- Source commit and test report are recorded in PROGRESS.md.

Not-run provider/device tests remain BLOCKED/NOT RUN; they cannot be waived by a green unit-test task.
