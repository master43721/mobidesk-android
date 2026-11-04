# FP-02 Revision Notes

**Version:** 0.1 — 4 November 2026

## Package provenance

This first FP-02 draft was prepared from the current checked-in product and engineering documents and a read-only inspection of current main application code. It is a proposed feature-level specification, not an approved change to MobiDesk PRD v1.2.

## Source-based decisions captured

1. FP-02 scope follows FEATURE_INVENTORY.md: student name/approved profile, optional college label, one Cloud PC card, lifecycle/RDP readiness, refresh, Connect when eligible, states, settings/sign-out and R2 Dock placeholder.
2. Existing parent DESIGN.md S-02/S-03/S-07/S-09 are reused rather than creating a new UI journey.
3. Existing API_CONTRACT.md GET /v1/vm-status is consumed; no new FP-02 API is invented.
4. Lifecycle and transport remain separate; RUNNING + RDP_NEGOTIATION_OK + fresh status + valid assignment + authenticated student is required for Connect.
5. The 15-second sample-staleness rule and 1,500 ms server probe budget come from the existing parent API/UX documents.
6. Exact PRD §12 English messages are retained; no replacement error IDs are invented.
7. FP-03/FP-04/FP-05/FP-06 work is not pulled into FP-02.

## Current implementation gaps captured

- Existing dashboard has a static disabled remote session action and static not-ready notice.
- Existing StudentWorkspaceRepository returns a list of VMs; the current dashboard loops the list rather than enforcing exactly one.
- Existing VM query selects a database name; it is not confirmed that every value is a student-safe display label.
- No current Android StatusRepository/broker status client was observed.
- Current checked-in FP-01 source still requires integration verification; see PROGRESS.md.

## Deliberate non-assumptions

- No current live assignment values or counts were inferred from the 27 October Supabase discovery report.
- No stable broker hostname/base URL was invented.
- No server readiness was inferred from the Owner's statement that SSH access is restored.
- No polling cadence, UI design system revision, profile field or new database column was silently chosen.
- No PRD parent requirement wording or IDs were altered.

## Changes in future versions

Any Owner decision or API/assignment contract change must update PRD.md, TECHNICAL_DESIGN.md, IMPLEMENTATION_PLAN.md, TEST_PLAN.md, DECISIONS.md, OPEN_QUESTIONS.md and TRACEABILITY.md together.
