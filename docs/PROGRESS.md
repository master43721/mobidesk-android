# MobiDesk — Progress and Gate Status

**Snapshot date:** 4 November 2026  
**Normative baseline:** PRD v1.2 (Project Owner instruction declares it final; PDF cover text still has a pending-approval label).  
**Planning branch:** docs/college-admin-student-account-model  
**No source-code change is included in this documentation suite.**

## 1. Current gate status

| Gate | PRD meaning | Status on 3 Nov 2026 | Evidence / missing exit criteria |
|---|---|---|---|
| G0 — Product/baseline freeze | Owner-approved PRD; repo/branch/FreeRDP gitlink rechecked; source conflicts recorded | PARTIAL / technical baseline PASS; CI BLOCKED | [VERIFIED: main@5979320] target repo and Kotlin/Compose stack match PRD §6.1; SDK/ABI baseline and signed FreeRDP v3.32.0 gitlink verified. Owner says v1.2 is final; PDF cover still says review draft. Repository visibility is public despite README saying private. [VERIFIED: Actions run 41595687606] Hosted runner could not locate platforms;android-37 at SDK installation; Gradle never ran. |
| G1 — Contract/threat-model freeze | No-client-VM-ID grant contract, status, session-active check, guest auth, pin and local bridge are designed and testable | IN PROGRESS | This docs suite is defining contract, OpenAPI, threat model, spec, design, tests and traceability. Passing contract tests and owner decisions are not yet evidenced. |
| G2 — Broker ready | Auth/assignment/replay/revocation/allowlist; four approved endpoints; status and stable WSS; loopback-only broker binding | NOT STARTED / BLOCKED | [VERIFIED: main source tree and README at main@5979320] no broker service/client implementation is in this Android repo; live broker, stable WSS route and active-session recheck are not evidenced. VM4 remains excluded. |
| G3 — Android session ready | Native RDP Activity, Windows credential prompt, pinned FreeRDP, cert callback and secure local bridge | NOT STARTED / BLOCKED | [VERIFIED: MainActivity.kt, AndroidManifest.xml and app source tree at main@5979320] launcher and Compose shell exist; no MobiDesk-owned RdpSessionActivity/session coordinator or protected WSS bridge is present. |
| G4 — Physical R1 acceptance | Correct assigned VM and all authorization/performance/recovery/cleanup gates pass on RD-1 | NOT STARTED / BLOCKED | RD-1 physical availability is UNKNOWN. No physical RDP attempt ledger or measured FPS/latency dataset is evidenced. |
| G5 — Release and operations ready | Exact owner-signed artifact; distribution, support, backup/restore, retention and rollback approved | NOT STARTED / BLOCKED | [VERIFIED: app/build.gradle at main@5979320] no owner release signing config is present; historical debug signing is not release signing. Distribution and lifecycle/retention decisions remain open. |

## 2. Verified baseline facts

- **[VERIFIED: GitHub metadata and main@5979320]** Canonical repository is master43721/mobidesk-android; main currently points to commit 5979320b05e1e6c1d872d82dcc17e65b0901e7e5.
- **[VERIFIED: app/build.gradle and build.gradle at main@5979320]** Minimum/compile/target SDK is 29/37/37; Java/Kotlin target is 11; native ABIs are arm64-v8a and x86_64.
- **[VERIFIED: third_party/FreeRDP gitlink and upstream commit API]** FreeRDP is pinned to the signed release 3.32.0 commit d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1.
- **[VERIFIED: .github/workflows/android-ci.yml at main@5979320]** CI runs JVM tests and a debug APK build; connected Android instrumentation is opt-in and uses an API 36 emulator.
- **[VERIFIED: AndroidManifest.xml at main@5979320]** Cleartext traffic is disabled in the current manifest.
- **[VERIFIED: README.md at main@5979320]** The repository reports that remote desktop session integration is incomplete and that the historical APK is a debug artifact only.

| CI SDK provisioning failure | 3 Nov 2026 | Run 41595687606 / job 125782381543 stopped at SDK package installation: platforms;android-37 not found; Gradle test/build and later steps skipped | Open CI/build follow-up MD-G0-03; no CI/Gradle edits in this docs branch |

## 3. Dated progress log

