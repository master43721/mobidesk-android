package com.mobidesk.android

import com.mobidesk.android.network.LocalRdpBridge
import com.mobidesk.android.network.RdpRelayClient
import com.mobidesk.android.network.RdpRelayListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class LocalRdpBridgeTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private class FakeRelayClient : RdpRelayClient {
        val sentData = mutableListOf<ByteArray>()
        override fun connect(relayUrl: String, grantToken: String, listener: RdpRelayListener) {}
        override fun sendBinary(data: ByteArray, offset: Int, length: Int): Boolean {
            sentData.add(data.copyOfRange(offset, offset + length))
            return true
        }
        override fun disconnect(code: Int, reason: String) {}
    }

    @Test
    fun startListener_bindsToLoopbackPort() {
        val relayClient = FakeRelayClient()
        val bridge = LocalRdpBridge(relayClient, testScope)

        val port = bridge.startListener()
        assertTrue("Port should be dynamic ephemeral port (> 0)", port > 0)
        assertEquals(port, bridge.localPort)

        bridge.close()
    }

    @Test
    fun acceptAndPump_acceptsOnePeerAndImmediatelyClosesListener() {
        val relayClient = FakeRelayClient()
        val bridge = LocalRdpBridge(relayClient, testScope)
        val port = bridge.startListener()

        val connectedLatch = CountDownLatch(1)
        bridge.acceptAndPump { }

        // Client 1 (Simulating FreeRDP) connects
        val client1 = Socket("127.0.0.1", port)
        assertTrue(client1.isConnected)

        // Give bridge a moment to accept and close ServerSocket
        Thread.sleep(50)

        // Rogue Client 2 attempts to connect to the same port -> MUST FAIL!
        var secondConnectionFailed = false
        try {
            val client2 = Socket("127.0.0.1", port)
            client2.close()
        } catch (_: ConnectException) {
            secondConnectionFailed = true
        }

        assertTrue("Second connection attempt must fail because bridge listener closed", secondConnectionFailed)

        // Test sending bytes from FreeRDP to RelayClient
        val testPayload = byteArrayOf(0x03, 0x00, 0x00, 0x13, 0x0e, 0xe0.toByte())
        val out: OutputStream = client1.getOutputStream()
        out.write(testPayload)
        out.flush()

        Thread.sleep(50)
        assertEquals(1, relayClient.sentData.size)
        assertArrayEquals(testPayload, relayClient.sentData[0])

        // Test writing bytes from RelayClient to FreeRDP
        val relayPayload = byteArrayOf(0x03, 0x00, 0x00, 0x0b, 0x06, 0xd0.toByte())
        bridge.writeToFreeRdp(relayPayload)

        val inp: InputStream = client1.getInputStream()
        val buf = ByteArray(relayPayload.size)
        val read = inp.read(buf)
        assertEquals(relayPayload.size, read)
        assertArrayEquals(relayPayload, buf)

        client1.close()
        bridge.close()
    }

    @Test
    fun writeToFreeRdp_beforePeerConnects_buffersAndFlushesOnAccept() {
        val relayClient = FakeRelayClient()
        val bridge = LocalRdpBridge(relayClient, testScope)
        val port = bridge.startListener()

        // Relay receives packets BEFORE FreeRDP connects
        val earlyPacket = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        bridge.writeToFreeRdp(earlyPacket)

        bridge.acceptAndPump { }

        // FreeRDP connects after packet was already queued
        val client = Socket("127.0.0.1", port)
        val inp: InputStream = client.getInputStream()
        val buf = ByteArray(earlyPacket.size)
        val read = inp.read(buf)

        assertEquals(earlyPacket.size, read)
        assertArrayEquals(earlyPacket, buf)

        client.close()
        bridge.close()
    }
}
