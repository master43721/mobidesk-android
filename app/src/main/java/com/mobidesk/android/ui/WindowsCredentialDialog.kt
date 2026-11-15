package com.mobidesk.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mobidesk.android.data.WindowsGuestCredentials

/**
 * Windows Guest Credential Modal Dialog for Network Level Authentication (NLA) - FP-05.
 *
 * Security Invariants:
 * 1. Explicitly informs the student that Windows credentials are lab-specific and separate
 *    from their Supabase/MobiDesk identity.
 * 2. Inputs are converted to [WindowsGuestCredentials] (mutable char arrays) and local Compose
 *    String states are cleared immediately on submit or dismiss.
 * 3. Never logged, never placed into Intent extras or saved to local storage.
 */
@Composable
fun WindowsCredentialDialog(
    onDismiss: () -> Unit,
    onConnect: (WindowsGuestCredentials) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    fun clearLocalStates() {
        username = ""
        domain = ""
        password = ""
    }

    AlertDialog(
        onDismissRequest = {
            clearLocalStates()
            onDismiss()
        },
        title = {
            Text(
                text = "Windows Lab Sign-In",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter your Windows lab account credentials. These are separate from your MobiDesk sign-in credentials.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Windows Username") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("windows_username_field"),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Domain (Optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("windows_domain_field"),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Windows Password") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("windows_password_field"),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        val desc = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = desc)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (username.isNotBlank() && password.isNotBlank()) {
                                val creds = WindowsGuestCredentials(
                                    username = username.toCharArray(),
                                    domain = domain.takeIf { it.isNotBlank() }?.toCharArray(),
                                    password = password.toCharArray()
                                )
                                clearLocalStates()
                                onConnect(creds)
                            }
                        }
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val creds = WindowsGuestCredentials(
                        username = username.toCharArray(),
                        domain = domain.takeIf { it.isNotBlank() }?.toCharArray(),
                        password = password.toCharArray()
                    )
                    clearLocalStates()
                    onConnect(creds)
                },
                enabled = username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.testTag("windows_connect_confirm_button")
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    clearLocalStates()
                    onDismiss()
                },
                modifier = Modifier.testTag("windows_connect_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
