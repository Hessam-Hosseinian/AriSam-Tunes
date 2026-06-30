package com.arisamtunes.feature.player

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
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
import com.arisamtunes.data.catalog.SongDto
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
        var showLyrics by remember { mutableStateOf(false) }
        var coverColors by remember {
            mutableStateOf(listOf(Color(0xFF8B5CF6), Color(0xFF4C1D95), Color(0xFF06060F)))
        }
        AnimatedPlayerBackground(colors = coverColors, coverUrl = song.coverImageUrl)
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            if (showLyrics) {
                LiveLyricsScreen(
                    song = song,
                    progressMillis = if (state.crossfadeSong != null) state.crossfadeProgressMillis else state.progressMillis,
                    isPlaying = state.isPlaying,
                    onBack = { showLyrics = false },
                    onPalette = { coverColors = it },
                )
                return@CompositionLocalProvider
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = .08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .1f)),
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), modifier = Modifier.size(20.dp))
                        }
                    }
                    Text(
                        stringResource(R.string.now_playing).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = .42f),
                    )
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = .08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .1f)),
                    ) {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                VinylHero(
                    coverUrl = song.coverImageUrl,
                    title = song.title,
                    isPlaying = state.isPlaying,
                    bands = state.visualizerBands,
                    onPalette = { coverColors = it },
                )
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOfNotNull(song.artistName, song.album?.takeIf(String::isNotBlank)).joinToString("  ·  "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = .55f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = {}) { Icon(Icons.Rounded.FavoriteBorder, null, tint = Color.White.copy(alpha = .5f)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        QualityBadge("HI-RES", Color(0xFFA78BFA))
                        QualityBadge("LOSSLESS", Color(0xFF34D399))
                        QualityBadge("DOLBY ATMOS", Color(0xFF60A5FA))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        PlayerTextAction(Icons.Rounded.Share, "Share")
                        PlayerTextAction(Icons.Rounded.MoreVert, "More")
                    }
                }
                state.playbackError?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                }
                AudioVisualizer(
                    isPlaying = state.isPlaying,
                    bands = state.visualizerBands,
                    compact = true,
                )
                val displayedProgress = if (state.crossfadeSong != null) state.crossfadeProgressSeconds else state.progressSeconds
                Column(Modifier.fillMaxWidth()) {
                    Slider(
                        value = displayedProgress.coerceAtMost(song.durationSeconds.coerceAtLeast(1)).toFloat(),
                        onValueChange = { viewModel.seekTo(it.toInt()) },
                        valueRange = 0f..song.durationSeconds.coerceAtLeast(1).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFFB45CFF),
                            inactiveTrackColor = Color.White.copy(alpha = .1f),
                        ),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatDuration(displayedProgress), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .38f))
                        Text("-${formatDuration((song.durationSeconds - displayedProgress).coerceAtLeast(0))}", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .38f))
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = {}) { Icon(Icons.Rounded.Shuffle, null, tint = Color.White.copy(alpha = .42f)) }
                    IconButton(onClick = viewModel::skipToPrevious, modifier = Modifier.size(60.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.previous_track), modifier = Modifier.size(38.dp))
                    }
                    Box(
                        Modifier.size(68.dp).clip(CircleShape).background(
                            Brush.linearGradient(listOf(Color(0xFFA855F7), Color(0xFFEC4899))),
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(onClick = viewModel::togglePlayPause, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                    IconButton(onClick = viewModel::skipToNext, modifier = Modifier.size(60.dp)) {
                        Icon(Icons.Rounded.SkipNext, stringResource(R.string.next_track), modifier = Modifier.size(38.dp))
                    }
                    IconButton(onClick = {}) { Icon(Icons.Rounded.Repeat, null, tint = Color.White.copy(alpha = .42f)) }
                }
                SecondaryPlayerControls(
                    onLyrics = { showLyrics = true },
                    onCrossfade = viewModel::toggleCrossfade,
                    onSpeed = viewModel::cyclePlaybackSpeed,
                    onSleep = { viewModel.setSleepTimer(if (state.sleepTimerEndsAtMillis == null) 15 else 0) },
                )
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
                val gap = 2.dp.toPx()
                val barWidth = ((size.width - gap * (bars - 1)) / bars).coerceAtLeast(2.dp.toPx())
                val usableHeight = size.height - 2.dp.toPx()
                animatedBands.forEachIndexed { index, normalized ->
                    val level = normalized.coerceIn(0f, 1f)
                    val barHeight = (usableHeight * level).coerceIn(5.dp.toPx(), usableHeight)
                    val x = index * (barWidth + gap)
                    val fraction = if (bars == 1) 0f else index.toFloat() / (bars - 1)
                    val baseColor = lerp(Color(0xFF8B5CF6), Color(0xFFEC4899), fraction)
                    val topColor = lerp(baseColor, Color.White, .22f)
                    val top = size.height - barHeight
                    if (level > .56f && isPlaying) {
                        drawLine(
                            color = baseColor.copy(alpha = .22f),
                            start = Offset(x + barWidth / 2f, top),
                            end = Offset(x + barWidth / 2f, size.height),
                            strokeWidth = barWidth * 2.3f,
                            cap = StrokeCap.Round,
                        )
                    }
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(topColor, baseColor),
                            startY = top,
                            endY = size.height,
                        ),
                        topLeft = Offset(x, top),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
                        alpha = if (isPlaying) .62f + level * .38f else .2f,
                    )
                }
            }
        }
    }
}

