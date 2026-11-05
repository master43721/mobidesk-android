# FP-02 Progress and Evidence

**Version:** 0.2 · **Snapshot:** 5 November 2026  
**Repository:** master43721/mobidesk-android  
**Specification branch:** docs/fp-02-v0.2-corrected-codespaces  
**Application implementation:** Not started by this change.

## 1. Current status

| Workstream | Status | Evidence |
|---|---|---|
| FP-02 critique review | DONE | Seven review groups reconciled in v0.2 |
| Connect contradiction | CORRECTED IN SPEC | PRD.md §9; parent amendment proposal filed |
| Freshness clock | DECIDED IN SPEC | Monotonic receipt-age + round-trip upper bound |
| Assignment/card status consistency | CONTRACT PROPOSED | assignmentTag requires FP-03/FP-04 agreement |
| Missing state copy | CORRECTED IN SPEC | PRD.md §6 includes every enum and freshness state |
| Status client ownership | CORRECTED IN SPEC | FP-02 owns Android StatusRepository; FP-04 owns endpoint |
| Timeout/cancellation/429 | SPECIFIED | 8s deadline, cancellation, Retry-After, cooldown |
| Parallel initial requests | SPECIFIED | Workspace/status concurrent after AuthLevel |
| Traceability | CORRECTED IN SPEC | CORE-25/27/29 mapped; FR-VM-01 split correctly |
| Estimate | CORRECTED | 17–27 h arithmetic sum |
| Schedule | DATED | 5–11 November 2026 plan, reforecast on dependency slip |
| Parent PRD/API approval | OPEN | Proposal and API draft require G1/Owner approval |
| FP-01 integration | BLOCKED / VERIFY COMMIT | Owner reports complete, but inspected main source lacks FULLY_AUTHENTICATED handoff |
| FP-03 result/tag | BLOCKED | Typed singleton and safe label not verified |
| FP-04 deployment/tag/429 | BLOCKED | Status endpoint and approved HTTPS route not evidenced |
| FP-05 callback | BLOCKED | No-argument navigation contract not verified |
| Application code/tests | NOT STARTED / NOT RUN | This branch changes documentation and Codespaces configuration only |

## 2. Repository evidence

- Canonical codebase: master43721/mobidesk-android, native Kotlin + Jetpack Compose with pinned FreeRDP submodule.
- Current main MobiDeskViewModel.kt blob inspected: 32bbfc45c96baf9021bd34f2d860d44bbd95b6ed.
- That source observes Supabase SessionStatus.Authenticated, sets a boolean isAuthenticated and loads workspace. No FULLY_AUTHENTICATED AuthLevel contract is present in that inspected file.
- This is a repository integration discrepancy, not a claim that the Owner did not complete FP-01 locally or on a different commit.
- Current status API contract is a G1 proposal and does not prove that the endpoint is deployed.

## 3. Workspace / Codespaces evidence

- Existing Codespace: didactic-giggle-xr5r9j9g76q629974; Central India; repository master43721/mobidesk-android.
- Upgraded from 2 cores/8 GB to 4 cores/16 GB (standardLinux32gb). This machine change takes effect on next start.
- GitHub reports the existing Codespace has uncommitted and unpushed changes on main. No reset, clean or branch switch was performed.
- The v0.2 branch adds a reproducible Android devcontainer configuration. Build and tests must be run inside the Codespace after it checks out this branch or the merged setup.

## 4. No-claim statement

No Android/Kotlin source, Supabase schema/data/RLS, broker, host VM, Cloudflare tunnel, FreeRDP gitlink or release-signing file was changed. No build, unit test, device test or physical RDP test is claimed as run.

## 5. Next evidence to record

For each implementation step record the exact branch/base SHA, source files, Gradle task, test report path, result, emulator/device and reviewer. Use PASS, PARTIAL, FAIL, BLOCKED or NOT RUN; do not convert a missing result to PASS.
