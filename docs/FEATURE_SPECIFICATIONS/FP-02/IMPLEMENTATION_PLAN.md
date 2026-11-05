# FP-02 Implementation Plan — Student Dashboard and Workspace

**Version:** 0.1 — source-grounded Owner review draft  
**Date:** 4 November 2026  
**Status:** Planning draft; no application code changed.

## 1. Delivery approach

Implement the workspace as a focused evolution of the existing Compose dashboard. Do not redesign the whole application or duplicate FP-03/FP-04 backend behavior.

The work is dependent on three upstream contracts:
- FP-01: a stable fully-authenticated student session gate.
- FP-03: an exactly-one, student-safe profile/assignment presentation result.
- FP-04: an authenticated VM lifecycle/RDP status client that consumes the current broker API contract.

If any dependency is missing, complete the safe UI/domain/test seam work, but do not fake data or ship a false-positive Ready state.

## 2. Work packages and effort estimate

Estimates are focused engineering hours for one developer with AI assistance. They are indicative, not a delivery commitment, and exclude FP-03/FP-04 implementation, broker deployment and physical device availability.

| Task | Work | Estimate | Exit evidence / dependency |
|---|---|---:|---|
| FP02-001 | Reconfirm current main, FP-01 AuthLevel integration and baseline Compose behavior | 1–1.5 h | Main source inspected; baseline run captured; FP-01 integration proven or blocker recorded |
| FP02-002 | Freeze screen-facing workspace/assignment/status models with FP-03/FP-04 | 1–2 h | Typed contract agreed; no client target selector or sensitive fields |
| FP02-003 | Add/adjust WorkspaceViewModel state composition and fakeable repository seams | 2–3 h | Unit tests cover state transitions; no network work in Composables |
| FP02-004 | Implement S-02 dashboard profile + single Cloud PC card | 1.5–2.5 h | Existing layout retained; zero/multi assignment never renders arbitrary card |
| FP02-005 | Implement S-03 lifecycle/RDP detail and freshness display | 1.5–2.5 h | All enums mapped; 15s boundary test passes |
| FP02-006 | Add manual refresh and refresh-before-Connect orchestration | 1–2 h | Refresh disables Connect; fresh-ready route is the only path to FP-05 |
| FP02-007 | Add exact PRD error presentation and complete empty/stale/unknown states | 1–1.5 h | Error snapshots match PRD; no raw details |
| FP02-008 | Add Settings/Sign-out delegation and inert R2 Dock placeholder | 0.5–1 h | FP-01 action called; no admin or dock actions |
| FP02-009 | Unit/Compose/accessibility/contract-adapter tests | 2.5–3.5 h | Baseline and FP02 test plan results recorded |
| FP02-010 | Security/diff/traceability/docs and Owner acceptance | 1–1.5 h | No sensitive values or unrelated code; requirement evidence in PROGRESS |

**Indicative FP-02 app-only effort:** approximately 14–19 focused hours, assuming FP-01/FP-03/FP-04 interfaces are ready. If those contracts are not ready, report dependency work separately; do not absorb their implementation into FP-02.

## 3. Phase sequence

### Phase 0 — Current baseline and dependency check
1. Fetch latest `main` and create a fresh implementation branch from it.
2. Confirm FP-01 implementation commit, tests and current AuthLevel contract.
3. Re-run existing sign-in/dashboard Compose tests.
4. Inspect current `MobiDeskViewModel`, repositories, Composables, API DTOs and build configuration.
5. Capture the current visual/test baseline.

**Stop:** if FP-01 has no fully-authenticated handoff in current source, do not bypass it with a generic SDK Authenticated boolean.

### Phase 1 — Contract freeze
1. Agree the FP-03 screen-facing typed assignment result.
2. Confirm the student-safe Cloud PC label and permitted profile fields.
3. Agree the FP-04 status DTO and exact error mapping from `API_CONTRACT.md`.
4. Confirm the approved broker HTTPS base URL/configuration source.
5. Keep API ownership with FP-04; no new endpoint is added by FP-02.

**Stop:** do not code a guess about VM label, assignment cardinality, status JSON or broker URL.

