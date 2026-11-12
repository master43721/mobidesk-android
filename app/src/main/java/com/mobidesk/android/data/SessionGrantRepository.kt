package com.mobidesk.android.data

/**
 * Repository interface for requesting one-time session grants from the session broker (FP-06).
 */
interface SessionGrantRepository {
    /**
     * Request a one-time single-use session grant.
     *
     * @param accessToken Current authenticated student Supabase access JWT.
     * @return [SessionGrantResult] containing the issued grant or safe error.
     */
    suspend fun requestSessionGrant(accessToken: String): SessionGrantResult
}
