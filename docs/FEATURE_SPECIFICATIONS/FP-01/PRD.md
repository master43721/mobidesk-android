# FP-01 Product Requirements Document — Student Authentication and Account Recovery

**Version:** 0.1 — REVIEW DRAFT  
**Date:** 3 November 2026  
**Parent feature:** FP-01  
**Product:** MobiDesk  
**Repository:** master43721/mobidesk-android  
**Normative parent:** MobiDesk PRD v1.2  
**Approval:** Not approved. Project Owner critique required.

## 1. Product summary

FP-01 lets a student sign in to MobiDesk using the institution-provisioned email/password identity, restores a valid Auth session on later launches, provides sign-out, and lets the student request and complete password recovery.

Authentication is delegated to the existing Supabase Auth project through the Supabase Kotlin SDK already included in the Android application. MobiDesk does not become an identity provider and does not create student accounts.

FP-01 proves **who has authenticated**. It does not prove that the user has a student profile, college enrolment or assigned VM. Those are later workspace/authorization checks owned by FP-02 and FP-03.

## 2. Problem and desired outcome

A student must be able to enter MobiDesk with the institution-provisioned identity without creating another locally managed account. Returning students should not need to sign in again while their Auth session remains valid and refreshable. A student who forgets a password must have a verified recovery path back into the app.

A successful FP-01 flow is:
1. Open MobiDesk.
2. Restore an existing valid Supabase session or show the sign-in screen.
3. Sign in with the institution-provisioned email/password.
4. Remain authenticated while the SDK session remains valid and refreshable.
5. Sign out deliberately.
6. Request password recovery and complete the reset through the verified Android deep-link callback.

## 3. Actors

| Actor | Responsibility |
|---|---|
| Student | Sign in, restore session, sign out and recover a forgotten password. |
| Institution / Project Owner | Provision eligible identities and control Auth project settings. |
| Supabase Auth | Verify credentials, manage Auth sessions, send recovery links and accept password updates. |
| Android client | Collect credentials, invoke the Auth SDK, observe Auth state and process deep links. |
| FP-02 workspace | Separately verify that the authenticated user has an approved student profile. |

## 4. Scope

### Included

- Supabase email/password sign-in.
- Trim surrounding email whitespace; pass password exactly as entered.
- Busy state and duplicate-submit prevention.
- Auth session initialization/restoration.
- Observation of authenticated, unauthenticated, initializing and refresh-failure states.
- Deliberate sign-out.
- Forgot-password request.
- Recovery URI/deep-link handling using the configured PKCE flow.
- New-password and confirmation form.
- Safe user-facing error and success messages.
- Missing Supabase configuration state.
- Authentication-focused unit, UI, integration and security tests.

### Excluded

- Student self-registration/create-account screen or app-side signup API.
- Institution roster provisioning, invitation administration, account linking, deletion or administrator management.
- Student profile, college, VM assignment or VM access authorization.
- Resend-confirmation UI unless approved.
- Email change, multi-device session management, MFA, passkeys and social login in this draft.
- Windows guest authentication (FP-05), broker grants, FreeRDP and RDP sessions.
- Custom password database or local password cache.

**Configuration caveat:** the absence of an app signup screen does not prove that public signup is disabled in the Supabase project. Project-side signup configuration is an open security item and is described in SUPABASE_CONFIGURATION.md.

## 5. User journeys

### 5.1 First launch — no Auth session

1. Check that the Supabase URL and client-safe publishable key are configured.
2. If either is absent, show a configuration-unavailable state and disable sign-in.
3. If configured, wait for the SDK's initial session status before routing.
4. If no valid session exists, show sign-in.
5. Student enters school email and password.
6. Trim surrounding whitespace from email only; pass password unchanged.
7. Submit once and display progress.
8. On success, observe the Supabase Auth state stream and publish the authenticated state.
9. FP-02 receives the identity and separately checks the student profile.

### 5.2 Returning user

1. Initialize the Supabase Auth client.
2. Let the pinned SDK determine the restored/refreshable session state.
3. Do not flash the sign-in screen before initial restoration completes.
4. Publish Authenticated to FP-02, or show sign-in when unauthenticated/refresh failed.

### 5.3 Sign-out

1. Student selects Sign out.
2. Disable duplicate actions and invoke Supabase sign-out.
3. Clear local authenticated UI and protected in-memory state when local sign-out is confirmed.
4. Return to sign-in.
5. Do not claim server-side revocation if the operation failed or only local sign-out occurred. The exact SDK scope/default must be checked against the pinned SDK.

### 5.4 Forgotten password

