package com.mobidesk.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobidesk.android.data.AuthErrorMessages
import com.mobidesk.android.data.AuthLevel
import com.mobidesk.android.data.AuthRepository
import com.mobidesk.android.data.RecoveryStage
import com.mobidesk.android.data.RecoveryStateStore
import com.mobidesk.android.data.SharedPreferencesRecoveryStateStore
import com.mobidesk.android.data.StudentWorkspaceRepository
import com.mobidesk.android.data.StudentWorkspaceResult
import com.mobidesk.android.data.SupabaseAuthRepository
import com.mobidesk.android.data.WorkspaceRepository
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MobiDeskUiState(
    val serviceConfigured: Boolean = false,
    val isRestoringSession: Boolean = true,
    val isBusy: Boolean = false,
    val isAuthenticated: Boolean = false,
    val authLevel: AuthLevel = AuthLevel.SIGNED_OUT,
    val email: String? = null,
    val workspace: StudentWorkspaceResult? = null,
    val isLoadingWorkspace: Boolean = false,
    val isPasswordRecovery: Boolean = false,
    val isRecoveryInterrupted: Boolean = false,
    val signOutFailed: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class MobiDeskViewModel @JvmOverloads constructor(
    application: Application,
    customAuthRepository: AuthRepository? = null,
    customWorkspaceRepository: WorkspaceRepository? = null,
    customRecoveryStore: RecoveryStateStore? = null
) : AndroidViewModel(application) {
    private val app = application as? MobiDeskApplication
    private val supabase = app?.supabaseClient
    private val authRepository: AuthRepository? =
        customAuthRepository ?: supabase?.let(::SupabaseAuthRepository)
    private val workspaceRepository: WorkspaceRepository? =
        customWorkspaceRepository ?: supabase?.let(::StudentWorkspaceRepository)
    private val recoveryStore: RecoveryStateStore =
        customRecoveryStore ?: app?.recoveryStateStore ?: SharedPreferencesRecoveryStateStore(application)

    private val _uiState = MutableStateFlow(
        MobiDeskUiState(
            serviceConfigured = authRepository != null,
            isRestoringSession = authRepository != null,
            authLevel = AuthLevel.SIGNED_OUT
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
                        val stage = recoveryStore.getStage()
                        when (stage) {
                            RecoveryStage.CALLBACK_PENDING -> {
                                if (!recoveryStore.isExchangeInProgress) {
                                    // Process death occurred while CALLBACK_PENDING.
                                    // Do not replay code or treat session as valid.
                                    // Fail closed, clear partial session, show interrupted recovery screen.
                                    workspaceRequestedFor = null
                                    recoveryStore.setStage(RecoveryStage.INTERRUPTED)
                                    viewModelScope.launch {
                                        runCatching { auth.clearLocalSession() }
                                    }
                                    _uiState.update {
                                        it.copy(
                                            isRestoringSession = false,
                                            isBusy = false,
                                            isAuthenticated = false,
                                            authLevel = AuthLevel.RECOVERY_ONLY,
                                            isPasswordRecovery = false,
                                            isRecoveryInterrupted = true,
                                            email = null,
                                            workspace = null,
                                            isLoadingWorkspace = false,
                                            errorMessage = "This recovery link was interrupted or has expired. Please request a new recovery link."
                                        )
                                    }
                                } else {
                                    // Active PKCE exchange in flight in this process.
                                    // HARD INVARIANT: Zero workspace calls during RECOVERY_ONLY.
                                    workspaceRequestedFor = null
                                    _uiState.update {
                                        it.copy(
                                            isRestoringSession = false,
                                            isBusy = false,
                                            isAuthenticated = true,
                                            authLevel = AuthLevel.RECOVERY_ONLY,
                                            isPasswordRecovery = false,
                                            isRecoveryInterrupted = false,
                                            email = status.session.user?.email,
                                            workspace = null,
                                            isLoadingWorkspace = false,
                                            errorMessage = null
                                        )
                                    }
                                }
                            }
                            RecoveryStage.PASSWORD_UPDATE_REQUIRED -> {
                                // HARD INVARIANT: Zero workspace calls during RECOVERY_ONLY.
                                // Restore new-password form if SDK session is valid.
                                workspaceRequestedFor = null
                                _uiState.update {
                                    it.copy(
                                        isRestoringSession = false,
                                        isBusy = false,
                                        isAuthenticated = true,
                                        authLevel = AuthLevel.RECOVERY_ONLY,
                                        isPasswordRecovery = true,
                                        isRecoveryInterrupted = false,
                                        email = status.session.user?.email,
                                        workspace = null,
                                        isLoadingWorkspace = false,
                                        errorMessage = null
                                    )
                                }
                            }
                            RecoveryStage.INTERRUPTED -> {
                                // Process death or failure during callback: fail-closed, clear partial session
                                workspaceRequestedFor = null
                                viewModelScope.launch {
                                    runCatching { auth.clearLocalSession() }
                                }
                                _uiState.update {
                                    it.copy(
                                        isRestoringSession = false,
                                        isBusy = false,
                                        isAuthenticated = false,
                                        authLevel = AuthLevel.RECOVERY_ONLY,
                                        isPasswordRecovery = false,
                                        isRecoveryInterrupted = true,
                                        email = null,
                                        workspace = null,
                                        isLoadingWorkspace = false,
                                        errorMessage = "This recovery link was interrupted or has expired. Please request a new recovery link."
                                    )
                                }
                            }
                            RecoveryStage.NONE -> {
                                val user = status.session.user
                                _uiState.update {
                                    it.copy(
                                        isRestoringSession = false,
                                        isBusy = false,
                                        isAuthenticated = true,
                                        authLevel = AuthLevel.FULLY_AUTHENTICATED,
                                        isPasswordRecovery = false,
                                        isRecoveryInterrupted = false,
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
                        }
                    }
                    is SessionStatus.RefreshFailure -> {
                        recoveryStore.clear()
                        workspaceRequestedFor = null
                        _uiState.update {
                            it.copy(
                                isRestoringSession = false,
                                isBusy = false,
                                isAuthenticated = false,
                                authLevel = AuthLevel.SIGNED_OUT,
                                email = null,
                                workspace = null,
                                isLoadingWorkspace = false,
                                isPasswordRecovery = false,
                                isRecoveryInterrupted = false,
                                signOutFailed = false,
                                errorMessage = "Your session expired. Sign in again."
                            )
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        // If recovery was required but session is gone, clear stale recovery
                        if (recoveryStore.getStage() == RecoveryStage.PASSWORD_UPDATE_REQUIRED) {
                            recoveryStore.clear()
                        }
                        workspaceRequestedFor = null
                        _uiState.update {
                            it.copy(
                                isRestoringSession = false,
                                isBusy = false,
                                isAuthenticated = false,
                                authLevel = AuthLevel.SIGNED_OUT,
                                email = null,
                                workspace = null,
                                isLoadingWorkspace = false,
                                isPasswordRecovery = false,
                                signOutFailed = false
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
        if (_uiState.value.isBusy) return
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
        if (_uiState.value.isBusy) return
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
            it.copy(
                isPasswordRecovery = true,
                isRecoveryInterrupted = false,
                authLevel = AuthLevel.RECOVERY_ONLY,
                workspace = null,
                isLoadingWorkspace = false,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun cancelPasswordRecovery() {
        val auth = authRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, infoMessage = null) }
            val remoteResult = runCatching { auth.signOutCurrentSession() }
            val remoteFailed = remoteResult.isFailure
            if (remoteFailed) {
                runCatching { auth.clearLocalSession() }
            }
            recoveryStore.clear()
            workspaceRequestedFor = null
            _uiState.update {
                it.copy(
                    isBusy = false,
                    isAuthenticated = false,
                    authLevel = AuthLevel.SIGNED_OUT,
                    isPasswordRecovery = false,
                    isRecoveryInterrupted = false,
                    workspace = null,
                    isLoadingWorkspace = false,
                    errorMessage = null,
                    infoMessage = if (remoteFailed) {
                        "Signed out on this device. Remote session revocation could not be confirmed."
                    } else {
                        null
                    }
                )
            }
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
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, infoMessage = null) }
            runCatching { auth.updatePassword(password) }
                .onSuccess {
                    recoveryStore.clear()
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            authLevel = AuthLevel.FULLY_AUTHENTICATED,
                            isPasswordRecovery = false,
                            isRecoveryInterrupted = false,
                            infoMessage = "Your password has been updated."
                        )
                    }
                    val userId = auth.currentUser()?.id
                    if (userId != null) {
                        loadWorkspace(userId)
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
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, infoMessage = null) }
            runCatching { auth.signOutCurrentSession() }
                .onSuccess {
                    recoveryStore.clear()
                    workspaceRequestedFor = null
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            isAuthenticated = false,
                            authLevel = AuthLevel.SIGNED_OUT,
                            email = null,
                            workspace = null,
                            isLoadingWorkspace = false,
                            isPasswordRecovery = false,
                            signOutFailed = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            signOutFailed = true,
                            errorMessage = "Could not confirm sign-out with the service. Check your connection or sign out on this device."
                        )
                    }
                }
        }
    }

    fun signOutDeviceOnly() {
        val auth = authRepository ?: return
        viewModelScope.launch {
            runCatching { auth.clearLocalSession() }
            recoveryStore.clear()
            workspaceRequestedFor = null
            _uiState.update {
                it.copy(
                    isBusy = false,
                    isAuthenticated = false,
                    authLevel = AuthLevel.SIGNED_OUT,
                    email = null,
                    workspace = null,
                    isLoadingWorkspace = false,
                    isPasswordRecovery = false,
                    signOutFailed = false,
                    errorMessage = null,
                    infoMessage = "Signed out on this device. Remote session revocation could not be confirmed."
                )
            }
        }
    }

    fun retryWorkspaceLoad() {
        val userId = authRepository?.currentUser()?.id ?: return
        if (_uiState.value.authLevel == AuthLevel.FULLY_AUTHENTICATED) {
            loadWorkspace(userId)
        }
    }

    fun showAuthLinkError(error: Throwable) {
        _uiState.update { it.copy(errorMessage = AuthErrorMessages.safeMessage(error)) }
    }

    fun handleRecoveryCallbackError(error: Throwable) {
        workspaceRequestedFor = null
        viewModelScope.launch {
            runCatching { authRepository?.clearLocalSession() }
        }
        _uiState.update {
            it.copy(
                isBusy = false,
                isAuthenticated = false,
                authLevel = AuthLevel.RECOVERY_ONLY,
                isPasswordRecovery = false,
                isRecoveryInterrupted = true,
                workspace = null,
                isLoadingWorkspace = false,
                errorMessage = AuthErrorMessages.safeMessage(error)
            )
        }
    }

    fun retryRecoveryAfterInterruption() {
        recoveryStore.clear()
        _uiState.update {
            it.copy(
                isRecoveryInterrupted = false,
                isPasswordRecovery = false,
                authLevel = AuthLevel.SIGNED_OUT,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    private fun loadWorkspace(userId: String) {
        // HARD INVARIANT: only load workspace when fully authenticated
        if (_uiState.value.authLevel != AuthLevel.FULLY_AUTHENTICATED) {
            return
        }
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
