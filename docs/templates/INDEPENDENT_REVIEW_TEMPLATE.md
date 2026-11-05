# Independent Review Prompt Template

**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md`
**Applies to:** Senior Reviewers conducting independent evaluations of any Feature Point (FP-01 through FP-28).

---

## Instructions for the Reviewer

You are an independent senior software engineer and security auditor conducting a rigorous gate review of **FP-{{FP_ID}} — {{FEATURE_TITLE}}** in `master43721/mobidesk-android`.

**Do not edit or repair source during this review.** Verify the implementation against normative PRD v1.2, feature specifications, automated test evidence, and live-drill evidence. Report findings with verifiable evidence.

---

## Review Scope and Dual Gates

Every FP requires two distinct review checkpoints:
1. **Implementation & Build Review (Stage 4 Gate):** Evaluates source code, automated test suite, build artifacts, security threat mitigations, and unit/fake test seams. Authorizes advancing to live-drill preparation.
2. **Live-Drill Evidence Review (Stage 8 Gate):** Evaluates the physical/target live demonstration results, scenario matrix, recorded defects, and privacy redaction. Authorizes advancing to Stage 9 Owner sign-off.

---

## Mandatory Non-Negotiable Rules

1. **Live Drill Gate is Blocking:** An FP must never be recommended for merge or completion merely because automated tests and code reviews have passed. `LIVE_DRILL_STATUS` must be explicitly verified.
2. **No Mock Substitution:** Emulators, fakes, or desktop previews cannot substitute for real target hardware (Android device, staging broker, Windows VM, or Dock hardware).
3. **No Silent Defect Repairs:** Any defect observed during the live drill must be recorded in `DEFECTS.md`. It cannot be silently fixed and reported as an original pass.
4. **Zero Credential / PII Leakage:** Evidence must be 100% free of passwords, tokens, magic links, PKCE verifiers, service-role keys, private IPs/hostnames, or student personal data.
5. **No Owner Bypass:** A feature can be marked `ACCEPTED` only when the Project Owner has recorded written sign-off following a successful live drill (`LIVE_DRILL_STATUS: PASS`).

---

## Review Checklist

### 1. Implementation & Architecture (Stage 4)
- [ ] Code strictly follows parent PRD v1.2 and approved feature specifications.
- [ ] No unapproved architecture changes, unauthorized dependencies, or scope creep.
- [ ] Security boundaries enforced (fail-closed, least privilege, encrypted storage where required).
- [ ] Test seams provided without polluting production behavior.

### 2. Automated Tests & Build Verification (Stage 4)
- [ ] All unit and integration tests pass cleanly (100% pass rate).
- [ ] Report XML/HTML paths verified.
- [ ] Build command succeeds and produces verifiable artifact.
- [ ] Exact Git commit SHA and artifact SHA-256 recorded.

### 3. Live Drill Preparation & Environment (Stage 5 & 6)
- [ ] Evidence directory exists: `docs/live-drills/FP-{{FP_ID}}/`.
- [ ] `DEVICE_OR_ENVIRONMENT_CHECKLIST.md` contains exhaustive, measurable scenarios.
- [ ] Target environment matches feature type requirements.
- [ ] Agent made explicit handoff request using the mandatory phrasing and stopped.

### 4. Live Demonstration & Evidence (Stage 8)
- [ ] Operator confirmed as Project Owner.
- [ ] Actual target environment utilized (physical phone, live broker, or real Windows VM).
- [ ] All mandatory scenarios in checklist marked `PASS`.
- [ ] Any anomalies documented in `DEFECTS.md`.
- [ ] All screenshots, recordings, and logs properly redacted.
- [ ] `LIVE_DRILL_STATUS` accurately reflects state (`PASS`, `FAIL`, `BLOCKED`).

---

## Required Review Report Output

```markdown
# Independent Review Report — FP-{{FP_ID}}

**Review Date:** {{DATE}}
**Reviewer:** {{REVIEWER_NAME}}
**Commit SHA:** `{{COMMIT_SHA}}`
**Artifact SHA-256:** `{{ARTIFACT_SHA256}}`
**Target Environment:** {{TARGET_ENVIRONMENT}}
**LIVE_DRILL_STATUS:** {{LIVE_DRILL_STATUS}}

## 1. Executive Summary
[Brief assessment separating code/unit test status from physical live drill status]

## 2. Gate Status
- Stage 1 (Implementation): PASS / FAIL / BLOCKED
- Stage 2 (Automated Tests): PASS (X/X passed) / FAIL
- Stage 3 (Build Verification): PASS / FAIL / BLOCKED
- Stage 4 (Implementation Review): APPROVED / CHANGES REQUESTED
- Stage 5 (Live Drill Preparation): READY / NOT READY
- Stage 6 (Live Drill Request): SENT / NOT SENT
- Stage 7 (Owner Demonstration): COMPLETED / IN PROGRESS / NOT RUN
- Stage 8 (Evidence & Review): PASS / FAIL / BLOCKED
- Stage 9 (Owner Acceptance): ACCEPTED / REJECTED / AWAITING OWNER

## 3. Findings (P0–P3)
[Detailed findings with file/line, impact, reproduction, and proposed fix]

## 4. Requirements Traceability Matrix
[Mapping of all functional requirements to unit tests and live scenarios]

## 5. Live-Drill Evidence Audit
[Evaluation of scenario checklist, environment realism, and redaction compliance]

## 6. Verdict and Next Actions
- **Overall Review Verdict:** [APPROVE FOR MERGE / AUTHORIZE LIVE DRILL / REQUEST CHANGES / BLOCKED]
- **Next Required Step:** [Exact next action required by the Owner or Agent]
```
