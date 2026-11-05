# FP-02 Technical Design — Student Dashboard and Workspace

**Version:** 0.2 · **Date:** 5 November 2026  
**Status:** Target design; no application source change is claimed by this document.

## 1. Design boundaries

Preserve the native Kotlin + Jetpack Compose app and its ViewModel/repository pattern. Composables render state and emit user intents; they do not call Supabase or Ktor directly.

| Responsibility | Owner |
|---|---|
| AuthLevel and current session lifecycle | FP-01 |
| Student profile, exactly-one assignment resolution, safe Cloud PC label and assignmentTag | FP-03 |
| Android StatusRepository, freshness, response composition, refresh and Connect orchestration | FP-02 |
| Broker status endpoint, assignment derivation, status probe, assignmentTag and rate limiting | FP-04 |
| Windows credentials and NLA prompt | FP-05 |
| Grant, WSS, local bridge, FreeRDP and session lifecycle | FP-06+ |

FP-02 owns the client StatusRepository because its contract, cancellation, timeout, UI state and fake-server tests are part of the dashboard. FP-04 owns the server implementation. The two must be independently testable against one frozen contract.

## 2. Current source baseline and verification gap

The inspected main snapshot has MobiDeskViewModel.kt, MobiDeskRepositories.kt and MobiDeskApp.kt. The current ViewModel shown in main still reacts to Supabase SessionStatus.Authenticated and loads the workspace; no FULLY_AUTHENTICATED AuthLevel state was found in that file. The current workspace repository returns a list of VM summaries, and the dashboard code historically iterated those rows. A live Android StatusRepository was not found in the inspected main snapshot.

The Owner reports FP-01 implementation complete. Before implementation, identify and verify its exact source commit or merge it into main; do not substitute the raw SDK Authenticated state for AuthLevel.

## 3. Component flow

~~~text
FP-01 FULLY_AUTHENTICATED
      |
      v
WorkspaceViewModel (FP-02 composition)
      |-- WorkspaceRepository (FP-03 implementation)
      |     |-- StudentHeader
      |     |-- AssignmentPresentation.Assigned(label, assignmentTag)
      |
      |-- StatusRepository (FP-02 Android client)
            |-- GET /v1/vm-status (FP-04 server)
            |-- StatusSample(..., assignmentTag)
      |
      v
PairedWorkspaceSnapshot
      |-- tags equal: render one card + status
      |-- tags differ: discard and paired reload once
      |-- repeated mismatch: ASSIGN-02, no Connect
      |
      v
S-02 / S-03 Compose UI
      |
      |-- Connect action -> fresh status -> matching tag -> FP-05 callback
      |-- FP-06/broker rechecks during grant
~~~

## 4. Minimal screen-facing models

Use existing project model conventions where practical; do not create a broad new hierarchy.

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

The assignmentTag is opaque, equality-only, non-secret and not user-visible. It is not a VM ID, target selector, credential or authorization token. Do not persist it to logs or analytics. FP-03 and FP-04 must obtain the same value from the same canonical server-side assignment resolver and active assignment revision.

## 5. Initial load: parallel, session-scoped

After FP-01 emits FULLY_AUTHENTICATED:
1. Capture immutable Auth subject/session generation and current access token provider.
2. Start FP-03 workspace/profile read and FP-04 status request concurrently. The broker derives assignment itself; the status request has no selector.
3. Cancel both if the screen is left, the session generation changes, or a newer paired load supersedes them.
4. Ignore any completion from an obsolete session generation.
5. Require exactly one valid FP-03 assignment before rendering a Cloud PC card.
6. Compare workspace assignmentTag and status assignmentTag. If equal, publish one coherent snapshot.
7. On mismatch, discard both snapshots and repeat the paired load once. A second mismatch maps to ASSIGN-02; never render label A with status B.
8. If no valid assignment is returned, render the exact assignment result and do not expose a Connect action.

No sequential assignment-then-status dependency exists: the broker independently resolves the caller assignment, and the shared tag is the join check. Parallel loading removes one full network round trip.

## 6. Monotonic freshness

Do not compare sampledAt against System.currentTimeMillis(), LocalDateTime.now() or the phone wall clock.

For each HTTP status request capture:
- requestStartedElapsedMs = Android SystemClock.elapsedRealtime() immediately before dispatch;
- responseReceivedElapsedMs = elapsedRealtime() when the response headers/body are received;
- nowElapsedMs = elapsedRealtime() when rendering or deciding the handoff.

Compute a conservative age upper-bound:

ageUpperBoundMs = (responseReceivedElapsedMs - requestStartedElapsedMs)
                  + (nowElapsedMs - responseReceivedElapsedMs)

The first term is the full request round-trip duration and safely overestimates the age accrued between server sampling and client receipt, assuming FP-04 does not serve a sample from before this HTTP request. The second term is elapsed time on the monotonic device clock. Mark stale when ageUpperBoundMs >= staleAfterSeconds * 1000.

