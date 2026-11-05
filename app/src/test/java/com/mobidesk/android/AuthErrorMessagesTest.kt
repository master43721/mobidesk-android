package com.mobidesk.android

import com.mobidesk.android.data.AuthErrorMessages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AuthErrorMessagesTest {
    @Test
    fun invalidCredentialsUseAStableSafeMessage() {
        assertEquals(
            "The email or password is incorrect.",
            AuthErrorMessages.safeMessage(IllegalStateException("Invalid login credentials"))
        )
    }

    @Test
    fun backendDetailsAreNotShownToTheUser() {
        val internalMessage = "request failed token=do-not-display database.internal"
        val safeMessage = AuthErrorMessages.safeMessage(IllegalStateException(internalMessage))

        assertFalse(safeMessage.contains("do-not-display"))
        assertFalse(safeMessage.contains("database.internal"))
    }

    @Test
    fun typedInvalidCredentialsErrorCodeMapsSafely() {
        assertEquals(
            "The email or password is incorrect.",
            AuthErrorMessages.safeMessage(IllegalStateException("AuthApiError: code=invalid_credentials, msg=Invalid login credentials"))
        )
    }

    @Test
    fun emailNotConfirmedMapsToExpectedCopy() {
        assertEquals(
            "Confirm your school email before signing in.",
            AuthErrorMessages.safeMessage(IllegalStateException("AuthApiError: code=email_not_confirmed, msg=Email not confirmed"))
        )
    }

    @Test
    fun emailAddressNotAuthorizedDirectsToAdmin() {
        val message = AuthErrorMessages.safeMessage(IllegalStateException("AuthApiError: code=email_address_not_authorized, msg=Email address not authorized"))
        assertEquals(
            "Recovery email delivery is not configured for this recipient. Contact your institution's MobiDesk administrator.",
            message
        )
    }

    @Test
    fun overEmailSendRateLimitMapsToPatienceCopy() {
        assertEquals(
            "Too many recovery emails have been sent. Please wait before requesting another.",
            AuthErrorMessages.safeMessage(IllegalStateException("AuthApiError: code=over_email_send_rate_limit, msg=Over email rate limit"))
        )
    }

    @Test
    fun overRequestRateLimitMapsToRequestLimitCopy() {
        assertEquals(
            "Too many sign-in requests have been made. Please wait before retrying.",
            AuthErrorMessages.safeMessage(IllegalStateException("AuthApiError: code=over_request_rate_limit, msg=Over request rate limit"))
        )
    }

    @Test
    fun expiredFlowStateAndOtpMapsToExpiredLinkCopy() {
        assertEquals(
            "This recovery link has expired or has already been used. Please request a new recovery link.",
            AuthErrorMessages.safeMessage(IllegalStateException("AuthApiError: code=flow_state_expired, msg=Flow state expired"))
        )
        assertEquals(
            "This recovery link has expired or has already been used. Please request a new recovery link.",
            AuthErrorMessages.safeMessage(IllegalStateException("AuthApiError: code=otp_expired, msg=Token has expired"))
        )
    }

    @Test
    fun networkFailureMapsToConnectionCopy() {
        assertEquals(
            "We couldn't reach the sign-in service. Check your connection and try again.",
            AuthErrorMessages.safeMessage(java.net.UnknownHostException("Unable to resolve host supabase.co"))
        )
        assertEquals(
            "We couldn't reach the sign-in service. Check your connection and try again.",
            AuthErrorMessages.safeMessage(java.net.SocketTimeoutException("Connect timed out"))
        )
    }

    @Test
    fun unexpectedErrorReturnsSafeGenericFallback() {
        val safe = AuthErrorMessages.safeMessage(RuntimeException("FATAL: relation postgres.user does not exist at 0xdeadbeef"))
        assertEquals("We couldn't complete that request. Please try again.", safe)
        assertFalse(safe.contains("postgres"))
        assertFalse(safe.contains("deadbeef"))
    }
}

