# MobiDesk — Broker API Contract

**Document:** API contract (R1)  
**Version:** 1.0 · **Date:** 3 November 2026  
**Normative source:** MobiDesk PRD v1.2, §§5.2–5.3, 6.2–6.7, 9 and 12.  
**Status:** Contract proposal for G1 review. It does not claim that these endpoints are deployed.

## 1. Contract rules

1. The Android client authenticates to the control plane with its current Supabase access JWT.
2. The broker verifies the JWT and resolves exactly one active assignment from trusted server-side data. The client never sends a VM ID, VM name, hostname, IP address or port.
3. The WSS data plane uses a separate one-use grant as its bearer credential. A Supabase JWT is not the WSS grant.
4. Windows username/domain/password are guest NLA credentials. They remain on the client and are never sent to the broker.
5. Public API responses contain no private endpoint, student identifier, VM UUID, host inventory, internal topology or secret.
6. All timestamp values are RFC 3339 UTC. Request correlation IDs are opaque and non-identifying.
7. The WebSocket data channel is a custom binary byte-stream relay. It is not Microsoft RD Gateway, and WebSocket message boundaries are not RDP packet boundaries.
8. No endpoint in this document authorizes guest power-on/off. R1 student APIs are read/status/connect/disconnect only.

The contract is normative for server/client interoperability once the Project Owner approves G1. Where this document labels a value **PROPOSED DEFAULT**, the value must be validated in the broker/load tests before G2; it is not a new PRD requirement.

## 2. Authentication and authorization

| Route | Credential | Purpose |
|---|---|---|
| `GET /health` | None | Process liveness |
| `GET /ready` | None | Safe service readiness indication |
| `GET /v1/vm-status` | Supabase access JWT | Resolve the caller's own assignment and observe readiness |
| `POST /v1/session-grants` | Supabase access JWT | Authorize a single-use relay grant |
| `GET /v1/rdp` WebSocket upgrade | MobiDesk session grant | Consume grant and begin the assigned guest relay |

### Control-plane verification sequence

For each authenticated request, the broker MUST:
1. Verify JWT signature using the trusted issuer key set; validate issuer, audience, expiry, permitted algorithm, and subject.
2. Extract the JWT `session_id` claim. Do not trust a caller-supplied user ID, email, role or session ID.
3. Verify the Auth session remains active using the approved server-only Supabase session-state adapter. Failure, timeout or inability to verify is denial, not success.
4. Resolve the authenticated subject to exactly one valid student assignment. Zero, duplicate, disabled, stale or unmapped assignments fail closed.
5. Resolve the assignment to an owner-approved server-side VM mapping. The VM4 mapping remains excluded until independently verified and approved; no placeholder or substitution is allowed.
6. Apply per-user and source rate limits, session concurrency limits and global capacity limits before allocating relay resources.

The broker MUST repeat active session and assignment validation at relay start, then at least every 15 seconds while a relay is active. If the session is revoked or the assignment changes, close the relay within 30 seconds. The phone's local sign-out callback is not a substitute for server-side revocation.

## 3. Health endpoints

### 3.1 `GET /health` — liveness

Unauthenticated. Reports only whether the process can answer.

**200 example**
```json
{
  "status": "ok",
  "timestamp": "2026-11-03T06:00:00Z"
}
```

It MUST NOT return dependency names, versions, environment variables, hostnames, VM counts, private addresses, credentials or configuration.

### 3.2 `GET /ready` — readiness

Unauthenticated. Reports whether the broker should receive new traffic. It is distinct from process liveness. It MUST NOT return detailed dependency status or host/network details.

**200 example**
```json
{
  "status": "ready",
  "timestamp": "2026-11-03T06:00:00Z"
}
```

**503 example**
```json
{
  "status": "not_ready",
  "timestamp": "2026-11-03T06:00:00Z"
}
```

The broker MUST stop issuing new grants while not ready. Existing relay termination follows the relay/session safety rules.

## 4. VM readiness

### `GET /v1/vm-status`

**Authentication:** `Authorization: Bearer <Supabase access JWT>`  
**Request body:** none.  
**Query parameters:** none. The client MUST NOT choose an assignment or pass any target selector.

The broker derives the single active assignment and reads:
- VM lifecycle: `STOPPED | STARTING | RUNNING | STOPPING | ERROR | UNKNOWN`.
- RDP transport: `NOT_CHECKED | TCP_UNREACHABLE | TCP_REACHABLE | RDP_NEGOTIATION_OK | UNKNOWN`.

