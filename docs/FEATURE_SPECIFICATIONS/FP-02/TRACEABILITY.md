# FP-02 Requirement Traceability Matrix

**Version:** 0.1  
**Date:** 4 November 2026  
**Normative parent:** MobiDesk PRD v1.2 PDF.

## 1. Parent requirement ownership

| Parent requirement | Parent meaning (per checked-in traceability suite) | FP-02 relationship | Primary owner |
|---|---|---|---|
| FR-AUTH-01 | Sign-in, session restore, recovery and sign-out | Preconditions and sign-out delegation only | FP-01 |
| FR-AUTH-02 | Profile and assignment reads use current user token/RLS; cross-student reads denied | FP-02 displays the current student's data; FP-03 implements the secured profile/assignment queries and cardinality | FP-03 with FP-02 integration |
| FR-VM-01 | Exactly one active assignment; zero/duplicate/inactive/unmapped fail closed | Consume and render typed assignment result; never choose first row | FP-03; FP-02 UI |
| FR-VM-02 / FR-VM-03 | Server derives assignment/target and client cannot change routing | No target selector in FP-02; ensure Connect handoff contains no VM endpoint/selector | FP-03 / FP-06 |
| FR-VM-04 | Lifecycle and RDP transport distinct; probe budget 1,500 ms; timeout UNKNOWN | Render both dimensions and exact state mapping | FP-04; FP-02 UI |
| FR-VM-05 | Sample stale at 15 seconds; Connect requires fresh state and server grant-time recheck | Display sample age/stale state; refresh before Connect | FP-04; FP-02 UI |
| FR-OBS-01 | Safe stage/status observability | Do not emit sensitive telemetry; use parent allowlist only | Cross-cutting; FP-02 presentation events only if approved |
| FR-DOCK-01 / FR-DOCK-02 | Dock is R2 | Informational placeholder only; no R1 dock action | R2 |

FP-02 does not claim FR-VM-01's server-side assignment enforcement, FR-VM-02/03 routing security, FR-VM-04/05 broker probe correctness, or RDP/session requirements have been implemented by drawing the dashboard.

## 2. FP-02 functional requirement-to-test mapping

| FP-02 requirement | Primary test(s) | Design | Gate | Acceptance evidence |
|---|---|---|---|---|
| FP02-FR-001 | CORE-01/02/03 | TECHNICAL_DESIGN §§3,6 | G1/G4 | FP-01 AuthLevel integration trace |
| FP02-FR-002 | CORE-04/05/21/26 | §§5,10 | G1/G2/G4 | Own-profile fixture/RLS evidence; safe fields only |
| FP02-FR-003 | CORE-05 | §§5,9 | G4 | Null/blank optional field test |
| FP02-FR-004 | CORE-04/07/08 | §§5,6 | G1/G2/G4 | Single card only for typed Assigned result |
| FP02-FR-005 | CORE-08/26 | §§5,10 | G2/G4 | No list-first fallback, no VM selector |
| FP02-FR-006 | CORE-10/11/12 | §§5,7 | G2/G4 | Status not inferred from assignment |
| FP02-FR-007 | CORE-11/12 | §§5,9 | G2/G4 | Every lifecycle/transport enum mapped |
| FP02-FR-008 | CORE-15/17/30 | §§7,8 | G2/G4 | Staleness boundary/fake-clock result |
| FP02-FR-009 | CORE-13/14 | §8 | G2/G4 | Connect truth table |
| FP02-FR-010 | CORE-18/19/20 | §6 | G3/G4 | Fresh refresh then exactly one FP-05 handoff |
| FP02-FR-011 | CORE-16 | §6 | G4 | Refresh action and busy state |
| FP02-FR-012 | CORE-17/19 | §§6,7 | G2/G4 | Failed refresh fails closed |
| FP02-FR-013 | CORE-09/28 | §9 | G1/G4 | Exact PRD code/copy/retry |
| FP02-FR-014 | CORE-06–12/16–19 | §§4,9 | G4 | All meaningful UI states covered |
| FP02-FR-015 | CORE-22/23 | §4 | G4 | FP-01 sign-out/settings delegation |
| FP02-FR-016 | CORE-24 | §4 | R1/R2 | Dock display inert |
| FP02-FR-017 | CORE-21/22/30 | §§6,10 | G4 | Cache clearing on subject/session transition |
| FP02-FR-018 | Accessibility review §6 | §11 | G4 | TalkBack/font-scaling/device evidence |

## 3. Parent test IDs retained

- T-AUTH-04/T-AUTH-05 and T-NEG-04 cover caller identity/RLS and cross-student denial.
- T-VM-01..03 cover exactly-one assignment/cardinality and negative assignment cases; FP-03 owns the resolver.
- T-VM-08/T-VM-09 cover distinct lifecycle/transport and bounded status probing.
- T-VM-10/T-VM-11 cover freshness and refresh-before-Connect.
- E-02..E-07 cover assignment and VM status error semantics.
- T-NEG-02/T-NEG-03 cover rejection of client-selected VM targets and invalid assignments.
- T-PRIV-01/T-PRIV-02 cover forbidden sensitive data in logs/telemetry.
- G1 contract freeze and G2 broker readiness are upstream dependencies; G4 covers physical student workspace usability/readiness integration.

## 4. Traceability rules

- A passing dashboard rendering test does not prove RLS or exactly-one assignment enforcement.
- A fake StatusRepository test does not prove broker deployment or host probe correctness.
- Connect enabled is not proof of session authorization; FP-06 rechecks server-side.
- A requirement is PASS only with implementation commit, relevant test output, environment and reviewer.
- Unknown parent FR numbering is not repaired by FP-02; preserve v1.2 IDs and the existing FR-SEC-01..03 gap.
