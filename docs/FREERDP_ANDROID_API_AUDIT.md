# FreeRDP Android API audit — 2026-11-03

## Scope and conclusion

This audit inspected the checked-in FreeRDP Android submodule at commit `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1` (v3.32.0). It was a source-only inspection; it did not modify FreeRDP, contact a VM, or prove an end-to-end RDP connection.

**Conclusion:** FreeRDP has native C transport-I/O extension points, but its checked-in Android Java wrapper does not expose a raw socket or arbitrary byte-stream injection API. A secure MobiDesk relay therefore needs a native/JNI integration spike before app-level RDP work can be considered viable. RD Gateway WebSocket support is not a transparent raw-TCP-over-WebSocket mode and must not be treated as one.

## Android session and rendering surface

The upstream `SessionActivity` is in `third_party/FreeRDP/client/Android/Studio/freeRDPCore/src/main/java/com/freerdp/freerdpcore/presentation/SessionActivity.java`. It parses a `freerdp://` URI, a session instance, or a bookmark/connection reference, then creates a session. Its connection helpers are private; the Activity is not a small public API for MobiDesk to pass a verified per-student VM target to.

The Android UI uses a bitmap-backed `SessionView` (`.../presentation/SessionView.java`), not a public `SurfaceView`/`SurfaceHolder` connection API. `SessionActivity.bindSession()` attaches a bitmap to the view and input manager; graphics updates replace the bitmap. Any MobiDesk-owned session Activity must handle surface lifecycle, resizing, input, and teardown explicitly.

## Guest credentials and NLA

The FreeRDP wrapper has native-to-Java authentication callbacks in `.../services/LibFreeRDP.java`, including `OnAuthenticate(StringBuilder username, StringBuilder domain, StringBuilder password)` and gateway authentication. The upstream session UI obtains credentials from its dialog callback. NLA is selected through the `/sec:nla` argument mapping in `LibFreeRDP.setConnectionInfo`.

The bookmark path also serializes username, domain, and password into FreeRDP command-line arguments. MobiDesk must not put Windows credentials in Supabase, WebSocket grants, intents, URIs, persistent bookmarks, analytics, or logs. Prefer a user-at-session-time callback and erase mutable credential buffers promptly; prove where the native layer copies them before calling that production-ready.

## Certificate verification

The native Android client installs `VerifyCertificateEx` and `VerifyChangedCertificateEx` callbacks in `client/Android/Studio/freeRDPCore/src/main/cpp/android_freerdp.c`; the callback data includes the host, port, fingerprint, certificate subjects/issuers, and changed-certificate details. The Java wrapper forwards these values to UI callbacks. The stock Android Java UI does not provide a first-class certificate-pin store or a MobiDesk-specific trust policy.

Before shipping, implement a fail-closed per-VM certificate policy (or another owner-approved cryptographic identity policy) and test first-use, changed-certificate rejection, unknown host, and replay. The upstream UI's accept-all preference and callback return behavior must not be reused without validating native return semantics; the source contains a suspiciously inconsistent-looking branch that requires a focused test.

## Raw transport hooks and the WebSocket distinction

FreeRDP exposes native callbacks in `third_party/FreeRDP/include/freerdp/transport_io.h`, including `pTransportLayerRead`, `pTransportLayerWrite`, `pTCPConnect`, `pTransportConnectLayer`, and the `rdpTransportIo` table. Public C functions include `freerdp_set_io_callbacks(rdpContext*, const rdpTransportIo*)` and `freerdp_set_io_callback_context(rdpContext*, void*)`. The C implementation copies/replaces the callback table, so an integration must preserve defaults it is not intentionally replacing and install the custom callbacks before `freerdp_connect`.

These hooks are **not exposed by the checked-in Android Java wrapper** (`LibFreeRDP.java`). The current wrapper's JNI exports provide instance creation, argument parsing, and connect, but no method for handing a Kotlin `Socket`, file descriptor, or raw stream to FreeRDP. The bridge cannot be completed by merely adding a WebSocket URL to the existing Compose UI.

FreeRDP's RD Gateway WebSocket implementation (`libfreerdp/core/gateway/rdg.c`, `http.c`, `websocket.c`, and `wst.c`) performs an HTTP WebSocket upgrade and adds RD Gateway packet/framing semantics. That is distinct from a broker that forwards opaque RDP TCP bytes inside binary WebSocket frames. It is not interchangeable with the PRD's raw relay.

## Manifest containment

The FreeRDP library manifest declares its own session, shortcut, settings, about, and help UI components. The MobiDesk app manifest already marks the upstream `SessionActivity` and `SessionRequestHandlerActivity` non-exported. The merged debug manifest showed other upstream shortcut/settings/help/about components still exported, despite not being part of the student app flow.

This continuation adds app-manifest overrides to close those unused exported components (and removes the shortcut alias). The next build must confirm the merged manifest contains no exported FreeRDP components. MobiDesk's own launcher/auth callback remains intentionally exported; the future MobiDesk RDP activity must be explicitly non-exported.

## Required implementation spike before production RDP work

1. Add a minimal native/JNI adapter that creates a FreeRDP context, registers a custom transport-I/O path before connect, and can move one bounded byte stream bidirectionally without exposing a VM address to the Android client.
2. Prove how this adapter composes with FreeRDP's TLS/NLA layer, certificate callback, Android input/render lifecycle, cancellation, and reconnect behavior. Do not assume a local loopback socket is safe merely because it binds to `127.0.0.1`; threat-test another local app and ensure one-use authorization, peer restrictions or an equivalent per-session secret, bounded lifetime, and single-use consumption.
3. Test a temporary assigned VM end to end with a throwaway Windows credential that is different from Supabase credentials, a strict certificate policy, and server logs that exclude tokens/passwords.
4. If the C/JNI transport spike cannot satisfy those checks with the pinned client, stop and select a different supported native transport/client before presenting any APK as final.

## Current evidence

- FreeRDP Android native source builds locally for both configured ABIs as part of `assembleDebug`.
- The local JVM unit-test task passed (2 tests, 0 failures); this is not an RDP or emulator acceptance test.
- No successful native RDP session through MobiDesk's broker has been demonstrated.
