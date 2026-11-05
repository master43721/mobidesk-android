# FP-02 Progress and Evidence

**Version:** 0.1
**Snapshot:** 4 November 2026
**Canonical repository:** master43721/mobidesk-android
**Branch:** docs/fp-02-student-dashboard-workspace
**Implementation changes:** None. This branch contains FP-02 documentation only.

## 1. Current status

| Workstream | Status | Evidence |
|---|---|---|
| Feature scope | DRAFTED | FEATURE_INVENTORY.md FP-02 and parent DESIGN.md S-02/S-03/S-07/S-09 |
| Parent requirement mapping | DRAFTED | FR-AUTH-02, FR-VM-01, FR-VM-04/05 plus R2 Dock boundaries mapped in TRACEABILITY.md |
| Current dashboard source audit | DONE — read-only | Current main MobiDeskViewModel.kt, MobiDeskRepositories.kt and MobiDeskApp.kt inspected |
| FP-01 integration | BLOCKED / VERIFY OWNER CLAIM | Current main source snapshot still loads workspace on Supabase Authenticated; current implementation does not expose the completed AuthLevel gate described in FP-01 docs |
| FP-03 assignment contract | BLOCKED — DEPENDENCY | Current StudentWorkspaceResult.Ready contains a list; no exactly-one typed assignment result in inspected main source |
| FP-04 status integration | BLOCKED — DEPENDENCY | API contract specifies GET /v1/vm-status, but no StatusRepository/client in inspected Android source |
| Broker deployment/base URL | UNKNOWN | OPEN_ITEMS.md previously recorded stable broker HTTPS/WSS route as open; new server reachability report is not evidence of endpoint deployment |
| Student-safe VM label | UNKNOWN | Current source displays VmSummary.name; current values/semantics are not proven safe |
| Automated tests | NOT RUN | No application code changed in this documentation revision |
| Physical device/accessibility | NOT RUN | No physical device evidence in this documentation revision |
| FP-02 acceptance | NOT STARTED | Feature package only |

## 2. Repository provenance and FP-01 status note

The latest observed main commit during preparation is the merge of documentation PR #5. GitHub reports:
- PR #5 (FP-01 handoff documentation): merged.
- PR #4 (account-model documentation): merged.
- PR #3 (FP-01 review draft): still open and its PR description labels the package documentation-only.
- The checked-in `MobiDeskViewModel.kt` inspected on main still reacts to `SessionStatus.Authenticated` by setting authenticated UI state and calling `loadWorkspace(userId)`; the inspected `cancelPasswordRecovery()` clears a UI boolean only.
- Therefore the GitHub main source snapshot does not substantiate that FP-01 application implementation is completed, despite the Project Owner's report. FP-02 implementation must consume the actual completed FP-01 branch/commit once supplied or merged; it must not bypass AuthLevel with raw SDK status.

This note is not a claim that the Owner has not completed FP-01 locally or in another branch. It identifies a repository integration discrepancy to close before FP-02 code integration.

## 3. Current source baseline observations

- DashboardScreen currently shows student name and college label.
- The Cloud PC card renders every item in `workspace.assignedVms`.
- The button is permanently disabled with "Remote session not configured".
- A static notice says remote desktop is not ready.
- The action says "Refresh assignment".
- The repository selects `students(id,name,college)` and RLS-filtered `vms(id,name)`.
- Current main has no observed broker status repository/client in the student workspace path.
- API_CONTRACT.md already specifies the target status route and payload semantics; this is not evidence of a deployed endpoint.

## 4. No-change statement

No Android source, Gradle, Manifest, dependency, Supabase live setting/schema/data, broker, server, VM, Cloudflare tunnel, firewall, RDP or FreeRDP file was changed. No build or test result is claimed.

## 5. Next steps and Mandatory Live Drill Gate

Per `docs/MANDATORY_LIVE_DRILL_POLICY.md`, FP-02 is subject to the mandatory 9-stage gate sequence:
- **Current `LIVE_DRILL_STATUS`:** `NOT REQUESTED` (Blocks FP completion; implementation has not started).
- **Owner Acceptance Status:** `UNACCEPTED`.
- **Live Drill Requirement:** Execution on a physical Android device against the approved staging broker API with real student assignment and live VM lifecycle/status data. Connect button gating must be physically demonstrated.

### Phased Execution Sequence
1. Await FP-01 live-drill completion (`LIVE_DRILL_STATUS: PASS`) and written Project Owner acceptance.
2. Agree on FP-03 typed exactly-one assignment result and student-safe label.
3. Verify FP-04 status endpoint/client contract and stable server route.
4. Obtain Owner answers to OPEN_QUESTIONS.md.
5. Implement FP-02 on a fresh branch from latest main in small phases with 100% automated test coverage.
6. Conduct independent implementation review.
7. Prepare `docs/live-drills/FP-02/` and set `LIVE_DRILL_STATUS = READY FOR OWNER`.
8. Send explicit live-drill handoff request to Project Owner and **STOP**.
9. Owner executes live demonstration on physical Android phone; audit evidence and record written Owner acceptance.
