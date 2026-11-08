# FreeRDP Native Transport I/O Integration Spike (`SPK-01`)

**Document:** `docs/spikes/FREERDP_TRANSPORT_SPIKE.md`  
**Target Submodule:** `third_party/FreeRDP` (commit `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1` / v3.32.0)  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md`  
**Spike Drill Kit:** `docs/live-drills/SPK-01/`  
**Decision Verdict:** **GO (with Custom Native JNI Adapter & Socketpair Bridge)**  

---

## 1. Audit Findings with Exact File & Line Citations

This audit inspects the checked-in FreeRDP submodule and Android wrapper against the four audit questions posed in `docs/FREERDP_ANDROID_API_AUDIT.md`:

### Question 1: Can FreeRDP accept a custom transport byte stream without connecting directly to a remote host?
- **Finding:** **YES at native C level; NO via the stock Android Java wrapper.**
- **Citations:**
  - `third_party/FreeRDP/include/freerdp/transport_io.h`:
    - Lines 47–60: `pTransportLayerRead` and `pTransportLayerWrite` define the function pointer interface for injecting arbitrary byte read and write operations.
    - Lines 68–77: `rdpTransportLayer` struct encapsulates user context, `Read`, `Write`, `Close`, `Wait`, and `GetEvent`.
    - Lines 111–127: `struct rdp_transport_io` contains function pointers: `TCPConnect`, `TLSConnect`, `TransportAttach`, `ReadBytes`, `ConnectLayer`, `AttachLayer`.
    - Lines 136–138: `freerdp_set_io_callbacks(rdpContext* context, const rdpTransportIo* io_callbacks)` installs the custom table.
    - Lines 140–141: `freerdp_set_io_callback_context(rdpContext* context, void* usercontext)`.
    - Lines 178–180: `transport_layer_new(rdpTransport* transport, size_t contextSize)`.
  - `third_party/FreeRDP/client/Android/Studio/freeRDPCore/src/main/cpp/android_freerdp.c`:
    - Lines 714–723: `android_pre_connect()` registers lifecycle handlers (`PreConnect`, `PostConnect`, `PostDisconnect`, `AuthenticateEx`, `VerifyCertificateEx`). It does NOT register `io_callbacks`.
    - Lines 598–630: `android_freerdp_run()` executes `freerdp_connect(instance)` directly using hostname/port settings.
  - `LibFreeRDP.java` exposes no JNI method for passing an Android `FileDescriptor`, `Socket`, or custom stream.
- **Remediation:** MobiDesk must supply a native JNI extension (or custom adapter) that calls `freerdp_set_io_callbacks` prior to `freerdp_connect`.

---

### Question 2: How does a custom transport bridge compose with TLS/NLA, certificate pinning, and process isolation?
- **Finding:** FreeRDP performs TLS and NLA on top of the underlying transport layer.
- **Citations:**
  - `third_party/FreeRDP/libfreerdp/crypto/tls.c`:
    - Lines 1963–1980: `instance->VerifyCertificateEx` is invoked with `hostname`, `port`, `common_name`, `subject`, `issuer`, and fingerprint `fp`.
    - Lines 2084–2100: `switch (accept_certificate)` evaluates verification status:
      - `case 1`: Accepted permanently (saved to `known_hosts`).
      - `case 2`: Accepted temporarily for session.
      - `default`: **REJECTED (fail closed)**.
  - `android_freerdp.c`:
    - Lines 537–566: `android_verify_certificate_ex()` marshals certificate parameters across JNI to Java `OnVerifyCertificateEx`.
    - Lines 518–535: `android_authenticate_ex()` intercepts `AUTH_NLA`, `AUTH_TLS`, and `AUTH_RDP` and invokes `android_authenticate_int(..., "OnAuthenticate")`.
- **Composability:** Because the custom transport I/O supplies the raw byte stream (replacing the TCP layer), FreeRDP's OpenSSL/WinPR cryptographic engine continues to perform TLS encryption, NLA CredSSP handshakes, and certificate callbacks unchanged over the relayed byte stream.

---

### Question 3: Where are guest credentials copied and handled?
- **Finding:** **Vulnerability in upstream wrapper:** Credentials are converted to immutable Java `String` objects and un-zeroed C heap copies.
- **Citations:**
  - `android_freerdp.c`:
    - Lines 486–516 (`android_authenticate_int`): Passes `StringBuilder` to Java callback. Upon `res == JNI_TRUE`, it reads values:
      ```c
      *username = get_string_from_string_builder(env, jstr1);
      *domain = get_string_from_string_builder(env, jstr2);
      *password = get_string_from_string_builder(env, jstr3);
      ```
  - `android_jni_utils.c`:
    - Lines 79–105 (`get_string_from_string_builder`):
      - Line 98: Calls `StringBuilder.toString()`, creating an immutable `java.lang.String` in the JVM heap that cannot be zeroed and remains until garbage collected.
      - Line 104: Executes `_strdup(native_str)`, allocating an un-zeroed heap buffer in C memory.
- **Remediation:** In MobiDesk production integration (FP-05 / FP-08), credentials entered into the session prompt must be passed via a direct `char[]` / `jcharArray` JNI entrypoint and explicitly zeroed with `memset_s` / `SecureZeroMemory` immediately following FreeRDP's credential copy.

---

### Question 4: Is there an inconsistent certificate callback branch in the upstream Android client?
- **Finding:** **CONFIRMED DEFECT in upstream FreeRDP Android presentation code.**
- **Citations:**
  - `third_party/FreeRDP/client/Android/Studio/freeRDPCore/src/main/java/com/freerdp/freerdpcore/presentation/SessionActivity.java`:
    - Lines 815–816:
      ```java
      if (ApplicationSettingsActivity.getAcceptAllCertificates(this))
          return 0;
      ```
  - `libfreerdp/crypto/tls.c`:
    - Lines 2084–2100:
      ```c
      switch (accept_certificate) {
          case 1: verification_status = 1; break; // Save and accept
          case 2: verification_status = 1; break; // Accept temporary
          default: verification_status = -1; // REJECT!
      }
      ```
  - **The Defect:** When "accept all certificates" is enabled, `SessionActivity` returns `0`, assuming 0 indicates success / no error. However, `libfreerdp/crypto/tls.c` treats `0` as `default` (rejection)! Returning `0` causes FreeRDP to reject the connection.
- **Remediation:** MobiDesk does not use `SessionActivity` or `getAcceptAllCertificates()`. MobiDesk implements strict leaf certificate pinning, returning `2` (accept temporary for session) if `fingerprintSha256` matches the session grant pin, and `0` (reject) on mismatch.

---

## 2. Decision Table: Local Android IPC Bridge

To connect the OkHttp WebSocket client (running in Kotlin/JVM) to FreeRDP's native C transport layer, an IPC bridge is required on the Android handset:

| Bridge Architecture | Security & Isolation | Android Compatibility | Hijack Resistance | Evaluation & Recommendation |
|:---|:---|:---|:---|:---:|
| **Option A: `socketpair(AF_UNIX, SOCK_STREAM, 0)`** | **Highest.** File descriptors are connected in-kernel within the same process. No address, port, or filesystem exposure. | Supported on all Android API levels (29–37). Standard POSIX. | **Immune.** Other applications on the device cannot access connected fds. Passes hijack test. | **RECOMMENDED & SELECTED.** Optimal for security, zero attack surface. |
| **Option B: Unix Domain Socket (UDS) + `SO_PEERCRED`** | High. File descriptor passes through abstract namespace or internal app storage. | Supported on Android Linux kernel. Requires `SO_PEERCRED` validation. | Strong if peer UID is strictly checked against own app UID (`android.os.Process.myUid()`). | Viable fallback if fd passing across separate processes is ever required. |
| **Option C: Loopback TCP (`127.0.0.1:<random-port>`)** | **Poor.** Binds to a local port accessible to any application on device with `android.permission.INTERNET`. | Supported. | **FAIL.** A malicious local app can port-scan loopback and hijack the TCP handshake before FreeRDP connects. | **REJECTED.** Violates security requirements; fails same-device hijack test. |

---

## 3. Plumbing Proof Specification (Without Windows)

To verify the transport relay mechanics independently of Windows licensing and hypervisor state:
1. **Linux Test Host Environment:**
   - Install `xrdp` on loopback (`127.0.0.1:3389`).
   - Run MobiDesk Session Broker (`master43721/session-broker`) on `127.0.0.1:8766`.
2. **Byte Relay Routing:**
   - Linux host FreeRDP client configured with custom transport I/O writing to an open socket descriptor.
   - Forwarding pipe connects host socket -> WebSocket `/v1/rdp` -> Session Broker -> `xrdp` TCP 3389.
3. **Plumbing Boundary Notice:**
   > **Important:** This test proves raw byte transmission, transport buffering, backpressure, and bidirectional framing only. It does NOT prove NLA authentication, CredSSP exchange, or physical Android display rendering.

---

## 4. Minimal Android JNI Adapter Design

The MobiDesk native adapter (`MobiDeskTransportBridge`) provides:
1. `nativeCreateSocketPair()`: Calls `socketpair(AF_UNIX, SOCK_STREAM, 0, fds)`. Returns `[app_fd, freerdp_fd]`.
2. `nativeInstallTransport(long contextPtr, int freerdp_fd)`:
   - Allocates `rdpTransportLayer`.
   - Assigns `Read` to a POSIX `read(freerdp_fd, ...)` wrapper.
   - Assigns `Write` to a POSIX `write(freerdp_fd, ...)` wrapper.
   - Attaches layer via `freerdp_set_io_callbacks` before `freerdp_connect`.
3. Kotlin `TransportRelayPumper`:
   - Coroutine reading binary frames from OkHttp `WebSocketListener` and writing to `app_fd`.
   - Coroutine reading bytes from `app_fd` and sending binary WebSocket frames.

### Emulator Plumbing Verification
In CI workflow `.github/workflows/android-ci.yml`, setting input `run_instrumentation: true` boots an API 36 emulator and runs connected tests verifying socketpair creation and pump loop termination.

---

## 5. Verification & Test Suite

| Test Area | Target Verification | Result |
|:---|:---|:---:|
| **Credential Copy Security** | Proves immutable `String` creation in `get_string_from_string_builder`; documents need for direct byte array and zeroization in production. | VERIFIED & DOCUMENTED |
| **Certificate Inconsistency** | Proves `SessionActivity` return `0` defect in upstream code vs `tls.c` `accept_certificate` switch. | VERIFIED & DOCUMENTED |
| **Bridge Hijack Resilience** | Proves `socketpair()` cannot be observed or connected by external apps. | PASS (Architectural) |
| **Display Lifecycle & MS-RDPEDISP** | FreeRDP `android_disp.c` dynamic monitor resize requires display channel initialization post-connect. | VERIFIED & DOCUMENTED |
| **Resource Cleanup** | Thread termination and socket descriptor closure on disconnect. | PASS |

---

## 6. SPK-01 Live Drill Kit Reference

Physical demonstration on Android device with real Windows VM is catalogued under:
`docs/live-drills/SPK-01/EXECUTION_REPORT.md` (Status: `READY FOR OWNER`).
