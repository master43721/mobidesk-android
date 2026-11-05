# FP-02 Implementation Plan — Student Dashboard and Workspace

**Version:** 0.2 · **Date:** 5 November 2026  
**Estimate basis:** one developer with AI assistance, focused engineering hours  
**Status:** ready for implementation after dependency gates; not a claim of work completed.

## 1. Delivery approach

Keep the current Kotlin/Compose dashboard and evolve it. FP-02 owns the Android StatusRepository and its contract tests. FP-04 owns the server endpoint/probe. FP-03 owns authoritative assignment resolution and safe label. FP-01 owns AuthLevel. FP-05 owns Windows credentials and the no-argument handoff destination.

Do not substitute mocks for a missing upstream contract. Use fakes only in deterministic tests.

## 2. FP-02 task estimate

The min/max totals below are the arithmetic sum of the rows: **17–27 focused hours**. They exclude FP-03/FP-04 backend work, endpoint deployment, Windows guest provisioning and physical-device availability.

| Task | Work | Min–max | Exit evidence |
|---|---|---:|---|
| FP02-001 | Reconfirm main/base SHA, FP-01 AuthLevel integration and dashboard baseline | 1–1.5 h | Exact FULLY_AUTHENTICATED source path and regression baseline |
| FP02-002 | Freeze FP-03/04 screen and API contracts, including assignmentTag | 1.5–2.5 h | Reviewed DTOs, tag semantics and no-selector contract |
| FP02-003 | Implement FP-02 Android StatusRepository: JWT, 8s timeout, cancellation, parsing, 429/Retry-After and cooldown | 3–4.5 h | Fake-server contract tests and no sensitive request fields |
| FP02-004 | Implement parallel session-scoped workspace/status loading and tag reconciliation | 2–3 h | Concurrency, account-switch and mismatch tests |
| FP02-005 | Implement pure monotonic freshness and separate button-availability/handoff predicates | 1.5–2.5 h | Fake monotonic clock, full truth table and wall-clock-jump tests |
| FP02-006 | Implement S-02/S-03 Compose UI, state copy, manual refresh and Connect interaction | 2.5–4 h | All lifecycle/transport/refresh states rendered |
| FP02-007 | Implement assignment/error/settings/sign-out and Dock placeholder states | 1–1.5 h | Exact parent copy, FP-01 delegation and inert Dock |
| FP02-008 | Add string resources, semantics, TalkBack announcements and scaling fixes | 0.5–1.5 h | Accessibility checklist and reviewed en-IN resources |
| FP02-009 | Unit, Compose, contract, negative, race, timeout and rate-limit tests | 3–4 h | All applicable CORE cases pass with reports |
| FP02-010 | Security/diff review, traceability, docs and Owner acceptance report | 1–2 h | Clean scoped diff and evidence ledger |

**Arithmetic:** minimum 17 h; maximum 27 h.

## 3. External dependency work (not included in 17–27h)

| Dependency | Expected work | Estimate | Required before |
|---|---|---:|---|
| FP-01 | Identify/merge completed AuthLevel source commit; prove recovery-only is not workspace-authenticated | 0.5–1.5 h integration/verification | FP02-001 |
| FP-03 | Typed zero/one/invalid result, safe label and shared-resolver assignmentTag | 4–6 h | FP02-002/004 |
| FP-04 | Add assignmentTag, no-reused-sample behavior, HTTP 429 + Retry-After, endpoint and probe tests/deployment | 6–10 h | FP02-002/003/004 |
| FP-05 | Confirm stable no-argument navigation callback to Windows credentials | 1–2 h | FP02-006 |
| FP-06 | Grant-time assignment/readiness revalidation | Separate FP-06 scope | End-to-end session acceptance |

These are planning estimates, not completed work. Broker DNS/TLS, server access, RDP guest configuration and deployment wait time are separate critical-path risks.

## 4. Date-based execution sequence

