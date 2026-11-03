# MobiDesk — Technical Specification

**Version:** 1.0 · **Date:** 3 November 2026  
**Normative source:** MobiDesk PRD v1.2.  
**Scope:** Documentation for canonical repository master43721/mobidesk-android only.  
**Status:** Implementation specification. Unproven integration points are marked UNKNOWN/BLOCKING. No application code is changed by this document.

## 1. Specification rules

- PRD v1.2 defines desired behavior. Current code and third-party source define available implementation evidence, not product behavior.
- Every statement about the present repository or pinned third-party implementation is labelled with evidence and commit/file.
- Package/module names below are target logical boundaries; they are not claims that these packages/classes already exist.
- No exact FreeRDP method, Intent extra, native callback return value or transport capability is assumed beyond the exact pinned-source observations recorded in §7.
- No production broker hostname, private VM endpoint, credential, student identifier, signing key or service secret belongs in this document.
- Any source, Gradle, C/C++, submodule or CI change is outside this documentation-only change and requires a separate reviewed implementation change.

## 2. Current repository baseline relevant to implementation

- [VERIFIED: settings.gradle at main@a6c7282b4652b10b561ca0578c006c6d4b71cbc3] The current Gradle project includes :app and :freeRDPCore; the implementation is a native Kotlin/Jetpack Compose Android application.
- [VERIFIED: app/build.gradle and build.gradle at main@a6c7282b4652b10b561ca0578c006c6d4b71cbc3] Current Android build uses minSdk 29, compileSdk 37, targetSdk 37, Java/Kotlin target 11 and ABIs arm64-v8a and x86_64.
- [VERIFIED: .gitmodules and repository gitlink at main@a6c7282b4652b10b561ca0578c006c6d4b71cbc3] FreeRDP is pinned under third_party/FreeRDP at d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1, the upstream signed release commit identified as v3.32.0.
- [VERIFIED: app/build.gradle at the same commit] Current app dependencies include Ktor Android client 3.4.3, but the inspected dependency list does not explicitly include a Ktor WebSockets client artifact.
- [VERIFIED: MobiDeskRepositories.kt at the same commit] Current student workspace queries select the current user's student profile and RLS-filtered VM summary. A broker API client, live status repository, grant coordinator, MobiDesk RDP Activity and protected raw-WSS bridge are not present in the inspected application source.
- [VERIFIED: AndroidManifest.xml at the same commit] The manifest disables cleartext traffic. Existing upstream FreeRDP activities are non-exported; a MobiDesk-owned RdpSessionActivity is not currently declared.
- [VERIFIED: app/build.gradle at the same commit] Release has no explicit owner signing configuration in the inspected Gradle file. The build configuration is not evidence of an owner-signed release.

The current baseline does not satisfy an end-to-end R1 connection. The modules below describe the target design.

## 3. Target module and package layout

Keep the existing Gradle project/module shape unless an implementation decision approves a separate module. The package tree below is a logical boundary inside the app; it does not require immediate package moves.

~~~text
com.mobidesk.android
├── app
│   └── MobiDeskApplication
├── auth
│   ├── AuthSessionProvider
│   └── SupabaseAuthRepository
├── data
│   ├── student
│   │   ├── StudentProfileDto
│   │   └── AssignmentDto
│   └── broker
│       ├── BrokerClient
│       └── BrokerDtos
├── domain
│   ├── identity
│   │   ├── StudentProfile
│   │   └── AssignmentState
│   ├── status
│   │   ├── VmLifecycle
│   │   └── RdpTransportState
│   └── session
│       ├── SessionPhase
│       ├── SessionFailure
│       └── SessionPolicy
├── repository
│   ├── StudentWorkspaceRepository
│   └── StatusRepository
├── session
│   ├── SessionCoordinator
│   ├── SessionLifecycleOwner
│   └── SessionCleanup
├── security
│   ├── GuestCredentialBuffer
│   ├── CertificatePinPolicy
│   └── DeviceIntegrityPolicy
├── rdp
│   ├── RdpSessionActivity
│   ├── RdpRenderTarget
│   ├── FreeRdpAdapter
│   └── RdpInputMapper
├── transport
│   ├── RelayWebSocket
│   ├── LocalBridge
│   ├── ByteStreamPump
│   └── BoundedByteQueue
├── telemetry
│   ├── TelemetryEmitter
│   └── SessionTelemetryEvent
└── ui
    ├── auth
    ├── workspace
    ├── status
    ├── credentials
    └── session
