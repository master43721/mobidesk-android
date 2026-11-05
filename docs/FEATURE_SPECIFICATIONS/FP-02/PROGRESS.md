# FP-02 Progress and Evidence

**Version:** 0.2 · **Snapshot:** 5 November 2026  
**Repository:** master43721/mobidesk-android  
**Specification branch:** docs/fp-02-v0.2-final  
**Application implementation:** Not started by this documentation change.

## Current status

| Workstream | State | Evidence |
|---|---|---|
| Critique review | DONE | Seven groups reconciled |
| Connect contradiction | Corrected in FP-02 spec | PRD.md §6; parent amendment filed |
| Freshness clock | Decided | Monotonic receipt-age + request RTT upper-bound |
| Assignment/card consistency | Contract proposed | Opaque assignmentTag needs FP-03/04 |
| Enum/freshness copy | Written | PRD.md §4 |
| Status client ownership | Corrected | FP-02 Android StatusRepository; FP-04 endpoint |
| Timeout/cancellation/429 | Specified | 8s, cancellation, Retry-After, 2s debounce |
| Parallel reads | Specified | After AuthLevel, paired by tag |
| Traceability | Corrected | CORE-25/27/29/41 mapped; FR-VM-01 split; grant-level ASSIGN-03 clears/reloads |
| Estimate/schedule | Corrected | 17–27h; dated 5–11 Nov |
| Parent PRD/API approval | OPEN | Amendment and API draft need G1/Owner approval |
| FP-01 integration | BLOCKED — verify commit | Owner reports complete; inspected main lacks FULLY_AUTHENTICATED |
| FP-03 result/tag | BLOCKED | Typed singleton and safe label not verified |
| FP-04 API/tag/429 | BLOCKED | Endpoint/deployment not evidenced |
| FP-05 callback | BLOCKED | No-argument callback not verified |
| App code and tests | NOT STARTED / NOT RUN | This change is docs and Codespaces configuration only |

## Repository evidence

- Canonical repo: master43721/mobidesk-android, native Kotlin/Compose with pinned FreeRDP submodule.
- Inspected main MobiDeskViewModel.kt blob: 32bbfc45c96baf9021bd34f2d860d44bbd95b6ed.
- That source observes Supabase SessionStatus.Authenticated, sets isAuthenticated and loads workspace; the inspected file does not expose FULLY_AUTHENTICATED AuthLevel.
- This is a repository integration discrepancy, not a claim that local/other-branch FP-01 work does not exist.
- API_CONTRACT is a G1 proposal; it does not prove a deployed endpoint.

## Codespaces evidence

- Existing Codespace: didactic-giggle-xr5r9j9g76q629974, Central India, master43721/mobidesk-android.
- Upgraded from 2 cores/8 GB to 4 cores/16 GB (standardLinux32gb); machine change applies on next start.
- GitHub reports uncommitted and unpushed work on main. No reset, clean or branch switch was performed.
- v0.2 branch adds a reproducible Android devcontainer; build/tests remain NOT RUN until opened and verified.

## No-claim statement

No Android source, Supabase schema/data/RLS, broker, server VM, tunnel, FreeRDP gitlink or signing key was changed. No build, unit test, device or physical RDP test is claimed.

Record each future result with exact branch/base SHA, source files, Gradle task, report path, result, device and reviewer. Missing evidence is BLOCKED or NOT RUN, never PASS.
