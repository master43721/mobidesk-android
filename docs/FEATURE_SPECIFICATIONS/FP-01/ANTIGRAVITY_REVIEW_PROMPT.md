# FP-01 Independent Review Prompt

You are the independent reviewer for FP-01 — Student Authentication and Account Recovery in `master43721/mobidesk-android`.

**Do not edit or repair source during this review.** Review the implementation branch against the latest main, normative parent PRD v1.2 and all relevant feature/product engineering files. Report findings with evidence. Make changes only after a separate explicit instruction.

## Required documents

Read latest checked-in versions of:
- `docs/MobiDesk_PRD_v1.2.pdf`, `docs/FEATURE_INVENTORY.md`, `docs/PRD.md`, `docs/TECHNICAL_SPEC.md`.
- All documents in `docs/FEATURE_SPECIFICATIONS/FP-01/`, especially PRD, TECHNICAL_DESIGN, IMPLEMENTATION_PLAN, TEST_PLAN, SECURITY_PRIVACY, SUPABASE_CONFIGURATION, DECISIONS, OPEN_QUESTIONS, TRACEABILITY and PROGRESS.
- Cross-feature `docs/API_CONTRACT.md`, `docs/DECISIONS.md`, `docs/OPEN_ITEMS.md`, `docs/IMPLEMENTATION_PLAN.md`, `docs/TEST_PLAN.md`, `docs/SECURITY_THREAT_MODEL.md`, `docs/TRACEABILITY_CHECK.md`.
- Complete implementation diff, Gradle/build files, manifest, tests and generated reports.

## Non-negotiable rules

1. No student signup UI or `supabase.auth.signUp`; College Administrator provisions students and sends activation invitations.
2. Invitation dispatch/activation and Admin RBAC are separate features, not FP-01.
3. FP-01 is sign-in/session restore/sign-out/password recovery for pre-provisioned accounts.
4. RECOVERY_ONLY never loads workspace/profile/assignment data.
5. FULLY_AUTHENTICATED is published only after normal sign-in or successful password update.
6. SDK Authenticated alone cannot prove FULLY_AUTHENTICATED.
7. Exact scheme/host/path callback, one-time idempotency; no raw code/OTP/URL/password/verifier/token in persistence/logs.
8. Recovery cancel/back signs out; remote failure offers local clear and honest wording.
9. Sign-out uses current-session LOCAL semantics; never claim global/all-device revocation.
10. Android includes only public Supabase URL and publishable/anon key.
11. Preserve Kotlin/Compose, current PKCE and pinned Supabase Kotlin 3.6.0; no new architecture rewrite.
12. Do not change Supabase signup/provider settings. The signup toggle can be disabled only after trusted Admin invitation path is proven; keep Email provider enabled.

## Review checklist

### Branch and scope
- Confirm branch is based on latest main and report base/merge-base SHA.
- List every changed path; flag unrelated work, generated files, secrets, undocumented architecture/dependency/Gradle changes.
- Verify min/compile/target SDK, Java/Kotlin target, ABI and manifest compatibility.

### Requirement traceability
For every FP01-FR-001..018 provide requirement ID, exact implementation file/symbol/line, associated test(s), evidence, status (PASS/PARTIAL/FAIL/BLOCKED/NOT TESTED) and residual gap.
Confirm FR-AUTH-01 is FP-01. FR-AUTH-02 (profile/assignment queries using user token and RLS) belongs to FP-02/FP-03 and must not be claimed done here. Do not invent requirement wording or IDs.

### Runtime/security flows
Independently trace cold start, session restoration, provisioned login, refresh failure, local sign-out, remote sign-out failure, reset request (known and unknown accounts), cold and warm recovery callbacks, callback/Auth event ordering, duplicate onCreate/onNewIntent, Activity recreation, process death in CALLBACK_PENDING, process death in PASSWORD_UPDATE_REQUIRED, cancel/back, password update success/failure, malformed URI, duplicate taps and missing Supabase config.

