# MobiDesk — R1 Verification and Acceptance Test Plan

**Version:** 1.0 · **Date:** 3 November 2026  
**Normative source:** MobiDesk PRD v1.2.  
**Rule:** A build, code review or plan is not a passing test. Each PASS requires recorded procedure, tested commit, environment, result, evidence link/hash and reviewer.

## 1. Test strategy and levels

| Level | Name | Intended coverage | Evidence |
|---|---|---|---|
| UNIT | Unit | Domain state machine, input transforms, expiry/staleness, error mapping, queue and cleanup logic | JUnit result and test report |
| CONTRACT | Contract | OpenAPI/schema, JWT/grant/assignment rules, error catalogue and WSS handshake behavior | Broker contract suite output |
| INTEGRATION | Integration | Supabase Auth/RLS, broker session checks, allowlist, private relay, FreeRDP callback and certificate policy | Redacted service logs and test harness report |
| DEVICE | Physical device | RD-1 Android behavior, real Windows NLA/RDP, touch/keyboard, rotation, FLAG_SECURE and performance | Device build fingerprint, attempt ledger, capture/measurement report |
| STATIC | Static/release | APK contents, ABI, signing, cleartext, trust-all, secret strings, exported Activity, dependencies/SBOM | Scan output and artifact hashes |

Test users, assignments, guest credentials and VM workload must be synthetic or owner-approved controlled test assets. Do not use student production passwords or include real student data, hostnames, private IPs or guest endpoint details in evidence committed to Git.

### Test result values
- **PASS:** all assertions pass and evidence is linked.
- **FAIL:** one or more assertions fail.
- **BLOCKED:** prerequisite, device, endpoint, approval or proof is unavailable.
- **NOT RUN:** not yet attempted.
- **NOT APPLICABLE:** requirement explicitly deferred (for example R2 dock on R1).

No requirement is PASS just because source code or a document exists.

## 2. Requirement traceability matrix

### 2.1 Functional requirements

