package com.mobidesk.android.rdp

import com.mobidesk.android.data.SessionGrant
import com.mobidesk.android.data.WindowsGuestCredentials

/**
 * Ephemeral In-Memory Carrier for Active RDP Session Parameters.
 *
 * Security Invariants:
 * 1. ZERO Intent extra storage: Android Intent extras are exposed to system logs, IPC inspection,
 *    and task record dumps. Session grant tokens and Windows guest credentials must NEVER be placed
 *    into an Intent bundle.
 * 2. Consumed exactly once upon [RdpSessionActivity] creation.
 * 3. Deterministically cleared on failure or session teardown.
 */
object RdpSessionCarrier {

    data class SessionData(
        val grant: SessionGrant,
        val credentials: WindowsGuestCredentials,
        val relayUrl: String
    )

    @Volatile
    private var pendingSession: SessionData? = null

    /**
     * Stages session parameters before launching [RdpSessionActivity].
     */
    fun stageSession(grant: SessionGrant, credentials: WindowsGuestCredentials, relayUrl: String) {
        synchronized(this) {
            pendingSession?.credentials?.clear()
            pendingSession = SessionData(
                grant = grant,
                credentials = credentials,
                relayUrl = relayUrl
            )
        }
    }

    /**
     * Consumes and clears the staged session parameters.
     */
    fun consumeSession(): SessionData? {
        synchronized(this) {
            val session = pendingSession
            pendingSession = null
            return session
        }
    }

    /**
     * Clears and securely zeroes any held credentials.
     */
    fun clear() {
        synchronized(this) {
            pendingSession?.credentials?.clear()
            pendingSession = null
        }
    }
}
