package com.mobidesk.android

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.mobidesk.android.data.WindowsGuestCredentials
import com.mobidesk.android.ui.WindowsCredentialDialog
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MobiDeskSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun ensureSignedOutState() {
        // Reset local preferences so session restore does not bypass sign-in
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        try {
            context.getSharedPreferences("mobidesk_secure_session", Context.MODE_PRIVATE).edit().clear().commit()
            context.getSharedPreferences("mobidesk_session", Context.MODE_PRIVATE).edit().clear().commit()
            context.getSharedPreferences("mobidesk_recovery_state", Context.MODE_PRIVATE).edit().clear().commit()
        } catch (_: Throwable) {}
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        try {
            context.getSharedPreferences("mobidesk_secure_session", Context.MODE_PRIVATE).edit().clear().commit()
            context.getSharedPreferences("mobidesk_session", Context.MODE_PRIVATE).edit().clear().commit()
            context.getSharedPreferences("mobidesk_recovery_state", Context.MODE_PRIVATE).edit().clear().commit()
        } catch (_: Throwable) {}
    }

    @Test
    fun signInScreenDoesNotOfferFakePreviewAccess() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Sign in").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("MobiDesk").assertIsDisplayed()
        compose.onNodeWithText("Sign in").assertIsDisplayed()
        compose.onNodeWithText("Forgot password?").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Explore the labeled preview").assertDoesNotExist()
        compose.onNodeWithText("Demo Cloud PC").assertDoesNotExist()
        compose.onNodeWithText(
            "Accounts are provisioned by your institution; self-registration is not available.",
            substring = true
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun passwordRecoveryRequestScreenOpensWithoutSendingAnEmail() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Sign in").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Forgot password?").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Reset your password").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Reset your password").assertIsDisplayed()
        compose.onNodeWithText("Send recovery link").assertIsDisplayed()

        // Navigate back to Sign in
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Sign in").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun guestCredentialDialogInputsAndValidation() {
        var submittedCreds: WindowsGuestCredentials? = null
        var dismissed = false

        compose.setContent {
            WindowsCredentialDialog(
                onDismiss = { dismissed = true },
                onConnect = { creds -> submittedCreds = creds }
            )
        }

        // Verify title & security disclaimer
        compose.onNodeWithText("Windows Lab Sign-In").assertIsDisplayed()
        compose.onNodeWithText("Enter your Windows lab account credentials", substring = true).assertIsDisplayed()

        // Connect button should initially be disabled until username & password entered
        compose.onNodeWithTag("windows_connect_confirm_button").assertIsNotEnabled()

        // Enter username & password
        compose.onNodeWithTag("windows_username_field").performTextInput("demo_student")
        compose.onNodeWithTag("windows_password_field").performTextInput("DemoStudent@MobiDesk2026!")

        // Connect button should now be enabled
        compose.onNodeWithTag("windows_connect_confirm_button").assertIsEnabled()

        // Click Connect
        compose.onNodeWithTag("windows_connect_confirm_button").performClick()

        // Assert submitted credentials match and can be zeroized
        assertNotNull(submittedCreds)
        assertEquals("demo_student", String(submittedCreds!!.username))
        submittedCreds!!.clear()
        assertTrue(submittedCreds!!.isDestroyed)
    }

    @Test
    fun signInAndVerifyDashboardWorkspace() {
        val email = System.getenv("TEST_STUDENT_EMAIL") ?: "y25cs340@rvrjc.ac.in"
        val password = System.getenv("TEST_STUDENT_PASSWORD") ?: "rvrjc@1985"

        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Sign in").fetchSemanticsNodes().isNotEmpty()
        }

        // Enter student email & password
        compose.onNodeWithText("School email").performScrollTo().performTextInput(email)
        compose.onNodeWithText("Password").performScrollTo().performTextInput(password)

        // Click Sign In
        compose.onNodeWithText("Sign in").performScrollTo().performClick()

        // Wait for authenticated workspace to appear
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithText("win-01", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // Verify workspace display elements
        compose.onNodeWithText("win-01", substring = true).assertIsDisplayed()

        // Verify connect button is visible
        compose.onNodeWithText("Connect", substring = true).assertIsDisplayed()

        // Sign out to verify session termination
        compose.onNodeWithText("Sign out", substring = true).performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Sign in").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Sign in").assertIsDisplayed()
    }
}
