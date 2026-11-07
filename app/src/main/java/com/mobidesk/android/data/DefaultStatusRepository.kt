package com.mobidesk.android.data

import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

interface MonotonicClock {
    fun elapsedRealtime(): Long
}

object SystemMonotonicClock : MonotonicClock {
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
}

class DefaultStatusRepository(
    private val baseUrl: String,
    private val clock: MonotonicClock = SystemMonotonicClock,
    private val connectTimeoutMs: Int = 8000,
    private val readTimeoutMs: Int = 8000
) : StatusRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun getVmStatus(accessToken: String): StatusResult = withContext(Dispatchers.IO) {
        val requestUrl = if (baseUrl.endsWith("/")) "${baseUrl}v1/vm-status" else "$baseUrl/v1/vm-status"
        var connection: HttpURLConnection? = null
        val startElapsed = clock.elapsedRealtime()

        try {
            val url = URL(requestUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                doInput = true
                useCaches = false
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            val endElapsed = clock.elapsedRealtime()

            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    try {
                        val responseDto = json.decodeFromString<VmStatusResponse>(body)
                        val assignmentTag = responseDto.assignmentTag
                        if (assignmentTag.isNullOrBlank()) {
                            StatusResult.Error(
                                message = "We could not verify your assigned Cloud PC. Try again.",
                                isTransient = false
                            )
                        } else {
                            val sample = VmStatusSample(
                                lifecycle = responseDto.lifecycle,
                                rdpTransport = responseDto.rdpTransport,
                                sampledAtFormatted = responseDto.sampledAt,
                                probeDurationMs = responseDto.probeDurationMs,
                                staleAfterSeconds = if (responseDto.staleAfterSeconds > 0) responseDto.staleAfterSeconds else 15L,
                                requestId = responseDto.requestId,
                                assignmentTag = assignmentTag,
                                requestStartedElapsedMs = startElapsed,
                                responseReceivedElapsedMs = endElapsed
                            )
                            StatusResult.Success(sample)
                        }
                    } catch (e: Exception) {
                        StatusResult.Error(
                            message = "We could not verify your assigned Cloud PC. Try again.",
                            isTransient = true
                        )
                    }
                }
                429 -> {
                    val retryAfterHeader = connection.getHeaderField("Retry-After")?.toLongOrNull()
                    val errorBody = try {
                        val stream = connection.errorStream ?: connection.inputStream
                        stream?.bufferedReader()?.use { it.readText() }
                    } catch (e: Exception) {
                        null
                    }

                    var bodyRetryAfter: Long? = null
                    var errorMsg = "We could not verify your assigned Cloud PC. Try again."
                    if (!errorBody.isNullOrBlank()) {
                        try {
                            val errorDto = json.decodeFromString<ApiErrorResponse>(errorBody)
                            bodyRetryAfter = errorDto.retryAfterSeconds
                            if (!errorDto.message.isNullOrBlank()) {
                                errorMsg = errorDto.message
                            }
                        } catch (ignored: Exception) {}
                    }

                    val candidate = maxOf(retryAfterHeader ?: 0L, bodyRetryAfter ?: 0L)
                    val retryAfterSeconds = if (candidate > 0L) candidate else 2L

                    StatusResult.RateLimited(
                        retryAfterSeconds = retryAfterSeconds,
                        message = errorMsg
                    )
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    StatusResult.AuthExpired("Your sign-in session expired. Sign in again.")
                }
                else -> {
                    StatusResult.Error(
                        message = "We could not verify your assigned Cloud PC. Try again.",
                        isTransient = true
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            StatusResult.Error(
                message = "We couldn't reach the sign-in service. Check your connection and try again.",
                isTransient = true
            )
        } catch (e: Exception) {
            StatusResult.Error(
                message = "We couldn't reach the sign-in service. Check your connection and try again.",
                isTransient = true
            )
        } finally {
            connection?.disconnect()
        }
    }
}
