# MobiDesk — Technical Specification

**Version:** 1.0 · **Date:** 3 November 2026  
**Status:** Engineering specification for review. This document defines target interfaces and constraints; it does not claim that the broker or end-to-end RDP path is deployed.  
**Applies to:** `master43721/mobidesk-android`, native Kotlin/Jetpack Compose client, pinned FreeRDP Android v3.32.0, Supabase Auth/Postgres/RLS, and a server-side session broker.

## 1. Purpose and implementation rules

This specification translates PRD v1.2 into engineering contracts suitable for implementation and verification.

**Normative source order**
1. Approved PRD requirements define product behaviour.
2. This specification defines implementation contracts within that behaviour.
3. Technical design defines component topology and sequence.
4. Existing source and audit reports describe observed implementation only.

No source-code convention, sample endpoint or undocumented upstream API may override an approved requirement. Unknown upstream details are explicit spikes, not guessed method names or magic Intent extras.

### R1 architecture decisions
- Native Kotlin + Jetpack Compose app; no Flutter or MethodChannel architecture.
- Dedicated non-exported `RdpSessionActivity` owns the native FreeRDP session renderer and input controls.
- Supabase identifies the student and stores their profile/assignment; broker owns live VM status, session grants and relay.
- Broker derives the assignment. Android never chooses a VM or endpoint.
- Student supplies a separate Windows NLA username/password each session for the pilot.
- WSS is a custom binary RDP relay, not an RD Gateway. Exact compatibility with the pinned FreeRDP release must be proven.
- Public TCP/3389 is prohibited.

## 2. Runtime components and module boundaries

| Logical module | Primary responsibility | Depends on | Must not own |
|---|---|---|---|
| `app` / Compose UI | Screens, navigation, user-visible state and accessibility | Domain use cases | VM routing, guest password persistence, broker secrets |
| `auth` | Supabase sign-in, restore, refresh, sign-out and recovery | Supabase SDK | Windows NLA credentials |
| `student` | Own profile and assignment read models | Auth session, RLS | Admin assignment mutation |
| `broker-client` | Typed HTTPS status/grant/close requests | Auth token provider, HTTP client | Host/port selection |
| `session-domain` | State machine, session policy and error mapping | Broker client, session ports | Android Activity/View instances |
| `rdp-session` | FreeRDP adapter, native Activity integration and input mapping | Pinned FreeRDP API | Supabase service-role credentials |
| `transport` | WSS binary relay and protected local stream bridge | Android sockets/approved TLS stack | Grant creation or VM selection |
| `telemetry` | Privacy-safe stage timings and reason codes | Session events | Tokens, passwords, screen contents or direct identifiers |

These are logical boundaries. They may map onto existing Gradle modules/packages only after the current repository structure is inspected; do not create duplicate application modules or refactor unrelated source merely to match the table.

### Dependency direction
UI -> domain/use cases -> interfaces (ports). Infrastructure adapters implement those ports:
- Supabase adapter implements Auth/profile/assignment ports.
- HTTPS adapter implements broker control API.
- WSS/local bridge adapter implements byte-stream transport.
- FreeRDP adapter implements native desktop session.
- Telemetry adapter emits approved metadata only.

The domain/session coordinator must not import Compose UI classes. FreeRDP classes must not leak into ordinary dashboard ViewModels.

## 3. Configuration and environment

### Client
- Application ID observed in baseline: `com.mobidesk.android`.
- Minimum Android SDK observed: 29.
- Compile/target SDK observed in baseline: 37; revalidate against repository Gradle configuration at build time.
- Native ABIs observed in debug packaging: `arm64-v8a` and `x86_64`.
- FreeRDP Android source is pinned as a git submodule; baseline records v3.32.0. Do not update the pin as part of the session feature without a separate compatibility decision.
- Supabase URL and publishable client key are supplied via local ignored configuration / build environment. No service-role key, SMTP secret, Windows password, broker signing secret or release keystore is embedded in source.

### Broker
Environment is server-only and must be validated at startup:
- public HTTPS/WSS origin;
- Supabase issuer/project URL and supported JWT verification configuration;
- server-side Supabase authorization client/credential;
- current VM UUID-to-private-endpoint allowlist;
- allowed destination port (3389 only for R1);
- grant HMAC/hash key or secure token-hash implementation;
- TLS certificate/private key from secret manager or reverse proxy;
- concurrency, rate, frame, buffered-byte, idle and total-session limits;
- RDP certificate CA/pin inventory;
- operational health, log redaction and audit settings.

