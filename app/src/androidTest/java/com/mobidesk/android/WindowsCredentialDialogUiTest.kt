package com.mobidesk.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.mobidesk.android.data.WindowsGuestCredentials
import com.mobidesk.android.ui.WindowsCredentialDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WindowsCredentialDialogUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

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
}