| PRD requirement | Test IDs | Level | Acceptance summary | Gate |
|---|---|---|---|---|
| FR-AUTH-01 | T-AUTH-01, T-AUTH-02, T-AUTH-03 | INTEGRATION, DEVICE | Existing sign-in, session restore, sign-out and recovery regressions pass; expired session maps AUTH-01; no self-registration | G1, G4 |
| FR-AUTH-02 | T-AUTH-04, T-AUTH-05 | CONTRACT, INTEGRATION | Anonymous denied; user A cannot read user B profile or assignment; all queries use caller identity/RLS | G1, G2 |
| FR-VM-01 | T-VM-01, T-VM-02, T-VM-03 | UNIT, CONTRACT | Exactly one active assignment accepted; zero/duplicate/inactive/unmapped fail closed; never select first row | G1, G2 |
| FR-VM-02 | T-VM-04, T-VM-05 | CONTRACT, INTEGRATION | Grant is bound to server-derived assignment and only owner-approved endpoint mapping; client has no target field | G2 |
| FR-VM-03 | T-VM-06, T-VM-07 | CONTRACT, INTEGRATION | Altered subject/target/VM fields do not change routing; cross-user and unallowlisted requests are denied | G2 |
| FR-VM-04 | T-VM-08, T-VM-09 | UNIT, CONTRACT, INTEGRATION | Lifecycle and RDP transport are distinct; probe budget 1500ms; timeout becomes UNKNOWN | G2, G4 |
| FR-VM-05 | T-VM-10, T-VM-11 | UNIT, DEVICE | Sample is stale at 15s; Connect requires fresh status and server-side grant-time recheck | G2, G4 |
| FR-SES-01 | T-SES-01, T-SES-02 | CONTRACT, INTEGRATION | Empty-body grant request uses Supabase JWT only and no destination/guest credential data | G2 |
| FR-SES-02 | T-SES-03, T-SES-04, T-SES-05 | UNIT, CONTRACT, INTEGRATION | Grant has >=256 bits entropy, <=60s TTL, bound user/session/assignment, hashed at rest and single-use | G2 |
| FR-SES-03 | T-SES-06, T-SES-07 | INTEGRATION | Active session and assignment rechecked <=15s; active relay closes <=30s after revocation/change | G2, G4 |
| FR-SES-04 | T-SES-08, T-SES-09 | UNIT, INTEGRATION | One active session per student; pilot cap enforced; all cancel/failure paths release slot | G2, G4 |
| FR-GUEST-01 | T-GUEST-01, T-GUEST-02, T-GUEST-03 | DEVICE, STATIC | Separate Windows credential prompt before grant; guest password absent from broker request, Intent, URI, saved state and disk | G3, G4 |
| FR-GUEST-02 | T-GUEST-04, T-GUEST-05 | UNIT, DEVICE, STATIC | Mutable credential buffer and native copies cleared on every terminal path; no logging/crash/analytics exposure | G3, G4 |
| FR-RDP-01 | T-RDP-01, T-RDP-02 | INTEGRATION, DEVICE | FreeRDP pinned v3.32.0 launches a real Windows NLA session through approved secure relay | G3, G4 |
| FR-RDP-02 | T-RDP-03, T-RDP-04, T-RDP-05 | INTEGRATION, DEVICE, STATIC | Grant-bound certificate pin/CA accepted only when trusted; mismatch/expiry/missing pin hard-fails; no accept-all | G2, G3, G4 |
| FR-RDP-03 | T-RDP-06, T-RDP-07 | UNIT, DEVICE | Native RdpSessionActivity owns the render boundary; attach/detach and window lifecycle do not leak native resources | G3, G4 |
| FR-RDP-04 | T-RDP-08, T-RDP-09, T-RDP-10 | INTEGRATION, DEVICE | Pinned-source spike verifies MS-RDPEDISP, guest resize, rotation and renderer rebind or approved reconnect fallback | G3, G4 |
| FR-RDP-05 | T-BRIDGE-01 through T-BRIDGE-07 | UNIT, INTEGRATION, DEVICE | Protected LocalBridge, byte fidelity, bounds, backpressure, EOF/half-close, cancel and hijack resistance | G2, G3 |
| FR-INPUT-01 | T-INPUT-01, T-INPUT-02 | DEVICE | Trackpad/direct touch, pointer, click/drag, scroll and right-click work on RD-1 | G4 |
| FR-INPUT-02 | T-INPUT-03, T-INPUT-04 | DEVICE | Soft/hardware keyboard, Unicode, scan codes, modifiers and Ctrl+Alt+Delete work | G4 |
| FR-INPUT-03 | T-INPUT-05 | DEVICE | FLAG_SECURE blocks standard screenshot and recent-task preview capture | G4, G5 |
| FR-LIFE-01 | T-LIFE-01, T-LIFE-02 | DEVICE, INTEGRATION | Connect/connected/disconnecting/disconnected/failed/expired lifecycle states are correct; no orphan session | G3, G4 |
| FR-LIFE-02 | T-LIFE-03, T-LIFE-04 | DEVICE, INTEGRATION | True background/screen lock for 5s tears down session; rotation/IME does not falsely background it | G3, G4 |
| FR-LIFE-03 | T-LIFE-05, T-LIFE-06 | DEVICE, INTEGRATION | Network loss and process/Activity recreation clean local and broker state; retry obtains new grant | G3, G4 |
| FR-LIFE-04 | T-LIFE-07 | UNIT, INTEGRATION | Cleanup is idempotent and runs after every failure/cancel/timeout/revocation path | G3, G4 |
| FR-OBS-01 | T-OBS-01, T-OBS-02 | UNIT, STATIC, INTEGRATION | Required stage events and timings exist; forbidden data is absent from telemetry/logs | G4, G5 |
| FR-SEC-01 | T-SEC-01, T-SEC-02 | CONTRACT, INTEGRATION | **PRD numbering gap:** no FR-SEC-01 definition. Candidate tests validate PRD §9 items 1–2 (JWT-only client auth and JWT claim verification), but no new FR ID is assigned here. | G1, G2 |
| FR-SEC-02 | T-SEC-03, T-SEC-04 | CONTRACT, INTEGRATION | **PRD numbering gap:** no FR-SEC-02 definition. Candidate tests validate PRD §9 items 3–5 (session revocation, grant constraints and private relay boundary), pending owner-approved ID repair. | G1, G2 |
| FR-SEC-03 | T-SEC-05, T-SEC-06 | INTEGRATION, DEVICE, STATIC | **PRD numbering gap:** no FR-SEC-03 definition. Candidate tests validate PRD §9 items 6–8 (bridge, certificate, secrets) pending owner-approved ID repair. | G1, G2, G3 |
| FR-SEC-04 | T-SEC-07, T-SEC-08 | DEVICE, STATIC | Root/unlocked-bootloader high-confidence signal blocks session; unknown integrity signal is not CLEAN | G3, G4 |
| FR-REL-01 | T-REL-01, T-REL-02 | STATIC, RELEASE | Reproducible owner-signed release artifact; signer, source commit, submodule SHA and SHA-256 recorded | G5 |
| FR-REL-02 | T-REL-03, T-REL-04, T-REL-05 | STATIC, RELEASE | Release scan rejects mocks/demo, debug endpoints, cleartext/trust-all, secrets, accidental exported session Activity and excluded channels | G5 |
| FR-DOCK-01 | T-DOCK-01, T-DOCK-02 | DEVICE / R2 | R2-only dock rendering/control; no R1 dock stream | R2 only |
| FR-DOCK-02 | T-DOCK-03, T-DOCK-04, T-DOCK-05 | INTEGRATION / R2 | R2 authenticated dock pairing with explicit confirmation and key-backed challenge-response; spoofed AOA strings rejected | R2 only |

