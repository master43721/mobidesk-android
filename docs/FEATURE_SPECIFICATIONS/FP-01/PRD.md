# FP-01 Product Requirements Document — Student Authentication and Account Recovery

**Version:** 0.2 — OWNER CRITIQUE REVISION  
**Date:** 4 November 2026  
**Parent feature:** FP-01  
**Repository:** master43721/mobidesk-android  
**Normative parent:** MobiDesk PRD v1.2  
**Status:** OWNER-DIRECTED IMPLEMENTATION HANDOFF — implement the scoped requirements and selected defaults; production/release acceptance remains gated by external configuration and unresolved policy decisions.

## 1. Executive summary

FP-01 preserves and safely completes the existing MobiDesk student authentication experience: email/password sign-in, Auth-session restoration, sign-out, password-recovery email request and password update through a PKCE callback.

This is a **targeted hardening/refactoring feature**, not a replacement authentication architecture. Existing screens, Supabase Auth SDK integration and successful flows should be retained unless a test or security defect justifies changing them.

FP-01 proves **authentication identity**. It does not prove the user is an institution-approved student or authorized for a VM. FP-02/FP-03 own profile and assignment authorization. The handoff now carries an explicit AuthLevel so a password-recovery session cannot be mistaken for a normal fully authenticated application session.

## 2. Problem statement

The current app already contains the main Auth happy paths. The problem is that the current implementation does not maintain a distinct recovery-only authentication level. Supabase's PKCE recovery callback establishes an ordinary Authenticated SDK session before the current success callback switches the UI into password-recovery mode. The current ViewModel may therefore load the student workspace during recovery. Cancelling recovery only hides the form and leaves that authenticated session active. Recovery state is held only in ViewModel memory, and callback re-delivery after Activity recreation may exchange a consumed code again.

The current sign-out error path also leaves the authenticated UI active when the remote sign-out call fails. Finally, the historical project audit reported that custom SMTP was disabled, while Supabase's built-in email service is restricted to team-member addresses and a very low project-wide sending rate. Password recovery cannot be treated as operational for ordinary students until a production-capable email sender is configured and tested.

## 3. Desired product outcomes

A provisioned student can:
1. Sign in using their institution email and password.
2. Return to MobiDesk and restore a valid session without seeing a sign-in/dashboard flash.
3. Sign out of the current Supabase session.
4. Request password recovery using their institution email.
5. Open a valid recovery link and reach a recovery-only state.
6. Set a new password and then transition to a fully authenticated application state.
7. Cancel or abandon recovery without entering the student workspace.
8. Resume a valid recovery form after process death, or safely restart recovery if callback processing was interrupted.
9. Receive an actionable message for SMTP authorization failures and rate limiting.

## 4. Parent PRD traceability

The parent PRD v1.2 requirements are applied as follows:

| Parent requirement | Parent wording supplied from v1.2 | FP-01 disposition |
|---|---|---|
| FR-AUTH-01 | Preserve existing sign-in, session restoration, password recovery and sign-out behaviour. | **Direct FP-01 requirement.** FP-01 preserves the current paths and fixes demonstrated recovery/sign-out defects without introducing a new Auth architecture. |
| FR-AUTH-02 | All profile and assignment queries use the user token and RLS. | **Not an FP-01 requirement.** This is owned by FP-02/FP-03. FP-01 must hand off a verified Auth state and must not perform profile/assignment authorization itself. |

FR-AUTH-02 must not be mapped to password recovery. FP-01 may include an integration test proving that the Auth layer hands off identity without performing a profile/assignment query, but the RLS query requirements remain in FP-02/FP-03.

## 5. Scope

### Included
- Institution email/password sign-in.
- Initial Auth session restoration.
- Auth state transitions and refresh failures.
- Current-session sign-out and a safe local sign-out fallback.
- Forgot-password request.
- PKCE recovery callback handling.
- A durable recovery-state marker and recovery-only AuthLevel.
- Successful password update and transition to full authentication.
- Recovery cancellation/Back behavior that does not leave the student signed in.
- Callback de-duplication across Activity recreation/process restart.
- Safe handling of Supabase Auth errors, including email authorization and rate limits.
- A narrow Auth repository interface so tests can inject a fake.
- Supabase project email-delivery readiness requirements.
- Focused unit, UI, integration and device acceptance tests.

