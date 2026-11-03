# MobiDesk R1 test plan

**Specification:** [`docs/MobiDesk_PRD_v1.2.pdf`](docs/MobiDesk_PRD_v1.2.pdf), owner-review draft pending approval. **Rule:** no row is passed by a build alone. Mark `[x]` only with linked test output or signed evidence in `PROGRESS.md`.

## Automated and contract tests

| Requirement | Acceptance test | Current evidence/status |
|---|---|---|
| FR-AUTH-01 | Frozen-baseline regression for sign-in, session restore, recovery and sign-out; no recovery email sent in smoke test | App flow exists; local JVM unit suite has 2 tests; full Android regression remains pending. |
| FR-AUTH-02 | Supabase/RLS tests: anonymous denied; user A cannot read user B profile or assignment; request uses caller token | Schema/RLS were inspected; live JWT isolation tests pending. |
| FR-VM-01 | Zero/one/duplicate/malformed assignment fixtures; exactly one VM or safe block | Repository UI exists; exact-one fail-closed tests pending. |
| FR-VM-02 / FR-VM-03 | Capture empty-body grant request; prove no VM ID/name/host/IP/port is sent; tamper/cross-user/unmapped assignment denied | Target API not implemented; broker prototype must be changed. |
| FR-VM-04 / FR-VM-05 | Status reports lifecycle and RDP transport separately; clock-controlled stale sample after 15 s blocks Connect; Connect rechecks status | No status API/client yet. |
| FR-SES-01 / FR-SES-02 | At least 256-bit random, single-use, 60-second, session/assignment-bound grant; replay, expiry, cross-user, concurrency and query-string rejection tests | Prototype covers several grant mechanics, but target binding/contract is not fully compliant; not release evidence. |
| FR-SES-03 | Revoke assignment or Supabase session during active relay; periodic check at least every 15 s; relay closes within 30 s | Not implemented. |
| FR-SES-04 | Per-student single active RDP; bounded pilot-wide sessions; cancel/failure releases slots | Prototype has process-local concurrency tests; no deployable/global state or full fault test. |
| FR-GUEST-01 / FR-GUEST-02 | Windows username/domain/password prompt occurs before grant; no password in grant request, Intent, URI, saved state, disk, log or telemetry; verify mutable-buffer cleanup paths | Not implemented. Supabase and Windows credentials remain separate. |
| FR-RDP-01 / FR-RDP-02 | Physical Android RDP/NLA with approved per-VM certificate pin; wrong/missing pin fails closed | Pinned-source audit confirmed authentication/certificate callbacks; no MobiDesk session launch, pin policy, or certificate rejection test. |
| FR-RDP-03 / FR-RDP-04 | Native MobiDesk Activity owns a render target; inspect pinned source for surface lifecycle/RDPEDISP and test rotation/attach/detach | Source audit done; FreeRDP exposes a bitmap-backed View. MobiDesk Activity, RDPEDISP, lifecycle and rotation tests remain unimplemented. |
| FR-RDP-05 | Byte-for-byte local bridge/WSS echo; preserve byte order across frames, bounded queues/backpressure/EOF/cancel; malicious local app cannot hijack | No Android bridge. FreeRDP has native C transport hooks but no raw-stream API in the Android Java wrapper; native/JNI feasibility spike required. Never ship an unauthenticated localhost TCP bridge. |
| FR-INPUT-01 / FR-INPUT-02 | Reference-phone checklist for pointer/trackpad, click/drag/scroll, soft/hardware keyboard, modifiers and Ctrl+Alt+Delete | Not implemented. |
| FR-INPUT-03 | Verify `FLAG_SECURE` blocks screenshots/task-switcher preview on RD-1 | Not implemented. |
| FR-LIFE-01–04 | Rotation, foreground/background, screen lock, network loss, cancellation and process death; 5-second background teardown; fresh grant on reconnect; 10+ repeated cleanup cycles with zero orphan sockets/native workers/slots | Not implemented. |
| FR-OBS-01 / NFR-PRIV-01 | Telemetry schema and static/log/packet scan show no passwords, JWTs, grants, screen content or private endpoints | Not implemented. |
| FR-SEC-04 | Device integrity policy blocks high-confidence rooted/unlocked device and reports unavailable signal as UNKNOWN | Not implemented; record limitations. |
| FR-REL-01 / FR-REL-02 | Reproducible owner-signed artifact; verify signer, source commit and SHA-256; inspect release for mocks, test endpoints, cleartext/trust-all policy | No owner release key/artifact. Local debug build passed and merged FreeRDP components were contained; first CI run failed at SDK setup before Gradle. Debug APK is not final. |
| NFR-PERF-01 | 20 controlled attempts; p90 credential-submit-to-first-frame ≤20 s | No measurements. |
| NFR-PERF-02 | 120 s 1280×720 motion workload; average ≥15 presented FPS, record p50/p90/drops | No measurements. |
| NFR-PERF-03 | On baseline Wi-Fi RTT ≤40 ms, scripted input p90 ≤250 ms | No measurements. |
| NFR-PERF-04 | At least 19/20 controlled attempts succeed first try; stage-specific failures | No measurements. |
| NFR-PERF-05 | 20 connect/disconnect cycles; zero native crash/leaked slot/socket | No measurements. |
| NFR-PERF-06 | ≤10 s Wi-Fi interruption recovers within 15 s with a fresh grant, if VM remains available | No measurements. |
| NFR-SEC-01 | Zero unauthorized successes across negative tests | No end-to-end evidence. |
| NFR-COMP-01 | Verify SDK/Gradle/ABI baseline and APK contents after build | Local Gradle build completed: 2 JVM tests passed, native libraries compiled, merged manifest has zero exported FreeRDP components, debug APK signature/ZIP verified. GitHub CI still needs a green run. |

## Release gates

- **G0 Product/baseline freeze:** Owner approves PRD; branch, commit and FreeRDP gitlink recorded.
- **G1 Contract/threat-model freeze:** API, assignment, status, active-session, guest-auth, certificate and local-bridge tests agreed.
- **G2 Broker ready:** auth/assignment/replay/revocation/allowlist tests pass; only owner-approved VM endpoints are mapped; stable TLS/WSS and loopback binding verified.
- **G3 Android session ready:** native session Activity, Windows prompt, pinned FreeRDP, certificate callback, protected local bridge and instrumentation pass.
- **G4 Physical R1 acceptance:** correct assigned VM, negative authorization, performance, recovery and cleanup pass on the nominated physical reference device and controlled Wi-Fi.
- **G5 Release/operations ready:** exact artifact signed; signer/hash/source revision, support, backup/restore, retention and rollback evidence approved.

Current gate status is tracked in [`PROGRESS.md`](PROGRESS.md); unresolved dependencies are in [`OPEN_ITEMS.md`](OPEN_ITEMS.md).
