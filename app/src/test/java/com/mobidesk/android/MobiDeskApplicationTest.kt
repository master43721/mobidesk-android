package com.mobidesk.android

import org.junit.Assert.assertNotNull
import org.junit.Test

class MobiDeskApplicationTest {

    @Test
    fun sessionManagerInitializationReturnsValidInstance() {
        val app = MobiDeskApplication()
        val sessionManager = app.createEncryptedSessionManager()
        assertNotNull(sessionManager)
    }
}
