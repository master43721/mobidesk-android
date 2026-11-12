package com.mobidesk.android

import com.mobidesk.android.network.RdpRelayListener
import com.mobidesk.android.network.StandardRdpRelayClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class StandardRdpRelayClientTest {

    private lateinit var mockServer: ServerSocket
    private var serverPort: Int = 0

    @Before
    fun setUp() {
        mockServer = ServerSocket(0)
        serverPort = mockServer.localPort
    }

    @After
    fun tearDown() {
        mockServer.close()
    }

    @Test
    fun connect_sendsUpgradeAndBearerGrant_andExchangesBinaryFrames() {
        val connectedLatch = CountDownLatch(1)
        val messageReceivedLatch = CountDownLatch(1)
        var receivedBytesOnClient: ByteArray? = null

        val client = StandardRdpRelayClient(CoroutineScope(Dispatchers.IO))

        // Start mock server in thread
        val serverThread = Thread {
            val socket = mockServer.accept()
            val inp = socket.getInputStream()
            val out = socket.getOutputStream()

            // Read HTTP upgrade request
            val headers = readHeaders(inp)
            assertTrue(headers.contains("Upgrade: websocket"))
            assertTrue(headers.contains("Authorization: Bearer my-one-time-grant"))

            // Extract Sec-WebSocket-Key and compute Sec-WebSocket-Accept
            val key = headers.lines().first { it.startsWith("Sec-WebSocket-Key: ") }.removePrefix("Sec-WebSocket-Key: ").trim()
            val acceptKey = computeAcceptKey(key)

            // Send 101 Switching Protocols
            val response = "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: $acceptKey\r\n\r\n"
            out.write(response.toByteArray(Charsets.US_ASCII))
            out.flush()

            // Send a binary frame to client: Opcode 0x02, unmasked, 4 bytes [0x01, 0x02, 0x03, 0x04]
            out.write(byteArrayOf(0x82.toByte(), 0x04, 0x01, 0x02, 0x03, 0x04))
            out.flush()

            // Read binary frame sent from client
            val b1 = inp.read()
            val b2 = inp.read()
            assertEquals(0x82, b1) // FIN + Binary opcode
            assertTrue((b2 and 0x80) != 0) // Masked from client!

            socket.close()
        }
        serverThread.start()

        client.connect("ws://127.0.0.1:$serverPort/v1/rdp", "my-one-time-grant", object : RdpRelayListener {
            override fun onConnected() {
                connectedLatch.countDown()
            }

            override fun onBinaryMessage(data: ByteArray) {
                receivedBytesOnClient = data
                messageReceivedLatch.countDown()
            }

            override fun onClosed(code: Int, reason: String) {}
            override fun onFailure(error: Throwable) {}
        })

        assertTrue("Connected callback timed out", connectedLatch.await(5, TimeUnit.SECONDS))
        assertTrue("Message received timed out", messageReceivedLatch.await(5, TimeUnit.SECONDS))
        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04), receivedBytesOnClient)

        // Test sending binary frame from client
        val sent = client.sendBinary(byteArrayOf(0xAA.toByte(), 0xBB.toByte()))
        assertTrue("sendBinary should return true", sent)

        serverThread.join(2000)
        client.disconnect()
    }

    @Test
    fun textFrame_causesProtocolError_andCloses() {
        val closedLatch = CountDownLatch(1)
        var closeCode = 0

        val client = StandardRdpRelayClient(CoroutineScope(Dispatchers.IO))

        val serverThread = Thread {
            val socket = mockServer.accept()
            val inp = socket.getInputStream()
            val out = socket.getOutputStream()

            val headers = readHeaders(inp)
            val key = headers.lines().first { it.startsWith("Sec-WebSocket-Key: ") }.removePrefix("Sec-WebSocket-Key: ").trim()
            val acceptKey = computeAcceptKey(key)

            val response = "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: $acceptKey\r\n\r\n"
            out.write(response.toByteArray(Charsets.US_ASCII))
            out.flush()

            // Prohibited text frame: Opcode 0x01, payload "hello"
            out.write(byteArrayOf(0x81.toByte(), 0x05, 'h'.code.toByte(), 'e'.code.toByte(), 'l'.code.toByte(), 'l'.code.toByte(), 'o'.code.toByte()))
            out.flush()

            socket.close()
        }
        serverThread.start()

        client.connect("ws://127.0.0.1:$serverPort/v1/rdp", "grant", object : RdpRelayListener {
            override fun onConnected() {}
            override fun onBinaryMessage(data: ByteArray) {}
            override fun onClosed(code: Int, reason: String) {
                closeCode = code
                closedLatch.countDown()
            }
            override fun onFailure(error: Throwable) {}
        })

        assertTrue("Expected close callback on prohibited text frame", closedLatch.await(5, TimeUnit.SECONDS))
        assertEquals(1002, closeCode)

        serverThread.join(2000)
        client.disconnect()
    }

    @Test
    fun continuationFrame_deliversPayload() {
        val messageLatch = CountDownLatch(2)
        val receivedList = mutableListOf<ByteArray>()

        val client = StandardRdpRelayClient(CoroutineScope(Dispatchers.IO))

        val serverThread = Thread {
            val socket = mockServer.accept()
            val inp = socket.getInputStream()
            val out = socket.getOutputStream()

            val headers = readHeaders(inp)
            val key = headers.lines().first { it.startsWith("Sec-WebSocket-Key: ") }.removePrefix("Sec-WebSocket-Key: ").trim()
            val acceptKey = computeAcceptKey(key)

            val response = "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: $acceptKey\r\n\r\n"
            out.write(response.toByteArray(Charsets.US_ASCII))
            out.flush()

            // 1. Initial binary frame with FIN=0: Opcode 0x02, unmasked, 2 bytes [0x01, 0x02]
            out.write(byteArrayOf(0x02.toByte(), 0x02, 0x01, 0x02))
            out.flush()

            // 2. Continuation frame with FIN=1: Opcode 0x00, unmasked, 2 bytes [0x03, 0x04]
            out.write(byteArrayOf(0x80.toByte(), 0x02, 0x03, 0x04))
            out.flush()

            socket.close()
        }
        serverThread.start()

        client.connect("ws://127.0.0.1:$serverPort/v1/rdp", "grant", object : RdpRelayListener {
            override fun onConnected() {}
            override fun onBinaryMessage(data: ByteArray) {
                receivedList.add(data)
                messageLatch.countDown()
            }
            override fun onClosed(code: Int, reason: String) {}
            override fun onFailure(error: Throwable) {}
        })

        assertTrue("Expected 2 binary frame callbacks", messageLatch.await(5, TimeUnit.SECONDS))
        assertEquals(2, receivedList.size)
        assertArrayEquals(byteArrayOf(0x01, 0x02), receivedList[0])
        assertArrayEquals(byteArrayOf(0x03, 0x04), receivedList[1])

        serverThread.join(2000)
        client.disconnect()
    }

    private fun readHeaders(inp: InputStream): String {
        val sb = StringBuilder()
        var prev = -1
        while (true) {
            val ch = inp.read()
            if (ch == -1) break
            sb.append(ch.toChar())
            if (sb.endsWith("\r\n\r\n")) break
        }
        return sb.toString()
    }

    private fun computeAcceptKey(key: String): String {
        val guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        val sha1 = MessageDigest.getInstance("SHA-1")
        val hash = sha1.digest((key + guid).toByteArray(Charsets.US_ASCII))
        return Base64.getEncoder().encodeToString(hash)
    }
}