### Excluded
- Student account creation or signup UI.
- Student self-registration or direct Auth user creation from the Android client.
- College Administrator roster provisioning and invitation dispatch (owned by the Administrator/account-provisioning feature track). FP-01 must authenticate already-provisioned accounts and must not implement the invitation-management workflow.
- Institution roster provisioning or administrator account management.
- Student profile, college and VM assignment queries.
- RDP/Windows guest credentials, session broker and remote desktop.
- MFA, passkeys, social sign-in or a second identity provider for the R1 pilot unless institution policy mandates one.
- Replacing the Supabase SDK or introducing an AuthSessionCoordinator/AuthDeepLinkRouter architecture.
- A new navigation/screen redesign; retain the existing Compose sign-in and recovery screens unless needed to fix a specific issue.

## 6. Authentication levels and FP-02 handoff

The app must distinguish these levels:

| AuthLevel | Meaning | May enter FP-02 workspace? |
|---|---|---|
| SIGNED_OUT | No usable Supabase session | No |
| RECOVERY_ONLY | Supabase session was established by password recovery and the password update has not completed | **No** |
| FULLY_AUTHENTICATED | Normal sign-in or successfully completed recovery | Yes, subject to FP-02 profile checks |

A Supabase SessionStatus.Authenticated event alone is insufficient to determine FULLY_AUTHENTICATED because the PKCE recovery flow imports a normal SDK session. The app must combine SDK session state with its own durable recovery marker.

FP-02 may begin profile/assignment checks only after FP-01 publishes FULLY_AUTHENTICATED. RECOVERY_ONLY must never trigger workspace/profile loading.

## 7. Functional requirements

| ID | Requirement | Priority | Acceptance evidence |
|---|---|---:|---|
| FP01-FR-001 | Preserve Supabase email/password sign-in for institution-provisioned users. | MUST | Existing baseline and sign-in integration tests pass. |
| FP01-FR-002 | Preserve session restoration and prevent premature signed-out/signed-in route flashes. | MUST | Cold-start and process-recreation tests. |
| FP01-FR-003 | Publish SIGNED_OUT, RECOVERY_ONLY and FULLY_AUTHENTICATED AuthLevel to app navigation/FP-02. | MUST | State transition and FP-02 handoff tests. |
| FP01-FR-004 | A recovery PKCE callback MUST set a durable recovery-pending marker before asynchronous code exchange begins. | MUST | Callback-order test and process-death test. |
| FP01-FR-005 | An SDK Authenticated event received while recovery is pending MUST remain RECOVERY_ONLY and MUST NOT trigger workspace/profile queries. | MUST | Fake repository test verifies no workspace call. |
| FP01-FR-006 | A successful password update MUST clear the recovery marker and transition to FULLY_AUTHENTICATED. | MUST | Recovery integration/state test. |
| FP01-FR-007 | Cancelling/backing out of RECOVERY_ONLY MUST sign out the recovery session and return to SIGNED_OUT; it MUST NOT expose the workspace. | MUST | Cancel/back test with Auth-state and workspace assertions. |
| FP01-FR-008 | If remote current-session sign-out fails, the user MUST have a clear local sign-out fallback. The UI MUST explain that remote revocation could not be confirmed. | MUST | Network-failure test; local SDK session is cleared. |
| FP01-FR-009 | Re-delivery of the same recovery callback during Activity recreation/process restart MUST NOT exchange a consumed PKCE code again. | MUST | Duplicate callback and recreation tests. |
| FP01-FR-010 | If the process dies while callback processing is pending, the next launch MUST NOT treat the resulting SDK session as FULLY_AUTHENTICATED. It MUST show an interrupted-recovery path and safely clear/restart the partial session. | MUST | Process-death injection test. |
| FP01-FR-011 | If the process dies after the recovery session is ready for password update, the app MUST restore the recovery screen while the SDK session remains valid. | MUST | Process-recreation test. |
| FP01-FR-012 | The app MUST preserve the existing exact recovery redirect and PKCE flow unless a reviewed redirect change is approved. | MUST | Redirect allow-list and physical email-link test. |
| FP01-FR-013 | The app MUST display an actionable error for email_address_not_authorized and over_email_send_rate_limit. | MUST | Auth error mapping tests and staging tests. |
| FP01-FR-014 | The app MUST distinguish request/IP rate limiting (over_request_rate_limit) from email-sending rate limiting. | MUST | Error mapping test. |
| FP01-FR-015 | The app MUST NOT expose raw exceptions, passwords, access/refresh tokens, callback codes, PKCE verifier or full recovery URLs. | MUST | Log and artifact inspection. |
| FP01-FR-016 | The app MUST use an injectable Auth repository interface so ViewModel tests can supply a fake without constructing the production Supabase client. | MUST | Unit tests instantiate ViewModel with fake repository. |
| FP01-FR-017 | The app MUST preserve the current sign-in, recovery and workspace experience except where the above defects require correction. | MUST | Baseline regression suite. |
| FP01-FR-018 | The app MUST NOT expose student signup. Project-level signup configuration MUST be checked against the institution provisioning policy. | MUST | UI test plus Supabase project setting evidence. |