### 2.2 Non-functional requirements

| PRD requirement | Test ID | Level | Acceptance | Gate |
|---|---|---|---|---|
| NFR-PERF-01 | T-PERF-01 | DEVICE | At least 20 controlled attempts; p90 from Windows credential submit to first frame <=20s | G4 |
| NFR-PERF-02 | T-PERF-02 | DEVICE | 120-second 1280x720 motion workload; average presented FPS >=15; report p50/p90 frame interval and dropped frames | G4 |
| NFR-PERF-03 | T-PERF-03 | DEVICE | Under RTT <=40ms, scripted input-to-visible-response p90 <=250ms | G4 |
| NFR-PERF-04 | T-PERF-04 | DEVICE | >=19 of 20 controlled attempts succeed on first try; all failures classified | G4 |
| NFR-PERF-05 | T-PERF-05 | DEVICE, INTEGRATION | 20 consecutive connect/disconnect cycles; zero native crash, leaked socket, relay slot, queue or worker | G4 |
| NFR-PERF-06 | T-PERF-06 | DEVICE, INTEGRATION | With a <=10s Wi-Fi interruption, session recovers within <=15s with a new grant if VM remains ready; otherwise safe failure | G4 |
| NFR-SEC-01 | T-SEC-09 | CONTRACT, INTEGRATION | Zero unauthorized successes across all negative authorization/session/endpoint tests | G2, G4, G5 |
| NFR-PRIV-01 | T-PRIV-01, T-PRIV-02 | STATIC, INTEGRATION | Zero credentials, grant/JWT, student identifiers, private endpoint or screen content in logs/telemetry | G3, G5 |
| NFR-COMP-01 | T-COMP-01, T-COMP-02 | STATIC, BUILD | Preserve minSdk29 / compileSdk37 / targetSdk37 / arm64-v8a + x86_64 unless approved change recorded | G0, G5 |

### 2.3 PRD §9 security controls

The PRD contains 14 numbered security controls. These are section locators, not invented FR-SEC identifiers.

