# MobiDesk — Product Requirements Document

**Version:** 1.2  
**Date:** 3 November 2026  
**Status:** Owner review draft; design recommendations are defined, owner ratification remains required.  
**Canonical repository:** https://github.com/master43721/mobidesk-android  
**Product:** Native Android Kotlin + Jetpack Compose client with the pinned FreeRDP Android engine.

## 1. Product summary

MobiDesk gives an authenticated student access to the Windows Cloud PC assigned to that student by the institution. The Android app is the student-facing client; a trusted server-side broker authenticates the user, resolves the assignment, issues a short-lived connection grant, and relays RDP traffic to the assigned VM over a private network.

MobiDesk is not a generic remote-desktop client, public VM marketplace, hypervisor administration console, Windows identity provider, or raw public RDP gateway. A student can connect only to the VM assigned to their authenticated identity.

## 2. Problem and outcome

Students need a dependable way to use institution-provided Windows lab resources from an Android device without exposing VM addresses or credentials and without requiring a full desktop computer at the client.

The pilot succeeds when an enrolled student can sign in, see their own assigned VM and truthful readiness state, authenticate to Windows using a separate guest credential, start a secure RDP session, interact with the desktop, and end the session with server-side resources and secrets cleaned up.

## 3. Users and roles

- **Student:** signs in, sees only their own profile and assignment, checks status, connects and disconnects.
- **Project Owner / Operator:** provisions users and VM assignments through trusted administrative processes; monitors service health and performs maintenance.
- **Lab IT:** consulted for Windows guest account policy, certificate lifecycle, VM operations and lab network controls. No undocumented administrative UI is assumed.

Self-registration is out of scope. Existing roster provisioning and Supabase row-level security remain authoritative.

## 4. Scope and release boundary

### R1 pilot includes
- Native Kotlin/Jetpack Compose Android app.
- Existing Supabase email/password authentication, session restoration and student-scoped profile/assignment reads.
- User-scoped VM status from a trusted broker.
- Broker-authenticated short-lived session grant and WSS byte relay to a private RDP endpoint.
- Native FreeRDP session surface and dedicated MobiDesk RDP Activity.
- Separate Windows NLA credential prompt and safe credential lifecycle.
- Keyboard, pointer, touch, scrolling, scaling, disconnect and error states.
- Certificate validation, session revocation, rate limits, audit-safe telemetry and cleanup.
- Foreground-only RDP session lifecycle for the first pilot.

### Explicitly out of R1
- Student self-registration or student-controlled VM reassignment.
- Client-supplied VM IDs, hostnames, ports or hypervisor commands.
- Public TCP/3389 exposure or direct client-to-hypervisor access.
- Broker-issued Windows passwords or reuse of Supabase passwords for Windows.
- Admin VM inventory/power controls, clipboard/file transfer, audio redirection and arbitrary USB forwarding unless separately approved and threat-modelled.
- Raspberry Pi dock streaming. Dock remains R2 pending an architecture spike.
- Production / Play Store release signing until distribution and key ownership are approved.

## 5. Core user journeys

1. **Sign in:** student enters institution-provisioned credentials; app restores only a valid Supabase session.
2. **Load workspace:** app fetches the student's own profile and assigned VM. Empty, loading and backend-error states are explicit.
3. **Check readiness:** app requests status from the broker using the signed-in user's access token. UI distinguishes VM lifecycle from RDP network readiness.
4. **Connect:** student enters their separate Windows guest username/password. App requests a grant without identifying a VM; broker derives the assignment server-side.
5. **Launch RDP:** app establishes WSS relay using the one-use grant, validates the server identity, and invokes the pinned FreeRDP engine in a native session Activity.
6. **Use session:** student interacts using supported touch, mouse/trackpad and keyboard controls.
7. **Disconnect or fail:** app closes FreeRDP, relay, local bridge, timers and secret buffers; broker invalidates the grant and records a safe audit event.

## 6. Normative requirements

The keywords MUST, MUST NOT, SHOULD and MAY are normative.

