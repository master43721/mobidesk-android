# FP-02 Decision Register

**Version:** 0.2 · **Date:** 5 November 2026

## 1. Decisions locked for v0.2

| ID | Decision | Rationale / source | State |
|---|---|---|---|
| D-01 | One student home screen, identity and Cloud PC | FEATURE_INVENTORY FP-02; DESIGN S-02 | Confirmed |
| D-02 | Show approved name and optional college label | Parent S-02 | Confirmed |
| D-03 | FP-02 renders at most one card; FP-03 resolves exactly one assignment | Separates app presentation from data authorization | Confirmed split |
| D-04 | Current-user RLS and server-side assignment enforcement | FR-AUTH-02 / FR-VM-01 | Required |
| D-05 | No VM ID, UUID, hostname, IP, port or Guac routing | Parent privacy boundary | Required |
| D-06 | Lifecycle and RDP transport remain separate | PRD §5.2 / API_CONTRACT | Required |
| D-07 | Sample lifetime is 15 seconds | Parent API / PRD | Required |
| D-08 | PRD.md §9 is the one source of truth for Connect availability and handoff | Resolves stale-button contradiction and prevents duplication | Locked for FP-02; parent amendment pending |
| D-09 | Broker revalidates assignment and readiness at grant time | UI is not authorization | Required |
| D-10 | Student VM controls absent; Dock is R2 placeholder | Parent release scope | Required |
| D-11 | Settings/sign-out delegate to FP-01 | Avoid duplicate auth code | Required |
| D-12 | Status GET has no body/query/target selector | Parent API security contract | Required |
| D-13 | Preserve existing Compose/dashboard structure | Existing code/design | Default |
| D-14 | No periodic polling. Load on entry; manual Refresh; fresh status on every Connect tap. Stale/unknown is still user-actionable for a fresh check. | Avoid battery/network polling and the 15-second race | Locked for FP-02; parent amendment pending |
| D-15 | Failed refresh invalidates actionable prior readiness | Prevent stale positive state from authorizing | Required |
| D-16 | Omit absent/blank college label | “Where available” | Locked |
| D-17 | Use only an FP-03-approved safe Cloud PC label | vms.name safety not proven | Required dependency |
| D-18 | Connect callback is no-argument; FP-05 owns credentials; FP-06 owns grant/relay | Avoid target/credential leakage | Required |
| D-19 | Use minimal fakeable WorkspaceRepository and StatusRepository seams | Unit/contract tests without live broker | Locked |
| D-20 | Monotonic receipt-age with conservative request RTT bound; sampledAt display only | Phone wall-clock skew cannot gate status | Locked |
| D-21 | FP-03/FP-04 return matching opaque assignmentTag from the same trusted resolver; FP-02 compares before composing status and label | Prevent reassignment race between separate calls | Proposed parent/API amendment |
| D-22 | Fetch workspace and broker status in parallel after AuthLevel | Broker independently derives assignment; assignmentTag joins results | Locked once D-21 contract is approved |
| D-23 | FP-02 owns Android StatusRepository; FP-04 owns server endpoint/probe | Correct client/server ownership | Locked |
| D-24 | 8s client deadline, cancellable calls, 2s local request debounce, honor 429 Retry-After; no automatic retry | Mobile RTT plus 1.5s server probe budget | Locked |
| D-25 | R1 source locale en-IN; Telugu te-IN only after reviewed translation | Prevent unreviewed mixed-language copy | Owner decision recorded |
| D-26 | English display copy for all lifecycle/transport enums and freshness states is written in PRD.md §6 | No blank state or UX-review placeholder | Locked |

## 2. Change control

The Connect/freshness rule and assignmentTag are proposed changes to the parent PRD/API. They are documented in docs/PRD_CHANGE_PROPOSALS/PRD-1.2-AMD-01-FP02-STATUS-CONSISTENCY.md. They must be accepted in the parent successor/amendment before G1 is frozen. Do not represent a proposed parent change as already approved.

## 3. Dependency confirmations still needed

- Exact FP-01 implementation commit and FULLY_AUTHENTICATED handoff on the source branch used for FP-02.
- FP-03 typed result, safe label and assignmentTag implementation.
- FP-04 API endpoint, request-fresh observation, assignmentTag, 429 and Retry-After behavior.
- FP-05 no-argument navigation callback.
- Owner approval of parent amendment and Telugu translation scope.
