package com.mobidesk.android

import com.mobidesk.android.data.WindowsGuestCredentials
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowsGuestCredentialsTest {

    @Test
    fun validCredentials_reportsValidAndNotDestroyed() {
        val creds = WindowsGuestCredentials(
            username = "student_lab".toCharArray(),
            domain = "LABDOM".toCharArray(),
            password = "SecretPassword123!".toCharArray()
        )

        assertTrue(creds.isValid)
        assertFalse(creds.isDestroyed)
        assertEquals("student_lab", String(creds.username))
        assertEquals("LABDOM", String(creds.domain!!))
        assertEquals("SecretPassword123!", String(creds.password))
    }

    @Test
    fun clear_zeroesOutAllMemoryAndMarksDestroyed() {
        val userArray = "student_lab".toCharArray()
        val domArray = "LABDOM".toCharArray()
        val passArray = "SecretPassword123!".toCharArray()

        val creds = WindowsGuestCredentials(
            username = userArray,
            domain = domArray,
            password = passArray
        )

        creds.clear()

        assertTrue(creds.isDestroyed)
        assertFalse(creds.isValid)

        // Verify array contents are completely zeroed with null chars
        val expectedUserZeros = CharArray("student_lab".length) { '\u0000' }
        val expectedDomZeros = CharArray("LABDOM".length) { '\u0000' }
        val expectedPassZeros = CharArray("SecretPassword123!".length) { '\u0000' }

        assertArrayEquals(expectedUserZeros, userArray)
        assertArrayEquals(expectedDomZeros, domArray)
        assertArrayEquals(expectedPassZeros, passArray)
    }

    @Test
    fun useBlock_automaticallyClearsMemoryOnExit() {
        val passArray = "MyPassword".toCharArray()
        WindowsGuestCredentials(
            username = "user".toCharArray(),
            password = passArray
        ).use { creds ->
            assertTrue(creds.isValid)
        }

        val expectedZeros = CharArray("MyPassword".length) { '\u0000' }
        assertArrayEquals(expectedZeros, passArray)
    }

    @Test
    fun toString_doesNotLeakCredentials() {
        val creds = WindowsGuestCredentials(
            username = "student_super_secret".toCharArray(),
            password = "SensitivePassword99".toCharArray()
        )

        val str = creds.toString()
        assertFalse(str.contains("student_super_secret"))
        assertFalse(str.contains("SensitivePassword99"))
        assertEquals("WindowsGuestCredentials(cleared=false)", str)

        creds.clear()
        assertEquals("WindowsGuestCredentials(cleared=true)", creds.toString())
    }

    @Test
    fun emptyCredentials_reportsInvalid() {
        val emptyUser = WindowsGuestCredentials(
            username = "".toCharArray(),
            password = "pass".toCharArray()
        )
        assertFalse(emptyUser.isValid)

        val emptyPass = WindowsGuestCredentials(
            username = "user".toCharArray(),
            password = "".toCharArray()
        )
        assertFalse(emptyPass.isValid)
    }
}
