# FP-01 Decision Register

**Version:** 0.2 — OWNER CRITIQUE REVISION  
**Date:** 4 November 2026

## 1. Verified repository and SDK facts

| ID | Topic | Evidence | Disposition |
|---|---|---|---|
| FP01-D-01 | Auth provider | Canonical app uses Supabase Auth through supabase-kt BOM 3.6.0 and auth-kt. | VERIFIED |
| FP01-D-02 | Sign-in method | Current source uses Supabase email/password sign-in; current screen asks for school email. | VERIFIED |
| FP01-D-03 | PKCE | MobiDeskApplication configures FlowType.PKCE. | VERIFIED |
| FP01-D-04 | App registration | Current Android UI has no create-account/sign-up route. This does not disable provider-level signup. | VERIFIED |
| FP01-D-05 | Session restoration | MobiDeskViewModel observes Initializing, Authenticated, RefreshFailure and NotAuthenticated. | VERIFIED |
| FP01-D-06 | Recovery implementation | Current source requests reset email, handles the app callback and updates password. | VERIFIED — happy path only |
| FP01-D-07 | Recovery state gap | Callback exchange imports an SDK Authenticated session before handleDeeplinks invokes onSessionSuccess; current Auth state observer can start workspace loading first. | VERIFIED FROM PINNED SDK SOURCE + APP SOURCE; DEFECT |
| FP01-D-08 | Recovery cancellation gap | cancelPasswordRecovery() only clears the UI boolean; it does not sign out or clear the recovery session. | VERIFIED FROM APP SOURCE; DEFECT |
| FP01-D-09 | Process-death gap | Recovery state currently lives in ViewModel memory; no durable recovery marker is present. | VERIFIED FROM APP SOURCE; DEFECT |
| FP01-D-10 | Callback replay risk | MainActivity calls handleAuthIntent from onCreate and onNewIntent; no durable one-time callback guard is present. SDK deletes the PKCE verifier after successful code exchange. | VERIFIED DESIGN GAP; runtime duplicate behavior still requires test |
| FP01-D-11 | Sign-out scope | supabase-kt 3.6.0 Auth.signOut default is SignOutScope.LOCAL. It sends current-session logout and clears the local session on success. | VERIFIED AGAINST TAG 3.6.0 |
| FP01-D-12 | Sign-out failure | AuthImpl throws on non-ignored remote errors before clearing the local session; current app shows an error and remains in authenticated state. | VERIFIED FROM PINNED SDK + APP SOURCE; DEFECT |
| FP01-D-13 | Session storage abstraction | supabase-kt 3.6.0 defaults to SettingsSessionManager, which serializes UserSession as JSON into the Settings abstraction. SDK source does not establish encryption at rest for this app configuration. | VERIFIED SERIALIZATION; ENCRYPTION/ANDROID BACKEND UNKNOWN |
| FP01-D-14 | Auth/workspace coupling | MobiDeskViewModel owns Auth and starts StudentWorkspaceRepository loading after Authenticated. | VERIFIED; REFACTOR SEAM REQUIRED |
| FP01-D-15 | Test seam | ViewModel constructs SupabaseAuthRepository itself from MobiDeskApplication; no Auth repository interface injection is present. | VERIFIED; TESTABILITY GAP |
| FP01-D-16 | Error model | AuthErrorCode in supabase-kt 3.6.0 includes EmailAddressNotAuthorized, OverEmailSendRateLimit and OverRequestRateLimit. | VERIFIED AGAINST TAG 3.6.0 |
| FP01-D-17 | Email delivery | Historical SUPABASE_DISCOVERY.md reported custom SMTP disabled. Supabase default SMTP only permits organization team-member recipients and is limited to 2 emails/hour in the documented configuration. | REPORTED HISTORICAL CONFIG + VERIFIED CURRENT PLATFORM LIMIT; STUDENT DELIVERY BLOCKED UNTIL SMTP/HOOK |
| FP01-D-18 | Parent mapping | FR-AUTH-01 owns sign-in/session restore/recovery/sign-out; FR-AUTH-02 is profile/assignment queries with user token and RLS. | OWNER-SUPPLIED EXACT PARENT TRANSCRIPTION; FR-AUTH-02 OWNED BY FP-02/03 |

