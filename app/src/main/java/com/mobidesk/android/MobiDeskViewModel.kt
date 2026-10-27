package com.mobidesk.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobidesk.android.data.AuthErrorMessages
import com.mobidesk.android.data.StudentWorkspaceRepository
import com.mobidesk.android.data.StudentWorkspaceResult
import com.mobidesk.android.data.SupabaseAuthRepository
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MobiDeskUiState(
    val serviceConfigured: Boolean = false,
    val isRestoringSession: Boolean = true,
    val isBusy: Boolean = false,
    val isAuthenticated: Boolean = false,
    val email: String? = null,
    val workspace: StudentWorkspaceResult? = null,
    val isLoadingWorkspace: Boolean = false,
    val isPasswordRecovery: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class MobiDeskViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MobiDeskApplication
    private val supabase = app.supabaseClient
    private val authRepository = supabase?.let(::SupabaseAuthRepository)
    private val workspaceRepository = supabase?.let(::StudentWorkspaceRepository)

    private val _uiState = MutableStateFlow(
        MobiDeskUiState(
            serviceConfigured = supabase != null,
            isRestoringSession = supabase != null
        )
    )
    val uiState = _uiState.asStateFlow()
    private var workspaceRequestedFor: String? = null

    init {
        observeSession()
    }

    private fun observeSession() {
        val auth = authRepository ?: return
        viewModelScope.launch {
            auth.sessionStatus.collect { status ->
                when (status) {
                    SessionStatus.Initializing -> {
                        _uiState.update { it.copy(isRestoringSession = true) }
                    }
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        _uiState.update {
                            it.copy(
                                isRestoringSession = false,
                                isBusy = false,
                                isAuthenticated = true,
                                email = user?.email,
                                errorMessage = null
                            )
                        }
                        val userId = user?.id
                        if (userId == null) {
                            workspaceRequestedFor = null
                            _uiState.update {
                                it.copy(
                                    isLoadingWorkspace = false,
                                    errorMessage = "We couldn't verify this account. Sign in again."
                                )
                            }
                        } else if (workspaceRequestedFor != userId) {
                            loadWorkspace(userId)
                        }
                    }
                    is SessionStatus.RefreshFailure -> {
                        workspaceRequestedFor = null
                        _uiState.update {
                            it.copy(
                                isRestoringSession = false,
                                isBusy = false,
                                isAuthenticated = false,
                                email = null,
                                workspace = null,
                                isLoadingWorkspace = false,
                                isPasswordRecovery = false,
                                errorMessage = "Your session expired. Sign in again."
                            )
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        workspaceRequestedFor = null
                        _uiState.update {
                            it.copy(
                                isRestoringSession = false,
                                isBusy = false,
                                isAuthenticated = false,
                                email = null,
                                workspace = null,
                                isLoadingWorkspace = false,
                                isPasswordRecovery = false
                            )
                        }
                    }
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        val auth = authRepository ?: return showConfigurationError()
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your school email and password.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, infoMessage = null) }
            runCatching { auth.signIn(normalizedEmail, password) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isBusy = false, errorMessage = AuthErrorMessages.safeMessage(error))
                    }
                }
        }
    }

    fun requestPasswordReset(email: String) {
        val auth = authRepository ?: return showConfigurationError()
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your school email first.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, infoMessage = null) }
            runCatching { auth.requestPasswordReset(normalizedEmail) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            infoMessage = "If this account can receive recovery email, instructions will be sent."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isBusy = false, errorMessage = AuthErrorMessages.safeMessage(error))
                    }
                }
        }
    }

    fun beginPasswordRecovery() {
        _uiState.update {
            it.copy(isPasswordRecovery = true, errorMessage = null, infoMessage = null)
        }
    }

    fun cancelPasswordRecovery() {
        _uiState.update {
            it.copy(isPasswordRecovery = false, errorMessage = null, infoMessage = null)
        }
    }

    fun updateRecoveredPassword(password: String, confirmation: String) {
        val auth = authRepository ?: return showConfigurationError()
        if (password.length < 8) {
            _uiState.update { it.copy(errorMessage = "Use a password with at least 8 characters.") }
            return
        }
        if (password != confirmation) {
            _uiState.update { it.copy(errorMessage = "The passwords don't match.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, infoMessage = null) }
            runCatching { auth.updatePassword(password) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            isPasswordRecovery = false,
                            infoMessage = "Your password has been updated."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isBusy = false, errorMessage = AuthErrorMessages.safeMessage(error))
                    }
                }
        }
    }

    fun signOut() {
        val auth = authRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, infoMessage = null) }
            runCatching { auth.signOut() }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isBusy = false, errorMessage = AuthErrorMessages.safeMessage(error))
                    }
                }
        }
    }

    fun retryWorkspaceLoad() {
        val userId = authRepository?.currentUser()?.id ?: return
        loadWorkspace(userId)
    }

    fun showAuthLinkError(error: Throwable) {
        _uiState.update { it.copy(errorMessage = AuthErrorMessages.safeMessage(error)) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    private fun loadWorkspace(userId: String) {
        val repository = workspaceRepository ?: return
        workspaceRequestedFor = userId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWorkspace = true, workspace = null) }
            runCatching { repository.loadFor(userId) }
                .onSuccess { workspace ->
                    _uiState.update {
                        it.copy(isLoadingWorkspace = false, workspace = workspace, errorMessage = null)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingWorkspace = false,
                            workspace = null,
                            errorMessage = AuthErrorMessages.safeMessage(error)
                        )
                    }
                }
        }
    }

    private fun showConfigurationError() {
        _uiState.update {
            it.copy(errorMessage = "This build is missing its Supabase client configuration.")
        }
    }
}
