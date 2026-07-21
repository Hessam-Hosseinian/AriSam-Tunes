@file:Suppress("PreviewAnnotationInFunctionWithParameters")

package com.arisamtunes.feature.player

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.Canvas
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil3.toBitmap
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.GlassCard
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.preview.PreviewCatalogData
import com.arisamtunes.core.design.theme.AriSamTheme
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.local.LocalLibraryRepository
import com.arisamtunes.feature.downloads.DownloadEnqueueResult
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


@Composable
fun MiniPlayer(
    onOpen: () -> Unit,
    coverModifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val song = state.currentSong ?: return
    val shape = RoundedCornerShape(24.dp)
    val motion = rememberInfiniteTransition(label = "mini-player-motion")
    val artworkGlow by motion.animateFloat(
        initialValue = .28f,
        targetValue = .72f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mini-player-artwork-glow",
    )
    val progress by animateFloatAsState(
        targetValue = playerProgress(state.progressSeconds, song.durationSeconds),
        animationSpec = tween(420, easing = LinearEasing),
        label = "mini-player-progress",
    )
    PressScaleBox(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(14.dp, shape, ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = .2f))
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = .62f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = .48f),
                        ),
                    ),
                    shape = shape,
                ),
            shape = shape,
            color = Color.Transparent,
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = .42f),
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.surface,
                            ),
                        ),
                    )
                    .animateContentSize(),
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, top = 9.dp, end = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = artworkGlow * .28f),
                                RoundedCornerShape(19.dp),
                            )
                            .padding(3.dp),
                    ) {
                        AsyncImage(
                            model = song.coverImageUrl,
                            contentDescription = song.title,
                            error = painterResource(R.drawable.arisam_app_icon_dark),
                            contentScale = ContentScale.Crop,
                            modifier = coverModifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).size(20.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shadowElevation = 3.dp,
                        ) {
                            MiniPlayerEqualizer(
                                isPlaying = state.isPlaying,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    AnimatedContent(
                        targetState = song,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically(tween(280)) { it / 3 }) togetherWith
                                (fadeOut(tween(140)) + slideOutVertically(tween(220)) { -it / 3 })
                        },
                        contentKey = SongDto::id,
                        modifier = Modifier.weight(1f),
                        label = "mini-player-song",
                    ) { animatedSong ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MiniPlayerEqualizer(isPlaying = state.isPlaying, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = stringResource(if (state.isPlaying) R.string.now_playing else R.string.pause),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text(
                                animatedSong.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                animatedSong.artistName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = viewModel::skipToNext, modifier = Modifier.size(38.dp)) {
                        Icon(
                            playerSkipIcon(isNext = true),
                            stringResource(R.string.next_track),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shadowElevation = 5.dp,
                    ) {
                        IconButton(onClick = viewModel::togglePlayPause, modifier = Modifier.size(46.dp)) {
                            AnimatedContent(
                                targetState = state.isPlaying,
                                transitionSpec = { fadeIn(spring()) togetherWith fadeOut(tween(100)) },
                                label = "mini-player-play-pause",
                            ) { isPlaying ->
                                Icon(
                                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    stringResource(if (isPlaying) R.string.pause else R.string.play),
                                )
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                                ),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val transition = rememberInfiniteTransition(label = "mini-player-equalizer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(780, easing = LinearEasing)),
        label = "mini-player-equalizer-phase",
    )
    val activity by animateFloatAsState(
        targetValue = if (isPlaying) 1f else .16f,
        animationSpec = tween(260),
        label = "mini-player-equalizer-activity",
    )
    Canvas(modifier) {
        val barWidth = size.width / 5f
        val gap = barWidth
        repeat(3) { index ->
            val wave = (kotlin.math.sin((phase * 2f * Math.PI + index * 1.7)).toFloat() + 1f) / 2f
            val barHeight = size.height * (.22f + activity * (.28f + wave * .5f))
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (barWidth + gap), (size.height - barHeight) / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth, barWidth),
            )
        }
    }
}

@Composable
fun ChatMiniPlayer(
    onOpen: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    MiniPlayer(onOpen = onOpen, viewModel = viewModel)
}

@Composable
fun NowPlayingRoute(
    onBack: () -> Unit,
    onShowSongInfo: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onShareSong: (String) -> Unit,
    onOpenRhythmGame: () -> Unit,
    coverModifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val song = state.crossfadeSong ?: state.currentSong
    if (song == null) {
        EmptyPlayer()
    } else {
        var showLyrics by remember { mutableStateOf(false) }
        var showQueue by remember { mutableStateOf(false) }
        var showSpeedPicker by remember { mutableStateOf(false) }
        var showSleepPicker by remember { mutableStateOf(false) }
        BackHandler(enabled = showLyrics || showQueue) {
            when {
                showQueue -> showQueue = false
                showLyrics -> showLyrics = false
            }
        }
        val context = LocalContext.current
        val resources = LocalResources.current
        val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
        LaunchedEffect(viewModel, resources) {
            viewModel.downloadResults.collect { result ->
                val message = when (result) {
                    DownloadEnqueueResult.Queued -> R.string.download_queued
                    DownloadEnqueueResult.AlreadyQueued -> R.string.download_already_queued
                    DownloadEnqueueResult.AlreadyDownloaded -> R.string.download_already_saved
                    DownloadEnqueueResult.PremiumRequired -> R.string.download_premium_required
                }
                Toast.makeText(context, resources.getString(message), Toast.LENGTH_SHORT).show()
            }
        }
        val isLiked by remember(song.id) { viewModel.observeIsLiked(song.id) }
            .collectAsStateWithLifecycle(initialValue = false)
        val download by remember(song.id) { viewModel.observeDownload(song.id) }
            .collectAsStateWithLifecycle(initialValue = null)
        val isSaved = download?.let {
            it.downloadState == LocalLibraryRepository.DownloadStateCompleted && File(it.localFilePath).isFile
        } == true
        val isDownloading = download?.downloadState == LocalLibraryRepository.DownloadStateQueued ||
            download?.downloadState == LocalLibraryRepository.DownloadStateRunning
        val playerContentColor = MaterialTheme.colorScheme.onBackground
        var coverColors by remember {
            mutableStateOf(listOf(Color(0xFF8B5CF6), Color(0xFF4C1D95), Color(0xFF06060F)))
        }
        AnimatedPlayerBackground(colors = coverColors, coverUrl = song.coverImageUrl)
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
          CompositionLocalProvider(LocalContentColor provides playerContentColor) {
            if (showQueue) {
                PlayerQueueScreen(
                    currentSong = song,
                    queue = state.queue,
                    onBack = { showQueue = false },
                    onSongClick = { selected ->
                        viewModel.play(selected, state.queue)
                        showQueue = false
                    },
                )
                return@CompositionLocalProvider
            }
            if (showLyrics) {
                LiveLyricsScreen(
                    song = song,
                    progressMillis = if (state.crossfadeSong != null) state.crossfadeProgressMillis else state.progressMillis,
                    isPlaying = state.isPlaying,
                    onBack = { showLyrics = false },
                    onPalette = { coverColors = it },
                    onSeek = viewModel::seekToMillis,
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
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .82f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), modifier = Modifier.size(20.dp))
                        }
                    }
                    Text(
                        stringResource(R.string.now_playing).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .82f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        IconButton(onClick = { showQueue = true }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.PlaylistPlay,
                                stringResource(R.string.queue),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PremiumAlbumCover(
                        coverUrl = song.coverImageUrl,
                        title = song.title,
                        isPlaying = state.isPlaying,
                        onPalette = { coverColors = it },
                        coverModifier = coverModifier,
                    )
                    PlayerOrbitAction(
                        icon = Icons.Rounded.Mic,
                        label = stringResource(R.string.lyrics),
                        selected = false,
                        onClick = { showLyrics = true },
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                    PlayerOrbitAction(
                        icon = if (isSaved) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                        label = stringResource(
                            when {
                                isSaved -> R.string.saved
                                isDownloading -> R.string.downloading
                                isPremium -> R.string.download
                                else -> R.string.premium_required_title
                            },
                        ),
                        selected = isSaved || isDownloading,
                        onClick = {
                            if (!isSaved) {
                                viewModel.download(song)
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                song.artistName,
                                modifier = Modifier.clickable { onArtistClick(song.artistName) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            song.album?.takeIf(String::isNotBlank)?.let { album ->
                                Text(album, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        IconButton(onClick = { viewModel.toggleLike(song) }) {
                            AnimatedContent(
                                targetState = isLiked,
                                transitionSpec = { fadeIn(tween(180)) + scaleIn(initialScale = .65f) togetherWith fadeOut(tween(100)) },
                                label = "player-like",
                            ) { liked ->
                                Icon(
                                    if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    null,
                                    tint = if (liked) Color(0xFFF43F5E) else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    // Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    //     QualityBadge("HI-RES", Color(0xFFA78BFA))
                    //     QualityBadge("LOSSLESS", Color(0xFF34D399))
                    //     QualityBadge("DOLBY ATMOS", Color(0xFF60A5FA))
                    // }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        PlayerTextAction(Icons.Rounded.SportsEsports, stringResource(R.string.rhythm_game)) { onOpenRhythmGame() }
                        PlayerTextAction(Icons.Rounded.Share, stringResource(R.string.chat_share_song)) { onShareSong(song.id) }
                        PlayerTextAction(Icons.Rounded.Info, stringResource(R.string.song_information)) {
                            onShowSongInfo(song.id)
                        }
                    }
                }
                state.playbackError?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                }
                // visualizer در یک ردیف جداگانه و crossfade زیر آن
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Visualizer در بالای ردیف
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.fillMaxWidth()) {
                            AudioVisualizer(
                                isPlaying = state.isPlaying,
                                bands = state.visualizerBands,
                                compact = true,
                            )
                        }
                    }

                    // دکمه Crossfade در زیر visualizer با تراز راست
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        PlayerUtilityChip(
                            icon = Icons.Rounded.SwapHoriz,
                            label = stringResource(R.string.crossfade),
                            selected = state.isCrossfadeEnabled,
                            onClick = viewModel::toggleCrossfade,
                        )
                    }
                }
                val displayedProgress = if (state.crossfadeSong != null) state.crossfadeProgressSeconds else state.progressSeconds
                Column(Modifier.fillMaxWidth()) {
                    PlayerSeekBar(displayedProgress, song.durationSeconds, viewModel::seekTo)
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(formatDuration(displayedProgress), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PlayerUtilityChip(
                                icon = Icons.Rounded.Speed,
                                label = stringResource(R.string.playback_speed_value, formatSpeed(state.playbackSpeed)),
                                selected = state.playbackSpeed != 1f,
                                onClick = { showSpeedPicker = true },
                            )
                            PlayerUtilityChip(
                                icon = Icons.Rounded.Timer,
                                label = stringResource(R.string.sleep),
                                selected = state.sleepTimerEndsAtMillis != null,
                                onClick = { showSleepPicker = true },
                            )
                        }
                        Text("-${formatDuration((song.durationSeconds - displayedProgress).coerceAtLeast(0))}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    PlaybackModeButton(
                        icon = Icons.Rounded.Shuffle,
                        selected = state.isShuffleEnabled,
                        contentDescription = "Shuffle",
                        onClick = viewModel::toggleShuffle,
                    )
                    IconButton(onClick = viewModel::skipToPrevious, modifier = Modifier.size(60.dp)) {
                        Icon(playerSkipIcon(isNext = false), stringResource(R.string.previous_track), modifier = Modifier.size(38.dp))
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
                        Icon(playerSkipIcon(isNext = true), stringResource(R.string.next_track), modifier = Modifier.size(38.dp))
                    }
                    PlaybackModeButton(
                        icon = if (state.repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        selected = state.repeatMode > 0,
                        contentDescription = "Repeat",
                        onClick = viewModel::cycleRepeatMode,
                    )
                }
            }
          }
        }
        if (showSpeedPicker) {
            PlaybackSpeedSheet(
                selectedSpeed = state.playbackSpeed,
                onSelect = {
                    viewModel.setPlaybackSpeed(it)
                    showSpeedPicker = false
                },
                onDismiss = { showSpeedPicker = false },
            )
        }
        if (showSleepPicker) {
            SleepTimerSheet(
                endsAtMillis = state.sleepTimerEndsAtMillis,
                onSelectMinutes = {
                    viewModel.setSleepTimer(it)
                    showSleepPicker = false
                },
                onDismiss = { showSleepPicker = false },
            )
        }
    }
}

@Composable
private fun PlayerSeekBar(
    progressSeconds: Int,
    durationSeconds: Int,
    onSeek: (Int) -> Unit,
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val thumbColor = MaterialTheme.colorScheme.onSurface
    val duration = durationSeconds.coerceAtLeast(1)
    val fraction = (progressSeconds.toFloat() / duration).coerceIn(0f, 1f)
    Canvas(
        modifier = Modifier.fillMaxWidth().height(28.dp).pointerInput(duration) {
            awaitEachGesture {
                val down = awaitFirstDown()
                fun seekTo(x: Float) {
                    onSeek(((x / size.width.coerceAtLeast(1)) * duration).toInt().coerceIn(0, duration))
                }
                seekTo(down.position.x)
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.first()
                    seekTo(change.position.x)
                    change.consume()
                } while (event.changes.any { it.pressed })
            }
        },
    ) {
        val centerY = size.height / 2f
        val padding = 9.dp.toPx()
        val startX = padding
        val endX = size.width - padding
        val thumbX = startX + (endX - startX) * fraction
        drawLine(trackColor, Offset(startX, centerY), Offset(endX, centerY), 4.dp.toPx(), StrokeCap.Round)
        if (fraction > 0f) {
            drawLine(
                brush = Brush.horizontalGradient(listOf(Color(0xFFA855F7), Color(0xFFEC4899)), startX, endX),
                start = Offset(startX, centerY),
                end = Offset(thumbX, centerY),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        drawCircle(Color(0xFFEC4899).copy(alpha = .20f), 9.dp.toPx(), Offset(thumbX, centerY))
        drawCircle(thumbColor, 5.dp.toPx(), Offset(thumbX, centerY))
        drawCircle(Color(0xFFEC4899), 2.dp.toPx(), Offset(thumbX, centerY))
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
            animationSpec = tween(
                durationMillis = 82 + (index % 4) * 5,
                easing = androidx.compose.animation.core.FastOutSlowInEasing,
            ),
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
private fun PremiumAlbumCover(
    coverUrl: String,
    title: String,
    isPlaying: Boolean,
    onPalette: (List<Color>) -> Unit = {},
    coverModifier: Modifier = Modifier,
) {
    val motion = rememberInfiniteTransition(label = "coverGlow")
    val pulse by motion.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(1_800), repeatMode = RepeatMode.Reverse),
        label = "coverGlowPulse",
    )
    Box(
        modifier = Modifier.size(248.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Box(
        //     Modifier.size(230.dp)
        //         .graphicsLayer {
        //             val scale = if (isPlaying) pulse else 1f
        //             scaleX = scale
        //             scaleY = scale
        //             alpha = if (isPlaying) .7f else .42f
        //         }
        //         .blur(22.dp)
        //         .clip(MaterialTheme.shapes.extraLarge)
        //         .background(
        //             Brush.linearGradient(
        //                 listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
        //             ),
        //         ),
        // )
        AsyncImage(
            model = coverUrl,
            contentDescription = title,
            error = painterResource(R.drawable.arisam_app_icon_dark),
            contentScale = ContentScale.Crop,
            modifier = coverModifier.size(220.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge),
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
            Modifier.size(220.dp).clip(MaterialTheme.shapes.extraLarge).background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = .2f)),
                    startY = 110f,
                ),
            ),
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
private fun PlayerTextAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlayerOrbitAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PressScaleBox(onClick = onClick, modifier = modifier.width(58.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .9f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = label,
                        modifier = Modifier.size(19.dp),
                        tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlayerUtilityChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    PressScaleBox(onClick = onClick) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.size(15.dp),
                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackSpeedSheet(
    selectedSpeed: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val speeds = listOf(.75f, 1f, 1.25f, 1.5f, 2f)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.playback_speed), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.playback_speed_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            speeds.forEach { speed ->
                val selected = kotlin.math.abs(speed - selectedSpeed) < .01f
                PressScaleBox(onClick = { onSelect(speed) }, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Speed, null)
                            Text(
                                stringResource(R.string.playback_speed_value, formatSpeed(speed)),
                                Modifier.weight(1f).padding(horizontal = 14.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            if (selected) Icon(Icons.Rounded.CheckCircle, null)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    endsAtMillis: Long?,
    onSelectMinutes: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(15, 30, 45, 60)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.sleep_timer_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.sleep_timer_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (endsAtMillis != null) {
                val remaining = ((endsAtMillis - System.currentTimeMillis()).coerceAtLeast(0L) + 59_999L) / 60_000L
                TimerChoice(
                    label = stringResource(R.string.sleep_timer_remaining, remaining.toInt()),
                    selected = true,
                    onClick = { onSelectMinutes(0) },
                    trailing = stringResource(R.string.sleep_timer_off),
                )
            }
            options.forEach { minutes ->
                TimerChoice(
                    label = stringResource(R.string.sleep_timer_minutes, minutes),
                    selected = false,
                    onClick = { onSelectMinutes(minutes) },
                )
            }
            if (endsAtMillis == null) {
                TimerChoice(
                    label = stringResource(R.string.sleep_timer_off),
                    selected = true,
                    onClick = { onSelectMinutes(0) },
                )
            }
        }
    }
}

@Composable
private fun TimerChoice(label: String, selected: Boolean, onClick: () -> Unit, trailing: String? = null) {
    PressScaleBox(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.Timer, null)
                Text(label, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                trailing?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                if (selected) Icon(Icons.Rounded.CheckCircle, null)
            }
        }
    }
}

private fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0').trimEnd('.')

@Composable
private fun PlaybackModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription, modifier = Modifier.size(20.dp), tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlayerQueueScreen(
    currentSong: SongDto,
    queue: List<SongDto>,
    onBack: () -> Unit,
    onSongClick: (SongDto) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.playing_queue).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.track_count, queue.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.size(48.dp))
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "queue_hint") {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, null, tint = Color(0xFFD8B4FE))
                        Text(stringResource(R.string.player_queue_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            itemsIndexed(queue, key = { _, item -> item.id }) { index, item ->
                val active = item.id == currentSong.id
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                        .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onSongClick(item) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AsyncImage(
                        model = item.coverImageUrl,
                        contentDescription = item.title,
                        error = painterResource(R.drawable.arisam_app_icon_dark),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(52.dp).clip(MaterialTheme.shapes.small),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    if (active) Icon(Icons.Rounded.GraphicEq, null, tint = Color(0xFFB57BFF))
                    else Text("${index + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
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
    onSeek: (Long) -> Unit,
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
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), modifier = Modifier.size(20.dp))
                }
            }
            Text(
                stringResource(R.string.live_lyrics).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(48.dp))
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = song.coverImageUrl,
                contentDescription = song.title,
                error = painterResource(R.drawable.arisam_app_icon_dark),
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
                Text(song.artistName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            if (isPlaying) Icon(Icons.Rounded.GraphicEq, null, tint = Color(0xFFB57BFF))
        }
        if (lines.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.Mic, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.lyrics_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        modifier = Modifier.fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onSeek(line.startMillis) }
                            .padding(vertical = 6.dp)
                            .graphicsLayer {
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
                        color = if (index == activeIndex) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

internal data class TimedLyricLine(val startMillis: Long, val text: String)

private fun coil3.Image.toPaletteBitmap(): Bitmap {
    val bitmap = toBitmap()
    return if (bitmap.config == Bitmap.Config.HARDWARE) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        bitmap
    }
}

private val LrcLinePattern = Regex("""^\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]\s*(.+)$""")

internal fun parseTimedLyrics(song: SongDto): List<TimedLyricLine> {
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

internal fun parseSyncedLyricsJson(rawJson: String?): List<TimedLyricLine> {
    if (rawJson.isNullOrBlank()) return emptyList()
    return runCatching {
        val root = Json.parseToJsonElement(rawJson)
        val lines = when (root) {
            is kotlinx.serialization.json.JsonArray -> root
            else -> root.jsonObject["lines"]?.jsonArray ?: return@runCatching emptyList()
        }
        lines.mapNotNull { element ->
            val item = element.jsonObject
            val startMillis = item["startTimeMs"]?.jsonPrimitive?.doubleOrNull?.toLong()
                ?: item["start_time_ms"]?.jsonPrimitive?.doubleOrNull?.toLong()
                ?: item["time"]?.jsonPrimitive?.doubleOrNull?.times(1_000.0)?.toLong()
                ?: return@mapNotNull null
            val text = (item["text"] ?: item["words"])
                ?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            TimedLyricLine(startMillis, text)
        }.sortedBy(TimedLyricLine::startMillis)
    }.getOrDefault(emptyList())
}

@Composable
private fun AnimatedPlayerBackground(colors: List<Color>, coverUrl: String) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < .5f
    val motion = rememberInfiniteTransition(label = "coverGradient")
    val firstColor by animateColorAsState(
        targetValue = colors.firstOrNull() ?: Color(0xFF24123A),
        animationSpec = tween(1_100, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "playerPalettePrimary",
    )
    val secondColor by animateColorAsState(
        targetValue = colors.getOrElse(1) { Color(0xFF101F32) },
        animationSpec = tween(1_300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "playerPaletteSecondary",
    )
    val phase by motion.animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(8_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "coverGradientPhase",
    )
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AsyncImage(
            model = coverUrl,
            contentDescription = null,
            error = painterResource(R.drawable.arisam_app_icon_dark),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = 1.28f
                scaleY = 1.28f
                alpha = if (isDark) .24f else .1f
            }.blur(72.dp),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(
                        firstColor.copy(alpha = if (isDark) .78f else .12f),
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
                        secondColor.copy(alpha = if (isDark) .7f else .1f),
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
                    if (isDark) {
                        listOf(Color.Transparent, Color(0xE804040C))
                    } else {
                        listOf(MaterialTheme.colorScheme.surface.copy(alpha = .45f), MaterialTheme.colorScheme.background.copy(alpha = .96f))
                    },
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

@Composable
private fun playerSkipIcon(isNext: Boolean): ImageVector {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pointsForward = if (isRtl) !isNext else isNext
    return if (pointsForward) Icons.Rounded.SkipNext else Icons.Rounded.SkipPrevious
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

@Preview(name = "Player - Now Playing", showBackground = true)
@Composable
private fun NowPlayingScreenPreview() {
    AriSamTheme(darkTheme = true) {
        val song = PreviewCatalogData.songs.first()
        PlayerPreviewScreen(
            state = PlayerState(
                currentSong = song,
                isPlaying = true,
                progressSeconds = 86,
                progressMillis = 86_000L,
                queue = PreviewCatalogData.songs,
                isShuffleEnabled = true,
                repeatMode = 1,
                visualizerBands = List(32) { index -> .18f + (index % 7) * .1f },
            ),
        )
    }
}

@Preview(name = "Player - Queue", showBackground = true)
@Composable
private fun PlayerQueuePreview() {
    AriSamTheme(darkTheme = true) {
        PlayerQueueScreen(
            currentSong = PreviewCatalogData.songs.first(),
            queue = PreviewCatalogData.songs,
            onBack = {},
            onSongClick = {},
        )
    }
}

@Composable
private fun PlayerPreviewScreen(state: PlayerState) {
    val song = state.currentSong ?: return EmptyPlayer()
    AnimatedPlayerBackground(
        colors = listOf(Color(0xFF2A1246), Color(0xFF123047), Color(0xFF05060D)),
        coverUrl = song.coverImageUrl,
    )
    CompositionLocalProvider(LocalContentColor provides Color.White) {
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
                    IconButton(onClick = {}) {
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
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PremiumAlbumCover(
                    coverUrl = song.coverImageUrl,
                    title = song.title,
                    isPlaying = state.isPlaying,
                )
                PlayerOrbitAction(
                    icon = Icons.Rounded.Mic,
                    label = stringResource(R.string.lyrics),
                    selected = false,
                    onClick = {},
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                PlayerOrbitAction(
                    icon = Icons.Rounded.Download,
                    label = stringResource(R.string.save),
                    selected = false,
                    onClick = {},
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
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
                    IconButton(onClick = {}) {
                        Icon(Icons.Rounded.Favorite, null, tint = Color(0xFFF43F5E))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    PlayerTextAction(Icons.Rounded.Share, "Share") {}
                    PlayerTextAction(Icons.Rounded.Info, stringResource(R.string.song_information)) {}
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    AudioVisualizer(
                        isPlaying = state.isPlaying,
                        bands = state.visualizerBands,
                        compact = true,
                    )
                }
                PlayerUtilityChip(
                    icon = Icons.Rounded.SwapHoriz,
                    label = stringResource(R.string.crossfade),
                    selected = state.isCrossfadeEnabled,
                    onClick = {},
                )
            }
            Column(Modifier.fillMaxWidth()) {
                PlayerSeekBar(state.progressSeconds, song.durationSeconds) {}
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatDuration(state.progressSeconds), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .38f))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PlayerUtilityChip(
                            icon = Icons.Rounded.Speed,
                            label = stringResource(R.string.playback_speed_value, formatSpeed(state.playbackSpeed)),
                            selected = state.playbackSpeed != 1f,
                            onClick = {},
                        )
                        PlayerUtilityChip(
                            icon = Icons.Rounded.Timer,
                            label = stringResource(R.string.sleep),
                            selected = state.sleepTimerEndsAtMillis != null,
                            onClick = {},
                        )
                    }
                    Text("-${formatDuration((song.durationSeconds - state.progressSeconds).coerceAtLeast(0))}", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .38f))
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.Shuffle, null, tint = if (state.isShuffleEnabled) Color(0xFFB57BFF) else Color.White.copy(alpha = .42f))
                }
                IconButton(onClick = {}, modifier = Modifier.size(60.dp)) {
                    Icon(playerSkipIcon(isNext = false), stringResource(R.string.previous_track), modifier = Modifier.size(38.dp))
                }
                Box(
                    Modifier.size(68.dp).clip(CircleShape).background(
                        Brush.linearGradient(listOf(Color(0xFFA855F7), Color(0xFFEC4899))),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = {}, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Rounded.Pause, stringResource(R.string.pause), modifier = Modifier.size(40.dp))
                    }
                }
                IconButton(onClick = {}, modifier = Modifier.size(60.dp)) {
                    Icon(playerSkipIcon(isNext = true), stringResource(R.string.next_track), modifier = Modifier.size(38.dp))
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.Repeat, null, tint = if (state.repeatMode > 0) Color(0xFFB57BFF) else Color.White.copy(alpha = .42f))
                }
            }
        }
    }
}
