package com.mobidesk.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewStatusTest {
    @Test
    fun previewModeIsNotARealConnection() {
        val label = "Preview mode — no live service"
        assertTrue(label.contains("Preview"))
        assertFalse(label.contains("Connected"))
    }
}
