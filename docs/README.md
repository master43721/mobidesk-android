# MobiDesk project documentation

**Documentation set:** v1.2 · **Snapshot:** 3 November 2026  
**Canonical source:** [master43721/mobidesk-android](https://github.com/master43721/mobidesk-android)

## Start here
- [PRD](PRD.md) — normative product requirements and pilot acceptance gates.
- [Decisions](DECISIONS.md) — recommended architecture decisions and remaining owner approvals.
- [Architecture](ARCHITECTURE.md) — system boundaries and session flow.
- [API contract](API_CONTRACT.md) — proposed broker control and relay APIs.
- [Security threat model](SECURITY_THREAT_MODEL.md) — assets, trust boundaries, abuse controls and rollback.
- [Performance plan](PERFORMANCE_PLAN.md) — measurable FPS, latency, reliability and cleanup gates.
- [Tasks](TASKS.md) — phase delivery checklist.
- [Progress](PROGRESS.md) — current evidence-backed state.
- [Open items](OPEN_ITEMS.md) — unresolved owner questions.
- [Test plan](TEST_PLAN.md) — verification scenarios and release gates.
- [Operations](OPERATIONS.md) — pilot runbook and operational controls.
- [Baseline audit](BASELINE_AUDIT.md) — historical implementation evidence, not normative scope.
- [PRD review](PRD_REVIEW.md) — critique traceability and changes in v1.2.
- [Glossary](GLOSSARY.md) — shared terms.
- [Changelog](CHANGELOG.md) — documentation revisions.

Existing repo documents (Android build report, platform notes, SECURITY, server implementation plan and Supabase discovery) are retained as supporting evidence. Where an older audit conflicts with PRD v1.2, PRD requirements describe the desired product and the audit describes only what was observed at its snapshot.

**Scope rule:** this documentation describes the native Kotlin/Jetpack Compose repository only. Do not mix in unrelated app repositories or assume a separate imported workspace is live-synced.
