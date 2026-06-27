package com.arisamtunes.core.design.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.arisamtunes.core.design.colors.*
import com.arisamtunes.core.design.shape.AriSamShapes
import com.arisamtunes.core.design.spacing.AriSamSpacing
import com.arisamtunes.core.design.spacing.LocalAriSamSpacing
import com.arisamtunes.core.design.typography.AriSamTypography
import com.arisamtunes.core.design.typography.scaled

private val DarkColors = darkColorScheme(
    primary = VioletLight,
    onPrimary = Midnight,
    secondary = Cyan,
    tertiary = Rose,
    background = Midnight,
    onBackground = OnDark,
    surface = DeepNavy,
    onSurface = OnDark,
    surfaceVariant = GlassNavy,
    onSurfaceVariant = OnDarkMuted,
    outline = DarkOutline,
    error = ErrorDark,
)

private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    secondary = Cyan,
    tertiary = Rose,
    background = Cloud,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8E0F8),
    onSurfaceVariant = OnLightMuted,
    outline = LightOutline,
    error = ErrorLight,
)

@Composable
fun AriSamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }

    CompositionLocalProvider(LocalAriSamSpacing provides AriSamSpacing()) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AriSamTypography.scaled(fontScale),
            shapes = AriSamShapes,
            content = content,
        )
    }
}

object AriSamThemeTokens {
    val spacing: AriSamSpacing
        @Composable @ReadOnlyComposable get() = LocalAriSamSpacing.current

    val tehranAmber = TehranAmber
}