Use a WorkspaceRepository spy to prove zero workspace/profile/assignment calls during RECOVERY_ONLY, including the race where Supabase emits Authenticated before the deeplink success callback. Prove marker commit precedes async PKCE exchange. Verify one-way fingerprint/tombstone, callback replay protection and Activity-intent neutralization.

### Tests/build
Run all four baseline tests and every applicable FP01-CORE-01..24 case. Run `:app:testDebugUnitTest`, `:app:connectedDebugAndroidTest` and debug assembly as supported by the current project. Inspect test reports; a task not run is NOT RUN. Report emulator/device and SDK. Verify TalkBack/focus/font scaling, URI collision, merged manifest flags, session storage and secret/log scan.

### Privacy/errors
Verify typed handling for invalid_credentials, email_not_confirmed, email_address_not_authorized, over_email_send_rate_limit, over_request_rate_limit, expired/missing OTP state and network errors. Confirm generic identical response for known/unknown recovery requests. Scan UI state, saved state, preferences, logs, crash reports, analytics and git diff for PII, password, access/refresh token, callback code/OTP, PKCE verifier, raw URL and provider secrets. Ensure fallback sign-out does not claim remote revocation.

### External gates
Inspect live Supabase settings read-only only. Local Mailpit is not proof of real SMTP delivery. Production recovery acceptance requires verified SMTP/Send Email Hook and sender domain, non-team synthetic mailbox delivery, real physical-device callback, PKCE exchange, password update, new-password login and old-password rejection. Public signup remains unchanged in FP-01.

### Live drill gate and physical demonstration review (Mandatory Stage 8)
Review `docs/live-drills/FP-01/` (`EXECUTION_REPORT.md`, `DEVICE_OR_ENVIRONMENT_CHECKLIST.md`, `DEFECTS.md`).
Evaluate `LIVE_DRILL_STATUS` against allowed values: `NOT REQUESTED`, `NOT READY`, `READY FOR OWNER`, `IN PROGRESS`, `PASS`, `FAIL`, `BLOCKED`, `NOT RUN`.
Verify:
- Artifact was tested on real physical Android device (SDK 29–37), not emulator.
- All 13 physical scenarios in `DEVICE_OR_ENVIRONMENT_CHECKLIST.md` are accounted for.
- Any defect or crash is recorded in `DEFECTS.md` (no silent fixes).
- Redaction compliance: zero passwords, tokens, PKCE verifiers, or student PII in logs/screenshots.
- Owner written decision is recorded. FP cannot be marked COMPLETE or ACCEPTED without Owner sign-off and `LIVE_DRILL_STATUS = PASS`.

## Required review output

1. Verdict: APPROVE FOR LIVE DRILL / APPROVE FOR OWNER SIGN-OFF / REQUEST CHANGES / BLOCKED / NOT READY.
2. `LIVE_DRILL_STATUS` assessment: current state, blockers, and next action.
3. Executive summary separating implemented behavior from documented or unverified behavior.
4. P0–P3 findings, each with file/line, requirement, observed behavior, impact, reproduction/test evidence and proposed fix.
5. FP01-FR-001..018 traceability matrix with PASS/PARTIAL/FAIL/BLOCKED/NOT TESTED.
6. Baseline + CORE-01..24 matrix with command, exact test, environment, result and report path.
7. Security/privacy and secret-scan results.
8. Parent PRD/cross-feature consistency check (FR-AUTH-01 vs FR-AUTH-02; no signup; invitation boundary).
9. Diff/build/manifest/dependency compatibility and unrelated-change report.
10. Live-drill scenario matrix and defect audit.
11. Merge recommendation, blockers and exact closure evidence.

Be strict. A successful build or passing unit tests is NOT feature acceptance. A green fake-repository test is not proof of physical callback/process-death behavior. A local Mailpit test is not proof of production SMTP. Missing physical evidence must remain BLOCKED or NOT RUN. FP-01 cannot be accepted without a live demonstration on physical Android hardware and explicit Project Owner sign-off.
