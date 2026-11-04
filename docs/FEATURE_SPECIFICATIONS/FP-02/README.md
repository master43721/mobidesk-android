# FP-02 — Student Dashboard and Workspace

**Feature:** FP-02  
**Priority / release:** P0 / R1  
**Draft:** 0.1 — source-grounded Owner review draft  
**Snapshot:** 4 November 2026  
**Canonical repository:** master43721/mobidesk-android  
**Status:** REVIEW DRAFT — not approved for implementation.

## Purpose

Provide the Student's single home screen showing their approved profile and one Cloud PC, with honest lifecycle/RDP readiness, refresh and Connect handoff only when eligible.

FP-02 is primarily student workspace presentation/orchestration. FP-03 owns assignment resolution; FP-04 owns live readiness; FP-05 owns guest credentials; FP-06 owns grant/session authorization.

## Package documents

| File | Purpose |
|---|---|
| PRD.md | Feature outcome, scope, 18 requirements, UX states and acceptance |
| TECHNICAL_DESIGN.md | Current-source audit, screen architecture, typed contracts, status mapping and security |
| IMPLEMENTATION_PLAN.md | Sequenced work packages, dependencies and verification plan |
| TEST_PLAN.md | 30 core cases, status truth table, exact copy, accessibility and negative tests |
| SECURITY_PRIVACY.md | Data classification, trust boundary, UI/cache/logging controls |
| DECISIONS.md | Source-confirmed decisions and proposed implementation defaults |
| OPEN_QUESTIONS.md | Owner and cross-feature questions that must remain visible |
| TRACEABILITY.md | Parent FR -> FP-02 requirements -> test/gate matrix |
| PROGRESS.md | Current source evidence, dependency blockers and no-change statement |
| REVISION_NOTES.md | Package provenance and assumptions/limitations |
| ANTIGRAVITY_HANDOFF.md | Copy-ready FP-02 implementation brief |
| ANTIGRAVITY_REVIEW_PROMPT.md | Independent post-implementation alignment/security review |

## Read-first sources

- `docs/MobiDesk_PRD_v1.2.pdf` — normative parent product requirements.
- `docs/FEATURE_INVENTORY.md` — FP-02 and adjacent FP-03/04/05/06 boundaries.
- `docs/DESIGN.md` — screens S-02, S-03, S-07 and S-09; exact status/error treatment.
- `docs/API_CONTRACT.md` — `GET /v1/vm-status`, auth, enums, sample freshness and errors.
- `docs/TECHNICAL_SPEC.md` — ViewModel/repository/domain boundaries.
- `docs/SECURITY_THREAT_MODEL.md` — current user/RLS, no client-selected target, no sensitive telemetry.
- `docs/TEST_PLAN.md` and `docs/TRACEABILITY_CHECK.md` — parent tests and gate ownership.
- `docs/SUPABASE_DISCOVERY.md` — historical 27 Oct snapshot only; revalidate live schema before relying on it.

## Scope guard

- No student self-registration, Admin functions or assignment writes.
- No VM power controls, VM picker or direct RDP target.
- No hard-coded "Online" state.
- No raw VM/host routing data in client UI.
- No grant, Windows credentials, WSS relay or FreeRDP implementation.
- Dock remains a disabled/informational R2 placeholder.

## Review protocol

Review FP-02 PRD §§4, 7, 12–14 and OPEN_QUESTIONS.md. Resolve safe VM label, assignment result, broker endpoint availability, freshness clock handling, refresh cadence and FP-01 integration evidence before implementation. Do not silently change the parent PRD or adjacent feature ownership.
