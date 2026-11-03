# FP-01 Technical Design — Student Authentication and Account Recovery

**Version:** 0.1 — REVIEW DRAFT  
**Date:** 3 November 2026  
**Scope:** master43721/mobidesk-android only  
**Status:** Proposed target design; no source code changed.

## 1. Design principles

1. Supabase Auth remains the only student authentication authority.
2. The pinned Supabase Kotlin SDK remains at the repository version unless a separately reviewed dependency decision is made.
3. Auth state comes from the SDK session-state flow, not from button success alone.
4. Passwords are transient input. Tokens and PKCE callback codes are never exposed to UI state or logs.
5. Authentication is distinct from student-profile and VM authorization.
6. The existing source implementation is evidence, not a requirement. The target design may refactor its boundaries.

## 2. Verified repository baseline

Snapshot inspected: main branch at commit 5979320b05e1e6c1d872d82dcc17e65b0901e7e5.

| Area | Verified observation | FP-01 implication |
|---|---|---|
| App stack | Native Kotlin + Jetpack Compose | Keep Auth implementation native; no Flutter architecture. |
| Build | minSdk 29, compile/target 37/37, Java/Kotlin 11, ABIs arm64-v8a and x86_64 | Preserve baseline. |
| Supabase libraries | BOM 3.6.0, auth-kt, postgrest-kt; Ktor Android client 3.4.3 | Pin compatibility; no dependency upgrade in FP-01 without a reason. |
| Client configuration | SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY are read from local.properties and injected as BuildConfig values | Missing values produce an unconfigured client; use only a publishable/anon key in the APK. |
| Auth client | MobiDeskApplication creates a Supabase client and installs Auth with FlowType.PKCE and custom scheme/host | PKCE is already selected in code. |
| Redirect | Callback URI is configured as com.mobidesk.android://auth-callback; recovery URI appends /recovery | Must verify exact provider allow-list and callback path handling. |
| Manifest | MainActivity is exported for launcher and custom-scheme VIEW callback; backup disabled; cleartext disabled | Validate callback routing and keep backup/cleartext protections. |
| Repository | SupabaseAuthRepository wraps signIn, resetPasswordForEmail, updateUser password and signOut | Existing operations cover core happy paths. |
| ViewModel | MobiDeskViewModel observes sessionStatus including Initializing, Authenticated, RefreshFailure and NotAuthenticated | Keep the SDK state stream authoritative. |
| ViewModel boundary | Same ViewModel also starts StudentWorkspaceRepository reads after Authenticated | Current coupling crosses FP-01 into FP-02; plan a clean handoff/refactor. |
| UI | MobiDeskApp holds email in rememberSaveable and password in remember; password is cleared after authenticated state | Preserve non-persisted password behavior; verify process recreation. |
| Deep link | MainActivity validates VIEW action, scheme and host, detects recovery path and calls handleDeeplinks | Add negative tests for malformed, wrong path/host, expired and replayed callbacks. |
| Tests | Two AuthErrorMessages unit tests; two Compose smoke tests (no preview/signup route and reset screen navigation) | Current coverage is only a small smoke baseline; no real Auth integration/recovery evidence. |

### Current source references

- app/src/main/java/com/mobidesk/android/MobiDeskApplication.kt
- app/src/main/java/com/mobidesk/android/MainActivity.kt
- app/src/main/java/com/mobidesk/android/MobiDeskViewModel.kt
- app/src/main/java/com/mobidesk/android/data/MobiDeskRepositories.kt
- app/src/main/java/com/mobidesk/android/ui/MobiDeskApp.kt
- app/src/test/java/com/mobidesk/android/AuthErrorMessagesTest.kt
- app/src/androidTest/java/com/mobidesk/android/MobiDeskSmokeTest.kt
- app/build.gradle
- app/src/main/AndroidManifest.xml
- docs/SUPABASE_DISCOVERY.md (historical snapshot only)

## 3. Proposed target component boundary

The following is a logical design; package/class names are proposals, not claims that they already exist.

| Component | Responsibility | Must not own |
|---|---|---|
| SupabaseClientProvider / MobiDeskApplication | Build client once from runtime/build configuration; install Auth with PKCE | UI routing or student assignment decisions |
| AuthRepository | Sign in, observe session state, sign out, request reset, update recovered password | PostgREST profile or VM queries |
| AuthSessionCoordinator | Serialize Auth operations, translate SDK session states into app-owned domain state | Raw password/token in observable state |
| AuthViewModel | Expose immutable Auth UI state and commands | Supabase-specific exception leakage |
| SignInScreen | Collect email/password, show validation/progress/messages | SDK calls or session persistence |
| RecoveryRequestScreen | Collect email and request reset | Account enumeration |
| RecoveryPasswordScreen | Collect new password and confirmation after valid callback | Handling arbitrary deep links itself |
| AuthDeepLinkRouter | Validate expected callback intent and route through pinned SDK handler | Trusting URI query values as authenticated identity |
| FP-02 Workspace Coordinator | Consume Authenticated and perform student-profile/workspace checks | Re-implementing Supabase password authentication |

