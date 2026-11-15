package com.mobidesk.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mobidesk.android.data.RecoveryStage
import com.mobidesk.android.data.SharedPreferencesRecoveryStateStore
import com.mobidesk.android.rdp.RdpSessionActivity
import com.mobidesk.android.ui.MobiDeskApp
import com.mobidesk.android.ui.MobiDeskTheme
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MobiDeskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAuthIntent(intent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigateToSessionEvent.collect {
                    val sessionIntent = Intent(this@MainActivity, RdpSessionActivity::class.java)
                    startActivity(sessionIntent)
                }
            }
        }

        setContent {
            MobiDeskTheme {
                MobiDeskApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val currentIntent = intent ?: return
        val uri = currentIntent.data ?: return
        if (currentIntent.action != Intent.ACTION_VIEW) return
        if (uri.scheme != BuildConfig.AUTH_CALLBACK_SCHEME || uri.host != BuildConfig.AUTH_CALLBACK_HOST) return

        val app = application as? MobiDeskApplication
        val recoveryStore = app?.recoveryStateStore ?: SharedPreferencesRecoveryStateStore(this)

        val isRecoveryLink = uri.path == "/recovery" || uri.pathSegments == listOf("recovery")
        val isRootCallback = uri.path.isNullOrEmpty() || uri.path == "/" || uri.pathSegments.isEmpty()

        // Reject unexpected paths; only exact root callback or exact recovery callback
        if (!isRecoveryLink && !isRootCallback) {
            neutralizeIntent(currentIntent)
            return
        }

        if (isRecoveryLink) {
            // Check for error parameters returned by Supabase Auth (e.g. expired or invalid link)
            val errorParam = uri.getQueryParameter("error") ?: uri.getQueryParameter("error_code")
            if (!errorParam.isNullOrBlank()) {
                val errorDescription = uri.getQueryParameter("error_description") ?: errorParam
                recoveryStore.setStage(RecoveryStage.INTERRUPTED, null)
                recoveryStore.isExchangeInProgress = false
                viewModel.handleRecoveryCallbackError(IllegalStateException("AuthApiError: code=$errorParam, msg=$errorDescription"))
                neutralizeIntent(currentIntent)
                return
            }

            val code = uri.getQueryParameter("code")
            if (code.isNullOrBlank()) {
                // CORE-16: Malformed recovery link without code or error parameter
                neutralizeIntent(currentIntent)
                return
            }

            val fingerprint = SharedPreferencesRecoveryStateStore.sha256Fingerprint(code)

            // Re-delivery of existing valid password recovery state (recreation / onNewIntent)
            if (recoveryStore.getStage() == RecoveryStage.PASSWORD_UPDATE_REQUIRED &&
                recoveryStore.getCodeFingerprint() == fingerprint
            ) {
                viewModel.beginPasswordRecovery()
                neutralizeIntent(currentIntent)
                return
            }

            // Guard against duplicate intent delivery / replay of already consumed code
            if (recoveryStore.isFingerprintConsumed(fingerprint)) {
                neutralizeIntent(currentIntent)
                return
            }

            // Synchronously commit CALLBACK_PENDING before invoking asynchronous handleDeeplinks
            val committed = recoveryStore.setStage(RecoveryStage.CALLBACK_PENDING, fingerprint)
            if (!committed) {
                // If the durable marker write fails, fail closed
                viewModel.showAuthLinkError(IllegalStateException("Failed to securely initialize recovery state."))
                neutralizeIntent(currentIntent)
                return
            }

            recoveryStore.isExchangeInProgress = true

            app?.supabaseClient?.handleDeeplinks(
                intent = currentIntent,
                onSessionSuccess = {
                    recoveryStore.isExchangeInProgress = false
                    recoveryStore.setStage(RecoveryStage.PASSWORD_UPDATE_REQUIRED, fingerprint)
                    recoveryStore.markFingerprintConsumed(fingerprint)
                    viewModel.beginPasswordRecovery()
                    neutralizeIntent(currentIntent)
                },
                onError = { error ->
                    recoveryStore.isExchangeInProgress = false
                    recoveryStore.setStage(RecoveryStage.INTERRUPTED, fingerprint)
                    viewModel.handleRecoveryCallbackError(error)
                    neutralizeIntent(currentIntent)
                }
            )
        } else {
            app?.supabaseClient?.handleDeeplinks(
                intent = currentIntent,
                onSessionSuccess = {
                    neutralizeIntent(currentIntent)
                },
                onError = { error ->
                    viewModel.showAuthLinkError(error)
                    neutralizeIntent(currentIntent)
                }
            )
        }
    }

    private fun neutralizeIntent(intent: Intent) {
        intent.data = null
        intent.action = null
    }
}
