package com.arisamtunes.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil3.toBitmap
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.GlassCard
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.theme.AriSamThemeTokens

@Composable
fun MiniPlayer(
    onOpen: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val song = state.currentSong ?: return
    PressScaleBox(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(horizontal = AriSamThemeTokens.spacing.sm, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = song.coverImageUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small),
                    )
                    Spacer(Modifier.width(AriSamThemeTokens.spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    IconButton(onClick = viewModel::togglePlayPause) {
                        Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, stringResource(if (state.isPlaying) R.string.pause else R.string.play))
                    }
                    IconButton(onClick = viewModel::skipToNext) {
                        Icon(Icons.Rounded.SkipNext, stringResource(R.string.next_track))
                    }
                }
                LinearProgressIndicator(
                    progress = { playerProgress(state.progressSeconds, song.durationSeconds) },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                )
            }
        }
    }
}

@Composable
fun NowPlayingRoute(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val song = state.crossfadeSong ?: state.currentSong
    if (song == null) {
        EmptyPlayer()
    } else {
        var coverColors by remember(song.id) {
            mutableStateOf(listOf(Color(0xFF151515), Color(0xFF07191D), Color.Black))
        }
        AnimatedPlayerBackground(colors = coverColors)
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.back), modifier = Modifier.size(34.dp))
                    }
                    Row {
                        IconButton(onClick = viewModel::toggleCrossfade) {
                            Icon(Icons.Rounded.SwapHoriz, stringResource(if (state.isCrossfadeEnabled) R.string.crossfade_on else R.string.crossfade_off))
                        }
                        IconButton(onClick = viewModel::cyclePlaybackSpeed) {
                            Icon(Icons.Rounded.VolumeUp, null)
                        }
                        IconButton(onClick = {}) { Icon(Icons.Rounded.GraphicEq, null) }
                        IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, null) }
                    }
                }
                AlbumCover(
                    coverUrl = song.coverImageUrl,
                    title = song.title,
                    modifier = Modifier.fillMaxWidth(.92f).aspectRatio(1f),
                    onPalette = { coverColors = it },
                )
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(song.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artistName, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = .76f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                state.playbackError?.let { error ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(AriSamThemeTokens.spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.sm),
                        ) {
                            Text(error, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            androidx.compose.material3.TextButton(onClick = viewModel::retryPlayback) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
                AudioVisualizer(
                    isPlaying = state.isPlaying,
                    bands = state.visualizerBands,
                    compact = true,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, null, modifier = Modifier.size(30.dp)) }
                    IconButton(onClick = {}) { Icon(Icons.Rounded.FavoriteBorder, null, modifier = Modifier.size(30.dp)) }
                    IconButton(onClick = {}) { Icon(Icons.Rounded.Add, null, modifier = Modifier.size(32.dp)) }
                }
                val displayedProgress = if (state.crossfadeSong != null) state.crossfadeProgressSeconds else state.progressSeconds
                Column(Modifier.fillMaxWidth()) {
                    Slider(
                        value = displayedProgress.coerceAtMost(song.durationSeconds.coerceAtLeast(1)).toFloat(),
                        onValueChange = { viewModel.seekTo(it.toInt()) },
                        valueRange = 0f..song.durationSeconds.coerceAtLeast(1).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = .32f),
                        ),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatDuration(displayedProgress), style = MaterialTheme.typography.labelMedium)
                        Text(formatDuration(song.durationSeconds), style = MaterialTheme.typography.labelMedium)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = {}) { Icon(Icons.Rounded.Shuffle, null) }
                    IconButton(onClick = viewModel::skipToPrevious, modifier = Modifier.size(60.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.previous_track), modifier = Modifier.size(38.dp))
                    }
                    IconButton(onClick = viewModel::togglePlayPause, modifier = Modifier.size(64.dp)) {
                        Icon(
                            if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                            modifier = Modifier.size(46.dp),
                        )
                    }
                    IconButton(onClick = viewModel::skipToNext, modifier = Modifier.size(60.dp)) {
                        Icon(Icons.Rounded.SkipNext, stringResource(R.string.next_track), modifier = Modifier.size(38.dp))
                    }
                    IconButton(onClick = {}) { Icon(Icons.Rounded.Repeat, null) }
                }
            }
        }
    }
}