1. Student selects Forgot password.
2. Student enters school email.
3. Invoke Supabase password recovery using the configured recovery redirect URI.
4. Show a generic acknowledgement that does not disclose whether the address maps to an account.
5. Student opens the recovery email link.
6. Android validates callback scheme/host/path and sends the intent to the Supabase Kotlin SDK deep-link handler.
7. Show the new-password form only after the SDK establishes a valid recovery session.
8. Student enters and confirms the new password.
9. Validate confirmation and the locally displayed minimum; Supabase remains authoritative for its configured password policy.
10. Invoke Supabase user password update.
11. Show success and apply the Owner-approved post-recovery session policy.

### 5.5 Invalid or expired recovery link

- Do not enable password update until a valid recovery session exists.
- Show a safe invalid/expired-link message.
- Offer a path to request a new recovery link.
- Do not trust an arbitrary URL merely because it uses the app's custom scheme.

## 6. Functional requirements

IDs use the FP01 prefix to avoid collision with parent PRD identifiers.

| ID | Requirement | Priority | Acceptance evidence |
|---|---|---:|---|
| FP01-FR-001 | MUST authenticate existing students using the configured Supabase email/password provider. | MUST | Staging integration test for valid and invalid credentials. |
| FP01-FR-002 | MUST NOT expose student self-registration or account creation in the app. | MUST | Navigation/UI test and APK/source scan. |
| FP01-FR-003 | MUST wait for initial Auth session restoration before selecting the signed-in or signed-out route. | MUST | Cold-start and process-recreation tests. |
| FP01-FR-004 | MUST derive authenticated state from the Supabase session-state stream, not solely from a successful sign-in call return. | MUST | Repository/ViewModel state tests. |
| FP01-FR-005 | MUST handle Initializing, Authenticated, NotAuthenticated and RefreshFailure states without contradictory UI. | MUST | State-transition tests. |
| FP01-FR-006 | MUST clear password input after successful sign-in and MUST NOT persist passwords in SavedState, preferences, database or logs. | MUST | UI, process-recreation and static/log checks. |
| FP01-FR-007 | MUST trim surrounding email whitespace and MUST pass the password unchanged. | MUST | Input transformation tests. |
| FP01-FR-008 | MUST prevent duplicate sign-in, recovery-request and password-update submissions while an operation is active. | MUST | UI and ViewModel tests. |
| FP01-FR-009 | MUST provide sign-out and clear local authenticated UI after local sign-out is confirmed. | MUST | SDK integration and network-failure tests. |
| FP01-FR-010 | MUST request password recovery using the configured, allow-listed redirect URI. | MUST | Staging mail/deep-link integration test. |
| FP01-FR-011 | MUST process recovery deep links through the pinned SDK and reject malformed or unexpected callback destinations. | MUST | Deep-link instrumentation tests. |
| FP01-FR-012 | MUST enable password update only after a valid recovery session is established. | MUST | Valid, expired, altered and replayed-link tests. |
| FP01-FR-013 | MUST validate new-password confirmation; Supabase remains authoritative for the project password policy. | MUST | Unit and staging policy tests. |
| FP01-FR-014 | MUST give a generic recovery-request acknowledgement that does not disclose account existence. | MUST — proposal for Owner approval | Compare existing/non-existing test addresses. |
| FP01-FR-015 | MUST NOT expose raw backend exceptions, tokens, callback codes, URLs or internal service details to users or logs. | MUST | Error mapping and log scan. |
| FP01-FR-016 | MUST fail closed when required Supabase URL/publishable-key configuration is absent. | MUST | Build-variant/configuration test. |
| FP01-FR-017 | MUST NOT package a Supabase service-role/secret key. Only a client-safe publishable/anon key may be used in the Android app. | MUST | Repository/build artifact secret scan. |
| FP01-FR-018 | MUST hand Authenticated identity/session state to FP-02 without asserting student profile or VM authorization. | MUST | Architecture boundary and integration test. |

The parent PRD index lists FR-AUTH-01 and FR-AUTH-02. Their exact wording and one-to-one traceability must be checked against the full parent PDF before FP-01 v1.0 is approved. This draft does not silently redefine parent IDs.

## 7. User experience requirements

### Screens and states

| State/screen | Required behavior |
|---|---|
| Sign in | School email, password, visibility toggle, Sign in, Forgot password, institution-provisioned account explanation. |
| Restoring | Loading state; no premature sign-in/dashboard flash. |
| Submitting | Disable controls, show progress and prevent duplicate request. |
| Sign-in failure | Approved safe message; retain email, do not persist password. |
| Recovery request | Email field, Send recovery link, generic acknowledgement, Back. |
| Recovery verification | Show verification progress; no usable new-password form yet. |
| New password | New password, confirmation, visibility control, Update password. |
| Recovery failure | Safe expired/invalid-link message and request-new-link path. |
| Not configured | Explain sign-in is unavailable; no fake login or demo fallback. |

