package com.mobidesk.android.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class DefaultSessionGrantRepository(
    private val baseUrl: String,
    private val clock: MonotonicClock = SystemMonotonicClock,
    private val connectTimeoutMs: Int = 8000,
    private val readTimeoutMs: Int = 8000
) : SessionGrantRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun requestSessionGrant(accessToken: String): SessionGrantResult = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext SessionGrantResult.AuthExpired("Your sign-in session expired. Sign in again.")
        }

        val requestUrl = if (baseUrl.endsWith("/")) "${baseUrl}v1/session-grants" else "$baseUrl/v1/session-grants"
        var connection: HttpURLConnection? = null
        val startElapsed = clock.elapsedRealtime()

        try {
            val url = URL(requestUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                doInput = true
                doOutput = true
                useCaches = false
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            // Per API Contract v1.1 §5: Send empty JSON object {}
            connection.outputStream.use { os ->
                os.write("{}".toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = connection.responseCode
            val endElapsed = clock.elapsedRealtime()

            when (responseCode) {
                HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED -> {
                    val body = connection.inputStream.use { readBounded(it) }
                    try {
                        val responseDto = json.decodeFromString<SessionGrantResponse>(body)
                        val grantToken = responseDto.grant
                        val certPin = responseDto.rdpCertificate

                        if (grantToken.isBlank()) {
                            SessionGrantResult.Error(
                                message = "We could not authorize your remote desktop session. Try again.",
                                isTransient = false
                            )
                        } else if (certPin == null || certPin.fingerprintSha256.isBlank()) {
                            SessionGrantResult.Error(
                                message = "The remote computer's security certificate could not be verified. Contact the lab administrator.",
                                isTransient = false
                            )
                        } else {
                            SessionGrantResult.Success(
                                SessionGrant(
                                    grant = grantToken,
                                    expiresAt = responseDto.expiresAt,
                                    relayPath = responseDto.relayPath.ifBlank { "/v1/rdp" },
                                    rdpCertificate = certPin,
                                    requestId = responseDto.requestId,
                                    receivedAtElapsedMs = endElapsed,
                                    maxDurationSeconds = 60L
                                )
                            )
                        }
                    } catch (e: Exception) {
                        SessionGrantResult.Error(
                            message = "We could not authorize your remote desktop session. Try again.",
                            isTransient = false
                        )
                    }
                }
                429 -> {
                    val retryAfterSeconds = parseRetryAfter(connection)
                    val body = connection.errorStream?.use { readBounded(it) }
                    val customMessage = body?.let { parseErrorMessage(it) }
                    SessionGrantResult.RateLimited(
                        retryAfterSeconds = retryAfterSeconds,
                        message = customMessage ?: "Too many connection attempts. Try again in $retryAfterSeconds seconds."
                    )
                }
                HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> {
                    SessionGrantResult.AuthExpired("Your sign-in session expired. Sign in again.")
                }
                else -> {
                    val body = connection.errorStream?.use { readBounded(it) }
                    val customMessage = body?.let { parseErrorMessage(it) }
                    SessionGrantResult.Error(
                        message = customMessage ?: "We could not authorize your remote desktop session. Try again.",
                        isTransient = responseCode in 500..599
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            SessionGrantResult.Error(
                message = "We could not reach the session broker in time. Try again.",
                isTransient = true
            )
        } catch (e: Exception) {
            SessionGrantResult.Error(
                message = "We could not authorize your remote desktop session. Try again.",
                isTransient = true
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun readBounded(stream: InputStream, maxBytes: Int = 65536): String {
        val buffer = ByteArray(1024)
        val baos = ByteArrayOutputStream()
        var total = 0
        var read: Int
        while (stream.read(buffer).also { read = it } != -1) {
            val toWrite = minOf(read, maxBytes - total)
            if (toWrite > 0) {
                baos.write(buffer, 0, toWrite)
                total += toWrite
            }
            if (total >= maxBytes) break
        }
        return baos.toString(Charsets.UTF_8.name())
    }

    private fun parseErrorMessage(jsonBody: String): String? {
        return try {
            val errorResponse = json.decodeFromString<ApiErrorResponse>(jsonBody)
            errorResponse.message?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRetryAfter(connection: HttpURLConnection): Long {
        val header = connection.getHeaderField("Retry-After")
        if (!header.isNullOrBlank()) {
            header.toLongOrNull()?.let { return it.coerceAtLeast(1L) }
        }
        return 10L
    }
}