### Identity and assignment
- **ID-01 MUST** authenticate students through the existing Supabase Auth project.
- **ID-02 MUST** scope profile and assignment reads to the authenticated student's own identity under RLS.
- **ID-03 MUST NOT** accept a VM ID, email, hostname, IP or port from the Android client as an authorization input.
- **ID-04 MUST** derive exactly one active VM assignment on the broker using validated user identity and trusted server-side data.
- **ID-05 MUST** deny zero, multiple, inactive or inconsistent assignments; it MUST NOT guess or silently reassign.
- **ID-06 MUST NOT** provide self-registration or student assignment mutation in R1.

### VM status
- **VM-01 MUST** obtain live lifecycle/readiness information from the broker or a documented authoritative status source; Supabase assignment rows alone are not live hypervisor state.
- **VM-02 MUST** show lifecycle separately from RDP readiness. Lifecycle values: STOPPED, STARTING, RUNNING, STOPPING, MAINTENANCE, UNKNOWN. Readiness values: BOOTING, RDP_PORT_REACHABLE, RDP_READY, RDP_UNAVAILABLE, UNKNOWN.
- **VM-03 MUST** apply a bounded health-probe timeout of 1.5 seconds per request and mark stale status after 15 seconds.
- **VM-04 MUST NOT** describe a successful TCP/3389 probe as full NLA/RDP readiness. Only a validated readiness check may yield RDP_READY.
- **VM-05 MUST** enable Connect only when the assignment is valid and the broker reports the minimum readiness state required by the approved client flow.

### Session grants and relay
- **SE-01 MUST** expose HTTPS POST /v1/session-grants. The client sends the Supabase bearer token; it sends no VM ID, username, password, host or port.
- **SE-02 MUST** validate token signature, issuer, audience and expiry, then verify the associated session_id is still active in the Supabase auth.sessions source of truth before issuing a grant.
- **SE-03 MUST** issue cryptographically random, one-use grants with a 60-second expiry, bound to user, assigned VM and session; store only a hash at rest.
- **SE-04 MUST** provide WSS GET /v1/rdp for the byte relay. The relay is not an RD Gateway and does not replace Windows NLA.
- **SE-05 MUST** limit one concurrent session per student and cap total concurrent relays to the configured pilot limit (initially five).
- **SE-06 MUST** enforce per-user and per-source rate limits, connection/idle timeouts, message/frame limits and bounded resource use.
- **SE-07 MUST** re-check active Supabase session and assignment state while relaying at intervals no greater than 15 seconds; a revoked session or changed assignment MUST close the relay within 30 seconds.
- **SE-08 MUST** invalidate grants on use, expiry, cancellation, logout, authorization failure and relay teardown.
- **SE-09 MUST** never expose VM private addresses or credentials in client responses, logs or telemetry.

### Windows guest authentication
- **AUTH-01 MUST** use a Windows guest account distinct from the student's Supabase password.
- **AUTH-02 R1 recommendation:** student enters the Windows username and password for each session; the broker does not issue, store or proxy the guest password.
- **AUTH-03 MUST** keep guest credentials in memory only, avoid persistence, crash reporting, analytics, clipboard and logs, and clear application/native copies as soon as FreeRDP no longer requires them, including failure and disconnect paths.
- **AUTH-04 MUST** use a mutable character buffer where supported and avoid immutable String copies across the security boundary. Native buffers/settings/callback copies must be released or cleared on teardown.
- **AUTH-05 MUST** show a clear credential error and permit a deliberate retry without leaking the credential or reusing a stale grant.
- **AUTH-06 MUST NOT** silently switch to an ephemeral password or directory-joined design. Those remain future alternatives requiring approved operations design.