~~~

### Dependency rules

| Layer | May depend on | Must not depend on |
|---|---|---|
| Compose screens / ViewModels | Domain types, use cases, UI state | FreeRDP native objects, VM host routes, grant storage |
| Domain | Kotlin standard types and declared interfaces | Android Activity/View, Supabase SDK, Ktor, FreeRDP |
| Repositories | Auth provider, Supabase or BrokerClient adapters | Compose rendering |
| SessionCoordinator | Domain/session ports, BrokerClient, transport adapter, FreeRdpAdapter | Private VM endpoint selection from UI |
| RdpSessionActivity | SessionCoordinator, RdpRenderTarget, input overlay | Supabase service-role credentials, VM address |
| FreeRdpAdapter | Exact pinned FreeRDP Android API and native context lifecycle | Direct persistence of bookmark/password data |
| LocalBridge / RelayWebSocket | Socket/stream APIs, grant handoff from coordinator | Assignment selection or guest credential storage |
| TelemetryEmitter | Explicit allowlisted event model | Raw headers, RDP payload, credentials, screen content |

No UI composable owns a native FreeRDP context. No domain model contains a host/IP/port that came from user input.

## 4. Class responsibilities and interface contracts

| Class / contract | Responsibility | Required invariant |
|---|---|---|
| SessionCoordinator | Own one connect attempt from status recheck through grant, relay, RDP/NLA, connected, reconnect and teardown | One active attempt per student; every terminal path calls idempotent cleanup |
| RdpSessionActivity | Dedicated non-exported native window, owns render target and native toolbar | Adds FLAG_SECURE; receives no JWT, raw grant, VM endpoint or password through Intent/Bundle |
| RdpRenderTarget | App-owned abstraction for renderer attach/detach, viewport, resize, invalidation, frame timing and close | Renderer surface lifetime is distinct from the coordinator's logical session; actual FreeRDP rebind capability is UNKNOWN until spike |
| BrokerClient | Authenticated HTTPS status and grant calls; safe error decoding | Status/grant requests contain no client-chosen VM identifier or endpoint |
| StatusRepository | Fetches fresh status, enforces 15-second staleness and blocks Connect on UNKNOWN/not-ready | UI cannot infer RUNNING/RDP readiness from assignment existence |
| CredentialPrompt | Prompts for separate Windows username/domain/password before grant | Credentials are never serialized, persisted or sent to broker |
| LocalBridge | Connects the FreeRDP local byte stream to the broker WSS relay | Authenticated peer/descriptor or proven alternative; no unauthenticated localhost listener |
| TelemetryEmitter | Emits stage duration, PRD error code and safe result metadata | Schema allowlist contains no direct student ID, JWT, grant, password, endpoint or screen pixels |
| AuthSessionProvider | Supplies current Supabase access JWT and exposes sign-out/session state | No service-role/secret key in the Android client |
| AssignmentRepository | Loads own profile/assignment display model through RLS | Zero/multiple assignment blocks Connect; never selects first row |
| FreeRdpAdapter | Wraps pinned Java/JNI FreeRDP functions and callbacks | No upstream symbol/argument assumptions; certificate policy is fail-closed |
| SessionCleanup | Coordinates cleanup across input, renderer, native context, bridge, WSS, grant and secrets | Idempotent; called after cancellation, process/activity loss, network loss, NLA failure and normal disconnect |

### SessionCoordinator contract

Input:
- authenticated user session provider;
- a fresh VmStatus obtained through BrokerClient;
- user-entered GuestCredentialBuffer;
- cancellation/lifecycle signals.

Output:
- a StateFlow/observable state containing only phase, safe error code/message, opaque attempt reference and connection timings;
- no raw JWT, grant, guest password or private endpoint is exposed to UI state.

The coordinator is serialized: a second Connect request while PREPARING, CONNECTING, CONNECTED, RECONNECTING or CLOSING is rejected as a duplicate attempt. It does not create parallel native contexts.

### BrokerClient contract

