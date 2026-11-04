# MobiDesk — Engineering Documentation

**Documentation set:** 1.0 · **Snapshot:** 4 November 2026  
**Canonical repository:** [master43721/mobidesk-android](https://github.com/master43721/mobidesk-android)  
**Normative product baseline:** [MobiDesk PRD v1.2 PDF](MobiDesk_PRD_v1.2.pdf)

## Source-of-truth rules

1. PRD v1.2 PDF is the normative product requirement source for this suite.
2. PRD outranks current app code. The repository audit records implementation evidence only.
3. Do not modify the PRD from this suite. Proposed edits belong in [PRD_V1_3_CHANGES.md](PRD_V1_3_CHANGES.md) and require Project Owner approval.
4. Facts about repository/infrastructure state must carry evidence labels: [VERIFIED: file/commit], [REPORTED], [ASSUMED] or [UNKNOWN].
5. All unknowns and PRD conflicts stay visible in [OPEN_ITEMS.md](OPEN_ITEMS.md).
6. This documentation set does not modify application source, Gradle files, submodules, CI, Supabase, servers or VMs.

## Feature-level implementation packages

- [FP-01 — Student Authentication and Account Recovery](FEATURE_SPECIFICATIONS/FP-01/README.md): focused Auth hardening package, test plan, security/Supabase notes and Antigravity handoff. No student self-registration.
- [FP-02 — Student Dashboard and Workspace](FEATURE_SPECIFICATIONS/FP-02/README.md): student workspace PRD, technical design, implementation plan, 30-case test plan, security/privacy, traceability and Antigravity implementation/review prompts. FP-03 assignment and FP-04 status contracts remain upstream dependencies.

## Read first

[FEATURE_INVENTORY.md](FEATURE_INVENTORY.md) is the product-level feature catalogue. It now includes the Owner-confirmed College Administrator and Student account model. The account model and administrator FPs are owner-directed proposed amendments because PRD v1.2 explicitly excluded an admin console; the v1.2 PDF remains normative until a successor PRD is approved. See [ADMIN_ACCOUNT_MODEL.md](ADMIN_ACCOUNT_MODEL.md).


| Document | Purpose |
|---|---|
| [PRD.md](PRD.md) | Navigational requirement index; the linked PDF remains the normative source |
| [FEATURE_INVENTORY.md](FEATURE_INVENTORY.md) | 28 product Feature Points, R1/R2 boundary and Owner scope decisions |
| [ADMIN_ACCOUNT_MODEL.md](ADMIN_ACCOUNT_MODEL.md) | Two-role account model, admin bootstrap, roster-first student invitation, RBAC, server/VM controls and student session management |
| [BASELINE_AUDIT.md](BASELINE_AUDIT.md) | G0 repository/stack/build/submodule/security baseline and requirement implementation snapshot |
| [OPEN_ITEMS.md](OPEN_ITEMS.md) | Decisions, owners, proposed due dates and closure evidence |
| [TASKS.md](TASKS.md) | G0–G5 work breakdown, estimates, dependencies and schedule reforecast |
| [PROGRESS.md](PROGRESS.md) | Current gate status and append-only dated evidence log |
| [DECISIONS.md](DECISIONS.md) | Decision register and owner approvals |
| [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) | Gated implementation order and stop conditions |
| [PRD_V1_3_CHANGES.md](PRD_V1_3_CHANGES.md) | Proposed PRD edits only; does not alter v1.2 |

## API and security

| Document | Purpose |
|---|---|
| [API_CONTRACT.md](API_CONTRACT.md) | Normative broker API/relay contract proposal for G1 review |
| [openapi.yaml](openapi.yaml) | OpenAPI 3.1 representation of broker control routes and WSS handshake |
| [SECURITY_THREAT_MODEL.md](SECURITY_THREAT_MODEL.md) | Trust boundaries, STRIDE, abuse cases, residual risks and security tests |

## Client engineering and UX

| Document | Purpose |
|---|---|
| [TECHNICAL_SPEC.md](TECHNICAL_SPEC.md) | Low-level Android/FreeRDP/broker specification, class responsibilities, state machine and integration spikes |
| [DESIGN.md](DESIGN.md) | Product/UX design, screen inventory, flows, wireframes, input model, copy and accessibility |
| [TEST_PLAN.md](TEST_PLAN.md) | Requirement/test traceability, negative tests, RD-1 procedure and acceptance gates |
| [PERFORMANCE_PLAN.md](PERFORMANCE_PLAN.md) | Physical-device first-frame, FPS, input latency, reliability, cleanup and recovery measurement |
| [TRACEABILITY_CHECK.md](TRACEABILITY_CHECK.md) | PRD requirement -> design -> test -> gate completeness check |

## Operations and delivery

| Document | Purpose |
|---|---|
| [OPERATIONS.md](OPERATIONS.md) | Distribution, signing, broker deployment, pins, guest lockout, backup/retention, incident and rollback |
| [CHANGELOG.md](CHANGELOG.md) | Documentation-set revision history |

## Existing repository evidence retained

The following pre-existing documents remain supporting evidence; they do not override the PRD:
- [ANDROID_BUILD_REPORT.md](ANDROID_BUILD_REPORT.md)
- [PLATFORM_NOTES.md](PLATFORM_NOTES.md)
- [SUPABASE_DISCOVERY.md](SUPABASE_DISCOVERY.md)
- [SERVER_IMPLEMENTATION_PLAN.md](SERVER_IMPLEMENTATION_PLAN.md)
- [SECURITY.md](SECURITY.md)
- [PRD_TRACEABILITY.md](PRD_TRACEABILITY.md), if present on the selected branch; verify its contents and age before relying on it.

Historical evidence must be interpreted using its own snapshot date. In particular, historical Supabase row counts and VM assignment statements must not be treated as current assignment data.

## Account and administrator scope update — 4 Nov 2026

- **OWNER CONFIRMED:** no student self-signup; College Administrator provisions student Auth identities and sends activation invitations.
- **OWNER CONFIRMED:** one initial College Administrator account is bootstrapped outside the Android client; the same app routes to Administrator or Student workspace only after server-side role validation.
- Administrator can manage student accounts/assignments, approved server/VM operations and active student sessions through trusted backend APIs. Arbitrary shell/SSH and direct hypervisor access remain excluded.
- Supabase Auth Admin invitation operations require a trusted server secret. The Android client must never contain that key.
- The v1.2 PDF is unchanged; proposed requirements are in PRD_V1_3_CHANGES.md.

## G0 findings and open constraints

- [VERIFIED: main@5979320] The canonical repository and PRD §6.1 agree on native Kotlin/Jetpack Compose.
- [VERIFIED: main@5979320] minSdk is 29; compileSdk/targetSdk are 37/37; Java/Kotlin target is 11; native ABIs are arm64-v8a and x86_64.
- [VERIFIED: main@5979320 plus pinned upstream FreeRDP commit] FreeRDP is pinned to the signed v3.32.0 release commit.
- [UNKNOWN] RD-1 physical phone availability, production broker host/route, guest account lockout model, current backup/restore policy and distribution choice.
- G3 remains blocked on exact pinned FreeRDP transport, secure LocalBridge, certificate callback, credential lifecycle and render/resize integration proofs.
- VM4 remains excluded until independently verified and approved.
- FR-SEC-01..03 are undefined in PRD v1.2; see TRACEABILITY_CHECK.md and PRD_V1_3_CHANGES.md.

## Change and review workflow

Work on branch `docs/prd-v1.2-suite`. Each documentation file is committed separately. The branch began from an earlier mainline base and was one commit behind main at the 3 Nov 2026 audit; the three-dot compare contained documentation paths only. Review the final diff against main; no application-code or configuration change is permitted. Open a pull request for review; do not merge automatically.


## FP-01 implementation handoff — 4 Nov 2026

- [FP-01 package index](FEATURE_SPECIFICATIONS/FP-01/README.md)
- [Antigravity implementation handoff](FEATURE_SPECIFICATIONS/FP-01/ANTIGRAVITY_HANDOFF.md)
- [Antigravity independent review prompt](FEATURE_SPECIFICATIONS/FP-01/ANTIGRAVITY_REVIEW_PROMPT.md)

The FP-01 package was ported to a fresh documentation branch based on current main; the old draft branch was 39 commits behind. No application source or live Supabase configuration changed in this documentation update.
