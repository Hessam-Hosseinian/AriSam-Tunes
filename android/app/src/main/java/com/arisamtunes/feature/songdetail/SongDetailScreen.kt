package com.arisamtunes.feature.songdetail

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.components.ShimmerBox
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.catalog.PlaylistDto
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.feature.playlists.PlaylistEditorSheet
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

@Composable
fun SongDetailRoute(
    onBack: () -> Unit,
    onPlay: (SongDto) -> Unit,
    onShare: (SongDto) -> Unit,
    viewModel: SongDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SongDetailScreen(
        state = state,
        onBack = onBack,
        onPlay = onPlay,
        onShare = onShare,
        onRetry = viewModel::refresh,
        onToggleLike = viewModel::toggleLike,
        onAddToPlaylistClick = viewModel::openPlaylistPicker,
        onPlaylistSelected = viewModel::addToPlaylist,
        onDismissPlaylistPicker = viewModel::closePlaylistPicker,
        onCreatePlaylist = viewModel::openPlaylistCreator,
        onDismissPlaylistCreator = viewModel::closePlaylistCreator,
        onSaveNewPlaylist = viewModel::createPlaylistAndAdd,
    )
}

@Composable
private fun SongDetailScreen(
    state: SongDetailUiState,
    onBack: () -> Unit,
    onPlay: (SongDto) -> Unit,
    onShare: (SongDto) -> Unit,
    onRetry: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onPlaylistSelected: (PlaylistDto) -> Unit,
    onDismissPlaylistPicker: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onDismissPlaylistCreator: () -> Unit,
    onSaveNewPlaylist: (String, String?, Boolean) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val songAddedMessage = stringResource(R.string.playlist_song_added)
    val ambientMotion = rememberInfiniteTransition(label = "songDetailAmbient")
    val ambientPhase by ambientMotion.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "songDetailAmbientPhase",
    )
    val contentState = when {
        state.isLoading -> SongDetailContentState.Loading
        state.hasError -> SongDetailContentState.Error
        state.song == null -> SongDetailContentState.Empty
        else -> SongDetailContentState.Ready
    }
    LaunchedEffect(state.playlistActionDone) {
        if (state.playlistActionDone) snackbarHostState.showSnackbar(songAddedMessage)
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = .16f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        SongAmbientBackdrop(ambientPhase)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            SongDetailTopBar(onBack = onBack, phase = ambientPhase)
            AnimatedContent(
                targetState = contentState,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(360)) + slideInVertically(tween(480, easing = FastOutSlowInEasing)) { it / 12 }) togetherWith
                        fadeOut(tween(180))
                },
                label = "songDetailContent",
            ) { target ->
                when (target) {
                    SongDetailContentState.Loading -> Loading()
                    SongDetailContentState.Error -> ErrorState(onRetry)
                    SongDetailContentState.Empty -> EmptyState()
                    SongDetailContentState.Ready -> state.song?.let { song ->
                        SongMetadata(
                            song = song,
                            isLiked = state.isLiked,
                            onPlay = onPlay,
                            onShare = onShare,
                            onToggleLike = onToggleLike,
                            onAddToPlaylistClick = onAddToPlaylistClick,
                        )
                    }
                }
            }
        }
        SnackbarHost(
            snackbarHostState,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(AriSamThemeTokens.spacing.lg),
        )
    }
    if (state.showPlaylistPicker) {
        PlaylistPickerDialog(
            playlists = state.playlists,
            isLoading = state.isLoadingPlaylists,
            isAdding = state.isAddingToPlaylist,
            hasError = state.playlistActionFailed,
            onDismiss = onDismissPlaylistPicker,
            onPlaylistSelected = onPlaylistSelected,
            onCreatePlaylist = onCreatePlaylist,
        )
    }
    if (state.showPlaylistCreator) {
        PlaylistEditorSheet(
            playlist = null,
            isSaving = state.isAddingToPlaylist,
            hasError = state.playlistActionFailed,
            onDismiss = onDismissPlaylistCreator,
            onSave = onSaveNewPlaylist,
            onDelete = null,
        )
    }
}

private enum class SongDetailContentState { Loading, Error, Empty, Ready }

