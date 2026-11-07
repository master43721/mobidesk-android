package com.mobidesk.android

import com.mobidesk.android.data.DefaultStatusRepository
import com.mobidesk.android.data.MonotonicClock
import com.mobidesk.android.data.RdpTransport
import com.mobidesk.android.data.StatusResult
import com.mobidesk.android.data.VmLifecycle
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class StatusRepositoryContractTest {

    private lateinit var serverSocket: ServerSocket
    private var serverPort: Int = 0
    private val isRunning = AtomicBoolean(true)

    class FakeMonotonicClock(var timeMs: Long = 1000L) : MonotonicClock {
        override fun elapsedRealtime(): Long = timeMs
    }

    private val fakeClock = FakeMonotonicClock(5000L)

    @Before
    fun setup() {
        serverSocket = ServerSocket(0)
        serverPort = serverSocket.localPort
        isRunning.set(true)
    }

    @After
    fun tearDown() {
        isRunning.set(false)
        runCatching { serverSocket.close() }
    }

    @Test
    fun getVmStatus_successContract() = runBlocking {
        val jsonResponse = """{"lifecycle":"RUNNING","rdpTransport":"RDP_NEGOTIATION_OK","sampledAt":"2026-11-05T10:00:00Z","probeDurationMs":250,"staleAfterSeconds":15,"requestId":"req_123","assignmentTag":"asg_tag_vm1"}"""

        var recordedMethod: String? = null
        var recordedUri: String? = null
        var recordedAuth: String? = null

        val serverThread = thread {
            try {
                val client = serverSocket.accept()
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val line = reader.readLine()
                val parts = line?.split(" ")
                if (parts != null && parts.size >= 2) {
                    recordedMethod = parts[0]
                    recordedUri = parts[1]
                }
                var headerLine = reader.readLine()
                while (!headerLine.isNullOrBlank()) {
                    if (headerLine.startsWith("Authorization:", ignoreCase = true)) {
                        recordedAuth = headerLine.substringAfter(":").trim()
                    }
                    headerLine = reader.readLine()
                }

                val writer = OutputStreamWriter(client.getOutputStream())
                writer.write("HTTP/1.1 200 OK\r\n")
                writer.write("Content-Type: application/json\r\n")
                writer.write("Content-Length: ${jsonResponse.toByteArray().size}\r\n")
                writer.write("\r\n")
                writer.write(jsonResponse)
                writer.flush()
                client.close()
            } catch (ignored: Exception) {}
        }

        val repository = DefaultStatusRepository(
            baseUrl = "http://127.0.0.1:$serverPort",
            clock = fakeClock
        )

        val result = repository.getVmStatus("test-access-token")
        serverThread.join(2000)

        assertEquals("GET", recordedMethod)
        assertEquals("/v1/vm-status", recordedUri)
        assertEquals("Bearer test-access-token", recordedAuth)

        assertTrue(result is StatusResult.Success)
        val success = result as StatusResult.Success
        assertEquals(VmLifecycle.RUNNING, success.sample.lifecycle)
        assertEquals(RdpTransport.RDP_NEGOTIATION_OK, success.sample.rdpTransport)
        assertEquals("asg_tag_vm1", success.sample.assignmentTag)
        assertEquals(15L, success.sample.staleAfterSeconds)
        assertEquals("req_123", success.sample.requestId)
    }

    @Test
    fun getVmStatus_missingAssignmentTag_failsClosed() = runBlocking {
        val jsonResponse = """{"lifecycle":"RUNNING","rdpTransport":"RDP_NEGOTIATION_OK","sampledAt":"2026-11-05T10:00:00Z","probeDurationMs":250,"staleAfterSeconds":15,"requestId":"req_123"}"""

        val serverThread = thread {
            try {
                val client = serverSocket.accept()
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                while (!reader.readLine().isNullOrBlank()) {}
                val writer = OutputStreamWriter(client.getOutputStream())
                writer.write("HTTP/1.1 200 OK\r\n")
                writer.write("Content-Type: application/json\r\n")
                writer.write("Content-Length: ${jsonResponse.toByteArray().size}\r\n")
                writer.write("\r\n")
                writer.write(jsonResponse)
                writer.flush()
                client.close()
            } catch (ignored: Exception) {}
        }

        val repository = DefaultStatusRepository(
            baseUrl = "http://127.0.0.1:$serverPort",
            clock = fakeClock
        )

        val result = repository.getVmStatus("token")
        serverThread.join(2000)

        assertTrue(result is StatusResult.Error)
        assertEquals("We could not verify your assigned Cloud PC. Try again.", (result as StatusResult.Error).message)
    }

    @Test
    fun getVmStatus_rateLimitedWithRetryAfterHeaderAndBody() = runBlocking {
        val jsonResponse = """{"code":"ASSIGN-02","message":"We could not verify your assigned Cloud PC. Try again.","retryable":true,"requestId":"req_rate","retryAfterSeconds":10}"""

        val serverThread = thread {
            try {
                val client = serverSocket.accept()
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                while (!reader.readLine().isNullOrBlank()) {}
                val writer = OutputStreamWriter(client.getOutputStream())
                writer.write("HTTP/1.1 429 Too Many Requests\r\n")
                writer.write("Content-Type: application/json\r\n")
                writer.write("Retry-After: 5\r\n")
                writer.write("Content-Length: ${jsonResponse.toByteArray().size}\r\n")
                writer.write("\r\n")
                writer.write(jsonResponse)
                writer.flush()
                client.close()
            } catch (ignored: Exception) {}
        }

        val repository = DefaultStatusRepository(
            baseUrl = "http://127.0.0.1:$serverPort",
            clock = fakeClock
        )

        val result = repository.getVmStatus("token")
        serverThread.join(2000)

        assertTrue(result is StatusResult.RateLimited)
        val rateLimited = result as StatusResult.RateLimited
        assertEquals(10L, rateLimited.retryAfterSeconds)
        assertEquals("We could not verify your assigned Cloud PC. Try again.", rateLimited.message)
    }

    @Test
    fun getVmStatus_rateLimitedWithUntrustedServerMessage_preservesCatalogCopy() = runBlocking {
        val jsonResponse = """{"code":"RATE_LIMIT","message":"Custom internal gateway rate limit notice","retryable":true,"retryAfterSeconds":7}"""

        val serverThread = thread {
            try {
                val client = serverSocket.accept()
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                while (!reader.readLine().isNullOrBlank()) {}
                val writer = OutputStreamWriter(client.getOutputStream())
                writer.write("HTTP/1.1 429 Too Many Requests\r\n")
                writer.write("Content-Type: application/json\r\n")
                writer.write("Content-Length: ${jsonResponse.toByteArray().size}\r\n")
                writer.write("\r\n")
                writer.write(jsonResponse)
                writer.flush()
                client.close()
            } catch (ignored: Exception) {}
        }

        val repository = DefaultStatusRepository(
            baseUrl = "http://127.0.0.1:$serverPort",
            clock = fakeClock
        )

        val result = repository.getVmStatus("token")
        serverThread.join(2000)

        assertTrue(result is StatusResult.RateLimited)
        val rateLimited = result as StatusResult.RateLimited
        assertEquals(7L, rateLimited.retryAfterSeconds)
        assertEquals("We could not verify your assigned Cloud PC. Try again.", rateLimited.message)
    }

    @Test
    fun getVmStatus_unauthorizedReturnsAuthExpired() = runBlocking {
        val serverThread = thread {
            try {
                val client = serverSocket.accept()
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                while (!reader.readLine().isNullOrBlank()) {}
                val writer = OutputStreamWriter(client.getOutputStream())
                writer.write("HTTP/1.1 401 Unauthorized\r\n")
                writer.write("Content-Length: 0\r\n")
                writer.write("\r\n")
                writer.flush()
                client.close()
            } catch (ignored: Exception) {}
        }

        val repository = DefaultStatusRepository(
            baseUrl = "http://127.0.0.1:$serverPort",
            clock = fakeClock
        )

        val result = repository.getVmStatus("expired-token")
        serverThread.join(2000)

        assertTrue(result is StatusResult.AuthExpired)
        assertEquals("Your sign-in session expired. Sign in again.", (result as StatusResult.AuthExpired).message)
    }

    @Test
    fun getVmStatus_timeoutReturnsError() = runBlocking {
        val serverThread = thread {
            try {
                val client = serverSocket.accept()
                Thread.sleep(500)
                client.close()
            } catch (ignored: Exception) {}
        }

        val repository = DefaultStatusRepository(
            baseUrl = "http://127.0.0.1:$serverPort",
            clock = fakeClock,
            connectTimeoutMs = 100,
            readTimeoutMs = 100
        )

        val result = repository.getVmStatus("token")
        serverThread.join(2000)

        assertTrue(result is StatusResult.Error)
        assertEquals(
            "We could not verify your assigned Cloud PC. Try again.",
            (result as StatusResult.Error).message
        )
    }

    @Test
    fun getVmStatus_oversizedPayloadFailsClosed() = runBlocking {
        val largePadding = "x".repeat(70000)
        val payload = """{"lifecycle":"RUNNING","rdpTransport":"RDP_NEGOTIATION_OK","sampledAt":"2026-11-03T06:00:00Z","probeDurationMs":384,"staleAfterSeconds":15,"requestId":"req_1","assignmentTag":"asg_1","padding":"$largePadding"}"""
        val response = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${payload.toByteArray().size}\r\nConnection: close\r\n\r\n$payload"

        val serverThread = thread {
            try {
                val client = serverSocket.accept()
                client.getOutputStream().write(response.toByteArray(Charsets.UTF_8))
                client.getOutputStream().flush()
                client.close()
            } catch (ignored: Exception) {}
        }

        val repository = DefaultStatusRepository(
            baseUrl = "http://127.0.0.1:$serverPort",
            clock = fakeClock
        )

        val result = repository.getVmStatus("token")
        serverThread.join(2000)

        assertTrue("Oversized stream must fail closed to StatusResult.Error", result is StatusResult.Error)
        assertEquals(
            "We could not verify your assigned Cloud PC. Try again.",
            (result as StatusResult.Error).message
        )
    }
}
