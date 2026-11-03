# MobiDesk — Security Threat Model

**Version:** 1.2 · **Date:** 3 November 2026 · **Scope:** R1 student session, broker and relay.

## Assets and trust boundaries
Assets: student identity and assignment; Supabase access/refresh tokens; one-use session grant; Windows guest credentials; RDP session data; VM private network; RDP server identity; broker availability; audit and telemetry data.

Trust boundaries: Android-to-Supabase Auth; Android-to-broker HTTPS; Android-to-broker WSS; broker-to-assigned-VM private network; Android-to-local-FreeRDP bridge; sensitive Android UI surfaces. Dock USB/AOA is R2 only.

## Threats and mandatory controls
| Threat | Control / test |
|---|---|
| Student changes VM ID to reach another VM | No client destination parameter; broker derives one assignment from authenticated user; test cross-student and altered-request cases |
| Stolen/replayed grant | >=256-bit random secret, 60s TTL, single atomic use, hash at rest, TLS, grant redaction; test replay and race |
| Revoked JWT works until expiry | Check auth.sessions at grant issue; re-check active relays <=15s and close within 30s |
| Token/grant/password leaks via logs | Structured secret redaction; no request/body dumps; test logs and crash reports |
| RDP server impersonation | Internal CA or per-VM SHA256 pin; fail closed on mismatch; test rotated and rogue certificate |
| Local bridge port hijack | Descriptor/Unix socket preferred; loopback fallback uses one-use secret, one accepted connection, listener closed, timeout; test rogue local connection |
| Broker enumeration or DoS | Per-user/source rate limits, generic errors, bounded probes, relay concurrency/idle limits, request size caps and abuse tests |
| Assignment changes mid-session | Periodic revalidation and relay teardown <=30s; test reassignment during active session |
| Windows password disclosure | Distinct guest credentials, memory only, no Supabase reuse, log/analytics/crash redaction, native cleanup |
| Public RDP exposure | No public TCP/3389; firewall and external scan verification; WSS is relay not direct guest access |
| Screenshot/recents leakage | FLAG_SECURE on session; verify screenshot and app-switcher behaviour |
| Rooted-device risk | Best-effort root policy blocks pilot session except explicitly approved test devices; document detection limitations |
| Spoofed USB dock | R2 only; user confirmation and key-backed challenge-response; USB strings not identity |

## Broker abuse controls from day one
- Use generic external errors to reduce assignment/account enumeration.
- Apply rate limits before expensive status checks and grant creation.
- Cap total relay connections, one concurrent relay per student, connection duration, idle time and buffered bytes.
- Make grant consumption atomic to defeat simultaneous replay.
- Bound outbound destinations using a server-owned VM allowlist; never compose shell commands from client data.
- Audit security outcomes using opaque correlation IDs; never record bearer/grant/password material.
- Alert on repeated invalid grants, unusual user concurrency and resource exhaustion.

## Windows guest credentials
R1 pilot recommendation: student provides Windows username/password per connection. Keep in mutable memory buffers where supported; do not write to disk, analytics, clipboard, logs or crash data. Clear Java/native buffers/settings copies on auth failure, disconnect, cancellation and Activity destruction, as soon as FreeRDP no longer needs the secret. Avoid promising secure erasure of immutable JVM copies.

## Certificate policy
Pin per VM or use managed internal CA. Store trust material in a trusted server-side inventory or provision it through a grant-bound signed policy. Do not offer accept-any fallback. Define replacement, expiry, mismatch handling and rollback before first pilot.

## Operational response / rollback
Stop issuing new grants, revoke active relays and isolate the affected VM on suspected cross-student routing, certificate mismatch, token compromise, public RDP exposure, repeated bridge hijack or inability to tear down sessions. Restore the previous reviewed broker/client version only after confirming access control and certificate policy. Never mitigate by disabling TLS/cert validation or opening public RDP.
