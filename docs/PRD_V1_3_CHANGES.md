# MobiDesk PRD v1.2 — Proposed v1.3 Change Set

**Date:** 3 November 2026  
**Source of truth:** MobiDesk_PRD_v1.2.pdf, current main branch.  
**Instruction:** This is a proposal only. PRD v1.2 and its PDF were not modified. No proposed text becomes normative until the Project Owner approves a new PRD revision.

## 1. Summary

| Item | v1.2 observation | Proposed v1.3 action | Owner approval |
|---|---|---|---|
| Stack/repository | §6.1 says native Compose; current repo evidence confirms native Compose and repo name | No requirement change. Update audit provenance to separate the prior Flutter audit from this repo. | Informational; G0 passed |
| minSdk | §8.1 says API 29 or newer; NFR-COMP-01 says preserve verified repo baseline | Clarify exact current baseline: min 29, compile/target 37/37; retain unless separate approval | No change to current API 29 |
| FR-SEC-01 to 03 | Security section has controls but numbering begins with FR-SEC-04 only | Add three named requirements and trace them to §9 items 1–3 | Required |
| RD-1 | §8.1 nominates Pixel 6 / Android 16 / API 36 / arm64 | Confirm actual physical device; update only if it differs | Required before G4 |
| Schedule | §14 targets broker on 8 Nov, native RDP on 10 Nov, device acceptance on 12 Nov, release on 13 Nov | Reforecast sequential one-person work | Required |
| Approval status | PDF cover says owner-review draft; Project Owner now says v1.2 is final | Change status label in a future approved revision | Required for documentary consistency |
| PDF / text source | Current main has the full PDF; the prior docs branch contains a non-faithful docs/PRD.md. This suite adds a navigational index, not a full transcription. | PDF remains canonical; approve full text export only if needed | Required |
| VM4 | §10/§15 excludes unverified VM4 | No change; retain exclusion | Confirm before any later addition |
| Pinned FreeRDP integration | §6.6–6.8 specify desired stream/render/cert behavior; pinned upstream Java path has unresolved transport and cert integration semantics | No relaxation of security requirements; document specific integration spikes and stop criteria | Required for G3 |

## 2. Proposed diff — FR-SEC numbering repair

### Before (v1.2 structure)
~~~text
FR-SEC-04: [root / unlocked-device requirement]
...
Section 9 contains mandatory security controls, but no FR-SEC-01, FR-SEC-02 or FR-SEC-03 definitions are present.
~~~

### After (proposed)
~~~text
FR-SEC-01 — Client credential boundary
The Android client MUST authenticate to the broker using only the current Supabase user access token and MUST NOT contain or transmit a Supabase service-role key.

FR-SEC-02 — Broker token verification
The broker MUST validate the Supabase JWT signature, issuer, audience, expiry, subject and session_id. It MUST NOT authorize from email, client metadata or caller-supplied user/role fields.

FR-SEC-03 — Active session authorization
The broker MUST verify the JWT session_id is active before issuing a grant and MUST re-check active session/assignment state at least every 15 seconds during a relay. A revoked session or changed assignment MUST close the relay within 30 seconds.

FR-SEC-04 — Device integrity
Retain the existing R1 rooted/unlocked-bootloader requirement without weakening its UNKNOWN state handling.
~~~

**Cross-reference decision:** v1.2 already has FR-SES-03 for relay/session lifecycle. Keep that lifecycle requirement but add a reference to FR-SEC-03 rather than deleting or silently renumbering an existing ID. Finalize whether FR-SEC-03 and FR-SES-03 are separate controls (security authorization vs session behavior) or one requirement with two traceability references.

## 3. Proposed diff — minSdk and compatibility baseline

### Before (v1.2 §8.1)
~~~text
Minimum Android SDK: API 29 or newer.
NFR-COMP-01: preserve the repository's verified min/compile/target SDK and ABI baseline.
~~~

