# FP-02 — Student Dashboard and Workspace

**Version:** 0.2  
**Date:** 5 November 2026  
**Status:** Corrected owner-review package; implementation not started in this branch  
**Canonical repository:** master43721/mobidesk-android

## Purpose

FP-02 provides the authenticated student's single home screen: approved identity, exactly one safe Cloud PC card, truthful lifecycle and RDP status, status refresh, a guarded Connect handoff, account actions and the R2 Dock placeholder.

This package corrects the v0.1 review findings. It does not claim that FP-03 assignment resolution, the FP-04 broker endpoint, or FP-05/FP-06 session work is complete.

## Read in this order

1. PRD.md — normative FP-02 behavior and the single Connect policy.
2. TECHNICAL_DESIGN.md — client/server boundaries, parallel loading, assignment consistency and monotonic freshness.
3. IMPLEMENTATION_PLAN.md — tasks, estimates, dates, dependencies and stop gates.
4. TEST_PLAN.md — test cases and release evidence.
5. SECURITY_PRIVACY.md — privacy, authorization and failure behavior.
6. DECISIONS.md — decisions made in v0.2.
7. OPEN_QUESTIONS.md — only unresolved external approvals and integration facts.
8. TRACEABILITY.md — parent and FP02 requirement-to-test mapping.
9. PROGRESS.md — verified repository evidence and current blockers.
10. REVISION_NOTES.md — what changed from v0.1.

Parent amendment proposal: [AMD-01](../../PRD_CHANGE_PROPOSALS/PRD-1.2-AMD-01-FP02-STATUS-CONSISTENCY.md).

Development environment: [GitHub Codespaces workflow](../../CODESPACES_WORKFLOW.md).

## Ownership

| Area | Owner |
|---|---|
| Fully authenticated session, recovery and sign-out | FP-01 |
| Student profile and authoritative exactly-one assignment resolution | FP-03 |
| Android StatusRepository, status UI, refresh, Connect orchestration and client contract tests | FP-02 |
| Broker status endpoint, probe, rate limiting and assignment-tag calculation | FP-04 |
| Windows credential prompt and NLA | FP-05 |
| Grant, relay, FreeRDP transport and session lifecycle | FP-06+ |
| Dock pairing and external display | R2 |

FP-02 owns the student-facing rule that no more than one assigned Cloud PC card is rendered. FP-03 owns whether the underlying assignment is valid and unique. Both boundaries are required for acceptance.

## v0.2 decisions at a glance

- A stale, missing or indeterminate sample does not make the Connect affordance unreachable. Connect remains available when identity and assignment are valid and the sample is stale, missing or indeterminate. A fresh known-negative status disables it; when that status becomes stale, it is indeterminate again. In-flight/cooldown gates temporarily disable it. Tapping always performs a fresh status check before FP-05.
- A fresh sample is aged using Android monotonic elapsed time from response receipt, conservatively adding the request round-trip duration. A lifecycle-aware local timer updates visible age and re-enables Connect when a fresh-negative sample expires; it makes no network requests. The device wall clock is not used to decide freshness.
- FP-03 and FP-04 provide the same opaque assignmentTag for an assigned snapshot. FP-02 displays a card and status together only when the tags match.
- Profile/assignment and status are requested in parallel after FP-01 confirms the session.
- Android StatusRepository belongs to FP-02. FP-04 continues to own the server endpoint and probe.
- Status requests have an 8-second client deadline, cancellation, 429/Retry-After handling and a 2-second local refresh debounce.
- R1 ships reviewed English (en-IN) string resources. Telugu (te-IN) translations are a follow-up requiring review; no unreviewed translation is embedded.

## Acceptance warning

A passing Compose test or a visible Ready state does not prove RLS, broker deployment, grant authorization, Windows NLA or a working RDP session. FP-02 is complete only after its tests pass and the FP-01/03/04/05/06 contracts are integrated with real evidence.