### 7.1 Owner-confirmed account-provisioning boundary (4 November 2026)
- Students do not self-register and the Android app must not expose a Create account route or call `supabase.auth.signUp`.
- The College Administrator provisions the student roster/Auth identity and sends an invitation to activate that already-created account. Invitation dispatch and invitation-specific callback handling belong to the Administrator/account-provisioning feature track, not FP-01.
- FP-01 handles normal email/password sign-in, session restoration, sign-out and password recovery for provisioned accounts. Do not implement or invent an invitation flow in this feature, and do not break a separately owned invitation route.
- The Owner's later account-model decision requires the trusted invitation path to be proven before disabling the project-level “Allow new users to sign up” setting. Keep the Email provider enabled for existing email/password login. FP-01 implementation must not change live Supabase settings.

## 8. Sign-in, sign-out and recovery behavior

### 8.1 Sign-in
- Keep email/password sign-in.
- Use the existing school/institution email field and password field.
- Trim surrounding email whitespace only; pass password unchanged.
- Do not add a new sign-in provider or login identifier in this FP.
- A successful signIn call is not enough to navigate: use the Auth session-state flow.
- A FULLY_AUTHENTICATED session is handed to FP-02 for profile authorization.

### 8.2 Session restoration
- Keep the existing SDK-managed session restoration.
- Wait for the initial session status before deciding the app route.
- Restore RECOVERY_ONLY from the durable recovery marker, not from SessionStatus.Authenticated alone.
- If marker state says CALLBACK_PENDING after process death, do not show workspace. Clear the partial recovery session locally and ask the user to request a new recovery link.
- If marker state says PASSWORD_UPDATE_REQUIRED and the SDK session is still valid, restore the new-password form.
- If the SDK session is missing/expired while recovery is pending, clear the marker and return to the recovery request/sign-in route.

### 8.3 Sign-out
The pinned supabase-kt 3.6.0 source declares signOut(scope = SignOutScope.LOCAL) as the default. LOCAL means the current Supabase session, not all sessions on all devices. The call posts the current-session logout request and then clears the local session on success.

If the remote request fails:
- Show “Could not confirm sign-out with the service.”
- Offer Retry and “Sign out on this device”.
- The device-only action calls the SDK's local clearSession path, clears local Auth/recovery UI state and returns to SIGNED_OUT.
- Explain that local clearing does not prove the remote refresh session was revoked.
- Do not claim the user was signed out of all devices.

### 8.4 Password recovery
1. Student selects Forgot password and enters institution email.
2. App requests a recovery email using the exact configured redirect.
3. App shows a generic acknowledgement.
4. When the recovery callback arrives, app validates the expected callback route and writes RECOVERY_CALLBACK_PENDING before calling the asynchronous SDK handler.
5. SDK verifies/exchanges the PKCE code.
6. On callback success, app persists PASSWORD_UPDATE_REQUIRED before exposing the password form.
7. Authenticated SDK status remains RECOVERY_ONLY while this marker exists.
8. On valid new-password confirmation, call the SDK password update.
9. On success, clear the recovery marker and publish FULLY_AUTHENTICATED.
10. On cancellation, sign out the recovery session and return to SIGNED_OUT.
11. On failed/interrupted callback, never expose workspace; request a fresh link.

## 9. UX and error requirements

Keep the current sign-in, reset-request and new-password screens. Add/revise only the states required by the recovery lifecycle:

- Recovery callback being verified.
- Recovery ready for password update.
- Recovery interrupted; request a new link.
- Recovery cancelled; user is signed out.
- Sign-out could not reach Supabase; Retry or sign out on this device.
- Email delivery is not configured for this address.
- Too many recovery emails; wait before retry.
- Too many Auth requests; wait before retry.

Do not collapse all rate-limit and mail-provider errors into “Something went wrong”.

### Error behavior

| Supabase condition | Student-facing behavior |
|---|---|
| invalid_credentials | Generic “Email or password is incorrect.” |
| email_not_confirmed | Explain that the email must be confirmed; current GoTrue source checks the password before returning this error. Verify behavior against the deployed project before release. |
| email_address_not_authorized | Explain that recovery email delivery is not enabled for this address; direct student to the approved college support path. |
| over_email_send_rate_limit | Explain that the email service is temporarily rate-limited and the student should wait before requesting again. |
| over_request_rate_limit | Explain that too many requests were made and the student should wait before retrying. |
| flow_state_expired / flow_state_not_found / otp_expired | Explain that the recovery link expired or was already used; offer a new recovery request. |
| network/timeout | Preserve email, clear busy state and allow a bounded retry. |
| unknown provider error | Safe fallback and opaque diagnostic event; never show the raw exception. |

