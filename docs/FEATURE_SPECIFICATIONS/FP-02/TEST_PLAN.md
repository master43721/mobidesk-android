# FP-02 Test Plan — Student Dashboard and Workspace

**Version:** 0.2 · **Date:** 5 November 2026  
**Status:** Test design; no test run is claimed by this document.

## 1. Strategy

Test the ViewModel/state composition with fake FP-03 WorkspaceRepository and FP-02 StatusRepository backed by a fake HTTP server. Use synthetic accounts only. Contract/integration tests use an approved isolated backend. Physical UI checks are separate. FP-02 does not claim RLS, probe, grant, NLA or FreeRDP acceptance.

## 2. Preconditions

- FP-01 exposes a verified FULLY_AUTHENTICATED state and session generation.
- FP-03 exposes a typed zero/one/invalid result, safe label and assignmentTag.
- FP-04 implements the reviewed status DTO, matching assignmentTag, request-fresh samples and 429 Retry-After.
- FP-05 exposes a no-argument destination callback.
- Baseline unit/Compose tests pass before FP-02 code changes.

Missing preconditions are BLOCKED, not replaced by mocks in integration or release evidence.

## 3. Core cases

| ID | Scenario | Expected result | Level |
|---|---|---|---|
| CORE-01 | FP-01 Initializing/restoring | No workspace/status calls; restore state | ViewModel |
| CORE-02 | SIGNED_OUT | Sign-in; no cached workspace | ViewModel/UI |
| CORE-03 | RECOVERY_ONLY | No protected workspace/status calls | Integration |
| CORE-04 | Fully authenticated, profile and exactly one assignment | Header and one Cloud PC card | Unit/Compose |
| CORE-05 | College label null/blank | Omit label | Unit/Compose |
| CORE-06 | Profile absent | Safe account-not-provisioned state; no card | Unit/Compose |
| CORE-07 | No active assignment | Exact ASSIGN-01; no Connect | Unit/Compose |
| CORE-08 | Duplicate/inactive/unmapped assignment | FP-03 invalid result/ASSIGN-03; no first-row fallback | Contract/Compose |
| CORE-09 | Assignment verification failure | Exact ASSIGN-02; bounded user retry | Unit/Compose |
| CORE-10 | Assigned workspace, no sample | Status not checked; Connect remains a fresh-check affordance after initial load | Unit/Compose |
| CORE-11 | Every lifecycle enum | STOPPED/STARTING/RUNNING/STOPPING/ERROR/UNKNOWN copy is explicit | Unit |
| CORE-12 | Every transport enum | NOT_CHECKED/TCP_UNREACHABLE/TCP_REACHABLE/RDP_NEGOTIATION_OK/UNKNOWN is distinct | Unit |
| CORE-13 | Fresh RUNNING + RDP_NEGOTIATION_OK + matching tag | Connect available; tap still refreshes before handoff | Unit |
| CORE-14 | Definitive negative lifecycle/transport | Connect disabled; manual Refresh remains available | Unit |
| CORE-15 | Conservative age reaches staleAfterSeconds boundary | Stale at >=15,000 ms; Connect remains available to request a fresh status unless a known-negative state exists | Fake monotonic clock |
| CORE-16 | Manual refresh succeeds | Busy state, replaces sample and recalculates freshness | Unit/Compose |
| CORE-17 | Manual refresh fails after Ready | Old sample cannot authorize; show unavailable/indeterminate state | Unit |
| CORE-18 | Tap Connect while prior sample is stale | Fresh status requested; no handoff until fresh eligible result and matching tag | Unit/Compose |
| CORE-19 | Fresh status changes to STOPPED/UNKNOWN/unreachable | No handoff; exact state shown | Unit/Compose |
| CORE-20 | Fresh eligible status and matching tag | Exactly one no-argument FP-05 handoff | Unit/navigation |
| CORE-21 | Auth subject/session changes in flight | Clear prior state; ignore old asynchronous responses | Unit |
| CORE-22 | Sign-out/AUTH-01 | Clear cache and delegate sign-in route to FP-01 | Integration/UI |
| CORE-23 | Settings/sign-out controls | Delegate to FP-01; no duplicate auth | Compose |
| CORE-24 | Dock placeholder | Exact R2 informational state; no action | Compose |
| CORE-25 | Status request contract | Current bearer JWT; GET; no body/query/subject/VM selector; allowlisted DTO | Fake server/contract |
| CORE-26 | Sensitive values in UI/state/logs | No user ID, VM ID/UUID, hostname/IP/port, Guac field, token or raw exception | Static/unit |
| CORE-27 | API 401/AUTH-01 | Exact AUTH-01 copy; clear state; FP-01 re-auth route | Contract/Compose |
| CORE-28 | Parent assignment/VM error copy | Seven exact PRD strings; no raw backend message | Snapshot |
| CORE-29 | Duplicate Refresh/Connect taps | One in-flight status request; no duplicate handoff; 2s local debounce | Unit/UI |
| CORE-30 | Re-entry/activity recreation | No persisted status claimed fresh; reload or indeterminate state | Device/integration |
| CORE-31 | Status request exceeds 8s | Cancel HTTP call; mark unavailable; no old Ready action; retry is user-driven | Fake server |
| CORE-32 | Screen exit/session change/newer request during HTTP call | Request cancelled; cancellation produces no error toast; late result ignored | Coroutine test |
| CORE-33 | HTTP 429 with Retry-After header and body | Use larger valid delay; show ASSIGN-02; no automatic retry before deadline | Fake server |
| CORE-34 | 429 lacks valid retry metadata | User-driven fallback cooldown 2/4/8/16/30s; reset after success | Fake clock |
| CORE-35 | Initial workspace and status read | Requests start concurrently after AuthLevel; measured critical path is max(latencies), not sum | ViewModel |
| CORE-36 | Assignment tags match | One coherent card/status snapshot can render | Unit/contract |
| CORE-37 | Assignment tags differ | Discard pair, reload both once; second mismatch shows ASSIGN-02 and no Connect handoff | Unit/contract |
| CORE-38 | Device wall clock jumps forward/back | Freshness unchanged; monotonic clock only | Fake clock/device |
| CORE-39 | Status freshness from a request-fresh sample | Receipt-age upper-bound includes request RTT; stale at boundary; server does not reuse prior request sample | Contract/fake clock |
| CORE-40 | Unknown/malformed enum or missing assignmentTag | Indeterminate/error state; fail closed; no handoff | Contract/unit |