- Adds Supabase access token to the Authorization header for GET /v1/vm-status and POST /v1/session-grants.
- Grant request body is empty; no VM selector or guest credentials.
- Uses TLS validation and bounded connection/request timeout.
- Decodes only the PRD §12 error codes; unknown internal error is mapped to a safe catalogue code, never surfaced raw.
- Grant response is held in memory for the one connection attempt, consumed once and cleared on all exit paths.
- A WSS client is a separate adapter. [VERIFIED: app/build.gradle at main@a6c7282] Ktor Android client 3.4.3 is present; an explicit Ktor WebSocket client dependency is not listed in the inspected app dependencies. Select a compatible implementation in a separate reviewed source/build change. This document does not add that dependency.

## 5. Connection state machine

~~~mermaid
stateDiagram-v2
    [*] --> IDLE
    IDLE --> CHECKING_STATUS: Connect / create attempt ID
    CHECKING_STATUS --> PROMPTING_GUEST_CREDENTIALS: assigned + RUNNING + RDP_NEGOTIATION_OK + fresh
    CHECKING_STATUS --> FAILED: UNKNOWN / stale / stopped / assignment error
    PROMPTING_GUEST_CREDENTIALS --> REQUESTING_GRANT: submit separate Windows credentials
    PROMPTING_GUEST_CREDENTIALS --> IDLE: cancel / clear credential input
    REQUESTING_GRANT --> OPENING_WSS: valid one-use grant
    REQUESTING_GRANT --> FAILED: auth / assignment / rate / readiness denial
    OPENING_WSS --> STARTING_LOCAL_TRANSPORT: WSS upgraded and authenticated
    OPENING_WSS --> FAILED: tunnel / grant / certificate-policy setup failure
    STARTING_LOCAL_TRANSPORT --> NEGOTIATING_RDP_NLA: protected byte stream ready
    STARTING_LOCAL_TRANSPORT --> FAILED: local peer auth / stream setup failure
    NEGOTIATING_RDP_NLA --> CONNECTED: RDP negotiation + NLA + first desktop frame
    NEGOTIATING_RDP_NLA --> FAILED: RDP / certificate / guest auth failure
    CONNECTED --> RECONNECTING: network interruption / stream loss
    CONNECTED --> CLOSING: disconnect / 5s background timeout / revocation
    RECONNECTING --> CHECKING_STATUS: bounded retry, new attempt + new grant
    RECONNECTING --> CLOSING: retry exhausted / revoked / assignment changed
    FAILED --> CLOSING: cleanup begins
    CLOSING --> IDLE: cleanup verified
    CLOSING --> FAILED: cleanup cannot be confirmed
    FAILED --> [*]: report safe error after cleanup result
    IDLE --> [*]
~~~

### Entry/exit actions

| State | Entry actions | Exit condition |
|---|---|---|
| IDLE | No active RDP context/relay/bridge/guest secret; display dashboard | User taps Connect |
| CHECKING_STATUS | Mint opaque attempt ID; fetch status; verify fresh sample and unique assignment | Only RUNNING + RDP_NEGOTIATION_OK proceeds; stale/unknown/blocked state fails |
| PROMPTING_GUEST_CREDENTIALS | Show guest-account prompt; explain not college password; collect username/domain/password in transient buffer | Submit -> REQUESTING_GRANT; cancel -> clear input and IDLE |
| REQUESTING_GRANT | Request grant using Supabase JWT only; no VM selector; recheck status on broker | Grant response accepted only if <=60s and pin present |
| OPENING_WSS | Open WSS using grant Authorization header; do not put grant in URL; validate TLS | HTTP 101 + one-use consume -> STARTING_LOCAL_TRANSPORT |
| STARTING_LOCAL_TRANSPORT | Acquire selected protected descriptor/socket; start bounded stream pumps | Bridge ready -> NEGOTIATING_RDP_NLA; failed peer proof -> cleanup |
| NEGOTIATING_RDP_NLA | Initialize exact FreeRDP v3.32.0 adapter; pass ephemeral guest credentials via verified callback; validate RDP certificate against grant pin | NLA success + first frame -> CONNECTED |
| CONNECTED | Enable input; emit first-frame and FPS stage events; enforce screen-lock/background policy | User disconnect, revocation, lifecycle timeout, or network failure |
| RECONNECTING | Immediately block input; release current grant/relay/bridge; bounded retry with new status and grant | Return to CHECKING_STATUS or close after retry deadline |
| FAILED | Freeze user input; map failure to PRD §12 catalogue; preserve only safe request ID | Always enters cleanup |
| CLOSING | Idempotent cleanup described below; ask broker to close/revoke as best effort | IDLE only when local resources are gone and remote slot closure is confirmed or timeout recorded |