A private TCP connect to port 3389 can produce only `TCP_REACHABLE`. `RDP_NEGOTIATION_OK` requires the broker's bounded RDP negotiation check. It does not prove that the student's Windows credentials will pass NLA.

The lifecycle/transport observation MUST complete within the PRD's 1,500 ms probe budget. If a relevant provider/probe times out or its result cannot be trusted, report `UNKNOWN` for that dimension; do not infer `RUNNING`, `TCP_REACHABLE` or `RDP_NEGOTIATION_OK`.

**200 example**
```json
{
  "lifecycle": "RUNNING",
  "rdpTransport": "RDP_NEGOTIATION_OK",
  "sampledAt": "2026-11-03T06:00:00Z",
  "probeDurationMs": 384,
  "staleAfterSeconds": 15,
  "requestId": "req_7f1b24a890d643d8"
}
```

**200 timeout/unknown example**
```json
{
  "lifecycle": "UNKNOWN",
  "rdpTransport": "UNKNOWN",
  "sampledAt": "2026-11-03T06:00:01Z",
  "probeDurationMs": 1500,
  "staleAfterSeconds": 15,
  "requestId": "req_9a26d0f4712a45ef"
}
```

The sample becomes stale 15 seconds after `sampledAt`. The client MUST refresh before Connect; the grant endpoint MUST independently recheck assignment/readiness. The response MUST NOT expose a VM ID, VM display name, endpoint, hostname or port.

## 5. Session grant

### `POST /v1/session-grants`

**Authentication:** `Authorization: Bearer <Supabase access JWT>`  
**Content-Type:** `application/json` when a body is sent.  
**Body:** empty JSON object `{}` or no body. The schema permits no fields. Requests containing a destination selector or credentials MUST be rejected.

**Illustrative request**
```http
POST /v1/session-grants HTTP/1.1
Authorization: Bearer <SUPABASE_ACCESS_JWT>
Content-Length: 2
Content-Type: application/json

{}
```

The broker validates caller/session, unique assignment, server allowlist, readiness, user/source rate limits and concurrency before issuing a grant.

**Grant properties**
- Secret is generated using a cryptographically secure random generator with at least 256 bits of entropy. A 32-byte value encoded as unpadded base64url is the recommended representation.
- Expiry is no more than 60 seconds from issuance.
- Grant is bound to the authenticated user, active Supabase `session_id`, current assignment/version and one server-resolved VM target.
- Grant is atomically single-use. Concurrent consume attempts cannot both succeed.
- Only a cryptographic hash of the grant is stored at rest. Raw grant material is returned once and never logged or persisted by the client.
- Grant is invalidated on consume, expiry, cancellation, logout/revocation, assignment change, authorization failure and relay teardown.
- One active relay per student. Pilot-wide capacity is bounded. The currently approved R1 VM set is four; the configured relay cap MUST NOT exceed the approved operational capacity. The exact deployed cap is an owner-controlled configuration value.
- Pilot grant rate limit: 5 requests per minute per authenticated user. Rejected requests do not disclose assignment existence.

**200 example**
```json
{
  "grant": "bXJ0U0Q3ZkJ2ZkR6WjFZQ0h1d2pTNGt4...",
  "expiresAt": "2026-11-03T06:01:00Z",
  "relayPath": "/v1/rdp",
  "rdpCertificate": {
    "mode": "LEAF_X509_DER_SHA256",
    "fingerprintSha256": "7f4a2c9e1d6b8035a7c4e2f90123456789abcdef0123456789abcdef01234567"
  },
  "requestId": "req_70d6ad416fe94b32"
}
```

The sample fingerprint is fake and illustrative. Fingerprints are lowercase hexadecimal SHA-256 of the DER-encoded leaf certificate. If an approved internal-CA mode is selected, `mode` MUST identify that explicitly and `fingerprintSha256` is the trusted CA certificate fingerprint; client validation semantics must be agreed before implementation. Rotation requires staged trust: provision and verify the next pin/CA out-of-band, overlap only for an owner-approved window, switch guest certificates, verify, then retire the old pin. Unknown, missing, expired or mismatched trust policy fails closed. No user “accept anyway” option exists.

The grant response MUST NOT contain `vm_id`, VM name, hostname, private IP, port, guest username/password, broker internal topology or other students' data.

## 6. RDP WebSocket relay

### `GET /v1/rdp` — WebSocket upgrade

**Transport:** WSS only.  
**Authentication:** `Authorization: Bearer <one-use MobiDesk grant>`.  
**Query string:** MUST NOT contain the grant or any secret.  
**Data frames after upgrade:** binary only. Text data frames are protocol violations.

