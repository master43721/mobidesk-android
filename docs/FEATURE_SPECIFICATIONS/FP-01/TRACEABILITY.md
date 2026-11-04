# FP-01 Requirement Traceability Matrix

**Version:** 0.2 — OWNER CRITIQUE REVISION  
**Date:** 4 November 2026

## 1. Correct parent PRD mapping

The parent PRD v1.2 requirements are transcribed below from the v1.2 PDF review:

| Parent ID | Parent wording | Owning feature | FP-01 mapping |
|---|---|---|---|
| FR-AUTH-01 | Preserve existing sign-in, session restoration, password recovery and sign-out behaviour. | FP-01 | Direct parent requirement. FP-01 preserves existing behavior and corrects recovery/sign-out defects. |
| FR-AUTH-02 | All profile and assignment queries use the user token and RLS. | FP-02 / FP-03 | Not an FP-01 requirement. It is owned by the workspace/profile/assignment features. |

**Correction from v0.1:** The previous traceability incorrectly mapped password recovery to FR-AUTH-02 and deferred checking the parent wording. That mapping is removed. FP-01 maps to FR-AUTH-01. FR-AUTH-02 is explicitly excluded from FP-01's requirement ownership.

FP-01 may test that it does not start profile/assignment queries while AuthLevel is RECOVERY_ONLY. That is an integration boundary assertion, not evidence that FP-01 implements FR-AUTH-02.

## 2. FP-01 requirement-to-test mapping

| FP-01 requirement | Primary acceptance tests |
|---|---|
| FP01-FR-001 | FP01-CORE-02, FP01-CORE-03 |
| FP01-FR-002 | FP01-CORE-01, FP01-CORE-04 |
| FP01-FR-003 | FP01-CORE-05, FP01-CORE-06, FP01-CORE-15 |
| FP01-FR-004 | FP01-CORE-08 |
| FP01-FR-005 | FP01-CORE-08, FP01-CORE-15 |
| FP01-FR-006 | FP01-CORE-12 |
| FP01-FR-007 | FP01-CORE-11 |
| FP01-FR-008 | FP01-CORE-06, FP01-CORE-07 |
| FP01-FR-009 | FP01-CORE-09 |
| FP01-FR-010 | FP01-CORE-10, FP01-CORE-11 |
| FP01-FR-011 | FP01-CORE-10, FP01-CORE-11 |
| FP01-FR-012 | FP01-CORE-10, FP01-CORE-13 |
| FP01-FR-013 | FP01-CORE-14 |
| FP01-FR-014 | FP01-CORE-14 |
| FP01-FR-015 | FP01-CORE-16 |
| FP01-FR-016 | FP01-CORE-17 |
| FP01-FR-017 | FP01-BASE-01..04 |
| FP01-FR-018 | FP01-CORE-18 plus project-setting evidence |

## 3. Parent test ownership

- FR-AUTH-01 acceptance is covered by the FP-01 core tests plus regression of the four existing baseline tests.
- FR-AUTH-02 is traced to FP-02/FP-03 RLS/profile/assignment tests. It must not be marked complete from FP-01 evidence.
- Parent PRD §9 security requirements remain cross-cutting. FP-01 covers only its Auth/session/recovery/logging portion.
- Parent PRD §12 error handling is mapped to the relevant Auth, recovery and session errors; other VM/RDP errors belong to later feature packages.

## 4. Traceability rules

- A requirement without a passing test is not accepted as complete.
- A test without a requirement must be justified as security, accessibility or platform-quality coverage.
- PASS requires test output, source commit, test environment and reviewer.
- Parent requirement IDs and wording remain unchanged until the Project Owner approves a parent PRD revision.