### Mandatory cleanup order

1. Atomically mark the attempt CLOSING and reject new input.
2. Cancel queued pointer/keyboard work and detach input from the FreeRDP instance.
3. Cancel or interrupt any FreeRDP connect/worker operation using the exact supported API; wait within a bounded deadline.
4. Close FreeRDP protocol context and release its session instance once worker threads have stopped.
5. Close both directions of LocalBridge; stop reader/writer workers; release descriptors, socket, queues and temporary handshake state.
6. Close WSS and wait for bounded close completion.
7. Invoke the idempotent broker session-close/cancel path if exposed; server-side grant/session expiry remains authoritative.
8. Clear Kotlin/Java mutable credential buffers and any app-owned native credential copies.
9. Detach/free bitmap/GDI render resources and callbacks; clear lifecycle timers/wake locks.
10. Emit a safe cleanup event; if server slot closure cannot be confirmed, retain a non-secret cleanup status and block a new session until status reconciliation.

Cleanup MUST be idempotent and safe after partial initialization. A thrown exception in one cleanup stage must not skip subsequent stages.

## 6. LocalBridge design

### 6.1 Preferred transport order

1. **Socketpair / connected native descriptor.** Preferred if a reviewed custom FreeRDP transport hook can accept the connected file descriptor without changing protocol semantics.
2. **App-private Unix-domain socket.** Require correct private directory/socket permissions and peer UID verification (for example, SO_PEERCRED where the chosen Android API/implementation exposes it); authenticate before forwarding.
3. **Loopback TCP fallback.** Allowed only if both the client and FreeRDP integration can implement a one-time authenticated handshake that is removed before raw RDP bytes reach FreeRDP.

A random port is not authentication. If the exact pinned client cannot prove peer identity or a one-time secret handshake, the fallback is prohibited.

### 6.2 Pinned-version API observation

- [VERIFIED: FreeRDP LibFreeRDP.java at upstream commit d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1] The inspected Android Java façade exposes native methods for freerdp_new, freerdp_free, freerdp_parse_arguments, freerdp_connect, freerdp_disconnect, graphics update, cursor/key input, clipboard operations and monitor layout. The inspected method list does not expose a Java method accepting an arbitrary connected socket/file descriptor.
- [VERIFIED: same pinned source, LibFreeRDP.setConnectionInfo] The existing adapter builds FreeRDP arguments from a BookmarkBase, including /v:<hostname>, /port:<port>, NLA selection, and may add /u:, /d:, /p: from bookmark fields.
- [VERIFIED: same pinned source, SessionActivity.processIntent] Upstream SessionActivity supports URI input, PARAM_INSTANCE = "instance", and PARAM_CONNECTION_REFERENCE = "conRef"; connection-reference logic can construct/resolve hostname or bookmark targets. MobiDesk MUST NOT accept a user-selected hostname/reference or pass a VM endpoint via this path.
- [VERIFIED: same pinned source, LibFreeRDP.setConnectionInfo] The inspected command construction adds /cert:ignore; the upstream SessionActivity also checks an AcceptAllCertificates preference before invoking verification dialogs. The exact CLI flag semantics and native certificate callback return semantics must be proven. MobiDesk MUST NOT inherit a trust-all preference or ship until the pinned path is proven to reject an untrusted/mismatched certificate.
- [UNKNOWN / BLOCKING FOR G3] The inspected Java façade does not show the custom socket-descriptor injection needed to feed the broker's raw byte stream directly. A lower-level native extension or another proven transport adapter may be required. Do not assume it exists.
- [VERIFIED: same pinned source, SessionView.java] Upstream SessionView extends Android View, renders a bitmap-backed GDI surface through Canvas, and receives surface updates/resizes from its session. It is not a SurfaceView in the inspected implementation.
- [VERIFIED: same pinned source, SessionActivity.onDestroy] Upstream SessionActivity disconnects and frees its FreeRDP session on Activity destruction. Its current activity lifecycle is therefore not evidence of independent session/renderer lifetime.
- [VERIFIED: same pinned source, android_disp.c, android_freerdp.c and Android CMakeLists.txt] The Android client sources include the DISP channel implementation and a freerdp_send_monitor_layout wrapper; the CMake source list includes android_disp.c. This proves a display-layout code path exists in the checked-in upstream source, not that every guest supports resizing or that a live session can move between Android displays.
- [VERIFIED: same pinned source, LibFreeRDP.java] The upstream Android UI listener includes a StringBuilder username/domain/password authentication callback and certificate verification callbacks with a fingerprint argument. This does not prove secure erasure or the callback's accept/reject result semantics.