FP-04 MUST return a newly sampled observation for each successful status request; it MUST NOT reuse a prior request's cached sample. This is necessary because no server sample-age field or trusted device wall clock is present. sampledAt is formatted for display only. If the monotonic source is unavailable or the timing values are invalid, freshness is unknown and the handoff gate fails closed.

The sample lifetime remains the parent contract value of 15 seconds. Network latency can make the client expire a sample slightly early, but it cannot make an old sample appear younger because the device clock is wrong.

## 7. Android StatusRepository mechanics

- FP-02 implementation location: app/src/main/java/com/mobidesk/android/data/StatusRepository.kt or the existing equivalent data package.
- Obtain the current Supabase access JWT from the authenticated session provider for every request; do not cache it in UI state.
- Use HTTPS and GET /v1/vm-status with no body, query, subject ID or target selector.
- End-to-end client deadline: 8,000 ms, including network wait. This is deliberately above the 1,500 ms server probe budget to leave room for mobile RTT.
- Use cancellable coroutine/HTTP calls. Cancel on screen exit, sign-out, Auth subject change, replacement by a newer request or explicit cancellation. Cancellation is not an error toast.
- On timeout/network/5xx, replace any prior Ready status with indeterminate/unavailable state; never retain an actionable old success.
- On HTTP 429, parse Retry-After as a non-negative delta-seconds value and also parse retryAfterSeconds from the safe response body. Use the larger valid value. Do not schedule an automatic retry. Set a monotonic nextAllowedRequestAt and show the existing ASSIGN-02 user copy with a countdown or retry instruction.
- If a 429 omits usable retry metadata, use bounded user-driven exponential cooldowns of 2, 4, 8, 16 and at most 30 seconds. Reset after a successful status response.
- Apply a 2,000 ms local minimum interval to explicit refresh/Connect-triggered status requests. Serialize simultaneous refresh/Connect actions so one in-flight request is shared or later taps are ignored.
- Treat unknown HTTP status, malformed JSON and unknown enum values as indeterminate; never map them to RUNNING or RDP_NEGOTIATION_OK.

## 8. Status, assignment and UI state composition

Workspace UI state contains only:
- current session generation and FULLY_AUTHENTICATED gate;
- safe profile/header and typed assignment presentation;
- optional paired status sample;
- request in-flight state and last safe result;
- monotonic receipt timing and freshness;
- retry cooldown deadline;
- safe PRD error code/message;
- Connect action availability.

Do not keep JWT, guest credentials, grant, private endpoint, hostname, IP, port, VM UUID or raw server body in Compose state or SavedStateHandle.

## 9. Connect policy

PRD.md §9 is the single normative Connect rule. This section defines its technical implementation; it must not be independently reworded in another document.

Separate two concepts:
- **Connect button availability** is an affordance that lets a valid student request a fresh check. It is not proof that a session is authorized.
- **Handoff eligibility** is evaluated only after a successful fresh status response and matching assignmentTag.

A stale/missing/UNKNOWN/NOT_CHECKED result remains actionable so the student can tap Connect to initiate the required fresh status check. A known STOPPED/STARTING/STOPPING/ERROR lifecycle or TCP_UNREACHABLE/TCP_REACHABLE transport is a definitive negative and disables Connect; manual Refresh remains possible. While a request or server cooldown is active, Connect is temporarily disabled.

A handoff is permitted only after all predicates in PRD.md §9 are rechecked. The FP-05 callback has no VM or credential arguments and is invoked at most once per successful tap. FP-06/broker authorization is authoritative.

## 10. State copy and localisation

Use Android string resources, not literals inside Composables. R1 English India (en-IN) strings are the source of truth and must match PRD.md §6 and the unchanged parent error catalogue. Telugu te-IN translation is a follow-up; add it only after review by a fluent reviewer. Never mix languages in one sentence or silently transliterate technical statuses.

Lifecycle labels include STOPPED, STARTING, RUNNING, STOPPING, ERROR and UNKNOWN. Transport labels include NOT_CHECKED, TCP_UNREACHABLE, TCP_REACHABLE, RDP_NEGOTIATION_OK and UNKNOWN. Freshness strings distinguish no sample, current, seconds-old and stale.

## 11. Security and lifecycle

- Clear workspace/status on sign-out, AUTH-01, Auth subject/session generation change and invalidation.
- Never issue a status request outside FULLY_AUTHENTICATED.
- Never display a VM identifier or route.
- No R1 student power/assignment controls.
- Status and assignment are read-only; broker and FP-06 revalidate at grant time.
- RDP negotiation does not imply Windows NLA success.

## 12. Non-goals

No database migration or assignment resolver implementation, broker process, VM probe, session grant, WSS, FreeRDP or Dock implementation is included. Any required API/server change is assigned to the parent amendment and FP-04.
