# MobiDesk — Decision Register

**Date:** 3–4 November 2026  
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
| DEC-06 | Windows guest auth | Student enters separate Windows NLA credentials for each session; broker never receives guest password | Owner confirmed prompt-per-session pilot path on 3 Nov 2026; PRD §6.4; DESIGN.md §5.4 | OWNER CONFIRMED — R1 |
| DEC-07 | Grant semantics | 256-bit+ CSPRNG grant; 60s maximum TTL; single-use; hash at rest; bound to user/session/assignment | PRD §§6.3, 9; API_CONTRACT.md | REQUIRED |
| DEC-08 | Revocation | Validate Auth session_id at grant; revalidate session and assignment every <=15s during relay; close within <=30s after revocation/change | PRD §§6.3, 9; API_CONTRACT.md | REQUIRED |
| DEC-09 | RDP identity | Verify server using grant-bound SHA-256 fingerprint or approved internal CA; no trust-all/accept-any | PRD §6.7; SECURITY_THREAT_MODEL.md | REQUIRED |
| DEC-10 | LocalBridge | Prefer socketpair/connected descriptor, then app-private Unix socket with peer UID verification; TCP fallback only with authenticated one-shot handshake | PRD §6.6; TECHNICAL_SPEC.md §6 | REQUIRED |
| DEC-11 | Native session window | Dedicated non-exported RdpSessionActivity with RdpRenderTarget and FLAG_SECURE | PRD §§6.1, 6.8, 9; DESIGN.md | REQUIRED |
| DEC-12 | RDP status semantics | Separate VM lifecycle from RDP transport; TCP reachability is not RDP negotiation or NLA success | PRD §5.2; API_CONTRACT.md §4 | REQUIRED |
| DEC-13 | R1 backgrounding | PRD v1.2 normative rule remains foreground-only; Owner has directed background continuity as the desired R1 change | PRD §§5.3, 6.8; see DEC-27 and PRD_V1_3_CHANGES.md | V1.2 BASELINE — CHANGE PROPOSED |
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
| DEC-26 | Dock rendering architecture | Compare phone-render/H.264-to-Pi and Pi-native FreeRDP; select only after a measured spike | Owner choice: keep both options open; see FP-20 | OWNER CONFIRMED — SELECTION OPEN |
| DEC-27 | R1 background session direction | Allow an active RDP session to continue when the user backgrounds MobiDesk/switches apps; do not assume force-stop or OS process-death survival | Owner choice on 3 Nov 2026; conflicts with PRD v1.2 foreground-only/5-second teardown rule | OWNER-DIRECTED PROPOSED PRD CHANGE — NOT NORMATIVE UNTIL V1.3 APPROVAL |
| DEC-28 | Screen-lock behavior | Decide separately whether screen lock/display-off preserves the session, pauses it or terminates it | Owner's background selection did not expressly settle lock-screen behavior; privacy/battery/platform implications need an explicit decision | OPEN |
| DEC-29 | Additional product scope | No additional scope beyond R1 Mobile Cloud PC and R2 Dock; admin console excluded | Owner choice on 3 Nov 2026 | SUPERSEDED 4 Nov 2026 by DEC-30..DEC-37 |


