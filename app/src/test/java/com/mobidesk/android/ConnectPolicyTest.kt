package com.mobidesk.android

import com.mobidesk.android.data.AuthLevel
import com.mobidesk.android.data.ConnectPolicy
import com.mobidesk.android.data.RdpTransport
import com.mobidesk.android.data.VmLifecycle
import com.mobidesk.android.data.VmStatusSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectPolicyTest {

    private fun createSample(
        lifecycle: VmLifecycle,
        rdpTransport: RdpTransport,
        tag: String = "asg_tag_vm1",
        staleAfterSeconds: Long = 15L,
        startElapsedMs: Long = 1000L,
        receivedElapsedMs: Long = 1200L
    ): VmStatusSample {
        return VmStatusSample(
            lifecycle = lifecycle,
            rdpTransport = rdpTransport,
            sampledAtFormatted = "2026-11-05T12:00:00Z",
            probeDurationMs = 200L,
            staleAfterSeconds = staleAfterSeconds,
            requestId = "req_1",
            assignmentTag = tag,
            requestStartedElapsedMs = startElapsedMs,
            responseReceivedElapsedMs = receivedElapsedMs
        )
    }

    @Test
    fun ageUpperBound_calculationAndFreshness() {
        val sample = createSample(
            lifecycle = VmLifecycle.RUNNING,
            rdpTransport = RdpTransport.RDP_NEGOTIATION_OK,
            startElapsedMs = 1000L,
            receivedElapsedMs = 1200L
        )

        assertEquals(200L, sample.ageUpperBoundMs(1200L))
        assertTrue(sample.isFresh(1200L))
        assertEquals("Updated just now", sample.relativeAgeText(1200L))

        assertEquals(6200L, sample.ageUpperBoundMs(7200L))
        assertTrue(sample.isFresh(7200L))
        assertEquals("Updated 6 seconds ago", sample.relativeAgeText(7200L))

        assertEquals(15000L, sample.ageUpperBoundMs(16000L))
        assertFalse(sample.isFresh(16000L))
        assertEquals("Status needs refreshing", sample.relativeAgeText(16000L))
    }

    @Test
    fun connectAvailability_truthTable() {
        val nowElapsedMs = 2000L

        assertTrue(
            ConnectPolicy.isConnectAvailable(
                authLevel = AuthLevel.FULLY_AUTHENTICATED,
                isAssigned = true,
                sample = null,
                nowElapsedMs = nowElapsedMs,
                isRequestInFlight = false,
                isCooldownActive = false
            )
        )

        assertFalse(
            ConnectPolicy.isConnectAvailable(
                authLevel = AuthLevel.RECOVERY_ONLY,
                isAssigned = true,
                sample = null,
                nowElapsedMs = nowElapsedMs,
                isRequestInFlight = false,
                isCooldownActive = false
            )
        )
        assertFalse(
            ConnectPolicy.isConnectAvailable(
                authLevel = AuthLevel.FULLY_AUTHENTICATED,
                isAssigned = false,
                sample = null,
                nowElapsedMs = nowElapsedMs,
                isRequestInFlight = false,
                isCooldownActive = false
            )
        )

        val stoppedSample = createSample(VmLifecycle.STOPPED, RdpTransport.UNKNOWN)
        assertFalse(
            ConnectPolicy.isConnectAvailable(
                authLevel = AuthLevel.FULLY_AUTHENTICATED,
                isAssigned = true,
                sample = stoppedSample,
                nowElapsedMs = nowElapsedMs,
                isRequestInFlight = false,
                isCooldownActive = false
            )
        )

        val unreachableSample = createSample(VmLifecycle.RUNNING, RdpTransport.TCP_UNREACHABLE)
        assertFalse(
            ConnectPolicy.isConnectAvailable(
                authLevel = AuthLevel.FULLY_AUTHENTICATED,
                isAssigned = true,
                sample = unreachableSample,
                nowElapsedMs = nowElapsedMs,
                isRequestInFlight = false,
                isCooldownActive = false
            )
        )

        val tcpReachableSample = createSample(VmLifecycle.RUNNING, RdpTransport.TCP_REACHABLE)
        assertFalse(
            ConnectPolicy.isConnectAvailable(
                authLevel = AuthLevel.FULLY_AUTHENTICATED,
                isAssigned = true,
                sample = tcpReachableSample,
                nowElapsedMs = nowElapsedMs,
                isRequestInFlight = false,
                isCooldownActive = false
            )
        )

        val readySample = createSample(VmLifecycle.RUNNING, RdpTransport.RDP_NEGOTIATION_OK)
        assertTrue(
            ConnectPolicy.isConnectAvailable(
                authLevel = AuthLevel.FULLY_AUTHENTICATED,
                isAssigned = true,
                sample = readySample,
                nowElapsedMs = nowElapsedMs,
                isRequestInFlight = false,
                isCooldownActive = false
            )
        )

        val unknownSample = createSample(VmLifecycle.UNKNOWN, RdpTransport.UNKNOWN)
        assertTrue(
            ConnectPolicy.isConnectAvailable(
                authLevel = AuthLevel.FULLY_AUTHENTICATED,
                isAssigned = true,
                sample = unknownSample,
                nowElapsedMs = nowElapsedMs,
                isRequestInFlight = false,
                isCooldownActive = false
            )
        )

        val staleStoppedSample = stoppedSample
        val staleNowMs = 20000L
        assertTrue(
            ConnectPolicy.isConnectAvailable(
                authLevel = AuthLevel.FULLY_AUTHENTICATED,
                isAssigned = true,
                sample = staleStoppedSample,
                nowElapsedMs = staleNowMs,
                isRequestInFlight = false,
                isCooldownActive = false
            )
        )
    }

    @Test
    fun handoffEligibility_strictVerification() {
        val nowElapsedMs = 2000L
        val readySample = createSample(VmLifecycle.RUNNING, RdpTransport.RDP_NEGOTIATION_OK, tag = "tag-1")

        assertTrue(
            ConnectPolicy.isHandoffEligible(
                authLevel = AuthLevel.FULLY_AUTHENTICATED,
                isAssigned = true,
                currentAssignmentTag = "tag-1",
                sample = readySample,
                nowElapsedMs = nowElapsedMs
            )
        )

        assertFalse(
            ConnectPolicy.isHandoffEligible(
                authLevel = AuthLevel.FULLY_AUTHENTICATED,
                isAssigned = true,
                currentAssignmentTag = "tag-different",
                sample = readySample,
                nowElapsedMs = nowElapsedMs
            )
        )

        val staleNowMs = 20000L
        assertFalse(
            ConnectPolicy.isHandoffEligible(
                authLevel = AuthLevel.FULLY_AUTHENTICATED,
                isAssigned = true,
                currentAssignmentTag = "tag-1",
                sample = readySample,
                nowElapsedMs = staleNowMs
            )
        )

        val startingSample = createSample(VmLifecycle.STARTING, RdpTransport.RDP_NEGOTIATION_OK, tag = "tag-1")
        assertFalse(
            ConnectPolicy.isHandoffEligible(
                authLevel = AuthLevel.FULLY_AUTHENTICATED,
                isAssigned = true,
                currentAssignmentTag = "tag-1",
                sample = startingSample,
                nowElapsedMs = nowElapsedMs
            )
        )

        val tcpSample = createSample(VmLifecycle.RUNNING, RdpTransport.TCP_REACHABLE, tag = "tag-1")
        assertFalse(
            ConnectPolicy.isHandoffEligible(
                authLevel = AuthLevel.FULLY_AUTHENTICATED,
                isAssigned = true,
                currentAssignmentTag = "tag-1",
                sample = tcpSample,
                nowElapsedMs = nowElapsedMs
            )
        )
    }
}
