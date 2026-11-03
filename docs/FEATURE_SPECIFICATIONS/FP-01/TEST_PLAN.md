# FP-01 Test Plan — Student Authentication and Account Recovery

**Version:** 0.1 — REVIEW DRAFT  
**Date:** 3 November 2026  
**Status:** Test design only. No tests in this document are claimed as executed.

## 1. Test objectives

Prove that FP-01:
- authenticates only through the approved Supabase Auth flow;
- restores a valid session correctly;
- responds correctly to Auth state changes;
- supports deliberate sign-out;
- completes recovery through a verified callback;
- protects passwords, tokens and recovery callback material;
- does not leak account existence through user-facing responses;
- hands authenticated identity to FP-02 without claiming student/VM authorization.

## 2. Test environments and test data

| Environment | Purpose | Rules |
|---|---|---|
| Local unit tests | Validation, state transitions, error mapping | No live credentials or real student records. |
| Compose instrumentation | Screen routes, enabled/disabled controls, accessibility semantics | Inject fake Auth repository; no production Supabase. |
| Supabase staging | Real sign-in, refresh, reset email, PKCE callback and sign-out | Synthetic test accounts only; staging Auth configuration must match approved policy. |
| Physical Android device | App launch/restore, actual mail-app/deep-link handoff, process recreation | Record model/API/ABI and build hash in private test report. |
| Release artifact inspection | Secret/log/configuration checks | No live user credentials in artifact or test log. |

Do not use current student accounts, real student names/emails or production credentials in automated tests. Do not write raw JWTs, recovery URLs or PKCE codes into artifacts.

## 3. Unit tests

| Test ID | Scenario | Expected result |
|---|---|---|
| FP01-UT-001 | Email has outer spaces | Email is trimmed before Auth call. |
| FP01-UT-002 | Email has internal spaces | Local validator rejects or provider safely rejects; no silent mutation. |
| FP01-UT-003 | Password includes leading/trailing spaces | Password is passed unchanged. |
| FP01-UT-004 | Empty email | No Auth network call; actionable validation. |
| FP01-UT-005 | Empty password | No Auth network call; actionable validation. |
| FP01-UT-006 | Valid sign-in SDK call returns but Auth flow has not emitted Authenticated | UI remains in signing-in/awaiting-auth state; no premature navigation. |
| FP01-UT-007 | Authenticated event emitted | App transitions to Authenticated and emits identity to FP-02. |
| FP01-UT-008 | Initializing event | Restoration UI remains visible. |
| FP01-UT-009 | NotAuthenticated event | Signed-out route shown. |
| FP01-UT-010 | RefreshFailure event | Clear protected UI and route to sign-in with safe expiry behavior. |
| FP01-UT-011 | Duplicate sign-in tap | Only one in-flight operation. |
| FP01-UT-012 | Reset request duplicate tap | Only one in-flight operation. |
| FP01-UT-013 | Password confirmation mismatch | Update API is not called. |
| FP01-UT-014 | Password shorter than approved client minimum | Update API is not called; approved validation shown. |
| FP01-UT-015 | Password policy rejects an otherwise locally valid password | Safe provider policy message; no raw internal details. |
| FP01-UT-016 | Unknown provider exception includes token/host | Safe fallback contains neither. |
| FP01-UT-017 | Error mapper receives invalid credentials and unconfirmed-email errors | Output follows Owner-approved non-enumeration policy. |
| FP01-UT-018 | Sign-out completes locally | Auth state changes to SignedOut; protected in-memory state cleared. |
| FP01-UT-019 | Supabase configuration missing | Client reports ConfigUnavailable; no network request. |
| FP01-UT-020 | Authenticated user is emitted | Auth layer does not call VM/profile repository in its own responsibility boundary. |

## 4. UI and navigation tests

| Test ID | Scenario | Expected result |
|---|---|---|
| FP01-UI-001 | Signed-out initial state | Sign-in screen displayed. |
| FP01-UI-002 | Session restoring | Loading state shown; no sign-in/dashboard flash. |
| FP01-UI-003 | Password visibility toggle | Password visibility changes without persisting value. |
| FP01-UI-004 | Sign-in operation busy | Inputs/buttons disabled and progress shown. |
| FP01-UI-005 | Forgot password action | Reset request screen opens. |
| FP01-UI-006 | Recovery request busy | Duplicate send prevented. |
| FP01-UI-007 | Recovery request generic success | Same generic acknowledgement copy displayed. |
| FP01-UI-008 | Password recovery callback not yet verified | New-password fields cannot be submitted. |
| FP01-UI-009 | Recovery ready | New-password and confirmation controls are enabled. |
| FP01-UI-010 | Password mismatch | Error shown; update disabled/not called. |
| FP01-UI-011 | Missing Supabase configuration | Sign-in and reset actions disabled; no demo/preview bypass. |
| FP01-UI-012 | No app registration route | No Create account/sign-up screen reachable. |
| FP01-UI-013 | Back navigation from reset request | Returns to sign-in without stale success/error message. |
| FP01-UI-014 | TalkBack reads fields/buttons/errors | Labels and live announcements are meaningful. |

## 5. Supabase staging integration tests

Use synthetic student identities that are created through the approved institution provisioning process.

