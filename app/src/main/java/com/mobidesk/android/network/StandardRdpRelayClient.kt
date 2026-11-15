package com.mobidesk.android.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.URI
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLSocketFactory

/**
 * Standard RFC 6455 Binary WebSocket Client for the WSS RDP Byte-Stream Relay (FP-07).
 *
 * Implemented using pure standard Java/Android networking (Socket/SSLSocket) to eliminate
 * any external runtime dependency risk while strictly enforcing:
 * - Binary data frames only (text frames immediately abort with protocol error)
 * - Client-side frame masking per RFC 6455 §5.3
 * - Bounded message sizes (64 KiB maximum frame)
 * - Handling of WebSocket close codes (4401..4408 per MobiDesk Contract v1.1)
 */
class StandardRdpRelayClient(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val sslSocketFactory: SSLSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
) : RdpRelayClient {

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private val isConnected = AtomicBoolean(false)
    private var readJob: Job? = null
    private val secureRandom = SecureRandom()

    override fun connect(relayUrl: String, grantToken: String, listener: RdpRelayListener) {
        disconnect()

        val uri = URI(relayUrl)
        val scheme = uri.scheme?.lowercase() ?: "wss"
        val isSecure = scheme == "wss" || scheme == "https"
        val host = uri.host ?: "127.0.0.1"
        val port = if (uri.port != -1) uri.port else if (isSecure) 443 else 80
        val path = if (uri.rawPath.isNullOrBlank()) "/v1/rdp" else uri.rawPath

        readJob = coroutineScope.launch {
            try {
                val rawSocket = if (isSecure) {
                    sslSocketFactory.createSocket(host, port)
                } else {
                    Socket(host, port)
                }
                rawSocket.tcpNoDelay = true
                socket = rawSocket

                val out = rawSocket.getOutputStream()
                val inp = rawSocket.getInputStream()
                outputStream = out
                inputStream = inp

                // Perform WebSocket Upgrade Handshake
                val nonceBytes = ByteArray(16)
                secureRandom.nextBytes(nonceBytes)
                val key = Base64.getEncoder().encodeToString(nonceBytes)

                val hostHeader = if (port == 80 || port == 443) host else "$host:$port"
                val handshakeRequest = buildString {
                    append("GET ").append(path).append(" HTTP/1.1\r\n")
                    append("Host: ").append(hostHeader).append("\r\n")
                    append("Upgrade: websocket\r\n")
                    append("Connection: Upgrade\r\n")
                    append("Sec-WebSocket-Key: ").append(key).append("\r\n")
                    append("Sec-WebSocket-Version: 13\r\n")
                    if (grantToken.isNotBlank()) {
                        append("Authorization: Bearer ").append(grantToken).append("\r\n")
                    }
                    append("\r\n")
                }

                out.write(handshakeRequest.toByteArray(Charsets.US_ASCII))
                out.flush()

                // Read and validate HTTP 101 response
                val responseLine = readLine(inp)
                if (!responseLine.startsWith("HTTP/1.1 101") && !responseLine.startsWith("HTTP/1.0 101")) {
                    throw IllegalStateException("WebSocket handshake failed with response: $responseLine")
                }

                // Consume headers until blank line
                while (true) {
                    val header = readLine(inp)
                    if (header.isEmpty()) break
                }

                isConnected.set(true)
                listener.onConnected()

                // Read WebSocket frames
                while (isActive && isConnected.get()) {
                    val byte1 = inp.read()
                    if (byte1 == -1) break
                    val fin = (byte1 and 0x80) != 0
                    val opcode = byte1 and 0x0F

                    val byte2 = inp.read()
                    if (byte2 == -1) break
                    val isMasked = (byte2 and 0x80) != 0
                    var payloadLen = (byte2 and 0x7F).toLong()

                    if (payloadLen == 126L) {
                        val b0 = inp.read()
                        val b1 = inp.read()
                        if (b0 == -1 || b1 == -1) break
                        payloadLen = ((b0 and 0xFF) shl 8 or (b1 and 0xFF)).toLong()
                    } else if (payloadLen == 127L) {
                        val lenBytes = ByteArray(8)
                        readFully(inp, lenBytes)
                        payloadLen = ByteBuffer.wrap(lenBytes).long
                    }

                    if (payloadLen > 65536L) {
                        // Exceeds 64 KiB cap
                        disconnect(1009, "Frame exceeds 64 KiB cap")
                        listener.onClosed(1009, "Frame exceeds 64 KiB cap")
                        break
                    }

                    val maskKey = if (isMasked) {
                        val mask = ByteArray(4)
                        readFully(inp, mask)
                        mask
                    } else null

                    val payload = ByteArray(payloadLen.toInt())
                    readFully(inp, payload)

                    if (maskKey != null) {
                        for (i in payload.indices) {
                            payload[i] = (payload[i].toInt() xor maskKey[i % 4].toInt()).toByte()
                        }
                    }

                    when (opcode) {
                        0x01 -> {
                            // Text frame is prohibited per Contract v1.1 §6!
                            disconnect(1002, "Text frames prohibited")
                            listener.onClosed(1002, "Text frames prohibited")
                            break
                        }
                        0x00, 0x02 -> {
                            // Binary data frame (0x02) or Continuation frame (0x00) per RFC 6455 §5.4 / Contract v1.1 §6
                            listener.onBinaryMessage(payload)
                        }
                        0x08 -> {
                            // Close frame
                            val closeCode = if (payload.size >= 2) {
                                ((payload[0].toInt() and 0xFF) shl 8) or (payload[1].toInt() and 0xFF)
                            } else 1000
                            val reason = if (payload.size > 2) {
                                String(payload, 2, payload.size - 2, Charsets.UTF_8)
                            } else "Closed"
                            disconnect(closeCode, reason)
                            listener.onClosed(closeCode, reason)
                            break
                        }
                        0x09 -> {
                            // Ping -> reply with Pong (0x0A)
                            sendFrame(0x0A, payload)
                        }
                        0x0A -> {
                            // Pong
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Cancelled
            } catch (e: Exception) {
                if (isConnected.get()) {
                    listener.onFailure(e)
                }
            } finally {
                disconnect()
            }
        }
    }

    override fun sendBinary(data: ByteArray, offset: Int, length: Int): Boolean {
        if (!isConnected.get()) return false
        val chunk = if (offset == 0 && length == data.size) data else data.copyOfRange(offset, offset + length)
        return try {
            sendFrame(0x02, chunk)
            true
        } catch (_: Exception) {
            disconnect()
            false
        }
    }

    private fun sendFrame(opcode: Int, payload: ByteArray) {
        val out = outputStream ?: return
        synchronized(out) {
            val mask = ByteArray(4)
            secureRandom.nextBytes(mask)

            val header = ByteArrayOutputStream()
            header.write(0x80 or (opcode and 0x0F)) // FIN + opcode

            val len = payload.size
            if (len <= 125) {
                header.write(0x80 or len) // Masked + len
            } else if (len <= 65535) {
                header.write(0x80 or 126)
                header.write((len shr 8) and 0xFF)
                header.write(len and 0xFF)
            } else {
                header.write(0x80 or 127)
                val buffer = ByteBuffer.allocate(8).putLong(len.toLong()).array()
                header.write(buffer)
            }

            header.write(mask)

            val maskedPayload = ByteArray(payload.size)
            for (i in payload.indices) {
                maskedPayload[i] = (payload[i].toInt() xor mask[i % 4].toInt()).toByte()
            }

            out.write(header.toByteArray())
            out.write(maskedPayload)
            out.flush()
        }
    }

    override fun disconnect(code: Int, reason: String) {
        if (!isConnected.getAndSet(false)) return
        readJob?.cancel()
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        outputStream = null
        inputStream = null
    }

    private fun readLine(inp: InputStream): String {
        val baos = ByteArrayOutputStream()
        while (true) {
            val ch = inp.read()
            if (ch == -1) break
            if (ch == '\n'.code) {
                val bytes = baos.toByteArray()
                val len = if (bytes.isNotEmpty() && bytes.last() == '\r'.code.toByte()) bytes.size - 1 else bytes.size
                return String(bytes, 0, len, Charsets.US_ASCII)
            }
            baos.write(ch)
        }
        return baos.toString(Charsets.US_ASCII.name())
    }

    private fun readFully(inp: InputStream, dst: ByteArray) {
        var offset = 0
        while (offset < dst.size) {
            val read = inp.read(dst, offset, dst.size - offset)
            if (read == -1) throw IllegalStateException("Unexpected EOF reading frame payload")
            offset += read
        }
    }
}