| PRD §9 item | Security control to prove | Test IDs | Gate |
|---:|---|---|---|
| 1 | Client uses only current user access JWT; no service-role key in APK | T-SEC-01 | G2, G5 |
| 2 | Verify JWT issuer/audience/expiry/signature/subject/session_id; never trust email/client metadata for role | T-SEC-02 | G2 |
| 3 | Active session_id checked at grant; active relay checked every <=15s; close <=30s after revoke | T-SEC-03 | G2 |
| 4 | Grant entropy >=256 bits, hash at rest, <=60s, single-use and binding; 5/min/user | T-SEC-04 | G2 |
| 5 | Broker loopback/private bind; WSS authenticated; broker only to private TCP/3389; no public RDP/arbitrary forwarding | T-SEC-05 | G2, G5 |
| 6 | Protected LocalBridge; socketpair preferred, Unix peer UID check or authenticated one-shot TCP | T-SEC-06 | G3 |
| 7 | Certificate pin/CA validation fail-closed | T-RDP-03 to T-RDP-05 | G2, G3 |
| 8 | No secret in disk, Logcat, crash, analytics, URL or persistent client storage | T-PRIV-01, T-GUEST-04/05 | G3, G5 |
| 9 | FLAG_SECURE on sensitive session UI | T-INPUT-05 | G4 |
| 10 | Root/unlocked bootloader policy; UNKNOWN is not CLEAN | T-SEC-07/08 | G4 |
| 11 | TLS 1.2+, release cleartext off and trust-all off | T-SEC-10 | G2, G5 |
| 12 | Audit grant/relay/revocation events with opaque IDs/reason codes only | T-OBS-01/02 | G2, G5 |
| 13 | Lab IT approves Windows lockout/failed-logon monitoring; bounded retry and no username enumeration | T-GUEST-06, T-SEC-11 | G3, G5 |
| 14 | Review pinned FreeRDP against applicable security advisories; no unreviewed high/critical issue at release | T-SEC-12 | G5 |

### 2.4 PRD §12 error-code tests

Each test injects the fault and asserts the exact error code and exact English user-facing string. Telugu translations remain draft until reviewed by a native speaker.

| Test ID | PRD error code | Expected action | Retry expectation |
|---|---|---|---|
| E-01 | AUTH-01 | Sign in again | New Auth session |
| E-02 | ASSIGN-01 | Contact lab administrator | No automatic retry |
| E-03 | ASSIGN-02 | Refresh status | Bounded retry |
| E-04 | ASSIGN-03 | Administrator review | No retry |
| E-05 | VM-01 | Show powered-off state; no power button | Wait for operator |
| E-06 | VM-02 | Show starting/not-ready state | Refresh after backoff |
| E-07 | VM-03 | Show unreachable state | Refresh after backoff |
| E-08 | GRANT-01 | Generic authorization failure | New request after backoff |
| E-09 | GRANT-02 | Grant expired/used | Request new grant |
| E-10 | TUNNEL-01 | Secure connection unavailable | Bounded retry; fresh grant |
| E-11 | RDP-CERT-01 | Hard block certificate mismatch | No user override |
| E-12 | RDP-AUTH-01 | Re-prompt separate Windows credentials | Bounded retry, new grant |
| E-13 | RDP-01 | RDP connection did not complete | Recheck status then retry |
| E-14 | SESSION-01 | Connection lost / safe reconnect | New grant |
| E-15 | SESSION-02 | Admin/policy ended session | Wait for reauthorization |
| E-16 | SESSION-03 | Cleanup not verified; block new session | Only after verified cleanup |

## 3. Mandatory negative and security tests

