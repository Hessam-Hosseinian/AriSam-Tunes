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
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryVariant,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = AriSamPalette.violetInk,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkBorder,
    outlineVariant = DarkBorder.copy(alpha = .62f),
    error = ErrorDark,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    inverseSurface = LightSurfaceVariant,
    inverseOnSurface = LightTextPrimary,
    inversePrimary = LightPrimary,
    scrim = Color.Black,
)

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerHigh = LightBorder,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightBorder,
    outlineVariant = LightBorder.copy(alpha = .72f),
    error = ErrorLight,
    onError = Color.White,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkTextPrimary,
    inversePrimary = DarkPrimary,
    scrim = Color.Black,
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
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
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
    val onTehranAmber = TehranAmberInk
}
