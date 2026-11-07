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

## Additional design hardening — 5 November 2026

- Added a lifecycle-aware local monotonic age timer. It updates visible age text and transitions a fresh known-negative state to stale/Connect-available at the 15-second boundary without issuing a status request. CORE-42 covers the zero-network behavior. This preserves the no-network-polling policy while preventing a permanently disabled Connect affordance.

---

## Development Loop Iteration 1 (2026-11-07)

### Loop A — Review and Self-Critique
- **Readiness:** BLOCKED
- **Iterations used:** 1
- **Findings & Severity:**
  - `C5 / Stream Bounded Reading`: MAJOR — `DefaultStatusRepository.readBounded` read characters via `Reader` and silently truncated on reaching limit. Resolved in commit `edc3c4a` by reading raw bytes from `InputStream`, bounding to 65,536 bytes, and failing closed with `ProtocolException` on overflow.
  - `C5 / Parent Error Catalogue Mismatch (F6 defect class)`: MAJOR — Timeout and generic catch blocks returned `"We couldn't reach the Cloud PC service. Check your connection and try again."` which was an invented copy string. Resolved in commit `edc3c4a` by aligning strictly to PRD v1.2 §12 / `strings.xml` parent error catalogue `ASSIGN-02`: `"We could not verify your assigned Cloud PC. Try again."`.
  - `AMD-01 / Connect Contradiction & Freshness`: NEGOTIABLE — Proposed PRD amendment AMD-01 resolves Connect gate contradiction and establishes monotonic age + shared `assignmentTag`. Logged in `docs/dev-loop/NEGOTIABLE_REGISTER.md` (NEG-02).
  - `Fake Broker Contract`: UNKNOWN — Synthetic contract responses pass schema validation via `scripts/validate-fake-broker-contract.ps1`. Live broker remains UNKNOWN until external broker repo is integrated.
- **Remaining NEGOTIABLE items:** NEG-02 (AMD-01 approval), NEG-06 (Telugu localization review).

### Loop B — Automated Test & Output Mismatch Log
- **Local Toolchain Check:** FAILED (`scripts/verify.ps1` output: `BLOCKED_MISSING_TOOLCHAIN`). JDK and Android SDK are not installed on this host.
- **Expected vs Actual Mismatches:**
  - *Mismatch 1:* Expected local `./gradlew testDebugUnitTest` execution; Actual: `BLOCKED_MISSING_TOOLCHAIN`. Classification: `Environment`.
  - *Mismatch 2:* Contract test for oversized payload added (`StatusRepositoryContractTest.getVmStatus_oversizedPayloadFailsClosed`), but JVM execution deferred to CI run. Classification: `Environment`.
- **Static Verification Scripts:**
  - `scripts/scan-secrets-logs.ps1`: PASSED (0 leaks)
  - `scripts/check-release-config.ps1`: PASSED (0 release risks)
  - `scripts/check-traceability.ps1`: PASSED (0 unmapped requirements, 0 missing tests)
  - `scripts/check-docs.ps1`: PASSED (0 case collisions, 0 broken links, 0 duplicate plan files)
  - `scripts/validate-fake-broker-contract.ps1`: PASSED (0 schema violations, 0 leaks)
