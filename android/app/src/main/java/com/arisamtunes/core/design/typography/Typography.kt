package com.arisamtunes.core.design.typography

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val BrandFont = FontFamily.SansSerif

private fun brandStyle(weight: FontWeight, size: Int, lineHeight: Int) = TextStyle(
    fontFamily = BrandFont,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

val AriSamTypography = Typography(
    displayLarge = brandStyle(FontWeight.Bold, 48, 54),
    displayMedium = brandStyle(FontWeight.Bold, 40, 46),
    headlineLarge = brandStyle(FontWeight.Bold, 32, 38),
    headlineMedium = brandStyle(FontWeight.SemiBold, 28, 34),
    titleLarge = brandStyle(FontWeight.SemiBold, 22, 28),
    titleMedium = brandStyle(FontWeight.SemiBold, 16, 22),
    bodyLarge = brandStyle(FontWeight.Normal, 16, 24),
    bodyMedium = brandStyle(FontWeight.Normal, 14, 20),
    labelLarge = brandStyle(FontWeight.SemiBold, 14, 20),
    labelMedium = brandStyle(FontWeight.Medium, 12, 16),
)

fun Typography.scaled(scale: Float): Typography {
    val safeScale = scale.coerceIn(0.85f, 1.35f)
    fun TextStyle.scale() = copy(fontSize = fontSize * safeScale, lineHeight = lineHeight * safeScale)
    return copy(
        displayLarge = displayLarge.scale(),
        displayMedium = displayMedium.scale(),
        headlineLarge = headlineLarge.scale(),
        headlineMedium = headlineMedium.scale(),
        titleLarge = titleLarge.scale(),
        titleMedium = titleMedium.scale(),
        bodyLarge = bodyLarge.scale(),
        bodyMedium = bodyMedium.scale(),
        labelLarge = labelLarge.scale(),
        labelMedium = labelMedium.scale(),
    )
}