Reject startup when the VM endpoint map is malformed, duplicates a VM identity, contains forbidden address classes, has a missing certificate policy, or exceeds the configured pilot capacity. If the pilot enables only a subset of VMs, the allowlist must describe precisely that subset; do not fabricate placeholders.

## 4. Typed domain model

The following are language-neutral contracts. Map them to Kotlin sealed interfaces/data classes and broker server types as appropriate.

### Student workspace
- `StudentProfile(id, displayName, institutionLabel)`
- `AssignmentState = ASSIGNED | NONE | AMBIGUOUS | INACTIVE | UNKNOWN`
- `AssignedVm(vmIdInternal, displayLabel)` — internal identifier remains domain/server data and MUST NOT be sent from client as authorization input.
- UI response exposes only display-safe fields. Assignment lookup must resolve exactly one row.

### VM status
- `VmLifecycle = STOPPED | STARTING | RUNNING | STOPPING | MAINTENANCE | UNKNOWN`
- `RdpReadiness = BOOTING | RDP_PORT_REACHABLE | RDP_READY | RDP_UNAVAILABLE | UNKNOWN`
- `VmStatus(assignmentState, lifecycle, readiness, observedAt, expiresAt, retryAfterSeconds, requestId)`
- Maximum per-probe budget: 1500ms. UI treats data older than 15 seconds as stale.
- A TCP connect to 3389 is only `RDP_PORT_REACHABLE`. `RDP_READY` requires a successful bounded RDP negotiation/health signal. A full student session still requires NLA.

### Session policy
- `Grant(grantSecret, expiresAt, relayPath, requestId, certificatePolicy)`
- `SessionIdentity(userId, authSessionId, assignmentVersion, opaqueSessionId)`
- `GuestCredentials(username, domain?, passwordChars)` — password is mutable in-memory buffer only and must not be serialized, persisted or logged.
- `SessionPhase = IDLE | PREPARING | REQUESTING_GRANT | CONNECTING_RELAY | NEGOTIATING_RDP | AUTHENTICATING_GUEST | CONNECTED | RECONNECTING | CLOSING | FAILED | CLOSED`
- `SessionFailure(code, retryable, safeMessage, requestId?)`

## 5. Broker HTTP API contract

All timestamps are ISO-8601 UTC. JSON fields are camelCase. Responses must not expose internal VM endpoint metadata. All control endpoints use HTTPS.

### 5.1 `GET /health`
Purpose: process liveness only. No user data, VM inventory or secrets. A healthy process response does not prove Supabase, hypervisor or RDP readiness.

### 5.2 `GET /v1/vm-status`
Headers: `Authorization: Bearer <Supabase access token>`.

No request body and no query parameter identifying a VM.

Successful response (illustrative schema, exact optional fields may be omitted):
```json
{
  "assignmentState": "ASSIGNED",
  "vmLifecycle": "RUNNING",
  "rdpReadiness": "RDP_PORT_REACHABLE",
  "observedAt": "2026-11-03T05:00:00Z",
  "expiresAt": "2026-11-03T05:00:15Z",
  "retryAfterSeconds": 5,
  "requestId": "opaque-correlation-id"
}
```
The example value `RDP_PORT_REACHABLE` intentionally does not mean NLA has succeeded. If assignment lookup is unavailable or ambiguous, return a safe error/state and do not probe a guessed guest.

### 5.3 `POST /v1/session-grants`
Headers: `Authorization: Bearer <Supabase access token>`, `Content-Type: application/json`.

Body: empty JSON object or no body. Reject/ignore unknown destination-routing fields; under no circumstance can they alter the server-derived destination.

On success:
```json
{
  "grant": "<single-use-secret>",
  "expiresAt": "2026-11-03T05:01:00Z",
  "relayPath": "/v1/rdp",
  "requestId": "opaque-correlation-id",
  "certificatePolicy": {
    "mode": "SHA256_PIN",
    "sha256": "<approved-fingerprint>"
  }
}
```
The certificate policy example is illustrative. Decide whether the trusted fingerprint is passed to client or validated entirely in broker/FreeRDP after the pinned engine API is proven. It must be integrity-bound to the authenticated grant/session, not user-editable.

