package com.mobidesk.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class MobiDeskSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun signInExplainsBackendAndPreviewIsClearlyLabeled() {
        compose.onNodeWithText("MobiDesk").assertIsDisplayed()
        compose.onNodeWithText("Sign in").assertIsDisplayed()
        compose.onNodeWithText("Explore the labeled preview").performClick()
        compose.onNodeWithText("Preview mode — no live service").assertIsDisplayed()
        compose.onNodeWithText("Demo Cloud PC").assertIsDisplayed()
        compose.onNodeWithText("Status not connected").assertIsDisplayed()
    }
}