### Verified current baseline
- [VERIFIED: build.gradle and app/build.gradle at main@5979320b05e1e6c1d872d82dcc17e65b0901e7e5] minSdk 29, compileSdk 37, targetSdk 37.
- [VERIFIED: build.gradle and release.properties at the same commit] Required native ABIs arm64-v8a and x86_64.
- [VERIFIED: app/build.gradle at the same commit] Java/Kotlin target 11.

### After (proposed clarification)
~~~text
The current approved compatibility baseline is minSdk 29, compileSdk 37, targetSdk 37, with arm64-v8a and x86_64 native ABIs. NFR-COMP-01 preserves this baseline. Any change requires an explicit compatibility decision, affected-device analysis and regression evidence. The minSdk value MUST NOT be raised merely to satisfy a Google Play target API policy.
~~~

**Disposition:** No minimum change is required. API 29 is the selected value. The compile/target values and ABIs are separately preserved. The minSdk and targetSdk must not be conflated.

## 4. Proposed diff — RD-1 device identification

### Before (v1.2 §8.1)
~~~text
RD-1: Google Pixel 6, Android 16 / API 36, arm64.
~~~

### Current evidence
- [VERIFIED: PRD v1.2 §8.1] The nominated reference device is Pixel 6 / Android 16 / API 36 / arm64.
- [UNKNOWN: 3 Nov 2026 repository and device audit] Physical availability/ownership of that exact device is not evidenced. An emulator is not a substitute for physical-device acceptance.

### After (proposed; fill only when physically verified)
~~~text
RD-1: <actual model and variant>, Android <release> / API <level>, ABI <arm64-v8a or approved target ABI>, build fingerprint <recorded in private test evidence>.
The device identity MUST be verified from the physical device before G4. If the verified device differs from Pixel 6, the Project Owner MUST approve a PRD update before performance results are considered release evidence.
~~~

Do not replace Pixel 6 with a guessed model. The device/build fingerprint belongs in test evidence, not a public documentation file if it could identify a private device.

## 5. Proposed diff — §14 schedule reforecast

### Before (v1.2 §14 targets)
| Milestone | Original target |
|---|---|
| PRD approval and architecture | 4 Nov 2026 |
| Repository/commit freeze | 4 Nov 2026 |
| Windows guest accounts | 5 Nov 2026 |
| Active session/status design | 6 Nov 2026 |
| Stable WSS route | 6 Nov 2026 |
| Broker API/authorization implementation | 8 Nov 2026 |
| Native FreeRDP integration | 10 Nov 2026 |
| Physical R1 acceptance | 12 Nov 2026 |
| Signing/pilot release | 13 Nov 2026 |

### After (proposed planning dates, not a delivery promise)
| Milestone | Proposed forecast | Reason |
|---|---|---|
| G0 repository/stack/SDK/gitlink audit | 4 Nov 2026 | Technical G0 check passed on 3 Nov; visibility and status-label decisions still need owner closure |
| G1 contract/threat/UX/test design freeze | 11 Nov 2026 | Approximately 24–32 focused hours for one owner |
| Guest account / Windows lockout approval | 6 Nov 2026 | Lab IT policy dependency |
| Stable broker HTTPS/WSS route | 12 Nov 2026 | Owner DNS/TLS/network and WSS tests |
| G2 broker ready | 25 Nov 2026 | Approximately 48–64 focused hours; session lookup, unique assignment, grant, relay, rate and revocation |
| G3 native FreeRDP integration | 18–31 Dec 2026 | Approximately 80–120 focused hours; transport, local bridge, surface, NLA, certificate and RDPEDISP spikes unproven |
| G4 physical R1 acceptance | 8–15 Jan 2027 | Requires G2/G3 plus RD-1 confirmation and 20-attempt/20-cycle performance/security evidence |
| G5 signing and operations ready | 20–28 Jan 2027 | Requires G4, distribution/key choice, backup/restore, retention and runbooks |

