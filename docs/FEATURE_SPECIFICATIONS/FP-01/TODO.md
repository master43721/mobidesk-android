# FP-01: Task Checklist & Verification Status (`TODO.md`)

**Feature Point:** FP-01 — Student Authentication and Account Recovery  
**Status:** IMPLEMENTED & HARDENED (Pending Live Drill Physical Gate)

---

## Task Checklist

- [x] **FP01-001**: Align PRD requirements & acceptance boundary
- [x] **FP01-002**: Encrypted session storage with Keystore & fallback tier (`MobiDeskApplication.kt`)
- [x] **FP01-003**: AuthRepository & WorkspaceRepository injection interfaces (`MobiDeskRepositories.kt`)
- [x] **FP01-004**: Enforce `AuthLevel.FULLY_AUTHENTICATED` state invariant (`MobiDeskViewModel.kt`)
- [x] **FP01-005**: Durable recovery state storage & anti-replay (`RecoveryStateStore.kt`)
- [x] **FP01-006**: Intent sanitization for PKCE callback in `MainActivity.kt`
- [x] **FP01-007**: Safe user-facing error message mapper (`AuthErrorMessages.kt`)
- [x] **FP01-008**: Accessible Jetpack Compose UI with TalkBack tags (`MobiDeskApp.kt`)
- [x] **FP01-009**: Unit test suite implemented and passing (40/40 tests)
  - [x] `AuthErrorMessagesTest.kt` (10 tests)
  - [x] `MobiDeskViewModelTest.kt` (23 tests)
  - [x] `RecoveryStateStoreTest.kt` (6 tests)
  - [x] `MobiDeskApplicationTest.kt` (1 test)
- [ ] **FP01-010**: Physical Live Drill Execution (Hands-on test on physical phone)
  - [x] Checklist prepared (`docs/live-drills/FP-01/DEVICE_OR_ENVIRONMENT_CHECKLIST.md`)
  - [ ] Execute physical test scenarios & log in `docs/live-drills/FP-01/EXECUTION_REPORT.md`
  - [ ] Record APK SHA-256 and screenshots in `docs/live-drills/FP-01/evidence/`
- [ ] **FP01-011**: Owner Acceptance Sign-off
