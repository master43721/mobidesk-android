# MobiDesk — Agent & Contributor Governance (`AGENTS.md`)

**Repository:** `master43721/mobidesk-android`  
**Normative Product Authority:** `docs/MobiDesk_PRD_v1.2.pdf`  
**Process Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md`

---

## 1. Core Architecture & Tech Stack Rules

1. **Native Android Stack Only**:
   - Written exclusively in native Kotlin, Jetpack Compose, and official FreeRDP (C/C++ via JNI/NDK).
   - No cross-platform runtimes (no Flutter, no React Native, no Expo).
2. **Submodule Architecture**:
   - Upstream FreeRDP is pinned as a Git submodule in `third_party/FreeRDP` (commit `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1`).
   - Do not vendor loose upstream C files directly into application source directories.
3. **Security Invariants**:
   - Zero credential or token leakage in logs, navigation arguments, or exception messages.
   - Session tokens must be stored using Android Keystore-backed encrypted storage (`EncryptedSharedPreferences`).
   - The Android client must NEVER be given a Supabase `service_role` key or hypervisor credentials.
   - Internal VM hostnames, private IPs, and raw internal port numbers (e.g. TCP/3389) must never be displayed in the student user interface.

---

## 2. Branching & Contribution Workflow (Trunk-Based)

- **Trunk (`main`)**: The production-ready, peer-reviewed trunk. Never force-push to `main`.
- **Feature Branches (`fp-NN-<slug>`)**:
  - Created just-in-time from `main` when implementation begins (e.g., `fp-01-auth-hardening`, `fp-02-student-workspace`).
  - Merged back into `main` **only** after fulfilling the Terminal Acceptance Invariant below.
- **Milestone Tags**:
  - Tag historical bootstrap: `baseline-fp-00`.
  - Tag accepted feature points after merge: `fp-NN-accepted`.

---

## 3. Mandatory Terminal Acceptance Invariant (The Live Drill Gate)

Per `docs/MANDATORY_LIVE_DRILL_POLICY.md`, no feature point (FP) is considered complete or accepted merely because code compiles or unit tests pass. 

Completion strictly requires all 6 criteria:

```text
FP Acceptance = (Implementation == PASS)
              ∧ (Automated Tests == PASS)
              ∧ (Build Verification == PASS)
              ∧ (Independent Code Review == PASS)
              ∧ (Physical Live Drill == PASS)
              ∧ (Project Owner Decision == ACCEPTED)
```

### Every FP Branch & Package Structure
Inside `docs/FEATURE_SPECIFICATIONS/FP-XX/`:
- `PLAN.md`: Scoped engineering implementation plan.
- `TODO.md`: Scoped task checklist and verification status.
- `PRD.md`: Scoped product requirements derived from `docs/MobiDesk_PRD_v1.2.pdf`.
- `TECHNICAL_DESIGN.md`: Architecture, data contracts, and state machines.
- `TEST_PLAN.md`: Test matrix (unit, JVM, integration).
- `DECISIONS.md`: Feature-specific architectural decisions.

Inside `docs/live-drills/FP-XX/`:
- `DEVICE_OR_ENVIRONMENT_CHECKLIST.md`: Physical phone and network prerequisites.
- `EXECUTION_REPORT.md`: Physical test run log recording:
  - Exact Git commit SHA
  - APK build number & SHA-256 checksum
  - Pass/Fail results of real physical user interactions
- `DEFECTS.md`: Known bugs or live drill regressions.
- `evidence/`: Screenshots, screen recordings, and device logs.

---

## 4. Binary & APK Artifact Policy

- **No Binaries in Git**: Never commit `.apk`, `.aab`, or large test binaries directly into Git history.
- **Local Testing (`executables-apks/`)**: Developers may drop test APKs into `executables-apks/` on their local machine; this directory is ignored by `.gitignore`.
- **CI Artifacts**: Automated builds produce APKs uploaded as GitHub Actions run artifacts.

---

## 5. Single Source of Truth for Documentation

All canonical documentation resides in `/docs/` with uppercase naming:
- `docs/DESIGN.md`: End-to-end screen transitions and user journey flows.
- `docs/TECHNICAL_SPEC.md`: Technical architecture and system boundaries.
- `docs/DECISIONS.md`: Global decision log.
- `docs/GLOSSARY.md`: Project terminology.
- `docs/API_CONTRACT.md`: OpenAPI and broker contracts.
- `docs/FEATURE_INVENTORY.md`: All 28 Feature Points inventory.
- Root duplicate markdown files are retired to eliminate split sources of truth.
