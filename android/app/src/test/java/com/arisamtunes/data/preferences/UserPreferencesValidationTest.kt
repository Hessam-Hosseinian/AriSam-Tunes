package com.arisamtunes.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class UserPreferencesValidationTest {
    @Test
    fun missingOrNonFiniteFontScale_usesDefault() {
        assertEquals(1f, sanitizeFontScale(null))
        assertEquals(1f, sanitizeFontScale(Float.NaN))
        assertEquals(1f, sanitizeFontScale(Float.POSITIVE_INFINITY))
    }

    @Test
    fun fontScale_isClampedToSupportedRange() {
        assertEquals(MinFontScale, sanitizeFontScale(0.2f))
        assertEquals(1.1f, sanitizeFontScale(1.1f))
        assertEquals(MaxFontScale, sanitizeFontScale(2f))
    }
}
