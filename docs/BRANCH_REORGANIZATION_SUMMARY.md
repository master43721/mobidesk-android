# Branch Reorganization & Lifecycle Management Guide

**Date:** 2026-11-06
**Repository:** `master43721/mobidesk-android`

## 1. Branch Sequence & Responsibilities

The codebase is organized in progressive implementation steps:

```
[step/fp-00-baseline] (Original skeleton)
         │
         ▼
[step/fp-01-authentication]
  - Hardened student authentication, session encryption, and password recovery
  - 40 passing unit tests (AuthErrorMessages, MobiDeskViewModel, RecoveryStateStore, MobiDeskApplication)
  - Live drill checklists & defect tracking (docs/live-drills/FP-01)
         │
         ▼
[step/fp-02-student-workspace]
  - Builds on FP-01
  - Student dashboard specifications, OpenAPI 3.1 contracts, assignment tagging
  - Live drill checklists (docs/live-drills/FP-02)
         │
         ▼
[main]
  - Consolidated stable trunk combining verified implementations, specifications, and test suites
```

## 2. Remote Branch Cleanup Instructions (for Repository Admin)

To prune dead and merged branches from GitHub remote `origin`:
Run git branch deletion on remote for:
- `feature/fp-01-authentication-draft`
- `docs/fp-02-v0.2-corrected-codespaces`
- `docs/college-admin-student-account-model`
- `docs/fp-01-current-main-handoff`
- `docs/fp-02-student-dashboard-workspace`
- `docs/prd-v1.2-suite`
- `docs/mobidesk-prd-v1.2`

To push the new step branches to GitHub remote:
Run git branch publish for:
- `step/fp-01-authentication`
- `step/fp-02-student-workspace`