The 10 Nov native integration target is at risk: from the 3 Nov planning snapshot it is seven calendar days away (five weekdays assuming a Monday–Friday schedule). The current native integration estimate is 80–120 focused hours. This reforecast assumes one engineer/Project Owner with AI assistance, approximately six focused engineering hours per weekday and no major upstream transport redesign. Any failed FreeRDP raw-stream or bridge spike extends the forecast.

## 6. Proposed diff — approval status and canonical PRD source

### Before (PDF cover, p. 2)
~~~text
Status: Owner review draft — final scope and architecture decisions recorded; owner approval still required.
~~~

### Current owner direction (3 Nov 2026)
The Project Owner states: “PRD v1.2 is the final decided prd.”

### After (proposed)
~~~text
Status: Approved baseline — MobiDesk Project Owner confirmed PRD v1.2 as the normative product baseline on 3 November 2026. Changes require a separately approved PRD revision.
~~~

The printed PDF status must be updated only in a future approved PRD revision. This task does not rewrite the PDF.

### Canonical-file clarification
- [VERIFIED: main tree at main@5979320...] docs/MobiDesk_PRD_v1.2.pdf exists on main and contains the 23-page PRD.
- [VERIFIED: main@5979320] The canonical PDF exists; docs/PRD.md is not present on main.
- [VERIFIED: prior branch docs/mobidesk-prd-v1.2] its docs/PRD.md text projection is incomplete.
- This suite adds a short navigational index at docs/PRD.md; it is not a full-text projection. The PDF remains canonical and unchanged.

## 7. Conflicts discovered while preparing the engineering suite

| Finding | Evidence | Required treatment |
|---|---|---|
| Previous audit says Flutter + Kotlin; current PRD says Compose | [VERIFIED: current main source tree, settings.gradle and MainActivity.kt] native Kotlin/Compose matches PRD §6.1. The prior Flutter audit's source repository is UNKNOWN. | No PRD stack edit. Retain current repo audit, record previous audit provenance as unknown. |
| README says repository is private; GitHub metadata reports public | [VERIFIED: GitHub repo metadata and README at main@5979320...] | Owner confirms visibility. Do not put operational secrets or endpoints into repository docs. |
| Pinned FreeRDP Java façade has no obvious connected-FD method in inspected method list | [VERIFIED: pinned FreeRDP LibFreeRDP.java at v3.32.0 gitlink] | G3 raw-stream/bridge spike. No weakening of local peer-auth rule. |
| Pinned FreeRDP argument builder adds /cert:ignore and upstream Activity supports an accept-all preference | [VERIFIED: pinned FreeRDP LibFreeRDP.java and SessionActivity.java] | Prove exact semantics and force fail-closed pin path; no user override. If it cannot be proven, no RDP release. |
| Pinned FreeRDP argument builder may place bookmark password into a /p: string argument | [VERIFIED: pinned FreeRDP LibFreeRDP.java] | Do not set persistent BookmarkBase credentials. Use callback with proven buffer lifecycle or redesign adapter. |
| Upstream SessionActivity resolves conRef/URI/hostname and frees session on Activity destroy | [VERIFIED: pinned FreeRDP SessionActivity.java] | Do not pass user target selectors; prove MobiDesk session/renderer lifecycle separately. |
| Pinned Android SessionView is bitmap/GDI-backed View; DISP channel implementation exists | [VERIFIED: pinned FreeRDP SessionView.java, android_disp.c, CMakeLists.txt] | Verify render-target rebind, resize and actual RDPEDISP negotiation; do not claim seamless hot-plug. |
| FreeRDP upstream setConnectionInfo adds clipboard; other redirection flags can be enabled | [VERIFIED: pinned FreeRDP LibFreeRDP.java] | Prove R1 excluded channels are off in MobiDesk argument config and release static scan. |
| Current repo release build has no explicit signing config | [VERIFIED: app/build.gradle at main@5979320...] | Keep G5 blocked pending channel and key custody; do not claim production artifact. |
| PRD §9 controls contain FR-SEC-01..03 numbering gap | [VERIFIED: PRD v1.2 PDF] | Apply proposed ID patch only after owner approval. Current test suite traces §9 items using section locators, not invented FR definitions. |