| DEC-30 | Application roles | Prototype has two roles: college_admin and student; authoritative role and active status live in private.app_user_roles, checked server-side; single-college tenant | Owner instruction on 4 Nov 2026; docs/ADMIN_ACCOUNT_MODEL.md | OWNER CONFIRMED — SCHEMA PROPOSED |
| DEC-31 | Student account creation | No student self-signup. College Administrator creates roster/Auth account through trusted backend and sends invitation; student only activates pre-created account and sets password | Owner instruction on 4 Nov 2026; Supabase admin invitation docs and current roster trigger | OWNER CONFIRMED |
| DEC-32 | Initial College Administrator bootstrap | One initial college_admin account is provisioned out-of-band by Project Owner; student provisioning UI cannot create/promote admins | Owner instruction on 4 Nov 2026; least-privilege bootstrap design | OWNER CONFIRMED |
| DEC-33 | Role-aware app | The same Kotlin/Compose app resolves role server-side and routes to Administrator or Student workspace | Owner instruction on 4 Nov 2026; docs/ADMIN_ACCOUNT_MODEL.md | OWNER CONFIRMED |
| DEC-34 | Privileged administration boundary | Student creation/invites, role checks, assignment writes and host/VM/session actions are performed by trusted server APIs; no service-role key or direct libvirt/SSH access in Android | Supabase Admin API requires trusted server secret; docs/ADMIN_ACCOUNT_MODEL.md | REQUIRED |
| DEC-35 | Student first activation | Admin invitation link activates a pre-created Auth identity; exact Supabase Kotlin 3.6.0 invite callback path must be proven separately from password recovery | Pinned SDK OtpType.Email.INVITE and admin invite APIs; docs/ADMIN_ACCOUNT_MODEL.md | OWNER CONFIRMED — TECHNICAL SPIKE REQUIRED |
| DEC-36 | Admin operational capabilities | College Administrator can manage roster-backed student accounts, one-VM assignments, approved host/VM lifecycle actions and active student sessions; all privileged actions are audited | Owner instruction on 4 Nov 2026; FP-23..FP-28 | OWNER CONFIRMED |
| DEC-37 | Admin action boundary | No arbitrary shell/SSH, raw libvirt console, unrestricted network configuration or host credentials from Android | Security boundary in docs/ADMIN_ACCOUNT_MODEL.md | REQUIRED |

## 2. Owner approval register

| Decision / item | Owner | Due date proposed | Closure evidence |
|---|---|---|---|
| Repository visibility | Project Owner | 4 Nov 2026 | GitHub visibility matches intended privacy/access policy |
| PRD approval status and canonical source projection | Project Owner | 5 Nov 2026 | Written approval; future revision plan if PDF cover is to be changed |
| Actual RD-1 hardware | Project Owner | 5 Nov 2026 | Physical device model, OS/API, ABI/build recorded in private test evidence |
| Distribution and signing key custody | Project Owner | 6 Nov 2026 | Selected channel and approved key custody/recovery plan |
| FR-SEC-01..03 repair | Project Owner | 6 Nov 2026 | Approval of proposed v1.3 wording |
| Guest Windows account and lockout model | Project Owner + Lab IT | 6 Nov 2026 | Approved account, reset, lockout, failed-logon monitoring and offboarding procedure |
| College Administrator bootstrap and MFA | Project Owner | 7 Nov 2026 | Initial admin provisioned outside app; server-authoritative role and MFA verification recorded |
| Student invitation and activation path | Project Owner | 8 Nov 2026 | Admin-created roster user receives invite; exact SDK callback, first password and invalid/expired-link tests pass |
| Administrator API and role/RLS design | Project Owner | 10 Nov 2026 | Approved API contract, role checks, RLS policies and audit event specification |
| Admin server/VM action allowlist | Project Owner + Lab IT | 12 Nov 2026 | Approved host/VM actions, confirmations, rollback and audit requirements |
| VM persistence/backup/retention/semester end | Project Owner + Lab IT | 13 Nov 2026 | Approved policy and successful restore test |

## 3. Change control

For any changed decision:
1. Create a new DEC-XX record with date, owner and reason.
2. Identify PRD requirements, APIs, tests, data model, release gates and rollback affected.
3. Do not weaken a PRD MUST without an approved PRD revision.
4. Keep old decisions with status SUPERSEDED rather than deleting history.


## 4. FP-02 v0.2 and Codespaces decisions — 5 Nov 2026