@Composable
private fun SongAmbientBackdrop(phase: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(320.dp)
                .graphicsLayer {
                    translationX = 170f + phase * 42f
                    translationY = -95f + phase * 34f
                    alpha = .2f
                    scaleX = 1f + phase * .06f
                    scaleY = 1f + phase * .06f
                }
                .background(
                    Brush.radialGradient(listOf(primary, Color.Transparent)),
                    CircleShape,
                ),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .size(250.dp)
                .graphicsLayer {
                    translationX = -145f - phase * 30f
                    translationY = phase * 75f
                    alpha = .14f
                }
                .background(
                    Brush.radialGradient(listOf(secondary, Color.Transparent)),
                    CircleShape,
                ),
        )
    }
}

@Composable
private fun SongDetailTopBar(onBack: () -> Unit, phase: Float) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PressScaleBox(onClick = onBack) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .88f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.song_information),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                stringResource(R.string.song_detail_header_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        MiniEqualizer(phase = phase, modifier = Modifier.width(46.dp).height(30.dp))
    }
}

@Composable
private fun SongMetadata(
    song: SongDto,
    isLiked: Boolean,
    onPlay: (SongDto) -> Unit,
    onShare: (SongDto) -> Unit,
    onToggleLike: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
) {
    val spacing = AriSamThemeTokens.spacing
    val facts = song.metadataFacts()
    val listState = rememberLazyListState()
    val heroCollapse by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 420f).coerceIn(0f, 1f)
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = PaddingValues(top = spacing.sm, bottom = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "hero", contentType = "hero") { SongHero(song, onPlay, heroCollapse) }
        item(key = "facts", contentType = "facts") {
            RevealSection(delayMillis = 90) { SongQuickFacts(song) }
        }
        item(key = "actions", contentType = "actions") {
            RevealSection(delayMillis = 160) {
                SongActionDock(
                    song = song,
                    isLiked = isLiked,
                    onToggleLike = onToggleLike,
                    onAddToPlaylistClick = onAddToPlaylistClick,
                    onShare = onShare,
                )
            }
        }
        if (song.tags.isNotEmpty() || song.isExplicit || song.isLocal || song.isDemo) {
            item(key = "chips", contentType = "chips") {
                RevealSection(delayMillis = 210) { SongChips(song) }
            }
        }
        item(key = "metadata_core", contentType = "metadata") {
            RevealSection(delayMillis = 240) {
                MetadataSection(
                    title = R.string.metadata_core,
                    rows = facts.take(8),
                    accent = MaterialTheme.colorScheme.primary,
                )
            }
        }
        item(key = "metadata_audio", contentType = "metadata") {
            RevealSection(delayMillis = 280) {
                MetadataSection(
                    title = R.string.metadata_audio,
                    rows = facts.drop(8).take(8),
                    accent = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        item(key = "metadata_catalog", contentType = "metadata") {
            RevealSection(delayMillis = 320) {
                MetadataSection(
                    title = R.string.metadata_catalog,
                    rows = facts.drop(16),
                    accent = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        val extraRows = song.extraMetadata.metadataRows()
        if (extraRows.isNotEmpty()) {
            item(key = "metadata_extra", contentType = "metadata") {
                RevealSection(delayMillis = 360) {
                    MetadataSection(R.string.metadata_discovered_extra, extraRows, MaterialTheme.colorScheme.primary)
                }
            }
        }
        song.lyrics?.takeIf(String::isNotBlank)?.let { lyrics ->
            item(key = "lyrics", contentType = "metadata") {
                RevealSection(delayMillis = 400) {
                    MetadataSection(
                        title = R.string.metadata_lyrics,
                        rows = listOf(MetadataRow(R.string.metadata_lyrics, lyrics)),
                        accent = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun RevealSection(delayMillis: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(340)) + slideInVertically(tween(480, easing = FastOutSlowInEasing)) { it / 4 },
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistPickerDialog(
    playlists: List<PlaylistDto>,
    isLoading: Boolean,
    isAdding: Boolean,
    hasError: Boolean,
    onDismiss: () -> Unit,
    onPlaylistSelected: (PlaylistDto) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(playlists, query) { playlists.filter { it.name.contains(query.trim(), ignoreCase = true) } }
    ModalBottomSheet(
        onDismissRequest = { if (!isAdding) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = AriSamThemeTokens.spacing.xl)
                .padding(bottom = AriSamThemeTokens.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.xs)) {
                Text(stringResource(R.string.playlist_add_song), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.playlist_picker_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!isLoading && playlists.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    placeholder = { Text(stringResource(R.string.playlist_search)) },
                    enabled = !isAdding,
                )
            }
            Button(onClick = onCreatePlaylist, enabled = !isAdding, modifier = Modifier.fillMaxWidth().height(50.dp), shape = MaterialTheme.shapes.large) {
                Icon(Icons.Rounded.Add, null)
                Text(stringResource(R.string.playlist_create_new), Modifier.padding(start = AriSamThemeTokens.spacing.sm))
            }
            when {
                isLoading || isAdding -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                hasError -> Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                    Text(stringResource(R.string.playlist_action_error), Modifier.fillMaxWidth().padding(AriSamThemeTokens.spacing.lg))
                }
                playlists.isEmpty() -> Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.fillMaxWidth().padding(AriSamThemeTokens.spacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.MusicNote, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.playlist_create_first), Modifier.padding(top = AriSamThemeTokens.spacing.md), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                filtered.isEmpty() -> Text(stringResource(R.string.playlist_search_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp), verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.sm)) {
                    items(filtered.size, key = { filtered[it].id }) { index ->
                        val playlist = filtered[index]
                        PressScaleBox({ onPlaylistSelected(playlist) }, Modifier.fillMaxWidth()) {
                            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                                ListItem(
                                    headlineContent = { Text(playlist.name, fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text(stringResource(R.string.home_song_count, playlist.songCount)) },
                                    leadingContent = {
                                        Surface(modifier = Modifier.size(46.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null) }
                                        }
                                    },
                                    trailingContent = { Icon(Icons.Rounded.AddCircle, stringResource(R.string.playlist_add_song), tint = MaterialTheme.colorScheme.primary) },
                                    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                )
                            }
                        }
                    }
                }
            }
            TextButton(onClick = onDismiss, enabled = !isAdding, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.cancel)) }
        }
    }
}

@Composable
private fun SongHero(song: SongDto, onPlay: (SongDto) -> Unit, collapseProgress: Float) {
    val spacing = AriSamThemeTokens.spacing
    val motion = rememberInfiniteTransition(label = "songHeroMotion")
    val floatOffset by motion.animateFloat(
        initialValue = -5f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "songCoverFloat",
    )
    val glowRotation by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing)),
        label = "songGlowRotation",
    )
    var entered by remember(song.id) { mutableStateOf(false) }
    LaunchedEffect(song.id) { entered = true }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(520)) + scaleIn(tween(620, easing = FastOutSlowInEasing), initialScale = .82f),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(286.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(274.dp)
                        .rotate(glowRotation)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = .08f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = .55f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = .12f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = .08f),
                                ),
                            ),
                            RoundedCornerShape(48.dp),
                        ),
                )
                Box(
                    Modifier
                        .size(248.dp)
                        .graphicsLayer {
                            translationY = floatOffset - collapseProgress * 12f
                            scaleX = 1f - collapseProgress * .045f
                            scaleY = 1f - collapseProgress * .045f
                            shadowElevation = 30f
                            shape = RoundedCornerShape(38.dp)
                            clip = true
                        },
                ) {
                    AsyncImage(
                        model = song.coverImageUrl,
                        contentDescription = song.title,
                        error = painterResource(R.drawable.arisam_app_icon_dark),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.scrim.copy(alpha = .84f),
                                    ),
                                ),
                            ),
                    )
                    MiniEqualizer(
                        phase = floatOffset / 6f,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                            .width(62.dp)
                            .height(34.dp),
                    )
                }
                PulsingPlayButton(
                    onClick = { onPlay(song) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 28.dp, bottom = 6.dp),
                )
            }
        }
        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(440, delayMillis = 110)) + slideInVertically(tween(520, delayMillis = 110)) { it / 3 },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    song.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artistName,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                song.album?.takeIf(String::isNotBlank)?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingPlayButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "songPlayPulse")
    val pulse by transition.animateFloat(
        initialValue = .92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "songPlayPulseScale",
    )
    Box(modifier.size(78.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(70.dp)
                .scale(pulse)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = .18f), CircleShape),
        )
        PressScaleBox(onClick = onClick, modifier = Modifier.size(62.dp)) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(
                    Modifier.background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                        ),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PlayArrow, stringResource(R.string.play), modifier = Modifier.size(34.dp))
                }
            }
        }
    }
}

