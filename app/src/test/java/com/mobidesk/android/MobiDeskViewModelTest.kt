package com.mobidesk.android

import android.app.Application
import com.mobidesk.android.data.AuthLevel
import com.mobidesk.android.data.AuthRepository
import com.mobidesk.android.data.RecoveryStage
import com.mobidesk.android.data.RecoveryStateStore
import com.mobidesk.android.data.StudentProfile
import com.mobidesk.android.data.StudentWorkspaceResult
import com.mobidesk.android.data.VmSummary
import com.mobidesk.android.data.WorkspaceRepository
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MobiDeskViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var fakeAuth: FakeAuthRepository
    private lateinit var fakeStore: FakeRecoveryStateStore
    private lateinit var workspaceSpy: WorkspaceRepositorySpy
    private lateinit var mockApp: Application

    private fun sampleUserSession(userId: String = "student-user-1", email: String = "student@college.edu"): UserSession {
        val payload = """
            {
                "access_token": "mock-token-xyz",
                "token_type": "bearer",
                "expires_in": 3600,
                "refresh_token": "mock-refresh-token",
                "user": {
                    "id": "$userId",
                    "email": "$email",
                    "aud": "authenticated",
                    "role": "authenticated"
                }
            }
        """.trimIndent()
        return json.decodeFromString<UserSession>(payload)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuth = FakeAuthRepository()
        fakeStore = FakeRecoveryStateStore()
        workspaceSpy = WorkspaceRepositorySpy()
        mockApp = Application()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCore01ColdStartInitialSessionRestoration() {
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )

        fakeAuth.emitStatus(SessionStatus.Initializing)
        assertTrue(viewModel.uiState.value.isRestoringSession)
        assertEquals(AuthLevel.SIGNED_OUT, viewModel.uiState.value.authLevel)

        fakeAuth.emitStatus(SessionStatus.NotAuthenticated(false))
        assertFalse(viewModel.uiState.value.isRestoringSession)
        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertEquals(AuthLevel.SIGNED_OUT, viewModel.uiState.value.authLevel)
    }

    @Test
    fun testCore02ProvisionedStudentLoginSuccess() {
        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )

        fakeAuth.emitStatus(SessionStatus.Authenticated(session))

        val state = viewModel.uiState.value
        assertTrue(state.isAuthenticated)
        assertEquals(AuthLevel.FULLY_AUTHENTICATED, state.authLevel)
        assertFalse(state.isPasswordRecovery)
        assertEquals("student@college.edu", state.email)
        assertEquals(1, workspaceSpy.loadCalls.size)
        assertEquals("student-user-1", workspaceSpy.loadCalls.first())
        assertTrue(state.workspace is StudentWorkspaceResult.Ready)
    }

    @Test
    fun testCore03InvalidCredentialsShowsSafeErrorMessage() {
        fakeAuth.shouldFailSignInWith = IllegalStateException("AuthApiError: code=invalid_credentials, msg=Invalid login credentials")
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )

        viewModel.signIn("student@college.edu", "wrongpass")

        val state = viewModel.uiState.value
        assertFalse(state.isBusy)
        assertEquals("The email or password is incorrect.", state.errorMessage)
    }

    @Test
    fun testCore05RefreshFailureTransitionsToSignedOut() {
        fakeStore.currentStage = RecoveryStage.PASSWORD_UPDATE_REQUIRED
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )

        fakeAuth.emitStatus(SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(IllegalStateException("Refresh token expired"))))

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticated)
        assertEquals(AuthLevel.SIGNED_OUT, state.authLevel)
        assertEquals("Your session expired. Sign in again.", state.errorMessage)
        assertEquals(RecoveryStage.NONE, fakeStore.currentStage)
        assertEquals(1, fakeStore.clearCalls)
    }

    @Test
    fun testCore06LocalSignOutClearsStateAndStore() {
        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )
        fakeAuth.emitStatus(SessionStatus.Authenticated(session))

        viewModel.signOut()

        val state = viewModel.uiState.value
        assertEquals(1, fakeAuth.signOutCurrentSessionCalls)
        assertFalse(state.isAuthenticated)
        assertEquals(AuthLevel.SIGNED_OUT, state.authLevel)
        assertNull(state.workspace)
        assertEquals(RecoveryStage.NONE, fakeStore.currentStage)
        assertTrue(fakeStore.clearCalls >= 1)
    }

    @Test
    fun testCore07SignOutFailureAllowsDeviceOnlySignOut() {
        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )
        fakeAuth.emitStatus(SessionStatus.Authenticated(session))

        fakeAuth.shouldFailSignOutWith = IOException("Connection reset by peer")
        viewModel.signOut()

        var state = viewModel.uiState.value
        assertTrue(state.signOutFailed)
        assertEquals("Could not confirm sign-out with the service. Check your connection or sign out on this device.", state.errorMessage)

        viewModel.signOutDeviceOnly()

        state = viewModel.uiState.value
        assertEquals(1, fakeAuth.clearLocalSessionCalls)
        assertFalse(state.isAuthenticated)
        assertFalse(state.signOutFailed)
        assertEquals(AuthLevel.SIGNED_OUT, state.authLevel)
        assertEquals("Signed out on this device. Remote session revocation could not be confirmed.", state.infoMessage)
    }

    @Test
    fun testCore08CallbackPendingBlocksWorkspaceLoading() {
        fakeStore.currentStage = RecoveryStage.CALLBACK_PENDING
        fakeStore.isExchangeInProgress = true
        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )

        fakeAuth.emitStatus(SessionStatus.Authenticated(session))

        val state = viewModel.uiState.value
        assertEquals(AuthLevel.RECOVERY_ONLY, state.authLevel)
        assertFalse(state.isPasswordRecovery)
        assertNull(state.workspace)
        assertFalse(state.isLoadingWorkspace)
        assertEquals("HARD INVARIANT: Workspace repo must never be called during CALLBACK_PENDING", 0, workspaceSpy.loadCalls.size)
    }

    @Test
    fun testCore09CancelPasswordRecoverySignsOutAndClearsMarker() {
        fakeStore.currentStage = RecoveryStage.PASSWORD_UPDATE_REQUIRED
        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )
        fakeAuth.emitStatus(SessionStatus.Authenticated(session))

        viewModel.cancelPasswordRecovery()

        val state = viewModel.uiState.value
        assertEquals(1, fakeAuth.signOutCurrentSessionCalls)
        assertFalse(state.isAuthenticated)
        assertEquals(AuthLevel.SIGNED_OUT, state.authLevel)
        assertFalse(state.isPasswordRecovery)
        assertNull(state.infoMessage)
        assertEquals(RecoveryStage.NONE, fakeStore.currentStage)
        assertEquals(0, workspaceSpy.loadCalls.size)
    }

    @Test
    fun testCore09CancelPasswordRecoveryRemoteFailureShowsHonestDisclaimer() {
        fakeStore.currentStage = RecoveryStage.PASSWORD_UPDATE_REQUIRED
        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )
        fakeAuth.emitStatus(SessionStatus.Authenticated(session))

        fakeAuth.shouldFailSignOutWith = IOException("Connection reset by peer")
        viewModel.cancelPasswordRecovery()

        val state = viewModel.uiState.value
        assertEquals(1, fakeAuth.signOutCurrentSessionCalls)
        assertEquals(1, fakeAuth.clearLocalSessionCalls)
        assertFalse(state.isAuthenticated)
        assertEquals(AuthLevel.SIGNED_OUT, state.authLevel)
        assertFalse(state.isPasswordRecovery)
        assertEquals(RecoveryStage.NONE, fakeStore.currentStage)
        assertEquals("Signed out on this device. Remote session revocation could not be confirmed.", state.infoMessage)
        assertEquals(0, workspaceSpy.loadCalls.size)
    }

    @Test
    fun testCore10PasswordUpdateRequiredEntersRecoveryOnly() {
        fakeStore.currentStage = RecoveryStage.PASSWORD_UPDATE_REQUIRED
        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )

        fakeAuth.emitStatus(SessionStatus.Authenticated(session))

        val state = viewModel.uiState.value
        assertEquals(AuthLevel.RECOVERY_ONLY, state.authLevel)
        assertTrue(state.isPasswordRecovery)
        assertEquals(0, workspaceSpy.loadCalls.size)
    }

    @Test
    fun testCore13InterruptedRecoveryFailsClosedAndClearsPartialSession() {
        fakeStore.currentStage = RecoveryStage.INTERRUPTED
        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )

        fakeAuth.emitStatus(SessionStatus.Authenticated(session))

        val state = viewModel.uiState.value
        assertEquals(AuthLevel.RECOVERY_ONLY, state.authLevel)
        assertFalse(state.isAuthenticated)
        assertTrue(state.isRecoveryInterrupted)
        assertEquals(1, fakeAuth.clearLocalSessionCalls)
        assertEquals(0, workspaceSpy.loadCalls.size)
    }

    @Test
    fun testCore13ProcessDeathWhileCallbackPendingFailsClosedOnStartup() {
        // Marker left in CALLBACK_PENDING across process death with no exchange in progress
        fakeStore.currentStage = RecoveryStage.CALLBACK_PENDING
        fakeStore.isExchangeInProgress = false

        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )

        fakeAuth.emitStatus(SessionStatus.Authenticated(session))

        val state = viewModel.uiState.value
        assertEquals(RecoveryStage.INTERRUPTED, fakeStore.currentStage)
        assertEquals(AuthLevel.RECOVERY_ONLY, state.authLevel)
        assertFalse(state.isAuthenticated)
        assertTrue(state.isRecoveryInterrupted)
        assertEquals(1, fakeAuth.clearLocalSessionCalls)
        assertEquals(0, workspaceSpy.loadCalls.size)
    }

    @Test
    fun testCore14PasswordUpdateSuccessTransitionsToFullyAuthenticatedAndLoadsWorkspace() {
        fakeStore.currentStage = RecoveryStage.PASSWORD_UPDATE_REQUIRED
        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )
        fakeAuth.emitStatus(SessionStatus.Authenticated(session))
        assertEquals(0, workspaceSpy.loadCalls.size)

        viewModel.updateRecoveredPassword("newValidPassword123", "newValidPassword123")

        val state = viewModel.uiState.value
        assertEquals("newValidPassword123", fakeAuth.updatedPasswords.firstOrNull())
        assertEquals(RecoveryStage.NONE, fakeStore.currentStage)
        assertEquals(AuthLevel.FULLY_AUTHENTICATED, state.authLevel)
        assertFalse(state.isPasswordRecovery)
        assertEquals("Your password has been updated.", state.infoMessage)
        assertEquals(1, workspaceSpy.loadCalls.size)
    }

    @Test
    fun testCore14PasswordUpdateFailureRemainsRecoveryOnly() {
        fakeStore.currentStage = RecoveryStage.PASSWORD_UPDATE_REQUIRED
        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )
        fakeAuth.emitStatus(SessionStatus.Authenticated(session))
        assertEquals(0, workspaceSpy.loadCalls.size)

        fakeAuth.shouldFailUpdatePasswordWith = IOException("Connection lost during password update")
        viewModel.updateRecoveredPassword("newValidPassword123", "newValidPassword123")

        val state = viewModel.uiState.value
        assertEquals(AuthLevel.RECOVERY_ONLY, state.authLevel)
        assertTrue(state.isPasswordRecovery)
        assertEquals("We couldn't reach the sign-in service. Check your connection and try again.", state.errorMessage)
        assertEquals(0, workspaceSpy.loadCalls.size)
    }

    @Test
    fun testCore15WorkspaceSpyProvesZeroCallsDuringRecoveryOnly() {
        fakeStore.currentStage = RecoveryStage.PASSWORD_UPDATE_REQUIRED
        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )
        fakeAuth.emitStatus(SessionStatus.Authenticated(session))

        // Short password fails validation
        viewModel.updateRecoveredPassword("short", "short")
        assertEquals("Use a password with at least 8 characters.", viewModel.uiState.value.errorMessage)
        assertEquals(0, workspaceSpy.loadCalls.size)

        // Password mismatch fails validation
        viewModel.updateRecoveredPassword("password123", "mismatch123")
        assertEquals("The passwords don't match.", viewModel.uiState.value.errorMessage)
        assertEquals(0, workspaceSpy.loadCalls.size)

        // Calling retryWorkspaceLoad during recovery has no effect
        viewModel.retryWorkspaceLoad()
        assertEquals(0, workspaceSpy.loadCalls.size)
    }

    @Test
    fun testCore17BusyGuardPreventsDuplicateInvocations() {
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )

        fakeAuth.isSignInBlocking = true
        viewModel.signIn("student@college.edu", "password123")
        assertTrue(viewModel.uiState.value.isBusy)

        // Second call while busy should be ignored
        viewModel.signIn("student@college.edu", "password123")
        assertEquals(1, fakeAuth.signInCalls.size)
    }

    @Test
    fun testCore18FakeInjectionAllowsTestingWithoutSupabaseClient() {
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )
        assertTrue(viewModel.uiState.value.serviceConfigured)
    }

    @Test
    fun testCore19EmailAddressNotAuthorizedDirectsToAdmin() {
        fakeAuth.shouldFailResetWith = IllegalStateException("AuthApiError: code=email_address_not_authorized, msg=Email address not authorized")
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )

        viewModel.requestPasswordReset("unauthorized@college.edu")

        val state = viewModel.uiState.value
        assertEquals(
            "Recovery email delivery is not configured for this recipient. Contact your institution's MobiDesk administrator.",
            state.errorMessage
        )
    }

    @Test
    fun testCore20OverEmailSendRateLimitMapsToPatience() {
        fakeAuth.shouldFailResetWith = IllegalStateException("AuthApiError: code=over_email_send_rate_limit, msg=Over email rate limit")
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )

        viewModel.requestPasswordReset("student@college.edu")

        val state = viewModel.uiState.value
        assertEquals(
            "Too many recovery emails have been sent. Please wait before requesting another.",
            state.errorMessage
        )
    }

    @Test
    fun testCore21OverRequestRateLimitMapsToPatience() {
        fakeAuth.shouldFailSignInWith = IllegalStateException("AuthApiError: code=over_request_rate_limit, msg=Over request rate limit")
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )

        viewModel.signIn("student@college.edu", "password123")

        val state = viewModel.uiState.value
        assertEquals(
            "Too many sign-in requests have been made. Please wait before retrying.",
            state.errorMessage
        )
    }

    @Test
    fun testCore22EmailNotConfirmedOrdering() {
        fakeAuth.shouldFailSignInWith = IllegalStateException("AuthApiError: code=email_not_confirmed, msg=Email not confirmed")
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )

        viewModel.signIn("unconfirmed@college.edu", "password123")

        val state = viewModel.uiState.value
        assertEquals(
            "Confirm your school email before signing in.",
            state.errorMessage
        )
    }

    @Test
    fun testCore23KnownAndUnknownEmailResetProduceIdenticalAcknowledgement() {
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customRecoveryStore = fakeStore
        )

        // Request 1
        viewModel.requestPasswordReset("known@college.edu")
        val msg1 = viewModel.uiState.value.infoMessage

        // Request 2
        viewModel.clearMessages()
        viewModel.requestPasswordReset("unknown@college.edu")
        val msg2 = viewModel.uiState.value.infoMessage

        assertEquals(
            "If this account can receive recovery email, instructions will be sent.",
            msg1
        )
        assertEquals(msg1, msg2)
    }

    @Test
    fun testCore24MissingClientShowsConfigurationError() {
        val unconfiguredApp = Application()
        val viewModel = MobiDeskViewModel(
            application = unconfiguredApp,
            customAuthRepository = null,
            customWorkspaceRepository = null,
            customRecoveryStore = fakeStore
        )

        assertFalse(viewModel.uiState.value.serviceConfigured)
        viewModel.signIn("student@college.edu", "password123")
        assertEquals("This build is missing its Supabase client configuration.", viewModel.uiState.value.errorMessage)
    }
}

