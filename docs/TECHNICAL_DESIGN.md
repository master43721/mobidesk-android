# MobiDesk — Technical Design Document

**Version:** 1.0 · **Date:** 3 November 2026  
**Status:** Target design for review; not a record of deployed architecture.  
**System:** Native Android client, Supabase identity/data, session broker, private Windows VM network, pinned FreeRDP Android engine.

## 1. Design goals

1. Enforce one-student-to-one-assigned-VM routing at the trusted server boundary.
2. Keep Windows credentials separate from Supabase identity.
3. Relay RDP traffic securely without exposing guest endpoints or public TCP/3389.
4. Integrate with the pinned FreeRDP Android code through a version-verified adapter.
5. Maintain explicit session, network, native-resource and credential lifecycles.
6. Produce measurable and auditable pilot evidence.
7. Defer unproven Raspberry Pi dock architecture until a comparative experiment.

### Non-goals
R1 is not a hypervisor UI, remote shell, arbitrary RDP client, public RD Gateway, Windows password manager, multi-tenant VM marketplace, file transfer solution or dock streaming product.

## 2. Context and deployment topology

```text
                         CONTROL PLANE
+----------------------+   HTTPS   +--------------------------+
| Android app          |---------->| MobiDesk session broker  |
| - Compose dashboard |           | - JWT/session validation |
| - Auth session       |           | - assignment resolution  |
| - session coordinator|           | - readiness adapter      |
+----------+-----------+           | - one-use grant store     |
           |                       | - policy/rate limits      |
           | WSS binary relay      | - relay lifecycle        |
           +======================>| - audit/telemetry         |
                                   +-----+---------------+----+
                                         |               |
                                 Supabase Auth/RLS   Libvirt adapter
                                         |               |
                                   +-----v----+    +-----v------+
                                   | Supabase |    | KVM host   |
                                   | Postgres |    | VM state   |
                                   +----------+    +-----+------+
                                                         |
                                                  private TCP/3389
                                                         |
                                                  +------v------+
                                                  | Windows VM  |
                                                  | RDP + NLA   |
                                                  +-------------+

                         CLIENT DATA PATH
+----------------------+  protected local stream + WSS relay
| RdpSessionActivity   |<=====================================>
| FreeRDP v3.32.0      |
| RDP render/input     |
+----------------------+
```

The broker is the only component that knows the mapping from authenticated student to approved VM endpoint. The Android client receives an assignment display label and status but not routable host data. The broker's VM inventory is server-only.

## 3. Component design

### 3.1 Android application
Responsibilities:
- Supabase authentication and session restore.
- Own student profile and assignment display.
- User-scoped readiness requests.
- Connection credential prompt and explicit user confirmation.
- Session coordinator and transient state machine.
- Native FreeRDP Activity and controls.
- Safe error presentation and privacy-safe telemetry.

Dashboard ViewModels expose immutable UI state and invoke use cases. They do not import FreeRDP types or hold Windows password state. Ordinary Compose screen state is not a storage mechanism for a guest password.

### 3.2 Session coordinator
A single coordinator owns a session attempt. It must:
- allocate an opaque local session handle;
- collect guest credentials before requesting the grant;
- request the grant using the current Supabase access token;
- construct and validate the WSS connection;
- configure the FreeRDP adapter with grant-bound server identity policy;
- observe connection, NLA, first-frame and failure events;
- own cancellation and timeouts;
- release all native/network resources in deterministic order.

The coordinator should be single-session scoped. A second start while a session is preparing/active returns a defined conflict rather than creating an overlapping native session.

### 3.3 RdpSessionActivity
A dedicated, non-exported Activity owns the FreeRDP renderer and native session interaction. It is responsible for:
- attach/detach of the verified FreeRDP rendering surface;
- orientation, resize and window insets;
- keyboard focus and IME;
- touch-to-pointer mapping;
- pointer, wheel and modifier toolbar;
- connected/disconnected/error overlays;
- FLAG_SECURE;
- user disconnect and lifecycle teardown.

The session coordinator/session core must own protocol lifetime independently of a transient view where pinned FreeRDP allows it. If the exact upstream API does not allow safe renderer rebinding, R1 must use a documented no-recreation/no-rotation limitation or a controlled reconnect; it must not claim seamless surface migration without proof.