| Date (2026) | Work | Gate / output |
|---|---|---|
| 5 Nov | Correct spec, parent amendment proposal, API addendum, Codespaces setup | v0.2 review branch; no product code claim |
| 6 Nov | Verify FP-01 commit/AuthLevel; agree FP-03 tag/result; agree FP-04 API addendum | G1 client contract freeze or explicit BLOCKED |
| 7 Nov | Build StatusRepository and fake-server contract tests | FP02-003 |
| 8 Nov | Parallel loader, session generation, tag mismatch recovery, monotonic clock | FP02-004/005 |
| 9 Nov | S-02/S-03 status, refresh and Connect flows; state resources | FP02-006/007/008 |
| 10 Nov | Complete CORE tests, negative/security tests, regression build | G3 candidate evidence |
| 11 Nov | Independent review, accessibility, Owner acceptance and integration gate | FP-02 DONE only if upstream gates pass |

Reforecast if FP-01/03/04/05 dependencies are not ready on their dates. Do not remove a safety gate to preserve a calendar date.

## 5. Phases and stop conditions

### Phase 0 — Source and environment
- Work only in master43721/mobidesk-android.
- Preserve the existing Codespace worktree; inspect and commit/stash its current uncommitted changes before changing branches.
- Fetch latest main and record base SHA.
- Verify the FP-01 FULLY_AUTHENTICATED contract and recovery-only behavior.
- Run existing unit tests and assemble the debug baseline.

**Stop** if AuthLevel is absent from the source branch used for FP-02.

### Phase 1 — Freeze contracts
- Freeze FP-03 typed assignment result, safe label and assignmentTag source.
- Freeze FP-04 status DTO with assignmentTag, no target selector, no cached prior sample and 429 Retry-After behavior.
- Freeze the FP-05 no-argument navigation callback.
- Confirm approved HTTPS broker base URL source; never hard-code it.

**Stop** on guessed field names, unreviewed labels or undocumented API semantics.

### Phase 2 — StatusRepository
- Add the Android client in FP-02.
- Add serialization, fake HTTP server and contract tests.
- Enforce 8-second timeout, cancellation, 2-second tap debounce and server Retry-After.
- Prove current JWT/no-selector/no-private-fields.

### Phase 3 — State composition
- Parallelize profile/assignment and status requests after AuthLevel.
- Capture current Auth session generation; ignore late results from older generations.
- Pair results by assignmentTag; reload the pair once on mismatch; fail with ASSIGN-02 on repeated mismatch.
- Add monotonic freshness and exact eligibility functions.

### Phase 4 — Compose and accessibility
- Evolve S-02, add S-03 detail, all enum copy, freshness, loading and service error states.
- Apply the single Connect policy in PRD.md §9.
- Keep Dock informational and delegate account actions to FP-01.

### Phase 5 — Verification and acceptance
- Run baseline, all applicable CORE tests, fake-server contracts and UI tests.
- Run account-switch, timeout, 429, stale and tag-mismatch negatives.
- Run privacy/static scans and review complete diff.
- Verify TalkBack, 1.3x and 1.5x font scaling.
- Record commit SHA, exact Gradle tasks, test reports and device/emulator.

## 6. Verification A / Verification B

Every phase has two independent proofs:
- A: direct test/contract showing the requirement.
- B: independent code/diff review checking adjacent negative states and data boundaries.

Examples: monotonic 15-second boundary test plus wall-clock-jump test; assignmentTag mismatch test plus request/DTO scan; status 429 fake-server test plus no-auto-retry inspection; Connect truth table plus handoff callback count assertion.

## 7. Definition of Done

All FP02-FR-001..018 are implemented and tested; FP-01 AuthLevel and FP-03/04/05 contracts are integrated; all applicable CORE tests pass; no stale or mismatched sample can cause handoff; error copy is exact; accessibility/privacy review passes; no unrelated FreeRDP, broker, Supabase or VM code is introduced; PROGRESS.md contains reproducible evidence.

A debug APK or fake status is not acceptance.
