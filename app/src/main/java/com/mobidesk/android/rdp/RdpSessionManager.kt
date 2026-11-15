package com.mobidesk.android.rdp

import android.content.Context
import android.util.Log
import com.freerdp.freerdpcore.application.GlobalApp
import com.freerdp.freerdpcore.application.SessionState
import com.freerdp.freerdpcore.domain.BookmarkBase
import com.freerdp.freerdpcore.services.LibFreeRDP
import com.mobidesk.android.data.RdpCertificateVerifier
import com.mobidesk.android.data.SessionGrant
import com.mobidesk.android.data.WindowsGuestCredentials
import com.mobidesk.android.network.LocalRdpBridge
import com.mobidesk.android.network.RdpRelayClient
import com.mobidesk.android.network.RdpRelayListener
import com.mobidesk.android.network.StandardRdpRelayClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

enum class RdpSessionState {
    IDLE,
    CONNECTING_RELAY,
    RELAY_CONNECTED,
    RDP_CONNECTING,
    CONNECTED,
    DISCONNECTING,
    DISCONNECTED,
    ERROR
}

interface RdpSessionListener {
    fun onStateChanged(state: RdpSessionState)
    fun onCertificateVerified(fingerprint: String, accepted: Boolean)
    fun onGraphicsUpdate(x: Int, y: Int, width: Int, height: Int)
    fun onSettingsChanged(width: Int, height: Int, bpp: Int)
    fun onError(message: String, isFatal: Boolean)
}

/**
 * Orchestrates the full FreeRDP connection pipeline (FP-05 through FP-11).
 *
 * Sequence:
 * 1. Initializes [LocalRdpBridge] loopback listener (FP-08).
 * 2. Connects [RdpRelayClient] WSS stream with [SessionGrant] bearer token (FP-06, FP-07).
 * 3. On relay connection, begins bidirectional byte pumping on [LocalRdpBridge].
 * 4. Configures pinned FreeRDP engine targeting 127.0.0.1:<bridgePort> (FP-09).
 * 5. Handles NLA authentication callback with [WindowsGuestCredentials] (FP-05) and zeroes credentials.
 * 6. Verifies server leaf certificate via [RdpCertificateVerifier] (FP-10).
 * 7. Dispatches graphics updates and manages full lifecycle teardown.
 */
