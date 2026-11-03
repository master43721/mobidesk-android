# MobiDesk — Feature Inventory and Release Scope

**Document version:** 1.0  
**Snapshot:** 3 November 2026  
**Canonical repository:** [master43721/mobidesk-android](https://github.com/master43721/mobidesk-android)  
**Normative source:** `docs/MobiDesk_PRD_v1.2.pdf` unless a row is explicitly marked as an owner-directed proposed amendment.  
**Purpose:** Product-level feature breakdown for estimation, implementation planning, acceptance and progress tracking.

> **Important source distinction:** The PRD v1.2 PDF remains the current normative specification. This inventory records the Project Owner's latest choices. It does not silently rewrite v1.2. Any conflict with v1.2 is marked **OWNER-DIRECTED PROPOSED CHANGE** and must be incorporated into an approved successor PRD before implementation treats it as normative.

## 1. How Feature Points (FPs) are defined

A Feature Point (FP) is one coherent, independently identifiable product capability with a clear user/system outcome and its own acceptance boundary.

- A large capability with substantial architecture, native code, backend integration or security implications receives its own FP.
- Two or three tiny, tightly related capabilities may be grouped when they share one implementation boundary and can be accepted together.
- An FP is not automatically a screen, API route, class, commit or coding task.
- Each FP will later be decomposed into engineering tasks, tests, dependencies and evidence.
- A feature is not complete merely because its UI exists or an APK compiles.

### Status labels

| Label | Meaning |
|---|---|
| **V1.2 BASELINE** | Supported by the current normative PRD v1.2. |
| **OWNER CONFIRMED** | Project Owner explicitly selected the option; consistent with v1.2 or a direct clarification of it. |
| **OWNER-DIRECTED PROPOSED CHANGE** | Owner selected a direction that conflicts with or extends v1.2. Record it, but do not treat it as normative until a successor PRD is approved. |
| **R2 PLANNED** | Product capability is planned for the Dock release, not R1. |
| **ENGINEERING DECISION OPEN** | Product outcome is known, but architecture or platform implementation must be proven before commitment. |
| **OUT OF SCOPE** | Explicitly not included in the current R1/R2 scope. |

## 2. Product scope decisions recorded

| Topic | Owner decision | Status and implementation treatment |
|---|---|---|
| Canonical codebase | Work only in `master43721/mobidesk-android` | **OWNER CONFIRMED**. No other repository is an implementation source. |
| Android client stack | Native Kotlin + Jetpack Compose with pinned FreeRDP | **V1.2 BASELINE**. |
| Windows guest authentication | Student enters separate Windows credentials for each session | **OWNER CONFIRMED**. FP-05 is the pilot path; broker-issued ephemeral passwords and directory-based login are not selected. |
| R1 background session | An active RDP session should continue when the user backgrounds the app / switches to another app | **OWNER-DIRECTED PROPOSED CHANGE**. V1.2 currently requires foreground-only behavior and a five-second teardown. A successor PRD amendment and Android platform spike are required. |
| Device screen lock | Not explicitly resolved by the background choice | **ENGINEERING DECISION OPEN**. Do not assume screen-off/lock-screen persistence is approved. |
| R2 Dock architecture | Keep both phone-side encode/stream and Pi-native FreeRDP options open; compare both before selection | **OWNER CONFIRMED / ENGINEERING DECISION OPEN**. No implementation path is selected yet. |
| Extra features | No extra features added | **OWNER CONFIRMED**. Clipboard synchronization, file transfer and administrative console remain outside current scope. |
| R1/R2 split | Deliver secure Mobile Cloud PC first; Dock capabilities follow in R2 | **V1.2 BASELINE**. |

## 3. R1 — Mobile Cloud PC

R1 delivers the complete student workflow from college authentication to a secure Windows desktop session on the Android phone.

### FP-01 — Student Authentication and Account Recovery

> Detailed specification package: [FP-01 review draft](FEATURE_SPECIFICATIONS/FP-01/README.md). Draft v0.1; awaiting Owner critique.
**Priority:** P0 · **Release:** R1 · **Status:** V1.2 BASELINE

**Outcome:** A provisioned student securely signs in using their institutional identity.

**Included capabilities**
- Supabase email/password sign-in.
- Sign-out and invalidation of the local authenticated state.
- Restore a valid existing Supabase session at app launch.
- Handle expired, revoked and invalid sessions.
- Password visibility toggle.
- Forgot-password and password-recovery flow using the configured Supabase recovery path.
- Prevent duplicate sign-in submissions and show safe authentication errors.
- No self-registration or student-created accounts.

**Acceptance boundary**
- Only an authenticated, institution-provisioned student can enter protected workspace screens.
- Expired/revoked sessions return to sign-in.
- Recovery does not claim that an email was delivered until the configured mail flow is verified.
- Credentials and tokens never appear in logs or navigation arguments.

### FP-02 — Student Dashboard and Workspace
**Priority:** P0 · **Release:** R1 · **Status:** V1.2 BASELINE

**Outcome:** The student has one clear home screen for their identity and Cloud PC.

**Included capabilities**
- Student name and approved profile fields.
- College/institution label where available.
- One assigned Cloud PC card.
- Cloud PC display label, lifecycle summary and RDP readiness summary.
- Connect action when eligible.
- Refresh-status action.
- Loading, empty, stale and backend-error states.
- Settings/account entry and sign-out access.
- Informational Dock placeholder only; no R1 Dock streaming controls.

**Acceptance boundary**
- Only the current student's profile and one valid assigned VM are shown.
- No infrastructure hostname, IP, VM UUID, port or other student's data is exposed.
- No hard-coded “Online” indicator or fake assignment is displayed.

### FP-03 — Student-to-VM Assignment Resolution and Authorization
**Priority:** P0 · **Release:** R1 · **Status:** V1.2 BASELINE

**Outcome:** Every student is bound to exactly the VM they are authorized to use.

**Included capabilities**
- Read the student's own profile and assignment under Supabase RLS.
- Resolve assignment using authenticated subject and trusted server-side data.
- Enforce exactly one active assignment.
- Handle missing, duplicate, inactive and inconsistent assignments.
- Server-side VM allowlist and approved endpoint mapping.
- Exclude unverified VM entries until independently checked and approved.
- Deny client-supplied VM IDs, hostnames, IP addresses and ports as authorization inputs.
- No student VM picker or self-reassignment.

**Acceptance boundary**
- Zero, multiple, inactive or unmapped assignments fail closed.
- The broker derives the target VM; the mobile client cannot choose or change it.
- An assignment change is reflected in grant authorization and active-session revocation.

### FP-04 — Live VM Lifecycle and RDP Readiness
**Priority:** P0 · **Release:** R1 · **Status:** V1.2 BASELINE

**Outcome:** The app reports whether the assigned Cloud PC can accept a remote-desktop sign-in.

**Included capabilities**
- Broker-owned VM lifecycle status.
- Separate RDP transport/readiness status.
- Bounded server-side readiness probe.
- Last-sampled timestamp, probe duration and freshness.
- Manual refresh and refresh-before-connect.
- Explicit unknown, timeout, stale, stopped, starting and unavailable states.
- Connect eligibility derived from valid assignment and fresh readiness.

**Acceptance boundary**
- Assignment rows alone never imply a live VM state.
- TCP port reachability is not called successful RDP negotiation.
- RDP negotiation readiness is not called successful Windows NLA authentication.
- A stale or unknown result blocks Connect.

### FP-05 — Separate Windows Guest Credential Prompt and NLA
**Priority:** P0 · **Release:** R1 · **Status:** OWNER CONFIRMED; consistent with V1.2

**Outcome:** The student authenticates to Windows using the separate lab account.

**Included capabilities**
- Dedicated Windows lab credential prompt.
- Windows username and password; optional domain where required by the approved guest configuration.
- Explicit explanation that Windows credentials differ from college/Supabase credentials.
- Local validation and password visibility toggle.
- Cancel and clear credential fields.
- Pass credentials into the pinned FreeRDP authentication callback for NLA.
- Bounded retry and safe authentication failure behavior.

**Owner-selected pilot path**
- Prompt the student for Windows credentials on every new session.
- Do not reuse the Supabase password.
- Do not have the broker issue or store ephemeral Windows passwords in the pilot.
- Directory-joined guest authentication is not selected for this pilot.

**Acceptance boundary**
- A real Windows NLA handshake succeeds with the entered guest credentials.
- Guest credentials never go to the session broker, persistent storage, analytics, URLs, Intent extras or logs.
- App-owned mutable copies are cleared as soon as the pinned FreeRDP API permits.

### FP-06 — Broker Session Authorization and One-Time Grant
**Priority:** P0 · **Release:** R1 · **Status:** V1.2 BASELINE

**Outcome:** A trusted broker authorizes one session for the authenticated student's currently assigned VM.

**Included capabilities**
- Validate Supabase JWT signature, issuer, audience, subject and expiry.
- Validate the JWT `session_id` against active Supabase session state.
- Resolve the current unique VM assignment server-side.
- Verify assignment/version and VM allowlist.
- Issue a cryptographically random one-use grant.
- Grant expiry (maximum 60 seconds), hash-only storage and atomic one-time consume.
- Bind grant to authenticated user, active session, assignment and target trust policy.
- Per-user rate limit, concurrency limits and grant replay prevention.
- Recheck eligibility before relay establishment.

**Acceptance boundary**
- Grant request contains no VM selector or guest credentials.
- A replayed, expired, revoked, cross-user or assignment-mismatched grant is rejected.
- Grant issuance and consumption produce safe audit events without secret values.

### FP-07 — Secure WSS RDP Byte-Stream Relay
**Priority:** P0 · **Release:** R1 · **Status:** V1.2 BASELINE

**Outcome:** The broker securely relays the RDP byte stream between Android and the assigned private Windows VM.

**Included capabilities**
- TLS-protected HTTPS control plane and WSS data plane.
- Authenticated WebSocket upgrade using the one-use grant.
- Binary, ordered, full-duplex byte-stream forwarding.
- Private upstream connection only to the server-selected allowlisted guest.
- Fixed approved RDP destination port; no arbitrary TCP proxying.
- Bounded frame/message sizes, queues, buffers and concurrent relays.
- Backpressure, heartbeat, idle timeout, cancellation and cleanup.
- No public guest TCP/3389 exposure.

**Acceptance boundary**
- Byte sequence/order is preserved regardless of WebSocket message boundaries.
- Text frames, oversized frames, invalid grants and arbitrary destinations are rejected.
- Relay resources are bounded and released on every terminal path.

### FP-08 — Protected FreeRDP Local Transport Bridge
**Priority:** P0 · **Release:** R1 · **Status:** V1.2 BASELINE; implementation proof blocking

**Outcome:** The WSS relay is connected to FreeRDP without allowing another local app to hijack the stream.

**Included capabilities**
- Local byte-stream adapter for the FreeRDP transport.
- Ordered reader and writer paths in both directions.
- Cancellation, bounded queues, backpressure, EOF and teardown behavior.
- Preferred socketpair/connected descriptor integration.
- App-private Unix-domain socket with peer verification if supported.
- Loopback TCP fallback only with a one-use high-entropy peer handshake.
- Exact one authenticated peer; close the listener after successful accept.

**Acceptance boundary**
- No unauthenticated localhost listener.
- A random port alone is not treated as peer authentication.
- A malicious same-device app cannot bind, pre-connect, replay or inject into the bridge.
- If the pinned FreeRDP integration cannot participate in authenticated transport, release is blocked.

### FP-09 — Native FreeRDP Engine and RDP Protocol Integration
**Priority:** P0 · **Release:** R1 · **Status:** V1.2 BASELINE; transport spike required

**Outcome:** The app establishes a real RDP connection using the repository's pinned FreeRDP Android engine.

**Included capabilities**
- Integrate the exact pinned FreeRDP Java/JNI/native source.
- App-owned FreeRDP adapter and configuration boundary.
- Connect/disconnect and native-context lifecycle.
- Integrate the selected protected byte-stream transport.
- RDP negotiation and Windows NLA.
- Desktop graphics callbacks and input forwarding.
- Disable R1-excluded channels and redirection.
- Handle native errors and release native resources safely.

**Acceptance boundary**
- A real assigned Windows VM is reached through the broker using the pinned FreeRDP version.
- Exact transport/descriptor APIs and callback semantics are proven against the pinned source.
- No assumption is made that RD Gateway WebSocket equals the custom raw byte-stream relay.

### FP-10 — RDP Server Certificate Identity and Rotation
**Priority:** P0 · **Release:** R1 · **Status:** V1.2 BASELINE

**Outcome:** The client can verify that the RDP server is the trusted assigned Windows guest.

**Included capabilities**
- Internal CA or approved per-VM SHA-256 certificate fingerprint trust.
- Broker-delivered grant trust policy.
- FreeRDP certificate verification callback integration.
- Validate trust identity and certificate validity.
- Reject missing, expired, untrusted or mismatched certificates.
- Controlled certificate rotation and rollback.
- No accept-all preference or “continue anyway” UI.

**Acceptance boundary**
- Valid trusted identity succeeds.
- Any missing or mismatched trust material fails closed.
- Rotation/rollback is tested with the broker and guest certificate in a coordinated procedure.

### FP-11 — Native RDP Session Window and Desktop Rendering
**Priority:** P0 · **Release:** R1 · **Status:** V1.2 BASELINE; render lifecycle spike required

**Outcome:** A real Windows desktop is presented in a dedicated native Android session experience.

**Included capabilities**
- Dedicated non-exported `RdpSessionActivity`.
- Native render target bound to the pinned FreeRDP renderer.
- Full remote desktop viewport and scaling.
- Render frame update/invalidation handling.
- Native overlay toolbar and connection indicator.
- Activity lifecycle separated from logical RDP session ownership.
- Display size and resolution handling where supported.
- `FLAG_SECURE` before remote pixels are displayed.

**Architecture boundary**
- Jetpack Compose owns sign-in, workspace, status and app navigation.
- Native Android owns the active RDP session Activity, remote render target and session toolbar.
- The remote desktop is not decoded/redrawn by a Flutter or Compose video pipeline.
- The pinned FreeRDP Android renderer is observed as bitmap-backed View in the inspected source; SurfaceView/TextureView must not be assumed without a separate proof.

**Acceptance boundary**
- A real guest desktop is displayed on the device.
- Rotation, Activity recreation, resize and renderer lifecycle behavior are verified.
- No screenshot/recent-task preview reveals the protected desktop.

### FP-12 — Touch, Pointer, Mouse and Trackpad Interaction
**Priority:** P0 · **Release:** R1 · **Status:** V1.2 BASELINE

**Outcome:** The student can operate desktop applications using the phone's touchscreen or an external pointing device.

**Included capabilities**
- Trackpad mode with relative pointer movement.
- Tap-to-left-click.
- Long-press and drag.
- Two-finger scrolling.
- Two-finger right-click.
- Direct-touch mode only if supported and tested by the pinned client.
- External mouse movement, buttons, hover and wheel.
- Correct coordinate mapping between phone viewport and remote desktop.
- Gesture feedback and first-use hint.

**Acceptance boundary**
- Click, drag, scroll and pointer movement are accurate on the reference device.
- Gestures intended for RDP are not intercepted as unintended Android context actions.

### FP-13 — Keyboard, Soft Input and Shortcut Controls
**Priority:** P0 · **Release:** R1 · **Status:** V1.2 BASELINE

**Outcome:** Students can type and issue common desktop shortcuts from Android.

**Included capabilities**
- Android soft keyboard show/hide.
- Physical keyboard forwarding.
- Unicode text and supported special-key mapping.
- Ctrl, Alt, Shift and Win/Meta modifiers.
- Esc, Tab, Delete and arrow keys.
- Modifier lock/unlock and clear-modifier action.
- Dedicated Ctrl+Alt+Delete action through the correct FreeRDP API.
- Toolbar collapse/expand and keyboard focus handling.

**Acceptance boundary**
- Text and shortcuts work in Windows applications.
- No stuck modifier state remains after cancellation/disconnect.
- Opening the IME does not incorrectly trigger the app-background lifecycle policy.

### FP-14 — RDP Session Lifecycle, Background Continuity and Cleanup
**Priority:** P0 · **Release:** R1 · **Status:** MIXED — V1.2 lifecycle baseline plus OWNER-DIRECTED PROPOSED background amendment

**Outcome:** A remote desktop session is managed safely from connect through disconnect, including the Owner-selected background behavior.

**Included capabilities**
- Single active connection attempt/session per student.
- Explicit session states and connection progress.
- Cancel during connection and disconnect while connected.
- Network interruption detection and bounded reconnect with a fresh grant.
- Session/assignment revocation and server-side relay termination.
- Rotation and Activity recreation handling.
- Native context, socket, listener, WSS, worker, queue, grant and credential cleanup.
- Idempotent cleanup after failure, timeout, cancellation and process/activity loss.
- No new session while cleanup remains uncertain.

**Background behavior selected by Owner**
- The user may move MobiDesk to the background or switch to another app while an active RDP session continues.
- The live session owner and network transport must not depend on the RDP Activity remaining visible.
- The app must expose an honest, user-visible active-session indication and a deliberate return/disconnect path.
- Start any required long-running component while the session is being established from a visible/user-initiated flow; do not depend on launching a foreground service later from an arbitrary background state.
- The precise Android service type, permissions, notification design, resource policy and target-API compatibility require a dedicated spike.
- Android force-stop, OS process termination, loss of network and broker revocation are not promised to preserve a session; they must lead to safe cleanup/reconnect behavior.

**Conflict with v1.2**
- PRD v1.2 currently says R1 is foreground-only and true backgrounding/screen lock starts a five-second cancelable teardown.
- Owner has now selected background continuation as the desired R1 direction.
- Treat this as **OWNER-DIRECTED PROPOSED CHANGE** until the successor PRD formally replaces the v1.2 rule.
- Screen-lock/screen-off behavior remains open; the background decision does not by itself authorize showing a remote desktop on the lock screen or keeping it active indefinitely with the display off.

**Acceptance boundary**
- With the selected implementation, switching to another app for the tested background interval does not end an otherwise healthy authorized session.
- Returning to MobiDesk restores the same session/desktop if it remains active.
- Notification/ongoing status and explicit disconnect work.
- OS kill, force-stop, revoked identity, network loss and resource restrictions are handled without leaking credentials or orphaning relay/native resources.

### FP-15 — Device Integrity and Sensitive Screen Protection
**Priority:** P0 · **Release:** R1 · **Status:** V1.2 BASELINE

**Outcome:** Reduce the chance that sensitive remote desktop data is captured or used from an unsupported compromised device.

**Included capabilities**
- Apply `FLAG_SECURE` to the active RDP window.
- Protect recent-task preview and standard screenshot capture.
- Root and unlocked-bootloader detection according to the PRD policy.
- Handle missing/unknown device-integrity results explicitly.
- Block unsupported device states according to the approved rule.
- No user bypass for a failed integrity check.

**Acceptance boundary**
- Standard screenshots and task-switcher previews do not reveal RDP pixels.
- Rooted/unlocked/unknown integrity results follow the specified fail-closed policy.

### FP-16 — Error Catalogue and Guided Recovery
**Priority:** P0 · **Release:** R1 · **Status:** V1.2 BASELINE

**Outcome:** Give students a safe, understandable next action when any part of the workflow fails.

**Included capabilities**
- Central mapping of backend, broker, transport, NLA, certificate, FreeRDP and lifecycle failures.
- Exact PRD error-code and English-copy mapping.
- Safe retry, refresh, sign-in, return-to-workspace or administrator-contact actions.
- No raw stack trace, endpoint or secret shown to the student.
- Prevent retry when policy says administrator action or verified cleanup is needed.
- Keep error logging separate from user-facing copy.

**Acceptance boundary**
- The existing PRD §12 catalogue is used without silently inventing or renumbering error codes.
- Every error follows its documented retry rule.

### FP-17 — Privacy-Safe Telemetry, Diagnostics and Performance Measurement
**Priority:** P0 · **Release:** R1 · **Status:** V1.2 BASELINE

**Outcome:** Measure real product performance and diagnose failure without collecting remote desktop content or credentials.

**Included capabilities**
- Opaque attempt ID.
- Monotonic stage timings for status, grant, WSS, LocalBridge, RDP, NLA, first frame and cleanup.
- Presented FPS and frame-interval measurement.
- Input-to-visible-response timing.
- Retry, failure-stage and PRD error-code counts.
- Coarse network/device/build classification.
- Redacted local diagnostics and server-side grant/relay audit events.
- Explicit telemetry allowlist and privacy scan.

**R1 performance gates**
| Measurement | Current engineering acceptance target |
|---|---:|
| First desktop frame | p90 <=20 seconds over >=20 controlled attempts |
| Presented FPS | Average >=15 FPS over a 120-second 1280x720 workload |
| Input response | p90 <=250 ms under baseline RTT <=40 ms |
| First-attempt reliability | >=19/20 controlled attempts |
| Cleanup | 20 cycles with zero native crash or resource/secret leak |
| Recovery | <=15 seconds following <=10 seconds of Wi-Fi interruption when VM remains ready |

These are acceptance targets from the current engineering plan, not achieved measurements. A 4-student pilot must not be reported with misleading cohort percentages or statistical claims.

**Acceptance boundary**
- Every G4 metric is backed by a reproducible device/test record.
- Logs and telemetry contain no password, JWT, grant, student identifier, private endpoint or screen/clipboard content.

### FP-18 — Release Artifact, Signing and Build Integrity
**Priority:** P0 · **Release:** R1 · **Status:** V1.2 BASELINE

**Outcome:** Deliver a traceable and secure Android artifact.

**Included capabilities**
- Debug/test and production release build separation.
- Preserve the approved SDK and ABI baseline unless formally changed.
- Exact FreeRDP gitlink/source provenance.
- Owner-approved signing model and key custody.
- APK/AAB generation based on selected distribution channel.
- Artifact SHA-256 and signer fingerprint record.
- Static scan for mocks/demo flags, secrets, cleartext, trust-all, debug endpoints and excluded channels.
- Install, upgrade and release verification.
- Source commit, version and dependency manifest traceability.

**Acceptance boundary**
- A build or debug APK is not a production release.
- Production distribution is blocked until signing, channel, test and operations gates are satisfied.

## 4. R2 — External Monitor and Dock

R2 extends the authorized desktop experience to a paired Raspberry Pi/HDMI dock. The architecture remains open until the comparative spike is complete.

### FP-19 — Dock Discovery, Pairing and Device Authentication
**Priority:** R2 · **Status:** R2 PLANNED

**Outcome:** A phone can recognize and explicitly authorize a supported dock.

**Included capabilities**
- USB-C/OTG and Android Open Accessory discovery.
- Dock capability/version discovery.
- Explicit user confirmation before desktop content is shared.
- Cryptographic challenge-response using a dock-provisioned key.
- Pairing, unpairing and key revocation.
- Reject unpaired, unknown or spoofed devices.

**Acceptance boundary**
- AOA manufacturer/model strings are discovery metadata only, never proof of identity.
- A dock cannot receive desktop content until explicit confirmation and key-backed authentication succeed.

### FP-20 — Dock Desktop Delivery Architecture and External Rendering
**Priority:** R2 · **Status:** R2 PLANNED; ENGINEERING DECISION OPEN

**Outcome:** Present the same authorized Windows desktop on a dock-connected HDMI monitor.

**Two architectures must be compared before selection**
1. **Phone-render/transcode path:** Android remains the FreeRDP endpoint, renders the desktop, encodes H.264 and sends the stream over the dock link to the Pi for HDMI output.
2. **Pi-native FreeRDP path:** The Pi runs the FreeRDP client; the phone acts as identity/session controller and authorizes the Pi's session connection over the approved tethered/private route.

**Comparison criteria**
- End-to-end input-to-visible-response latency.
- Presented FPS, dropped frames and display quality.
- CPU/GPU utilization on phone and Pi.
- Phone battery impact and charging behavior.
- Pi thermal behavior and stability.
- USB throughput and reconnect reliability.
- Security boundary and dock key handling.
- Session start time, resource use and recovery behavior.

**Acceptance boundary**
- No architecture is preselected.
- The chosen path requires a measured comparison report and explicit Owner decision.
- The dock uses the same authorized student/VM session policy; it cannot select another VM or bypass the broker.

### FP-21 — Dock Keyboard, Mouse and Input Forwarding
**Priority:** R2 · **Status:** R2 PLANNED

**Outcome:** A dock-connected keyboard/mouse can control the same authorized Windows desktop.

**Included capabilities**
- Mouse movement, click, right-click, wheel and drag.
- Keyboard key-down/key-up, modifiers and special keys.
- Coordinate mapping for external display resolution.
- Input routing through the selected dock architecture.
- Pairing/session validation for each input source.
- Detach and stale-input handling.

**Acceptance boundary**
- Only authenticated, paired dock input is forwarded to the active authorized session.
- Disconnecting or revoking the dock prevents further input.

### FP-22 — Dock Attach/Detach, Display Handover and Session Continuity
**Priority:** R2 · **Status:** R2 PLANNED; ENGINEERING DECISION OPEN

**Outcome:** Students can attach or remove the dock without changing their authorized VM or leaving orphaned resources.

**Included capabilities**
- Detect dock attach/detach.
- Negotiate supported monitor capability and resolution.
- Enter/leave external display mode.
- Preserve the logical student session where technically supported.
- Resize or reattach FreeRDP render target where proven.
- Controlled reconnect to the same assigned VM if live migration/rebind is unsupported.
- Clear dock pairing/session state on detach, revoke or fault.
- Recover from USB power loss, unplug/replug and display changes.

**Acceptance boundary**
- The chosen path preserves assignment authorization.
- RDP Display Control (MS-RDPEDISP), Android display handling and Pi display recovery are tested.
- Seamless hot-plug is not promised unless proven; reconnect to the same assigned VM is the explicit fallback.

## 5. Explicitly excluded from current scope

The Owner selected **no extra features** beyond the listed R1 and R2 capabilities. The following remain excluded unless separately approved through change control:

- Clipboard synchronization.
- File transfer or shared folders.
- Audio, microphone, camera or arbitrary USB redirection.
- Student VM picker or student-controlled reassignment.
- Student VM power-on/off or hypervisor administration.
- Administrative management console.
- Multi-VM selection or simultaneous desktop sessions for a student.
- Public direct RDP access or arbitrary TCP forwarding.
- Persistent guest-password storage or automatic Supabase-to-Windows password reuse.

## 6. Cross-feature dependencies

| Dependency | FPs affected | Rule |
|---|---|---|
| Identity and assignment | FP-01, FP-03, FP-06 | No session starts without authenticated identity and exactly one valid assignment. |
| Status and readiness | FP-04, FP-06, FP-07 | Status is fresh; grant/relay revalidate independently. |
| Guest authentication | FP-05, FP-09, FP-10 | NLA happens at the guest; guest credentials stay client-side; certificate trust is independently verified. |
| RDP transport | FP-07, FP-08, FP-09 | Raw WSS relay compatibility with the exact pinned FreeRDP source is a blocking spike. |
| Rendering/input | FP-11, FP-12, FP-13 | Render and input lifecycle are native; UI cannot own native context. |
| Lifecycle/background | FP-14, FP-17, FP-18 | Background behavior requires Android platform, telemetry, battery and release-policy verification. |
| Dock | FP-19, FP-20, FP-21, FP-22 | Pair/authentication precedes any display/input streaming; both render architectures are compared. |

## 7. First implementation-planning sequence

This is a dependency order, not a promise that the FPs are small or can be completed in one agent task.

1. **Foundation:** FP-01, FP-02, FP-03, FP-04.
2. **Authorization/guest identity:** FP-05, FP-06.
3. **Transport and protocol spikes:** FP-07, FP-08, FP-09, FP-10.
4. **Remote desktop experience:** FP-11, FP-12, FP-13.
5. **Session quality and release:** FP-14, FP-15, FP-16, FP-17, FP-18.
6. **Dock R2:** FP-19 first; FP-20 architecture comparison; FP-21 and FP-22 after the selected architecture and contracts are approved.

Do not split the large transport, FreeRDP, rendering, security or background-session capabilities into unrelated tiny FPs. Split them into engineering subtasks beneath their parent FP.

## 8. Sources and change control

- Normative product source: `docs/MobiDesk_PRD_v1.2.pdf`, especially §§4–13.
- UX and student journey source: `docs/DESIGN.md`.
- Native implementation constraints: `docs/TECHNICAL_SPEC.md`.
- Broker contract: `docs/API_CONTRACT.md` and `docs/openapi.yaml`.
- Security controls: `docs/SECURITY_THREAT_MODEL.md`.
- Test/performance gates: `docs/TEST_PLAN.md`, `docs/PERFORMANCE_PLAN.md`.
- Decisions and open questions: `docs/DECISIONS.md`, `docs/OPEN_ITEMS.md`.

Changes to this inventory must record the reason, affected FP IDs, PRD requirement impact, decision ID, tests and migration/rollback consequences. Do not silently promote an owner-directed proposal to normative scope.