These are implementation or documentation conflicts, not permission to relax the PRD security contract.

## 8. Other items explicitly unchanged

- VM4 remains unverified and excluded. No placeholder endpoint or silent reassignment is proposed.
- Windows guest credentials remain separate from Supabase and are not broker-issued.
- Certificate pinning, active session revocation, authenticated LocalBridge and R2 dock identity remain required.
- Dock remains R2; its architecture must be selected by measured phone-transcode versus Pi-native FreeRDP experiments.
- No new public VM status, power-control, admin inventory or student assignment mutation requirement is proposed.
- No source, Gradle, CI or Supabase migration is proposed in this patch document.

## 9. Approval checklist

- [ ] Project Owner approves FR-SEC-01..03 wording and cross-reference to FR-SES-03.
- [ ] Project Owner confirms API29 minSdk and 37/37 compile/target baseline.
- [ ] RD-1 physical device is confirmed; if different, approved PRD change is created.
- [ ] Project Owner accepts the §14 forecast or supplies a replacement plan.
- [ ] Project Owner approves the PRD status-label/canonical PDF clarification.
- [ ] Project Owner confirms repository visibility.
- [ ] Lab IT approves guest-account, VM lifecycle and certificate operations.
- [ ] Project Owner approves final v1.3 revision before it supersedes v1.2.

Until all applicable approvals are recorded, PRD v1.2 remains unchanged and authoritative.


## 8. Owner-directed change — R1 background session continuity

**Owner direction recorded:** 3 November 2026.  
**Affected feature:** FP-14 — RDP Session Lifecycle, Background Continuity and Cleanup.  
**Current normative conflict:** PRD v1.2 §5.3 / §6.8 and current DESIGN.md §7 specify foreground-only R1 behavior and teardown after five seconds of true backgrounding or screen lock.

### Proposed successor-PRD wording

> When a student backgrounds MobiDesk or switches to another application during an authenticated RDP session, MobiDesk SHOULD keep the active session connected while the session remains authorized, the device/network permit it, and the user has not explicitly disconnected. The session controller and transport MUST NOT depend on the RDP Activity remaining visible. The app MUST provide a clear, ongoing user-visible session indication and a direct return/disconnect action. Revocation, assignment change, network loss, explicit disconnect, force-stop or operating-system process termination MUST result in bounded cleanup and safe recovery; session survival after force-stop or process termination is not guaranteed.

The exact use of MUST/SHOULD, background duration, notification behavior and lock-screen behavior must be finalized in the successor PRD.

### Required engineering proof before implementation is considered ready

- Start the long-running session component only from the visible, user-initiated session flow.
- Identify a foreground-service type and permission that accurately represents this use case; do not label an RDP socket as `dataSync`, `connectedDevice` or `specialUse` without platform/policy validation.
- Provide the required ongoing notification with return-to-session and explicit disconnect actions if a foreground service is selected.
- Validate service declaration, type-specific permission, start restrictions and notification behavior on the repository's target SDK and the nominated reference device.
- Prove the native FreeRDP context, bridge, WSS transport, input state and cleanup remain correct while the Activity is not visible.
- Test app task switching, return, notification action, network interruption, OS resource pressure, process death and explicit user stop.
- Measure foreground/background battery, memory, network use, thermal behavior and session reliability.
- Decide screen-lock/display-off behavior separately; background continuation does not automatically approve displaying or maintaining a session while the device is locked.

### Platform references

- Android foreground service types and type-specific declarations: https://developer.android.com/develop/background-work/services/fgs/service-types
- Android restrictions on starting foreground services from background: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Android foreground-service overview and user-visible notification expectation: https://developer.android.com/develop/background-work/services/fgs

### Status

**OWNER-DIRECTED PROPOSAL — NOT YET NORMATIVE.** PRD v1.2 remains unchanged and continues to specify foreground-only behavior until Project Owner formally approves a successor PRD revision. DEC-27 and DEC-28 track the direction and the unresolved lock-screen question.