@Composable
private fun MiniEqualizer(phase: Float, modifier: Modifier = Modifier) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(5) { index ->
            val wave = kotlin.math.abs(kotlin.math.sin((phase * 2.1f + index * .72f).toDouble())).toFloat()
            Box(
                Modifier
                    .width(4.dp)
                    .height((8 + wave * 22).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun SongActionDock(
    song: SongDto,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onShare: (SongDto) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AriSamThemeTokens.spacing.lg),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .86f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f)),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SongAction(
                icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = stringResource(if (isLiked) R.string.song_liked else R.string.song_like),
                selected = isLiked,
                onClick = onToggleLike,
            )
            ActionDivider()
            SongAction(
                icon = Icons.Rounded.Add,
                label = stringResource(R.string.playlist_add_song),
                onClick = onAddToPlaylistClick,
            )
            ActionDivider()
            SongAction(
                icon = Icons.Rounded.Share,
                label = stringResource(R.string.share),
                onClick = { onShare(song) },
            )
        }
    }
}

@Composable
private fun RowScope.SongAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.14f else 1f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "songActionIconScale",
    )
    PressScaleBox(onClick = onClick, modifier = Modifier.weight(1f)) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .2f)
                else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .7f),
                contentColor = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(22.dp).scale(iconScale))
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActionDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(48.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f)),
    )
}

