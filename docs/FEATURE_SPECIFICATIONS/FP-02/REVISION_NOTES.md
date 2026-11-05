# FP-02 Revision Notes

**Version:** 0.2 · **Date:** 5 November 2026

## Changes from v0.1

1. Resolved the Connect contradiction. Stale/missing/unknown status remains actionable for a fresh-check action; a fresh known-negative state disables Connect; once any state becomes stale it is indeterminate again. Every tap refreshes before handoff.
2. Defined freshness with Android `elapsedRealtime()` request/receipt timing and a conservative request round-trip upper bound. Phone wall time is not used for the gate.
3. Added opaque `assignmentTag` equality between FP-03 workspace and FP-04 status; mismatch discards/reloads the pair once.
4. Added lifecycle/transport copy for every enum and all freshness labels. Source locale is en-IN; reviewed Telugu remains a follow-up.
5. Moved Android `StatusRepository` and its fake-server contract tests to FP-02; FP-04 retains endpoint/probe ownership.
6. Added an 8-second timeout, coroutine cancellation, HTTP 429/Retry-After handling and 2-second debounce.
7. Parallelized profile/assignment and broker status calls after FP-01 AuthLevel.
8. Corrected FR-VM-01 split: FP-02 owns exactly-one card presentation; FP-03 owns authoritative resolution and assignment authorization.
9. Corrected traceability for CORE-25, CORE-27 and CORE-29; added CORE-31..41, including grant-level ASSIGN-03 invalidation.
10. Corrected implementation estimate to 17–27 focused hours and added external dependency estimates and a dated execution sequence.
11. Filed parent PRD amendment AMD-01 and API_CONTRACT v1.1 proposal. Parent PRD v1.2 remains the normative source until the Owner approves a successor/amendment.
12. Moved agent implementation/review prompts out of the product spec folder into `tools/prompts/fp-02/`.
13. Added project-level continuity entries in DECISIONS, PROGRESS and TASKS; added GLOSSARY and TOOL_CALLS.
14. Added pinned Android Codespaces devcontainer with Java 21, Android SDK 37.0, build-tools 37.0.0, NDK/CMake, NASM, recursive submodule setup and persistent caches.
15. Corrected the SDK package name after PR CI proved `platforms;android-37` was unavailable. CI and the devcontainer now request `platforms;android-37.0` using channel 3 and verify `android.jar`/apksigner before building.

## Scope and no-change statement

This review branch changes FP-02 documentation, parent/API proposals, CI SDK provisioning and Codespaces configuration only. It does not implement FP-02 application behavior, modify Supabase, deploy the broker, change VM state or alter the FreeRDP gitlink. CI run 42024067666 failed before Gradle due to the old SDK package name. The corrected run was pending at the last recorded check; no build/test/device/RDP pass is claimed.
