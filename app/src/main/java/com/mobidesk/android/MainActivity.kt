package com.mobidesk.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.mobidesk.android.ui.MobiDeskApp
import com.mobidesk.android.ui.MobiDeskTheme
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    private val viewModel: MobiDeskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAuthIntent(intent)
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
        val isRecoveryLink = uri.pathSegments.contains("recovery")
        (application as? MobiDeskApplication)?.supabaseClient?.handleDeeplinks(
            intent = currentIntent,
            onSessionSuccess = {
                if (isRecoveryLink) viewModel.beginPasswordRecovery()
            },
            onError = viewModel::showAuthLinkError
        )
    }
}
