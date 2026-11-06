# FP-{{FP_ID}} Target Device / Environment Live-Drill Checklist

**Feature Package:** FP-{{FP_ID}} — {{FEATURE_TITLE}}
**Target Application / Component:** `{{APPLICATION_OR_SUBSYSTEM}}`
**Base Commit:** `{{COMMIT_SHA}}`
**LIVE_DRILL_STATUS:** {{STATUS}}
**Operator:** Master43721 (Owner)
**Verification Agent:** Antigravity

---

## Live-Drill Protocol & Stop Rules

1. Each scenario requires real execution in the actual target environment (e.g. physical touch on Android phone, real API call to staging broker, live connection to Windows VM).
2. Emulators, mocks, or unit tests do not satisfy live-drill verification.
3. Steps are executed and confirmed interactively one stage at a time.
4. Stop Rule: Any crash, security violation, data leakage, or failed criterion immediately halts the drill. Record in `DEFECTS.md`.

---

## Scenarios Matrix

| Scenario # | Scenario Description | Required Input / Action | Expected Result | Result | Evidence Reference |
|:---:|:---|:---|:---|:---:|:---|
| **1** | [Scenario Title] | [Exact physical or API action] | [Concrete expected behavior] | NOT RUN | [Link to screenshot/log] |
| **2** | [Scenario Title] | [Exact physical or API action] | [Concrete expected behavior] | NOT RUN | [Link to screenshot/log] |
| **3** | [Scenario Title] | [Exact physical or API action] | [Concrete expected behavior] | NOT RUN | [Link to screenshot/log] |