@Composable
private fun VinylHero(
    coverUrl: String,
    title: String,
    isPlaying: Boolean,
    bands: List<Float>,
    onPalette: (List<Color>) -> Unit = {},
) {
    val motion = rememberInfiniteTransition(label = "vinylMotion")
    val rotation by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20_000, easing = LinearEasing)),
        label = "vinylRotation",
    )
    val pulse by motion.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1_100), repeatMode = RepeatMode.Reverse),
        label = "vinylPulse",
    )
    Box(
        modifier = Modifier.size(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        listOf(236.dp, 214.dp, 192.dp).forEachIndexed { index, diameter ->
            Box(
                Modifier.size(diameter)
                    .graphicsLayer {
                        val amount = if (isPlaying) (pulse - 1f) * (1f - index * .18f) else 0f
                        scaleX = 1f + amount
                        scaleY = 1f + amount
                        alpha = .38f - index * .08f
                    }
                    .border(1.dp, Color(0xFFB57BFF), CircleShape),
            )
        }
        Canvas(Modifier.size(236.dp).rotate(rotation * .08f)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * .43f
            val maxBarLength = size.minDimension * .065f
            val count = 72
            repeat(count) { index ->
                val angle = (2f * PI.toFloat() * index / count) - PI.toFloat() / 2f
                val level = bands.getOrElse(index % bands.size.coerceAtLeast(1)) { .05f }.coerceIn(.04f, 1f)
                val length = 2.dp.toPx() + maxBarLength * level
                val direction = Offset(cos(angle), sin(angle))
                val start = center + direction * baseRadius
                val end = center + direction * (baseRadius + length)
                drawLine(
                    color = lerp(Color(0xFF8B5CF6), Color(0xFFEC4899), index.toFloat() / count)
                        .copy(alpha = if (isPlaying) .45f + level * .55f else .22f),
                    start = start,
                    end = end,
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
        AsyncImage(
            model = coverUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(196.dp).rotate(rotation)
                .clip(CircleShape)
                .border(2.dp, Color(0xFFB57BFF).copy(alpha = .7f), CircleShape),
            onSuccess = { success ->
                Palette.from(success.result.image.toPaletteBitmap())
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
        Box(
            Modifier.size(14.dp).clip(CircleShape).background(Color(0xFF090916))
                .border(2.dp, Color(0xFFB57BFF), CircleShape),
        )
    }
}

@Composable
private fun QualityBadge(label: String, color: Color) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = .1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = .3f)),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PlayerTextAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = Color.White.copy(alpha = .4f))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .4f))
    }
}

@Composable
private fun SecondaryPlayerControls(
    onLyrics: () -> Unit,
    onCrossfade: () -> Unit,
    onSpeed: () -> Unit,
    onSleep: () -> Unit,
) {
    val items = listOf(
        Triple(Icons.Rounded.Mic, "Lyrics", onLyrics),
        Triple(Icons.AutoMirrored.Rounded.PlaylistPlay, "Queue", {}),
        Triple(Icons.Rounded.SwapHoriz, "Crossfade", onCrossfade),
        Triple(Icons.Rounded.Download, "Save", {}),
        Triple(Icons.Rounded.Tune, "EQ", onSpeed),
        Triple(Icons.Rounded.Timer, "Sleep", onSleep),
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = Color.White.copy(alpha = .045f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .08f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { (icon, label, action) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = MaterialTheme.shapes.small,
                        color = Color.White.copy(alpha = .07f),
                    ) {
                        IconButton(onClick = action) {
                            Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = .6f))
                        }
                    }
                    Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .34f))
                }
            }
        }
    }
}

