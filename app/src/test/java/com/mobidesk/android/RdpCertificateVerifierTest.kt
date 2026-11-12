package com.mobidesk.android

import com.mobidesk.android.data.RdpCertificateVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

class RdpCertificateVerifierTest {

    private val validSha256 = "7f4a2c9e1d6b8035a7c4e2f90123456789abcdef0123456789abcdef01234567"

    @Test
    fun exactMatchingFingerprint_returnsAcceptTemporary() {
        val verifier = RdpCertificateVerifier(validSha256)
        val result = verifier.verify(validSha256)
        assertEquals(RdpCertificateVerifier.ACCEPT_TEMPORARY, result)
        assertEquals(2, result)
    }

    @Test
    fun matchingFingerprint_withColonsAndUpperCase_normalizesAndAccepts() {
        val verifier = RdpCertificateVerifier(validSha256)
        // Format with colons and uppercase like "7F:4A:2C:9E:..."
        val colonFp = validSha256.chunked(2).joinToString(":").uppercase()
        val result = verifier.verify(colonFp)
        assertEquals(RdpCertificateVerifier.ACCEPT_TEMPORARY, result)
    }

    @Test
    fun matchingFingerprint_withDashesAndSpaces_normalizesAndAccepts() {
        val verifier = RdpCertificateVerifier(validSha256)
        val spaceFp = validSha256.chunked(4).joinToString(" ").uppercase()
        val result = verifier.verify(spaceFp)
        assertEquals(RdpCertificateVerifier.ACCEPT_TEMPORARY, result)
    }

    @Test
    fun mismatchedFingerprint_failsClosedWithReject() {
        val verifier = RdpCertificateVerifier(validSha256)
        val wrongFp = "aaaaaaaa1d6b8035a7c4e2f90123456789abcdef0123456789abcdef01234567"
        val result = verifier.verify(wrongFp)
        assertEquals(RdpCertificateVerifier.REJECT, result)
        assertEquals(0, result)
    }

    @Test
    fun nullOrBlankFingerprint_failsClosed() {
        val verifier = RdpCertificateVerifier(validSha256)
        assertEquals(RdpCertificateVerifier.REJECT, verifier.verify(null))
        assertEquals(RdpCertificateVerifier.REJECT, verifier.verify(""))
        assertEquals(RdpCertificateVerifier.REJECT, verifier.verify("   "))
    }

    @Test
    fun invalidLengthFingerprint_failsClosed() {
        val verifier = RdpCertificateVerifier(validSha256)
        // Too short (e.g. SHA-1 40 hex chars instead of SHA-256 64 chars)
        assertEquals(RdpCertificateVerifier.REJECT, verifier.verify("7f4a2c9e1d6b8035a7c4e2f90123456789abcdef"))
        // Too long
        assertEquals(RdpCertificateVerifier.REJECT, verifier.verify(validSha256 + "00"))
    }

    @Test
    fun emptyExpectedFingerprint_failsClosed() {
        val verifier = RdpCertificateVerifier("")
        assertEquals(RdpCertificateVerifier.REJECT, verifier.verify(validSha256))
    }
}
