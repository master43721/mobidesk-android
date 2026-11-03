# FP-01 Requirement Traceability Matrix

**Version:** 0.1 — REVIEW DRAFT  
**Date:** 3 November 2026

## 1. Parent-to-feature mapping

The parent documentation index identifies FR-AUTH-01 and FR-AUTH-02 as authentication requirements. Their exact normative wording should be transcribed/checked against MobiDesk_PRD_v1.2.pdf before final approval. The provisional grouping below must not be mistaken for exact parent wording.

| Parent PRD reference | FP-01 requirements | Design location | Test evidence |
|---|---|---|---|
| FR-AUTH-01 — exact text to verify | FP01-FR-001 through FP01-FR-009, FP01-FR-016 through FP01-FR-018 | TECHNICAL_DESIGN §§2–5 | FP01-UT-001..012, FP01-INT-001..010, FP01-UI-001..004 |
| FR-AUTH-02 — exact text to verify | FP01-FR-010 through FP01-FR-015 | TECHNICAL_DESIGN §§5.4–5.6 | FP01-INT-011..018, FP01-DL-001..012, FP01-SEC-003..012 |
| Parent PRD §9 security controls | Credential handling, no privileged key, session state, callback validation, privacy and enumeration controls | SECURITY_PRIVACY.md | FP01-SEC-001..012 |
| Parent PRD §12 error catalogue | Auth/expiry errors and safe recovery behavior | PRD.md §9; SECURITY_PRIVACY.md | FP01-UT-016..017 and integration/error cases |
| Parent NFR-COMP-01 | SDK/API/ABI compatibility preservation | TECHNICAL_DESIGN.md §2 | Build/compatibility checks in final acceptance report |

## 2. FP01 requirement-to-test mapping

| FP-01 requirement | Primary tests |
|---|---|
| FP01-FR-001 | INT-001, INT-002, INT-003, UT-001..005 |
| FP01-FR-002 | UI-012, SEC project signup check |
| FP01-FR-003 | UT-008..010, UI-001..002, INT-005..007 |
| FP01-FR-004 | UT-006..007 |
| FP01-FR-005 | UT-008..010, UI-002 |
| FP01-FR-006 | SEC-003..006; process-recreation UI test |
| FP01-FR-007 | UT-001..003 |
| FP01-FR-008 | UT-011..012, UI-004, UI-006 |
| FP01-FR-009 | UT-018, INT-009..010 |
| FP01-FR-010 | INT-011..012, SUPABASE_CONFIGURATION.md §6 |
| FP01-FR-011 | DL-001..012 |
| FP01-FR-012 | INT-013..016, DL-004..009 |
| FP01-FR-013 | UT-013..015, INT-014..016 |
| FP01-FR-014 | INT-011..012, SEC-008..009 |
| FP01-FR-015 | UT-016, SEC-003..004, SEC-011 |
| FP01-FR-016 | UT-019, UI-011 |
| FP01-FR-017 | SEC-001..002 |
| FP01-FR-018 | UT-020 and FP-02 handoff integration test |

## 3. Traceability rules

- A requirement without a test is not ready for implementation acceptance.
- A test without a requirement must be justified as security, accessibility or platform quality coverage.
- A PASS requires test result, source commit, environment and evidence link.
- Parent PRD references remain unchanged until an approved parent PRD revision.
- When requirements change after Owner critique, update this matrix and all related task/test IDs together.
