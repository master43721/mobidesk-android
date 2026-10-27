package com.mobidesk.android

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
}
