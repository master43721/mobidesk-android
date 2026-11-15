package com.mobidesk.android.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Protected Local Transport Bridge connecting the Stock FreeRDP engine to the
 * WebSocket byte-stream relay (FP-08).
 *
 * Security Invariants:
 * 1. Binds exclusively to 127.0.0.1 on an OS-allocated dynamic port.
 * 2. Accepts exactly ONE peer connection. The [ServerSocket] listener is closed
 *    immediately upon accepting the first connection to prevent any malicious local app
 *    from connecting or hijacking the bridge.
 * 3. Bidirectional ordered byte streaming between the FreeRDP socket and [RdpRelayClient].
 * 4. Bounded I/O buffers (64 KiB) and deterministic resource cleanup.
 */
class LocalRdpBridge(
    private val relayClient: RdpRelayClient,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : Closeable {

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private val isRunning = AtomicBoolean(false)
    private var pumpJob: Job? = null

    private val pendingInboundQueue = java.util.concurrent.ConcurrentLinkedQueue<ByteArray>()
    private val socketLock = Any()

    val localPort: Int
        get() = serverSocket?.localPort ?: clientSocket?.localPort ?: -1

    /**
     * Initializes the loopback server socket on an ephemeral port.
     * Must be called before FreeRDP attempts to connect.
     */
    fun startListener(): Int {
        val ss = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        serverSocket = ss
        isRunning.set(true)
        return ss.localPort
    }

    /**
     * Waits for the single authorized connection from FreeRDP, closes the listener,
     * and begins the bidirectional byte pump.
     */
    fun acceptAndPump(onBridgeError: (Throwable) -> Unit = {}) {
        val ss = serverSocket ?: throw IllegalStateException("Bridge listener not started")
        pumpJob = coroutineScope.launch {
            try {
                // Accept the single connection from local FreeRDP
                val socket = ss.accept()

                // IMMEDIATELY close the server socket so no rogue app can connect!
                try {
                    ss.close()
                } catch (_: Exception) {}
                serverSocket = null

                socket.tcpNoDelay = true

                synchronized(socketLock) {
                    clientSocket = socket
                    val out = socket.getOutputStream()
                    while (!pendingInboundQueue.isEmpty()) {
                        val queued = pendingInboundQueue.poll() ?: break
                        out.write(queued)
                    }
                    out.flush()
                }

                // Start local socket -> WebSocket pump
                val inputStream: InputStream = socket.getInputStream()
                val buffer = ByteArray(65536) // 64 KiB buffer cap

                while (isActive && isRunning.get()) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break // EOF from FreeRDP
                    if (read > 0) {
                        val sent = relayClient.sendBinary(buffer, 0, read)
                        if (!sent) break
                    }
                }
            } catch (e: CancellationException) {
                // Normal cancellation
            } catch (e: Exception) {
                if (isRunning.get()) {
                    onBridgeError(e)
                }
            } finally {
                close()
            }
        }
    }

    /**
     * Forwards bytes received from the WebSocket relay into FreeRDP's socket.
     */
    fun writeToFreeRdp(data: ByteArray) {
        if (!isRunning.get()) return
        synchronized(socketLock) {
            val socket = clientSocket
            if (socket == null) {
                if (pendingInboundQueue.size < 16) {
                    pendingInboundQueue.add(data.clone())
                }
                return
            }
            if (socket.isClosed || !socket.isConnected) return
            try {
                val outputStream: OutputStream = socket.getOutputStream()
                outputStream.write(data)
                outputStream.flush()
            } catch (e: Exception) {
                close()
            }
        }
    }

    override fun close() {
        if (!isRunning.getAndSet(false)) return
        pendingInboundQueue.clear()
        pumpJob?.cancel()
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        synchronized(socketLock) {
            try {
                clientSocket?.close()
            } catch (_: Exception) {}
            clientSocket = null
        }
    }
}
