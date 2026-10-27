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
}
