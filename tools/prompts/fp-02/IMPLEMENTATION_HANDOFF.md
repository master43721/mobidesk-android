# FP-02 Implementation Handoff — Student Dashboard and Workspace

**Repository:** master43721/mobidesk-android  
**Target branch:** docs/fp-02-v0.2-corrected-codespaces  
**Implementation branch:** create from latest approved main after FP-02 v0.2 and parent/API contract approval.

## Assignment

Implement only FP-02 with native Kotlin + Jetpack Compose. Read the parent PRD, API contract, FP-02 v0.2 package and related FP-01/03/04/05/06 ownership docs before editing. Preserve existing UI architecture and pinned FreeRDP gitlink. Do not redesign unrelated screens or implement adjacent feature points.

## Preconditions / stop gates

1. FP-01 has a verified FULLY_AUTHENTICATED source commit and recovery-only tests.
2. FP-03 exposes typed zero/one/invalid assignment results, a safe student Cloud PC label and shared assignmentTag.
3. FP-04 exposes approved GET /v1/vm-status with assignmentTag, request-fresh sample, 429 Retry-After and stable approved HTTPS route.
4. Parent Connect/freshness amendment and API v1.1 are approved/frozen.
5. FP-05 confirms a no-argument navigation callback.

Do not replace missing dependencies with a fake Ready state, mock production API, guessed URL or client VM selector.

## Ownership

- FP-01: authentication, FULLY_AUTHENTICATED, recovery, sign-out.
- FP-02: Android StatusRepository, dashboard Compose, monotonic freshness, parallel loading, assignmentTag pairing, refresh/Connect orchestration, client tests.
- FP-03: profile/assignment RLS and authoritative exactly-one resolution, safe label, assignmentTag semantics.
- FP-04: server status endpoint, status probe, assignmentTag, server rate limit.
- FP-05: Windows guest credentials/NLA prompt and no-argument handoff.
- FP-06+: grant, WSS relay, local transport, FreeRDP session.

## Required behavior

Follow FP-02 PRD.md v0.2 as the single normative feature specification. Critical rules:
- No protected requests before FULLY_AUTHENTICATED.
- Parallel profile/assignment and status reads after authentication.
- Compose only one card for typed Assigned; no list-first fallback.
- Pair assignment and status by opaque assignmentTag; discard mismatch and retry pair once; second mismatch uses ASSIGN-02.
- Use SystemClock.elapsedRealtime() and conservative request-round-trip age; never use wall-clock sampledAt for eligibility.
- Every Connect tap makes a fresh status GET; never hand off from cached state.
- Connect remains available for stale/missing/UNKNOWN/NOT_CHECKED so user can request a new check; definitive negative status disables it. Respect in-flight and 429 cooldown gates.
- 8-second cancellable request deadline, HTTP 429 Retry-After, 2-second debounce, no automatic retry.
- No target selector, endpoint, VM ID, credentials or grant in status request or FP-05 navigation.
- FP-06/broker independently revalidates assignment/readiness.
- Exact parent error strings and all enum/freshness copy from resource-backed PRD.
- Clear previous student state on session change/sign-out; do not preserve freshness across process death.
- Account/Settings actions delegate FP-01; Dock is informational only.

If a later broker grant returns ASSIGN-03 or another definitive assignment-invalid result, clear the card and status immediately, suppress further Connect, and reload the FP-03 assignment/status pair once. Do not retry a grant against the previous assignment.

## Required tests

Implement all CORE-01..41 in FP-02 TEST_PLAN.md plus current baseline regression tests. Add a test for post-grant ASSIGN-03 clearing/reload before final review. Verify fake-server request method/path/headers/no-body/no-selector and exact DTO allowlist. Test timeout, cancellation, 429 with and without Retry-After, mismatch recovery, wall-clock jump, stale boundary, duplicate taps and single FP-05 callback.

Report exact source SHA, changed files, Gradle tasks, reports, device/API, CI URL, privacy scan, TalkBack/font scale and unresolved dependencies. No NOT RUN test may be labeled PASS.