Grant rules:
- minimum 256 bits of cryptographic entropy;
- 60-second expiry;
- bound to user ID, active Supabase session ID, assignment identity/version and approved VM;
- hash at rest; never log raw grant;
- atomic one-time consumption;
- per-user concurrency and rate limits;
- no guest password is issued or returned.

### 5.4 `GET /v1/rdp` (WebSocket upgrade)
- WSS only.
- Grant is presented in an Authorization header or another approved non-URL secret-bearing handshake mechanism. Do not put grant secrets in query strings, analytics, browser history or reverse-proxy access logs.
- Validate/consume grant before connecting upstream.
- Broker chooses upstream from server allowlist and fixed RDP port 3389.
- Relay binary bytes in order with backpressure. WebSocket message boundaries MUST NOT be mistaken for TCP byte-stream boundaries.
- Enforce configured frame/message size, buffered bytes, handshake, idle and total-session timeout.
- On termination, close both directions and release every upstream/client resource.

### 5.5 Optional close endpoint
If a control-plane `POST /v1/session-close` is implemented, it must be idempotent and scoped to the authenticated user/session. It is a best-effort accelerator; WSS close and broker timeouts remain authoritative for cleanup.

## 6. Identity, authorization and assignment rules

1. Verify JWT signature using trusted issuer keys; validate issuer, audience, expiry and permitted algorithm.
2. Extract stable subject and session identifier from validated claims. Never accept user ID from request JSON.
3. Verify the session is currently active in the Supabase Auth session source. Exact supported query/admin API and permissions must be validated against the deployed Supabase version; when uncertain or timed out, fail closed.
4. Resolve the active student record and assignment under the authenticated subject / server-controlled authorization query equivalent to the existing RLS policy.
5. Require exactly one valid assignment and an allowlisted VM mapping.
6. Revalidate immediately before grant issuance and on active relays. Re-check session and assignment at intervals no greater than 15 seconds; close relay within 30 seconds after revocation/change.
7. Use opaque error identifiers and never disclose whether another student or VM exists.

The Android client may query RLS-protected display data for its own dashboard, but that read is not a substitute for the broker's authorization decision.

## 7. Windows NLA and credential handling

R1 uses separately provisioned Windows guest credentials. The student enters username, optional domain and password in a secure native prompt before requesting a short grant.

- Do not reuse Supabase password.
- Do not send guest password to broker.
- Never place credential values in Android Intent extras, saved state, deep link, DataStore, SharedPreferences, database, analytics or logs.
- Keep password in mutable memory buffers where possible.
- Verify the pinned FreeRDP credential callback/ownership semantics before deciding when the app can clear its buffer.
- Clear Kotlin/Java and native copies as soon as FreeRDP no longer requires them, and on NLA failure, cancel, exception, Activity finish and process teardown.
- No claim of perfect erasure from managed immutable Strings or OS memory is allowed.
- On bad guest credentials, invalidate grant/session attempt and permit deliberate re-entry.

## 8. FreeRDP integration contract

The exact v3.32.0 source in the git submodule is authoritative for implementation details. Before coding, trace the version-pinned Android entry points, connection reference/configuration, callback lifecycle, rendering surface, input injection, certificate decision and disconnect path.

Required adapter capabilities:
- create/configure one FreeRDP session with NLA enabled;
- consume the protected local stream connected to WSS byte relay;
- bind the server identity policy;
- provide ephemeral guest credentials using supported native callbacks;
- expose connection events, failure codes and first-frame event;
- bind/unbind native rendering surface without losing ownership of the underlying protocol session where supported;
- map pointer, click, touch, wheel, keyboard text, scan codes and modifiers;
- terminate native threads and release references deterministically.

Do not assume FreeRDP's RD Gateway WebSocket mode can connect to an arbitrary raw WSS relay. Prove a byte-stream adapter exists; otherwise stop and redesign the broker/transport boundary before implementing a non-interoperable tunnel.

