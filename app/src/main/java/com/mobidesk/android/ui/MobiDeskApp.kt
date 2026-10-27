package com.mobidesk.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobidesk.android.MobiDeskUiState
import com.mobidesk.android.MobiDeskViewModel
import com.mobidesk.android.data.StudentWorkspaceResult

private enum class SignedOutRoute { SIGN_IN, RESET_REQUEST }

@Composable
fun MobiDeskApp(viewModel: MobiDeskViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var route by rememberSaveable { mutableStateOf(SignedOutRoute.SIGN_IN.name) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) password = ""
    }

    BackHandler(enabled = state.isPasswordRecovery || route != SignedOutRoute.SIGN_IN.name || showSettings) {
        when {
            state.isPasswordRecovery -> viewModel.cancelPasswordRecovery()
            showSettings -> showSettings = false
            else -> route = SignedOutRoute.SIGN_IN.name
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            state.isRestoringSession -> LoadingScreen("Restoring your secure session")
            state.isPasswordRecovery -> PasswordRecoveryScreen(
                state = state,
                onSave = viewModel::updateRecoveredPassword,
                onBack = viewModel::cancelPasswordRecovery
            )
            !state.isAuthenticated && route == SignedOutRoute.RESET_REQUEST.name -> PasswordResetRequestScreen(
                state = state,
                email = email,
                onEmailChange = { email = it },
                onSend = { viewModel.requestPasswordReset(email) },
                onBack = { route = SignedOutRoute.SIGN_IN.name },
                onClearMessages = viewModel::clearMessages
            )
            !state.isAuthenticated -> SignInScreen(
                state = state,
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                onSignIn = { viewModel.signIn(email, password) },
                onForgotPassword = {
                    viewModel.clearMessages()
                    route = SignedOutRoute.RESET_REQUEST.name
                }
            )
            showSettings -> SettingsScreen(
                state = state,
                onBack = { showSettings = false },
                onSignOut = {
                    showSettings = false
                    password = ""
                    viewModel.signOut()
                }
            )
            state.isLoadingWorkspace -> LoadingScreen("Loading your assigned workspace")
            state.workspace == StudentWorkspaceResult.AccountNotProvisioned -> AccountNotProvisionedScreen(
                state = state,
                onSignOut = viewModel::signOut
            )
            state.workspace is StudentWorkspaceResult.Ready -> DashboardScreen(
                state = state,
                workspace = state.workspace as StudentWorkspaceResult.Ready,
                onSettings = { showSettings = true },
                onRetry = viewModel::retryWorkspaceLoad,
                onSignOut = viewModel::signOut
            )
            else -> WorkspaceErrorScreen(
                state = state,
                onRetry = viewModel::retryWorkspaceLoad,
                onSignOut = viewModel::signOut
            )
        }
    }
}

@Composable
private fun SignInScreen(
    state: MobiDeskUiState,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onForgotPassword: () -> Unit
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
            BrandLockup()
            Spacer(Modifier.height(52.dp))
            Text("Your Windows workspace,\nwherever you study.", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                "Sign in with the school account assigned to your Cloud PC.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("School email") },
                singleLine = true,
                enabled = !state.isBusy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                enabled = !state.isBusy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp)
            )
            if (state.errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                InlineNotice(state.errorMessage, isError = true)
            }
            if (!state.serviceConfigured) {
                Spacer(Modifier.height(12.dp))
                InlineNotice("This build is missing its Supabase client configuration. Sign-in is disabled.", isError = false)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = state.serviceConfigured && !state.isBusy && email.isNotBlank() && password.isNotBlank(),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else {
                    Text("Sign in", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                }
            }
            TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
                Text("Forgot password?")
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Accounts are provisioned by your institution; self-registration is not available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(18.dp))
            ServiceNotice(
                icon = Icons.Outlined.Shield,
                title = "Protected by your school account",
                body = "MobiDesk checks the signed-in account against its student profile and assignment policies."
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PasswordResetRequestScreen(
    state: MobiDeskUiState,
    email: String,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    onClearMessages: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
            BrandLockup()
            Spacer(Modifier.height(40.dp))
            Text("Reset your password", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            Text(
                "We'll send a recovery link if this school account can receive email.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { onClearMessages(); onEmailChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("School email") },
                singleLine = true,
                enabled = !state.isBusy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(14.dp)
            )
            state.errorMessage?.let { Spacer(Modifier.height(12.dp)); InlineNotice(it, isError = true) }
            state.infoMessage?.let { Spacer(Modifier.height(12.dp)); InlineNotice(it, isError = false) }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onSend,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = state.serviceConfigured && !state.isBusy && email.isNotBlank(),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Send recovery link")
            }
            Spacer(Modifier.height(16.dp))
            if (!state.serviceConfigured) InlineNotice("Supabase is not configured in this build.", isError = false)
        }
    }
}