@Composable
private fun AudioVisualizer(
    isPlaying: Boolean,
    bands: List<Float>,
    compact: Boolean = false,
) {
    val primary = if (compact) Color.White else MaterialTheme.colorScheme.primary
    val secondary = if (compact) Color.White.copy(alpha = .58f) else MaterialTheme.colorScheme.secondary
    val animatedBands = bands.mapIndexed { index, level ->
        val value by animateFloatAsState(
            targetValue = if (isPlaying) level else level * 0.55f,
            animationSpec = tween(durationMillis = 42 + (index % 3) * 8, easing = LinearEasing),
            label = "visualizerBand$index",
        )
        value
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (compact) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = .74f),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (!compact) {
                Text(
                    stringResource(R.string.visualizer),
                    modifier = Modifier.padding(horizontal = AriSamThemeTokens.spacing.md),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Canvas(
                modifier = Modifier.fillMaxWidth().height(if (compact) 38.dp else 58.dp)
                    .padding(horizontal = AriSamThemeTokens.spacing.md),
            ) {
                val bars = animatedBands.size.coerceAtLeast(1)
                val gap = size.width / (bars * 2.1f)
                val barWidth = gap.coerceAtLeast(5f)
                val usableHeight = size.height * .9f
                animatedBands.forEachIndexed { index, normalized ->
                    val barHeight = (usableHeight * normalized).coerceIn(size.height * .16f, usableHeight)
                    val x = index * (barWidth + gap)
                    drawLine(
                        color = if (index % 2 == 0) primary else secondary,
                        start = androidx.compose.ui.geometry.Offset(x, (size.height - barHeight) / 2f),
                        end = androidx.compose.ui.geometry.Offset(x, (size.height + barHeight) / 2f),
                        strokeWidth = barWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumCover(
    coverUrl: String,
    title: String,
    modifier: Modifier = Modifier.size(260.dp),
    onPalette: (List<Color>) -> Unit = {},
) {
    Box(
        modifier = modifier.clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = coverUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onSuccess = { success ->
                Palette.from(success.result.image.toBitmap())
                    .maximumColorCount(12)
                    .resizeBitmapArea(12_000)
                    .generate { palette ->
                        palette ?: return@generate
                        val dominant = palette.getDominantColor(Color.Black.toArgb())
                        val vibrant = palette.darkVibrantSwatch?.rgb
                            ?: palette.vibrantSwatch?.rgb
                            ?: dominant
                        val muted = palette.darkMutedSwatch?.rgb
                            ?: palette.mutedSwatch?.rgb
                            ?: dominant
                        onPalette(
                            listOf(
                                lerp(Color(vibrant), Color.Black, .58f),
                                lerp(Color(dominant), Color.Black, .70f),
                                lerp(Color(muted), Color.Black, .78f),
                                Color.Black,
                            ),
                        )
                    }
            },
        )
    }
}

@Composable
private fun AnimatedPlayerBackground(colors: List<Color>) {
    val motion = rememberInfiniteTransition(label = "coverGradient")
    val phase by motion.animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(8_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "coverGradientPhase",
    )
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Black,
                        .38f to Color.Black,
                        .64f to colors.getOrElse(1) { Color.Black }.copy(alpha = .82f),
                        1f to colors.getOrElse(2) { Color.Black },
                    ),
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(
                        colors.firstOrNull()?.copy(alpha = .52f) ?: Color.Transparent,
                        Color.Transparent,
                    ),
                    center = Offset(phase * 1_100f, 1_520f),
                    radius = 920f,
                ),
            ),
        )
    }
}

@Composable
private fun EmptyPlayer() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.no_song_playing), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun playerProgress(position: Int, duration: Int): Float = if (duration <= 0) 0f else (position.toFloat() / duration).coerceIn(0f, 1f)

private fun formatDuration(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val remainingSeconds = safeSeconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}

private fun playerGradient(title: String, artist: String): Brush {
    val palettes = listOf(
        listOf(Color(0xFF321450), Color(0xFF7B2CBF), Color(0xFF10051F)),
        listOf(Color(0xFF09203F), Color(0xFF537895), Color(0xFF050B18)),
        listOf(Color(0xFF3D0C11), Color(0xFFD00000), Color(0xFF160305)),
        listOf(Color(0xFF082032), Color(0xFF2C394B), Color(0xFF000814)),
        listOf(Color(0xFF1B4332), Color(0xFF40916C), Color(0xFF081C15)),
    )
    val colors = palettes[Math.floorMod((title + artist).hashCode(), palettes.size)]
    return Brush.verticalGradient(colors)
}