### 6.3 Threading and queue design

- One coordinator owns bridge lifecycle and cancellation token.
- Use a single reader and a single writer per direction unless a proven implementation requires a different model. Preserve order; do not have concurrent writers reorder chunks.
- Chunk size is transport framing only; no chunk is assumed to align with an RDP PDU.
- Use bounded per-direction queues. Initial proposed defaults are in API_CONTRACT.md (1 MiB queue, high-water 768 KiB); validate via RDP traffic and memory tests.
- When outbound queue crosses high-water, pause reads from that producer. On drain timeout, close session instead of unbounded buffering.
- On EOF, propagate stream termination to the other side using the proven half-close mechanism. If the chosen WebSocket/FreeRDP adapter cannot represent half-close, close both sides and require a fresh grant for reconnect.
- Every worker must observe cancellation, use finite read/write/connect timeouts, and release its socket/channel in finally-equivalent cleanup.
- No Android main/UI thread performs socket blocking, native connect, wait-for-disconnect or buffer-drain work.

### 6.4 Loopback fallback preconditions

Before approving TCP fallback, the spike must prove:
- FreeRDP can connect to the bridge endpoint without taking an arbitrary user value.
- The legitimate FreeRDP peer can present a one-time high-entropy token in a bridge handshake.
- The bridge can validate the handshake before forwarding data and can strip it from the stream.
- Listener accepts one authenticated connection and closes immediately after accept.
- A same-device malicious app attempting early connect, port scan, token replay and second connection is rejected.
- Timeout, cancellation and process-death tests release listener/socket/secret.

If the FreeRDP API cannot attach the handshake or the bridge cannot identify the peer, TCP fallback fails the security requirement and must not ship.

## 7. FreeRDP integration plan and spike procedure

### 7.1 Exact version source map

The following observations refer only to upstream gitlink commit d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1:

| File | Verified source observation | Design implication |
|---|---|---|
| client/Android/Studio/freeRDPCore/src/main/java/com/freerdp/freerdpcore/presentation/SessionActivity.java | Existing upstream activity; processes connection URI, conRef or instance; connects from bookmark/URI; binds SessionView; disconnects/frees session in onDestroy | Do not pass user-controlled URI/hostname/reference; isolate MobiDesk lifecycle from this Activity behavior |
| .../presentation/SessionView.java | Native Android View with bitmap-backed GDI surface; onSurfaceChange attaches SessionState bitmap; Canvas paints bitmap | RdpRenderTarget may wrap the exact pinned view if safe/API-visible; do not assume SurfaceView |
| .../services/LibFreeRDP.java | Native façade for session creation/free/parse/connect/disconnect/updateGraphics/input; credential and certificate callbacks | Build a narrow app-owned FreeRdpAdapter; trace callback thread and object lifetimes |
| .../android_freerdp.c | Android JNI graphics update and desktop resize callbacks; GDI surface resize; DISP/RAIL/CLIPRDR channel callbacks | Establish bitmap and callback threading/resize behavior; disable out-of-scope channels |
| .../android_disp.c | Implements SendMonitorLayout on DISP client channel | A display-layout path exists; prove negotiated support and correct call sequence |
| .../CMakeLists.txt | Includes android_disp.c in native source list | Source is compiled in the upstream Android native build definition; inspect actual APK symbols as additional evidence |

### 7.2 Required spike sequence

**TS-RDP-01 — API inventory**
1. Check out exact gitlink and record SHA/tag.
2. Read Java façade, JNI method table, native initialization, session context, CMake configuration and Android manifest.
3. Enumerate all entry points for create/configure/connect/disconnect/free, credentials, certificate verification, graphics updates, resize, keyboard/pointer and monitor layout.
4. Record which APIs accept host/port only, and whether any raw descriptor/transport callback exists.
5. Deliver a source map with exact file/line references; no method signatures copied from upstream master.

