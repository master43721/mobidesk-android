# MobiDesk Engineering Documentation Changelog

## 1.0 — 3 November 2026

Initial engineering suite derived from MobiDesk PRD v1.2.

- Added G0 repository/build/FreeRDP baseline audit and implementation status.
- Added owner decisions, open items, G0–G5 task plan, one-person effort estimates, reforecast and dated progress log.
- Added broker REST/WebSocket API contract and OpenAPI 3.1 schema.
- Added STRIDE threat model, trust-boundary diagram, abuse cases, residual risks and security test references.
- Added low-level technical specification for native modules, session state machine, LocalBridge, FreeRDP v3.32.0 integration, credentials, certificate pinning, RLS sketch, telemetry and build/signing.
- Added separate student-facing UX design document with screen flows, wireframes, pointer/touch/keyboard model, error copy, accessibility and R2 dock placeholder.
- Added requirement-to-design-to-test-to-gate traceability matrix, negative tests, RD-1 test procedure, performance measurement and release static checks.
- Added operations runbook for distribution, key custody, certificate rotation, broker deployment, Windows lockout, backup/restore, retention, incidents and rollback.
- Added proposed PRD v1.3 patch list without changing the v1.2 PDF or any normative requirement.
- Recorded unresolved FR-SEC-01..03 numbering, repository visibility, PRD status label, physical RD-1 availability, VM4 exclusion, unknown backup/retention policy and unproven FreeRDP/LocalBridge integrations.
- All changes are documentation-only under docs/. No application source, Gradle, submodule, CI, Supabase, server or VM changes were made.