### Phase 2 — State and repository seams
1. Introduce only the minimal WorkspaceViewModel/repository contract needed by current architecture.
2. Inject fake WorkspaceRepository and fake StatusRepository in tests.
3. Build a pure Connect eligibility function with a fake clock.
4. Prove prior-user data clears on auth subject change/sign-out.

### Phase 3 — S-02 / S-03 UI
1. Evolve the current DashboardScreen, preserving existing visual language.
2. Render profile name and optional college label.
3. Render exactly one safe Cloud PC card for an Assigned result.
4. Show lifecycle and transport separately.
5. Show sample age/freshness.
6. Render not-assigned/review/unknown/error states from the typed result.
7. Keep Dock informational and inert.
8. Delegate settings/sign-out to FP-01.

### Phase 4 — Refresh and Connect handoff
1. Implement initial status load.
2. Implement explicit refresh status.
3. Disable Connect during refresh and on error/stale/unknown/not-ready states.
4. On Connect tap, force a fresh status read.
5. Only after fresh eligible status hand off to FP-05.
6. Do not create a grant or open an RDP socket in FP-02.

### Phase 5 — Verification and acceptance
1. Run existing unit and Compose tests.
2. Run all FP02 tests.
3. Verify error text, freshness and Connect truth table.
4. Verify account-switch/sign-out cache clearing.
5. Run TalkBack/font scaling checks on a supported device/emulator.
6. Review the source diff, dependency changes, logs and static data exposure.
7. Update PROGRESS.md with source SHA, tests, device and dependencies.

## 4. Proposed source change map

Reconfirm exact paths on latest main before editing.

| File / area | FP-02 change |
|---|---|
| `app/src/main/java/com/mobidesk/android/MobiDeskViewModel.kt` or a narrowly scoped workspace ViewModel | Compose FP-01, FP-03 and FP-04 state into a student workspace state; no direct assignment logic |
| `app/src/main/java/com/mobidesk/android/data/MobiDeskRepositories.kt` or small focused repository files | Consume FP-03 WorkspaceRepository contract; consume FP-04 StatusRepository contract; keep API adapters in their owning feature where possible |
| `app/src/main/java/com/mobidesk/android/ui/MobiDeskApp.kt` or focused workspace composables | Update S-02 and add S-03 status detail; exact parent copy |
| `app/src/test/...` | Pure eligibility, enum mapping, state and repository contract tests |
| `app/src/androidTest/...` | Compose state, no multiple cards, disabled Connect, refresh/error/accessibility tests |

Do not move files or create a broad package hierarchy solely to make FP-02 look architecturally larger.

## 5. Verification A / Verification B

For every phase:

- **Verification A — direct proof:** run the test or inspect the contract that demonstrates the phase exit condition.
- **Verification B — independent check:** inspect the diff and test neighboring states/security boundaries to verify the change did not introduce a bypass.

Examples:
- Connect eligibility unit test + independent exhaustive truth-table check.
- RLS/assignment contract test + independent source scan confirming no user/VM selector entered from Compose.
- Status enum UI test + independent check that TCP_REACHABLE cannot enable Connect.
- Account-switch UI test + independent ViewModel inspection that prior student's cached values are cleared.

## 6. Definition of Done

FP-02 is complete only when:
- FP-01 fully-authenticated gate is proven.
- FP-03 delivers exactly-one assignment semantics and user-safe display data.
- FP-04 provides authoritative fresh readiness state.
- Every FP02-FR requirement has test evidence.
- Assignment ambiguity, status error, staleness and session changes all fail closed.
- Connect only hands off after a fresh eligible status result.
- Exact PRD messages and accessibility requirements are met.
- The Dock remains a disabled/informational R2 placeholder.
- No admin, power control, assignment write, grant, guest credential, broker or RDP ownership has leaked into FP-02.
- PROGRESS.md contains evidence; unavailable external tests remain BLOCKED/NOT RUN and prevent a DONE claim.

## 7. External dependencies are not FP-02 coding tasks

- The SSH server becoming reachable is an operational prerequisite for later integration but does not prove an authenticated broker status endpoint.
- Broker DNS/TLS/base URL, host adapter, VM4 approval, status-probe behavior and RDP negotiation remain FP-04/server/OI dependencies.
- Live Supabase schema/RLS and safe display-field verification belong to FP-03.
- Physical RDP/NLA testing belongs to later connection/device FPs.