**TS-RDP-02 — Transport**
1. Prove whether a connected FD, socketpair or Unix socket can be injected into the native FreeRDP context.
2. If no supported API exists, evaluate a minimal app-owned native transport adapter without modifying the pinned submodule. Do not invent an API or modify submodule in this docs task.
3. Build an echo harness with byte patterns split at arbitrary offsets and coalesced into different WSS messages.
4. Verify ordered byte fidelity in both directions, EOF/half-close, cancellation and bounded queue/backpressure.
5. Only then connect the transport to an approved controlled test VM.
6. Verify no RDP bytes or credentials leak to public endpoints, logs or unrelated local apps.

**TS-RDP-03 — Credential and certificate callbacks**
1. Verify exact Java/JNI callback signatures and which thread invokes them.
2. Determine when the StringBuilder credential arguments are read/copied/cleared by native FreeRDP.
3. Test a success and failure NLA flow with fake test credentials, then inspect app/native copies during teardown.
4. Verify exact OnVerifyCertificateEx and changed-certificate callback return semantics.
5. Inspect /cert:ignore argument effect and all accept-all settings/preferences. Prove trusted fingerprint succeeds and changed/untrusted/missing pin fails closed.
6. Remove any upstream accept-all preference path from the MobiDesk wrapper configuration. No “accept anyway” UI.

**TS-RDP-04 — Render target and resize**
1. Verify whether the pinned SessionView is accessible/reusable as an embedded view from the app module.
2. Trace callback-to-bitmap update and UI-thread invalidation ordering.
3. Verify graphics resize allocates/replaces bitmap and input coordinate dimensions atomically.
4. Inspect Android DISP client channel, CMake inclusion and freerdp_send_monitor_layout wrapper.
5. Connect to a test Windows VM and request two supported resolutions; record server negotiation and callback result.
6. Recreate Activity/renderer and test whether the same FreeRDP instance survives. The upstream SessionActivity currently frees its session on destroy; any different lifecycle requires an explicit app-owned proof.
7. If hot surface migration fails, document the PRD-permitted R2 reconnect-to-the-same-assigned-VM fallback and verify Windows session behavior.

**TS-RDP-05 — Channel and feature reduction**
1. Inspect the configured argument path. The observed upstream setConnectionInfo adds /clipboard; optional settings can enable drive, printer, sound, microphone or camera redirection.
2. Prove all R1-excluded channels are disabled in the MobiDesk configuration and release build.
3. Confirm no file transfer, clipboard or device forwarding occurs without separate security review.

### 7.3 Spike exit artifacts

Each spike report must include gitlink SHA, inspected file paths, API table, test harness source/revision, device/VM class, exact commands, logs with secrets redacted, pass/fail result, limitations, owner decision and updated G3 blocker state. A source-level symbol existing is not proof that the end-to-end path works.

## 8. Windows credential handling

- Guest prompt is a mandatory state before grant creation. UI text explicitly says “Windows lab credentials”, not college sign-in.
- Password never reaches BrokerClient, telemetry, Compose navigation arguments, URI, Intent extras, Bundle, SavedStateHandle, DataStore, SharedPreferences, database or clipboard.
- Prefer CharArray or a native mutable buffer; minimise conversion to immutable String. Android text widgets may internally create immutable text objects; document/measure the selected widget's behavior rather than promising perfect erasure.
- The exact pinned FreeRDP callback exposes StringBuilder username/domain/password in its Java UI listener interface. [VERIFIED: pinned FreeRDP LibFreeRDP.java at gitlink d27a4f7...] This is a mutable container but does not itself prove secure erasure. Trace native callback copies before deciding when to clear.
- Do not populate persistent BookmarkBase username/password fields. The upstream source constructs command arguments including /u:, /d:, and /p: when those bookmark fields are non-empty; such values can produce additional Java String copies.
- Clear input and app-owned mutable copies on user cancel, grant failure, native callback completion, NLA failure/success when no longer required, network failure, Activity destruction and process teardown.
- Do not promise managed-heap forensic zeroization. Guarantee avoidance of persistence/logging and clear app-owned mutable/native copies as early as the API permits.
- Wrong NLA credentials map to PRD RDP-AUTH-01; local retry is bounded and each retry gets a new broker grant.

## 9. Android Activity, UI and device security

