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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.sm, vertical = AriSamThemeTokens.spacing.xs),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(AriSamThemeTokens.spacing.sm),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = song.coverImageUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.medium),
                    )
                    Spacer(Modifier.width(AriSamThemeTokens.spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    IconButton(onClick = viewModel::togglePlayPause) {
                        Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, stringResource(if (state.isPlaying) R.string.pause else R.string.play))
                    }
                    IconButton(onClick = viewModel::close) { Icon(Icons.Rounded.Close, stringResource(R.string.close_player)) }
                }
                LinearProgressIndicator(
                    progress = { playerProgress(state.progressSeconds, song.durationSeconds) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
            Text(stringResource(R.string.now_playing), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (song == null) {
            EmptyPlayer()
        } else {
            val gradient = remember(song.id) { playerGradient(song.title, song.artistName) }
            Column(
                modifier = Modifier.fillMaxSize().background(gradient).padding(AriSamThemeTokens.spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                RotatingCover(song.coverImageUrl, song.title, state.isPlaying, Modifier.fillMaxWidth(.72f).aspectRatio(1f))
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(song.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(song.artistName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        song.album?.takeIf(String::isNotBlank) ?: stringResource(R.string.player_queue_count, state.queue.size.coerceAtLeast(1)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
                )
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.md, vertical = AriSamThemeTokens.spacing.sm)) {
                        val displayedProgress = if (state.crossfadeSong != null) state.crossfadeProgressSeconds else state.progressSeconds
                        Slider(
                            value = displayedProgress.coerceAtMost(song.durationSeconds.coerceAtLeast(1)).toFloat(),
                            onValueChange = { viewModel.seekTo(it.toInt()) },
                            valueRange = 0f..song.durationSeconds.coerceAtLeast(1).toFloat(),
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatDuration(displayedProgress), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatDuration(song.durationSeconds), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(AriSamThemeTokens.spacing.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            IconButton(onClick = viewModel::skipToPrevious, modifier = Modifier.size(58.dp)) {
                                Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.previous_track), modifier = Modifier.size(34.dp))
                            }
                            Surface(
                                modifier = Modifier.size(82.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ) {
                                IconButton(onClick = viewModel::togglePlayPause, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                                        modifier = Modifier.size(50.dp),
                                    )
                                }
                            }
                            IconButton(onClick = viewModel::skipToNext, modifier = Modifier.size(58.dp)) {
                                Icon(Icons.Rounded.SkipNext, stringResource(R.string.next_track), modifier = Modifier.size(34.dp))
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.AssistChip(
                        onClick = viewModel::cyclePlaybackSpeed,
                        label = { Text("${state.playbackSpeed}x") },
                        leadingIcon = { Icon(Icons.Rounded.Speed, null) },
                    )
                    androidx.compose.material3.AssistChip(
                        onClick = { viewModel.setSleepTimer(if (state.sleepTimerEndsAtMillis == null) 15 else 0) },
                        label = { Text(stringResource(if (state.sleepTimerEndsAtMillis == null) R.string.sleep_timer else R.string.sleep_timer_on)) },
                        leadingIcon = { Icon(Icons.Rounded.Timer, null) },
                    )
                    androidx.compose.material3.AssistChip(
                        onClick = viewModel::toggleCrossfade,
                        label = { Text(stringResource(if (state.isCrossfadeEnabled) R.string.crossfade_on else R.string.crossfade_off)) },
                        leadingIcon = { Icon(Icons.Rounded.SwapHoriz, null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioVisualizer(
    isPlaying: Boolean,
    bands: List<Float>,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = .74f),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.visualizer),
                modifier = Modifier.padding(horizontal = AriSamThemeTokens.spacing.md),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Canvas(
                modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = AriSamThemeTokens.spacing.md),
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
private fun RotatingCover(coverUrl: String, title: String, isPlaying: Boolean, modifier: Modifier = Modifier.size(260.dp)) {
    val transition = rememberInfiniteTransition(label = "coverRotation")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "coverRotationDegrees",
    )
    Box(
        modifier = modifier.clip(MaterialTheme.shapes.extraLarge).background(
            Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surface)),
        ),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = coverUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().padding(18.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .rotate(if (isPlaying) rotation else 0f),
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