class FakeAuthRepository : AuthRepository {
    private val _sessionStatus = MutableStateFlow<SessionStatus>(SessionStatus.Initializing)
    override val sessionStatus = _sessionStatus.asStateFlow()

    var currentUser: UserInfo? = null
    var shouldFailSignInWith: Throwable? = null
    var shouldFailSignOutWith: Throwable? = null
    var shouldFailResetWith: Throwable? = null
    var shouldFailUpdatePasswordWith: Throwable? = null
    var isSignInBlocking: Boolean = false

    val signInCalls = mutableListOf<Pair<String, String>>()
    val resetCalls = mutableListOf<String>()
    val updatedPasswords = mutableListOf<String>()
    var signOutCurrentSessionCalls = 0
    var clearLocalSessionCalls = 0

    fun emitStatus(status: SessionStatus) {
        _sessionStatus.value = status
    }

    override fun currentUser(): UserInfo? = currentUser

    override suspend fun signIn(email: String, password: String) {
        signInCalls.add(email to password)
        if (isSignInBlocking) {
            // Leave busy state active
            return
        }
        shouldFailSignInWith?.let { throw it }
    }

    override suspend fun signOut() {
        signOutCurrentSession()
    }

    override suspend fun signOutCurrentSession() {
        signOutCurrentSessionCalls++
        shouldFailSignOutWith?.let { throw it }
    }

