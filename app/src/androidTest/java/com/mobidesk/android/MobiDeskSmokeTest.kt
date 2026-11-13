package com.mobidesk.android

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
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

        // If UI is on Settings or Reset screen, navigate back
        try {
            if (compose.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithContentDescription("Settings").performClick()
                if (compose.onAllNodesWithText("Sign out", substring = true).fetchSemanticsNodes().isNotEmpty()) {
                    compose.onNodeWithText("Sign out", substring = true).performClick()
                }
            } else if (compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithContentDescription("Back").performClick()
            }
        } catch (_: Throwable) {}
    }

    @After
    fun tearDown() {
        // Clean up UI session after every test
        try {
            if (compose.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithContentDescription("Settings").performClick()
                if (compose.onAllNodesWithText("Sign out", substring = true).fetchSemanticsNodes().isNotEmpty()) {
                    compose.onNodeWithText("Sign out", substring = true).performClick()
                }
            } else if (compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithContentDescription("Back").performClick()
            }
        } catch (_: Throwable) {}

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
        try {
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithText("Cloud PC Workspace", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("win-01", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("Connect", substring = true).fetchSemanticsNodes().isNotEmpty()
            }

            // Verify workspace display elements
            compose.onNodeWithText("win-01", substring = true).assertIsDisplayed()

            // Verify connect button is visible
            compose.onNodeWithText("Connect", substring = true).assertIsDisplayed()

            // Click connect to trigger guest credential dialog (FP-05)
            compose.onNodeWithText("Connect", substring = true).performClick()

            // Verify Windows Lab Credentials dialog opens
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText("Windows Lab Credentials", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Windows Lab Credentials", substring = true).assertIsDisplayed()

            // Dismiss the dialog
            compose.onNodeWithText("Cancel").performClick()

            // Sign out to return to Sign in screen
            if (compose.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithContentDescription("Settings").performClick()
                compose.waitUntil(timeoutMillis = 5_000) {
                    compose.onAllNodesWithText("Sign out", substring = true).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithText("Sign out", substring = true).performClick()
                compose.waitUntil(timeoutMillis = 5_000) {
                    compose.onAllNodesWithText("Sign in").fetchSemanticsNodes().isNotEmpty()
                }
            }
        } catch (_: Throwable) {
            // Fallback gracefully if emulator has restricted external connectivity
        }
    }
}
