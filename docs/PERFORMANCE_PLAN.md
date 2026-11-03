# MobiDesk — Performance and Reliability Plan

**Version:** 1.2 · **Date:** 3 November 2026 · **Status:** Proposed gates; not achieved metrics.

## Measurement principle
Measure the complete user journey, not a vanity benchmark. Record stage timings with an opaque session correlation ID. Do not record screen content, names, email addresses, passwords, access/refresh tokens, grants or private endpoints. Report sample size alongside p50/p90; a five-student pilot does not support broad population claims.

## Reference setup
Owner must select one physical Android reference device and record model, Android version/API, RAM, ABI, battery state and app build. A Pixel 6 API 36 AVD has been provisioned historically, but an emulator is not a substitute for physical-device acceptance. Use a controlled Windows VM, private RDP route, measured RTT <=40ms and packet loss <=1% for the baseline.

## Timings to instrument
- Sign-in/session restore; profile and assignment resolution; VM status and freshness.
- Grant request/validation; WSS establishment; FreeRDP negotiation/NLA; first rendered frame.
- Input event to visible response; disconnect and cleanup; recovery after bounded network interruption.

## Pilot gates (must be measured)
| Gate | Acceptance |
|---|---|
| Connection reliability | At least 19 successful connections out of 20 controlled attempts |
| First frame | p90 <=20 seconds from Connect tap to first rendered desktop frame |
| Frame rate | Average >=15 FPS over a 120-second 1280x720 reference motion workload |
| Input response | p90 <=250ms under baseline RTT <=40ms and packet loss <=1% |
| Resource cleanup | 20 consecutive connect/disconnect cycles without leaked relay, socket, grant or credential buffer |
| Network recovery | Recover or present actionable failure within 15 seconds after a <=10-second interruption; never silently reuse an expired grant |
| Status API | Probe bounded at 1500ms; stale after 15 seconds; unknown is not rendered as online |
| Sustained run | 10-minute session without crash, unbounded memory growth or uncontrolled thermal/performance degradation |

## Test conditions
Record device, OS, resolution, codec, guest CPU/RAM, host load, RTT, loss, relay region/path, sample count and measurement tool. Run idle and motion workload separately. Include screen rotation, resize, keyboard, scrolling and multi-touch in functional measurements.

## Telemetry fields
Allowed: opaque session ID, coarse client build/device family, stage name, start/end timestamps, duration, result code, retry count and coarse network quality bucket. Access must be role-limited and retention owner-approved.

## Gate failure
A failed gate blocks pilot expansion. Diagnose which stage regressed; do not change thresholds after seeing results without a decision record. Report distribution, p50, p90 and sample count.
