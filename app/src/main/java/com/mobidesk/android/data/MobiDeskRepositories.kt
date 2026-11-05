package com.mobidesk.android.data

import com.mobidesk.android.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface AuthRepository {
    val sessionStatus: Flow<SessionStatus>
    fun currentUser(): UserInfo?
    suspend fun signIn(email: String, password: String)
    suspend fun signOutCurrentSession()
    suspend fun clearLocalSession()
    suspend fun requestPasswordReset(email: String)
    suspend fun updatePassword(password: String)
    suspend fun signOut()
}

interface WorkspaceRepository {
    suspend fun loadFor(userId: String): StudentWorkspaceResult
}

class SupabaseAuthRepository(private val client: SupabaseClient) : AuthRepository {
    override val sessionStatus get() = client.auth.sessionStatus

    override fun currentUser(): UserInfo? = client.auth.currentUserOrNull()

    override suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun requestPasswordReset(email: String) {
        client.auth.resetPasswordForEmail(
            email = email,
            redirectUrl = BuildConfig.AUTH_RECOVERY_URI
        )
    }

    override suspend fun updatePassword(password: String) {
        client.auth.updateUser {
            this.password = password
        }
    }

    override suspend fun signOutCurrentSession() {
        client.auth.signOut(scope = SignOutScope.LOCAL)
    }

    override suspend fun clearLocalSession() {
        client.auth.clearSession()
    }

    override suspend fun signOut() {
        signOutCurrentSession()
    }
}

class StudentWorkspaceRepository(private val client: SupabaseClient) : WorkspaceRepository {
    override suspend fun loadFor(userId: String): StudentWorkspaceResult {
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
        if (error is AuthRestException) {
            when (error.errorCode) {
                AuthErrorCode.InvalidCredentials ->
                    return "The email or password is incorrect."
                AuthErrorCode.EmailNotConfirmed ->
                    return "Confirm your school email before signing in."
                AuthErrorCode.EmailAddressNotAuthorized ->
                    return "Recovery email delivery is not configured for this recipient. Contact your institution's MobiDesk administrator."
                AuthErrorCode.OverEmailSendRateLimit ->
                    return "Too many recovery emails have been sent. Please wait before requesting another."
                AuthErrorCode.OverRequestRateLimit ->
                    return "Too many sign-in requests have been made. Please wait before retrying."
                AuthErrorCode.FlowStateExpired,
                AuthErrorCode.FlowStateNotFound,
                AuthErrorCode.OtpExpired,
                AuthErrorCode.BadCodeVerifier ->
                    return "This recovery link has expired or has already been used. Please request a new recovery link."
                else -> { /* fall through to message checks */ }
            }
        }

        val message = error.message.orEmpty().lowercase()
        val errorType = error.javaClass.name.lowercase()
        return when {
            "invalid_credentials" in message || "invalid login credentials" in message || "invalid password" in message ->
                "The email or password is incorrect."
            "email_not_confirmed" in message || "email not confirmed" in message ->
                "Confirm your school email before signing in."
            "email_address_not_authorized" in message || "email address not authorized" in message ->
                "Recovery email delivery is not configured for this recipient. Contact your institution's MobiDesk administrator."
            "over_email_send_rate_limit" in message || "email rate limit" in message ->
                "Too many recovery emails have been sent. Please wait before requesting another."
            "over_request_rate_limit" in message || "request rate limit" in message ->
                "Too many sign-in requests have been made. Please wait before retrying."
            "flow_state_expired" in message || "flow_state_not_found" in message || "otp_expired" in message || "bad_code_verifier" in message ->
                "This recovery link has expired or has already been used. Please request a new recovery link."
            error is java.io.IOException || "network" in message || "timeout" in message || "connect" in message || "host" in message || "unreachable" in message || "socket" in message || "network" in errorType || "timeout" in errorType || "unknownhost" in errorType || "socket" in errorType ->
                "We couldn't reach the sign-in service. Check your connection and try again."
            else ->
                "We couldn't complete that request. Please try again."
        }
    }
}
