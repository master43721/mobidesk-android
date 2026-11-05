# FP-02 Requirement Traceability

**Version:** 0.2 · **Date:** 5 November 2026  
**Parent:** MobiDesk PRD v1.2; proposed amendment pending.

## 1. Parent ownership

| Parent requirement | FP-02 responsibility | Adjacent owner |
|---|---|---|
| FR-AUTH-01 | Consume authenticated state; delegate sign-out/re-auth | FP-01 owns auth/session/recovery |
| FR-AUTH-02 | Render only the current student's returned data; account-switch safety | FP-03 owns RLS/profile/assignment reads |
| FR-VM-01 | **FP-02 owns exactly-one Cloud PC card presentation, with no list or first-row fallback.** | FP-03 owns authoritative singleton resolution and assignment authorization. Both are required. |
| FR-VM-02/03 | No client target selector; no-argument handoff | FP-03/FP-06 own server target derivation/grant enforcement |
| FR-VM-04 | Render lifecycle and transport distinctly; client timeout state | FP-04 owns server probe and 1,500 ms budget |
| FR-VM-05 | Monotonic freshness UI, manual refresh and refresh-before-Connect | FP-04 owns sample/probe; FP-06/broker rechecks grant |
| FR-OBS-01 | Parent-approved safe presentation diagnostics only | Parent telemetry owner |
| FR-DOCK-01/02 | Informational placeholder only | R2 |

FR-VM-01 is a presentation requirement for FP-02 and a data authorization requirement for FP-03. A correct UI does not prove server assignment uniqueness.

## 2. FR-to-CORE mapping

| Requirement | Tests | Evidence |
|---|---|---|
| FP02-FR-001 | CORE-01/02/03/21/22/32 | AuthLevel + session generation |
| FP02-FR-002 | CORE-04/05/21/26/35 | Own-profile/account-switch |
| FP02-FR-003 | CORE-05 | Null/blank label |
| FP02-FR-004 | CORE-04/06/07/08 | Exactly-one card |
| FP02-FR-005 | CORE-08/25/26/40 | No selector/private field |
| FP02-FR-006 | CORE-10/11/12 | Separate lifecycle/transport |
| FP02-FR-007 | CORE-11/12/40 | Complete enum copy |
| FP02-FR-008 | CORE-15/17/30/38/39 | Monotonic time / wall-clock jump |
| FP02-FR-009 | CORE-10/13/14/18/19 | Connect availability predicate |
| FP02-FR-010 | CORE-18/19/20/29/36/37 | Fresh status, matching tag, one handoff |
| FP02-FR-011 | CORE-16/29/33/34 | Refresh/cooldown/429 |
| FP02-FR-012 | CORE-17/31/32/33/34/37/40/41 | Failure invalidates old readiness |
| FP02-FR-013 | CORE-09/27/28/33 | Exact parent error copy |
| FP02-FR-014 | CORE-06–12/16–19/31–40 | All states |
| FP02-FR-015 | CORE-22/23 | FP-01 delegation |
| FP02-FR-016 | CORE-24 | Inert Dock |
| FP02-FR-017 | CORE-21/22/30/32/35/41 | Cancellation/cache clearing |
| FP02-FR-018 | Accessibility review §6 | TalkBack/scaling |

## 3. Previously unmapped tests

- CORE-25 → FR-005/FR-010/FR-012 (status API/no selector).
- CORE-27 → FR-001/FR-013/FR-017 (AUTH-01 and clearing state).
- CORE-29 → FR-009/FR-010/FR-011/FR-012 (serialization/debounce/handoff).

## 4. Parent test IDs retained

T-AUTH-04/05; T-VM-01..03; T-VM-08/09/10/11; E-02..E-07; T-NEG-02/03/04; T-PRIV-01/02. A UI test does not prove RLS, broker deployment or grant authorization. Every PASS links source SHA, report, environment and reviewer.