### 3.4 FreeRDP adapter
Encapsulate upstream APIs behind a small app-owned interface. Do not scatter FreeRDP library calls across UI code. Adapter owns:
- connection configuration;
- authentication and certificate callbacks;
- transport stream adapter;
- session start/stop;
- renderer surface attachment;
- keyboard/pointer input;
- protocol event normalization.

Names of upstream Java classes, callback fields, URI/Intent extras and surface methods must be traced against the pinned gitlink before coding. The adapter must not modify upstream sources to conceal an integration mismatch.

### 3.5 Broker
Broker responsibilities:
- validate Supabase JWT and active session;
- resolve exactly one active student-to-VM assignment;
- provide VM lifecycle and bounded RDP readiness;
- issue/consume one-use grants;
- maintain server-owned VM allowlist;
- open private guest sockets and relay RDP bytes;
- enforce quotas, rate limits, timeouts, revocation and audit;
- close relays and clear transient secrets/state on every exit.

The broker is not responsible for provisioning the Windows password. Windows NLA terminates at the VM.

### 3.6 Infrastructure adapters
- **Supabase adapter:** identity/session validation and trusted assignment query. Keep privileged credentials only server-side.
- **Libvirt adapter:** read-only VM lifecycle/status for R1. Power operations are outside the student flow.
- **Network adapter:** connect only to an allowlisted private endpoint and port 3389.
- **Certificate inventory:** trusted CA or per-VM SHA-256 pin plus owner-controlled rotation metadata.

## 4. Sequence design

### 4.1 Workspace and readiness
1. User signs into Supabase.
2. App loads own profile and assignment with RLS.
3. App requests broker status using bearer token and no VM ID.
4. Broker verifies token/session and resolves assignment.
5. Broker reads lifecycle through libvirt adapter and probes RDP reachability under 1500ms.
6. Broker returns independent lifecycle/readiness values and freshness timestamp.
7. App shows stale/unknown status honestly and enables connect only under approved state.

### 4.2 Grant and RDP
1. App opens the separate Windows credentials prompt.
2. Credentials remain transient and are not sent to the broker.
3. App requests POST /v1/session-grants with bearer token and no target field.
4. Broker verifies JWT, active auth session, exactly-one assignment, VM allowlist, readiness, quota and rate limit.
5. Broker issues random 256-bit+ one-use grant, 60-second TTL, bound to user/session/assignment. Only hash is stored.
6. App creates protected local FreeRDP stream and WSS connection using the grant in a header, not URL.
7. Broker atomically consumes the grant, resolves only server-side target, then connects to private TCP/3389.
8. FreeRDP performs RDP negotiation, validates the guest server identity, performs Windows NLA and receives desktop output.
9. Activity displays frames and maps controls.
10. On any terminal event, client closes FreeRDP, WSS, local stream and credentials; broker closes upstream and invalidates state.

### 4.3 Revocation
- Broker checks active Supabase session at grant creation.
- Active relays re-check session and assignment at an interval no greater than 15 seconds; push invalidation may accelerate but bounded polling is the fallback.
- Session revocation or assignment change causes WSS close within 30 seconds.
- Phone-side sign-out is not treated as sufficient server revocation.

## 5. State machines

### 5.1 Dashboard state
```text
INITIAL -> RESTORING_AUTH
RESTORING_AUTH -> SIGNED_OUT | LOADING_WORKSPACE | AUTH_ERROR
LOADING_WORKSPACE -> NO_ASSIGNMENT | ASSIGNED | WORKSPACE_ERROR
ASSIGNED -> REFRESHING_STATUS -> READY | NOT_READY | STATUS_UNKNOWN
Any state -> SIGNED_OUT on successful local logout
```

No default/sample VM is displayed in NO_ASSIGNMENT or WORKSPACE_ERROR. Stale status moves to STATUS_UNKNOWN until refreshed.

### 5.2 Session state
```text
IDLE
  -> PREPARING
  -> REQUESTING_GRANT
  -> CONNECTING_RELAY
  -> NEGOTIATING_RDP
  -> AUTHENTICATING_GUEST
  -> CONNECTED
  -> CLOSING -> CLOSED

Any non-terminal state may transition to FAILED -> CLOSING -> CLOSED.
CONNECTED may transition to RECONNECTING only under an explicitly supported resume design.
A lost grant is never silently reused; a reconnect requires a new authorization grant.
```