@Composable
private fun PasswordRecoveryScreen(
    state: MobiDeskUiState,
    onSave: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
            BrandLockup()
            Spacer(Modifier.height(40.dp))
            Text("Choose a new password", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            Text(
                if (state.isAuthenticated) "Enter a new password for your school account."
                else "Verifying your secure recovery link…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = "New password",
                visible = passwordVisible,
                onToggleVisibility = { passwordVisible = !passwordVisible },
                enabled = state.isAuthenticated && !state.isBusy
            )
            Spacer(Modifier.height(14.dp))
            PasswordField(
                value = confirmation,
                onValueChange = { confirmation = it },
                label = "Confirm new password",
                visible = passwordVisible,
                onToggleVisibility = { passwordVisible = !passwordVisible },
                enabled = state.isAuthenticated && !state.isBusy
            )
            state.errorMessage?.let { Spacer(Modifier.height(12.dp)); InlineNotice(it, isError = true) }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { onSave(password, confirmation) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = state.isAuthenticated && !state.isBusy && password.isNotBlank() && confirmation.isNotBlank(),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Update password")
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    state: MobiDeskUiState,
    workspace: StudentWorkspaceResult.Ready,
    onSettings: () -> Unit,
    onRetry: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("MobiDesk", style = MaterialTheme.typography.titleLarge)
                Text("Student workspace", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
        }
        state.infoMessage?.let { InlineNotice(it, isError = false) }
        if (state.errorMessage != null) InlineNotice(state.errorMessage, isError = true)

        Column {
            Text("Welcome, ${workspace.profile.name}", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(5.dp))
            Text(workspace.profile.college, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(46.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Your Cloud PC", style = MaterialTheme.typography.titleLarge)
                        Text("Assignment from your school", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                if (workspace.assignedVms.isEmpty()) {
                    Text("No Cloud PC is assigned to this account yet.", style = MaterialTheme.typography.bodyLarge)
                    Text("Contact your MobiDesk administrator if you expected an assignment.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    workspace.assignedVms.forEach { vm ->
                        Text(vm.name, style = MaterialTheme.typography.titleMedium)
                        Text("Assignment verified", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Remote session not configured") }
                Text(
                    "A secure VM-control service and short-lived session ticket are required before remote access can be enabled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ServiceNotice(
            icon = Icons.Outlined.CloudOff,
            title = "Remote desktop is not ready",
            body = "Your account and assignment are read from Supabase. This build does not start VMs or open RDP sessions."
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Refresh assignment")
        }
        TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
    }
}

@Composable
private fun AccountNotProvisionedScreen(state: MobiDeskUiState, onSignOut: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
            BrandLockup()
            Spacer(Modifier.height(48.dp))
            Text("Account setup needed", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                "This signed-in account does not have an active student profile. Ask your institution to verify its MobiDesk roster before trying again.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            state.email?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                Text("Sign out")
            }
        }
    }
}

@Composable
private fun WorkspaceErrorScreen(state: MobiDeskUiState, onRetry: () -> Unit, onSignOut: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
            BrandLockup()
            Spacer(Modifier.height(42.dp))
            Text("Workspace unavailable", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                state.errorMessage ?: "We couldn't load your student workspace. Check your connection and retry.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(22.dp))
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                Text("Try again")
            }
            TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
        }
    }
}

@Composable
private fun SettingsScreen(state: MobiDeskUiState, onBack: () -> Unit, onSignOut: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
            Text("Account settings", style = MaterialTheme.typography.headlineSmall)
        }
        ServiceNotice(
            icon = Icons.Outlined.Shield,
            title = "School account",
            body = "Your signed-in identity is verified by Supabase Auth and scoped by database row-level security."
        )
        SettingRow("Signed-in account", state.email ?: "Verified school account")
        SettingRow("Profile and VM assignment", "Loaded from the signed-in account's access policies")
        SettingRow("Remote desktop", "Not configured in this build")
        SettingRow("VM start/stop controls", "Not configured in this build")
        Text(
            "MobiDesk does not store permanent Windows credentials. Remote access stays disabled until the secure session service is deployed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
        ) { Text("Sign out") }
    }
}

@Composable
private fun LoadingScreen(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator()
            Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BrandLockup() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrandMark(size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text("MobiDesk", style = MaterialTheme.typography.titleLarge)
            Text("CLOUD WORKSPACE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    enabled: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility, enabled = enabled) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (visible) "Hide password" else "Show password")
            }
        },
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun ServiceNotice(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InlineNotice(message: String, isError: Boolean) {
    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else color
        )
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.padding(top = 7.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }
}
