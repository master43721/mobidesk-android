# Proposed Amendment AMD-01 — FP-02 Status and Assignment Consistency

**Parent:** MobiDesk PRD v1.2 (dated 3 November 2026)  
**Proposed version:** v1.2.1 clarification or successor v1.3  
**Date:** 5 November 2026  
**Owner decision:** PENDING — this document is a proposal, not an approved normative change.

## 1. Problem

PRD v1.2 §5.1 says Connect is enabled only while the last status sample is fresh, but also says Connect causes a fresh broker-side authorization/readiness check. If stale status disables the only action that obtains a new sample, the student must discover and press a separate Refresh action and then race the 15-second expiry. This is internally contradictory and creates an avoidable dead-end.

The API contract also returns the status of the broker-resolved current assignment independently from the Android assignment/card read. If an assignment changes between those requests, the current contract gives the app no way to prove that the status and label refer to the same assignment revision.

Finally, comparing RFC3339 sampledAt with handset wall time makes the 15-second gate dependent on user/device clock correctness.

## 2. Proposed normative wording

### 2.1 Student journey — replace §5.1 step 4

> **Connect action:** When the student is fully authenticated and has one valid assignment, Connect is an action to request a fresh status check and proceed if eligible. A missing, stale or indeterminate observation does not by itself make Connect unavailable. A definitive negative observation (STOPPED, STARTING, STOPPING, ERROR, TCP_UNREACHABLE or TCP_REACHABLE) disables Connect while the state remains current. Every Connect tap obtains a new broker status observation and proceeds only if the fresh result is RUNNING plus RDP_NEGOTIATION_OK and belongs to the same assignment revision as the displayed Cloud PC. The broker independently revalidates the assignment and readiness while issuing the session grant. Connect availability is a usability affordance, not authorization.

### 2.2 Freshness — clarify §5.2 / FR-VM-05

> A status observation expires 15 seconds after its server sampling time. The Android client MUST use a monotonic elapsed-time source to measure age from response receipt and MUST NOT use the handset wall clock to decide whether a sample is fresh. The client may conservatively include the full request round-trip duration in the age bound. The broker MUST return a newly sampled observation for each successful status request and MUST NOT reuse an observation sampled before the request began. sampledAt remains a UTC display/diagnostic value. Every Connect action obtains a new status observation; the grant endpoint independently rechecks assignment and readiness.

### 2.3 Assignment-to-status consistency

> The FP-03 workspace assignment result and FP-04 status response MUST contain the same opaque assignmentTag for the same authenticated subject and assignment revision. The tag is equality-only metadata generated from the shared trusted assignment resolver, contains no readable student/VM identity or routing data, and changes when the assignment revision changes. The Android client MUST NOT display a label/status pair whose tags differ. On mismatch, it MUST discard the pair and retry the paired read once; a repeated mismatch fails closed using the existing ASSIGN-02 behavior. The tag is not an authorization credential or target selector.

### 2.4 Status rate limiting

> The broker may rate-limit status requests. A rate-limited request returns HTTP 429 with a standards-compliant Retry-After delta-seconds header and safe retryAfterSeconds body metadata when available. The client honors the longer valid delay, does not automatically retry, and maps the temporary inability to verify to the existing ASSIGN-02 copy. No new user-facing error code is created.

## 3. Proposed Connect availability state

| Condition | Connect |
|---|---|
| No status sample / NOT_CHECKED | Available when auth and assignment are valid |
| UNKNOWN lifecycle or transport | Available to request a fresh check |
| Stale previously positive observation | Available to request a fresh check |
| STOPPED / STARTING / STOPPING / ERROR | Disabled |
| TCP_UNREACHABLE / TCP_REACHABLE | Disabled |
| Status request in flight | Temporarily disabled |
| Retry-After cooldown active | Temporarily disabled |
| Auth or assignment invalid | Hidden/disabled |

Regardless of button availability, no handoff is possible from cached state. After each tap, only a fresh matching RUNNING + RDP_NEGOTIATION_OK observation can invoke FP-05. Grant-time authorization remains server-owned.

## 4. Requirements and files affected

- Preserve FR-VM-05 identifier; clarify its sample-age and fresh-check semantics.
- Clarify §5.1 Connect journey and §5.2 status freshness.
- Clarify API_CONTRACT.md §4/§7/§8 with assignmentTag, request-fresh observation and 429 behavior.
- Update DESIGN.md S-02/S-03 stale-state copy and Connect affordance to match the approved rule.
- Update FEATURE_INVENTORY FP-02 to point to the single Connect rule in the FP-02 PRD.
- Keep existing §12 user error codes/copy unchanged.
- Keep the 15-second sample lifetime and 1,500 ms server probe budget unchanged.
- Keep no-periodic-polling as the default; entry, manual Refresh and Connect-triggered status are the only client triggers.

## 5. Ownership

- FP-02: app's one-card presentation, Android StatusRepository, monotonic freshness calculation, refresh/Connect orchestration and client contract tests.
- FP-03: trusted exactly-one assignment result, safe user label and shared assignmentTag resolver semantics.
- FP-04: GET /v1/vm-status, server-side assignment re-derivation, request-fresh probe, assignmentTag in response, 1,500 ms probe budget and 429/Retry-After.
- FP-05: Windows credential prompt and no-argument handoff destination.
- FP-06+: grant-time revalidation, relay and session authorization.

## 6. Security analysis

The proposed behavior does not relax authorization. It makes Connect easier to reach but requires a successful fresh status read before the client handoff, and the broker still checks the authenticated subject, active assignment and readiness at grant issuance. The opaque assignmentTag only detects a race between two separately fetched representations; it is not accepted as a target or bearer credential. No VM identifier or endpoint is added to the public response.

## 7. Approval and migration

**Status: PENDING OWNER APPROVAL.** Until approved, parent PRD v1.2 remains normative and FP-02 v0.2 is a proposed implementation design only. On approval:
1. publish the approved parent PRD successor/errata and update DESIGN.md;
2. approve API_CONTRACT v1.1 and freeze FP-03/FP-04 tag generation;
3. update the parent traceability/test suite;
4. remove the amendment-pending gate from FP-02 G1.

No code may treat this proposal as approved merely because it is committed.
