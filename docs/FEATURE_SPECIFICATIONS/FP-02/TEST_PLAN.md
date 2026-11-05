# FP-02 Test Plan — Student Dashboard and Workspace

**Version:** 0.1 — source-grounded Owner review draft  
**Date:** 4 November 2026  
**Status:** Test cases proposed; none claimed run by this documentation package.

## 1. Testing strategy

Test FP-02 as a pure state composition and Compose presentation feature with fake FP-03 workspace and FP-04 status repositories. Do not require real student identities, a live broker or production VM merely to run deterministic UI/unit tests.

Integration tests must use an Owner-approved isolated environment and synthetic accounts. Real broker status tests must use the approved HTTPS endpoint and the current JWT. Physical UI/accessibility checks are recorded separately.

FP-02 does not retest the full RDP stream, Windows NLA, grant lifecycle or server assignment implementation owned by adjacent features; it tests that its own UI consumes their authoritative contracts correctly.

## 2. Preconditions and baseline

1. FP-01 exposes a testable `FULLY_AUTHENTICATED` student state.
2. FP-03's workspace result expresses profile and assignment cardinality without exposing routing fields.
3. FP-04's status DTO contains the documented lifecycle/transport enums, sampledAt, probeDurationMs, staleAfterSeconds and requestId.
4. Existing sign-in and dashboard regression tests pass before FP-02 changes.
5. Tests use synthetic identities/data only; no real student data is copied into fixtures or screenshots.

If a precondition is absent, mark dependent integration cases BLOCKED rather than bypassing it.

## 3. Core tests

| Test ID | Scenario | Expected result | Level |
|---|---|---|---|
| FP02-CORE-01 | FP-01 session is Initializing/restoring | Workspace/profile/status requests do not run; loading/restore state is shown | ViewModel |
| FP02-CORE-02 | No authenticated session / SIGNED_OUT | Sign-in flow shown; no workspace data retained | ViewModel/UI |
| FP02-CORE-03 | FP-01 RECOVERY_ONLY session | Workspace/profile/assignment/status requests do not run | Integration |
| FP02-CORE-04 | Fully authenticated provisioned student with valid profile and one assignment | Student header and exactly one Cloud PC card displayed | Unit/Compose |
| FP02-CORE-05 | Optional college/institution label is null/blank | Header remains valid; optional label omitted | Unit/Compose |
| FP02-CORE-06 | Profile absent / account not provisioned | Safe not-provisioned state; no fake profile or card | Unit/Compose |
| FP02-CORE-07 | No active assignment | Exact ASSIGN-01 copy; no Connect action | Unit/Compose |
| FP02-CORE-08 | Duplicate/ambiguous/inactive/unmapped assignment | Exact ASSIGN-03 or FP-03 contract result; no first-row fallback; no Connect | Contract/Compose |
| FP02-CORE-09 | Assignment verification is temporarily unavailable | Exact ASSIGN-02 behavior; retry follows parent policy; no Connect | Unit/Compose |
| FP02-CORE-10 | One valid assignment with no status sample | Lifecycle/transport shown as checking/unknown; Connect disabled | Unit/Compose |
| FP02-CORE-11 | Every lifecycle enum | Correct parent label and action: STOPPED, STARTING, RUNNING, STOPPING, ERROR, UNKNOWN | Unit |
| FP02-CORE-12 | Every RDP transport enum | Correct distinct label; TCP_REACHABLE and UNKNOWN never enable Connect | Unit |
| FP02-CORE-13 | RUNNING + RDP_NEGOTIATION_OK + fresh + valid assignment + fully authenticated | Connect enabled | Unit |
| FP02-CORE-14 | Any single Connect prerequisite false | Connect disabled; exhaustive truth table covers all combinations | Unit |
| FP02-CORE-15 | Status sample reaches staleAfterSeconds boundary (15 seconds in current contract) | Sample becomes stale and Connect is disabled | Unit/fake clock |
| FP02-CORE-16 | Manual refresh succeeds | Refresh indication shown, new sample replaces previous state, Connect recalculated | Unit/Compose |
| FP02-CORE-17 | Manual refresh fails after previous ready result | Previous result cannot remain actionable; mark unavailable/stale, Connect disabled | Unit |
| FP02-CORE-18 | Student taps Connect while previous sample is stale | Fresh status requested first; no FP-05 handoff until result is eligible | Unit/Compose |
| FP02-CORE-19 | Fresh status changes from ready to stopped/unknown/unreachable | No handoff; exact error/state shown | Unit/Compose |
| FP02-CORE-20 | Fresh status remains eligible | Exactly one FP-05 credential-prompt handoff; no VM selector or grant passed | Unit/navigation |
| FP02-CORE-21 | Auth user/session changes while workspace visible | Prior profile, assignment and status cleared before new user's state appears | Unit |
| FP02-CORE-22 | Sign-out / AUTH-01 session expiry | Workspace and cached status cleared; FP-01 sign-in route shown | Integration/UI |
| FP02-CORE-23 | Settings and sign-out controls | Settings opens; sign-out delegates to FP-01; no duplicate auth implementation | Compose |
| FP02-CORE-24 | Dock placeholder | Correct informational R2 text only; no enabled action/pairing route | Compose |
| FP02-CORE-25 | Status request contract | Bearer current-user token; no body/query/VM selector; no endpoint fields in response DTO | Contract |
| FP02-CORE-26 | Sensitive values in UI/logs/state | No VM ID, UUID, hostname, IP, port, Guac field, other-student data, token or raw exception | Static/unit |
| FP02-CORE-27 | API 401 / AUTH-01 | Exact parent copy and FP-01 re-auth route; clear stale workspace state | Contract/Compose |
| FP02-CORE-28 | ASSIGN/VM error copy | Exact English PRD strings and exact retry behavior; no raw backend message | Snapshot/Compose |
| FP02-CORE-29 | Duplicate refresh/connect taps | Requests serialized/debounced; no multiple simultaneous status requests or handoffs | Unit/UI |
| FP02-CORE-30 | Re-entry after refresh/navigation or Activity recreation | No stale status presented as fresh; view state restored only under the same valid Auth subject/session | Device/integration |

