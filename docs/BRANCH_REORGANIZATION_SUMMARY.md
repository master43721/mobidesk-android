# Branch Management & Historical Migration Record

> **Normative Authority Notice:** This document records the branch consolidation and migration history for `master43721/mobidesk-android`. Normative product authority is [`docs/MobiDesk_PRD_v1.2.pdf`](MobiDesk_PRD_v1.2.pdf). The authoritative engineering process and branch lifecycle rules are governed exclusively by [`AGENTS.md`](../AGENTS.md) and [`docs/MANDATORY_LIVE_DRILL_POLICY.md`](MANDATORY_LIVE_DRILL_POLICY.md).

---

## 1. Authoritative Branch Architecture (Trunk-Based)

Per [`AGENTS.md`](../AGENTS.md), the repository operates on a strict **trunk-based development workflow** with mandatory live-drill gating:

```text
[main] ─────────────────────────────────────────────────────────────► (Production Trunk)
   │                                                        ▲
   ├─► [fp-01-auth-hardening] ──(Physical Live Drill)───────┤ (accepted tag: fp-01-accepted)
   │
   └─► [fp-02-student-workspace] ──(Physical Live Drill)────┘ (accepted tag: fp-02-accepted)
```

### Core Invariants:
1. **Trunk (`main`)**: The production-ready, peer-reviewed trunk. Never force-push to `main`.
2. **Feature Branches (`fp-NN-<slug>`)**:
   - Branched directly from `main` when feature development starts.
   - Merged back into `main` **only** upon fulfilling all 6 criteria of the Mandatory Terminal Acceptance Invariant:
     ```text
     FP Acceptance = (Implementation == PASS)
                   ∧ (Automated Tests == PASS)
                   ∧ (Build Verification == PASS)
                   ∧ (Independent Code Review == PASS)
                   ∧ (Physical Live Drill == PASS)
                   ∧ (Project Owner Decision == ACCEPTED)
     ```
3. **Milestone Tags**:
   - `baseline-fp-00`: Historical bootstrap baseline.
   - `fp-NN-accepted`: Created immediately after merge upon physical live-drill completion and owner sign-off.

### Historical Note (Legacy FP-01 Merge State):
Pull Request #8 (`20d089c`) merged early FP-01 implementation into `main` prior to the formal establishment of the Mandatory Live Drill Gate. HEAD commit `c414c73` on `main` (tracked identically by `fp-01-auth-hardening`) represents the staging candidate. FP-01 remains `UNACCEPTED` until the physical phone live drill is executed and signed off. All subsequent feature points (e.g. `fp-02-student-workspace`) remain strictly on their feature branches until their live drill passes.

---

## 2. Remote Branch Cleanup Instructions (for Repository Admin)

The repository has transitioned from historical migration branches (`docs/*`, `step/*`, `step-by-step-*`) to trunk-based feature branches (`fp-01-auth-hardening`, `fp-02-student-workspace`).

To prune stale remote tracking branches from GitHub remote `origin`, repository administrators can use the provided cleanup scripts:
- **Bash / Linux / Codespaces**: [`scripts/prune-remote-branches.sh`](../scripts/prune-remote-branches.sh)
- **PowerShell / Windows**: [`scripts/prune-remote-branches.ps1`](../scripts/prune-remote-branches.ps1)

### Stale Branches to Delete on `origin`:
```bash
# Documentation snapshot branches (superseded by /docs/ in main):
git push origin --delete docs/college-admin-student-account-model
git push origin --delete docs/fp-01-current-main-handoff
git push origin --delete docs/fp-02-student-dashboard-workspace
git push origin --delete docs/fp-02-v0.2-corrected-codespaces
git push origin --delete docs/fp-02-v0.2-final
git push origin --delete docs/mandatory-live-drill-gate
git push origin --delete docs/mobidesk-prd-v1.2
git push origin --delete docs/prd-v1.2-suite

# Stale feature / draft PR branches (superseded by main and fp-NN-*):
git push origin --delete feature/fp-01-authentication-draft
git push origin --delete feature/fp-01-auth-hardening
git push origin --delete step-by-step-reorganization-and-hardening-3977521026406886263
git push origin --delete step/fp-02-4999127741281951902
```

### Active Branches to Retain:
- `main` (Trunk)
- `fp-01-auth-hardening` (FP-01 Live Drill staging candidate)
- `fp-02-student-workspace` (FP-02 implementation & testing)
