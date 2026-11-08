package com.mobidesk.android.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class VmLifecycle {
    @SerialName("STOPPED")
    STOPPED,
    @SerialName("STARTING")
    STARTING,
    @SerialName("RUNNING")
    RUNNING,
    @SerialName("STOPPING")
    STOPPING,
    @SerialName("ERROR")
    ERROR,
    @SerialName("UNKNOWN")
    UNKNOWN;

    val isKnownNegative: Boolean
        get() = this == STOPPED || this == STARTING || this == STOPPING || this == ERROR
}

@Serializable
enum class RdpTransport {
    @SerialName("NOT_CHECKED")
    NOT_CHECKED,
    @SerialName("TCP_UNREACHABLE")
    TCP_UNREACHABLE,
    @SerialName("TCP_REACHABLE")
    TCP_REACHABLE,
    @SerialName("RDP_NEGOTIATION_OK")
    RDP_NEGOTIATION_OK,
    @SerialName("UNKNOWN")
    UNKNOWN;

    val isKnownNegative: Boolean
        get() = this == TCP_UNREACHABLE || this == TCP_REACHABLE
}

@Serializable
data class VmStatusResponse(
    val lifecycle: VmLifecycle = VmLifecycle.UNKNOWN,
    val rdpTransport: RdpTransport = RdpTransport.UNKNOWN,
    val sampledAt: String? = null,
    val probeDurationMs: Long = 0L,
    val staleAfterSeconds: Long = 15L,
    val requestId: String? = null,
    val assignmentTag: String? = null
)

@Serializable
data class ApiErrorResponse(
    val code: String? = null,
    val message: String? = null,
    val retryable: Boolean = false,
    val requestId: String? = null,
    val retryAfterSeconds: Long? = null
)

data class VmStatusSample(
    val lifecycle: VmLifecycle,
    val rdpTransport: RdpTransport,
    val sampledAtFormatted: String?,
    val probeDurationMs: Long,
    val staleAfterSeconds: Long,
    val requestId: String?,
    val assignmentTag: String,
    val requestStartedElapsedMs: Long,
    val responseReceivedElapsedMs: Long
) {
    fun ageUpperBoundMs(nowElapsedMs: Long): Long {
        val rtt = (responseReceivedElapsedMs - requestStartedElapsedMs).coerceAtLeast(0L)
        val elapsedSinceReceived = (nowElapsedMs - responseReceivedElapsedMs).coerceAtLeast(0L)
        return rtt + elapsedSinceReceived
    }

    fun isFresh(nowElapsedMs: Long): Boolean {
        if (requestStartedElapsedMs <= 0 || responseReceivedElapsedMs < requestStartedElapsedMs) return false
        val ageMs = ageUpperBoundMs(nowElapsedMs)
        return ageMs < staleAfterSeconds * 1000L
    }

    fun relativeAgeText(nowElapsedMs: Long): String {
        val ageSeconds = ageUpperBoundMs(nowElapsedMs) / 1000L
        return when {
            ageSeconds < 5L -> "Updated just now"
            ageSeconds < staleAfterSeconds -> "Updated $ageSeconds seconds ago"
            else -> "Status needs refreshing"
        }
    }
}
