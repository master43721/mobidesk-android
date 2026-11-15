package com.mobidesk.android

import android.app.Application
import com.mobidesk.android.data.AssignedWorkspace
import com.mobidesk.android.data.AuthLevel
import com.mobidesk.android.data.AuthRepository
import com.mobidesk.android.data.MonotonicClock
import com.mobidesk.android.data.RdpTransport
import com.mobidesk.android.data.RecoveryStateStore
import com.mobidesk.android.data.StatusRepository
import com.mobidesk.android.data.StatusResult
import com.mobidesk.android.data.StudentProfile
import com.mobidesk.android.data.StudentWorkspaceResult
import com.mobidesk.android.data.VmLifecycle
import com.mobidesk.android.data.VmStatusSample
import com.mobidesk.android.data.WorkspaceRepository
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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

@OptIn(ExperimentalCoroutinesApi::class)
class MobiDeskFp02Test {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var fakeAuth: FakeAuthRepository
    private lateinit var fakeStore: FakeRecoveryStateStore
    private lateinit var workspaceSpy: WorkspaceRepositorySpy
    private lateinit var fakeStatusRepo: FakeStatusRepository
    private lateinit var fakeClock: FakeClock
    private lateinit var mockApp: Application

    class FakeClock(var timeMs: Long = 1000L) : MonotonicClock {
        override fun elapsedRealtime(): Long = timeMs
    }

    class FakeStatusRepository : StatusRepository {
        var calls = mutableListOf<String>()
        var stubResult: StatusResult = StatusResult.Success(
            VmStatusSample(
                lifecycle = VmLifecycle.RUNNING,
                rdpTransport = RdpTransport.RDP_NEGOTIATION_OK,
                sampledAtFormatted = "2026-11-05T12:00:00Z",
                probeDurationMs = 150L,
                staleAfterSeconds = 15L,
                requestId = "req_1",
                assignmentTag = "asg_tag_vm1",
                requestStartedElapsedMs = 1000L,
                responseReceivedElapsedMs = 1100L
            )
        )

        override suspend fun getVmStatus(accessToken: String): StatusResult {
            calls.add(accessToken)
            return stubResult
        }
    }

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
        fakeStatusRepo = FakeStatusRepository()
        fakeClock = FakeClock(2000L)
        mockApp = Application()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testParallelLoading_startsBothConcurrentlyOnAuthenticated() {
        workspaceSpy.stubResult = StudentWorkspaceResult.Assigned(
            profile = StudentProfile(id = "student-user-1", name = "Test Student", college = "Engineering"),
            assignedWorkspace = AssignedWorkspace(displayLabel = "CAD Workstation", assignmentTag = "asg_tag_vm1")
        )

        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customStatusRepository = fakeStatusRepo,
            customRecoveryStore = fakeStore,
            clock = fakeClock
        )

        fakeAuth.emitStatus(SessionStatus.Authenticated(session))

