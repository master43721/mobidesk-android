# FP-02 Technical Design — Student Dashboard and Workspace

**Version:** 0.2 · **Date:** 5 November 2026  
**Status:** Target design; no application code change is claimed.

## 1. Ownership

| Responsibility | Owner |
|---|---|
| FULLY_AUTHENTICATED session/recovery/sign-out | FP-01 |
| Profile, exactly-one assignment, safe label and assignmentTag | FP-03 |
| Android StatusRepository, freshness, status composition, refresh and Connect orchestration | FP-02 |
| Broker status endpoint, probe, assignmentTag and rate limiting | FP-04 |
| Windows credential prompt/NLA | FP-05 |
| Grant, WSS, local transport, FreeRDP and lifecycle | FP-06+ |

FP-02 owns the Android StatusRepository and its fake-server contract tests. FP-04 owns the server endpoint/probe.

## 2. Repository integration finding

The inspected main MobiDeskViewModel.kt blob is 32bbfc45c96baf9021bd34f2d860d44bbd95b6ed. It still observes Supabase SessionStatus.Authenticated, sets isAuthenticated and loads workspace; no FULLY_AUTHENTICATED AuthLevel was found in that file. The current workspace repository returns VM summaries and no live Android StatusRepository was found in the inspected snapshot.

The Owner reports FP-01 complete. Verify its exact source commit/merge before FP-02 implementation. Do not replace AuthLevel with the raw SDK Authenticated event.

## 3. Client flow

~~~text
FP-01 FULLY_AUTHENTICATED
    |-- FP-03 workspace/profile -> safe label + assignmentTag
    |-- FP-02 StatusRepository -> FP-04 GET /v1/vm-status -> status + assignmentTag
    Both requests start concurrently after AuthLevel.
    |
    v
Pair only if tags match. If mismatch, discard and reload both once.
Second mismatch -> ASSIGN-02; no Connect handoff.
    |
    v
Compose S-02 / S-03
Connect tap -> fresh status -> matching tag -> one no-argument FP-05 handoff.
FP-06/broker rechecks assignment/readiness during grant.
~~~

## 4. Minimal models

~~~kotlin
data class AssignedWorkspace(
    val displayLabel: String,
    val assignmentTag: String
)

data class VmStatusSample(
    val lifecycle: VmLifecycle,
    val rdpTransport: RdpTransport,
    val sampledAt: Instant,
    val probeDurationMs: Long,
    val staleAfterSeconds: Long,
    val requestId: String,
    val assignmentTag: String
)
~~~

assignmentTag is opaque, equality-only, non-secret, not user-visible, not an authorization credential and not a target selector. FP-03/FP-04 must obtain the same value from one canonical server-side resolver and assignment revision. Never log or persist it.

## 5. Parallel, session-scoped loading

After FULLY_AUTHENTICATED:
1. Capture immutable Auth subject/session generation and token provider.
2. Launch FP-03 workspace/profile read and FP-04 status request concurrently; status has no selector.
3. Cancel both when screen exits, session generation changes or newer paired load supersedes them.
4. Ignore late results from obsolete session generations.
5. Render a card only for FP-03 typed Assigned.
6. Publish a combined snapshot only when assignmentTag matches.
7. On mismatch, discard and reload both once. A second mismatch maps to ASSIGN-02 and blocks Connect.

No serial assignment-then-status dependency exists: the broker resolves assignment independently; the opaque tag joins results. Parallel execution removes one network round trip.

## 6. Monotonic freshness

Never compare sampledAt with System.currentTimeMillis(), LocalDateTime.now() or phone wall time.

Capture using Android SystemClock.elapsedRealtime():
- requestStartedElapsedMs immediately before dispatch;
- responseReceivedElapsedMs when the response is received;
- nowElapsedMs at display/Connect evaluation.

Compute a conservative age upper-bound:

~~~text
ageUpperBoundMs =
    (responseReceivedElapsedMs - requestStartedElapsedMs)
    + (nowElapsedMs - responseReceivedElapsedMs)
~~~

The first term is the full request round trip and bounds age accrued between a newly sampled observation and client receipt. The second is monotonic time since receipt. Mark stale when ageUpperBoundMs >= staleAfterSeconds * 1000.

This guarantee requires FP-04 to return a newly sampled observation per successful status request and not reuse a pre-request cached sample. sampledAt is formatted for display only. Invalid/missing monotonic timing yields indeterminate freshness and denies handoff. Network latency may expire a sample slightly early; the handset clock cannot make an old sample appear fresh.

## 7. Android StatusRepository mechanics

Implementation location: app/src/main/java/com/mobidesk/android/data/StatusRepository.kt or an equivalent existing data package.

- Current Supabase access JWT in Authorization for each request.
- HTTPS GET /v1/vm-status; no body, query, subject ID or target selector.
- Client deadline 8,000 ms end-to-end, above the parent 1,500 ms server probe budget to allow mobile RTT.
- Cancellable coroutine/HTTP call. Cancel on screen exit, sign-out, Auth change, replacement or explicit cancel. Cancellation is not an error toast.
- On timeout/network/5xx replace prior Ready status with indeterminate state. Never keep old actionable success.
- On 429 parse Retry-After as non-negative delta seconds and retryAfterSeconds body. Use the larger valid value. No automatic retry; store nextAllowedRequestAt using monotonic time and show existing ASSIGN-02 copy.
- If 429 has no valid retry metadata, use user-driven fallback cooldowns 2/4/8/16/30 seconds; reset after success.
- Enforce 2,000 ms local minimum interval for manual Refresh/Connect status calls; serialize taps to one in-flight request.
- Unknown/malformed enum or response is indeterminate, never RUNNING/READY.

## 8. State and data boundary

Keep only Auth session generation, safe profile/assignment, optional paired status, monotonic timing, request/cooldown state, safe PRD error and action availability. Do not keep JWT, Windows credentials, grant, private endpoint, hostname/IP/port, VM UUID or raw server body in Compose state or SavedStateHandle.

## 9. Connect implementation

PRD.md §6 is the one normative Connect rule; do not restate it elsewhere as a separate truth table.

Separate button availability (a valid student can request a new check) from handoff eligibility (computed only after new fresh status and matching tag). Stale/missing/UNKNOWN/NOT_CHECKED can initiate a fresh check. Known-negative states disable Connect; in-flight/cooldown/auth/assignment gates disable it temporarily. Only the complete PRD §6 predicate allows one no-argument FP-05 callback.

## 10. Strings, security and non-goals

Use Android string resources. R1 source is reviewed en-IN; te-IN follows only after reviewed translation. Clear data on sign-out/AUTH-01/session generation change. Broker is authoritative at grant time. No database migration, assignment resolver, broker process, VM probe, grant, WSS, FreeRDP or Dock implementation is included in FP-02.
