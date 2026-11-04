# FP-01 Antigravity Implementation Handoff

**Canonical repository:** `master43721/mobidesk-android`  
**Feature:** FP-01 — Student Authentication and Account Recovery  
**Prepared:** 4 November 2026  
**Fresh documentation branch base:** current `main` at preparation SHA `60b3152c84ec13a0f43ec59a983e968c0b5b3ff7`. Re-fetch main before coding.  
**Do not use:** `feature/fp-01-authentication-draft`; it was 39 commits behind current main when compared.

## 1. Goal

Complete FP-01 as a narrowly scoped hardening of the existing native Kotlin + Jetpack Compose + Supabase Auth implementation. Preserve existing screens, the single Supabase client, PKCE and pinned Supabase Kotlin SDK 3.6.0. Do not rewrite Auth architecture.

FP-01 authenticates identity only. FP-02/FP-03 own profile, assignment and workspace authorization. A password-recovery session must never be treated as a normal authenticated workspace session.

## 2. Required reading / precedence

Read the latest versions of:
- `docs/MobiDesk_PRD_v1.2.pdf` (normative parent PRD; do not change it).
- `docs/FEATURE_INVENTORY.md`, `docs/PRD.md`, `docs/TECHNICAL_SPEC.md`.
- Every file in `docs/FEATURE_SPECIFICATIONS/FP-01/`: PRD, TECHNICAL_DESIGN, IMPLEMENTATION_PLAN, TEST_PLAN, SECURITY_PRIVACY, SUPABASE_CONFIGURATION, DECISIONS, OPEN_QUESTIONS, TRACEABILITY and PROGRESS.
- Cross-feature documents: `docs/API_CONTRACT.md`, `docs/DECISIONS.md`, `docs/OPEN_ITEMS.md`, `docs/IMPLEMENTATION_PLAN.md`, `docs/TEST_PLAN.md`, `docs/SECURITY_THREAT_MODEL.md`, `docs/TRACEABILITY_CHECK.md`.

Use parent PRD > confirmed Owner decisions/Feature Inventory > FP-01 specification and tests > current code. Code is evidence of current behavior, not a substitute for requirements. Surface conflicts; do not silently reconcile or invent policies.

## 3. Owner-confirmed provisioning rule

- Students must not self-register. Do not add a Create Account screen or call `supabase.auth.signUp`.
- College Administrator provisions each Auth identity and sends an invitation to activate the already-created account.
- FP-01 handles sign-in, session restoration, sign-out and password recovery for already-provisioned accounts.
- Admin roster management, Auth Admin invitation dispatch, invitation-specific callback handling and admin role/RBAC work are separate account-management features (FP-23/FP-24 / proposed IAM scope), not FP-01.
- Do not alter the live Supabase “Allow new users to sign up” setting during FP-01. The separate trusted Admin invitation path must be implemented and proven first. Afterward an authorized operator may disable that project-level setting while keeping the Email provider enabled.
- Do not disable Email provider; existing institution-provisioned email/password login must continue working.
- No Admin APIs, service-role key, admin role table, server/VM operations or student provisioning code in FP-01.

## 4. Current source map — reverify before coding

The current-main source inspected at preparation showed:

| File | Existing behavior / required correction |
|---|---|
| `app/src/main/java/com/mobidesk/android/MainActivity.kt` | `handleAuthIntent` matches ACTION_VIEW and scheme/host but uses loose `pathSegments.contains("recovery")`; exact callback validation and idempotency are required. |
| `app/src/main/java/com/mobidesk/android/MobiDeskApplication.kt` | Creates one Supabase client, installs Auth with PKCE and configured callback; preserve. |
| `app/src/main/java/com/mobidesk/android/MobiDeskViewModel.kt` | On SDK Authenticated event calls `loadWorkspace(userId)`; recovery may therefore race into workspace. Cancel only clears a UI flag. Sign-out failure has no device-local clear exit. |
| `app/src/main/java/com/mobidesk/android/data/MobiDeskRepositories.kt` | Contains `SupabaseAuthRepository`, `StudentWorkspaceRepository` and `AuthErrorMessages`; add small interfaces/test seam. |
| `app/src/main/java/com/mobidesk/android/ui/MobiDeskApp.kt` | Existing Compose sign-in, reset, recovery, settings and dashboard; preserve layout and add only necessary states/actions. |
| `app/src/main/AndroidManifest.xml` | Callback Activity exported; backup disabled; cleartext disabled. Preserve and verify merged manifest. |
| `app/src/test/java/com/mobidesk/android/AuthErrorMessagesTest.kt` | Two baseline unit tests. |
| `app/src/androidTest/java/com/mobidesk/android/MobiDeskSmokeTest.kt` | Two baseline Compose smoke tests incl. no fake access/self-registration affordance. |