### Transport and server identity
- **NET-01 MUST** carry control traffic over HTTPS and RDP relay traffic over WSS with modern TLS.
- **NET-02 MUST NOT** expose TCP/3389 to the public internet; broker-to-VM RDP traffic stays on an approved private network.
- **NET-03 MUST** authenticate the RDP server certificate using an approved internal CA or a per-VM SHA-256 certificate pin delivered through a trusted, grant-bound policy.
- **NET-04 MUST NOT** accept all RDP certificates or provide an “accept anyway” bypass to students.
- **NET-05 MUST** define administrative certificate rotation, expiry, mismatch handling and rollback before pilot use.
- **NET-06 MUST NOT** assume an HTTP WebSocket endpoint is automatically a raw FreeRDP transport. The exact pinned FreeRDP v3.32.0 transport API and framing path must be verified in a spike.

### Android native session
- **AND-01 MUST** use a dedicated native RdpSessionActivity as the R1 rendering boundary, embedding the pinned FreeRDP surface and native input controls.
- **AND-02 MUST** keep FreeRDP session ownership/decoder lifecycle distinct from the current render surface so resize/recreation does not corrupt session state.
- **AND-03 MUST** handle connect, authenticating, connected, reconnecting, disconnecting, failed, expired and disconnected UI states.
- **AND-04 MUST** support pointer movement/click, direct touch mapping, keyboard text, special keys, scroll and explicit modifier states; lock states MUST have a visible reset.
- **AND-05 MUST** enable FLAG_SECURE in the RDP Activity and sensitive overlays to reduce screenshot and recent-task leakage.
- **AND-06 MUST** apply a documented best-effort rooted/unlocked-device policy. For the pilot, block sessions when root is detected unless the owner explicitly authorizes a controlled test exception.
- **AND-07 MUST** use a foreground-only session lifecycle in R1. On app background or screen lock, pause/close predictably; do not claim an Android foreground service until its type, permission and platform eligibility are validated.
- **AND-08 MUST** cleanly release FreeRDP, WSS, local bridge, wake locks, timers, credential buffers and UI callbacks on every exit path.

### Local transport bridge
- **BR-01 SHOULD** use a socketpair, connected descriptor or app-private Unix-domain socket if the pinned FreeRDP API permits it.
- **BR-02** If loopback TCP is unavoidable, MUST bind only to 127.0.0.1, use a cryptographically random single-use secret, start the listener immediately before launch, authenticate the local handshake, accept exactly one connection, close the listener immediately after accept, enforce a short handshake timeout and clear the secret.
- **BR-03 MUST NOT** bind to 0.0.0.0 or expose the bridge to LAN interfaces.
- **BR-04 MUST** include a local port-hijack and unauthorized-local-client test.

### Dock (R2, not in R1)
- **DK-01** MUST remain outside R1 acceptance.
- **DK-02** MUST compare two options before selecting the R2 architecture: phone decodes RDP and re-encodes video to H.264 for the Pi; versus FreeRDP running on the Pi with the phone as identity/session controller.
- **DK-03** MUST measure latency, sustained frame rate, power, thermal load, CPU/memory and recovery before architecture selection.
- **DK-04** MUST NOT trust USB AOA manufacturer/model/serial strings as device authentication. First pairing requires user confirmation and a dock-provisioned key with challenge-response.
- **DK-05** MUST NOT stream to an unpaired/unauthenticated dock.

## 7. Non-functional requirements and pilot gates

- **NFR-01 Security:** no public RDP port, no client-selected VM routing, no accept-all certificate mode, no logged access/guest secrets.
- **NFR-02 Availability:** a broker outage or unavailable VM yields a truthful actionable state; no stale “online” assertion.
- **NFR-03 Performance:** reference workload and gates are specified in PERFORMANCE_PLAN.md. Do not assert an FPS or latency target as achieved until measured on the selected physical device and pilot network.
- **NFR-04 Resource bounds:** status probes, grants and relays have bounded time, memory, concurrency and payload sizes.
- **NFR-05 Privacy:** telemetry uses opaque session IDs and stage timings; it MUST NOT contain passwords, JWTs, grant tokens, student email/name, screen content or private VM endpoints.
- **NFR-06 Compatibility:** validate the supported Android version/API range, ARM64 native libraries and exact pinned FreeRDP integration before each candidate release.
- **NFR-07 Recovery:** all failed/interrupted sessions release local and server-side resources and allow a deliberate retry.