| ID | Topic | Current decision / position | Evidence or reason | State |
|---|---|---|---|---|
| DEC-38 | FP-02 Connect affordance | Connect remains an action to request a fresh status check when status is absent, stale or indeterminate. A fresh known-negative state disables it; when that sample becomes stale, it becomes indeterminate. Every tap rechecks before FP-05. | docs/FEATURE_SPECIFICATIONS/FP-02/PRD.md §6; critique resolution | PROPOSED PARENT AMD-01 — OWNER APPROVAL PENDING |
| DEC-39 | FP-02 freshness clock | Use Android monotonic elapsedRealtime receipt-age plus conservative request RTT upper-bound. sampledAt is display-only; never gate using handset wall clock. | FP-02 TECHNICAL_DESIGN §6 and API_CONTRACT v1.1 proposal | PROPOSED API/PRD CHANGE — G1 |
| DEC-40 | Assignment/status consistency | FP-03 workspace and FP-04 status carry the same opaque equality-only assignmentTag from the shared trusted resolver. Mismatch discards/reloads pair once, then ASSIGN-02. | PRD-1.2-AMD-01; API_CONTRACT v1.1 | PROPOSED — FP-03/FP-04 CONTRACT REQUIRED |
| DEC-41 | Android status client ownership | FP-02 owns Android StatusRepository, timeout/cancellation, freshness, 429 handling and client contract tests; FP-04 owns server endpoint/probe/rate limiting. | FP-02 v0.2 ownership table | SELECTED FEATURE BOUNDARY — PARENT/API APPROVAL PENDING |
| DEC-42 | Status request mechanics | 8s client deadline, cancellation, 2s local debounce, honor 429 Retry-After, no automatic retry; workspace/status initial reads run in parallel after FULLY_AUTHENTICATED. | FP-02 v0.2 TECHNICAL_DESIGN | SELECTED FOR FP-02 — API G1 REQUIRED |
| DEC-43 | Primary Codespace | Reuse existing didactic-giggle Codespace for master43721/mobidesk-android; upgrade to available 4-core/16-GB/32-GB Central India tier; preserve existing dirty worktree. | Codespaces API state on 5 Nov 2026; machine update accepted | APPLIED — RESTART REQUIRED FOR NEW MACHINE SIZE |
| DEC-44 | FP-02 product spec package | Keep product requirements under docs/FEATURE_SPECIFICATIONS/FP-02; keep agent implementation/review prompts under tools/prompts/fp-02. | FP-02 v0.2 branch diff | APPLIED IN REVIEW BRANCH |
| DEC-45 | FP-01 to FP-02 integration proof | Owner reports FP-01 implementation complete. However, FP-01 feature branches' progress docs say implementation changes are documentation-only, and their MobiDeskViewModel blob is identical to main (32bbfc45c96baf9021bd34f2d860d44bbd95b6ed), still using raw SessionStatus.Authenticated without FULLY_AUTHENTICATED. The existing Codespace has dirty/unpushed main changes that may contain the reported implementation; inspect and preserve them before deciding. | FP-01 PROGRESS.md on feature/fp-01-authentication-draft and docs/fp-01-current-main-handoff; source blob SHA above; Codespaces status 5 Nov | OPEN — INTEGRATION VERIFICATION; DO NOT OVERWRITE WORKSPACE |



| DEC-46 | Development vs runtime host | Use GitHub Codespaces for native Android development/build/test and keep the HP Ubuntu Server as the persistent libvirt/VM and approved service host. Codespaces does not replace the VM host, physical Android device, Tailscale or Cloudflare runtime. | docs/CODESPACES_WORKFLOW.md; docs/OPERATIONS.md §12 | CONFIRMED ARCHITECTURE BOUNDARY |
| DEC-47 | Existing Codespace dirty worktree | Preserve and inspect the existing `didactic-giggle` uncommitted/unpushed main worktree before switching branches. No reset/clean/branch switch without an explicit reviewed preservation step. | GitHub Codespaces API status, 5 Nov 2026 | ACTIVE SAFETY HOLD |
