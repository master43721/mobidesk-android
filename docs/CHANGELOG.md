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


## 1.0 follow-up — 3 November 2026

- Refreshed factual references to the current main commit `5979320b05e1e6c1d872d82dcc17e65b0901e7e5`.
- Confirmed G0 stack/repository match: native Kotlin/Jetpack Compose in `master43721/mobidesk-android`; the older Flutter audit's provenance remains UNKNOWN.
- Resolved the SDK baseline to minSdk 29, compileSdk 37, targetSdk 37, Java/Kotlin target 11 and ABIs arm64-v8a/x86_64; no build configuration changed.
- Added `docs/PRD.md` as a navigational requirement index only. The 23-page PRD PDF remains normative and unchanged.
- Preserved FR-SEC-01..03 as a PRD numbering gap; mapped PRD §9 security controls independently pending Owner-approved PRD revision.
- Reforecast G3 FreeRDP integration to 18–24 Dec 2026, G4 physical acceptance to 30 Dec 2026–5 Jan 2027, and G5 readiness to 7–15 Jan 2027 based on 216–302 focused hours for one owner plus AI assistance.
- Updated supporting-document links and marked uncommitted local APK output as not present in the repository.

- Widened the G3 forecast to 18–31 Dec 2026 and moved G4/G5 to 8–15 Jan and 20–28 Jan 2027 to reflect the 80–120 hour one-person integration range and hand-off risk.
- Removed the loopback IP literal and changed the wireframe email to a reserved non-deliverable placeholder; the OpenAPI file contains no deployment hostname.

- Recorded GitHub Actions run 41595687606 failing before Gradle because the hosted runner could not locate `platforms;android-37`; added a separate CI follow-up without changing workflow or Gradle.


## 3 November 2026 — Owner-selected feature scope

- Added `FEATURE_INVENTORY.md` with 22 Feature Points (18 R1, 4 R2).
- Recorded prompt-per-session Windows guest authentication as the selected R1 pilot path.
- Recorded both Dock render architectures as candidates for a measured comparison; no architecture selected.
- Recorded no additional scope; clipboard, file transfer and admin console remain excluded.
- Recorded background-session continuity as an owner-directed proposed amendment to v1.2, not a normative change. Screen-lock behavior remains open.
- Linked the feature inventory from this index and updated DECISIONS, OPEN_ITEMS, TASKS and PROGRESS.


## 1.1 — 4 November 2026

Owner-confirmed College Administrator and Student account model added as a proposed successor-PRD scope change.

- Added ADMIN_ACCOUNT_MODEL.md defining one out-of-band College Administrator bootstrap, no student self-registration, trusted roster-first Auth invitations, account lifecycle, role separation, approved host/VM operations and session support.
- Expanded FEATURE_INVENTORY.md to 28 Feature Packages: 18 student R1, 6 College Administrator R1 and 4 Dock R2.
- Added DEC-30..DEC-37, OI-21..OI-28 and MD-ADM-01..MD-ADM-12 for role security, provisioning, invitation activation, assignments, server operations, sessions, audit and SMTP.
- Updated DESIGN, TEST_PLAN, API_CONTRACT, SECURITY_THREAT_MODEL, OPERATIONS and PRD_V1_3_CHANGES to cover the proposed administrator workflows.
- Supabase Auth Admin invitation remains server-only. The invitation/activation callback must be verified against the pinned Supabase Kotlin 3.6.0 SDK; it is not assumed to be the existing password-recovery callback.
- This PR changes documentation only. No app source, Supabase auth setting/schema/data, broker, server, VM or host configuration is changed. The v1.2 PDF remains unchanged.
