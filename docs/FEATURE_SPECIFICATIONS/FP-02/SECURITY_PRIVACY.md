# FP-02 Security and Privacy Requirements

**Version:** 0.2 · **Date:** 5 November 2026  
**Subordinate to:** MobiDesk PRD v1.2, SECURITY_THREAT_MODEL.md and approved parent amendments.

## 1. Security objective

Show only the current fully authenticated student's approved profile and one valid assigned Cloud PC. The Android client is untrusted. Connect availability is a usability affordance only; the broker and FP-06 remain authoritative.

## 2. Data allowlist

| Data | Student UI | Rule |
|---|---|---|
| Student display name | Yes | Current student RLS result only |
| College label | Optional | Approved profile field only |
| Safe Cloud PC display label | Yes | FP-03-approved field only |
| Lifecycle/transport | Yes | Approved enums and safe wording |
| sampledAt/probe duration | Optional | No private infrastructure detail |
| assignmentTag | No | Equality only in memory; never display or log |
| requestId | No | Opaque diagnostic only if parent telemetry permits |
| User ID/roll/other student | No | Never render or log |
| VM ID/UUID/name/hostname/IP/port/Guac | No | Never request as client target or expose |
| JWT, grant, guest credentials | No | Never place in Compose, navigation, disk or logs |
| Raw response/exception | No | Map to approved safe copy |

## 3. Identity and assignment

- Protected reads start only after FP-01 FULLY_AUTHENTICATED for the current session generation.
- Use the current Supabase user session and RLS; never accept a caller-chosen subject ID.
- FP-03 owns assignment authorization and exactly-one resolution.
- Clear all prior user state and cancel calls on sign-out, AUTH-01, session generation change or invalidation.
- Only FP-03 Assigned produces a card; zero/multiple/inactive/unmapped results fail closed.
- assignmentTag is opaque, equality-only and generated from the shared trusted assignment resolver. It is not a secret or authorization credential. Rotate on assignment revision; never log or persist it.

## 4. API and routing boundary

- StatusRepository is the FP-02 Android client adapter; FP-04 owns GET /v1/vm-status and probe.
- Current Supabase access JWT in Authorization header; HTTPS only.
- GET has no body, query, user selector or VM/endpoint selector.
- Status response allowlist: lifecycle, rdpTransport, sampledAt, probeDurationMs, staleAfterSeconds, requestId and assignmentTag.
- Unknown JSON properties must not be mapped into student UI models.
- The endpoint resolves the target server-side and returns no VM identity or route.
- No service-role credential is included in the APK.
- No student VM power, assignment or host-management action is present.

## 5. Freshness and Connect safety

Use PRD.md §9 as the single normative Connect rule. A stale/missing/indeterminate sample does not hand off; it leaves Connect available only as a way to request a new sample when auth/assignment are valid. The new response must be fresh by monotonic receipt-age and have a matching assignmentTag plus RUNNING and RDP_NEGOTIATION_OK before FP-05 handoff. Known-negative states disable Connect. In-flight calls and broker cooldown also disable it.

A failed refresh, timeout, 429 or assignment mismatch invalidates previous actionable readiness. The old sample may be shown explicitly as stale but cannot authorize. FP-06/broker performs independent grant-time assignment/readiness checks.

## 6. Timeouts, cancellation and rate limits

- 8-second cancellable client deadline; cancel on view/session lifecycle changes.
- Never convert cancellation into an error toast or retry loop.
- Respect non-negative Retry-After delta seconds and the matching safe retryAfterSeconds body value; use the larger valid delay.
- No automatic retry; user action only after cooldown.
- A local 2-second minimum refresh interval prevents rapid duplicate requests.
- Fallback cooldown is bounded at 2/4/8/16/30 seconds only when the server gives no usable retry value.

## 7. Monotonic time

Use SystemClock.elapsedRealtime() for age and cooldown. Never compare sampledAt with the handset wall clock to authorize. FP-04 must return a newly sampled observation for every successful request, not a pre-request cached response. If timing is invalid, state is indeterminate and handoff is denied.

## 8. Logging and telemetry

Permitted only where parent allowlist approves: feature stage, safe PRD error code, outcome class, elapsed duration and redacted opaque request correlation. Forbidden: student identifier/name/email, assignmentTag, JWT, Authorization header, grant, password, VM identity/routing, raw response, stack trace containing user data, screen pixels or RDP payload.

## 9. Threat controls

| Threat | Control |
|---|---|
| Stale Ready view | Fresh request on every Connect; monotonic age; broker recheck |
| Status for a different assignment than card | Shared assignmentTag comparison; paired reload once; ASSIGN-02 on repeat mismatch |
| Multiple VM rows | FP-03 typed singleton; no first-row selection |
| Port open mistaken for readiness | TCP_REACHABLE copy is distinct and never handoffs |
| Phone wall clock wrong | monotonic receipt-age only |
| Hung/abused refresh | 8s timeout, cancellation, debounce and 429 cooldown |
| Cross-account late result | session-generation check and cache clear |
| Backend details leaked | allowlisted DTO and exact safe copy |
| UI tampering | broker/FP-06 revalidates independently |

## 10. Acceptance

Cross-student negatives, tag mismatch, timeout/429, stale/wall-clock-jump, no-selector, sensitive-field scans and account-switch tests must pass. No private field or credential may appear in UI/log/state. Broker authorization remains independent of UI.