@Composable
private fun SongQuickFacts(song: SongDto) {
    val facts = listOfNotNull(
        song.durationSeconds.takeIf { it > 0 }?.let { Triple(Icons.Rounded.Schedule, stringResource(R.string.metadata_duration), formatDuration(it)) },
        song.album?.takeIf(String::isNotBlank)?.let { Triple(Icons.Rounded.Album, stringResource(R.string.metadata_album), it) },
        (song.fileFormat ?: song.codec)?.takeIf(String::isNotBlank)?.let { Triple(Icons.Rounded.GraphicEq, stringResource(R.string.metadata_file_format), it.uppercase()) },
    )
    if (facts.isEmpty()) return
    Row(
        Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        facts.forEach { (icon, label, value) ->
            Surface(
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SongChips(song: SongDto) {
    val spacing = AriSamThemeTokens.spacing
    Row(
        Modifier.fillMaxWidth().padding(horizontal = spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (song.isExplicit) AssistChip(onClick = { }, label = { Text(stringResource(R.string.metadata_explicit)) }, leadingIcon = { Icon(Icons.Rounded.Explicit, null) })
        if (song.isLocal) AssistChip(onClick = { }, label = { Text(stringResource(R.string.metadata_local)) })
        if (song.isDemo) AssistChip(onClick = { }, label = { Text(stringResource(R.string.metadata_demo)) })
        song.tags.take(2).forEach { tag -> AssistChip(onClick = { }, label = { Text(tag, maxLines = 1) }) }
    }
}

@Composable
private fun MetadataSection(@StringRes title: Int, rows: List<MetadataRow>, accent: Color) {
    val visibleRows = rows.filter { it.value.isNotBlank() }
    if (visibleRows.isEmpty()) return
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .88f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = .23f)),
    ) {
        Column(Modifier.padding(AriSamThemeTokens.spacing.lg), verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.sm)) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = accent.copy(alpha = .14f),
                    contentColor = accent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Info, null, modifier = Modifier.size(19.dp))
                    }
                }
                Column {
                    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text(
                        stringResource(R.string.song_detail_fields_count, visibleRows.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            visibleRows.forEachIndexed { index, row ->
                MetadataLine(row)
                if (index != visibleRows.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun MetadataLine(row: MetadataRow) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        Text(stringResource(row.label), modifier = Modifier.width(105.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(row.value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Loading() {
    val transition = rememberInfiniteTransition(label = "songLoadingMotion")
    val phase by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "songLoadingPhase",
    )
    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            ShimmerBox(
                Modifier
                    .size(248.dp)
                    .graphicsLayer {
                        translationY = phase * 5f
                        shadowElevation = 18f
                        shape = RoundedCornerShape(38.dp)
                        clip = true
                    },
            )
            MiniEqualizer(phase, Modifier.width(66.dp).height(38.dp))
        }
        ShimmerBox(Modifier.fillMaxWidth(.68f).height(30.dp))
        ShimmerBox(Modifier.fillMaxWidth(.42f).height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { ShimmerBox(Modifier.weight(1f).height(78.dp)) }
        }
        ShimmerBox(Modifier.fillMaxWidth().height(96.dp))
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) = Box(
    Modifier.fillMaxSize().navigationBarsPadding().padding(28.dp),
    contentAlignment = Alignment.Center,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.Rounded.Refresh, null, Modifier.size(44.dp))
            Text(stringResource(R.string.playlist_load_error_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun EmptyState() = Box(
    Modifier.fillMaxSize().navigationBarsPadding().padding(32.dp),
    contentAlignment = Alignment.Center,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .86f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.song_detail_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class MetadataRow(@param:StringRes val label: Int, val value: String)

private fun SongDto.metadataFacts() = listOfNotNull(
    text(R.string.metadata_title, title),
    text(R.string.metadata_artist, artistName),
    text(R.string.metadata_album, album),
    text(R.string.metadata_album_artist, albumArtist),
    text(R.string.metadata_genre, genre),
    text(R.string.metadata_composer, composer),
    text(R.string.metadata_producer, producer),
    text(R.string.metadata_mood, mood),
    durationSeconds.takeIf { it > 0 }?.let { MetadataRow(R.string.metadata_duration, formatDuration(it)) },
    bitrateKbps?.let { MetadataRow(R.string.metadata_bitrate, "$it kbps") },
    sampleRateHz?.let { MetadataRow(R.string.metadata_sample_rate, "$it Hz") },
    text(R.string.metadata_channels, channels),
    text(R.string.metadata_codec, codec),
    text(R.string.metadata_file_format, fileFormat),
    audioFileSize?.takeIf { it > 0L }?.let { MetadataRow(R.string.metadata_file_size, formatBytes(it)) },
    text(R.string.metadata_source_file, sourceFileName),
    trackNumber?.let { MetadataRow(R.string.metadata_track_number, it.toString()) },
    discNumber?.let { MetadataRow(R.string.metadata_disc_number, it.toString()) },
    releaseYear?.let { MetadataRow(R.string.metadata_release_year, it.toString()) },
    text(R.string.metadata_release_date, releaseDate),
    text(R.string.metadata_language, language),
    MetadataRow(R.string.metadata_popularity, popularity.toString()),
    MetadataRow(R.string.metadata_play_count, playCount.toString()),
    text(R.string.metadata_created_at, createdAt),
    text(R.string.metadata_updated_at, updatedAt),
    text(R.string.metadata_audio_url, audioUrl),
    text(R.string.metadata_cover_url, coverImageUrl),
)

private fun text(@StringRes label: Int, value: String?) = value?.takeIf { it.isNotBlank() }?.let { MetadataRow(label, it) }

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return "%.2f MB".format(mb)
}

private fun JsonObject.metadataRows(): List<MetadataRow> = entries.mapNotNull { (key, value) ->
    val rendered = when (value) {
        JsonNull -> null
        is JsonPrimitive -> value.contentOrNull ?: value.booleanOrNull?.toString() ?: value.longOrNull?.toString() ?: value.doubleOrNull?.toString()
        is JsonArray -> value.joinToString(", ") { it.toString().trim('"') }
        is JsonObject -> value.toString()
    }?.takeIf { it.isNotBlank() && it != "{}" && it != "[]" }
    rendered?.let { MetadataRow(R.string.metadata_extra_item, "$key: $it") }
}
