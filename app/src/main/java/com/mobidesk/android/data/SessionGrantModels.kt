package com.mobidesk.android.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data contracts for Broker Session Grant and Certificate Pinning (FP-06, FP-10).
 * Conforms to API Contract v1.1 (§5).
 */

@Serializable
data class RdpCertificatePin(
    @SerialName("mode")
    val mode: String = "LEAF_X509_DER_SHA256",
    @SerialName("fingerprintSha256")
    val fingerprintSha256: String = ""
)

@Serializable
data class SessionGrantResponse(
    @SerialName("grant")
    val grant: String = "",
    @SerialName("expiresAt")
    val expiresAt: String = "",
    @SerialName("relayPath")
    val relayPath: String = "/v1/rdp",
    @SerialName("rdpCertificate")
    val rdpCertificate: RdpCertificatePin? = null,
    @SerialName("requestId")
    val requestId: String? = null
)

data class SessionGrant(
    val grant: String,
    val expiresAt: String,
    val relayPath: String,
    val rdpCertificate: RdpCertificatePin,
    val requestId: String?,
    val receivedAtElapsedMs: Long = 0L,
    val maxDurationSeconds: Long = 60L
) {
    /**
     * Checks if grant has expired based on monotonic elapsed clock (max 60 seconds).
     */
    fun isExpired(nowElapsedMs: Long): Boolean {
        if (receivedAtElapsedMs <= 0L) return false
        val elapsed = (nowElapsedMs - receivedAtElapsedMs).coerceAtLeast(0L)
        return elapsed > (maxDurationSeconds * 1000L)
    }
}

sealed interface SessionGrantResult {
    data class Success(val grant: SessionGrant) : SessionGrantResult
    data class RateLimited(val retryAfterSeconds: Long, val message: String) : SessionGrantResult
    data class AuthExpired(val message: String) : SessionGrantResult
    data class Error(val message: String, val isTransient: Boolean = false) : SessionGrantResult
}
