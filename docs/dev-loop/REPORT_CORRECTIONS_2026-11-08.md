# MobiDesk Report Corrections & Truth-in-Reporting Record

**Document:** `docs/dev-loop/REPORT_CORRECTIONS_2026-11-08.md`  
**Date:** 2026-11-08  
**Author:** Antigravity Engineering (Lead Orchestrator)  
**Governance:** `AGENTS.md` & `docs/MANDATORY_LIVE_DRILL_POLICY.md`

---

## 1. Corrections to Prior Report

### (a) Real CI Results Across Branches and Runs
The prior report did not clearly distinguish local execution success from remote GitHub Actions run failures:
- **`fp-02-student-workspace` (`3393655`):**
  - Push Run `42475965882`: **FAILED** at Step 8 (`Run JVM unit tests and assemble debug APK`) due to CMake live dependency download failure (`libpng-1.6.58.tar.gz` returned HTTP 504 Gateway Timeout). URL: https://github.com/master43721/mobidesk-android/actions/runs/42475965882
  - PR #10 Run `42475969710`: **FAILED** at Step 7 (`Verify Live Drill Acceptance Gate Policy`) because PR #10 targets `main` while `DEVICE_OR_ENVIRONMENT_CHECKLIST.md` contains unexecuted (`NOT RUN`) steps. URL: https://github.com/master43721/mobidesk-android/actions/runs/42475969710
- **`fp-03-assignment-resolution` (`045965e`):**
  - Push Run `42475976994`: **FAILED** at Step 8 (`libpng` 504). URL: https://github.com/master43721/mobidesk-android/actions/runs/42475976994
  - PR #11 Run `42475980493` (targeting `fp-02`): **FAILED** at Step 8 (`libpng` 504). URL: https://github.com/master43721/mobidesk-android/actions/runs/42475980493
- **`fp-04-status-readiness` (`d7bbbe9`):**
  - Push Run `42475987683`: **FAILED** at Step 8 (`libpng` 504). URL: https://github.com/master43721/mobidesk-android/actions/runs/42475987683
  - PR #12 Run `42475992952` (targeting `fp-03`): **FAILED** at Step 8 (`libpng` 504). URL: https://github.com/master43721/mobidesk-android/actions/runs/42475992952
- **Older Run Failures:**
  - Run `42473265915` (push `fp-02` `b39271a`): FAILED at Step 8 (`libpng` 504).
  - Run `42473269037` (PR #10 `fp-02` `b39271a`): FAILED at Step 7 (gate policy).
  - Run `42473272599` (push `fp-03` `900c2a7`): FAILED at Step 8 (`libpng` 504).
  - Run `42473306127` (PR #11 `fp-03` `900c2a7`): FAILED at Step 8 (`libpng` 504).
  - Run `42473272420` (push `fp-04` `c7f5316`): FAILED at Step 8 (`libpng` 504).
  - Run `42473307099` (PR #12 `fp-04` `c7f5316`): FAILED at Step 8 (`libpng` 504).

### (b) Session Broker Remote Status
Branch `broker/catch-up-v1.1` @ commit `40eb181539d83f2ecd77b5711f0ee2a298fbbb41` exists **only locally** in `/workspaces/session-broker`. Pushing to `master43721/session-broker` failed with HTTP 403 Forbidden (exit code 128) because Codespaces default `$GITHUB_TOKEN` is scoped exclusively to `master43721/mobidesk-android`. Per Reporting Rule R3, this work cannot be labelled `READY FOR OWNER` until pushed and accessible.

### (c) FreeRDP Transport Spike Status
Branch `spike/freerdp-transport` @ commit `40d1b37` contains **only an architectural design document** (`docs/spikes/FREERDP_TRANSPORT_SPIKE.md`). It contains no executable C/Kotlin JNI bridge code, no active WebSocket client, and no verified frame capture. Per R3, it cannot be labelled `READY FOR OWNER`; its true status is **IN PROGRESS**.

### (d) FP-03 and FP-04 Test Count Provenance
The reported 55/55 passed JVM unit tests on `fp-03-assignment-resolution` and `fp-04-status-readiness` represent the **FP-02 test suite** inherited by branching. Branches `fp-03` and `fp-04` currently contain only `PROGRESS.md` gap analysis documents. Zero net code or new tests were implemented for FP-03 or FP-04 in those commits.

### (e) FP-01 Live Drill Kit Contents
The initial `docs/live-drills/FP-01/DEVICE_OR_ENVIRONMENT_CHECKLIST.md` contained 10 scenarios and omitted the deep-link magic-link resolution, process death during recovery, and token replay scenarios required by `docs/FEATURE_SPECIFICATIONS/FP-01/PRD.md`.

### (f) Register and Decision Document Alignment
- **NEG-02** is normative approval of **AMD-01** (Connect Gate & Monotonic Freshness Timer).
- **DEC-REVOKE** is the architectural decision between a dedicated Postgres role vs. `SECURITY DEFINER` stored procedure for the 15-second relay watchdog loop.
- NEG items and DEC items are distinct and now aligned exactly with `NEGOTIABLE_REGISTER.md` and `OWNER_DECISIONS_ROUND2.md`.

### (g) Recorded APK Provenance
The APK SHA-256 (`2aaff3ad28981c030221ce7171ebd105572f898b171599ed029a01892bcbcdc0`) recorded in the previous report was produced by a **local build** of `fp-04-status-readiness` in the Codespaces container. It was **not** fetched from a GitHub Actions CI run artifact of `fp-01-auth-hardening` or `fp-02-student-workspace`.