### Android UI boundary
- Compose dashboard initiates a session through the session coordinator.
- Native non-exported `RdpSessionActivity` owns desktop renderer and input overlay.
- Do not put bearer/grant/password in Activity extras. Pass an opaque in-process/session-store handle with process-death-safe semantics that does not persist secrets.
- Enable `FLAG_SECURE` on session Activity.
- On R1 background/screen lock, follow foreground-only close/pause policy. Do not introduce a foreground service until its Android version/type/permission requirements and real need are verified.

## 9. Local transport bridge contract

Preferred order:
1. socketpair or connected descriptor supported by the pinned FreeRDP custom transport;
2. app-private Unix-domain socket with same-UID peer verification;
3. loopback TCP only after security spike.

For TCP fallback: bind exclusively to 127.0.0.1; high-entropy ephemeral port; separate random one-time authenticator; listener created immediately before expected client connect; one accepted connection; listener closed immediately after accept; short deadline; authenticate before forwarding; bound buffers; erase secret; test malicious local client and port hijack. Random port alone is not protection. No LAN binding.

## 10. Failure contract and mapping

| Code | Layer | User action | Retry |
|---|---|---|---|
| AUTH_REQUIRED | Auth | Sign in again | After auth |
| SESSION_REVOKED | Broker | Return to sign-in | New session only |
| ASSIGNMENT_NOT_FOUND | Assignment | Contact operator | No |
| ASSIGNMENT_AMBIGUOUS | Assignment | Operator correction | No |
| VM_MAINTENANCE / VM_STOPPED | VM | Show truthful status | When changed |
| RDP_UNAVAILABLE | Readiness | Retry later | Yes |
| GRANT_EXPIRED / GRANT_USED | Grant | Request new grant | Yes |
| RELAY_LIMIT / RATE_LIMITED | Broker | Wait and retry | Yes, bounded |
| CERTIFICATE_MISMATCH | Security | Stop and contact operator | No |
| GUEST_AUTH_FAILED | Windows NLA | Re-enter guest credentials | Yes, rate limited |
| NETWORK_LOST | Relay | Show recoverable connection loss | New grant |
| CLIENT_UNSUPPORTED | Client policy | Explain supported device requirement | After upgrade |

Internal stack traces, private endpoint data, other users' data and secret-bearing messages are never shown.

## 11. Telemetry and observability

Allowed fields: opaque session ID, coarse client version/device family, stage identifier, start/end time, duration, safe result code, retry count, coarse network bucket, broker build. Avoid direct identifiers and all secret/session tokens.

Stages: auth restore, assignment, status, grant, WSS upgrade, FreeRDP negotiation, NLA, first frame, input response, disconnect and cleanup. Report sample count, p50 and p90. Pilot performance acceptance is in PERFORMANCE_PLAN.md.

## 12. Build, test and release constraints

- Preserve pinned FreeRDP submodule and upstream code unless a reviewed change is approved.
- Keep native libraries for approved ABIs; verify APK/AAB contents on every release candidate.
- Use local ignored configuration for publishable Supabase settings. Never commit privileged keys.
- Unit, JVM, broker contract, Android instrumentation, fault-injection, security and physical-device tests are required.
- A successful compile/debug APK is not proof of working RDP.
- Production release signing and distribution remain blocked pending owner decision.

## 13. Implementation spikes (must precede dependent work)

| Spike | Question | Exit evidence |
|---|---|---|
| TS-01 FreeRDP transport | Can pinned v3.32.0 consume an arbitrary byte stream with the proposed WSS adapter without RD Gateway assumptions? | Small proof app/test with byte fidelity, lifecycle and error behaviour |
| TS-02 Certificate callback | Can the Android build enforce CA/pin policy with fail-closed semantics? | Valid, invalid, rotated fingerprint tests |
| TS-03 Session Activity | Which exact pinned classes/callbacks/surface APIs are needed? | Version-pinned source map and surface lifecycle demo |
| TS-04 Local bridge | Which protected descriptor/socket option is actually supported? | Peer-auth/hijack test and teardown evidence |
| TS-05 RDP readiness | Which bounded no-credential probe differentiates open port from usable RDP negotiation? | State semantics and timing/failure report |
| TS-06 Revocation | Which Supabase-supported session-state query can enforce revocation? | Tested query, permission model and <=30s relay-close test |

No dependent production implementation is accepted until the relevant spike has an evidence artifact.