@Composable
private fun LiveLyricsScreen(
    song: SongDto,
    progressMillis: Long,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPalette: (List<Color>) -> Unit,
) {
    val lines = remember(song.id, song.lyrics, song.durationSeconds) {
        parseTimedLyrics(song)
    }
    val activeIndex = lines.indexOfLast { it.startMillis <= progressMillis }
    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = .08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .1f)),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), modifier = Modifier.size(20.dp))
                }
            }
            Text(
                stringResource(R.string.live_lyrics).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = .48f),
            )
            IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, null) }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = song.coverImageUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(CircleShape)
                    .border(1.dp, Color(0xFFB57BFF), CircleShape),
                onSuccess = { success ->
                    Palette.from(success.result.image.toPaletteBitmap()).maximumColorCount(10).generate { palette ->
                        palette ?: return@generate
                        val dominant = palette.getDominantColor(Color.Black.toArgb())
                        onPalette(
                            listOf(
                                lerp(Color(palette.vibrantSwatch?.rgb ?: dominant), Color.Black, .52f),
                                lerp(Color(dominant), Color.Black, .68f),
                                Color(0xFF06060F),
                            ),
                        )
                    }
                },
            )
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artistName, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .5f), maxLines = 1)
            }
            if (isPlaying) Icon(Icons.Rounded.GraphicEq, null, tint = Color(0xFFB57BFF))
        }
        if (lines.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.Mic, null, modifier = Modifier.size(42.dp), tint = Color.White.copy(alpha = .35f))
                    Text(stringResource(R.string.lyrics_unavailable), color = Color.White.copy(alpha = .48f))
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 140.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                itemsIndexed(lines, key = { index, line -> "${line.startMillis}-$index-${line.text}" }) { index, line ->
                    val distance = kotlin.math.abs(index - activeIndex)
                    Text(
                        line.text,
                        modifier = Modifier.fillMaxWidth().graphicsLayer {
                            alpha = when {
                                index == activeIndex -> 1f
                                distance == 1 -> .48f
                                else -> .24f
                            }
                            scaleX = if (index == activeIndex) 1f else .97f
                            scaleY = scaleX
                        },
                        style = if (index == activeIndex) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                        fontWeight = if (index == activeIndex) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (index == activeIndex) Color.White else Color.White.copy(alpha = .72f),
                    )
                }
            }
        }
    }
}

private data class TimedLyricLine(val startMillis: Long, val text: String)

private fun coil3.Image.toPaletteBitmap(): Bitmap {
    val bitmap = toBitmap()
    return if (bitmap.config == Bitmap.Config.HARDWARE) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        bitmap
    }
}

private val LrcLinePattern = Regex("""^\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]\s*(.+)$""")

private fun parseTimedLyrics(song: SongDto): List<TimedLyricLine> {
    val jsonLyrics = parseSyncedLyricsJson(
        song.extraMetadata["synced_lyrics_json"]?.jsonPrimitive?.contentOrNull,
    )
    if (jsonLyrics.isNotEmpty()) return jsonLyrics

    val syncedLrc = song.extraMetadata["synced_lyrics_lrc"]?.jsonPrimitive?.contentOrNull
    val rawLyrics = syncedLrc?.takeIf(String::isNotBlank) ?: song.lyrics.orEmpty()
    val cleanLines = rawLyrics.lineSequence()
        .map(String::trim)
        .filter { it.isNotBlank() && !it.startsWith("[offset:", ignoreCase = true) }
        .toList()
    if (cleanLines.isEmpty()) return emptyList()
    val timed = cleanLines.mapNotNull { line ->
        val match = LrcLinePattern.matchEntire(line) ?: return@mapNotNull null
        val minutes = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
        val seconds = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
        val fraction = match.groupValues[3].takeIf(String::isNotBlank)?.let { digits ->
            digits.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        } ?: 0L
        TimedLyricLine((minutes * 60L + seconds) * 1_000L + fraction, match.groupValues[4])
    }
    if (timed.size >= cleanLines.size / 2) return timed.sortedBy(TimedLyricLine::startMillis)

    val plainLines = cleanLines.map { LrcLinePattern.matchEntire(it)?.groupValues?.get(4) ?: it }
    val usableDuration = song.durationSeconds.coerceAtLeast(plainLines.size * 3) * 1_000L
    val weights = plainLines.map { it.length.coerceIn(8, 72) }
    val totalWeight = weights.sum().coerceAtLeast(1)
    var elapsedWeight = 0
    return plainLines.mapIndexed { index, text ->
        val start = ((elapsedWeight.toDouble() / totalWeight) * usableDuration).toLong()
        elapsedWeight += weights[index]
        TimedLyricLine(start, text)
    }
}

private fun parseSyncedLyricsJson(rawJson: String?): List<TimedLyricLine> {
    if (rawJson.isNullOrBlank()) return emptyList()
    return runCatching {
        Json.parseToJsonElement(rawJson).jsonArray.mapNotNull { element ->
            val item = element.jsonObject
            val time = item["time"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val text = item["text"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            TimedLyricLine((time * 1_000.0).toLong(), text)
        }.sortedBy(TimedLyricLine::startMillis)
    }.getOrDefault(emptyList())
}

@Composable
private fun AnimatedPlayerBackground(colors: List<Color>, coverUrl: String) {
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
        AsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = 1.28f
                scaleY = 1.28f
                alpha = .24f
            }.blur(72.dp),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(
                        colors.firstOrNull()?.copy(alpha = .78f) ?: Color.Transparent,
                        Color.Transparent,
                    ),
                    center = Offset(phase * 820f, -80f),
                    radius = 980f,
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(
                        colors.getOrElse(1) { Color.Transparent }.copy(alpha = .7f),
                        Color.Transparent,
                    ),
                    center = Offset((1f - phase) * 760f, 720f),
                    radius = 880f,
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xE804040C)),
                    startY = 760f,
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
