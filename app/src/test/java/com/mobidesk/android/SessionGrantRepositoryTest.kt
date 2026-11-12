package com.mobidesk.android

import com.mobidesk.android.data.DefaultSessionGrantRepository
import com.mobidesk.android.data.MonotonicClock
import com.mobidesk.android.data.SessionGrant
import com.mobidesk.android.data.SessionGrantResult
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

class SessionGrantRepositoryTest {

    private lateinit var server: HttpServer
    private var serverPort: Int = 0
    private var handlerResponseCode: Int = 200
    private var handlerResponseBody: String = ""
    private var handlerHeaders: MutableMap<String, String> = mutableMapOf()
    private var receivedAuthHeader: String? = null
    private var receivedBody: String? = null

    private val fakeClock = object : MonotonicClock {
        var timeMs = 1000L
        override fun elapsedRealtime(): Long = timeMs
    }

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        serverPort = server.address.port
        server.createContext("/v1/session-grants", HttpHandler { exchange: HttpExchange ->
            receivedAuthHeader = exchange.requestHeaders.getFirst("Authorization")
            receivedBody = exchange.requestBody.reader().readText()

            handlerHeaders.forEach { (k, v) -> exchange.responseHeaders.set(k, v) }
            val bytes = handlerResponseBody.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(handlerResponseCode, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.close()
        })
        server.start()
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun blankAccessToken_failsImmediatelyAsAuthExpired() = runBlocking {
        val repo = DefaultSessionGrantRepository("http://127.0.0.1:$serverPort", fakeClock)
        val result = repo.requestSessionGrant("   ")
        assertTrue(result is SessionGrantResult.AuthExpired)
        assertEquals(null, receivedAuthHeader)
    }

    @Test
    fun successfulResponse_parsesGrantAndCertificatePin() = runBlocking {
        handlerResponseCode = 200
        handlerResponseBody = """
            {
                "grant": "token_abc_123",
                "expiresAt": "2026-11-05T12:01:00Z",
                "relayPath": "/v1/rdp",
                "rdpCertificate": {
                    "mode": "LEAF_X509_DER_SHA256",
                    "fingerprintSha256": "7f4a2c9e1d6b8035a7c4e2f90123456789abcdef0123456789abcdef01234567"
                },
                "requestId": "req_grant_1"
            }
        """.trimIndent()

        val repo = DefaultSessionGrantRepository("http://127.0.0.1:$serverPort", fakeClock)
        val result = repo.requestSessionGrant("valid-access-jwt")

        assertTrue(result is SessionGrantResult.Success)
        val grant = (result as SessionGrantResult.Success).grant
        assertEquals("token_abc_123", grant.grant)
        assertEquals("/v1/rdp", grant.relayPath)
        assertEquals("7f4a2c9e1d6b8035a7c4e2f90123456789abcdef0123456789abcdef01234567", grant.rdpCertificate.fingerprintSha256)
        assertEquals("Bearer valid-access-jwt", receivedAuthHeader)
        assertEquals("{}", receivedBody)
    }

    @Test
    fun rateLimited429_parsesRetryAfter() = runBlocking {
        handlerResponseCode = 429
        handlerHeaders["Retry-After"] = "15"
        handlerResponseBody = """{"code": "GRANT-02", "message": "Too many connection attempts."}"""

        val repo = DefaultSessionGrantRepository("http://127.0.0.1:$serverPort", fakeClock)
        val result = repo.requestSessionGrant("jwt-token")

        assertTrue(result is SessionGrantResult.RateLimited)
        val rate = result as SessionGrantResult.RateLimited
        assertEquals(15L, rate.retryAfterSeconds)
        assertEquals("Too many connection attempts.", rate.message)
    }

    @Test
    fun unauthorized401_returnsAuthExpired() = runBlocking {
        handlerResponseCode = 401
        handlerResponseBody = """{"code": "AUTH-01", "message": "Your sign-in session expired. Sign in again."}"""

        val repo = DefaultSessionGrantRepository("http://127.0.0.1:$serverPort", fakeClock)
        val result = repo.requestSessionGrant("jwt-token")

        assertTrue(result is SessionGrantResult.AuthExpired)
    }

    @Test
    fun forbidden403_returnsAuthExpired() = runBlocking {
        handlerResponseCode = 403
        handlerResponseBody = """{"code": "AUTH-01", "message": "Your sign-in session expired. Sign in again."}"""

        val repo = DefaultSessionGrantRepository("http://127.0.0.1:$serverPort", fakeClock)
        val result = repo.requestSessionGrant("jwt-token")

        assertTrue(result is SessionGrantResult.AuthExpired)
    }

    @Test
    fun grantExpiration_monotonicAgeVerification() {
        val grant = SessionGrant(
            grant = "test_grant",
            expiresAt = "2026-11-05T12:01:00Z",
            relayPath = "/v1/rdp",
            rdpCertificate = com.mobidesk.android.data.RdpCertificatePin("LEAF_X509_DER_SHA256", "abc"),
            requestId = "req_1",
            receivedAtElapsedMs = 1000L,
            maxDurationSeconds = 60L
        )

        assertFalse(grant.isExpired(1000L))
        assertFalse(grant.isExpired(60000L))
        assertTrue(grant.isExpired(61001L))
        assertTrue(grant.isExpired(100000L))
    }
}
