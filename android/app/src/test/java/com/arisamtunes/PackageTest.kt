package com.arisamtunes

import org.junit.Assert.assertEquals
import org.junit.Test

class PackageTest {
    @Test
    fun `application package is stable`() {
        assertEquals("com.arisamtunes", BuildConfig.APPLICATION_ID)
    }
}