The WebSocket handshake is not accepted until the broker validates and atomically consumes the grant. On success the broker resolves the target exclusively from the server-side grant record/allowlist, connects to the assigned guest on private TCP/3389 and starts a full-duplex byte relay.

The broker MUST:
- preserve byte order in both directions;
- treat WebSocket message/frame boundaries as transport framing only, not RDP packet boundaries;
- reject text data frames and unexpected WebSocket subprotocols;
- apply bounded reads/writes and backpressure;
- bound each directional queue; on a full queue pause the upstream reader where possible, then terminate safely if the peer does not drain before the configured timeout;
- handle EOF, half-close, close, cancellation, broker restart and network interruption deterministically;
- enforce a maximum WebSocket message/frame size and total buffered bytes;
- enforce per-user and global concurrent relay limits;
- send WebSocket ping control frames and require pong within the configured timeout;
- close idle or expired/revoked sessions;
- close upstream TCP and WSS resources on every exit path;
- record only safe opaque event metadata and reason codes.

### Proposed relay defaults (not PRD constants)

These values are initial engineering defaults, subject to load/performance testing before G2 approval:

| Setting | Proposed initial value | Reason / validation |
|---|---:|---|
| Maximum WebSocket binary message | 64 KiB | Bound parser and frame memory; client segments continuous byte stream without semantic packet framing |
| Per-direction application queue | 1 MiB | Bounded memory per relay; verify against real display/input throughput |
| Queue high-water mark | 768 KiB | Pause reading before the hard cap |
| Queue drain timeout | 10 seconds | Close unhealthy consumer rather than buffer indefinitely |
| Ping interval | 20 seconds | Detect dead peer while limiting idle battery/network overhead |
| Pong timeout | 10 seconds | Close unresponsive connection |
| Idle timeout | 120 seconds without data in either direction | Proposed; verify FreeRDP keepalive behavior |
| Maximum relay duration | 4 hours | Proposed pilot safety bound; owner must approve |
| Connect/upstream timeout | 3 seconds | Proposed bounded private dial |
| Concurrent relays | no more than approved active pilot capacity | PRD requires bounded pilot-wide concurrency; VM4 remains excluded |

These values MUST be configurable, covered by tests and reviewed against the real FreeRDP traffic profile. A WebSocket data message may contain a partial RDP PDU or multiple PDUs; the relay MUST remain byte-stream correct.

### WebSocket close contract

The following application close codes are **PROPOSED** values in the WebSocket private-use range. The close reason is limited to the corresponding PRD §12 public error code; never include stack traces, endpoint data or secrets.

| Close code | PRD error code | Meaning |
|---:|---|---|
| 4401 | GRANT-01 | Grant invalid, unauthenticated or assignment could not be authorized |
| 4402 | GRANT-02 | Grant expired or already consumed |
| 4403 | SESSION-02 | Auth session/assignment revoked or session ended by policy |
| 4404 | TUNNEL-01 | Relay service unavailable or transport failed |
| 4405 | RDP-CERT-01 | Guest certificate trust failed |
| 4406 | SESSION-01 | Active network path was lost |
| 4407 | SESSION-03 | Cleanup could not be confirmed |
| 4408 | RDP-01 | RDP protocol did not complete |

Before the WebSocket upgrade, return the unified HTTP error schema. After the upgrade, do not send JSON/text error messages as data frames; close with the registered application code and safe PRD error code.

## 7. Unified error schema — PRD §12 one-to-one mapping

All control-plane errors use this shape. The `code` is exactly one of the PRD §12 catalogue entries. `message` MUST be the exact English user-facing copy from §12. The server MAY include a safe `retryAfterSeconds` where appropriate. Internal detail is never included.

```json
{
  "code": "GRANT-02",
  "message": "This session request expired. Try connecting again.",
  "retryable": true,
  "requestId": "req_70d6ad416fe94b32",
  "retryAfterSeconds": 1
}
```

