# FP-01 Implementation Plan — Focused Authentication Hardening

**Version:** 0.2 — OWNER CRITIQUE REVISION  
**Date:** 4 November 2026  
**Repository:** master43721/mobidesk-android  
**Status:** Planning draft; no application code changed by this documentation revision.

## 1. Delivery approach

FP-01 is approximately 250 lines of existing Auth logic plus UI and project settings. Do not rewrite it into a new authentication architecture.

Preserve the current Supabase Auth client, Compose screens and repository methods. Implement only the seams and fixes required by the defects:
- AuthRepository interface/injection seam.
- AuthLevel and durable recovery marker.
- Recovery cancel/process-death/callback replay fixes.
- Sign-out failure fallback.
- Typed Auth error mapping for mail authorization and rate limits.
- Custom SMTP/Send Email Hook readiness.
- Focused tests.

Do not add a coordinator, a new router package, three new screens or a DI framework in FP-01.

## 2. Work packages and estimated effort

Estimates are focused engineering hours for one engineer with AI assistance. They exclude waiting for SMTP provider/domain setup or Owner approvals. Parent PRD v1.2 dates are the planning window; these are ranges, not delivery promises.

| Task | Work | Estimate | Target window | Dependency / exit evidence |
|---|---|---:|---|---|
| FP01-001 | Correct FR-AUTH traceability and approve FP-01 boundary | 0.5 h | 4 Nov | Parent mapping: FR-AUTH-01 belongs FP-01; FR-AUTH-02 belongs FP-02/03. |
| FP01-002 | Verify supabase-kt 3.6.0 signOut LOCAL, clearSession, PKCE order and session manager source | 1.5 h | 4 Nov | SDK source references recorded; storage backend/encryption finding separated. |
| FP01-003 | Add AuthRepository + WorkspaceRepository interfaces and lightweight ViewModel factory/test seam | 1.5–2 h | 5 Nov | Production adapters remain unchanged; FakeAuthRepository and WorkspaceRepository spy can instantiate ViewModel and prove zero workspace calls during RECOVERY_ONLY. |
| FP01-004 | Add AuthLevel + durable recovery marker and gate workspace loading | 2–3 h | 5–6 Nov | Authenticated SDK event during recovery remains RECOVERY_ONLY; no workspace call. |
| FP01-005 | Fix recovery cancel/back and sign-out network-failure exit | 1.5–2 h | 6 Nov | Cancel clears recovery session; remote sign-out failure offers explicit device-local clear. |
| FP01-006 | Make recovery callback idempotent across Activity recreation/process restart | 2–3 h | 6–7 Nov | Pending/consumed callback cannot be exchanged twice; interrupted recovery fails closed. |
| FP01-007 | Map typed Auth errors and add email-send/request rate-limit UX | 1–1.5 h | 7 Nov | email_address_not_authorized, over_email_send_rate_limit and over_request_rate_limit have specific tested messages. |
| FP01-008 | Resolve provider/signup/confirmation/redirect/expiry/password/rate settings | 0.5–1 h | 7 Nov | Read-only settings evidence and Owner decision recorded. |
| FP01-009 | Configure custom SMTP or Send Email Hook and verify sender/domain | 2–4 h once provider/domain credentials are ready | 8–10 Nov | A non-team synthetic student address receives confirmation and recovery emails. Blocked until provider/domain selection and secure credentials are supplied. |
| FP01-010 | Run the must-run pilot suite and fix failures | 3–4 h | 10–12 Nov | Baseline 4 tests plus core Auth/recovery tests all pass; local/build evidence recorded. |
| FP01-011 | Accessibility, callback collision and storage review | 1.5–2.5 h | 12 Nov | TalkBack checklist, competing-app URI test and session storage recommendation. |
| FP01-012 | Traceability, release evidence and Owner acceptance | 1 h | 13 Nov | Every MUST has test evidence; Owner accepts remaining policy decisions. |

**Estimated engineering effort:** approximately 16–22 focused hours excluding external email-provider/DNS wait time. SMTP setup itself is estimated only after the Owner has selected a provider and supplied the sender/domain configuration securely.

## 3. Implementation sequence

### Phase A — Verify and establish seams
1. Re-run the existing baseline test suite.
2. Add AuthRepository and WorkspaceRepository interfaces with production/fake adapters through a small factory.
3. Confirm the exact Auth sign-out and session-storage behavior from the resolved 3.6.0 dependency.
4. Add AuthLevel and the durable recovery marker model.

