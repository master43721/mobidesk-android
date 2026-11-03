# MobiDesk project documentation

**Documentation set:** v1.2 · **Snapshot:** 3 November 2026  
**Canonical source:** [master43721/mobidesk-android](https://github.com/master43721/mobidesk-android)

## Product and design
- [PRD](PRD.md) — normative product requirements and pilot gates.
- [Technical Specification](TECHNICAL_SPECIFICATION.md) — module contracts, domain/API models, security constraints and implementation spikes.
- [Technical Design](TECHNICAL_DESIGN.md) — architecture, component responsibilities, sequences, state machines and deployment design.
- [Architecture](ARCHITECTURE.md) — system boundaries and session flow.
- [Decisions](DECISIONS.md) — recommended decisions and owner approvals.

## Engineering plans
- [API contract](API_CONTRACT.md)
- [Security threat model](SECURITY_THREAT_MODEL.md)
- [Performance plan](PERFORMANCE_PLAN.md)
- [Test plan](TEST_PLAN.md)
- [Tasks](TASKS.md)
- [Operations](OPERATIONS.md)
- [Open items](OPEN_ITEMS.md)
- [Progress](PROGRESS.md)

## Evidence and reference
- [Baseline audit](BASELINE_AUDIT.md) — implementation evidence, not normative scope.
- [PRD review](PRD_REVIEW.md) — critique traceability.
- [Glossary](GLOSSARY.md)
- [Changelog](CHANGELOG.md)

Existing Android build report, platform notes, security notes, server implementation plan and Supabase discovery are retained as supporting evidence. Where an older audit conflicts with PRD v1.2, the PRD specifies the target product; the audit describes what was observed at its snapshot.

**Scope rule:** this documentation describes the native Kotlin/Jetpack Compose repository only. Do not mix in unrelated app repositories or assume a separate imported workspace is live-synced.
