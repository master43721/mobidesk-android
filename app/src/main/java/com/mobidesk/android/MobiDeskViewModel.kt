package com.mobidesk.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobidesk.android.data.AuthErrorMessages
import com.mobidesk.android.data.AuthLevel
import com.mobidesk.android.data.AuthRepository
import com.mobidesk.android.data.ConnectPolicy
import com.mobidesk.android.data.DefaultStatusRepository
import com.mobidesk.android.data.MonotonicClock
import com.mobidesk.android.data.RdpTransport
import com.mobidesk.android.data.RecoveryStage
import com.mobidesk.android.data.RecoveryStateStore
import com.mobidesk.android.data.SharedPreferencesRecoveryStateStore
import com.mobidesk.android.data.StatusRepository
import com.mobidesk.android.data.StatusResult
import com.mobidesk.android.data.StudentWorkspaceRepository
import com.mobidesk.android.data.StudentWorkspaceResult
import com.mobidesk.android.data.SupabaseAuthRepository
import com.mobidesk.android.data.SystemMonotonicClock
import com.mobidesk.android.data.VmLifecycle
import com.mobidesk.android.data.VmStatusSample
import com.mobidesk.android.data.DefaultSessionGrantRepository
import com.mobidesk.android.data.SessionGrant
import com.mobidesk.android.data.SessionGrantRepository
import com.mobidesk.android.data.SessionGrantResult
import com.mobidesk.android.data.WindowsGuestCredentials
import com.mobidesk.android.data.WorkspaceRepository
import com.mobidesk.android.rdp.RdpSessionCarrier
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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
    val statusSample: VmStatusSample? = null,
    val isCheckingStatus: Boolean = false,
    val statusAgeText: String? = null,
    val isConnectEnabled: Boolean = false,
    val showGuestCredentialPrompt: Boolean = false,
    val isConnectingSession: Boolean = false,
    val sessionGrant: SessionGrant? = null,
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
    customStatusRepository: StatusRepository? = null,
    customRecoveryStore: RecoveryStateStore? = null,
    private val clock: MonotonicClock = SystemMonotonicClock,
    customSessionGrantRepository: SessionGrantRepository? = null
) : AndroidViewModel(application) {
    private val app = application as? MobiDeskApplication
    private val supabase = app?.supabaseClient
    private val authRepository: AuthRepository? =
        customAuthRepository ?: supabase?.let(::SupabaseAuthRepository)
    private val workspaceRepository: WorkspaceRepository? =
        customWorkspaceRepository ?: supabase?.let(::StudentWorkspaceRepository)
    private val statusRepository: StatusRepository? =
        customStatusRepository ?: if (authRepository != null && supabase != null) {
            DefaultStatusRepository(
                baseUrl = BuildConfig.SUPABASE_URL,
                clock = clock
            )
        } else null
    private val sessionGrantRepository: SessionGrantRepository? =
        customSessionGrantRepository ?: if (authRepository != null && supabase != null) {
            DefaultSessionGrantRepository(
                baseUrl = BuildConfig.SUPABASE_URL,
                clock = clock
            )
        } else null
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

    private val _navigateToConnectEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToConnectEvent = _navigateToConnectEvent.asSharedFlow()

    private val _navigateToSessionEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToSessionEvent = _navigateToSessionEvent.asSharedFlow()

    private var sessionGeneration: Long = 0L
    private var activeUserId: String? = null

    private var parallelLoadJob: Job? = null
    private var statusJob: Job? = null
    private var uiAgeTimerJob: Job? = null

    private var nextAllowedStatusRequestAt: Long = 0L
    private var lastActionTapAt: Long = 0L
    private var consecutiveTagMismatches: Int = 0

    init {
        observeSession()
        startUiAgeTimer()
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
                                    cancelActiveLoadsAndIncrementGeneration()
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
                                            statusSample = null,
                                            isCheckingStatus = false,
                                            errorMessage = "This recovery link was interrupted or has expired. Please request a new recovery link."
                                        )
                                    }
                                } else {
                                    cancelActiveLoadsAndIncrementGeneration()
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
                                            statusSample = null,
                                            isCheckingStatus = false,
                                            errorMessage = null
                                        )
                                    }
                                }
                            }
                            RecoveryStage.PASSWORD_UPDATE_REQUIRED -> {
                                cancelActiveLoadsAndIncrementGeneration()
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
                                        statusSample = null,
                                        isCheckingStatus = false,
                                        errorMessage = null
                                    )
                                }
                            }
                            RecoveryStage.INTERRUPTED -> {
                                cancelActiveLoadsAndIncrementGeneration()
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
                                        statusSample = null,
                                        isCheckingStatus = false,
                                        errorMessage = "This recovery link was interrupted or has expired. Please request a new recovery link."
                                    )
                                }
                            }
                            RecoveryStage.NONE -> {
                                val user = status.session.user
                                val userId = user?.id
                                if (userId == null) {
                                    cancelActiveLoadsAndIncrementGeneration()
                                    _uiState.update {
                                        it.copy(
                                            isRestoringSession = false,
                                            isBusy = false,
                                            isAuthenticated = false,
                                            authLevel = AuthLevel.SIGNED_OUT,
                                            isLoadingWorkspace = false,
                                            errorMessage = "We couldn't verify this account. Sign in again."
                                        )
                                    }
                                } else {
                                    val previousUserId = activeUserId
                                    if (previousUserId != userId || _uiState.value.authLevel != AuthLevel.FULLY_AUTHENTICATED) {
                                        activeUserId = userId
                                        sessionGeneration++
                                        consecutiveTagMismatches = 0
                                        parallelLoadJob?.cancel()
                                        statusJob?.cancel()
                                        _uiState.update {
                                            it.copy(
                                                isRestoringSession = false,
                                                isBusy = false,
                                                isAuthenticated = true,
                                                authLevel = AuthLevel.FULLY_AUTHENTICATED,
                                                isPasswordRecovery = false,
                                                isRecoveryInterrupted = false,
                                                email = user.email,
                                                errorMessage = null
                                            )
                                        }
                                        loadWorkspaceAndStatusParallel(userId, sessionGeneration)
                                    }
                                }
                            }
                        }
                    }
                    is SessionStatus.RefreshFailure -> {
                        cancelActiveLoadsAndIncrementGeneration()
                        recoveryStore.clear()
                        _uiState.update {
                            it.copy(
                                isRestoringSession = false,
                                isBusy = false,
                                isAuthenticated = false,
                                authLevel = AuthLevel.SIGNED_OUT,
                                email = null,
                                workspace = null,
                                isLoadingWorkspace = false,
                                statusSample = null,
                                isCheckingStatus = false,
                                isPasswordRecovery = false,
                                isRecoveryInterrupted = false,
                                signOutFailed = false,
                                errorMessage = "Your session expired. Sign in again."
                            )
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        cancelActiveLoadsAndIncrementGeneration()
                        if (recoveryStore.getStage() == RecoveryStage.PASSWORD_UPDATE_REQUIRED) {
                            recoveryStore.clear()
                        }
                        _uiState.update {
                            it.copy(
                                isRestoringSession = false,
                                isBusy = false,
                                isAuthenticated = false,
                                authLevel = AuthLevel.SIGNED_OUT,
                                email = null,
                                workspace = null,
                                isLoadingWorkspace = false,
                                statusSample = null,
                                isCheckingStatus = false,
                                isPasswordRecovery = false,
                                signOutFailed = false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun cancelActiveLoadsAndIncrementGeneration() {
        sessionGeneration++
        activeUserId = null
        consecutiveTagMismatches = 0
        parallelLoadJob?.cancel()
        parallelLoadJob = null
        statusJob?.cancel()
        statusJob = null
    }

    private fun startUiAgeTimer() {
        uiAgeTimerJob?.cancel()
        uiAgeTimerJob = viewModelScope.launch {
            while (isActive) {
                recalculateUiAgeAndConnect()
                delay(1000)
            }
        }
    }

    private fun recalculateUiAgeAndConnect() {
        val now = clock.elapsedRealtime()
        val currentSample = _uiState.value.statusSample
        val auth = _uiState.value.authLevel
        val isAssigned = _uiState.value.workspace is StudentWorkspaceResult.Assigned
        val isRequestInFlight = _uiState.value.isLoadingWorkspace || _uiState.value.isCheckingStatus
        val isCooldown = now < nextAllowedStatusRequestAt

        val ageText = currentSample?.relativeAgeText(now)
        val canConnect = ConnectPolicy.isConnectAvailable(
            authLevel = auth,
            isAssigned = isAssigned,
            sample = currentSample,
            nowElapsedMs = now,
            isRequestInFlight = isRequestInFlight,
            isCooldownActive = isCooldown
        )

        _uiState.update {
            it.copy(
                statusAgeText = ageText,
                isConnectEnabled = canConnect
            )
        }
    }

    private fun loadWorkspaceAndStatusParallel(userId: String, generation: Long) {
        val wkRepo = workspaceRepository ?: return
        val authRepo = authRepository ?: return
        val stRepo = statusRepository

        parallelLoadJob?.cancel()
        parallelLoadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingWorkspace = true,
                    isCheckingStatus = stRepo != null,
                    workspace = null,
                    statusSample = null,
                    isConnectEnabled = false
                )
            }

            try {
                if (stRepo == null) {
                    val workspace = wkRepo.loadFor(userId)
                    if (generation != sessionGeneration || _uiState.value.authLevel != AuthLevel.FULLY_AUTHENTICATED) {
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            isLoadingWorkspace = false,
                            isCheckingStatus = false,
                            workspace = workspace,
                            errorMessage = null
                        )
                    }
                    recalculateUiAgeAndConnect()
                    return@launch
                }

                val workspaceDeferred = async { wkRepo.loadFor(userId) }
                val statusDeferred = async {
                    val token = authRepo.currentAccessToken().orEmpty()
                    stRepo.getVmStatus(token)
                }

                val workspaceResult = workspaceDeferred.await()
                val statusResult = statusDeferred.await()

                if (generation != sessionGeneration || _uiState.value.authLevel != AuthLevel.FULLY_AUTHENTICATED) {
                    return@launch
                }

                handlePairedResults(workspaceResult, statusResult, userId, generation)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation == sessionGeneration) {
                    _uiState.update {
                        it.copy(
                            isLoadingWorkspace = false,
                            isCheckingStatus = false,
                            workspace = null,
                            statusSample = null,
                            errorMessage = AuthErrorMessages.safeMessage(e)
                        )
                    }
                    recalculateUiAgeAndConnect()
                }
            }
        }
    }

    private fun handlePairedResults(
        workspaceResult: StudentWorkspaceResult,
        statusResult: StatusResult,
        userId: String,
        generation: Long
    ) {
        when (workspaceResult) {
            is StudentWorkspaceResult.AccountNotProvisioned,
            is StudentWorkspaceResult.NoAssignment,
            is StudentWorkspaceResult.AssignmentInvalid -> {
                _uiState.update {
                    it.copy(
                        isLoadingWorkspace = false,
                        isCheckingStatus = false,
                        workspace = workspaceResult,
                        statusSample = null,
                        errorMessage = if (workspaceResult is StudentWorkspaceResult.AssignmentInvalid) {
                            "Your Cloud PC assignment needs administrator review."
                        } else null
                    )
                }
                recalculateUiAgeAndConnect()
            }
            is StudentWorkspaceResult.Assigned -> {
                val workspaceTag = workspaceResult.assignedWorkspace.assignmentTag
                when (statusResult) {
                    is StatusResult.Success -> {
                        val statusTag = statusResult.sample.assignmentTag
                        if (statusTag == workspaceTag) {
                            consecutiveTagMismatches = 0
                            _uiState.update {
                                it.copy(
                                    isLoadingWorkspace = false,
                                    isCheckingStatus = false,
                                    workspace = workspaceResult,
                                    statusSample = statusResult.sample,
                                    errorMessage = null
                                )
                            }
                        } else {
                            consecutiveTagMismatches++
                            if (consecutiveTagMismatches <= 1) {
                                loadWorkspaceAndStatusParallel(userId, generation)
                                return
                            } else {
                                _uiState.update {
                                    it.copy(
                                        isLoadingWorkspace = false,
                                        isCheckingStatus = false,
                                        workspace = null,
                                        statusSample = null,
                                        errorMessage = "We could not verify your assigned Cloud PC. Try again."
                                    )
                                }
                            }
                        }
                    }
                    is StatusResult.RateLimited -> {
                        val now = clock.elapsedRealtime()
                        nextAllowedStatusRequestAt = now + (statusResult.retryAfterSeconds * 1000L)
                        _uiState.update {
                            it.copy(
                                isLoadingWorkspace = false,
                                isCheckingStatus = false,
                                workspace = workspaceResult,
                                statusSample = null,
                                errorMessage = statusResult.message
                            )
                        }
                    }
                    is StatusResult.AuthExpired -> {
                        _uiState.update {
                            it.copy(
                                isLoadingWorkspace = false,
                                isCheckingStatus = false,
                                authLevel = AuthLevel.SIGNED_OUT,
                                isAuthenticated = false,
                                workspace = null,
                                statusSample = null,
                                errorMessage = statusResult.message
                            )
                        }
                    }
                    is StatusResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingWorkspace = false,
                                isCheckingStatus = false,
                                workspace = workspaceResult,
                                statusSample = null,
                                errorMessage = statusResult.message
                            )
                        }
                    }
                }
                recalculateUiAgeAndConnect()
            }
            is StudentWorkspaceResult.Ready -> {
                _uiState.update {
                    it.copy(
                        isLoadingWorkspace = false,
                        isCheckingStatus = false,
                        workspace = workspaceResult
                    )
                }
                recalculateUiAgeAndConnect()
            }
        }
    }

    fun refreshStatus() {
        val now = clock.elapsedRealtime()
        if (now - lastActionTapAt < 2000L) return
        if (now < nextAllowedStatusRequestAt) return
        lastActionTapAt = now

        val auth = authRepository ?: return
        val stRepo = statusRepository ?: return
        val currentWorkspace = _uiState.value.workspace as? StudentWorkspaceResult.Assigned ?: return
        val currentGen = sessionGeneration

        if (_uiState.value.isCheckingStatus) return

        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            _uiState.update { it.copy(isCheckingStatus = true, errorMessage = null) }
            recalculateUiAgeAndConnect()

            val token = auth.currentAccessToken().orEmpty()
            val result = stRepo.getVmStatus(token)

            if (currentGen != sessionGeneration || _uiState.value.authLevel != AuthLevel.FULLY_AUTHENTICATED) {
                return@launch
            }

            when (result) {
                is StatusResult.Success -> {
                    if (result.sample.assignmentTag == currentWorkspace.assignedWorkspace.assignmentTag) {
                        _uiState.update {
                            it.copy(
                                isCheckingStatus = false,
                                statusSample = result.sample,
                                errorMessage = null
                            )
                        }
                    } else {
                        val uid = activeUserId
                        if (uid != null) {
                            loadWorkspaceAndStatusParallel(uid, currentGen)
                            return@launch
                        }
                    }
                }
                is StatusResult.RateLimited -> {
                    val rateNow = clock.elapsedRealtime()
                    nextAllowedStatusRequestAt = rateNow + (result.retryAfterSeconds * 1000L)
                    _uiState.update {
                        it.copy(
                            isCheckingStatus = false,
                            statusSample = null,
                            errorMessage = result.message
                        )
                    }
                }
                is StatusResult.AuthExpired -> {
                    _uiState.update {
                        it.copy(
                            isCheckingStatus = false,
                            authLevel = AuthLevel.SIGNED_OUT,
                            isAuthenticated = false,
                            workspace = null,
                            statusSample = null,
                            errorMessage = result.message
                        )
                    }
                }
                is StatusResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCheckingStatus = false,
                            statusSample = null,
                            errorMessage = result.message
                        )
                    }
                }
            }
            recalculateUiAgeAndConnect()
        }
    }

    fun connect() {
        val now = clock.elapsedRealtime()
        if (now - lastActionTapAt < 2000L) return
        if (now < nextAllowedStatusRequestAt) return
        if (!_uiState.value.isConnectEnabled) return
        lastActionTapAt = now

        val auth = authRepository ?: return
        val stRepo = statusRepository ?: return
        val assignedWorkspace = (_uiState.value.workspace as? StudentWorkspaceResult.Assigned)?.assignedWorkspace ?: return
        val currentGen = sessionGeneration

        if (_uiState.value.isCheckingStatus) return

        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            _uiState.update { it.copy(isCheckingStatus = true, errorMessage = null) }
            recalculateUiAgeAndConnect()

            val token = auth.currentAccessToken().orEmpty()
            val result = stRepo.getVmStatus(token)

            if (currentGen != sessionGeneration || _uiState.value.authLevel != AuthLevel.FULLY_AUTHENTICATED) {
                return@launch
            }

            when (result) {
                is StatusResult.Success -> {
                    val sample = result.sample
                    val currentNow = clock.elapsedRealtime()
                    val eligible = ConnectPolicy.isHandoffEligible(
                        authLevel = _uiState.value.authLevel,
                        isAssigned = true,
                        currentAssignmentTag = assignedWorkspace.assignmentTag,
                        sample = sample,
                        nowElapsedMs = currentNow
                    )

                    _uiState.update {
                        it.copy(
                            isCheckingStatus = false,
                            statusSample = sample,
                            errorMessage = if (!eligible && sample.lifecycle == VmLifecycle.STOPPED) {
                                "Your Cloud PC is powered off. Contact the lab administrator."
                            } else if (!eligible && sample.lifecycle == VmLifecycle.STARTING) {
                                "Your Cloud PC is starting or is not ready yet."
                            } else if (!eligible && (sample.rdpTransport == RdpTransport.TCP_UNREACHABLE || sample.rdpTransport == RdpTransport.TCP_REACHABLE)) {
                                "Your Cloud PC cannot be reached right now."
                            } else null
                        )
                    }

                    if (eligible) {
                        _uiState.update { it.copy(showGuestCredentialPrompt = true) }
                        _navigateToConnectEvent.tryEmit(Unit)
                    }
                }
                is StatusResult.RateLimited -> {
                    val rateNow = clock.elapsedRealtime()
                    nextAllowedStatusRequestAt = rateNow + (result.retryAfterSeconds * 1000L)
                    _uiState.update {
                        it.copy(
                            isCheckingStatus = false,
                            statusSample = null,
                            errorMessage = result.message
                        )
                    }
                }
                is StatusResult.AuthExpired -> {
                    _uiState.update {
                        it.copy(
                            isCheckingStatus = false,
                            authLevel = AuthLevel.SIGNED_OUT,
                            isAuthenticated = false,
                            workspace = null,
                            statusSample = null,
                            errorMessage = result.message
                        )
                    }
                }
                is StatusResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCheckingStatus = false,
                            statusSample = null,
                            errorMessage = result.message
                        )
                    }
                }
            }
            recalculateUiAgeAndConnect()
        }
    }

    fun onGuestCredentialsSubmitted(credentials: WindowsGuestCredentials) {
        _uiState.update { it.copy(showGuestCredentialPrompt = false, isConnectingSession = true, errorMessage = null) }
        viewModelScope.launch {
            val auth = authRepository ?: run {
                credentials.clear()
                _uiState.update { it.copy(isConnectingSession = false, errorMessage = "Authentication service unavailable.") }
                return@launch
            }
            val grantRepo = sessionGrantRepository ?: run {
                credentials.clear()
                _uiState.update { it.copy(isConnectingSession = false, errorMessage = "Session authorization service unavailable.") }
                return@launch
            }
            val token = auth.currentAccessToken().orEmpty()
            val result = grantRepo.requestSessionGrant(token)
            when (result) {
                is SessionGrantResult.Success -> {
                    _uiState.update { it.copy(isConnectingSession = false, sessionGrant = result.grant) }
                    val brokerUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
                    val relayPath = if (result.grant.relayPath.startsWith("/")) result.grant.relayPath else "/${result.grant.relayPath}"
                    val wssUrl = if (brokerUrl.startsWith("https://")) {
                        brokerUrl.replaceFirst("https://", "wss://") + relayPath
                    } else if (brokerUrl.startsWith("http://")) {
                        brokerUrl.replaceFirst("http://", "ws://") + relayPath
                    } else {
                        "wss://$brokerUrl$relayPath"
                    }
                    RdpSessionCarrier.stageSession(
                        grant = result.grant,
                        credentials = credentials,
                        relayUrl = wssUrl
                    )
                    _navigateToSessionEvent.tryEmit(Unit)
                }
                is SessionGrantResult.RateLimited -> {
                    credentials.clear()
                    _uiState.update { it.copy(isConnectingSession = false, errorMessage = result.message) }
                }
                is SessionGrantResult.AuthExpired -> {
                    credentials.clear()
                    _uiState.update {
                        it.copy(
                            isConnectingSession = false,
                            authLevel = AuthLevel.SIGNED_OUT,
                            isAuthenticated = false,
                            errorMessage = result.message
                        )
                    }
                }
                is SessionGrantResult.Error -> {
                    credentials.clear()
                    _uiState.update { it.copy(isConnectingSession = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun onGuestCredentialPromptDismissed() {
        _uiState.update { it.copy(showGuestCredentialPrompt = false) }
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
        cancelActiveLoadsAndIncrementGeneration()
        _uiState.update {
            it.copy(
                isPasswordRecovery = true,
                isRecoveryInterrupted = false,
                authLevel = AuthLevel.RECOVERY_ONLY,
                workspace = null,
                isLoadingWorkspace = false,
                statusSample = null,
                isCheckingStatus = false,
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
            cancelActiveLoadsAndIncrementGeneration()
            _uiState.update {
                it.copy(
                    isBusy = false,
                    isAuthenticated = false,
                    authLevel = AuthLevel.SIGNED_OUT,
                    isPasswordRecovery = false,
                    isRecoveryInterrupted = false,
                    workspace = null,
                    isLoadingWorkspace = false,
                    statusSample = null,
                    isCheckingStatus = false,
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
                        activeUserId = userId
                        loadWorkspaceAndStatusParallel(userId, sessionGeneration)
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
                    cancelActiveLoadsAndIncrementGeneration()
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            isAuthenticated = false,
                            authLevel = AuthLevel.SIGNED_OUT,
                            email = null,
                            workspace = null,
                            isLoadingWorkspace = false,
                            statusSample = null,
                            isCheckingStatus = false,
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
            cancelActiveLoadsAndIncrementGeneration()
            _uiState.update {
                it.copy(
                    isBusy = false,
                    isAuthenticated = false,
                    authLevel = AuthLevel.SIGNED_OUT,
                    email = null,
                    workspace = null,
                    isLoadingWorkspace = false,
                    statusSample = null,
                    isCheckingStatus = false,
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
            loadWorkspaceAndStatusParallel(userId, sessionGeneration)
        }
    }

    fun showAuthLinkError(error: Throwable) {
        _uiState.update { it.copy(errorMessage = AuthErrorMessages.safeMessage(error)) }
    }

    fun handleRecoveryCallbackError(error: Throwable) {
        cancelActiveLoadsAndIncrementGeneration()
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
                statusSample = null,
                isCheckingStatus = false,
                errorMessage = AuthErrorMessages.safeMessage(error)
            )
        }
    }

    fun retryRecoveryAfterInterruption() {
        recoveryStore.clear()
        cancelActiveLoadsAndIncrementGeneration()
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

    private fun showConfigurationError() {
        _uiState.update {
            it.copy(errorMessage = "This build is missing its Supabase client configuration.")
        }
    }
}
