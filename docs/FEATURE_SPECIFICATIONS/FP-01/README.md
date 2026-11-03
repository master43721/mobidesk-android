# FP-01 — Student Authentication and Account Recovery

**Draft:** 0.1 — Project Owner critique requested  
**Snapshot:** 3 November 2026  
**Canonical repository:** https://github.com/master43721/mobidesk-android  
**Status:** REVIEW DRAFT — not approved for implementation.

## Purpose

Define the product requirements, design, implementation plan, security rules and acceptance tests for FP-01. This package is grounded in the native Kotlin/Jetpack Compose source in this repository and the pinned Supabase Kotlin SDK. It is intentionally a first draft for your critique.

## Package files

| File | Purpose |
|---|---|
| PRD.md | Product outcome, boundaries, functional requirements and acceptance criteria |
| TECHNICAL_DESIGN.md | Current implementation evidence, target architecture, flows and state model |
| IMPLEMENTATION_PLAN.md | Ordered work packages, file areas, dependencies and gates |
| TEST_PLAN.md | Unit, UI, Supabase integration, recovery and security cases |
| SECURITY_PRIVACY.md | Threats, credentials, session protection and privacy controls |
| SUPABASE_CONFIGURATION.md | Required project settings, redirects, email delivery and security configuration |
| DECISIONS.md | Confirmed facts, recommendations and undecided design choices |
| OPEN_QUESTIONS.md | Owner critique checklist and unresolved questions |
| PROGRESS.md | Evidence-based package status and draft changelog |

## Source hierarchy and evidence labels

1. Parent product specification: ../../MobiDesk_PRD_v1.2.pdf.
2. Parent feature boundary: ../../FEATURE_INVENTORY.md.
3. Current implementation evidence: canonical repository source and Gradle configuration.
4. Official Supabase documentation is external platform guidance, not proof of the live MobiDesk project's settings.
5. Requirements in this package cannot silently weaken the parent PRD.

Use **VERIFIED**, **REPORTED**, **PROPOSED**, **UNKNOWN** and **BLOCKING** consistently. Existing code is evidence of what exists, not authority over the desired product behavior.

## FP-01 boundary

**Included:** institutional email/password sign-in; Auth session restoration and state changes; sign-out; forgot-password request; recovery deep link; new-password update; safe UI feedback; Auth configuration readiness; authentication-focused tests.

**Excluded:** in-app account creation; roster/invitation administration; student profile and workspace reads; VM assignment; RDP/Windows guest credentials; broker grants; device administration; MFA/passkeys unless approved separately.

**Handoff:** FP-01 ends when it publishes a valid Supabase Auth state and user identity to FP-02. A valid Supabase account does not, by itself, prove student-profile eligibility or VM authorization.

## Critique protocol

For each requirement, choose **KEEP**, **CHANGE**, **REMOVE** or **ADD**. Please focus particularly on the questions in OPEN_QUESTIONS.md. No implementation work or parent PRD change is authorized by this review draft.
