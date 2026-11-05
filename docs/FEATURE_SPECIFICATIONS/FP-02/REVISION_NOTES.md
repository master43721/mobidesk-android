# FP-02 Revision Notes

**Version:** 0.2 · **Date:** 5 November 2026

## Changes from v0.1

1. Resolved the Connect contradiction: stale/missing/unknown is still actionable as a fresh-check action; known-negative, in-flight, invalid auth/assignment and cooldown gates disable. Every tap refreshes before handoff.
2. Defined freshness with Android elapsedRealtime request/receipt timing and a conservative round-trip age bound. Phone wall time is not used for the gate.
3. Added opaque assignmentTag equality between FP-03 workspace and FP-04 status; mismatch discards/reloads the pair once.
4. Added all lifecycle/transport enum copy and freshness labels; source locale en-IN; reviewed Telugu remains follow-up.
5. Moved Android StatusRepository/client contract tests to FP-02; FP-04 retains endpoint/probe.
6. Added 8s timeout, cancellation, HTTP 429/Retry-After and 2s debounce.
7. Parallelized profile/assignment and status calls after AuthLevel.
8. Corrected FR-VM-01 split: FP-02 owns exactly-one card presentation; FP-03 owns resolution/authorization.
9. Mapped CORE-25/27/29 and added CORE-31..40.
10. Corrected estimate to 17–27h and added dependency effort and dates.\n11. Added stale-formerly-negative semantics: once expired, every status is indeterminate and Connect can request a fresh observation.\n12. Added CORE-41 for a grant-level ASSIGN-03: clear old card/status and reload the assignment/status pair once.
13. Filed formal parent PRD/API change proposal; parent PRD v1.2 remains owner-review draft.
14. Moved tool prompts out of the product spec folder into tools/prompts/fp-02/.
15. Added pinned Android devcontainer with Java 21, Android SDK 37, NDK/CMake matching checked-in CI, NASM, submodule setup and persistent build caches.

This branch changes FP-02 documentation and Codespaces configuration only. No app code, Supabase, broker or VM implementation is claimed; tests remain NOT RUN.