### Proposed state model

Prefer one sealed/explicit Auth state over many independent booleans that can form impossible combinations.

- ConfigUnavailable
- RestoringSession
- SignedOut
- SigningIn
- Authenticated(userId, email/display-safe claims only)
- RefreshFailed
- RequestingRecovery
- RecoveryRequested
- VerifyingRecovery
- RecoveryReady
- UpdatingPassword
- RecoveryFailed
- SigningOut
- SignOutFailed

The model must not contain a password, access token, refresh token, PKCE verifier, callback code or raw exception.

### Boundary with FP-02

Current MobiDeskViewModel sets Authenticated and immediately calls loadWorkspace(userId). That combines two feature responsibilities. Proposed FP-01 output is:

- isAuthenticated / explicit Auth state;
- stable authenticated user ID required by downstream authorization;
- safe email display value if required;
- Auth event source/status.

FP-02 then performs profile and workspace checks. A Supabase Authenticated state must never be shown as "student verified" until FP-02 confirms the provisioned student profile.

## 4. Auth state machine

~~~mermaid
stateDiagram-v2
  [*] --> CheckingConfiguration
  CheckingConfiguration --> ConfigUnavailable: URL or publishable key missing
  CheckingConfiguration --> RestoringSession: configured
  RestoringSession --> SignedOut: NotAuthenticated
  RestoringSession --> Authenticated: Authenticated
  RestoringSession --> SignedOut: RefreshFailure
  SignedOut --> SigningIn: submit valid form
  SigningIn --> Authenticated: SDK session stream confirms
  SigningIn --> SignedOut: request rejected
  SignedOut --> RequestingRecovery: Forgot password
  RequestingRecovery --> RecoveryRequested: request accepted
  RequestingRecovery --> SignedOut: recoverable request error
  RecoveryRequested --> VerifyingRecovery: callback intent
  VerifyingRecovery --> RecoveryReady: valid SDK recovery session
  VerifyingRecovery --> RecoveryFailed: invalid or expired callback
  RecoveryFailed --> RequestingRecovery: request another link
  RecoveryReady --> UpdatingPassword: submit matching password
  UpdatingPassword --> Authenticated: password update succeeds and policy retains session
  UpdatingPassword --> RecoveryReady: update rejected
  Authenticated --> SigningOut: Sign out
  SigningOut --> SignedOut: local session cleared
  SigningOut --> SignOutFailed: state cannot be confirmed
  Authenticated --> SignedOut: RefreshFailure / session invalidated
  SignedOut --> [*]
~~~

The diagram's post-recovery destination assumes the current implementation behavior of retaining the callback-established Auth session after password update. Owner must confirm this before it becomes a requirement.

## 5. Operation contracts

### 5.1 Sign-in

Input:
- Email string.
- Password string.

Rules:
- Trim email outer whitespace only.
- Do not trim or log password.
- Validate non-empty input locally; server remains authoritative for actual credential validity and provider policy.
- Disable repeat submission while request is active.
- Successful SDK call is not sufficient by itself to route; observe Authenticated state from sessionStatus.
- No client-side domain allowlist is assumed. Account eligibility comes from the configured provisioning policy.

Output:
- Authenticated state only after SDK confirmation.
- Safe generic failure otherwise.

### 5.2 Session restoration

- Auth client must be initialized once per app process.
- The app waits for the initial session state.
- If the SDK refreshes a session, keep UI in a restoration/refresh state until a stable status is emitted.
- Refresh failure clears authenticated UI and sends the user to sign-in.
- Exact persisted storage, encryption and SDK lifecycle behavior must be inspected for supabase-kt 3.6.0; do not assume that the mere presence of automatic session restoration proves encrypted token storage.

### 5.3 Sign-out

- Call the pinned SDK's supported sign-out API.
- Establish whether the pinned SDK default is local, current-session or global sign-out.
- FP-01 UI must clear local authenticated state on confirmed local sign-out.
- If offline or the remote request fails, do not falsely claim all server-side sessions are revoked.
- Server session revocation and other-device invalidation behavior must be tested/documented.

### 5.4 Recovery request