| Date | Work | Result / evidence | Gate impact |
|---|---|---|---|
| 3 Nov 2026 | Read-only G0 repository and stack audit | docs/BASELINE_AUDIT.md; native Kotlin/Compose confirmed; no stack/repo mismatch | Technical G0 check passed |
| 3 Nov 2026 | Reconciled minSdk question | API 29 matches both PRD §8.1 and current Gradle baseline; compile/target 37/37 retained | NFR-COMP-01 baseline selected |
| 3 Nov 2026 | Revalidated FreeRDP gitlink | Pinned commit identified as signed FreeRDP 3.32.0 release | Supports G3 spike, not RDP completion |
| 3 Nov 2026 | Compared current main and prior docs branch | this suite branch existed at ca34e425a9f8c02dfc3f831ca620e590ecb6c789 with merge base a6c7282; it was 18 commits ahead and one behind current main; its three-dot diff contained documentation paths only | Avoids inheriting branch drift |
| 3 Nov 2026 | Identified owner evidence questions | Repository visibility, PDF cover status, RD-1 actual hardware, distribution, VM4 and WSS remain open | OPEN_ITEMS.md |

| 3 Nov 2026 | Branch drift and scope check | GitHub compare: docs/prd-v1.2-suite was 18 ahead / 1 behind main; three-dot diff contained only 18 docs paths | PR should be opened from this documentation branch after final checks |\n\n## 4. Append-only progress entry template

Copy this row for each meaningful engineering/test event. Do not record secrets, student identifiers, private endpoints, screen contents or bearer values.

| Date/time UTC | Branch + commit | Gate/task ID | Change or test performed | Environment / device | Result | Evidence link / artifact hash | Open issue / next action |
|---|---|---|---|---|---|---|---|
| YYYY-MM-DD HH:MM | branch@sha | MD-... | Exact change/test | Device/API/ABI or server class only | PASS / FAIL / BLOCKED / NOT RUN | Link or SHA-256 | Owner/action/date |

## 5. Gate evidence rules

- A document existing is not proof that its implementation is complete.
- A successful Gradle build or debug APK is not proof of a real Windows NLA/RDP session.
- Emulator-only testing does not satisfy the physical RD-1 performance gate.
- A TCP/3389 reachability check is not proof that RDP negotiation or Windows NLA succeeds.
- A test is PASS only when its procedure, commit, environment, result and evidence artifact are recorded.
- No G0–G5 gate is declared fully passed in this status report unless every PRD exit criterion has evidence.


## 6. Owner feature scope decisions recorded — 3 Nov 2026

- Added `docs/FEATURE_INVENTORY.md` with 22 independently scoped FPs: 18 R1 Mobile Cloud PC FPs and 4 R2 Dock FPs.
- **OWNER CONFIRMED:** Windows guest credentials are entered separately for every session; FP-05 follows the prompt-per-session pilot path. Broker-issued ephemeral guest passwords and directory-based login are not selected.
- **OWNER CONFIRMED:** Both R2 Dock desktop delivery options remain open for comparison: phone-side RDP render/H.264-to-Pi and Pi-native FreeRDP. FP-20 must not select an architecture before the comparative spike.
- **OWNER CONFIRMED:** No additional scope. Clipboard, file transfer and administrative console remain excluded.
- **OWNER-DIRECTED PROPOSED CHANGE:** R1 active sessions should continue when the user backgrounds the app/switches applications. This conflicts with PRD v1.2 foreground-only/5-second teardown language. It is recorded as a proposed successor-PRD amendment, not a normative v1.2 change.
- **OPEN:** Screen-lock/display-off behavior remains a separate decision. Background continuation does not imply persistence after force-stop or OS process termination.
- Updated DECISIONS.md, OPEN_ITEMS.md, TASKS.md and PRD_V1_3_CHANGES.md to maintain traceability. No Android source, Gradle, CI, submodule, broker deployment, Supabase data or VM state was changed.


## 7. Owner-directed account model update — 4 Nov 2026

- **OWNER CONFIRMED:** No student self-registration. The College Administrator provisions each student account and sends the activation invitation; student activation/password creation is not signup.
- **OWNER CONFIRMED:** The prototype has two application roles: one College Administrator account and student accounts. The initial College Administrator is bootstrapped out-of-band by the Project Owner.
- **OWNER CONFIRMED:** The College Administrator workspace manages roster-backed student provisioning, approved student-to-VM assignments, approved host/VM lifecycle operations and active student connections.
- **SECURITY BOUNDARY:** All privileged actions execute through trusted server-side APIs. No Supabase service-role/secret key, direct libvirt connection, SSH credential or arbitrary shell command is exposed to Android.
- Added docs/ADMIN_ACCOUNT_MODEL.md and FP-23..FP-28 in docs/FEATURE_INVENTORY.md. The admin scope conflicts with v1.2's explicit admin-console exclusion; proposed successor-PRD wording is maintained in PRD_V1_3_CHANGES.md. The v1.2 PDF is unchanged.
- The current branch contains documentation changes only. No Android source, Supabase settings, database schema, server or VM was changed. Invitation delivery, invite callback support, admin role/RLS, and admin control-plane APIs remain NOT IMPLEMENTED / BLOCKED pending the listed tasks and tests.