| PRD §12 code | HTTP / WebSocket context | Exact PRD user copy | Retry policy |
|---|---|---|---|
| AUTH-01 | HTTP 401 | Your sign-in session expired. Sign in again. | Re-authenticate |
| ASSIGN-01 | HTTP 200 state or 404/409 error | No Cloud PC is assigned to your account. Contact the lab administrator. | No retry until assignment changes |
| ASSIGN-02 | HTTP 200 UNKNOWN or 503 | We could not verify your assigned Cloud PC. Try again. | Bounded retry |
| ASSIGN-03 | HTTP 409 | Your Cloud PC assignment needs administrator review. | No retry |
| VM-01 | HTTP 200 lifecycle state | Your Cloud PC is powered off. Contact the lab administrator. | Later; no client power control |
| VM-02 | HTTP 200 readiness state | Your Cloud PC is starting or is not ready yet. | Retry status after backoff |
| VM-03 | HTTP 200 UNKNOWN / 503 | Your Cloud PC cannot be reached right now. | Retry status; no automatic grant |
| GRANT-01 | HTTP 403/409/503 | We could not authorize a session. Try again. | Fresh request only |
| GRANT-02 | HTTP 410 or WSS 4402 | This session request expired. Try connecting again. | Fresh grant only |
| TUNNEL-01 | HTTP 503 or WSS 4404 | The secure connection service could not be reached. | Bounded retry with a fresh grant |
| RDP-CERT-01 | WSS 4405 / session failure | The Cloud PC security certificate could not be verified. Connection blocked. | No user override; administrator action |
| RDP-AUTH-01 | Session/NLA failure | Windows sign-in failed. Check your separate lab credentials. | Re-prompt; new grant |
| RDP-01 | WSS 4408 / session failure | The Cloud PC could not complete the remote desktop connection. | Retry after status check |
| SESSION-01 | WSS 4406 | Connection lost. Reconnecting securely… | New grant and assignment check |
| SESSION-02 | WSS 4403 | Your session was ended by the administrator or access policy. | No retry until access revalidated |
| SESSION-03 | WSS 4407 / cleanup state | The session did not close cleanly. Reopen only after cleanup is verified. | Block duplicate session until cleanup completes |

The HTTP status values above are proposed transport mappings; the PRD-defined error codes and text are normative. Do not return a new user-facing error code. A low-level failure that does not fit a specific code maps to the closest PRD catalogue entry while retaining its detailed cause only in redacted server diagnostics.

## 8. Rate, concurrency and resource limits

- Session grant limit: 5 requests per minute per authenticated user.
- Concurrent sessions: one active RDP session per student.
- Global pilot concurrency: configured not to exceed owner-approved VM/session capacity; with the four approved R1 endpoints, recommended initial cap is 4, not 5.
- Status probes: per-user and per-source limits; cache only within the PRD's 15-second staleness window and never use cache to bypass the grant-time check.
- All limits are shared across broker instances or enforced by a globally consistent store; per-process counters alone are insufficient where multiple replicas exist.
- Queue, frame/message, total byte, idle, connect, pong and duration bounds are required.
- Denials use generic responses to reduce student/assignment enumeration.
- Grant consumption and active-session slot allocation must be atomic.

## 9. Audit events

Allowed event types: `GRANT_ISSUED`, `GRANT_DENIED`, `RELAY_OPENED`, `RELAY_CLOSED`, `SESSION_REVOKED`, `ASSIGNMENT_CHANGED`, `CERTIFICATE_REJECTED`, `RATE_LIMITED`, `CLEANUP_FAILED`.

Allowed fields: opaque request/session correlation ID, coarse time, PRD error code, outcome, duration, broker build, and non-identifying rate/concurrency bucket. Do not record JWTs, grant values or hashes, passwords, email, names, student IDs, VM UUIDs, addresses, hostnames, screen data or raw RDP payload.

## 10. Contract-test checklist

- [ ] JWT signature, issuer, audience, expiry and permitted algorithm are checked; invalid/expired token maps to AUTH-01.
- [ ] Session `session_id` is checked at grant time; missing/revoked/unknown session fails closed.
- [ ] Client request has no VM ID/name/hostname/IP/port/guest credential field; unknown body properties are rejected.
- [ ] Zero, duplicate, inactive and unmapped assignments never select a first row or alternate VM; VM4 remains denied while excluded.
- [ ] Status reports the exact lifecycle and RDP transport enums separately; timeout returns UNKNOWN; probe never exceeds 1500 ms.
- [ ] Status sample becomes stale after 15 seconds; Connect forces a fresh server-side check.
- [ ] Grant has >=256-bit entropy, <=60-second TTL, correct session/assignment binding and only hashed storage.
- [ ] Grant consume is atomic; replay and concurrent double-consume fail.
- [ ] Rate test proves no more than 5 user grant requests/minute; concurrency test proves one per user and configured pilot cap.
- [ ] Grant response includes the correct approved RDP certificate fingerprint and no VM endpoint or identity.
- [ ] WSS upgrade requires grant in Authorization header; query-string grants fail.
- [ ] WSS accepts binary frames only; text messages, invalid subprotocol and oversized messages close safely.
- [ ] Binary data stream remains byte-order correct across arbitrary WebSocket message segmentation/coalescing.
- [ ] Per-direction queues are bounded; backpressure pauses or closes; EOF/half-close/cancel/timeout clean both ends.
- [ ] Ping/pong, idle, connect and maximum session duration limits are exercised.
- [ ] Revocation is checked every <=15 seconds during relay; test measures close within <=30 seconds.
- [ ] Health/readiness endpoints reveal no dependency names, VM inventory, hostnames, IPs or secrets.
- [ ] Each error code maps exactly to one PRD §12 catalogue code/copy; no extra UI error is introduced.
- [ ] Log/trace/metrics scan proves no tokens, credentials, student identifiers, private endpoints, screen data or RDP content are recorded.
- [ ] Certificate mismatch, missing pin, rotation overlap/expiry and rollback all fail or succeed according to the approved policy, never accept-all.
- [ ] Disconnect and broker restart leave no upstream socket, relay slot or raw grant usable.


