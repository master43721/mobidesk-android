package com.mobidesk.android

import android.content.SharedPreferences
import com.mobidesk.android.data.RecoveryStage
import com.mobidesk.android.data.SharedPreferencesRecoveryStateStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryStateStoreTest {

    private class InMemorySharedPreferences : SharedPreferences {
        val map = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = map.toMap()

        override fun getString(key: String?, defValue: String?): String? =
            (map[key] as? String) ?: defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? =
            (map[key] as? Set<String>) ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            (map[key] as? Int) ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            (map[key] as? Long) ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            (map[key] as? Float) ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            (map[key] as? Boolean) ?: defValue

        override fun contains(key: String?): Boolean = map.containsKey(key)

        override fun edit(): SharedPreferences.Editor = InMemoryEditor(this)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }

    private class InMemoryEditor(private val prefs: InMemorySharedPreferences) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private val toRemove = mutableSetOf<String>()

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) {
                if (value != null) {
                    temp[key] = value
                    toRemove.remove(key)
                } else {
                    toRemove.add(key)
                    temp.remove(key)
                }
            }
            return this
        }

        override fun putStringSet(key: String?, values: Set<String>?): SharedPreferences.Editor {
            if (key != null) {
                if (values != null) {
                    temp[key] = values.toSet()
                    toRemove.remove(key)
                } else {
                    toRemove.add(key)
                    temp.remove(key)
                }
            }
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) {
                temp[key] = value
                toRemove.remove(key)
            }
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) {
                temp[key] = value
                toRemove.remove(key)
            }
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) {
                temp[key] = value
                toRemove.remove(key)
            }
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) {
                temp[key] = value
                toRemove.remove(key)
            }
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) {
                toRemove.add(key)
                temp.remove(key)
            }
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            prefs.map.clear()
            temp.clear()
            toRemove.clear()
            return this
        }

        override fun apply() {
            commit()
        }

        override fun commit(): Boolean {
            toRemove.forEach { prefs.map.remove(it) }
            temp.forEach { (k, v) -> prefs.map[k] = v }
            toRemove.clear()
            temp.clear()
            return true
        }
    }

    @Test
    fun testSha256FingerprintDeterministic() {
        val fp1 = SharedPreferencesRecoveryStateStore.sha256Fingerprint("secret-recovery-code-123")
        val fp2 = SharedPreferencesRecoveryStateStore.sha256Fingerprint("secret-recovery-code-123")
        val fp3 = SharedPreferencesRecoveryStateStore.sha256Fingerprint("different-code")

        assertEquals(64, fp1.length)
        assertEquals(fp1, fp2)
        assertFalse(fp1 == fp3)
    }

    @Test
    fun testSetStageAndGetStage() {
        val prefs = InMemorySharedPreferences()
        val store = SharedPreferencesRecoveryStateStore(prefs)

        assertEquals(RecoveryStage.NONE, store.getStage())
        assertNull(store.getCodeFingerprint())

        val committed = store.setStage(RecoveryStage.CALLBACK_PENDING, "test-fingerprint")
        assertTrue(committed)
        assertEquals(RecoveryStage.CALLBACK_PENDING, store.getStage())
        assertEquals("test-fingerprint", store.getCodeFingerprint())
        assertTrue(store.getTimestamp() > 0)
    }

    @Test
    fun testStageExpiresAfterMaxRetention() {
        val prefs = InMemorySharedPreferences()
        // 100ms retention window
        val store = SharedPreferencesRecoveryStateStore(prefs, maxRetentionMs = 100L)

        store.setStage(RecoveryStage.CALLBACK_PENDING, "fp-1")
        assertEquals(RecoveryStage.CALLBACK_PENDING, store.getStage())

        // Simulate passage of time by mutating the stored timestamp back by 200ms
        prefs.map["marker_timestamp"] = System.currentTimeMillis() - 200L

        // Calling getStage after expiry should clear marker and return NONE
        assertEquals(RecoveryStage.NONE, store.getStage())
        assertNull(store.getCodeFingerprint())
    }

    @Test
    fun testFingerprintConsumedAndReplayPrevention() {
        val prefs = InMemorySharedPreferences()
        val store = SharedPreferencesRecoveryStateStore(prefs)

        assertFalse(store.isFingerprintConsumed("fp-consumed-1"))

        store.markFingerprintConsumed("fp-consumed-1")
        assertTrue(store.isFingerprintConsumed("fp-consumed-1"))
        assertFalse(store.isFingerprintConsumed("fp-consumed-2"))
    }

    @Test
    fun testConsumedFingerprintPruningWhenAtLimit() {
        val prefs = InMemorySharedPreferences()
        val store = SharedPreferencesRecoveryStateStore(prefs)

        // Add 50 fingerprints
        for (i in 1..50) {
            store.markFingerprintConsumed("fp-$i")
        }

        // Add 51st fingerprint
        store.markFingerprintConsumed("fp-51")

        // Should contain fp-51 and total size should not exceed 50
        assertTrue(store.isFingerprintConsumed("fp-51"))
        val rawSet = prefs.getStringSet("consumed_fingerprints", emptySet()) ?: emptySet()
        assertTrue(rawSet.size <= 50)
    }

    @Test
    fun testClearRemovesMarkerButKeepsConsumedFingerprints() {
        val prefs = InMemorySharedPreferences()
        val store = SharedPreferencesRecoveryStateStore(prefs)

        store.setStage(RecoveryStage.PASSWORD_UPDATE_REQUIRED, "fp-active")
        store.markFingerprintConsumed("fp-active")

        store.clear()

        assertEquals(RecoveryStage.NONE, store.getStage())
        assertNull(store.getCodeFingerprint())
        // Replay defense preserved: consumed fingerprint is still recognized
        assertTrue(store.isFingerprintConsumed("fp-active"))
    }
}