Reinspect latest main, Gradle build files, resolved dependencies and exact configured recovery URI. Do not rely solely on this snapshot.

## 5. Required application behavior

### AuthLevel
Implement only these app-owned levels:
- `SIGNED_OUT`: no valid SDK session and no active recovery marker.
- `RECOVERY_ONLY`: SDK session exists while recovery stage is `CALLBACK_PENDING` or `PASSWORD_UPDATE_REQUIRED` (interrupted state must remain fail-closed).
- `FULLY_AUTHENTICATED`: valid SDK session and no active recovery marker; only this state may hand off to FP-02.

Use a small RecoveryStage such as `NONE`, `CALLBACK_PENDING`, `PASSWORD_UPDATE_REQUIRED`, `INTERRUPTED`. Keep Supabase SessionStatus as session truth. Do not add a broad second state machine.

**Hard invariant:** the workspace/profile/assignment repository is called only when SDK session is valid AND AuthLevel is `FULLY_AUTHENTICATED`. SDK `SessionStatus.Authenticated` alone is never sufficient.

### Recovery marker and callback
Persist only stage, a one-way SHA-256 callback-code fingerprint and bounded timestamp/expiry metadata. Never persist raw code, full URI/query, OTP, password, PKCE verifier, access/refresh token, full email or secret.

1. Verify actual recovery URI from BuildConfig and Supabase config; do not invent it.
2. Accept only ACTION_VIEW, exact scheme, exact host and exact recovery path; remove loose `contains("recovery")`.
3. Derive fingerprint from callback code in memory only.
4. Synchronously/durably commit `CALLBACK_PENDING` before invoking asynchronous `handleDeeplinks`. If the write fails, do not exchange code; fail closed.
5. While marker is pending, any SDK Authenticated event remains RECOVERY_ONLY and must clear/block workspace loading.
6. On PKCE success persist `PASSWORD_UPDATE_REQUIRED` before showing the new-password form.
7. Make delivery idempotent across onCreate/onNewIntent/recreation/process restart. Keep a bounded consumed fingerprint/tombstone long enough to prevent replay; neutralize the Activity intent after consumption.
8. If process dies in CALLBACK_PENDING, do not replay. On next launch show interrupted recovery, clear partial local recovery session safely and require a new recovery link.
9. If process dies in PASSWORD_UPDATE_REQUIRED, restore form only when SDK recovery session is valid; otherwise clear stale state and return safely to sign-in/recovery.
10. Callback failure/expired link stays fail-closed and allows a fresh request.
11. Successful password update clears active recovery state and only then publishes FULLY_AUTHENTICATED. Retain recovery-established SDK session as the current design default.
12. Recovery Cancel/Back signs out current session. On remote failure expose explicit SDK `clearSession()`; clear marker and return SIGNED_OUT with honest copy that remote revocation was not confirmed.

Use the lightest app-private persistence mechanism consistent with project conventions; do not add a database. Verify marker write durability. Do not invent expiry: inspect live recovery expiry and use it for marker retention. If unavailable, report the limitation and use conservative fail-closed cleanup.

### Test seams
- Add `AuthRepository` interface for session state/current user/sign-in/current-session sign-out/device-local clear/reset request/password update.
- Keep `SupabaseAuthRepository` as production adapter.
- Add narrow `WorkspaceRepository` interface as test seam only.
- Use small factory/injection; no Hilt/Koin or new DI framework.
- FakeAuthRepository + WorkspaceRepository spy must prove zero workspace calls during RECOVERY_ONLY.
- Preserve FP-02/FP-03 workspace query/authorization ownership.

### Sign-in/session restore
- Keep institutional email/password login and trim email whitespace only; do not alter password.
- Preserve SDK initial session restoration and prevent route flashes.
- Refresh failure/expired/not authenticated clears protected UI and returns SIGNED_OUT.
- Do not add demo/fake access or a new identity provider.

### Sign-out
- Call current-session sign-out with explicit `SignOutScope.LOCAL`.
- LOCAL is current session, not all devices.
- On remote failure show retry and “Sign out on this device” backed by `clearSession()`.
- Never claim remote/global revocation when only local state is cleared; clear protected workspace and transient password state.

### Safe error mapping
Use pinned SDK typed Auth error codes where available, not just message substring checks:
- `invalid_credentials`: generic incorrect email/password.
- `email_not_confirmed`: distinct actionable copy only after deployed GoTrue order is tested with synthetic user.
- `email_address_not_authorized`: delivery unavailable for this recipient; direct to college support without inventing contact details.
- `over_email_send_rate_limit`: wait before another email.
- `over_request_rate_limit`: distinct Auth request limit message.
- expired/missing flow state/OTP: link expired/already used; offer new request.
- network/unknown: safe fallback; never show raw provider exception.
Known/unknown reset requests must have identical generic acknowledgement.

## 6. Forbidden scope changes

