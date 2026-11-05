# FP-02 Decision Register

**Version:** 0.2 · **Date:** 5 November 2026

| ID | Decision | Rationale | State |
|---|---|---|---|
| D-01 | One student home screen, identity and Cloud PC | Parent FP-02 scope | Confirmed |
| D-02 | Show approved name and optional college label | Parent S-02 | Confirmed |
| D-03 | FP-02 renders at most one card; FP-03 resolves/enforces exactly one assignment | Presentation vs data authorization | Confirmed split |
| D-04 | Current-user RLS and server-side assignment checks | Parent FR-AUTH/FR-VM | Required |
| D-05 | No VM ID/UUID/hostname/IP/port/Guac in app | Privacy boundary | Required |
| D-06 | Lifecycle and transport remain distinct | Parent PRD §5.2 | Required |
| D-07 | Sample lifetime remains 15 seconds | Parent/API contract | Required |
| D-08 | PRD.md §6 is the sole normative Connect rule; a known-negative state disables only while its sample remains fresh | Resolves contradiction; once stale, even a former STOPPED result is indeterminate and can be refreshed through Connect | Locked in FP-02; parent change pending |
| D-09 | Broker revalidates during grant issuance | UI is not authorization | Required |
| D-10 | No student power/assignment controls; Dock is R2 placeholder | Release scope | Required |
| D-11 | Settings/sign-out delegate to FP-01 | Avoid duplicated auth | Locked |
| D-12 | Status GET has no body/query/target selector | API security boundary | Required |
| D-13 | Preserve existing Compose/repository structure | Current native app | Default |
| D-14 | No periodic polling; load on entry, manual Refresh and fresh status on every Connect. Stale/unknown remains user-actionable. | Avoid 15s race and background battery use | Locked; parent change pending |
| D-15 | Failed refresh invalidates actionable old readiness | Avoid stale positive authorization | Required |
| D-16 | Omit null/blank college label | Where available | Locked |
| D-17 | Use only FP-03-approved safe Cloud PC label | Do not assume raw vms.name is safe | Required |
| D-18 | No-argument FP-05 callback; FP-05 credentials; FP-06 grant/relay | Keep sensitive data out of navigation | Required |
| D-19 | Minimal fakeable WorkspaceRepository and StatusRepository | Deterministic tests | Locked |
| D-20 | Monotonic receipt-age plus request RTT upper-bound; sampledAt display only | Phone wall clock may be wrong | Locked |
| D-21 | Shared opaque assignmentTag from canonical resolver; compare FP-03 and FP-04 snapshots | Prevent assignment race | Proposed parent/API change |
| D-22 | Parallel workspace and broker status after AuthLevel | Broker resolves assignment independently; tag joins results | Locked after D-21 |
| D-23 | FP-02 owns Android StatusRepository; FP-04 owns endpoint/probe | Correct client/server boundary | Locked |
| D-24 | 8s client deadline, cancellation, 2s debounce, 429 Retry-After; no automatic retry | RTT plus 1.5s server probe budget | Locked |
| D-25 | R1 source locale en-IN; te-IN after reviewed translation | Avoid unreviewed translation | Owner review |
| D-26 | Complete enum/freshness copy written in PRD.md §4 | No UX placeholder | Locked |

## Change control

Connect/freshness and assignmentTag are proposed parent changes recorded in docs/PRD_CHANGE_PROPOSALS/PRD-1.2-AMD-01-FP02-STATUS-CONSISTENCY.md. They require Owner approval and G1 freeze. Do not represent them as approved until accepted.

## External confirmation

- FP-01 exact AuthLevel commit and recovery-only test.
- FP-03 typed result, safe label, assignmentTag source.
- FP-04 endpoint, probe, no-cached-sample, assignmentTag and 429.
- FP-05 no-argument navigation callback.
- Parent amendment approval and Telugu translation review.
