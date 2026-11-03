# MobiDesk — Decision Register

**Date:** 3 November 2026  
**Baseline:** PRD v1.2  
**Rule:** PRD requirements are normative. This register records approved facts, selected engineering interpretations and unresolved owner choices. A recommendation is not treated as an approval.

## 1. Decision records

| ID | Topic | Current decision / position | Evidence or reason | State |
|---|---|---|---|---|
| DEC-01 | Canonical repository | Use only master43721/mobidesk-android | [VERIFIED: GitHub repo metadata and main@5979320] Main is the native Kotlin/Jetpack Compose app named by PRD §6.1. | CONFIRMED |
| DEC-02 | Client stack | Native Kotlin + Jetpack Compose; no Flutter PlatformView architecture | [VERIFIED: settings.gradle, MainActivity.kt, app/build.gradle at main@5979320] | CONFIRMED |
| DEC-03 | Android SDK baseline | minSdk29 / compileSdk37 / targetSdk37; Java/Kotlin target11; ABIs arm64-v8a + x86_64 | [VERIFIED: build.gradle, app/build.gradle, release.properties at main@5979320]. API29 in PRD §8.1 agrees with NFR-COMP-01 preservation. | CONFIRMED |
| DEC-04 | FreeRDP version | Keep exact pinned v3.32.0 gitlink for the current R1 integration spike | [VERIFIED: .gitmodules/gitlink at main@5979320; upstream signed release commit d27a4f7...] | CONFIRMED |
| DEC-05 | Assignment authority | Broker derives exactly one VM assignment from authenticated subject and trusted server data; client never selects target | PRD §§6.3, 9; API_CONTRACT.md | REQUIRED |
| DEC-06 | Windows guest auth | Student enters separate Windows NLA credentials each session; broker never receives guest password | PRD §6.4; DESIGN.md §5.4 | REQUIRED |
| DEC-07 | Grant semantics | 256-bit+ CSPRNG grant; 60s maximum TTL; single-use; hash at rest; bound to user/session/assignment | PRD §§6.3, 9; API_CONTRACT.md | REQUIRED |
| DEC-08 | Revocation | Validate Auth session_id at grant; revalidate session and assignment every <=15s during relay; close within <=30s after revocation/change | PRD §§6.3, 9; API_CONTRACT.md | REQUIRED |
| DEC-09 | RDP identity | Verify server using grant-bound SHA-256 fingerprint or approved internal CA; no trust-all/accept-any | PRD §6.7; SECURITY_THREAT_MODEL.md | REQUIRED |
| DEC-10 | LocalBridge | Prefer socketpair/connected descriptor, then app-private Unix socket with peer UID verification; TCP fallback only with authenticated one-shot handshake | PRD §6.6; TECHNICAL_SPEC.md §6 | REQUIRED |
| DEC-11 | Native session window | Dedicated non-exported RdpSessionActivity with RdpRenderTarget and FLAG_SECURE | PRD §§6.1, 6.8, 9; DESIGN.md | REQUIRED |
| DEC-12 | RDP status semantics | Separate VM lifecycle from RDP transport; TCP reachability is not RDP negotiation or NLA success | PRD §5.2; API_CONTRACT.md §4 | REQUIRED |
| DEC-13 | R1 backgrounding | Foreground-only session; true background/screen lock starts a five-second cancelable teardown | PRD §§5.3, 6.8 | REQUIRED |
| DEC-14 | Dock | Dock streaming and cryptographic AOA pairing stay R2; R1 placeholder only | PRD §4.2; DESIGN.md §11 | DEFERRED TO R2 |
| DEC-15 | VM4 | Keep VM4 excluded until OS/endpoint verification and owner/Lab IT approval | PRD §§10, 15; OPEN_ITEMS.md OI-09 | EXCLUDED |
| DEC-16 | Reference device | PRD nominates Pixel 6 / Android16 / API36 / arm64; actual physical availability is UNKNOWN | PRD §8.1; OI-06 | OPEN |
| DEC-17 | Distribution | Public Play vs permanently private organization-only Play vs owner-managed APK/MDM | OPERATIONS.md §2; OI-08 | OWNER DECISION |
| DEC-18 | Repository visibility | GitHub currently reports public; README says private | [VERIFIED: GitHub metadata and README at main@5979320] | OWNER DECISION |
| DEC-19 | PRD approval label | Owner says v1.2 is final; PDF cover still says review draft / approval required | Owner instruction and PRD PDF p.2 | OWNER DOCUMENTATION DECISION |
| DEC-20 | FR-SEC numbering | FR-SEC-01..03 are not defined in v1.2; candidate wording is in PRD_V1_3_CHANGES.md | Direct source inventory | OWNER APPROVAL REQUIRED |
| DEC-21 | Windows account operations | Per-student guest account lifecycle/lockout/reset must be approved with Lab IT | PRD §9 item 13; OI-10 | OPEN |
| DEC-22 | VM lifecycle and data policy | Persistent/ephemeral, backup, restore, semester reset/delete are unknown | PRD §10; OI-11 | OPEN |
| DEC-23 | Transport integration | Exact FreeRDP custom raw-stream/socket injection, certificate callback and renderer lifecycle are not proven | TECHNICAL_SPEC.md §7; pinned source inspection | BLOCKING G3 |
| DEC-24 | Certificate rotation | Current grant contract carries one active fingerprint/CA policy. Use coordinated maintenance rotation unless a reviewed schema extension supports dual pins. | API_CONTRACT.md §5; OPERATIONS.md §5 | SELECTED R1 OPERATION |
| DEC-25 | Performance thresholds | Use the PRD's 20 attempts, p90, 120s FPS workload, 20-cycle cleanup and recovery criteria; do not report unmeasured outcomes | PRD NFR-PERF; PERFORMANCE_PLAN.md | REQUIRED |

## 2. Owner approval register

| Decision / item | Owner | Due date proposed | Closure evidence |
|---|---|---|---|
| Repository visibility | Project Owner | 4 Nov 2026 | GitHub visibility matches intended privacy/access policy |
| PRD approval status and canonical source projection | Project Owner | 5 Nov 2026 | Written approval; future revision plan if PDF cover is to be changed |
| Actual RD-1 hardware | Project Owner | 5 Nov 2026 | Physical device model, OS/API, ABI/build recorded in private test evidence |
| Distribution and signing key custody | Project Owner | 6 Nov 2026 | Selected channel and approved key custody/recovery plan |
| FR-SEC-01..03 repair | Project Owner | 6 Nov 2026 | Approval of proposed v1.3 wording |
| Guest Windows account and lockout model | Project Owner + Lab IT | 6 Nov 2026 | Approved account, reset, lockout, failed-logon monitoring and offboarding procedure |
| VM persistence/backup/retention/semester end | Project Owner + Lab IT | 13 Nov 2026 | Approved policy and successful restore test |

## 3. Change control

For any changed decision:
1. Create a new DEC-XX record with date, owner and reason.
2. Identify PRD requirements, APIs, tests, data model, release gates and rollback affected.
3. Do not weaken a PRD MUST without an approved PRD revision.
4. Keep old decisions with status SUPERSEDED rather than deleting history.