| Test ID | Scenario | Expected result |
|---|---|---|
| FP01-INT-001 | Correct provisioned email/password | Supabase Auth session becomes Authenticated. |
| FP01-INT-002 | Wrong password | Denied; approved generic message. |
| FP01-INT-003 | Nonexistent email | Denied; outward behavior is not distinguishable from invalid credentials, subject to provider response normalization. |
| FP01-INT-004 | Email unconfirmed | Denied; no account-state disclosure beyond Owner-approved behavior. |
| FP01-INT-005 | Existing session after process restart | Session restored if still valid and refreshable. |
| FP01-INT-006 | Expired/invalid refresh session | Authenticated state cleared and sign-in shown. |
| FP01-INT-007 | Network unavailable during initial restore | Safe state; no false authenticated state. |
| FP01-INT-008 | Network unavailable during sign-in | Safe retry, no duplicate operation or credential log. |
| FP01-INT-009 | Sign-out while online | Local state cleared and configured server-side sign-out scope behaves as documented. |
| FP01-INT-010 | Sign-out while offline | No false claim of remote revocation; local behavior matches Owner-selected policy. |
| FP01-INT-011 | Recovery for existing synthetic account | Generic acknowledgement and recovery email delivered to controlled mailbox. |
| FP01-INT-012 | Recovery for nonexistent synthetic address | Same user-visible acknowledgement; no disclosure. |
| FP01-INT-013 | Open valid recovery email on Android | Correct app callback invoked and SDK verifies session. |
| FP01-INT-014 | Update password from verified recovery session | New password accepted and expected session outcome occurs. |
| FP01-INT-015 | Sign in with new password | Success. |
| FP01-INT-016 | Sign in with old password | Failure. |
| FP01-INT-017 | Recovery email delivery after template/config change | Actual arrival verified; no success inferred from API response alone. |
| FP01-INT-018 | Project provider-level signup setting | Observed state matches Owner-approved account provisioning policy. |

## 6. Deep-link and recovery negative tests

| Test ID | Attack/failure | Expected result |
|---|---|---|
| FP01-DL-001 | Wrong URI scheme | Not accepted as Auth callback. |
| FP01-DL-002 | Wrong URI host | Not accepted as Auth callback. |
| FP01-DL-003 | Unexpected path | Not treated as recovery. |
| FP01-DL-004 | Missing callback code/token material | Recovery fails safely. |
| FP01-DL-005 | Expired recovery link | RecoveryFailed; new request offered. |
| FP01-DL-006 | Replayed recovery link/code | SDK rejects; no second password update. |
| FP01-DL-007 | Invalid PKCE state/verifier | SDK rejects; no RecoveryReady state. |
| FP01-DL-008 | Callback contains arbitrary user ID/email query fields | Those fields are not used as identity proof. |
| FP01-DL-009 | Two callback intents arrive in quick succession | One valid callback handled; no duplicate state mutation. |
| FP01-DL-010 | App process cold-starts from recovery link | SDK callback works without relying on pre-existing Activity/ViewModel state. |
| FP01-DL-011 | App already running receives new callback via onNewIntent | Correct callback path is handled once. |
| FP01-DL-012 | Another app claims custom URI scheme | Risk is documented; actual installed-device behavior matches selected redirect architecture. |

## 7. Security and privacy tests

| Test ID | Test | Expected result |
|---|---|---|
| FP01-SEC-001 | Search source and APK for service-role/secret key pattern | No service-role/secret key. |
| FP01-SEC-002 | Inspect BuildConfig and resource packaging | Only client-safe publishable/anon key and public project URL. |
| FP01-SEC-003 | Logcat during valid/invalid sign-in | No password, token, Authorization header or raw exception. |
| FP01-SEC-004 | Logcat/crash data during recovery | No full callback URI/query, PKCE code/verifier or token. |
| FP01-SEC-005 | Inspect SavedState/preferences/database | No password or recovery code stored. |
| FP01-SEC-006 | Inspect session persistence backend used by pinned SDK | Document storage location, encryption and backup behavior; no assumption from SDK auto-restore alone. |
| FP01-SEC-007 | Backup/restore check | Auth secrets are not included in unsupported app backup/export paths. |
| FP01-SEC-008 | Existing vs nonexistent recovery address response | No account-existence signal in app UI. |
| FP01-SEC-009 | Existing vs unconfirmed vs incorrect password messages | Match approved non-enumerating policy. |
| FP01-SEC-010 | Callback from wrong scheme/host/path | Rejected. |
| FP01-SEC-011 | User changes account/session while prior request active | Stale result cannot restore previous account's UI state. |
| FP01-SEC-012 | Rapid sign-in/sign-out requests | No race leaves wrong identity visible. |

## 8. Accessibility and usability tests

- Email field uses email keyboard and correct label.
- Password field uses password keyboard and secure visual transformation by default.
- Visibility toggle exposes a meaningful state description.
- Sign-in/reset/update actions announce busy state and are disabled while pending.
- Error and success messages are announced and remain visible long enough to be understood.
- Focus returns to the relevant input after recoverable validation errors.
- No information is conveyed by color alone.
- Layout supports font scaling and smaller supported device widths.

## 9. FP-01 release/acceptance gate

| Gate | Pass rule |
|---|---|
| G0 Requirements | Owner-approved PRD and parent FR-AUTH mapping. |
| G1 SDK behavior | Storage, sign-out and deep-link behavior verified on exact pinned SDK. |
| G2 Sign-in/session | All MUST unit/UI/INT session cases pass. |
| G3 Recovery | Real staging email, valid callback, password update, old/new credential checks pass. |
| G4 Security | All critical negative callback, enumeration, credential and secret-scan tests pass. |
| G5 Accessibility | Keyboard/TalkBack/font scaling reviewed. |
| G6 Evidence | Commit SHA, test reports, device/API, environment and reviewer are recorded. |

A test that is not run must be marked NOT RUN, not PASS. Emulator-only verification is not evidence for actual email-app deep-link handoff on the nominated physical device.