- RdpSessionActivity must be explicit, non-exported, and launched only from the authenticated app.
- Do not place JWT, grant, password, VM ID, endpoint or connection string in Intent extras/deep link.
- The coordinator passes only a short-lived in-process session handle or non-secret opaque attempt ID.
- Add FLAG_SECURE before sensitive session content is rendered. Verify screenshots, recording surfaces where supported and recent-task previews.
- Root/unlocked-bootloader high-confidence signal blocks R1 session. UNKNOWN is not CLEAN; follow PRD's unsupported-device policy and block under the release rule.
- R1 is foreground-only. Start a five-second cancelable teardown after a true stopped/background state or screen lock. Rotation, multi-window/IME and configuration changes are not automatically backgrounding; implement state-aware lifecycle detection.
- Do not add a foreground service merely to preserve Wi-Fi WSS/RDP. Any future R2 USB/background service requires a separate Android foreground-service type/permission/platform review.
- Keep hardware keyboard and IME events mapped to FreeRDP input. The native session toolbar must not consume all keyboard focus while the desktop is active.

## 10. Broker and infrastructure design

### 10.1 Authorization path
1. Verify Supabase JWT signature, issuer, audience, expiry and subject.
2. Extract and validate session_id against active Supabase Auth session state using a server-only adapter.
3. Resolve a single active assignment from trusted DB data; no client-selected VM ID.
4. Resolve assignment to a server-only allowlisted VM UUID/endpoint; VM4 remains excluded.
5. Read VM lifecycle through a trusted host/libvirt adapter.
6. Probe RDP transport privately with the 1500 ms budget. TCP reachability is not NLA success; return RDP_NEGOTIATION_OK only after the defined bounded RDP negotiation check.
7. Enforce 5 grants/minute/user, one session/user, configured pilot cap, grant TTL and rate/buffer/queue limits.
8. Issue one-use grant bound to user/session/assignment/version/VM and certificate pin policy.
9. At WSS, atomically consume grant and open private TCP/3389 to the selected server mapping.
10. Revalidate active session and assignment every <=15 seconds; close relay <=30 seconds after revocation/change.

### 10.2 Session and assignment data model

[REPORTED: docs/SUPABASE_DISCOVERY.md historical snapshot dated 27 Oct 2026] The public schema is documented as students, vms, and student_vm_access; the discovery file records students(id,name,college,...), vms(id,name,...), and student_vm_access(student_id,vm_id,granted_at). This schema evidence is historical; revalidate constraints/migrations before implementation.

[VERIFIED: MobiDeskRepositories.kt at main@a6c7282...] The client currently loads students(id,name,college) filtered by the authenticated user ID and an RLS-filtered vms(id,name) list.

Target conceptual model:

~~~text
auth.users
  1 ───── 1 students
                |
                | student_id
                v
           student_vm_access
                |
                | vm_id
                v
               vms
~~~

Required invariants:
- students.id is the identity key for the authenticated subject.
- Assignment relation contains only owner-approved mapping rows.
- For an active R1 connection, the authenticated subject resolves to exactly one active assignment.
- Zero, duplicate, inactive or unmapped assignment returns fail-closed error.
- The query checks at most two candidate active assignments to detect ambiguity; it must not select the first and continue.
- VM4 remains excluded until OS/endpoint verification and owner/Lab IT approval.

[UNKNOWN] The current active-assignment uniqueness constraint, historical assignment revocation representation and exact lifecycle columns are not established by this read-only documentation review. Do not assert that a unique constraint already exists. A future schema decision may use a unique partial index or a transactional active-assignment model; decide only after defining how old assignments are revoked/archived. No database migration is included in this task.

RLS sketch (conceptual, not executable migration):
- Student can SELECT only students.id = auth.uid().
- Student can SELECT only assignments where student_vm_access.student_id = auth.uid().
- Student can SELECT only VM display rows referenced by that own assignment.
- Student cannot INSERT/UPDATE/DELETE assignments.
- Broker assignment resolution uses either the authenticated user context with equivalent RLS or a narrowly scoped server-side function that validates subject/session before resolving. No service-role key in APK.

### 10.3 Broker status, grant and audit model

Server-only grant record (conceptual fields):
- token_hash;
- subject_id and session_id;
- assignment reference/version;
- allowlisted VM mapping key;
- created_at/expires_at/consumed_at/revoked_at;
- relay state and opaque session correlation ID.

Never store raw grant. Never store guest Windows password in broker. No VM routing details in client DTO.

Safe audit events: grant issued/denied, relay opened/closed, session revoked, assignment changed, cert rejected, rate limited and cleanup failed. Events include opaque request/session ID, stage, timestamp, PRD error code, duration and outcome only.

