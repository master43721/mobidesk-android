package com.mobidesk.android.data

/**
 * Strict RDP Server Leaf Certificate SHA-256 Fingerprint Pinning Verifier (FP-10).
 *
 * Conforms to PRD v1.2 §6.7, §9, and API Contract v1.1 (§5).
 * FreeRDP Return Status:
 * - 2: Accept certificate temporarily for this session (VERIFY_CERT_TEMPORARY)
 * - 0: Reject certificate and abort connection (fail-closed, maps to RDP-CERT-01)
 */
class RdpCertificateVerifier(
    private val expectedFingerprintSha256: String
) {

    /**
     * Verifies the certificate fingerprint presented during the FreeRDP TLS handshake.
     *
     * @param presentedFingerprint The SHA-256 fingerprint presented by the RDP server (hex or colon-separated).
     * @return 2 if valid match, 0 if mismatched, missing, or empty.
     */
    fun verify(presentedFingerprint: String?): Int {
        if (presentedFingerprint.isNullOrBlank() || expectedFingerprintSha256.isBlank()) {
            return REJECT
        }

        val cleanPresented = normalizeFingerprint(presentedFingerprint)
        val cleanExpected = normalizeFingerprint(expectedFingerprintSha256)

        // Exact 64-character SHA-256 hex string check
        if (cleanPresented.length != 64 || cleanExpected.length != 64) {
            return REJECT
        }

        return if (cleanPresented.equals(cleanExpected, ignoreCase = true)) {
            ACCEPT_TEMPORARY
        } else {
            REJECT
        }
    }

    private fun normalizeFingerprint(fp: String): String {
        val sb = StringBuilder(fp.length)
        for (ch in fp) {
            if (ch != ':' && ch != ' ' && ch != '-') {
                sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString()
    }

    companion object {
        const val REJECT = 0
        const val ACCEPT_TEMPORARY = 2
    }
}