| Test IDs | Negative scenario | Required result |
|---|---|---|
| T-NEG-01 | Anonymous, malformed, wrong-audience or expired JWT | AUTH-01 / deny |
| T-NEG-02 | Client sends vm_id, hostname, IP, port or another student's ID | Reject unknown target fields; no route change |
| T-NEG-03 | No assignment, duplicate assignment, inactive mapping or VM4 selector | Fail closed; never select first row or substitute VM |
| T-NEG-04 | Student A requests Student B's profile/assignment | RLS denies; no data returned |
| T-NEG-05 | Revoke Auth session before grant | Grant denied |
| T-NEG-06 | Revoke Auth session/change assignment during relay | Relay closes within 30s |
| T-NEG-07 | Replay, expire, race or use grant from another session | Single-use binding holds; replay denied |
| T-NEG-08 | More than 5 grants/min/user or concurrent-session cap | Rate/concurrency response; no slot leak |
| T-NEG-09 | Broker allowlist contains unknown endpoint or arbitrary port | Broker startup or dial fails closed |
| T-NEG-10 | Public internet scans for TCP/3389 | No guest RDP port reachable publicly |
| T-NEG-11 | Missing, mismatched, expired or changed RDP certificate | RDP-CERT-01; no accept-any path |
| T-NEG-12 | Local app attempts port pre-bind, scan, replay and second bridge connection | Unauthorized local connection rejected |
| T-NEG-13 | WebSocket text frame, invalid subprotocol, oversize frame or slow reader | Protocol rejected; queues remain bounded |
| T-NEG-14 | Broker stop/restart, network loss, EOF or half-close | Both directions and session slot cleaned |
| T-NEG-15 | Bad Windows guest credentials or repeated username guesses | RDP-AUTH-01; no username enumeration; lockout policy enforced |
| T-NEG-16 | Rooted/unlocked device or unknown integrity result | Block per PRD; UNKNOWN never becomes CLEAN |
| T-NEG-17 | Malformed/oversized RDP graphics/channel payload | Client remains bounded; native failure closes session safely |
| T-NEG-18 | Known applicable high/critical FreeRDP advisory | Release blocked until fixed/accepted by Owner through formal risk decision |
| T-NEG-19 | Logs, traces, crash reports and metrics | No token/password/student ID/endpoint/screen data |
| T-NEG-20 | FLAG_SECURE | Screenshot and task-switcher preview do not reveal the remote desktop |

## 4. LocalBridge test procedure

1. Use a synthetic test session and unique fake handshake token.
2. Verify the preferred socketpair/descriptor or app-private Unix-domain socket path, including peer UID verification where supported.
3. If loopback TCP is proposed, start a malicious same-device test app before the legitimate FreeRDP client and attempt to bind/accept the expected port.
4. Attempt port scanning, early connect, duplicate connect, token replay, invalid token, delayed handshake and oversized preface.
5. Verify exactly one authenticated connection is accepted, the listener closes immediately after accept and the secret is cleared after authentication/teardown.
6. Use deterministic byte patterns in both directions. Split at arbitrary byte offsets and coalesce across different WebSocket messages; verify byte-for-byte equality and order.
7. Force queue high-water, slow consumer, EOF, half-close, Wi-Fi loss, cancellation and process/activity destruction.
8. Verify bounded memory, bounded teardown time, no orphan reader/writer/native worker, no reusable token and no open listener after failure.

**Pass rule:** if the pinned FreeRDP API cannot participate in protected peer authentication, LocalBridge fails the security requirement. A random localhost port alone is an automatic FAIL.

## 5. RD-1 physical device procedure

**PRD nomination:** Pixel 6, Android 16 / API 36, arm64.  
**Availability:** [UNKNOWN as of 3 Nov 2026] Project Owner must verify that this exact physical device is available. If a different phone will be the test device, update the PRD through an approved change before G4; do not silently substitute.

Record:
- device model and product identifier;
- Android release/API/build fingerprint;
- ABI and available RAM;
- app version, source commit and FreeRDP gitlink;
- power/battery and thermal condition;
- Wi-Fi network RTT/loss test values;
- controlled guest build and desktop resolution;
- certificate pin policy version;
- test tool versions and operator.

