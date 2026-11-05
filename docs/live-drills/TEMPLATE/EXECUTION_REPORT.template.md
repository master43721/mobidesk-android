# FP-{{FP_ID}} Live-Drill Execution Report

**Feature Package:** FP-{{FP_ID}} — {{FEATURE_TITLE}}
**Application / Subsystem:** `{{APPLICATION_OR_SUBSYSTEM}}`
**Target Repository:** `master43721/mobidesk-android`
**Handoff Branch:** `{{BRANCH_NAME}}`
**Exact Source Commit (HEAD SHA):** `{{COMMIT_SHA}}`
**Execution Date:** {{DATE}}
**Current Live-Drill Status:** `LIVE_DRILL_STATUS: {{STATUS}}`
**Owner Sign-off Status:** {{OWNER_SIGN_OFF_STATUS}}

---

## 1. Executive Summary

[Summary of the feature, requirements reconciliation, and status of automated vs live verification.]

---

## 2. Environment & Tooling Verification

| Component | Version / Specification |
|:---|:---|
| Operating System | [e.g. Linux / Android version] |
| Runtime / Build Tools | [e.g. OpenJDK 21 / NDK 29 / Gradle 9.6.1] |
| Target Environment | [e.g. Physical Android Device / Test Windows VM / Staging Broker] |

---

## 3. Requirements Reconciliation Matrix

| Requirement ID | Description | Responsible Artifacts | Automated Test Reference | Live Scenario # | Status | Notes |
|:---:|:---|:---|:---|:---:|:---:|:---|
| `FP{{FP_NUM}}-FR-001` | [Requirement description] | [Class / file] | [Test name] | 1 | PASS / GATED | [Notes] |

---

## 4. Automated Test Summary

- Command: `{{TEST_COMMAND}}`
- Results: {{PASS_COUNT}} passed, {{FAIL_COUNT}} failed, 0 skipped (100% success rate).
- Report Path: `{{REPORT_PATH}}`

---

## 5. Artifact Verification

- Expected Artifact: `{{ARTIFACT_PATH}}`
- SHA-256 Checksum: `{{ARTIFACT_SHA256}}`
- Package / Version: `{{PACKAGE_ID}}` (v{{VERSION_NAME}})

---

## 6. Live Drill Outcome & Owner Sign-Off

- Mandatory Scenarios Result: {{PASS_SCENARIOS}} / {{TOTAL_SCENARIOS}} PASS
- Defects Encountered: See `DEFECTS.md`
- Owner Decision: [ACCEPTED / REJECTED] by Project Owner Master43721 on [Date]
