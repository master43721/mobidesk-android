# Generic Feature Live-Drill Handoff Template

**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md` § 4
**Applies to:** All future AI agents and engineers completing implementation and automated verification of any Feature Point (FP-01 through FP-28).

---

## Instructions for the Agent

When implementation (Stage 1), automated tests (Stage 2), integration build verification (Stage 3), and independent implementation review (Stage 4) have passed:

1. Populate the live-drill evidence directory: `docs/live-drills/FP-XX/`
   - `EXECUTION_REPORT.md`
   - `DEVICE_OR_ENVIRONMENT_CHECKLIST.md`
   - `DEFECTS.md`
2. Set `LIVE_DRILL_STATUS: READY FOR OWNER` in `docs/PROGRESS.md` and feature progress docs.
3. Transmit the following mandatory message to the Project Owner.
4. **STOP EXECUTION IMMEDIATELY.** Do not perform further work or claim completion until the Project Owner provides interaction results.

---

## Mandatory Handoff Message Structure

```markdown
FP-{{FP_ID}} is ready for its mandatory live drill. Here is the exact artifact, source SHA, test environment, installation/setup procedure, scenario checklist and expected outcomes. Please perform the live drill.

### 1. Feature Information
- **Feature Point:** {{FP_ID}} — {{FEATURE_TITLE}}
- **Source Branch:** `{{BRANCH_NAME}}`
- **Source Commit SHA:** `{{COMMIT_SHA}}`
- **Current Live Drill Status:** `READY FOR OWNER`

### 2. Artifact Details
- **Artifact Path:** `{{ARTIFACT_PATH}}` (e.g. `app/build/outputs/apk/debug/app-debug.apk`)
- **Artifact SHA-256:** `{{ARTIFACT_SHA256}}`
- **Build Command Used:** `{{BUILD_COMMAND}}`
- **Package ID / Version:** `{{PACKAGE_ID}}` (version {{VERSION_NAME}}, versionCode {{VERSION_CODE}})

### 3. Target Environment & Preflight Setup
- **Target Environment:** {{TARGET_ENVIRONMENT}} (e.g., Physical Android Phone with Android 10–14 / Deployed Test Broker / Windows Test VM)
- **Required Configuration:**
  - `{{CONFIG_STEP_1}}` (e.g., Supabase publishable key configured in `local.properties`)
  - `{{CONFIG_STEP_2}}` (e.g., QA test account credentials supplied out-of-band)
- **Installation Procedure:**
  ```bash
  {{INSTALLATION_COMMANDS}}
  # Example: adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```

### 4. Scenario Checklist
Please follow and verify each scenario in:
`docs/live-drills/{{FP_ID}}/DEVICE_OR_ENVIRONMENT_CHECKLIST.md`

Summary of key scenarios to exercise:
1. {{SCENARIO_1_SUMMARY}}
2. {{SCENARIO_2_SUMMARY}}
3. {{SCENARIO_3_SUMMARY}}
4. {{SCENARIO_4_SUMMARY}}

### 5. Expected Outcomes & Stop Rules
- **Expected Outcomes:** All {{SCENARIO_COUNT}} scenarios pass with expected UI states, correct backend interaction, safe error handling, and zero credential leakage.
- **Stop Rule:** If a crash, visual glitch, state error, or unexpected behavior is observed, please halt the demonstration and report the exact behavior. We will document it in `DEFECTS.md`.

---
*Awaiting your demonstration results. I will remain stopped until you report the live-drill outcome.*
```
