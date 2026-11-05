# FP-02 Implementation Plan — Student Dashboard and Workspace

**Version:** 0.2 · **Date:** 5 November 2026  
**Estimate:** focused engineering hours for one developer with AI assistance.

## 1. Delivery approach

Preserve the native Kotlin/Compose application. FP-02 owns the Android StatusRepository and client tests; FP-04 owns endpoint/probe. FP-03 owns authoritative assignment and label. FP-01 owns AuthLevel. FP-05 owns Windows credentials and handoff destination. Missing upstream contracts are BLOCKED, not replaced with production mocks.

## 2. FP-02 estimate

The rows sum to **17–27 hours**. External dependencies and service deployment are excluded.

| Task | Work | Estimate | Exit evidence |
|---|---|---:|---|
| FP02-001 | Reconfirm source SHA, FP-01 AuthLevel integration and baseline | 1–1.5h | Exact FULLY_AUTHENTICATED path; regression baseline |
| FP02-002 | Freeze FP-03/04 contract including assignmentTag | 1.5–2.5h | Reviewed models, tag source and no-selector contract |
| FP02-003 | Android StatusRepository: JWT, timeout, cancellation, DTO, 429/Retry-After | 3–4.5h | Fake-server contract tests |
| FP02-004 | Parallel loading, session generation and tag reconciliation | 2–3h | Parallel, account-switch and mismatch tests |
| FP02-005 | Monotonic freshness, local lifecycle-aware age timer and separate button/handoff predicates | 1.5–2.5h | Fake clock, 15s boundary, zero-network stale transition and wall-clock-jump tests |
| FP02-006 | Compose S-02/S-03, state copy, Refresh and Connect action | 2.5–4h | All states rendered |
| FP02-007 | Assignment/error/Settings/sign-out/Dock states | 1–1.5h | Exact parent copy; FP-01 delegation |
| FP02-008 | en-IN resources, TalkBack, semantics and font scaling | 0.5–1.5h | Accessibility checklist |
| FP02-009 | Unit, Compose, contract, race, timeout, rate-limit/security tests | 3–4h | CORE suite reports |
| FP02-010 | Diff/security review, traceability and acceptance report | 1–2h | Scoped clean diff and evidence ledger |

**Arithmetic:** minimum 17h; maximum 27h.

## 3. Separate dependency effort

| Dependency | Estimate | Required before |
|---|---:|---|
| FP-01 identify/merge AuthLevel source and verify recovery-only gate | 0.5–1.5h | FP02-001 |
| FP-03 typed singleton result, safe label, shared assignmentTag | 4–6h | FP02-002/004 |
| FP-04 tag, request-fresh status, 429/Retry-After, endpoint/probe tests | 6–10h | FP02-002/003/004 |
| FP-05 no-argument navigation callback | 1–2h | FP02-006 |
| FP-06 grant-time assignment/readiness validation | Separate FP-06 scope | End-to-end session |

These are estimates, not completed work. Broker DNS/TLS, server access, Windows guest config and deployment time are additional risks.

## 4. Dated sequence

| Date (2026) | Work | Gate |
|---|---|---|
| 5 Nov | Correct spec, parent proposal, API draft and Codespaces setup | v0.2 review branch |
| 6 Nov | Verify FP-01; agree FP-03 tag/result and FP-04 API | G1 contract freeze or BLOCKED |
| 7 Nov | StatusRepository and fake-server contracts | FP02-003 |
| 8 Nov | Parallel loader, tag recovery and monotonic time | FP02-004/005 |
| 9 Nov | Compose states, Refresh/Connect, resources | FP02-006/007/008 |
| 10 Nov | CORE suite, negatives, security and regression build | G3 candidate evidence |
| 11 Nov | Independent review, accessibility and Owner acceptance | FP-02 DONE only if upstream gates pass |

Reforecast on dependency slip; never remove a safety gate to keep a date.

## 5. Phases and stop conditions

**Phase 0 — source:** Work only in master43721/mobidesk-android. Preserve the existing Codespace worktree; inspect and commit/stash before branch switch. Record latest main SHA. Verify FULLY_AUTHENTICATED and recovery-only behavior. Run existing unit tests and baseline assemble. Stop if AuthLevel is missing.

**Phase 1 — contracts:** Freeze FP-03 typed result/label/tag, FP-04 request-fresh status/tag/429/Retry-After/no-selector DTO, FP-05 no-argument callback, approved HTTPS broker URL source. Stop on guessed fields or hard-coded endpoints.

**Phase 2 — repository:** Add FP-02 Android StatusRepository, fake server, 8s deadline, cancellation, 2s local debounce and Retry-After tests. Prove bearer/no selector/no private fields.

**Phase 3 — state:** Parallelize workspace/status after AuthLevel. Ignore late old-session responses. Pair by assignmentTag; reload both once on mismatch; second mismatch = ASSIGN-02. Implement monotonic freshness, a local-only lifecycle-aware age timer (no HTTP polling), and pure predicates.

**Phase 4 — Compose:** Add complete S-02/S-03 states/copy. Apply only PRD.md §6 Connect rule. Dock stays inert and account actions delegate to FP-01.

**Phase 5 — verification:** Run unit/Compose/contract/negative tests; timeout/429/stale/tag mismatch/account-switch tests; privacy scan; TalkBack/font-scale. Record SHA, exact Gradle tasks, reports and device.

## 6. Two independent verifications

For each phase require A: direct test/contract, and B: independent diff/code review of adjacent negative states and data boundaries. Examples: 15s test + wall-clock jump; assignmentTag mismatch test + DTO scan; 429 fake-server test + no-auto-retry review; Connect truth-table test + single callback assertion.

## 7. Definition of Done

FR-001..018 implemented and tested; AuthLevel and FP-03/04/05 contracts integrated; applicable CORE tests pass; no stale/mismatched status can hand off; exact copy and accessibility/privacy review pass; no unrelated FreeRDP/broker/Supabase/VM code; PROGRESS.md contains reproducible evidence. A debug APK or mock Ready is not acceptance.