### Phase B — Fix recovery and sign-out defects
1. Persist CALLBACK_PENDING before starting asynchronous PKCE handling.
2. Prevent workspace loading while recovery marker exists.
3. Persist PASSWORD_UPDATE_REQUIRED after callback success.
4. Restore the recovery form after process death when the stage is ready.
5. On interrupted callback, do not replay the consumed code; clear partial state and request a new link.
6. Cancel/back signs out the recovery session and returns to SIGNED_OUT.
7. If server sign-out fails, allow explicit SDK clearSession() and explain remote revocation was not confirmed.
8. Clear recovery marker and publish FULLY_AUTHENTICATED only after successful password update.

### Phase C — Provider configuration and error UX
1. Recheck live Supabase Auth settings.
2. Decide whether provider-level public signup must be disabled.
3. Configure custom SMTP or Send Email Hook.
4. Verify sender domain and recovery template/redirect.
5. Map Auth error codes for recipient authorization and both email/request rate limits.
6. Run the GoTrue valid-password/unconfirmed-email behavior test.

### Phase D — Focused acceptance
1. Baseline regression (4 existing tests).
2. Core tests listed in TEST_PLAN.md.
3. Mail delivery and real callback test in an approved test environment.
4. Physical Android callback/recreation/TalkBack tests.
5. Storage/backup/log scan.
6. Owner accepts remaining decisions and all MUST evidence.

## 4. Proposed source-file change map

| Current file | Target change |
|---|---|
| app/src/main/java/com/mobidesk/android/data/MobiDeskRepositories.kt | Introduce AuthRepository interface; SupabaseAuthRepository implements it. Put the existing StudentWorkspaceRepository behind a WorkspaceRepository interface for a fake/spy only. Keep workspace query behavior owned by FP-02/FP-03. |
| app/src/main/java/com/mobidesk/android/MobiDeskViewModel.kt | Inject AuthRepository through a lightweight factory/seam; add AuthLevel/recovery marker gating; never load workspace while RECOVERY_ONLY; fix cancel/sign-out failure paths. |
| app/src/main/java/com/mobidesk/android/MainActivity.kt | Exact callback path validation; persist callback marker before async SDK call; de-duplicate initial/re-delivered callback. |
| app/src/main/java/com/mobidesk/android/MobiDeskApplication.kt | Keep one Supabase client; expose repository factory if needed; no new Auth coordinator. |
| app/src/main/java/com/mobidesk/android/ui/MobiDeskApp.kt | Keep existing screens; add interrupted-recovery and sign-out-failure actions/copy. |
| app/src/test/java/com/mobidesk/android/AuthErrorMessagesTest.kt | Typed error-code tests and rate-limit/provider messages. |
| app/src/androidTest/java/com/mobidesk/android/MobiDeskSmokeTest.kt | Keep current smoke tests; add navigation and recovery gating tests. |
| Proposed app/src/main/java/com/mobidesk/android/data/RecoveryStateStore.kt | Small durable marker store; non-secret stage/fingerprint only. Exact persistence mechanism selected after current dependency/style review. |

No new package hierarchy or UI redesign is planned.

## 5. Test-environment dependency

No dedicated Supabase staging project is evidenced in the repository/docs. Do not write tests that silently assume one exists.

- Unit/ViewModel/UI tests inject FakeAuthRepository and a spy WorkspaceRepository; no live Supabase credentials are required.
- For provider integration, the Owner must nominate either an isolated local Supabase stack or a separate hosted test project and synthetic accounts.
- Real email recovery tests require custom SMTP/Send Email Hook and a non-team test mailbox.
- Do not run destructive Auth tests against real student accounts or production data.

Creating a hosted Supabase project/branch may incur charges and requires separate Owner approval. This implementation plan does not create one.

## 6. Definition of done

- FR-AUTH-01 behavior is preserved and the four baseline tests pass.
- Recovery-only sessions never load the workspace.
- Recovery cancel/back signs out the recovery session.
- Process death and callback replay are handled fail-closed.
- Sign-out network failure has an explicit local exit.
- Rate-limit/provider delivery errors are actionable.
- Custom SMTP/Send Email Hook sends to a synthetic non-team account.
- FP-02 receives FULLY_AUTHENTICATED only after normal sign-in or completed password update.
- The four baseline tests plus the mandatory core test set pass.
- Storage/backup behavior is documented; any encryption requirement has an implementation decision.
- Every remaining project-policy decision is recorded; no hidden UNKNOWN is labelled PASS.
- Owner signs off before the FP-01 PR is merged.

No source implementation is authorized by this plan until the Owner approves the v0.2 specification.