Invariants:
- At most one active session attempt per student.
- Grant is never persisted.
- Windows password is never sent to broker.
- A failure must enter cleanup, even when native callbacks throw or process lifecycle interrupts the UI.
- Certificate mismatch is terminal and non-retryable without operator remediation.

### 5.3 Broker grant lifecycle
```text
CREATED -> ISSUED -> CONSUMED -> ACTIVE_RELAY -> CLOSED
    |        |          |
    +------> EXPIRED    +-> REJECTED
Any state -> REVOKED on session/assignment invalidation.
```

Grant consumption must be atomic. A second concurrent consume must fail. On process restart, non-durable in-memory grants become invalid; clients request new grants.

## 6. Transport and protocol design

### 6.1 Control plane
HTTPS JSON calls: `GET /v1/vm-status`, `POST /v1/session-grants`, optional idempotent close endpoint. All are authenticated except liveness. Request IDs correlate safe logs.

### 6.2 Data plane
Custom WSS binary relay preserves RDP byte order:
- Upstream target is resolved from server-only allowlist and always port 3389.
- WebSocket frames are reassembled into a continuous byte stream. Frame boundaries do not map to RDP packet boundaries.
- Backpressure must pause reads or close peers rather than allow unbounded buffering.
- Enforce max frame, buffered bytes, idle timeout and maximum connection duration.
- TLS terminates at broker/proxy for WSS; RDP TLS/NLA occurs between FreeRDP and the Windows VM through the byte relay. Validate both security layers.
- Verify the exact FreeRDP v3.32.0 custom transport API. Do not configure an RD Gateway WebSocket mode as a substitute unless the wire protocol is actually the same.

### 6.3 Local FreeRDP stream
Preferred: socketpair or connected descriptor. Secondary: app-private Unix-domain socket with same-UID verification. Last-resort loopback TCP must use one-time random handshake secret, one accepted connection, listener closure, deadline and local hijack test. A random port alone does not authenticate the client.

### 6.4 Certificate identity
Use an approved internal CA or per-VM SHA-256 pin. No accept-all certificate preference in debug or release path that could reach real infrastructure. A fingerprint must be integrity-bound to the grant/session or obtained from a trusted pinned inventory. Document rotation and mismatch recovery.

## 7. Storage and secret lifecycle

| Data | Location | Lifetime | Prohibited |
|---|---|---|---|
| Supabase refresh/session token | Existing secure SDK-managed session storage | SDK lifecycle | Logs, analytics, Intent extras |
| Windows username/domain | In-memory session object only | Session attempt | Persistence unless later approved |
| Windows password | Mutable transient memory buffers | Minimum needed by NLA | Disk, broker, logs, clipboard, crash data |
| Session grant | In-memory only | <=60s and single use | URL, logs, durable storage |
| VM endpoint map | Broker-only config/store | Managed server lifecycle | Android app, user response |
| RDP CA/pin | Trusted managed inventory / grant-bound validated policy | Certificate validity and rotation | User-editable preference |
| Telemetry | Approved event sink | Owner-approved retention | Direct identifiers, tokens, password, screen data |

Do not overstate secure erasure in managed runtimes. Clear mutable buffers and native copies when no longer needed; prevent avoidable copies and ensure teardown runs on all known paths.

## 8. Concurrency, quotas and failure containment

Pilot defaults (must be configurable and load-tested):
- one active session per student;
- maximum five concurrent relays system-wide;
- grant TTL 60 seconds;
- status probe timeout 1500ms;
- session/assignment revalidation <=15s;
- revocation/assignment-change relay close <=30s;
- bounded inbound/outbound buffers;
- per-user grant rate default five requests per minute, tune only with abuse evidence;
- maximum relay duration and idle threshold set in broker configuration before pilot.

Use generic authentication/assignment denial responses to reduce account enumeration. Avoid retry storms with bounded backoff and jitter. Fail closed when Supabase session or assignment validation is unavailable.

## 9. Dock R2 architecture experiment

Do not choose dock architecture based on intuition. Build controlled prototypes:

**Option A — phone transcode:** phone runs FreeRDP, decodes desktop frames, encodes H.264 and streams to Pi display; Pi decodes and displays.

**Option B — Pi-native FreeRDP:** Pi runs RDP client/decoder/rendering locally; phone handles login, assignment authorization and session control.

Compare:
- end-to-end input-to-photon latency;
- sustained FPS and frame drops;
- phone/Pi CPU and memory;
- phone/Pi power draw and temperature;
- reconnect behaviour and USB/AOA complexity;
- security boundary, dock pairing and session revocation.

Use a dock-provisioned asymmetric key or securely provisioned device secret for challenge-response. AOA strings are discovery hints only. Explicit user confirmation is required for first pairing. Record results and select architecture in a new decision record.

## 10. Deployment topology and networking

- Public ingress terminates HTTPS/WSS at the approved broker edge.
- Broker application is bound to loopback/private interface behind that edge; the app port is not publicly exposed directly.
- Broker reaches VMs on a private interface/Tailscale or approved routed network; RDP port fixed to 3389.
- Firewall denies public inbound TCP/3389 and allows broker egress only to allowlisted VM endpoints.
- Guacamole's previous quick tunnel is a separate service and must not be reused for MobiDesk.
- Do not deploy until all enabled VM endpoint entries are current, certificate policy is known and the broker configuration passes fail-closed validation.

## 11. Threat cases and countermeasures

| Threat | Design response |
|---|---|
| Client changes target VM | No target fields; broker derives assignment |
| Grant replay | High entropy, atomic one-use, 60s expiry, hash at rest |
| JWT revoked during session | Auth-session/assignment recheck and bounded relay close |
| RDP MITM | CA/pin validation separate from WSS TLS |
| Local bridge hijack | Descriptor/Unix socket or authenticated one-use loopback handshake |
| Credential leakage | Separate Windows secret, volatile buffers, redaction and cleanup |
| Broker enumeration/DoS | Generic errors, per-user/source rate limits, capacity and buffer bounds |
| Public RDP exposure | Firewall and server-side private endpoint allowlist |
| Dock spoofing | R2 key-backed challenge-response; no trust in USB strings |
| Surface recreation breaks session | Separate protocol session from renderer if upstream permits; otherwise documented reconnect/limitation |

## 12. Verification and observability

Test layers:
1. Unit: domain state machines, freshness, expiry, error mapping, input transforms.
2. Broker contract: JWT/session, assignment, grant replay, allowlist, rate and relay boundaries.
3. Native adapter: FreeRDP callbacks, credential lifecycle, certificate decision and surface attach/detach.
4. Instrumentation: Activity navigation, keyboard/IME, touch, rotation, lifecycle, FLAG_SECURE and cleanup.
5. Fault injection: WSS loss, broker restart, Supabase outage, guest offline, slow/blocked sockets and assignment change.
6. Physical device: Windows NLA, certificate pin, 20 connection attempts, measured FPS/input latency and resource cleanup.

Performance target values and measurement conditions are normative in PERFORMANCE_PLAN.md, not repeated as claimed outcomes here.

Safe timing stages: status, grant, WSS upgrade, RDP negotiation, NLA, first frame, input response, close. Emit only opaque correlation ID, coarse client build, duration, stage and outcome. No credentials, tokens, endpoints, direct student identifiers or screen pixels.

## 13. Failure handling and rollback

On certificate mismatch, cross-student routing, session-revocation failure, public RDP exposure, bridge hijack or resource exhaustion:
1. stop new grants;
2. revoke/close active relays;
3. isolate affected VM or broker route when necessary;
4. preserve redacted audit evidence;
5. roll back to last reviewed build after verifying assignment and certificate policy;
6. record incident and corrective decision.

Never disable certificate validation, expose public RDP or silently redirect to a different VM as a workaround.

## 14. Decisions that remain deliberately open

- VM host/site and network path.
- Persistent vs ephemeral VMs and semester reset/deletion.
- Windows guest account provisioning policy.
- Physical reference Android device.
- Distribution and release-signing/key custody.
- Broker public hostname and stable WSS route.
- Telemetry retention and support permissions.
- Exact FreeRDP WSS transport API and renderer rebind semantics.

Each item requires an owner decision or spike evidence before dependent implementation proceeds.