### UX principles

- Use one consistent term: "school account" or "college account" (Owner to choose).
- Do not show Create account.
- Avoid account enumeration in sign-in and recovery messages.
- Test Android password-manager/autofill behavior; never add a MobiDesk remember-password option.
- Support keyboard focus order, TalkBack labels and accessible loading/error announcements.
- Do not tell a student that an email was delivered merely because the client request returned successfully.

## 8. Non-functional requirements

| Area | Requirement |
|---|---|
| Reliability | Temporary network failures must not leave contradictory Auth/UI state. |
| Cold start | Wait for initial SDK session status before routing; no numeric latency target is invented here. |
| Privacy | No email/password, JWT, refresh token, authorization header or PKCE code in app logs/analytics. |
| Compatibility | Preserve minSdk 29, compileSdk 37, targetSdk 37, Java/Kotlin 11, ABIs arm64-v8a and x86_64 unless formally changed. |
| Maintainability | Keep Auth operations separable from student workspace/VM data access. |
| Accessibility | Inputs, visibility toggles, loading and errors support TalkBack and keyboard navigation. |
| Localization | English is the current copy baseline; Telugu translations require review. |

## 9. Error handling — proposed for critique

| Condition | Candidate behavior |
|---|---|
| Missing local configuration | Sign-in unavailable; contact support. |
| Invalid email/password | Generic credential failure; do not reveal account existence. |
| Email not confirmed | Do not reveal account status unless Owner accepts the enumeration risk; show approved generic next action. |
| Network/timeout | Safe retry message. |
| Expired/invalid recovery link | Explain the link expired/cannot be verified and offer a new request. |
| Password policy rejection | Actionable policy message, never raw backend details. |
| Password mismatch | "The passwords don't match." |
| Sign-out failure | Do not claim remote revocation; preserve/reconcile local state according to proven SDK semantics. |
| Unknown provider error | Generic message plus opaque diagnostic event. |

The current implementation has a specific "email not confirmed" branch. The proposed generic behavior is a security refinement for Owner decision, not a statement that the app already behaves this way.

## 10. Acceptance criteria — FP-01 complete

FP-01 is DONE only when all of the following are evidenced:

1. A provisioned test student can sign in using Supabase email/password.
2. Invalid credentials use Owner-approved safe wording.
3. A valid Auth session restores after app process restart.
4. Expired/invalid sessions route to sign-in without briefly displaying authenticated UI.
5. Auth transitions remain coherent through network loss and refresh failure.
6. Sign-out clears local authenticated state; selected server-side revocation semantics are verified.
7. Existing and non-existing test addresses receive a generic recovery-request acknowledgement.
8. A real staging recovery email reaches the test device through the approved redirect.
9. A valid PKCE recovery callback permits a password update.
10. Expired, malformed, wrong-host and replayed callbacks fail safely.
11. Passwords, tokens and PKCE codes do not appear in logs, analytics, SavedState or persistent app storage.
12. The APK contains only the client-safe Supabase key; no service-role/secret key.
13. No app self-registration path exists.
14. FP-01 publishes Auth state to FP-02 but does not query or authorize VM assignment.

## 11. Dependencies and blockers

- Correct Supabase project URL and client-safe publishable key.
- Email/password provider enabled.
- Account provisioning and email-confirmation policy approved.
- Exact mobile recovery URI allow-listed.
- Email template and sender delivery verified.
- Supabase Kotlin SDK version remains pinned at 3.6.0 unless deliberately re-reviewed.
- Session persistence and sign-out semantics of the pinned SDK verified.
- Account-enumeration behavior reviewed.
- Parent FR-AUTH-01/02 wording confirmed before FP-01 approval.

## 12. Not promised

- Email delivery solely because the client request succeeds.
- Session survival after force-stop, app data clear or OS process termination.
- VM/Cloud PC access solely because Supabase authentication succeeds.
- MFA, passkeys or biometric reauthentication.
- Numeric sign-in latency not backed by measurement.

## 13. Owner review checklist

- Sign-in identifier: school email only, or username/roll number too?
- Is email recovery the desired and sufficient recovery channel?
- After recovery, keep the valid Auth session or force a fresh sign-in?
- Should unknown/unconfirmed account errors all be generic?
- What session lifetime and cross-device session policy is desired?
- Should public Supabase signup be disabled project-wide?
- Are MFA/passkeys excluded or required by institutional policy?
- What support contact and wording should be displayed to students?
- Confirm profile/VM authorization remains FP-02/FP-03, not FP-01.
