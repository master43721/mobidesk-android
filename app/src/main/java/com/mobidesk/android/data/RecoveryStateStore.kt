package com.mobidesk.android.data

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

enum class AuthLevel {
    SIGNED_OUT,
    RECOVERY_ONLY,
    FULLY_AUTHENTICATED
}

enum class RecoveryStage {
    NONE,
    CALLBACK_PENDING,
    PASSWORD_UPDATE_REQUIRED,
    INTERRUPTED
}

data class AuthState(
    val level: AuthLevel,
    val userId: String? = null,
    val email: String? = null,
    val recoveryStage: RecoveryStage = RecoveryStage.NONE
)

interface RecoveryStateStore {
    var isExchangeInProgress: Boolean
    fun getStage(): RecoveryStage
    fun getCodeFingerprint(): String?
    fun getTimestamp(): Long
    fun setStage(stage: RecoveryStage, codeFingerprint: String? = null): Boolean
    fun markFingerprintConsumed(codeFingerprint: String): Boolean
    fun isFingerprintConsumed(codeFingerprint: String): Boolean
    fun clear(): Boolean
}

class SharedPreferencesRecoveryStateStore(
    private val prefs: SharedPreferences,
    private val maxRetentionMs: Long = 3600_000L // 1 hour bounded retention aligned with recovery expiry
) : RecoveryStateStore {
    constructor(
        context: Context,
        maxRetentionMs: Long = 3600_000L
    ) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        maxRetentionMs
    )

    override var isExchangeInProgress: Boolean = false

    override fun getStage(): RecoveryStage {
        val stageName = prefs.getString(KEY_STAGE, null) ?: return RecoveryStage.NONE
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        if (timestamp > 0 && System.currentTimeMillis() - timestamp > maxRetentionMs) {
            clear()
            return RecoveryStage.NONE
        }
        return runCatching { RecoveryStage.valueOf(stageName) }.getOrDefault(RecoveryStage.NONE)
    }

    override fun getCodeFingerprint(): String? {
        return prefs.getString(KEY_CODE_FINGERPRINT, null)
    }

    override fun getTimestamp(): Long {
        return prefs.getLong(KEY_TIMESTAMP, 0L)
    }

    override fun setStage(stage: RecoveryStage, codeFingerprint: String?): Boolean {
        val editor = prefs.edit()
            .putString(KEY_STAGE, stage.name)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
        if (codeFingerprint != null) {
            editor.putString(KEY_CODE_FINGERPRINT, codeFingerprint)
        } else {
            editor.remove(KEY_CODE_FINGERPRINT)
        }
        return editor.commit()
    }

    override fun markFingerprintConsumed(codeFingerprint: String): Boolean {
        val now = System.currentTimeMillis()
        val rawSet = prefs.getStringSet(KEY_CONSUMED_FINGERPRINTS, emptySet()) ?: emptySet()
        // Filter out expired entries older than maxRetentionMs
        val validEntries = rawSet.mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val fp = parts[0]
                val ts = parts[1].toLongOrNull() ?: 0L
                if (now - ts <= maxRetentionMs) fp to ts else null
            } else {
                null
            }
        }.toMutableList()

        // Remove any existing entry for this fingerprint
        validEntries.removeAll { it.first == codeFingerprint }

        // Bounded retention: if size >= 50, drop oldest entries (FIFO by timestamp)
        while (validEntries.size >= 50) {
            val oldest = validEntries.minByOrNull { it.second } ?: break
            validEntries.remove(oldest)
        }

        // Add fresh timestamped entry
        validEntries.add(codeFingerprint to now)

        val serialized = validEntries.map { "${it.first}:${it.second}" }.toSet()
        return prefs.edit()
            .putStringSet(KEY_CONSUMED_FINGERPRINTS, serialized)
            .commit()
    }

    override fun isFingerprintConsumed(codeFingerprint: String): Boolean {
        val now = System.currentTimeMillis()
        val rawSet = prefs.getStringSet(KEY_CONSUMED_FINGERPRINTS, emptySet()) ?: emptySet()
        for (entry in rawSet) {
            val parts = entry.split(":")
            if (parts.size == 2) {
                val fp = parts[0]
                val ts = parts[1].toLongOrNull() ?: 0L
                if (fp == codeFingerprint && (now - ts <= maxRetentionMs)) {
                    return true
                }
            } else if (entry == codeFingerprint) {
                // Fallback for legacy format
                return true
            }
        }
        return false
    }

    override fun clear(): Boolean {
        isExchangeInProgress = false
        return prefs.edit()
            .remove(KEY_STAGE)
            .remove(KEY_CODE_FINGERPRINT)
            .remove(KEY_TIMESTAMP)
            .commit()
    }

    companion object {
        private const val PREFS_NAME = "mobidesk_recovery_state"
        private const val KEY_STAGE = "recovery_stage"
        private const val KEY_CODE_FINGERPRINT = "code_fingerprint"
        private const val KEY_TIMESTAMP = "marker_timestamp"
        private const val KEY_CONSUMED_FINGERPRINTS = "consumed_fingerprints"

        fun sha256Fingerprint(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