### Device run sequence
1. Install the exact signed test artifact and verify package signature/SHA-256.
2. Sign in using a synthetic provisioned test account.
3. Validate the correct single assignment and fresh readiness status.
4. Complete Windows credential prompt with a synthetic lab account.
5. Complete 20 controlled connect attempts. Record each stage: submit credentials, grant issue, WSS upgrade, local bridge, RDP negotiation, NLA result and first frame.
6. Run the 120-second 1280x720 motion workload during each required FPS measurement; record presented frames, average FPS, p50/p90 frame interval and drops.
7. Under controlled RTT <=40ms, run scripted input actions and measure input-to-visible-response p90.
8. Perform a controlled <=10-second Wi-Fi interruption and record recovery or safe failure within <=15 seconds. A recovery must use a fresh grant.
9. Perform screen rotation, IME open/close, background for <5s and >5s, screen lock, manual disconnect and broker revocation.
10. Run the 20-cycle connect/disconnect cleanup test and inspect relay slot, native context, bridge workers, sockets and credentials.
11. Run screenshot/recents, keyboard, trackpad, touch, modifier and accessibility procedures.
12. Export evidence after removing all secrets and personal data.

Emulator tests are useful for automation but do not replace RD-1 physical-device acceptance.

## 6. Performance measurement method

### NFR-PERF-01 — first-frame latency
- Minimum sample count: 20 complete controlled connection attempts.
- Start timestamp: user submits Windows credentials in the native prompt.
- End timestamp: first valid remote desktop frame is rendered into the RDP view.
- Report all samples, failures, p50 and p90. Do not discard failed attempts from success-rate reporting.
- Pass: p90 <=20 seconds.

### NFR-PERF-02 — frame rate
- Run identical 120-second 1280x720 motion workload on controlled guest.
- Count presented/rendered frames, frame timestamps, p50/p90 frame interval and dropped/late frames.
- Pass: average presented FPS >=15; retain raw timing samples and device/VM/network conditions.

### NFR-PERF-03 — input latency
- Baseline network RTT <=40ms.
- Send a deterministic sequence of pointer/keyboard input that causes a detectable remote visual response.
- Timestamp input dispatch and resulting frame presentation using monotonic time.
- Report p50/p90 and pass when p90 <=250ms.

### NFR-PERF-04 — connection reliability
- At least 20 attempts.
- Pass: at least 19/20 succeed first try; each failed attempt has a PRD error code and stage cause.

### NFR-PERF-05 — cleanup
- Run at least 20 consecutive connect/disconnect cycles.
- Pass: zero native crash, no orphan broker slot, no unclosed socket/listener, no stuck worker, no retained grant or guest credential buffer.

### NFR-PERF-06 — recovery
- Interrupt Wi-Fi for <=10 seconds while connected.
- If VM remains ready, recover within <=15 seconds with a new status check and fresh grant.
- If recovery is impossible, show PRD SESSION-01/02 and clean up within bounds; never reuse old grant.

## 7. APK static and privacy checks

For each release candidate:

- [ ] APK/AAB package ID and version match release record.
- [ ] Verify SHA-256 and signature certificate fingerprint; release key is owner-controlled and not the debug key.
- [ ] Inspect APK for both arm64-v8a and x86_64 required libraries.
- [ ] Confirm cleartext traffic is disabled and TLS validation is enabled.
- [ ] Confirm no trust-all certificate option or accept-all preference is reachable in release.
- [ ] Confirm MobiDesk RdpSessionActivity is not exported.
- [ ] Confirm no sample VM, mock/demo assignment, placeholder endpoint or debug broker hostname remains.
- [ ] Search strings/resources/bytecode for service-role keys, JWT samples, grant values, Windows test passwords, private addresses and hostnames.
- [ ] Search code/config for logs that may print Authorization, token, password, VM metadata or raw RDP bytes.
- [ ] Confirm R1 clipboard, drive, audio, microphone, camera and arbitrary channel forwarding are disabled.
- [ ] Produce SBOM/dependency inventory; review FreeRDP pinned SHA against upstream advisories and applicable CVEs.
- [ ] Confirm source commit and FreeRDP gitlink recorded beside artifact hash.

