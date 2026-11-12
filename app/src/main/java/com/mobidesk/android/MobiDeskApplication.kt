package com.mobidesk.android

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.freerdp.freerdpcore.application.GlobalApp
import com.mobidesk.android.data.RecoveryStateStore
import com.mobidesk.android.data.SharedPreferencesRecoveryStateStore
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

class MobiDeskApplication : GlobalApp() {
    override fun onCreate() {
        super.onCreate()
    }

    val recoveryStateStore: RecoveryStateStore by lazy {
        SharedPreferencesRecoveryStateStore(this)
    }

    @Suppress("DEPRECATION")
    internal fun createEncryptedSessionManager(): SessionManager {
        return try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val encryptedPrefs = EncryptedSharedPreferences.create(
                this,
                "mobidesk_secure_session",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            SettingsSessionManager(SharedPreferencesSettings(encryptedPrefs))
        } catch (_: Throwable) {
            try {
                val plainPrefs = getSharedPreferences("mobidesk_session", MODE_PRIVATE)
                SettingsSessionManager(SharedPreferencesSettings(plainPrefs))
            } catch (_: Throwable) {
                MemorySessionManager()
            }
        }
    }

    val supabaseClient: SupabaseClient? by lazy {
        val url = BuildConfig.SUPABASE_URL.trim()
        val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
        if (url.isBlank() || publishableKey.isBlank()) {
            null
        } else {
            createSupabaseClient(
                supabaseUrl = url,
                supabaseKey = publishableKey
            ) {
                install(Auth) {
                    flowType = FlowType.PKCE
                    scheme = BuildConfig.AUTH_CALLBACK_SCHEME
                    host = BuildConfig.AUTH_CALLBACK_HOST
                    defaultRedirectUrl = BuildConfig.AUTH_CALLBACK_URI
                    sessionManager = createEncryptedSessionManager()
                }
                install(Postgrest)
            }
        }
    }
}
