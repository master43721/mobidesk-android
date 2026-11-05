# FP-02 Test Plan — Student Dashboard and Workspace

**Version:** 0.2 · **Date:** 5 November 2026  
**Status:** Test design only; no test run is claimed.

## 1. Strategy and preconditions

Use fake FP-03 WorkspaceRepository and FP-02 StatusRepository/fake HTTP server for deterministic tests. Contract/integration tests use an approved isolated backend. A fake never proves RLS, broker deployment, grant, NLA or FreeRDP.

Before implementation verify FP-01 FULLY_AUTHENTICATED, FP-03 typed assignment+safe label+assignmentTag, FP-04 status DTO/tag/request-fresh behavior/429 Retry-After, and FP-05 no-argument callback. Missing dependencies are BLOCKED.

## 2. Core cases

| ID | Scenario | Expected | Level |
|---|---|---|---|
| CORE-01 | FP-01 Initializing/restoring | No workspace/status calls | ViewModel |
| CORE-02 | SIGNED_OUT | Sign-in; no cached workspace | UI |
| CORE-03 | RECOVERY_ONLY | No protected requests | Integration |
| CORE-04 | Fully authenticated, one assignment | Header + one card | Unit/Compose |
| CORE-05 | College label null/blank | Omit label | Unit |
| CORE-06 | Profile absent | Safe missing-profile state; no card | UI |
| CORE-07 | No assignment | Exact ASSIGN-01; no Connect | UI |
| CORE-08 | Duplicate/inactive/unmapped assignment | FP-03 invalid/ASSIGN-03; no first-row fallback | Contract |
| CORE-09 | Assignment lookup transient failure | ASSIGN-02; bounded user retry | UI |
| CORE-10 | Assigned but no sample | Not checked; Connect is a fresh-check affordance | UI |
| CORE-11 | All lifecycle enums | Six states have explicit copy | Unit |
| CORE-12 | All transport enums | Five states remain distinct | Unit |
| CORE-13 | Fresh RUNNING + RDP_NEGOTIATION_OK + matching tag | Connect available; tap still refreshes | Unit |
| CORE-14 | Known-negative lifecycle/transport | Connect disabled; manual Refresh remains | Unit |
| CORE-15 | Conservative age reaches 15s after any prior state, including STOPPED | Stale at >=15,000ms; former positive or negative becomes indeterminate and Connect is available for a fresh check | Fake clock |
| CORE-16 | Refresh succeeds | Busy, replaces sample, recalculates age | UI |
| CORE-17 | Refresh fails after Ready | Old sample not actionable | Unit |
| CORE-18 | Connect tapped with stale sample | Fresh status first; no premature handoff | UI |
| CORE-19 | Fresh result becomes STOPPED/UNKNOWN/unreachable | No handoff; truthful copy | Unit |
| CORE-20 | Fresh eligible result and matching tag | Exactly one no-argument FP-05 handoff | Navigation |
| CORE-21 | Auth subject/session changes in flight | Clear old state; ignore late result | Unit |
| CORE-22 | Sign-out/AUTH-01 | Clear state and delegate re-auth to FP-01 | Integration |
| CORE-23 | Settings/sign-out | FP-01 delegation; no duplicate auth | Compose |
| CORE-24 | Dock placeholder | Inert R2 state | Compose |
| CORE-25 | Status request contract | Current bearer; GET; no body/query/subject/VM selector; allowlisted DTO | Fake server |
| CORE-26 | Sensitive fields in UI/state/logs | No IDs, endpoint, token, raw exception or grant | Static/unit |
| CORE-27 | HTTP 401/AUTH-01 | Exact copy, clear state, FP-01 re-auth | Contract |
| CORE-28 | Parent assignment/VM error copy | Seven exact strings | Snapshot |
| CORE-29 | Duplicate Refresh/Connect taps | One request; one handoff max; 2s local debounce | Unit/UI |
| CORE-30 | Process recreation/re-entry | No persisted status claimed fresh | Device |
| CORE-31 | Status exceeds 8s | Cancel call; unavailable; old Ready not actionable | Fake server |
| CORE-32 | Screen exit/session change/new request | Cancel old request; no toast for cancellation; late result ignored | Coroutine |
| CORE-33 | HTTP 429 with header/body retry values | Use larger valid delay; ASSIGN-02; no automatic retry | Fake server |
| CORE-34 | 429 lacks usable retry metadata | User-driven fallback cooldown 2/4/8/16/30s; reset on success | Fake clock |
| CORE-35 | Initial workspace and status load | Start concurrently after AuthLevel | ViewModel |
| CORE-36 | Assignment tags match | One coherent card/status snapshot | Unit |
| CORE-37 | Assignment tags differ | Discard; reload pair once; second mismatch = ASSIGN-02/no handoff | Unit |
| CORE-38 | Phone wall clock jumps | Freshness unchanged | Fake clock |
| CORE-39 | Request-fresh status sample | RTT included in age upper-bound; old server cache rejected | Contract |
| CORE-40 | Missing tag, malformed JSON or unknown enum | Indeterminate; fail closed; no handoff | Contract |