Use only fake sentinel strings in scanning (for example, a synthetic test credential). Never place actual credentials in the test files or GitHub artifacts.

## 8. Evidence format

Every test record in PROGRESS.md must include:

~~~text
Test ID:
PRD requirement / section:
Result: PASS | FAIL | BLOCKED | NOT RUN | NOT APPLICABLE
Repository branch and commit:
FreeRDP gitlink:
Artifact SHA-256 and signing certificate:
Device model / Android API / ABI (if DEVICE):
Guest / broker environment class (no IP or hostname):
Procedure and command/tool version:
Sample count and raw results:
Expected result:
Actual result:
Logs/evidence link (redacted):
Reviewer:
Date/time UTC:
Open issue / next action:
~~~

Raw test evidence must not contain student identifiers, email, usernames, passwords, grants/JWTs, VM UUIDs, private IPs/hostnames, screenshots of student data or desktop contents.

## 9. Gate exit criteria

| Gate | Mandatory test groups | Exit condition |
|---|---|---|
| G0 | T-COMP-01/02; G0 repository audit | Owner PRD direction, exact main/branch/gitlink and SDK/ABI baseline recorded; conflicts open |
| G1 | T-AUTH-01–05; contract schema; SEC control map; error snapshot tests | API/OpenAPI, threat model, error and traceability reviews accepted; FR-SEC numbering issue explicitly handled |
| G2 | T-VM-01–11; T-SES-01–09; T-SEC-01–06,09–11; relay contract | Active session, assignment, grant, status, allowlist, WSS and revocation tests pass; VM4 remains excluded |
| G3 | T-GUEST-01–06; T-RDP-01–10; T-BRIDGE-01–07; TS-RDP-01–05 | Real pinned FreeRDP NLA session with secure bridge/cert validation, renderer lifecycle and complete cleanup |
| G4 | T-INPUT-01–05; T-LIFE-01–07; T-PERF-01–06; T-NEG-01–20 | Physical RD-1 procedure and all PRD reliability/performance/security gates pass |
| G5 | T-OBS-01/02; T-PRIV-01/02; T-REL-01–05; T-SEC-12 | Owner-signed artifact, static/privacy scan, support, rotation, backup/retention, incident/rollback evidence approved |

## 10. Current evidence status

- [VERIFIED: .github/workflows/android-ci.yml at main@5979320b05e1e6c1d872d82dcc17e65b0901e7e5] CI currently runs JVM unit tests and a debug APK build. API 36 emulator instrumentation is opt-in through manual workflow dispatch.
- [VERIFIED: README.md and docs/ANDROID_BUILD_REPORT.md at main] Historical build evidence describes a debug-signed artifact, not an owner-signed release or end-to-end RDP.
- [UNKNOWN] No current physical RD-1 end-to-end RDP performance ledger was found in the inspected repository evidence.
- [UNKNOWN] No broker production deployment, session_id revocation test, protected LocalBridge hijack test or certificate-pin acceptance dataset is evidenced in the current main source snapshot.

A gate can only be updated from UNKNOWN/NOT STARTED when the evidence above exists and has been reviewed.


## 6. College Administrator and student-account verification

**Status:** Owner-confirmed proposed scope for successor PRD v1.3; not a claim that these capabilities are implemented. The v1.2 PDF remains unchanged. All T-ADMIN cases below must be added to the automated/contract/device test backlog and linked to MD-ADM tasks.

