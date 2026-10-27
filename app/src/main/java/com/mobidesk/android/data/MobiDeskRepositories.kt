package com.mobidesk.android.data

import com.mobidesk.android.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseAuthRepository(private val client: SupabaseClient) {
    val sessionStatus get() = client.auth.sessionStatus

    fun currentUser() = client.auth.currentUserOrNull()

    suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun requestPasswordReset(email: String) {
        client.auth.resetPasswordForEmail(
            email = email,
            redirectUrl = BuildConfig.AUTH_RECOVERY_URI
        )
    }

    suspend fun updatePassword(password: String) {
        client.auth.updateUser {
            this.password = password
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }
}

class StudentWorkspaceRepository(private val client: SupabaseClient) {
    suspend fun loadFor(userId: String): StudentWorkspaceResult {
        val profiles = client.from("students").select(
            columns = Columns.list("id", "name", "college")
        ) {
            filter {
                eq("id", userId)
            }
        }.decodeList<StudentProfile>()

        val profile = profiles.firstOrNull()
            ?: return StudentWorkspaceResult.AccountNotProvisioned

        // The vms RLS policy returns only the signed-in user's assigned VMs.
        // Do not request guac_connection_id or hostname from the client.
        val assignedVms = client.from("vms").select(
            columns = Columns.list("id", "name")
        ).decodeList<VmSummary>()

        return StudentWorkspaceResult.Ready(
            profile = profile,
            assignedVms = assignedVms
        )
    }
}

@Serializable
data class StudentProfile(
    val id: String,
    val name: String,
    val college: String
)

@Serializable
data class VmSummary(
    val id: String,
    val name: String
)

sealed interface StudentWorkspaceResult {
    data object AccountNotProvisioned : StudentWorkspaceResult

    data class Ready(
        val profile: StudentProfile,
        val assignedVms: List<VmSummary>
    ) : StudentWorkspaceResult
}

object AuthErrorMessages {
    fun safeMessage(error: Throwable): String {
        val message = error.message.orEmpty().lowercase()
        return when {
            "invalid login credentials" in message || "invalid password" in message ->
                "The email or password is incorrect."
            "email not confirmed" in message || "email_not_confirmed" in message ->
                "Confirm your school email before signing in."
            "network" in message || "timeout" in message || "connect" in message ->
                "We couldn't reach the sign-in service. Check your connection and try again."
            else ->
                "We couldn't complete that request. Please try again."
        }
    }
}