## 4. Connect and readiness truth table

This table is the test oracle for PRD.md §9. It distinguishes whether the action can be requested from whether a handoff is eligible.

| Last observed status | Connect affordance | Handoff without new request |
|---|---|---|
| No sample / NOT_CHECKED | Available after initial load completes | Never |
| UNKNOWN lifecycle/transport | Available | Never |
| Stale formerly-ready sample | Available | Never |
| Fresh RUNNING + RDP_NEGOTIATION_OK + matching tag | Available | Never; tap refreshes first |
| STOPPED / STARTING / STOPPING / ERROR | Disabled | Never |
| TCP_UNREACHABLE / TCP_REACHABLE | Disabled | Never |
| Request in flight or Retry-After cooldown | Temporarily disabled | Never |
| Auth invalid or assignment invalid | Hidden/disabled | Never |

After every tap, a successful new sample must be fresh, match the currently displayed assignmentTag and report RUNNING plus RDP_NEGOTIATION_OK before the single FP-05 handoff. The broker then rechecks independently.

## 5. Error and copy snapshots

The following seven English strings must match MobiDesk PRD v1.2 §12 exactly:

- AUTH-01: Your sign-in session expired. Sign in again.
- ASSIGN-01: No Cloud PC is assigned to your account. Contact the lab administrator.
- ASSIGN-02: We could not verify your assigned Cloud PC. Try again.
- ASSIGN-03: Your Cloud PC assignment needs administrator review.
- VM-01: Your Cloud PC is powered off. Contact the lab administrator.
- VM-02: Your Cloud PC is starting or is not ready yet.
- VM-03: Your Cloud PC cannot be reached right now.

Also snapshot all enum helper copy and freshness states from PRD.md §6. They are state descriptions, not new error IDs.

## 6. Accessibility and UX

On approved emulator and physical device:
- TalkBack announces identity, lifecycle, transport, freshness, loading and errors meaningfully.
- Status is not communicated by color alone.
- Focus order is predictable; busy/error announcements occur once.
- Disabled Connect has an accessible explanation.
- At 1.3x and 1.5x font scale, the card and primary actions are not clipped.
- All labels and helper strings come from en-IN resources; te-IN is added only after translation review.
- No empty/unknown state resembles an Online/Ready state.

Record device model, Android/API, app SHA, tester, results and screenshots using synthetic data only.

## 7. Negative/security tests

- Cross-student data never appears.
- No caller-supplied subject/VM target reaches StatusRepository or Compose events.
- TCP_REACHABLE never enables a handoff.
- A stale cached Ready result never handoffs.
- AssignmentTag mismatch never renders combined card/status.
- Forged local UI state cannot alter broker authorization.
- No private field, JWT, Authorization header, guest credential, grant or raw exception enters logs, navigation, saved state, crash reports or analytics.
- Sign-out/account switch clears state and cancels requests.
- Status GET contains no body or target selector.
- Rate limit and timeout do not loop or auto-retry.

## 8. Release criteria

Every applicable CORE test passes with report evidence; baseline regressions pass; FP-03/FP-04 contracts are exercised in an isolated environment; exact copy, monotonic freshness, timeout, 429 and tag consistency are verified; TalkBack/scaling passes; privacy scans pass; source SHA, Gradle tasks, environment and reviewers are recorded in PROGRESS.md. A green build alone is not acceptance.