## 11. Telemetry schema

Every event is allowlisted. Example logical schema:

| Field | Type | Meaning |
|---|---|---|
| eventTimeUtc | RFC3339 timestamp | Wall-clock time for event correlation |
| attemptId | random opaque ID | Per connection attempt, not derived from student identity |
| stage | enum | AUTH_RESTORE, STATUS, CREDENTIAL_SUBMIT, GRANT, WSS, LOCAL_BRIDGE, RDP_NEGOTIATION, NLA, FIRST_FRAME, FPS_WINDOW, INPUT_RESPONSE, CLEANUP |
| durationMs | non-negative integer | Stage duration using monotonic clock |
| result | enum | STARTED, SUCCEEDED, FAILED, CANCELED, TIMED_OUT |
| prdErrorCode | PRD §12 code or null | Safe mapped failure |
| retryIndex | bounded integer | Retry count |
| clientVersion | coarse version string | Build identification |
| deviceClass | coarse/consented model family | Do not include serial/device ID |
| fpsAverage | aggregate numeric or null | Window summary, not frame content |
| inputLatencyP90Ms | aggregate numeric or null | Scripted workload only |
| networkRttBucket | coarse enum | Under/over baseline bucket |
| cleanupResult | enum | CONFIRMED, PENDING, FAILED |

Forbidden: email/name/roll number/student ID, JWT/session token, grant/hash, password/username, VM ID/hostname/IP/port, screen/frame/audio/clipboard payload, raw request headers, RDP PDU, native pointer address, keystore material.

Record p50/p90 and sample count in reports. No adoption percentage/vanity metric is used for the small pilot.

## 12. Build, signing and reproducibility

- [VERIFIED: build.gradle and app/build.gradle at main@a6c7282...] Preserve minSdk29, compileSdk37, targetSdk37, Java/Kotlin target11, arm64-v8a and x86_64 unless a separately approved build change records reason and regression evidence.
- [VERIFIED: .github/workflows/android-ci.yml at the same commit] CI uses Java21 to run Gradle and the app targets Java/Kotlin bytecode 11; CI builds a debug APK and runs JVM tests. Physical-device RDP testing is not in this workflow.
- [VERIFIED: app/build.gradle at the same commit] No explicit owner release signingConfig is present in the inspected release block.
- [UNKNOWN] The owner-controlled release/upload key custody and release distribution channel are not yet approved.
- A reproducible build record must include repository commit, submodule gitlink, JDK/Gradle/AGP/Kotlin/SDK/NDK/CMake versions, dependency resolution, build command, artifact SHA-256 and signing certificate fingerprint.
- Never commit local.properties, Supabase service-role secrets, broker keys, Windows passwords or signing key material.
- Debug-signed artifacts are test artifacts only. They do not pass FR-REL-01.
- Release static checks cover demo/mock fallback, cleartext, trust-all certificate flags/preferences, sample endpoints, exported session activities, secret logging and excluded RDP channels.

## 13. Implementation dependencies and blockers

| ID | Dependency | Status | Exit evidence |
|---|---|---|---|
| TS-01 | Exact FreeRDP v3.32.0 raw transport / descriptor injection | BLOCKING / UNKNOWN | Working byte-stream proof and protected peer validation |
| TS-02 | Exact certificate callback and /cert:ignore behavior | BLOCKING | Matching pin accepted; wrong/missing/changed cert rejected |
| TS-03 | Bookmark/argument/credential handling | BLOCKING | No endpoint from Intent, no persisted bookmark password, safe callback lifecycle |
| TS-04 | Bitmap-backed SessionView integration and renderer lifecycle | UNKNOWN | Rebind/rotation/resize evidence or documented reconnect fallback |
| TS-05 | DISP/MS-RDPEDISP monitor layout | PARTIAL SOURCE EVIDENCE | Two-resolution guest test; R2 behavior decision |
| TS-06 | Supabase active session_id lookup | BLOCKING | Server query, least privilege and <=30sec revocation close test |
| TS-07 | WSS client transport dependency/engine | UNKNOWN | Compatible WebSocket implementation with Authorization header and binary stream proof |
| TS-08 | Assignment uniqueness and lifecycle constraints | UNKNOWN | Migration/schema review with zero/duplicate fail-close tests |

Do not declare G3 complete until every blocking dependency is closed with a reproducible test.
