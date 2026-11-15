package com.mobidesk.android.data

object ConnectPolicy {

    fun isConnectAvailable(
        authLevel: AuthLevel,
        isAssigned: Boolean,
        sample: VmStatusSample?,
        nowElapsedMs: Long,
        isRequestInFlight: Boolean,
        isCooldownActive: Boolean
    ): Boolean {
        if (authLevel != AuthLevel.FULLY_AUTHENTICATED) return false
        if (!isAssigned) return false
        if (isRequestInFlight) return false
        if (isCooldownActive) return false

        if (sample == null) {
            return true
        }

        val isFresh = sample.isFresh(nowElapsedMs)
        if (!isFresh) {
            return true
        }

        if (sample.lifecycle.isKnownNegative || sample.rdpTransport.isKnownNegative) {
            return false
        }

        return true
    }

    fun isHandoffEligible(
        authLevel: AuthLevel,
        isAssigned: Boolean,
        currentAssignmentTag: String?,
        sample: VmStatusSample?,
        nowElapsedMs: Long
    ): Boolean {
        if (authLevel != AuthLevel.FULLY_AUTHENTICATED) return false
        if (!isAssigned) return false
        if (sample == null) return false
        if (currentAssignmentTag.isNullOrBlank() || sample.assignmentTag != currentAssignmentTag) return false
        if (!sample.isFresh(nowElapsedMs)) return false
        if (sample.lifecycle != VmLifecycle.RUNNING) return false
        if (sample.rdpTransport != RdpTransport.RDP_NEGOTIATION_OK) return false

        return true
    }
}
