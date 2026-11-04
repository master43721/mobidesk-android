# FP-01 Technical Design — Student Authentication and Account Recovery

**Version:** 0.2 — OWNER CRITIQUE REVISION  
**Date:** 4 November 2026  
**Repository:** master43721/mobidesk-android  
**Status:** Targeted design for review. No app source has been changed.

## 1. Design approach

Preserve the current Kotlin/Compose + Supabase Auth implementation. Do not replace it with a new AuthSessionCoordinator, AuthDeepLinkRouter or multi-layer state-machine architecture.

The smallest necessary changes are:
1. Introduce an app-level AuthLevel that distinguishes SIGNED_OUT, RECOVERY_ONLY and FULLY_AUTHENTICATED.
2. Persist a small recovery-state marker before PKCE callback processing and until password update/cancel is completed.
3. Prevent a recovery-created SDK Authenticated state from triggering workspace loading.
4. Make recovery cancel/back clear the recovery session and return to SIGNED_OUT.
5. Make duplicate callback delivery idempotent across Activity recreation/process restart.
6. Add an injectable AuthRepository interface so tests can use a fake.
7. Add a safe local-clear path when network sign-out cannot be confirmed.

The existing screen design remains. Refactor only where a failing acceptance test demonstrates a need.

## 2. Verified repository baseline

Snapshot: main at commit 5979320b05e1e6c1d872d82dcc17e65b0901e7e5.

| Source | Verified behavior | FP-01 consequence |
|---|---|---|
| MobiDeskApplication.kt | Creates Supabase client from SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY; installs Auth with PKCE and configured scheme/host. | Keep the single client and current SDK integration. |
| MainActivity.kt | Calls handleAuthIntent(intent) in onCreate and onNewIntent; checks ACTION_VIEW, scheme and host; detects recovery with pathSegments.contains("recovery"); passes matching intents to handleDeeplinks. | Exact path validation and durable callback de-duplication are needed. onCreate re-delivery after recreation is not guarded. |
| MobiDeskViewModel.kt | Observes Initializing, Authenticated, RefreshFailure and NotAuthenticated. On Authenticated it updates UI state and calls loadWorkspace(userId). | Must gate workspace loading on AuthLevel == FULLY_AUTHENTICATED, not SessionStatus.Authenticated alone. |
| MobiDeskViewModel.kt | cancelPasswordRecovery() only clears isPasswordRecovery; it does not sign out or clear the SDK session. | Recovery abandonment must clear the recovery session. |
| MobiDeskViewModel.kt | signOut() calls repository.signOut(); failure only places a safe error in UI state. | Add retry and explicit device-local clear fallback. |
| MobiDeskRepositories.kt | SupabaseAuthRepository wraps signIn, resetPasswordForEmail, updateUser password and signOut. | Retain implementation; expose it through an interface. |
| MobiDeskRepositories.kt | StudentWorkspaceRepository is separate from SupabaseAuthRepository, but the ViewModel coordinates both. | Keep the boundary explicit; no large repository redesign. |
| MobiDeskApp.kt | Existing Compose sign-in, reset request and recovery password screens. Password state is transient remember state, not rememberSaveable. | Keep screens; add recovery interruption/cancel/sign-out failure states only. |
| AndroidManifest.xml | Backup disabled, cleartext disabled; MainActivity is exported for launcher and custom URI callback. | Keep protections; callback intent validation remains required. |
| Existing tests | Two AuthErrorMessages unit tests and two Compose smoke tests. | Define and preserve this exact baseline suite; add focused tests rather than a large speculative catalogue. |

### Current implementation defects

**DEF-01 — Workspace can load during recovery.** In supabase-kt 3.6.0, the PKCE callback path exchanges the code and imports the session. The imported session becomes SessionStatus.Authenticated before MainActivity's handleDeeplinks onSessionSuccess callback is invoked. The current ViewModel may therefore call loadWorkspace before beginPasswordRecovery() is called.

**DEF-02 — Recovery cancellation leaves the user signed in.** cancelPasswordRecovery() flips a UI boolean only. It does not call signOut or clearSession.

**DEF-03 — Recovery state is not durable.** isPasswordRecovery exists only in ViewModel/UI state. After process death, the SDK-restored session can appear as ordinary Authenticated.

**DEF-04 — Callback re-delivery is not guarded.** onCreate and onNewIntent both route to handleAuthIntent. The SDK deletes the PKCE verifier after a successful code exchange, so a re-delivered callback can try to exchange a consumed code and create a spurious error. This is a code-path risk; runtime reproduction is a required test, not a claimed observation.

**DEF-05 — Sign-out failure has no exit path.** Current code displays an error but keeps the signed-in state; no explicit local clear option is exposed.

## 3. AuthLevel and minimal state model

Do not introduce a 14-state sealed state machine. Keep the SDK SessionStatus as the source of Supabase session truth and add one small app-owned auth-level value.

