package com.mobidesk.android

import android.app.Application
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

class MobiDeskApplication : Application() {
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
                }
                install(Postgrest)
            }
        }
    }
}
