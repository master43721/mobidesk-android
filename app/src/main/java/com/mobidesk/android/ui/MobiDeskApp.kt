package com.mobidesk.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SignalWifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private enum class AppScreen { SIGN_IN, DASHBOARD, SETTINGS }

@Composable
fun MobiDeskApp() {
    var screen by rememberSaveable { mutableStateOf(AppScreen.SIGN_IN.name) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentScreen = AppScreen.valueOf(screen)

    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    BackHandler(enabled = currentScreen != AppScreen.SIGN_IN) {
        screen = if (currentScreen == AppScreen.SETTINGS) AppScreen.DASHBOARD.name else AppScreen.SIGN_IN.name
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.navigationBars),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbar) }
    ) { insets ->
        when (currentScreen) {
            AppScreen.SIGN_IN -> SignInScreen(
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                passwordVisible = passwordVisible,
                onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
                onSignIn = { notify("Sign-in is unavailable until MobiDesk authentication is configured.") },
                onPreview = { screen = AppScreen.DASHBOARD.name },
                modifier = Modifier.padding(insets)
            )
            AppScreen.DASHBOARD -> DashboardScreen(
                onSettings = { screen = AppScreen.SETTINGS.name },
                onSignOut = {
                    email = ""
                    password = ""
                    screen = AppScreen.SIGN_IN.name
                },
                onConnect = { notify("No live session is available in preview. A server-issued connection ticket is required.") },
                onStart = { notify("VM control is unavailable until the authorized control API is configured.") },
                modifier = Modifier.padding(insets)
            )
            AppScreen.SETTINGS -> SettingsScreen(
                onBack = { screen = AppScreen.DASHBOARD.name },
                modifier = Modifier.padding(insets)
            )
        }
    }
}

@Composable
private fun SignInScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: () -> Unit,
    onSignIn: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        BrandLockup()
        Spacer(Modifier.height(52.dp))
        Text("Your Windows workspace,\nwherever you study.", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "Sign in with your school account to reach your assigned Cloud PC.",
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = onPasswordVisibilityChange) {
                    Text(if (passwordVisible) "Hide" else "Show")
                }
            },
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Sign in", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Your school controls access to your assigned PC.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(30.dp))
        ServiceNotice(
            icon = Icons.Outlined.CloudOff,
            title = "Service setup is pending",
            body = "Authentication and live Cloud PC data will appear here after the school service is configured."
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onPreview, modifier = Modifier.fillMaxWidth()) {
            Text("Explore the labeled preview", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Preview data is for interface review only; it is not connected to a real account or VM.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun DashboardScreen(
    onSettings: () -> Unit,
    onSignOut: () -> Unit,
    onConnect: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("MobiDesk", style = MaterialTheme.typography.titleLarge)
                Text("Student workspace", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        }

        PreviewBanner()

        Column {
            Text("Your Cloud PC", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text("An illustrative preview, not a live assignment", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Demo Cloud PC", style = MaterialTheme.typography.titleLarge)
                        Text("Sample assignment", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SampleTag()
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(color = Color(0xFFE4AA4F))
                    Spacer(Modifier.width(9.dp))
                    Column {
                        Text("Status not connected", style = MaterialTheme.typography.titleMedium)
                        Text("Live status requires the MobiDesk control service", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Connect to Cloud PC")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null)
                }
                TextButton(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Text("Request startup")
                }
            }
        }

        ServiceNotice(
            icon = Icons.Outlined.SignalWifiOff,
            title = "No school service connected",
            body = "This preview cannot start a VM or open a remote desktop. A trusted assignment and a short-lived connection ticket are required."
        )

        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text("Return to sign in")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(4.dp))
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }
        ServiceNotice(
            icon = Icons.Outlined.Shield,
            title = "Account not connected",
            body = "Profile, security, and session settings will be provided by the verified school service."
        )
        SettingRow(label = "Identity provider", value = "Not configured")
        SettingRow(label = "VM control API", value = "Not configured")
        SettingRow(label = "Remote desktop transport", value = "Not verified")
        SettingRow(label = "Session privacy", value = "Clipboard is not active in preview")
        Text(
            "MobiDesk does not store or request permanent Windows credentials in preview mode.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
private fun PreviewBanner() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            SampleTag()
            Spacer(Modifier.width(10.dp))
            Text("Preview mode — no live service", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun SampleTag() {
    Surface(color = Color(0xFF4B3820), shape = RoundedCornerShape(7.dp)) {
        Text(
            "SAMPLE",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFFFD18A)
        )
    }
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
private fun SettingRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}