## 9. College Administrator control plane — proposed G1 contract

**Status:** Proposed capability groups only; routes and JSON schemas are NOT FROZEN and are not claimed to be deployed. The current OpenAPI file continues to describe the student-facing status/grant/relay contract until the admin API design is reviewed.

### 9.1 Authorization requirements

Every admin request MUST:
- carry the current Supabase user access JWT over HTTPS;
- validate signature, issuer, audience, expiry, subject and active session_id;
- resolve College Administrator authority from the private.app_user_roles table on every privileged request; this proposed table is server-managed, RLS-protected and not directly writable/readable by clients. Never trust user_metadata, app_metadata as the authoritative privilege decision, email domain, request body role or client navigation;
- reject suspended/disabled accounts and stale/revoked sessions;
- enforce the one-college prototype scope and action-specific policy;
- require recent MFA/step-up assurance for account disabling, VM lifecycle actions and session termination;
- create a safe audit event for both allowed and denied privileged operations.

Student endpoints remain student-scoped and MUST NOT gain admin behavior through optional request fields.

### 9.2 Proposed operation groups

| Capability | Proposed route shape | Purpose / boundary |
|---|---|---|
| Admin overview | GET /v1/admin/overview | Safe host/VM counts, pending invitations, active sessions and health summary |
| Student directory | GET /v1/admin/students | List only this college's student records and account/assignment states |
| Provision student | POST /v1/admin/students | Create roster-first record and initiate server-side Auth invitation; request contains no password |
| Resend invitation | POST /v1/admin/students/{studentId}/resend-invite | Reissue eligible expired/failed invitation; idempotent/rate-limited |
| Student state | PATCH /v1/admin/students/{studentId} | Suspend/reactivate/disable under lifecycle policy; revoke affected sessions |
| VM inventory | GET /v1/admin/vms | Return only allowlisted VM identifiers/display names and safe lifecycle/readiness data |
| Assignment | PUT /v1/admin/assignments/{studentId} / DELETE same | Assign one eligible VM or remove assignment; conflict checks and audit |
| VM lifecycle | POST /v1/admin/vms/{vmId}/actions | Allowlisted start, graceful-shutdown, restart or maintenance action; no arbitrary command/endpoint |
| Active sessions | GET /v1/admin/sessions | Return safe operational metadata only |
| Terminate session | POST /v1/admin/sessions/{sessionId}/terminate | Invalidate grants, close relay and release slot |
| Audit activity | GET /v1/admin/audit-events | Read-only, redacted, role-protected activity |

These are route proposals to support design and estimation. Before implementation, freeze request/response schemas, status/error mappings, idempotency, pagination, limits, MFA policy, audit fields and rollback semantics in a reviewed API/OpenAPI change.

### 9.3 Provisioning transaction

The backend must create/confirm the private roster row before calling the Supabase Auth Admin invite operation. The Admin API secret is server-only. The operation must be idempotent across retry/timeout, must not create duplicate profiles, and must surface invitation delivery state honestly. The existing roster-gated Auth trigger must be integration-tested with an invited Auth user before the provisioning flow is enabled.

### 9.4 Server/VM action boundary

The control plane maps a request to a fixed host/VM inventory entry and a predefined lifecycle action. It never accepts arbitrary shell text, raw hostnames, user-provided IP addresses, ports or libvirt arguments. The trusted host agent executes the operation under least privilege, returns a bounded result, and records action correlation. The Android app has no SSH credential and no direct hypervisor route.

### 9.5 Connection support and revocation

The admin session view exposes only student label, assigned VM display label, session start/duration, safe state and opaque correlation ID. A termination request invalidates the grant/session and closes the WSS relay; the UI may show TERMINATING until the broker confirms closure. Do not display guest passwords, tokens, grants, RDP payloads, private endpoints or desktop content.