| AuthLevel | Derived from | Meaning |
|---|---|---|
| SIGNED_OUT | SDK has no usable session and no recovery marker | Normal sign-in route. |
| RECOVERY_ONLY | SDK session exists while recovery marker is CALLBACK_PENDING or PASSWORD_UPDATE_REQUIRED | Recovery route only; no workspace/profile request is permitted. |
| FULLY_AUTHENTICATED | SDK session exists and no recovery marker is active | Eligible for FP-02 workspace handoff. |

A small RecoveryStage enum is sufficient:
- NONE
- CALLBACK_PENDING
- PASSWORD_UPDATE_REQUIRED
- INTERRUPTED

Avoid duplicating all SDK network/loading states into a second state machine. Existing busy/error/info UI state can remain where it is.

### FP-02 handoff contract

Expose an app-owned AuthState with:
- AuthLevel;
- authenticated Supabase user ID only when a valid SDK session exists;
- display-safe email if the UI requires it;
- recovery stage when level is RECOVERY_ONLY.

Never expose password, access token, refresh token, PKCE verifier, callback code, raw exception or full callback URL in observable UI state.

The workspace load condition must be:

**SDK session is valid AND AuthLevel == FULLY_AUTHENTICATED.**

FP-02/FP-03 still independently check profile/assignment eligibility. FP-01 does not implement RLS/profile/assignment authorization.

## 4. Durable recovery marker

Use a small app-private durable state store for a recovery marker. The marker is not an authentication credential; it contains only:
- recovery stage (CALLBACK_PENDING, PASSWORD_UPDATE_REQUIRED or INTERRUPTED);
- callback fingerprint/hash for duplicate detection, never the raw code or URI;
- timestamp/expiry for stale recovery cleanup.

The marker MUST be written synchronously/durably before asynchronous handleDeeplinks code exchange is started. Do not rely on remember/rememberSaveable, ViewModel memory or a transient Activity field.

No password, access/refresh token, PKCE verifier, callback query string or full email address is stored in this marker. The Supabase SDK remains responsible for session persistence.

### Recovery lifecycle

1. Validate ACTION_VIEW, exact configured scheme, exact host and exact recovery path.
2. Read the callback code only in memory to derive a one-way fingerprint for de-duplication; never persist/log the code.
3. If the fingerprint is already pending/consumed, do not invoke handleDeeplinks again.
4. Persist CALLBACK_PENDING before starting asynchronous SDK exchange.
5. While CALLBACK_PENDING exists, any SessionStatus.Authenticated event is classified as RECOVERY_ONLY and cannot load workspace.
6. When SDK exchange succeeds, persist PASSWORD_UPDATE_REQUIRED before showing the password form.
7. After successful password update, clear recovery state and publish FULLY_AUTHENTICATED.
8. If user cancels/back, invoke current-session sign-out. If remote sign-out fails, offer local clearSession; clear marker and return to SIGNED_OUT.
9. If process dies while CALLBACK_PENDING, do not replay the callback. On next launch show an interrupted-recovery message, clear the partial session locally and ask for a new link.
10. If process dies while PASSWORD_UPDATE_REQUIRED, restore the recovery form when the SDK session remains valid. If the session is missing/expired, clear the marker and return to a safe recovery request state.

The marker should have a bounded lifetime aligned with the configured recovery link/session policy. Do not invent an expiry value; read the live Auth expiry setting and record the selected marker retention.

## 5. Callback processing and Activity integration

Keep the existing MainActivity entry point. Do not add a separate AuthDeepLinkRouter class in v0.2.

Required handling:
- Only VIEW intents for the configured scheme/host and exact recovery path are candidates for password recovery.
- Reject unexpected path shapes; do not use a loose contains("recovery") match.
- A valid candidate is placed in CALLBACK_PENDING before handleDeeplinks is invoked.
- handleDeeplinks still performs the SDK PKCE exchange; do not parse the resulting session from query parameters yourself.
- The callback success handler persists PASSWORD_UPDATE_REQUIRED before notifying the ViewModel/UI.
- The callback error handler moves to a recoverable interrupted/failed state and never enables workspace access.
- Duplicate intent delivery must be idempotent across onCreate, onNewIntent, configuration change and process restart.
- After a recovery callback is consumed, remove or neutralize the original Activity intent so that routine recreation does not resubmit it; the durable fingerprint/marker remains the source of idempotency.

A process death in the narrow window after SDK session import but before the app writes PASSWORD_UPDATE_REQUIRED must remain fail-closed: CALLBACK_PENDING is still durable, so the restored session is RECOVERY_ONLY and recovery is restarted safely rather than entering the workspace.

## 6. Repository seam — no new coordinator architecture

The current ViewModel constructs SupabaseAuthRepository directly. Introduce a small interface around the calls it needs:

