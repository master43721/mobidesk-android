package com.mobidesk.android.data

sealed interface StatusResult {
    data class Success(val sample: VmStatusSample) : StatusResult
    data class RateLimited(
        val retryAfterSeconds: Long,
        val message: String
    ) : StatusResult
    data class AuthExpired(val message: String) : StatusResult
    data class Error(val message: String, val isTransient: Boolean = true) : StatusResult
}

interface StatusRepository {
    suspend fun getVmStatus(accessToken: String): StatusResult
}