    override suspend fun clearLocalSession() {
        clearLocalSessionCalls++
    }

    override suspend fun requestPasswordReset(email: String) {
        resetCalls.add(email)
        shouldFailResetWith?.let { throw it }
    }

    override suspend fun updatePassword(password: String) {
        updatedPasswords.add(password)
        shouldFailUpdatePasswordWith?.let { throw it }
    }
}

class WorkspaceRepositorySpy : WorkspaceRepository {
    val loadCalls = mutableListOf<String>()
    var stubResult: StudentWorkspaceResult = StudentWorkspaceResult.Ready(
        profile = StudentProfile(id = "student-user-1", name = "Test Student", college = "Engineering"),
        assignedVms = listOf(VmSummary(id = "vm-101", name = "CAD Workstation"))
    )
    var shouldFailWith: Throwable? = null

    override suspend fun loadFor(userId: String): StudentWorkspaceResult {
        loadCalls.add(userId)
        shouldFailWith?.let { throw it }
        return stubResult
    }
}

class FakeRecoveryStateStore : RecoveryStateStore {
    override var isExchangeInProgress: Boolean = false
    var currentStage: RecoveryStage = RecoveryStage.NONE
    var currentTimestamp: Long = 0L
    var currentFingerprint: String? = null
    var clearCalls = 0
    val consumedFingerprints = mutableSetOf<String>()

    override fun getStage(): RecoveryStage = currentStage

    override fun getCodeFingerprint(): String? = currentFingerprint

    override fun getTimestamp(): Long = currentTimestamp

    override fun setStage(stage: RecoveryStage, codeFingerprint: String?): Boolean {
        this.currentStage = stage
        if (codeFingerprint != null) {
            this.currentFingerprint = codeFingerprint
        }
        this.currentTimestamp = System.currentTimeMillis()
        return true
    }

    override fun markFingerprintConsumed(codeFingerprint: String): Boolean {
        consumedFingerprints.add(codeFingerprint)
        return true
    }

    override fun isFingerprintConsumed(codeFingerprint: String): Boolean {
        return consumedFingerprints.contains(codeFingerprint)
    }

    override fun clear(): Boolean {
        currentStage = RecoveryStage.NONE
        currentTimestamp = 0L
        currentFingerprint = null
        isExchangeInProgress = false
        clearCalls++
        return true
    }
}