| AuthRepository method | Purpose |
|---|---|
| sessionStatus | Observe SDK Auth state. |
| currentUserId() | Obtain current authenticated user ID. |
| signIn(email, password) | Email/password sign-in. |
| signOutCurrentSession() | Call Supabase signOut() using explicit LOCAL scope. |
| clearLocalSession() | Call SDK clearSession() for device-local fallback. |
| requestPasswordReset(email) | Start recovery email request. |
| updatePassword(password) | Complete recovery. |

SupabaseAuthRepository implements this interface. A small ViewModel factory/constructor seam supplies either the real repository or a fake in unit tests. No DI framework is required for FP-01.

Keep StudentWorkspaceRepository separate. The current ViewModel may retain workspace orchestration for now, but it must invoke it only when AuthLevel is FULLY_AUTHENTICATED. Moving all workspace ownership into a new architecture is not part of FP-01 unless later tests show this small boundary is insufficient.

## 7. Pinned Supabase Kotlin SDK 3.6.0 facts

Verified source links:
- Auth interface and default sign-out scope: https://github.com/supabase/supabase-kt/blob/3.6.0/Auth/src/commonMain/kotlin/io/github/jan/supabase/auth/Auth.kt
- SignOutScope enum: https://github.com/supabase/supabase-kt/blob/3.6.0/Auth/src/commonMain/kotlin/io/github/jan/supabase/auth/SignOutScope.kt
- AuthImpl sign-out and PKCE exchange/session import: https://github.com/supabase/supabase-kt/blob/3.6.0/Auth/src/commonMain/kotlin/io/github/jan/supabase/auth/AuthImpl.kt
- Android handleDeeplinks: https://github.com/supabase/supabase-kt/blob/3.6.0/Auth/src/androidMain/kotlin/io/github/jan/supabase/auth/Android.kt
- AuthConfig defaults: https://github.com/supabase/supabase-kt/blob/3.6.0/Auth/src/commonMain/kotlin/io/github/jan/supabase/auth/AuthConfig.kt
- SettingsSessionManager: https://github.com/supabase/supabase-kt/blob/3.6.0/Auth/src/settingsMain/kotlin/io/github/jan/supabase/auth/SettingsSessionManager.kt

**Sign-out:** Auth.signOut(scope: SignOutScope = SignOutScope.LOCAL). LOCAL means current session, not all sessions. AuthImpl sends the server logout request and then clears the client session on success. A non-ignored REST/network failure can throw before local session clear. The SDK also exposes clearSession() for local cleanup.

**Session persistence:** default Auth config has autoLoadFromStorage=true and autoSaveToStorage=true. AuthImpl defaults to SettingsSessionManager; SettingsSessionManager serializes UserSession as JSON into the Settings API. The exact resolved Android Settings backing and at-rest encryption of the app's dependency graph were not proven in this review. This remains one short storage inspection, not a reason to redesign Auth now.

**PKCE callback:** Android handleDeeplinks checks scheme/host and passes the code to exchangeCodeForSession. AuthImpl exchanges the code, deletes the verifier on successful exchange, imports the session and sets Authenticated before the Android helper invokes onSessionSuccess. The SDK does not itself add MobiDesk's RECOVERY_ONLY application level.

## 8. Error mapping

Use typed AuthRestException/AuthErrorCode values from the pinned SDK where available, not fragile message substring matching alone.

| Error code | UI treatment |
|---|---|
| invalid_credentials | Generic credential error. |
| email_not_confirmed | Actionable confirmation message; do not automatically hide this behind generic invalid-credentials text. |
| email_address_not_authorized | Recovery email delivery is not configured for this recipient; direct to approved support path. |
| over_email_send_rate_limit | Too many emails; wait before requesting another. |
| over_request_rate_limit | Too many Auth requests; wait before retrying. |
| flow_state_expired / flow_state_not_found / otp_expired | Link is expired or already used; request a new link. |
| other/network | Safe generic message, retry where appropriate; no raw exception. |

Current upstream GoTrue source checks password validity before returning email_not_confirmed. The deployed project version must still be tested; do not treat this as a guaranteed deployment fact.

## 9. Required focused spikes

| Spike | Effort | Question | Exit evidence |
|---|---:|---|---|
| FP01-TS-01 | 1–2 h | What is the actual Android storage backend used by resolved supabase-kt 3.6.0 Settings, and is session data encrypted? | Gradle-resolved dependency source, storage backend, app-private storage/backup evidence and recommendation. |
| FP01-TS-02 | 1 h | Does callback processing fail/replay on Activity recreation or process kill? | Instrumented duplicate/recreation/process-kill test with no workspace exposure. |
| FP01-TS-03 | 0.5–1 h | Does deployed GoTrue return email_not_confirmed only after a valid password? | Synthetic staging/local Auth test for wrong and correct password on an unconfirmed user. |

No broad Auth architecture rewrite is justified by the current evidence.
