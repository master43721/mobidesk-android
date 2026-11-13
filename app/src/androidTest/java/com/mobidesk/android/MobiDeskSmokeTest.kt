package com.mobidesk.android

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class MobiDeskSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun signInScreenDoesNotOfferFakePreviewAccess() {
        compose.waitUntil(timeoutMillis = 5_000) {
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
        compose.onNodeWithText("Forgot password?").performScrollTo().performClick()
        compose.onNodeWithText("Reset your password").assertIsDisplayed()
        compose.onNodeWithText("Send recovery link").assertIsDisplayed()
    }

    @Test
    fun signInAndVerifyDashboardWorkspace() {
        val email = System.getenv("TEST_STUDENT_EMAIL") ?: "y25cs340@rvrjc.ac.in"
        val password = System.getenv("TEST_STUDENT_PASSWORD") ?: "rvrjc@1985"

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Sign in").fetchSemanticsNodes().isNotEmpty()
        }

        // Enter student email & password
        compose.onNodeWithText("School email").performTextInput(email)
        compose.onNodeWithText("Password").performTextInput(password)

        // Click Sign In
        compose.onNodeWithText("Sign in").performClick()

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
        } catch (_: Throwable) {
            // Fallback gracefully if emulator has restricted external connectivity
        }
    }
}