| Requirement ID (proposed) | Test ID | Level | Acceptance summary | Gate |
|---|---|---|---|---|
| IAM-01 two-role model | T-ADMIN-01 | UNIT, CONTRACT | Exactly college_admin and student roles are recognized; unknown role fails closed | G1, G2 |
| IAM-02 admin bootstrap | T-ADMIN-02 | INTEGRATION, STATIC | Initial admin is provisioned out-of-band; no bootstrap or admin creation route exists in student UI | G1, G2 |
| IAM-03 no student signup | T-ADMIN-03 | DEVICE, STATIC | No Create account/Sign up UI; Android client does not invoke Supabase signUp; public signup configuration is verified only after invite path passes | G1, G4, G5 |
| IAM-04 admin-created identity | T-ADMIN-04 | INTEGRATION | Student Auth creation is accepted only from an authorized backend admin operation; student calls are denied | G2 |
| IAM-05 roster-first invite | T-ADMIN-05 | INTEGRATION | Roster row exists before invite; existing trigger links exactly one student profile; duplicate/retry failures are idempotent | G2 |
| IAM-06 first activation | T-ADMIN-06 | DEVICE, INTEGRATION | Actual Supabase invitation email opens supported invite callback, activates pre-created user and enables first password setup | G2, G4 |
| IAM-07 invite negatives | T-ADMIN-07 | DEVICE, INTEGRATION | Forged, expired, replayed, no-code, wrong-type and non-outstanding invite callbacks do not create/replace a session or enter recovery | G2, G4 |
| IAM-08 role source | T-ADMIN-08 | CONTRACT, INTEGRATION | user_metadata, email suffix, local role preference and client-supplied role cannot grant college_admin | G2 |
| IAM-09 workspace isolation | T-ADMIN-09 | DEVICE, CONTRACT | College Administrator opens admin workspace; student opens student workspace; direct route/API tampering is denied | G2, G4 |
| IAM-10 service secret isolation | T-ADMIN-10 | STATIC, RELEASE | No Supabase service-role/secret key is present in APK, BuildConfig, resources, source or CI artifact | G2, G5 |
| ADM-01 student lifecycle | T-ADMIN-11 | INTEGRATION | Admin can invite, resend, suspend, reactivate and disable; student cannot manage account lifecycle | G2 |
| ADM-02 assignment manager | T-ADMIN-12 | CONTRACT, INTEGRATION | Admin can assign/change/unassign only eligible allowlisted VMs; duplicates/VM4/unapproved endpoints are rejected | G2 |
| ADM-03 host/VM operations | T-ADMIN-13 | INTEGRATION, SECURITY | Only authorized admin can request allowlisted lifecycle actions; no arbitrary shell or host/VM endpoint accepted | G2, G4 |
| ADM-04 session support | T-ADMIN-14 | INTEGRATION, DEVICE | Admin can list safe session metadata and terminate; relay closes and slot releases within policy window | G2, G4 |
| ADM-05 audit | T-ADMIN-15 | INTEGRATION, STATIC | Provisioning, status, assignment, VM action and session termination produce redacted immutable audit events | G2, G5 |

### 6.1 Required role and privilege negative tests

- Student attempts to call every administrative endpoint directly.
- Student changes user_metadata to college_admin or sends a role field in the request.
- Student calls an admin navigation route by deep link or crafted Intent.
- Suspended/disabled admin attempts a privileged operation with a still-unexpired JWT.
- Admin attempts an unapproved VM action or arbitrary host/port target.
- Invitation request is repeated, replayed or attempted for a duplicate/confirmed Auth identity.
- Admin provisioning attempts to create another college_admin through the student flow.
- Service secret scan includes release APK, debug APK, generated BuildConfig, resources and CI artifacts.

**Pass rule:** role UI is not an authorization control. Every privileged operation must be denied by the trusted backend when actor role, active session, account status or target policy is invalid.

### 6.2 Invitation and activation test environment

- Use a synthetic roster student and approved test email.
- Test the actual configured invitation email template, Redirect URL and delivery provider.
- Verify the exact Supabase Kotlin 3.6.0 invite callback payload; do not assume the existing password-recovery PKCE callback handles invitations.
- Local Supabase/Mailpit HTTP access, if used, must be debug-only; release cleartext remains disabled.
- Record the emulator/device, SDK, SDK 3.6.0 tag, app commit, invitation type, callback outcome and redacted evidence.
- Include Activity recreation and process-death tests during callback handling.
- Confirm invitation links expire and can be resent without creating duplicate Auth users or roster rows.
