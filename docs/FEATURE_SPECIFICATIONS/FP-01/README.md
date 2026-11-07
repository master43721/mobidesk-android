# FP-01 — Student Authentication and Account Recovery

**Draft:** 0.2 — Project Owner critique revision  
**Snapshot:** 4 November 2026  
**Repository:** master43721/mobidesk-android  
**Status:** Owner-directed implementation handoff; provider/device release gates remain open.

## Purpose

Specify the existing MobiDesk authentication feature with focused fixes for the recovery lifecycle, callback idempotency, sign-out failure and testability. This is a hardening package, not a new Auth architecture.

## Package documents

| File | Purpose |
|---|---|
| PRD.md | Product requirements, AuthLevel, recovery behavior, functional requirements and acceptance criteria |
| TECHNICAL_DESIGN.md | Source audit, minimal design, recovery marker, SDK 3.6.0 findings and AuthRepository seam |
| PLAN.md | Scoped implementation work packages, estimates and 4–13 November planning window |
| TEST_PLAN.md | Four existing baseline tests and 24 mandatory core pilot cases |
| SECURITY_PRIVACY.md | Recovery-session boundary, sign-out fallback, email-delivery blocker and data handling |
| SUPABASE_CONFIGURATION.md | Custom SMTP/Send Email Hook and project setting requirements |
| DECISIONS.md | Verified SDK facts, selected defaults and remaining Owner choices |
| OPEN_QUESTIONS.md | Six owner decisions plus concrete project/configuration checks |
| TRACEABILITY.md | Correct parent FR-AUTH-01/FR-AUTH-02 mapping |
| PROGRESS.md | Critique disposition and evidence-based progress |
| REVISION_NOTES.md | Detailed v0.1-to-v0.2 changes |

## Important source distinction

- Parent product requirements remain in docs/MobiDesk_PRD_v1.2.pdf.
- Parent FP boundary remains in docs/FEATURE_INVENTORY.md.
- Current source is evidence of current behavior, not the desired product behavior.
- Supabase SDK behavior is verified against the exact pinned 3.6.0 tag where stated.
- Live Supabase project configuration is not inferred from the historical 27 October discovery report.
- A proposed default is not an Owner-approved institutional policy.

## v0.2 headline corrections

1. FR-AUTH-01 maps to FP-01. FR-AUTH-02 profile/assignment RLS maps to FP-02/FP-03.
2. Recovery callback sessions are RECOVERY_ONLY until password update succeeds.
3. Recovery cancel/back signs out the recovery session.
4. Recovery marker survives process death; callback is de-duplicated.
5. SDK signOut default LOCAL is verified from 3.6.0 source; local clear fallback is specified.
6. No AuthSessionCoordinator/AuthDeepLinkRouter rewrite; add only AuthRepository seam and AuthLevel.
7. Custom SMTP/Send Email Hook is a release prerequisite for ordinary student email recovery.
8. Four baseline tests and a focused 24-case core suite replace the broad catalogue.

## Critique protocol

For each remaining Owner question, reply with KEEP / CHANGE and replacement wording. After Owner critique, revise all docs together and publish v1.0. No implementation should start from this unapproved draft.


## Owner account-model clarification — 4 November 2026

Students do not self-register. The College Administrator provisions the Auth identity and sends the activation invitation. FP-01 implements authentication and password recovery for already-provisioned accounts only. Do not add student signup, Auth user creation, invitation dispatch or Admin features here. The project-level signup toggle may be disabled only after the separate trusted Admin invitation path is proven; the Email provider remains enabled.

## Antigravity handoff

- [Implementation handoff](ANTIGRAVITY_HANDOFF.md) — end-to-end scope, constraints, phases, acceptance and copy-ready instructions.
- [Independent review prompt](ANTIGRAVITY_REVIEW_PROMPT.md) — strict post-implementation PRD/plan/decision/test/security audit.