## 10. Non-functional requirements

- Preserve minSdk 29, compileSdk/targetSdk 37/37, Java/Kotlin 11, arm64-v8a and x86_64.
- Do not add a new Auth provider or SDK dependency for this feature.
- Keep Auth state and UI state free of passwords, tokens, PKCE codes and raw callback URLs.
- Use a monotonic or elapsed clock only where timing is measured; no arbitrary sign-in latency KPI is introduced.
- Support TalkBack and Android keyboard/password-manager interactions.
- Keep error messages actionable and non-sensitive.
- The recovery-only marker must be durable across process death, but must not store a raw callback code or token.

## 11. Dependencies and release blockers

- [BLOCKING] Production-capable email delivery: the historical project audit reported custom SMTP disabled. Supabase's built-in email service only sends to organization team members and is currently limited to two emails/hour project-wide. Configure a custom SMTP provider or Send Email Hook, verify sender/domain and complete real mailbox tests before student password recovery is accepted.
- [BLOCKING] Verify live Supabase signup, email-confirmation, redirect, OTP expiry and rate-limit settings.
- [BLOCKING] Verify exact session storage backend and at-rest protection for pinned supabase-kt 3.6.0.
- [BLOCKING] Recovery marker and AuthLevel fix must pass process-death/cancel/replay tests.
- [BLOCKING] Design an injectable Auth repository seam without rewriting the app architecture.
- Staging/test environment: no dedicated staging Supabase project is evidenced. Unit/UI tests must not assume one exists. Integration testing remains blocked until the Owner nominates an isolated local or hosted test environment and synthetic test identities.

## 12. Acceptance criteria

FP-01 is DONE when:
1. The four named existing baseline tests pass.
2. Valid sign-in and session restoration pass on the target Android device.
3. Invalid credentials and unconfirmed-email behavior match approved copy and deployed Auth behavior.
4. SDK Authenticated state is not allowed to enter FP-02 while RECOVERY_ONLY is persisted.
5. Recovery callback sets durable pending state before asynchronous exchange.
6. Recovery callback cannot be exchanged twice due to Activity recreation/re-delivery.
7. Process death during callback processing does not produce a normal authenticated workspace.
8. Process death after recovery becomes ready restores the password-update form.
9. Cancel/back from recovery signs out the recovery session, clears the marker and never opens the workspace.
10. Successful password update clears the marker and enables FULLY_AUTHENTICATED.
11. Sign-out uses current-session LOCAL behavior; network failure has a safe device-local fallback and honest message.
12. SMTP can deliver confirmation/recovery messages to synthetic non-team student addresses in the approved test environment.
13. The app renders specific messages for email-not-authorized and rate-limit errors.
14. Logs/storage/APK contain no forbidden credential or callback material.
15. AuthRepository fake seam supports unit/UI testing without a live Supabase project.
16. FR-AUTH-02 profile/assignment RLS work is left to FP-02/FP-03 and is not claimed complete by FP-01.

## 13. Defaults adopted for this review draft

To avoid leaving routine choices as open questions:
- Sign-in identifier: school/institution email only (matches current UI).
- MFA/passkeys/social sign-in: excluded from R1 pilot unless institutional security policy makes one mandatory.
- Password managers/autofill: supported through standard Android behavior; MobiDesk never stores passwords.
- Recovery redirect: retain current PKCE custom URI for the pilot, subject to callback tests; evaluate verified HTTPS App Links before wider production distribution.
- After successful password update: retain the recovery-established Auth session and elevate to FULLY_AUTHENTICATED only after the password update succeeds.
- Account-provisioning boundary: no app signup; profile/assignment eligibility remains in FP-02/FP-03.
- Session lifetime: use owner-approved Supabase project policy; do not introduce a client-side session lifetime.
- Confirmation resend UI: excluded unless actual support needs justify a separate addition.

## 14. Owner review required

Please focus on:
- Whether the current-session LOCAL sign-out with device-local fallback is acceptable.
- The public-signup policy is now Owner-confirmed: no student self-registration. The live Supabase toggle is a controlled operations step and must remain unchanged until the trusted administrator invitation path is implemented and tested.
- Whether the project must enforce MFA before R1 pilot.
- Whether the proposed AuthLevel/recovery-marker behavior is correct.
- Whether a custom SMTP provider or Send Email Hook is preferred and who owns its setup.
- Any institutional policy that changes email confirmation, password/session lifetime or support contact.