No signup, invitation dispatch/activation, role management, admin workspace, RLS/profile/assignment changes, broker/VM/FreeRDP/Cloudflare/server/SSH work, new Auth coordinator/router, broad state machine, redesign, DI framework, SDK upgrade, compatibility target changes, dependency additions without justification, or live Supabase settings/database/auth-user/template/SMTP changes. No secrets in Git, docs, logs, BuildConfig or chat; only public URL + publishable/anon key may exist in the Android client.

## 7. Parent compatibility constraints

Reverify current Gradle; preserve parent baseline minSdk 29, compileSdk/targetSdk 37/37, Java/Kotlin target 11 and ABIs arm64-v8a/x86_64 unless current approved source says otherwise. Preserve Kotlin/Compose, Supabase Kotlin 3.6.0, PKCE, single Supabase client, allowBackup=false and usesCleartextTraffic=false. Do not assert session encryption at rest without inspecting resolved Settings backend and merged storage behavior.

## 8. Required tests

All four exact baseline tests are mandatory:
1. `AuthErrorMessagesTest.invalidCredentialsUseAStableSafeMessage`
2. `AuthErrorMessagesTest.backendDetailsAreNotShownToTheUser`
3. `MobiDeskSmokeTest.signInScreenDoesNotOfferFakePreviewAccess`
4. `MobiDeskSmokeTest.passwordRecoveryRequestScreenOpensWithoutSendingAnEmail`

All 24 FP01-CORE cases in `TEST_PLAN.md` must be implemented/run or explicitly marked BLOCKED/NOT RUN:
- CORE-01 cold start/no session; CORE-02 valid provisioned login; CORE-03 invalid credentials; CORE-04 restore after process restart; CORE-05 refresh failure/expiry; CORE-06 local sign-out success; CORE-07 sign-out network failure/local clear; CORE-08 marker before exchange + recovery gating; CORE-09 cancel/back; CORE-10 PKCE success/password form; CORE-11 duplicate callback/recreation; CORE-12 process death at password form; CORE-13 process death while callback pending; CORE-14 password update success/failure; CORE-15 workspace spy gating; CORE-16 malformed URI; CORE-17 duplicate taps; CORE-18 fake repository injection; CORE-19 email_address_not_authorized; CORE-20 email rate limit; CORE-21 request rate limit; CORE-22 email_not_confirmed ordering; CORE-23 known/unknown reset acknowledgement; CORE-24 missing Supabase config.
Also run applicable TalkBack/focus/font, custom URI collision, merged manifest/backup, session-storage, log/crash/analytics secret scan and real email/device checks from TEST_PLAN.md.

Run `:app:testDebugUnitTest`, `:app:connectedDebugAndroidTest` and debug assembly as supported by the actual project. Verify tests by report, not only Gradle exit code.

## 9. External release blockers — never fake

Historical discovery reported SMTP disabled; recheck live settings read-only. Custom SMTP or Send Email Hook is required for real recovery to ordinary non-team students. Local Supabase + Mailpit is valid repeatable local testing but does not prove production delivery. Real acceptance requires a synthetic non-team mailbox, exact redirect, physical Android callback, successful PKCE exchange, password update, new-password login and old-password rejection. Provider/domain/DNS/credentials are Owner-controlled; do not invent provider, paid plan or secrets. Do not change public signup toggle until Admin invitation path works; keep Email provider enabled.

## 10. Execution plan and two-step verification

1. Baseline: confirm latest main/head, Gradle/dependency/config and exact paths; run four existing tests before editing.
2. Test seam: add interfaces/factory; verify production adapters and fake/spy tests.
3. AuthLevel/marker: implement durable stage/fingerprint; prove zero workspace calls during recovery race.
4. Lifecycle: exact callback path, replay prevention, process death, cancellation, LOCAL sign-out and fallback.
5. UX/errors: typed codes, generic reset acknowledgement, rate limits and minimal Compose updates.
6. Acceptance: all baseline/core tests, build, diff, manifest, storage and secret scans.
7. External gates: local Mailpit if available; real SMTP/device tests are PASS only with actual evidence.
8. Docs: update FP-01 PROGRESS with base/source SHA, changed paths, commands, each test result, device/environment, blockers and requirement matrix.

Every phase gets two checks: (A) direct acceptance test/inspection; (B) independent diff/regression/security review. Use small commits. Continue safe work when hardware/provider access is missing, but mark gates BLOCKED/NOT RUN.

## 11. Final delivery report

Return implementation branch/base SHA; changed files; design/state/callback sequence; FP01-FR-001..018 status with evidence; all baseline and CORE-01..24 results; build/test report paths; physical device outcome; read-only Supabase setting evidence (no secrets); SMTP gate; storage/backup/logging scan; diff scope; unresolved risks. Update `PROGRESS.md`. Do not label FP-01 DONE while any MUST or acceptance gate lacks evidence.
