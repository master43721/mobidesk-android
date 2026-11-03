# MobiDesk — R1 Performance and Reliability Plan

**Version:** 1.0 · **Date:** 3 November 2026  
**Normative thresholds:** PRD v1.2 NFR-PERF-01 through NFR-PERF-06.  
**Status:** Test method only; no threshold is claimed as achieved.

## 1. Principles

- Record complete user-journey timing from the user's action to measurable outcome.
- Use a monotonic clock for interval measurement.
- Report sample count, failures, p50 and p90. Do not claim statistically meaningful cohort adoption metrics for the small pilot.
- Do not drop failed attempts from connection-success reporting.
- Preserve raw measurements with fake/synthetic account data; never store student identity, password, grant/JWT, private endpoint, screen pixels or clipboard contents.
- Use the same controlled guest workload and network conditions across comparison runs.

## 2. Reference test setup

**RD-1 nomination:** Pixel 6, Android 16 / API 36, arm64-v8a per PRD §8.1.

**Availability:** UNKNOWN as of 3 Nov 2026. Project Owner must verify the actual physical test phone before G4. If the actual phone is different, update the PRD through an approved revision; do not substitute an emulator or guessed model.

Record in private test evidence:
- physical model/variant, Android release/API, ABI and build fingerprint;
- app version, source commit, FreeRDP gitlink and artifact SHA-256;
- battery/thermal condition and display refresh configuration;
- controlled Windows guest workload and resolution;
- Wi-Fi network RTT, packet loss and relay route class;
- client/broker logs with secrets and identifiers redacted.

Baseline network condition for NFR-PERF-03: RTT <=40 ms. If packet loss is part of a run, record it explicitly; do not silently mix network conditions.

## 3. Instrumented stages

| Stage | Start event | End event |
|---|---|---|
| Auth restore | App starts restoration | Valid Supabase session or safe sign-in state |
| Workspace | Student workspace query begins | Profile + single assignment or safe error |
| Status probe | GET /v1/vm-status begins | Valid response or 1500ms timeout |
| Grant | User submits Windows credentials / grant request begins | Grant accepted or safe denial |
| WSS | WebSocket upgrade starts | HTTP 101 or failure |
| LocalBridge | Protected local transport starts | Peer authentication and stream ready |
| RDP negotiation | FreeRDP begins RDP connect | RDP negotiation established or error |
| NLA | Windows credential callback begins | Windows NLA success/failure |
| First frame | NLA/protocol reaches connected state | First desktop frame is presented |
| Input response | Scripted input is dispatched | Corresponding visual response is presented |
| Cleanup | Disconnect/terminal event begins | Native, local and broker resources confirmed released |

Allowed telemetry: opaque attempt ID, stage, start/end monotonic timestamps, duration, result, retry count, PRD error code, coarse network bucket and coarse device/build class. Do not include account names, student IDs, guest username/password, tokens, raw WSS URLs, VM metadata or desktop content.

## 4. PRD performance acceptance

| NFR | Procedure | Pass threshold |
|---|---|---|
| NFR-PERF-01 First frame | At least 20 controlled attempts; start at Windows credential submit; stop at first rendered frame; include failed attempts in success report | p90 <=20 seconds |
| NFR-PERF-02 Frame rate | 120-second deterministic 1280x720 motion workload; count presented frames; record frame interval and dropped/late frames | Average presented FPS >=15 |
| NFR-PERF-03 Input | Deterministic key/pointer workload with baseline RTT <=40ms; measure dispatch-to-visible-response using monotonic time | p90 <=250ms |
| NFR-PERF-04 Reliability | At least 20 controlled connection attempts on RD-1 | At least 19/20 succeed first try |
| NFR-PERF-05 Cleanup | 20 consecutive connect/disconnect cycles, including failed attempts, app background/close and network loss | Zero native crash, leaked slot, socket, worker, queue, listener, grant or credential buffer |
| NFR-PERF-06 Recovery | Interrupt Wi-Fi for <=10 seconds during connected session; verify status/assignment and request a new grant | Recover within <=15 seconds if guest remains ready; otherwise safe failure and full cleanup |

## 5. Measurement details

### First frame
- Run 20 attempts, ideally across at least two test sessions.
- Store per-attempt: start/end time, result, failure stage and error code.
- Calculate nearest-rank or an explicitly documented percentile method; use the same method for all comparisons.
- p90 must include successful connection latency distribution; the reliability report separately counts failed attempts so failures are not hidden by p90 calculation.

### Frame rate
- Freeze guest resolution at 1280x720.
- Use a repeatable motion workload such as a local animation/video with no external network dependency.
- Count frames presented by the client renderer, not server codec output or generated FPS target.
- Report average FPS, p50/p90 frame interval, frames dropped/late and CPU/memory/thermal condition.
- Repeat the workload after thermal stabilization; do not compare a cold device run with a thermally throttled device run without noting it.

### Input latency
- Use a deterministic test script (e.g. moving a visible remote cursor or typing into a test field) with observable state change.
- Timestamp client input dispatch and the first render that demonstrates the result.
- Measure at least 100 input events; report p50/p90 and event type.
- Baseline RTT must be <=40ms. If it is higher, label the run non-baseline and do not use it to pass the PRD threshold.

### Reliability / cleanup
- Keep a 20-attempt ledger and 20-cycle resource ledger.
- After each disconnect, verify: no local listener/socket, no active native FreeRDP instance, no reader/writer worker, no non-empty relay slot, grant unusable and no app-owned guest credential buffer remains.
- After network loss, do not reuse a grant; a successful retry must get a fresh grant and fresh assignment check.
- Include at least one revoked-session case and one certificate-mismatch case in cleanup cycle coverage.

## 6. Result report template

~~~text
Report ID:
Date/time UTC:
RD-1 model / Android API / ABI:
App version / source commit:
FreeRDP gitlink:
Artifact SHA-256:
Guest workload / resolution:
Network RTT / packet-loss condition:
Test IDs:
Attempt count:
Success count / failure count:
First-frame samples (ms):
First-frame p50 / p90:
Presented FPS average / p50 / p90 frame interval:
Input events / p50 / p90 latency:
Cleanup cycles / leaks:
Recovery interruption / recovery result:
Thermal or resource caveats:
Overall: PASS / FAIL / BLOCKED
Evidence links/hashes:
Reviewer:
~~~

## 7. Current status

- [UNKNOWN] RD-1 physical device availability.
- [UNKNOWN] No 20-attempt first-frame ledger is verified in the current repo evidence.
- [UNKNOWN] No 120-second measured FPS report is verified.
- [UNKNOWN] No 100-event input latency sample is verified.
- [UNKNOWN] No 20-cycle cleanup evidence is verified.
- [UNKNOWN] No physical Wi-Fi recovery dataset is verified.

No performance metric is marked PASS until a complete report is added to private test evidence and a non-sensitive reference is linked in PROGRESS.md.
