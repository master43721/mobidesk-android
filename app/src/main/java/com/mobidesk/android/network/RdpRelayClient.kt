package com.mobidesk.android.network

import java.io.Closeable

/**
 * Listener for WebSocket events in the WSS RDP byte-stream relay (FP-07).
 */
interface RdpRelayListener {
    fun onConnected()
    fun onBinaryMessage(data: ByteArray)
    fun onClosed(code: Int, reason: String)
    fun onFailure(error: Throwable)
}

/**
 * Contract for the WSS RDP byte-stream relay client (FP-07).
 *
 * Enforces:
 * - Binary frame relay only (text frames cause protocol abort)
 * - Maximum frame chunk size: 64 KiB (65536 bytes)
 * - Single-use grant bearer authentication in headers
 */
interface RdpRelayClient : Closeable {
    fun connect(relayUrl: String, grantToken: String, listener: RdpRelayListener)
    fun sendBinary(data: ByteArray, offset: Int = 0, length: Int = data.size): Boolean
    fun disconnect(code: Int = 1000, reason: String = "Normal closure")
    override fun close() {
        disconnect()
    }
}