        assertEquals(1, workspaceSpy.loadCalls.size)
        assertEquals(1, fakeStatusRepo.calls.size)
        val state = viewModel.uiState.value
        assertEquals(AuthLevel.FULLY_AUTHENTICATED, state.authLevel)
        assertTrue(state.workspace is StudentWorkspaceResult.Assigned)
        assertNotNull(state.statusSample)
        assertEquals("asg_tag_vm1", state.statusSample?.assignmentTag)
        assertTrue(state.isConnectEnabled)
    }

    @Test
    fun testTagMismatch_reloadsOnce_andTransitionsToAssign02OnSecondMismatch() {
        workspaceSpy.stubResult = StudentWorkspaceResult.Assigned(
            profile = StudentProfile(id = "student-user-1", name = "Test Student", college = "Engineering"),
            assignedWorkspace = AssignedWorkspace(displayLabel = "CAD Workstation", assignmentTag = "tag-A")
        )
        fakeStatusRepo.stubResult = StatusResult.Success(
            VmStatusSample(
                lifecycle = VmLifecycle.RUNNING,
                rdpTransport = RdpTransport.RDP_NEGOTIATION_OK,
                sampledAtFormatted = null,
                probeDurationMs = 100L,
                staleAfterSeconds = 15L,
                requestId = "req_1",
                assignmentTag = "tag-B",
                requestStartedElapsedMs = 1000L,
                responseReceivedElapsedMs = 1100L
            )
        )

        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customStatusRepository = fakeStatusRepo,
            customRecoveryStore = fakeStore,
            clock = fakeClock
        )

        fakeAuth.emitStatus(SessionStatus.Authenticated(session))

        assertEquals(2, workspaceSpy.loadCalls.size)
        assertEquals(2, fakeStatusRepo.calls.size)

        val state = viewModel.uiState.value
        assertEquals("We could not verify your assigned Cloud PC. Try again.", state.errorMessage)
        assertNull(state.workspace)
        assertNull(state.statusSample)
        assertFalse(state.isConnectEnabled)
    }

    @Test
    fun testRateLimit_setsCooldownAndAssign02Message() {
        workspaceSpy.stubResult = StudentWorkspaceResult.Assigned(
            profile = StudentProfile(id = "student-user-1", name = "Test Student", college = "Engineering"),
            assignedWorkspace = AssignedWorkspace(displayLabel = "CAD Workstation", assignmentTag = "tag-A")
        )
        fakeStatusRepo.stubResult = StatusResult.RateLimited(
            retryAfterSeconds = 10L,
            message = "We could not verify your assigned Cloud PC. Try again."
        )

        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customStatusRepository = fakeStatusRepo,
            customRecoveryStore = fakeStore,
            clock = fakeClock
        )

        fakeAuth.emitStatus(SessionStatus.Authenticated(session))

        val state = viewModel.uiState.value
        assertEquals("We could not verify your assigned Cloud PC. Try again.", state.errorMessage)
        assertNull(state.statusSample)
        assertFalse(state.isConnectEnabled)
    }

    @Test
    fun testConnectTap_invokesFreshStatusCheck_andNavigatesOnEligible() {
        workspaceSpy.stubResult = StudentWorkspaceResult.Assigned(
            profile = StudentProfile(id = "student-user-1", name = "Test Student", college = "Engineering"),
            assignedWorkspace = AssignedWorkspace(displayLabel = "CAD Workstation", assignmentTag = "tag-A")
        )
        fakeStatusRepo.stubResult = StatusResult.Success(
            VmStatusSample(
                lifecycle = VmLifecycle.RUNNING,
                rdpTransport = RdpTransport.RDP_NEGOTIATION_OK,
                sampledAtFormatted = null,
                probeDurationMs = 100L,
                staleAfterSeconds = 15L,
                requestId = "req_1",
                assignmentTag = "tag-A",
                requestStartedElapsedMs = 1000L,
                responseReceivedElapsedMs = 1100L
            )
        )

        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customStatusRepository = fakeStatusRepo,
            customRecoveryStore = fakeStore,
            clock = fakeClock
        )

        fakeAuth.emitStatus(SessionStatus.Authenticated(session))
        assertEquals(1, fakeStatusRepo.calls.size)

        fakeClock.timeMs += 3000L

        var navigated = false
        val scope = TestScope(testDispatcher)
        val collectJob = scope.launch {
            viewModel.navigateToConnectEvent.collect {
                navigated = true
            }
        }

        viewModel.connect()

        assertEquals(2, fakeStatusRepo.calls.size)
        assertTrue(navigated)

        collectJob.cancel()
    }

    @Test
    fun testManualRefresh_updatesSample() {
        workspaceSpy.stubResult = StudentWorkspaceResult.Assigned(
            profile = StudentProfile(id = "student-user-1", name = "Test Student", college = "Engineering"),
            assignedWorkspace = AssignedWorkspace(displayLabel = "CAD Workstation", assignmentTag = "tag-A")
        )
        fakeStatusRepo.stubResult = StatusResult.Success(
            VmStatusSample(
                lifecycle = VmLifecycle.RUNNING,
                rdpTransport = RdpTransport.RDP_NEGOTIATION_OK,
                sampledAtFormatted = null,
                probeDurationMs = 100L,
                staleAfterSeconds = 15L,
                requestId = "req_1",
                assignmentTag = "tag-A",
                requestStartedElapsedMs = 1000L,
                responseReceivedElapsedMs = 1100L
            )
        )

        val session = sampleUserSession()
        fakeAuth.currentUser = session.user
        val viewModel = MobiDeskViewModel(
            application = mockApp,
            customAuthRepository = fakeAuth,
            customWorkspaceRepository = workspaceSpy,
            customStatusRepository = fakeStatusRepo,
            customRecoveryStore = fakeStore,
            clock = fakeClock
        )

        fakeAuth.emitStatus(SessionStatus.Authenticated(session))
        assertEquals(1, fakeStatusRepo.calls.size)

        fakeClock.timeMs += 3000L

        fakeStatusRepo.stubResult = StatusResult.Success(
            VmStatusSample(
                lifecycle = VmLifecycle.STOPPED,
                rdpTransport = RdpTransport.UNKNOWN,
                sampledAtFormatted = null,
                probeDurationMs = 120L,
                staleAfterSeconds = 15L,
                requestId = "req_2",
                assignmentTag = "tag-A",
                requestStartedElapsedMs = 4000L,
                responseReceivedElapsedMs = 4100L
            )
        )

        viewModel.refreshStatus()
        assertEquals(2, fakeStatusRepo.calls.size)
        val state = viewModel.uiState.value
        assertEquals(VmLifecycle.STOPPED, state.statusSample?.lifecycle)
        assertFalse(state.isConnectEnabled)
    }
}
