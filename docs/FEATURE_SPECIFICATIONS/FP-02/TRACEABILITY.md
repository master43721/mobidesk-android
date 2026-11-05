# FP-02 Requirement Traceability

**Version:** 0.2 · **Date:** 5 November 2026  
**Parent baseline:** MobiDesk PRD v1.2; amendment proposal pending approval.

## 1. Parent requirement ownership

| Parent requirement | FP-02 ownership | Adjacent owner |
|---|---|---|
| FR-AUTH-01 | Consume authenticated state; delegate sign-out/re-auth only | FP-01 owns authentication/session/recovery |
| FR-AUTH-02 | Render only current student's returned data; account-switch safety | FP-03 owns RLS/profile/assignment reads and negative authorization |
| FR-VM-01 | **FP-02 owns the app display rule: render exactly one Cloud PC card, never a list or first-row fallback.** | FP-03 owns the authoritative assignment resolver/cardinality and server authorization. Acceptance requires both sides. |
| FR-VM-02/03 | No client target selector; handoff is no-argument | FP-03/FP-06 own server target derivation and grant enforcement |
| FR-VM-04 | Render lifecycle and transport distinctly; client timeout state | FP-04 owns server probe and 1,500 ms budget |
| FR-VM-05 | Monotonic freshness display, user-driven refresh and refresh-before-Connect | FP-04 owns sample/probe; FP-06/broker independently rechecks on grant |
| FR-OBS-01 | Only parent-approved safe presentation diagnostics | Parent telemetry owner |
| FR-DOCK-01/02 | Informational placeholder only | R2 Dock features |

The FP-02 card cardinality is a presentation acceptance rule. It does not prove that assignment data is unique or authorized; that remains FP-03.

## 2. FP02 functional requirement-to-test mapping

| FP-02 requirement | Primary CORE tests | Design source | Acceptance evidence |
|---|---|---|---|
| FP02-FR-001 | CORE-01/02/03/21/22/32 | TECHNICAL_DESIGN §§2,5,11 | AuthLevel source + session-generation evidence |
| FP02-FR-002 | CORE-04/05/21/26/35 | §§4,5,8 | Own-profile fixture and account-switch tests |
| FP02-FR-003 | CORE-05 | PRD §4/§6 | Null/blank resource test |
| FP02-FR-004 | CORE-04/06/07/08 | PRD §2/§4 | Exactly one card for valid Assigned |
| FP02-FR-005 | CORE-08/25/26/40 | SECURITY_PRIVACY §§2–4 | No selector, no sensitive DTO field |
| FP02-FR-006 | CORE-10/11/12 | TECHNICAL_DESIGN §8 | Assignment does not imply live status |
| FP02-FR-007 | CORE-11/12/40 | PRD §6 | Every enum has nonblank copy |
| FP02-FR-008 | CORE-15/17/30/38/39 | TECHNICAL_DESIGN §6 | Monotonic test and wall-clock-jump result |
| FP02-FR-009 | CORE-10/13/14/18/19 | PRD §9 | Button-availability truth table |
| FP02-FR-010 | CORE-18/19/20/29/36/37 | PRD §9; TECHNICAL_DESIGN §§5,9 | Fresh request, matching tag, exactly-one handoff |
| FP02-FR-011 | CORE-16/29/33/34 | TECHNICAL_DESIGN §7 | Refresh/cooldown/429 tests |
| FP02-FR-012 | CORE-17/31/32/33/34/37/40 | TECHNICAL_DESIGN §§5–7 | Failure invalidates old readiness |
| FP02-FR-013 | CORE-09/27/28/33 | PRD §7 | Exact error codes/copy and retry |
| FP02-FR-014 | CORE-06–12/16–19/31–40 | PRD §§5–7 | Complete status and error states |
| FP02-FR-015 | CORE-22/23 | PRD §5 | FP-01 delegation |
| FP02-FR-016 | CORE-24 | PRD §5 | Inert Dock |
| FP02-FR-017 | CORE-21/22/30/32/35 | TECHNICAL_DESIGN §§5,11 | Cancellation, cache clear, late-result suppression |
| FP02-FR-018 | Accessibility review §6 | PRD §6; TEST_PLAN §6 | TalkBack/font-scale/device report |

## 3. Previously unmapped test coverage

- CORE-25 maps to FR-005/FR-010/FR-012: status API contract and no target selector.
- CORE-27 maps to FR-001/FR-013/FR-017: AUTH-01 and state clearing.
- CORE-29 maps to FR-009/FR-010/FR-011/FR-012: duplicate action serialization/debounce.

## 4. Parent test IDs retained

T-AUTH-04/T-AUTH-05 and T-NEG-04 cover identity/RLS; T-VM-01..03 cover assignment cardinality; T-VM-08/T-VM-09 cover lifecycle/transport and probe budget; T-VM-10/T-VM-11 cover freshness and refresh-before-Connect; E-02..E-07 cover assignment/status errors; T-NEG-02/T-NEG-03 cover target selection/invalid assignment; T-PRIV-01/T-PRIV-02 cover privacy/logging.

## 5. Traceability rules

A UI test does not prove RLS or server assignment enforcement. A fake StatusRepository does not prove broker deployment. A green Connect button does not prove grant authorization. Every PASS requires source commit, test report, environment and reviewer. Parent amendments must preserve existing requirement IDs unless the Owner approves a new traceability version.