## 2. Selected design defaults for v0.2

| ID | Decision | Rationale | Status |
|---|---|---|---|
| FP01-D-19 | Keep email/password as the only FP-01 sign-in method; email-only identifier. | Matches current UI and parent behavior. | PROPOSED DEFAULT |
| FP01-D-20 | No AuthSessionCoordinator or AuthDeepLinkRouter rewrite. Add a small AuthRepository interface, AuthLevel and a durable recovery marker around current ViewModel/Activity flow. | Smallest change that addresses identified defects. | PROPOSED DEFAULT |
| FP01-D-21 | Recovery cancellation signs out current LOCAL session and clears recovery state; if remote sign-out fails, offer explicit device-local clearSession fallback. | Prevents recovery-only session from entering workspace. | PROPOSED DEFAULT |
| FP01-D-22 | After successful password update, retain the recovery-established session and transition to FULLY_AUTHENTICATED. | Matches current successful-flow behavior and avoids an unnecessary second sign-in. | PROPOSED DEFAULT |
| FP01-D-23 | Keep email_not_confirmed actionable and distinct from invalid_credentials. | Current upstream GoTrue checks password validity before checking email confirmation; the user already knows the password. Verify against deployed service. | PROPOSED DEFAULT — DEPLOYED VERSION TEST REQUIRED |
| FP01-D-24 | Show specific messages for email_address_not_authorized, over_email_send_rate_limit and over_request_rate_limit. | Supabase SDK exposes typed codes; generic fallback is not useful for recoverable rate/provider failures. | REQUIRED |
| FP01-D-25 | Configure custom SMTP or Send Email Hook before student recovery is accepted for pilot. | Built-in service restrictions make ordinary student recovery unusable. | RELEASE BLOCKER |
| FP01-D-26 | Keep current PKCE custom URI for the pilot; assess verified HTTPS App Links before broader production distribution. | Avoid unnecessary redirect rewrite in FP-01 while testing scheme collision risk. | PROPOSED DEFAULT |
| FP01-D-27 | No MFA/passkeys/social login in R1 unless college policy mandates one. | Keeps pilot scope bounded. | PROPOSED DEFAULT |
| FP01-D-28 | Standard Android password-manager/autofill support is allowed; MobiDesk never stores user passwords. | User convenience without app-managed password persistence. | PROPOSED DEFAULT |
| FP01-D-29 | Keep existing session lifetime/refresh project settings; do not invent client-side expiry. | Auth policy belongs to project/operator configuration. | PROPOSED DEFAULT |

## 3. Remaining Owner decisions

Only these matters remain open for this draft:
1. Confirm public signup is disabled at project level if the roster/institution is the only provisioning authority.
2. Confirm the institution does not require MFA for the R1 student pilot.
3. Select a custom SMTP provider or Send Email Hook and the authorized sender/domain. Do not place provider credentials in GitHub.
4. Confirm that retaining the Auth session after successful recovery is desired.
5. Confirm the acceptable sign-out fallback wording: local device sign-out may not confirm remote token revocation.
6. Approve custom URI for pilot, with the redirect-collision review as a release check.

## 4. Decision change control

Record any reversal with a new decision ID, owner/date, affected requirements, implementation, tests and rollback. Do not delete superseded decisions.


## 5. Later Owner direction incorporated from the account-model decision (4 Nov 2026)

| ID | Decision | Evidence / implementation consequence | Status |
|---|---|---|---|
| FP01-D-30 | Students must not self-register. The College Administrator provisions student Auth identities and sends activation invitations. | Current product-level decision in `docs/FEATURE_INVENTORY.md` and `docs/ADMIN_ACCOUNT_MODEL.md`. FP-01 must not add signup or invitation-management code. | OWNER CONFIRMED |
| FP01-D-31 | Disable project-level public signup only after the trusted Admin invitation path is implemented and tested; keep the Email provider enabled. | The Admin account model and PR #4 specify this sequence. FP-01 may verify the current setting read-only but must not change live Supabase configuration. | OWNER CONFIRMED SEQUENCE; LIVE ACTION PENDING |