## 3. Connect test oracle

PRD.md §6 is the normative policy.

| Last observed status | Connect affordance | Direct handoff |
|---|---|---|
| No sample / NOT_CHECKED | Available after initial load | Never |
| UNKNOWN lifecycle/transport | Available | Never |
| Stale formerly-ready or formerly-negative sample | Available | Never |
| Fresh RUNNING + RDP_NEGOTIATION_OK + matching tag | Available | Never; tap refreshes first |
| Fresh STOPPED / STARTING / STOPPING / ERROR | Disabled | Never |
| Fresh TCP_UNREACHABLE / TCP_REACHABLE | Disabled | Never |
| In-flight request or Retry-After cooldown | Temporarily disabled | Never |
| Auth or assignment invalid | Hidden/disabled | Never |

After a tap, only a new fresh sample with matching assignmentTag, RUNNING and RDP_NEGOTIATION_OK can invoke FP-05. Broker/grant checks remain authoritative.

| CORE-41 | FP-06/grant reports ASSIGN-03 after a previously valid card | Clear card/status, cancel requests, suppress Connect, reload assignment/status pair once; never retry against old assignment | Fake grant/client integration |\n\n## 4. Exact parent error snapshots

These seven strings must match PRD v1.2 §12 exactly:
- AUTH-01: Your sign-in session expired. Sign in again.
- ASSIGN-01: No Cloud PC is assigned to your account. Contact the lab administrator.
- ASSIGN-02: We could not verify your assigned Cloud PC. Try again.
- ASSIGN-03: Your Cloud PC assignment needs administrator review.
- VM-01: Your Cloud PC is powered off. Contact the lab administrator.
- VM-02: Your Cloud PC is starting or is not ready yet.
- VM-03: Your Cloud PC cannot be reached right now.

Snapshot enum/freshness state copy in PRD.md §4; these are state labels, not new error IDs.

## 5. Accessibility and negative/security tests

- TalkBack announces identity, lifecycle, transport, freshness, loading and errors; status is not color-only.
- Focus order predictable; no clipped primary action at 1.3x/1.5x font scale.
- Disabled Connect has an accessible explanation; async announcements occur once.
- Cross-student data never appears; no caller-supplied target selector reaches client or backend.
- TCP_REACHABLE never enables handoff; stale/mismatched status never hands off.
- No JWT, Authorization header, password, grant, assignmentTag, route or RDP payload in logs, saved state, crash reports or analytics.
- Sign-out/account switch cancels requests and clears state.
- 429/timeout do not loop or auto-retry.

## 6. Release evidence

All applicable CORE tests pass with report evidence; FP-03/04 contracts run in an isolated environment; monotonic freshness, timeout, cancellation, 429 and assignmentTag consistency are proven; exact copy/accessibility/privacy checks pass; source SHA, test tasks, reports, environment and reviewer are recorded. Green build alone is not acceptance.
