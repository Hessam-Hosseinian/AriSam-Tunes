package com.arisamtunes.core.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.arisamtunes.core.design.theme.AriSamThemeTokens

private const val PressedScale = 0.97f
private const val GlassAlpha = 1f

@Composable
fun PressScaleBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) PressedScale else 1f, label = "pressScale")
    Box(
        modifier = modifier
            .scale(scale)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick),
        content = content,
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(AriSamThemeTokens.spacing.lg),
    content: @Composable BoxScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = GlassAlpha),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Box(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -240f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1_150)),
        label = "shimmerOffset",
    )
    val colors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .38f),
        MaterialTheme.colorScheme.primary.copy(alpha = .18f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .38f),
    )
    Box(
        modifier.background(
            Brush.linearGradient(
                colors = colors,
                start = Offset(offset, offset),
                end = Offset(offset + 240f, offset + 240f),
            ),
            shape = MaterialTheme.shapes.large,
        ),
    )
}
