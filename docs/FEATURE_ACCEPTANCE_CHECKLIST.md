# MobiDesk — Feature Acceptance Checklist

**Effective Date:** 5 November 2026
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md`
**Purpose:** Standard acceptance gate checklist required for every Feature Point (FP-01 through FP-28).

---

## Acceptance Summary Matrix

| Stage | Check Description | Requirement | Gatekeeper | Status |
|:---:|:---|:---|:---:|:---:|
| **1** | Implementation Complete | Clean code matching PRD & technical design | Engineer / Agent | [ ] PASS |
| **2** | Automated Tests Verified | 100% pass rate in standard test suite | Gradle / CI | [ ] PASS |
| **3** | Integration Build Verified | Clean artifact build with verified SHA-256 | Compiler / Packager | [ ] PASS |
| **4** | Independent Review Passed | Senior review approves code & security model | Independent Reviewer | [ ] PASS |
| **5** | Live-Drill Prepared | Environment seeded, checklist ready | Agent | [ ] PASS |
| **6** | Explicit Live-Drill Request | Agent sent handoff message & stopped | Agent | [ ] PASS |
| **7** | Owner Live Demonstration | Real target execution (phone, broker, VM) | Project Owner | [ ] PASS |
| **8** | Evidence & Drill Review | Reports, defects, redacted evidence logged | Independent Reviewer | [ ] PASS |
| **9** | Explicit Owner Decision | Written Owner sign-off recorded | Project Owner | [ ] ACCEPTED |

---

## Detailed Stage Checklists

### Stage 1: Implementation
- [ ] Code strictly follows normative PRD v1.2 and approved feature specifications.
- [ ] No unapproved architecture changes, unauthorized dependencies, or scope creep.
- [ ] Security boundaries enforced (no secrets in APK, least privilege, fail-closed handling).
- [ ] Test seams provided without polluting production behavior.

### Stage 2: Automated Tests
- [ ] Unit tests pass: `./gradlew --no-daemon :app:testDebugUnitTest` (100% pass rate).
- [ ] Error mapping and boundary conditions tested.
- [ ] No fake or flaky assertions; test reports captured in standard Gradle report paths.
- [ ] Baseline regression tests pass without regression.

### Stage 3: Build & Integration Verification
- [ ] Clean build from Git commit: `./gradlew assembleDebug` (or subsystem build command).
- [ ] Exact Git commit SHA recorded.
- [ ] Artifact SHA-256 hash computed and recorded.
- [ ] Artifact package name, version name, and version code verified against specification.

### Stage 4: Independent Implementation Review
- [ ] Review conducted strictly using the independent review prompt template.
- [ ] Traceability matrix populated for all feature functional requirements.
- [ ] STRIDE security threat mitigations verified.
- [ ] P0–P3 findings resolved and verified prior to advancing to drill.

### Stage 5: Live-Drill Preparation
- [ ] Live drill directory scaffolded: `docs/live-drills/FP-XX/`.
- [ ] `DEVICE_OR_ENVIRONMENT_CHECKLIST.md` populated with concrete, measurable scenarios.
- [ ] Target environment provisioned (physical device, staging broker, or test VM).
- [ ] Staging accounts seeded without using real personal data.
- [ ] Stop rules and abort conditions defined.
- [ ] `LIVE_DRILL_STATUS` set to `READY FOR OWNER`.

### Stage 6: Explicit Live-Drill Request to Project Owner
- [ ] Agent formulates the mandatory handoff notification containing:
  - Exact artifact path and SHA-256 checksum.
  - Source Git branch and commit SHA.
  - Target environment specifications.
  - Setup and installation instructions.
  - Scenario checklist link.
  - Expected outcomes.
- [ ] Agent explicitly sends handoff to the Owner.
- [ ] **Agent STOPS execution** and does not proceed until Owner provides live demonstration feedback.

### Stage 7: Owner Live Demonstration in Target Environment
- [ ] Operator: Project Owner (Master43721).
- [ ] Real target environment used (physical Android device, deployed broker, or real Windows VM).
- [ ] Interactive touch and user flows exercised one stage at a time.
- [ ] `LIVE_DRILL_STATUS` set to `IN PROGRESS`.
- [ ] Every mandatory scenario in `DEVICE_OR_ENVIRONMENT_CHECKLIST.md` exercised.

### Stage 8: Evidence Capture & Independent Drill Review
- [ ] `EXECUTION_REPORT.md` updated with observed outcomes for every scenario.
- [ ] Any defects logged in `DEFECTS.md` with severity and reproduction steps.
- [ ] Screenshots, logs, or recordings added to `evidence/` with all PII and credentials redacted.
- [ ] Independent reviewer audits evidence against acceptance criteria.
- [ ] If all scenarios pass: `LIVE_DRILL_STATUS` set to `PASS`.
- [ ] If defects found: `LIVE_DRILL_STATUS` set to `FAIL` or `BLOCKED`.

### Stage 9: Explicit Owner Acceptance or Rejection
- [ ] Project Owner reviews the execution report, defects, and evidence.
- [ ] Project Owner explicitly records written acceptance: `ACCEPTED` or `REJECTED`.
- [ ] Acceptance recorded in `docs/PROGRESS.md`, `docs/TASKS.md`, and `docs/FEATURE_SPECIFICATIONS/FP-XX/PROGRESS.md`.
- [ ] Feature point status promoted to `COMPLETE` only after Owner acceptance.

---

## Terminal Acceptance Sign-Off Block

```markdown
### Final Feature Sign-Off

- **Feature Point:** FP-XX — [Feature Title]
- **Commit SHA:** [Exact 40-character Git SHA]
- **Artifact SHA-256:** [SHA-256 Hash]
- **Target Device / Environment:** [Model / OS / Service URI]
- **Automated Tests:** PASS ([Count]/[Count])
- **Independent Implementation Review:** APPROVED by [Reviewer Name]
- **Live Drill Status:** PASS
- **Independent Live-Drill Evidence Review:** APPROVED by [Reviewer Name]
- **Project Owner Verdict:** ACCEPTED by Master43721 on [Date]
```
