# FP-02: Task Checklist & Verification Status (`TODO.md`)

**Feature Point:** FP-02 — Student Dashboard and Workspace  
**Status:** IMPLEMENTATION INTEGRATED (Pending Live Drill Physical Gate)

---

## Task Checklist

- [x] **FP02-001**: Review normative PRD §6.1, §7 and establish acceptance boundaries
- [x] **FP02-002**: Define VM status models & lifecycle enums (`StatusModels.kt`)
- [x] **FP02-003**: Implement StatusRepository interface & DefaultStatusRepository (`DefaultStatusRepository.kt`)
- [x] **FP02-004**: Implement ConnectPolicy evaluation logic (`ConnectPolicy.kt`)
- [x] **FP02-005**: Compose Student Dashboard UI with profile and VM card (`MobiDeskApp.kt`)
- [x] **FP02-006**: Add pull-to-refresh and freshness timer indicators
- [x] **FP02-007**: Add string resources and localized labels (`strings.xml`)
- [x] **FP02-008**: Implement automated test suites
  - [x] `ConnectPolicyTest.kt`
  - [x] `StatusRepositoryContractTest.kt`
  - [x] `MobiDeskFp02Test.kt`
- [ ] **FP02-009**: Physical Live Drill Execution (Hands-on test on physical phone)
  - [x] Checklist prepared (`docs/live-drills/FP-02/DEVICE_OR_ENVIRONMENT_CHECKLIST.md`)
  - [ ] Execute physical test scenarios & log in `docs/live-drills/FP-02/EXECUTION_REPORT.md`
  - [ ] Record APK SHA-256 and screenshots in `docs/live-drills/FP-02/evidence/`
- [ ] **FP02-010**: Owner Acceptance Sign-off