## 8. Error and recovery contract

| Code | Meaning | Client action |
|---|---|---|
| AUTH_REQUIRED | Supabase session absent/expired | Sign in again |
| ASSIGNMENT_NOT_FOUND | No active assignment | Show help state; no connection attempt |
| ASSIGNMENT_AMBIGUOUS | More than one active assignment | Fail closed; operator action |
| VM_MAINTENANCE | VM under maintenance | Show maintenance status |
| VM_STOPPED | Assigned VM stopped | Report state; no invented start control |
| RDP_UNAVAILABLE | RDP path unavailable | Retry later; diagnostic reference |
| GRANT_EXPIRED / GRANT_USED | Ticket expired or consumed | Request a new grant |
| SESSION_REVOKED | Auth session revoked | Close relay and return to sign-in |
| ASSIGNMENT_CHANGED | Assignment changed during session | Close relay and refresh workspace |
| GUEST_AUTH_FAILED | Windows NLA rejected credentials | Re-prompt; no Supabase reset |
| CERTIFICATE_MISMATCH | RDP server identity invalid | Hard fail; operator remediation |
| RELAY_LIMIT | User/system capacity reached | Explain capacity and safe retry |
| NETWORK_LOST | Relay interrupted | Show reconnecting; do not silently reuse grant |
| CLIENT_UNSUPPORTED | Device/build unsupported by policy | Explain supported device requirement |

No error may reveal another student's identity, assignment, VM address or internal topology.

## 9. Acceptance and release gates

- **G0 — Baseline:** clean source/build inventory, exact dependency pins, known limitations and reproducible build.
- **G1 — Identity:** real Supabase test-user sign-in, own-profile/assignment RLS, negative cross-student access tests; no production PII in tests.
- **G2 — Broker:** user-scoped readiness, server-derived assignment, one-use grants, expiry, rate limits, revocation, relay cleanup, no public RDP exposure.
- **G3 — RDP:** successful FreeRDP NLA connection to an approved test VM; certificate mismatch fails closed; credential buffers and local bridge clean on every exit.
- **G4 — Interaction/performance:** pass the measurable gates in PERFORMANCE_PLAN.md on the selected reference physical device.
- **G5 — Pilot security:** authorization, session revocation, certificate, bridge hijack, rate/DoS, rooted-device and privacy tests pass.
- **G6 — Release:** owner approves distribution channel, supported OS/API matrix, privacy/support text and owner-controlled release signing key.

A passing compile or debug APK is not evidence of an end-to-end RDP session or production release readiness.

## 10. Data, privacy and audit

The Android client receives only the student's own permitted profile/assignment and status. The broker is the security boundary for routing and session grants. Audit records include outcome, timestamp, opaque user/session correlation and reason code, but not passwords, JWTs, grant tokens, screen content or private network addresses. Retention and operator access must be approved before production.

## 11. Rollout and rollback

Pilot begins with a small owner-selected cohort and controlled VMs. Enable one tested cohort at a time. Stop new grants and revoke active relays on a security incident, assignment inconsistency, certificate mismatch, unexpected public reachability, repeated resource exhaustion or failure to clean up. Roll back broker/client to the last reviewed build; never solve a transport problem by opening public RDP or disabling certificate validation.

## 12. Decisions and open owner approvals

Recommended pilot decisions are documented in DECISIONS.md. Owner approval is still required for: VM host/location and lifecycle model; Windows guest account provisioning; reference physical device; Play Store vs managed/direct distribution; release key custody; retention policy; and final broker deployment hostname/route. Lab IT must be consulted on guest accounts, certificate provisioning and VM operational controls.

## 13. Source-of-truth rule

This PRD defines intended product behaviour. Existing code, historical build reports and discovery documents are evidence of the current implementation only; they do not override a requirement. Any change to an approved requirement requires a decision record and PRD revision. The canonical project is the native Kotlin/Jetpack Compose repository named above.