- Use the existing configured recovery URI: com.mobidesk.android://auth-callback/recovery.
- Use a generic acknowledgement for both known and unknown addresses.
- Do not put user passwords into request data beyond the required authenticated API call.
- Never log recovery URL, access code, PKCE verifier, tokens or email address.
- Provider/email delivery is an external dependency and must be tested in staging.

### 5.5 Deep-link verification

1. Only handle an Intent with ACTION_VIEW.
2. Require the exact configured scheme and host.
3. Validate the path against the expected recovery path (current code uses pathSegments.contains("recovery"); exact-path behavior needs review).
4. Pass the original Intent to Supabase handleDeeplinks.
5. Show RecoveryReady only after the SDK reports successful session establishment.
6. Reject wrong host, wrong scheme, unexpected path, missing callback values, invalid state, expired code and replay.
7. Do not trust query parameters as user ID, email or authentication proof.

### 5.6 Password update

- Require a verified recovery session.
- Validate both fields match.
- Current code enforces a minimum of 8 characters; verify this matches the current Supabase policy before retaining it.
- The provider policy is authoritative and can reject a password even if the client check passes.
- Clear transient password/confirmation values after completion, cancellation and failure where safely possible.
- Never log backend password policy details that contain sensitive/internal data.

## 6. SDK and dependency policy

- Current implementation uses Supabase BOM 3.6.0 with auth-kt and postgrest-kt.
- Current Ktor client dependency is 3.4.3.
- Keep these versions for FP-01 unless an observed defect requires a version change and the upgrade is tested against the repository Kotlin/Gradle toolchain.
- Do not add an Auth service-role/admin SDK capability to the Android app.
- Do not introduce a second authentication provider or home-grown JWT parser.

## 7. Failure and recovery behavior

| Failure | Target behavior |
|---|---|
| Missing Supabase configuration | Disable Auth operations and show explicit environment/configuration error. |
| Invalid credentials | Generic credential error; no account existence signal. |
| Network/timeout | Preserve email, clear loading, show safe retry. |
| Refresh failure | Clear authenticated route, return to sign-in. |
| Callback from wrong URI | Ignore/reject safely and show no recovery form. |
| Expired/replayed recovery code | RecoveryFailed; user can request a new link. |
| Password policy rejection | Show safe actionable policy copy; retain no unnecessary password copy. |
| Sign-out request failure | Avoid false revocation message; reconcile local state based on verified SDK semantics. |

## 8. Observability

Allowed:
- Non-identifying event category (AUTH_SIGN_IN_SUCCESS/FAILURE, AUTH_RESTORE_SUCCESS/FAILURE, AUTH_SIGN_OUT, RECOVERY_REQUEST, RECOVERY_CALLBACK_SUCCESS/FAILURE, PASSWORD_UPDATE_SUCCESS/FAILURE).
- Opaque attempt ID.
- Duration and safe result class.
- SDK/app version and coarse network class if required for diagnostics.

Forbidden:
- Email, password, access/refresh token, PKCE verifier/code, full callback URI/query, raw Authorization header, raw Supabase exception, session ID or private project configuration.

## 9. Technical spikes / blockers

| Spike | Question | Exit evidence |
|---|---|---|
| FP01-TS-01 | How does supabase-kt 3.6.0 persist and protect sessions on Android? | Inspect pinned artifact source/storage backend; show no raw credential/token logs; record data-at-rest model. |
| FP01-TS-02 | What does client.auth.signOut() default to in 3.6.0? | Inspect source/API and test local session removal vs other-device/server revocation. |
| FP01-TS-03 | Does handleDeeplinks enforce PKCE state/verifier and reject replay as expected? | Tests for valid, wrong-state, expired, malformed and replayed callback. |
| FP01-TS-04 | Is the custom URI redirect sufficiently protected for release? | Threat review of scheme hijack and choice of verified HTTPS App Links vs current custom URI. |
| FP01-TS-05 | Are email delivery and account-enumeration behaviors correct in the live Supabase project? | Staging tests and owner-approved Auth config. |

## 10. Current implementation gaps (not yet proven failures)

- MobiDeskViewModel combines Auth and workspace loading.
- Error mapper distinguishes "email not confirmed"; the desired enumeration policy is unresolved.
- Password reset UI smoke test only opens the reset screen; it does not send a real mail or complete recovery.
- No test currently verifies the full recovery callback flow.
- Session persistence storage/encryption and sign-out scope have not been established from this repository inspection.
- Historical Supabase discovery reports custom SMTP disabled; actual current email delivery is unverified.
- Live public-signup setting must be rechecked; a missing app signup button does not disable provider-level signup.

