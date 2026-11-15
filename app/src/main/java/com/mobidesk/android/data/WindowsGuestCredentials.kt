package com.mobidesk.android.data

import java.util.Arrays

/**
 * Windows Guest Credentials container for Network Level Authentication (NLA) - FP-05.
 *
 * Security Invariants:
 * 1. Credentials are stored in mutable char arrays ([CharArray]) rather than immutable Strings
 *    to prevent residual retention in JVM heap / garbage collector memory.
 * 2. [clear] securely zeroes out the character arrays with '\u0000'.
 * 3. Never serialized, never logged, never transmitted to the session broker or Supabase.
 */
class WindowsGuestCredentials(
    val username: CharArray,
    val domain: CharArray? = null,
    val password: CharArray
) : AutoCloseable {

    @Volatile
    private var isCleared = false

    val isDestroyed: Boolean
        get() = isCleared

    val isValid: Boolean
        get() = !isCleared && username.isNotEmpty() && password.isNotEmpty()

    /**
     * Overwrites all credential memory with zeros and marks credentials as destroyed.
     */
    fun clear() {
        if (isCleared) return
        synchronized(this) {
            if (isCleared) return
            Arrays.fill(username, '\u0000')
            domain?.let { Arrays.fill(it, '\u0000') }
            Arrays.fill(password, '\u0000')
            isCleared = true
        }
    }

    override fun close() {
        clear()
    }

    override fun toString(): String {
        return "WindowsGuestCredentials(cleared=$isCleared)"
    }
}