class RdpSessionManager(
    private val context: Context,
    private val relayClient: RdpRelayClient = StandardRdpRelayClient(),
    private val sessionScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) : Closeable {

    private val isClosed = AtomicBoolean(false)
    private var localBridge: LocalRdpBridge? = null
    private var certVerifier: RdpCertificateVerifier? = null
    private var guestCredentials: WindowsGuestCredentials? = null
    private var freerdpSession: SessionState? = null
    private var sessionListener: RdpSessionListener? = null
    private var connectThread: Thread? = null

    @Volatile
    var currentState: RdpSessionState = RdpSessionState.IDLE
        private set

    fun setListener(listener: RdpSessionListener?) {
        this.sessionListener = listener
    }

    private fun updateState(newState: RdpSessionState) {
        currentState = newState
        sessionScope.launch {
            sessionListener?.onStateChanged(newState)
        }
    }

    /**
     * Initiates the full RDP connection pipeline.
     */
    fun startSession(
        grant: SessionGrant,
        credentials: WindowsGuestCredentials,
        relayUrl: String,
        desktopWidth: Int = 1280,
        desktopHeight: Int = 720
    ) {
        if (isClosed.get()) return
        this.guestCredentials = credentials
        this.certVerifier = RdpCertificateVerifier(grant.rdpCertificate.fingerprintSha256)

        updateState(RdpSessionState.CONNECTING_RELAY)

        // 1. Initialize the loopback bridge listener
        val bridge = LocalRdpBridge(relayClient)
        localBridge = bridge
        val localPort = bridge.startListener()

        // 2. Connect the WebSocket relay to the broker
        relayClient.connect(relayUrl, grant.grant, object : RdpRelayListener {
            override fun onConnected() {
                updateState(RdpSessionState.RELAY_CONNECTED)
                // 3. Start bridge pumping FreeRDP <-> WebSocket
                bridge.acceptAndPump { error ->
                    sessionScope.launch {
                        sessionListener?.onError("Local bridge communication failed", true)
                        disconnect()
                    }
                }

                // 4. Launch FreeRDP connection
                startFreeRdp(localPort, desktopWidth, desktopHeight)
            }

            override fun onBinaryMessage(data: ByteArray) {
                bridge.writeToFreeRdp(data)
            }

            override fun onClosed(code: Int, reason: String) {
                sessionScope.launch {
                    val msg = when (code) {
                        4401 -> "We could not authorize a session. Try again."
                        4402 -> "This session request expired. Try connecting again."
                        4403 -> "Your session was ended by the administrator or access policy."
                        4405 -> "The Cloud PC security certificate could not be verified. Connection blocked."
                        4406 -> "Connection lost. Reconnecting securely…"
                        else -> "Session closed ($reason)"
                    }
                    sessionListener?.onError(msg, code != 1000)
                    disconnect()
                }
            }

            override fun onFailure(error: Throwable) {
                sessionScope.launch {
                    sessionListener?.onError("Relay connection failed: ${error.message}", true)
                    disconnect()
                }
            }
        })
    }

    private fun startFreeRdp(localPort: Int, width: Int, height: Int) {
        updateState(RdpSessionState.RDP_CONNECTING)

        val bookmark = BookmarkBase().apply {
            setHostname("127.0.0.1")
            setPort(localPort)
            setUsername("") // Provided dynamically via OnAuthenticate callback
            setPassword("")
            setDomain("")
            activeScreenSettings.apply {
                this.width = width
                this.height = height
                setResolution(BookmarkBase.ScreenSettings.CUSTOM)
            }
            advancedSettings.apply {
                security = 3 // NLA (Network Level Authentication)
            }
            activePerformanceFlags.apply {
                setRemoteFX(false)
                setGfx(true)
            }
        }

        try {
            val session = GlobalApp.createSession(bookmark, context.applicationContext)
            freerdpSession = session

            session.setUIEventListener(object : LibFreeRDP.UIEventListener {
                override fun OnSettingsChanged(w: Int, h: Int, bpp: Int) {
                    sessionScope.launch {
                        sessionListener?.onSettingsChanged(w, h, bpp)
                    }
                }

                override fun OnAuthenticate(
                    usernameBuilder: StringBuilder,
                    domainBuilder: StringBuilder,
                    passwordBuilder: StringBuilder
                ): Boolean {
                    val creds = guestCredentials ?: return false
                    if (creds.isDestroyed || !creds.isValid) return false

                    usernameBuilder.append(String(creds.username))
                    creds.domain?.let { domainBuilder.append(String(it)) }
                    passwordBuilder.append(String(creds.password))

                    // Zero memory immediately after handing off to FreeRDP
                    creds.clear()
                    return true
                }

                override fun OnGatewayAuthenticate(
                    username: StringBuilder?,
                    domain: StringBuilder?,
                    password: StringBuilder?
                ): Boolean = false

                override fun OnVerifiyCertificateEx(
                    host: String?,
                    port: Long,
                    commonName: String?,
                    subject: String?,
                    issuer: String?,
                    fingerprint: String?,
                    flags: Long
                ): Int {
                    val verifier = certVerifier ?: return RdpCertificateVerifier.REJECT
                    val result = verifier.verify(fingerprint)
                    val accepted = result == RdpCertificateVerifier.ACCEPT_TEMPORARY
                    sessionScope.launch {
                        sessionListener?.onCertificateVerified(fingerprint.orEmpty(), accepted)
                    }
                    return result
                }

                override fun OnVerifyChangedCertificateEx(
                    host: String?,
                    port: Long,
                    commonName: String?,
                    subject: String?,
                    issuer: String?,
                    fingerprint: String?,
                    oldSubject: String?,
                    oldIssuer: String?,
                    oldFingerprint: String?,
                    flags: Long
                ): Int {
                    return OnVerifiyCertificateEx(host, port, commonName, subject, issuer, fingerprint, flags)
                }

                override fun OnExperimentalFeature(feature: Int): Boolean = false

                override fun OnGraphicsUpdate(x: Int, y: Int, w: Int, h: Int) {
                    sessionScope.launch {
                        if (currentState != RdpSessionState.CONNECTED) {
                            updateState(RdpSessionState.CONNECTED)
                        }
                        sessionListener?.onGraphicsUpdate(x, y, w, h)
                    }
                }

                override fun OnGraphicsResize(w: Int, h: Int, bpp: Int) {
                    sessionScope.launch {
                        sessionListener?.onSettingsChanged(w, h, bpp)
                    }
                }

                override fun OnRemoteClipboardChanged(data: String?) {}
                override fun OnRemoteClipboardImageChanged(data: ByteArray?) {}
                override fun OnPointerSet(pixels: IntArray?, width: Int, height: Int, hotX: Int, hotY: Int) {}
                override fun OnPointerSetNull() {}
                override fun OnPointerSetDefault() {}
                override fun OnRailWindowUpdate(windowId: Long, width: Int, height: Int, pixels: IntArray?) {}
                override fun OnRailWindowMove(windowId: Long, x: Int, y: Int, w: Int, h: Int) {}
                override fun OnRailWindowHide(windowId: Long) {}
                override fun OnRailWindowDestroy(windowId: Long) {}
                override fun OnRailSessionEnd() {}
                override fun OnRailMonitoredDesktop(windowIds: LongArray?, activeWindowId: Long) {}
            })

            connectThread = Thread({
                try {
                    session.connect(context.applicationContext)
                } catch (e: Exception) {
                    sessionScope.launch {
                        sessionListener?.onError("FreeRDP connection error: ${e.message}", true)
                        disconnect()
                    }
                }
            }, "MobiDesk-FreeRDP-Worker").apply {
                isDaemon = true
                start()
            }
        } catch (e: Exception) {
            sessionScope.launch {
                sessionListener?.onError("Failed to initialize FreeRDP engine: ${e.message}", true)
                disconnect()
            }
        }
    }

    fun getSessionState(): SessionState? = freerdpSession

    fun sendCursorEvent(x: Int, y: Int, flags: Int): Boolean {
        val inst = freerdpSession?.instance ?: return false
        return LibFreeRDP.sendCursorEvent(inst, x, y, flags)
    }

    fun sendKeyEvent(keyCode: Int, down: Boolean): Boolean {
        val inst = freerdpSession?.instance ?: return false
        return LibFreeRDP.sendKeyEvent(inst, keyCode, down)
    }

    fun sendUnicodeKeyEvent(unicode: Int, down: Boolean): Boolean {
        val inst = freerdpSession?.instance ?: return false
        return LibFreeRDP.sendUnicodeKeyEvent(inst, unicode, down)
    }

    fun disconnect() {
        if (currentState == RdpSessionState.DISCONNECTED || currentState == RdpSessionState.DISCONNECTING) return
        updateState(RdpSessionState.DISCONNECTING)

        try {
            connectThread?.interrupt()
            connectThread = null

            freerdpSession?.let {
                try {
                    LibFreeRDP.disconnect(it.instance)
                    GlobalApp.freeSession(it.instance)
                } catch (_: Exception) {}
            }
            freerdpSession = null

            localBridge?.close()
            localBridge = null

            relayClient.disconnect()

            guestCredentials?.clear()
            guestCredentials = null
        } finally {
            updateState(RdpSessionState.DISCONNECTED)
        }
    }

    override fun close() {
        if (isClosed.getAndSet(true)) return
        disconnect()
        sessionScope.cancel()
    }
}
