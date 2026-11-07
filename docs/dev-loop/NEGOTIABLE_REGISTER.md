# MobiDesk Negotiable Items Register

**Document:** `docs/dev-loop/NEGOTIABLE_REGISTER.md`  
**Purpose:** Single register of MINOR and owner-decision items requiring explicit Owner resolution.  
**Rule:** Non-negotiables (MUST/SEC/FR, failing tests, leaks, unverified claims) must never be logged here.

---

## Registered Items

| ID | Topic / Item | Severity | Rationale | Owner Decision Required | Current Status |
|:---|:---|:---:|:---|:---|:---:|
| **NEG-01** | Legacy Merge State on `main` (Critique C2 / F1) | Owner Decision | PR #8 merged FP-01 to `main` before Live Drill policy was codified. `main` currently contains unaccepted FP-01 code. | Choose between:<br>**(a)** Keep `main` as an owner-approved staging trunk, recorded in `AGENTS.md` with explicit sign-off.<br>**(b)** Revert PR #8 cleanly via `git revert` commits (no history rewrite). | PENDING OWNER CHOICE |
| **NEG-02** | PRD Amendment AMD-01 Approval (Connect Gate & Consistency) | Owner Decision | PRD v1.2 §5.1 had contradiction ("disabled when stale" vs "Connect triggers fresh check"). AMD-01 formalizes: Connect available when stale as fresh-check action, monotonic receipt-age timer, and shared `assignmentTag`. | Formally sign off on AMD-01 to promote from proposal to normative PRD v1.2.1 / v1.3 errata. | PENDING OWNER SIGN-OFF |
| **NEG-03** | Staging Credentials for FP-01 Physical Live Drill | External Dependency | Live drill requires physical authentication and password recovery on physical device. Client must never store production student data or secret keys. | Provide Supabase publishable key in `local.properties` and provisioned staging QA student test account. | PENDING OWNER PROVISIONING |
| **NEG-04** | Remote Branch Pruning Execution | Owner Admin Rights | 12 remote branches are superseded or merged. AI agents cannot perform branch deletions on shared origin. | Run `.\scripts\prune-remote-branches.ps1 -Execute` (or shell script) using repository administrator rights after creating tags & bundle. | PENDING OWNER EXECUTION |
| **NEG-05** | GitHub Branch Protection Enforcement | Owner Admin Rights | Protect production trunk `main` and feature branches `fp-*` against force-pushes and unreviewed merges. | Configure GitHub repository branch protection rules for `main` requiring PR approval and green CI. | PENDING OWNER CONFIGURATION |
| **NEG-06** | Localization / Telugu (te-IN) Translations | Minor | Draft Telugu strings exist in `docs/DESIGN.md` §12, but native speaker review is pending. PRD requires English (en-IN) fallback until verified. | Approve English-only strings in `app/src/main/res/values/strings.xml` for R1 pilot until human native review is completed. | ACCEPTED (English baseline in R1) |