## 4. Status presentation truth table

| Lifecycle | RDP transport | Fresh? | Connect |
|---|---|---:|---|
| RUNNING | RDP_NEGOTIATION_OK | Yes | Enabled, after refresh-before-handoff |
| RUNNING | TCP_REACHABLE | Yes | Disabled |
| RUNNING | NOT_CHECKED | Yes | Disabled |
| RUNNING | UNKNOWN | Yes | Disabled |
| RUNNING | RDP_NEGOTIATION_OK | No | Disabled |
| STOPPED | Any | Any | Disabled |
| STARTING | Any | Any | Disabled |
| STOPPING | Any | Any | Disabled |
| ERROR | Any | Any | Disabled |
| UNKNOWN | Any | Any | Disabled |
| Any | TCP_UNREACHABLE | Any | Disabled |
| Any | Any | Any while refreshing | Disabled |
| Any | Any | Any without valid assignment/Auth | Disabled |

Additional rule: do not convert a null/missing status field into RUNNING or RDP_NEGOTIATION_OK. Invalid/unknown enum values fail closed.

## 5. Exact PRD error-copy tests

Test snapshot equality against the parent English strings:

- ASSIGN-01 — "No Cloud PC is assigned to your account. Contact the lab administrator."
- ASSIGN-02 — "We could not verify your assigned Cloud PC. Try again."
- ASSIGN-03 — "Your Cloud PC assignment needs administrator review."
- VM-01 — "Your Cloud PC is powered off. Contact the lab administrator."
- VM-02 — "Your Cloud PC is starting or is not ready yet."
- VM-03 — "Your Cloud PC cannot be reached right now."
- AUTH-01 — "Your sign-in session expired. Sign in again."

The source contract is `docs/DESIGN.md §8` and `docs/API_CONTRACT.md §7`. Do not invent new PRD error codes.

## 6. Accessibility and UX acceptance

Manual review on an approved emulator and physical device:
- Every button has a meaningful accessible name.
- Status lifecycle and transport are read distinctly by TalkBack.
- Status is not communicated by color alone.
- Focus order is header/settings, student identity, Cloud PC label, lifecycle, transport, sample age, refresh, Connect, Dock information, sign out.
- Loading/refresh and errors are announced once and remain discoverable.
- At 1.3x and 1.5x font scaling, no essential label or primary action is clipped.
- Refresh/Connect disabled states have semantic explanation, not only dimmed color.
- No empty/unknown state resembles a successful "Online" state.

Record device model, Android/API, app commit, tester, results and any screenshots in a private test report; do not commit student screenshots.

## 7. Negative/security tests

- User A cannot render user B's profile or assigned Cloud PC.
- The screen cannot accept a user-entered subject ID or VM selector.
- A forged/local change to UI state does not alter broker-side assignment/grant authorization.
- No client call exposes VM ID, hostname, IP, RDP port or Guacamole connection ID.
- No VM power or assignment mutation control is exposed to students.
- A stale cached Ready response never enables Connect.
- A network error after a previous Ready response must clear actionable readiness.
- No raw status response, Authorization header, JWT or exception is logged.
- Status request contains no body/query target selector.
- A role other than student is routed by FP-01/account role flow, not displayed as student.

## 8. Release criteria

FP-02 test acceptance requires:
- all applicable CORE tests pass;
- baseline authentication/dashboard regressions pass;
- FP-03 and FP-04 integration contracts are exercised in an isolated environment;
- exact PRD copy/status semantics are verified;
- TalkBack/font scaling and account-switch tests pass;
- no cross-student or infrastructure-data disclosure;
- source commit, test output and environment recorded in PROGRESS.md.

A fake StatusRepository test does not prove broker deployment. A visible ready state does not prove grant authorization or successful NLA. A successful build alone is not feature acceptance.
