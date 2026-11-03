# MobiDesk — Broker API Contract (R1 proposal)

**Version:** 1.2 · **Date:** 3 November 2026 · **Status:** Proposed contract; not evidence of a deployed service.

## Security boundary
The Android app authenticates the student with Supabase. The broker verifies the user access token, active session and assignment, then resolves the only permitted VM on the server. The client MUST NOT provide vm_id, VM name, hostname, IP, port, Windows password or hypervisor instruction as an authorization/routing input.

All control endpoints require HTTPS. RDP relay uses WSS. Broker-to-VM TCP/3389 is private-network only.

## Endpoints

### GET /health
Unauthenticated liveness only. Returns service liveness/version metadata; MUST NOT return VM inventory or environment secrets. It is not a readiness or authorization endpoint.

### GET /v1/vm-status
Requires a Supabase bearer access token. Derives exactly one active assignment from authenticated identity. Suggested response:
- assignment_state: ASSIGNED | NONE | AMBIGUOUS | INACTIVE
- vm_lifecycle: STOPPED | STARTING | RUNNING | STOPPING | MAINTENANCE | UNKNOWN
- rdp_readiness: BOOTING | RDP_PORT_REACHABLE | RDP_READY | RDP_UNAVAILABLE | UNKNOWN
- observed_at, expires_at, retry_after_seconds, opaque request_id

Bound each probe to 1500ms; mark status stale after 15s. A TCP/3389 success yields only RDP_PORT_REACHABLE. RDP_READY requires a validated RDP negotiation check or stronger approved signal. Never return private endpoints.

### POST /v1/session-grants
Requires the Supabase bearer token. Request body: empty JSON object (or no body). There is no vm_id field. The broker validates JWT signature/issuer/audience/expiry and checks session_id against active auth.sessions, resolves exactly one active assignment, applies rate and concurrency policies, and creates a one-use grant:
- cryptographically random secret (minimum 256 bits entropy);
- expires in 60 seconds;
- bound to authenticated user, VM assignment and Supabase session;
- only a cryptographic hash is stored;
- consumed atomically once.

Response: grant, expires_at, relay_path (path only), and opaque request_id. No VM IP, hostname, port, guest password or private routing metadata.

### GET /v1/rdp
WSS upgrade with one-use grant authorization. Validate before opening the private upstream connection. Enforce byte/frame limits, idle/total timeouts, backpressure and concurrency. Relay RDP bytes without pretending to be an RD Gateway or Windows authentication service. Invalidate grant on use. On close, release upstream socket and all session resources.

## Revocation
At grant issuance, reject revoked/expired sessions. During active relays, check auth.sessions and current assignment at intervals <=15s (or a push invalidation plus bounded polling fallback). Close the relay within 30s after session revocation or assignment change. Sign-out on the phone alone is not sufficient.

## Failure codes
AUTH_REQUIRED, SESSION_REVOKED, ASSIGNMENT_NOT_FOUND, ASSIGNMENT_AMBIGUOUS, ASSIGNMENT_CHANGED, VM_MAINTENANCE, VM_STOPPED, RDP_UNAVAILABLE, GRANT_EXPIRED, GRANT_USED, RELAY_LIMIT, RATE_LIMITED, CERTIFICATE_MISMATCH, INTERNAL_ERROR.

Do not include student data, VM endpoint, credentials, stack trace or secret material in error responses.

## Open implementation spikes
1. Verify the exact pinned FreeRDP v3.32.0 WSS/raw transport interface and required framing. A generic WebSocket URL is not proof of compatibility.
2. Define a standards-safe RDP readiness probe; TCP port open does not prove NLA succeeds.
3. Confirm Supabase service-role usage is isolated to the broker, never Android; implement assignment lookup under authenticated user context or a server-side authorization query with equivalent enforcement.
