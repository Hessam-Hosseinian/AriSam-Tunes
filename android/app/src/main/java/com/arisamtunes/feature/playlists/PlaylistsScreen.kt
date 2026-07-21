package com.arisamtunes.feature.playlists

import com.arisamtunes.core.design.spacing.AriSamDimensions
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.components.ShimmerBox
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.catalog.PlaylistDto
import com.arisamtunes.data.catalog.SongDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsRoute(onPlaylistClick: (PlaylistDto) -> Unit, viewModel: PlaylistsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openCreate,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(stringResource(R.string.playlist_create), fontWeight = FontWeight.SemiBold) },
                containerColor = AriSamThemeTokens.tehranAmber,
                contentColor = AriSamThemeTokens.onTehranAmber,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.items.isEmpty() -> PlaylistsLoading()
                state.hasError && state.items.isEmpty() -> PlaylistFullPageMessage(
                    icon = Icons.Rounded.WarningAmber,
                    title = stringResource(R.string.playlist_load_error_title),
                    message = stringResource(R.string.playlist_load_error_hint),
                    actionLabel = stringResource(R.string.retry),
                    onAction = viewModel::refresh,
                )
                else -> PlaylistsContent(
                    state = state,
                    onPlaylistClick = onPlaylistClick,
                    onEdit = viewModel::openEdit,
                    onRefresh = viewModel::refresh,
                )
            }
        }
        if (state.showEditor) {
            PlaylistEditorSheet(
                playlist = state.editingPlaylist,
                isSaving = state.isSaving,
                hasError = state.actionFailed,
                onDismiss = viewModel::closeEditor,
                onSave = viewModel::savePlaylist,
                onDelete = state.editingPlaylist?.let { playlist -> { viewModel.deletePlaylist(playlist) } },
            )
        }
    }
}

@Composable
private fun PlaylistsContent(
    state: PlaylistsUiState,
    onPlaylistClick: (PlaylistDto) -> Unit,
    onEdit: (PlaylistDto) -> Unit,
    onRefresh: () -> Unit,
) {
    val spacing = AriSamThemeTokens.spacing
    val globalTitle = stringResource(R.string.home_global_playlists)
    val localTitle = stringResource(R.string.home_local_playlists)
    val personalTitle = stringResource(R.string.playlists_personal)
    val globalEmpty = stringResource(R.string.playlists_global_empty)
    val localEmpty = stringResource(R.string.playlists_local_empty)
    val userEmpty = stringResource(R.string.playlists_user_empty)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, top = spacing.md, bottom = AriSamDimensions.dp112),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            PlaylistHero(state.items.size, onRefresh)
        }
        playlistSection(
            title = globalTitle,
            emptyMessage = globalEmpty,
            playlists = state.globalPlaylists,
            onPlaylistClick = onPlaylistClick,
            onEdit = onEdit,
        )
        playlistSection(
            title = localTitle,
            emptyMessage = localEmpty,
            playlists = state.localPlaylists,
            onPlaylistClick = onPlaylistClick,
            onEdit = onEdit,
        )
        playlistSection(
            title = personalTitle,
            emptyMessage = userEmpty,
            playlists = state.userPlaylists,
            onPlaylistClick = onPlaylistClick,
            onEdit = onEdit,
        )
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.playlistSection(
    title: String,
    emptyMessage: String,
    playlists: List<PlaylistDto>,
    onPlaylistClick: (PlaylistDto) -> Unit,
    onEdit: (PlaylistDto) -> Unit,
) {
    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }, key = "section-$title") {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
    if (playlists.isEmpty()) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }, key = "empty-$title") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Text(
                    emptyMessage,
                    modifier = Modifier.padding(AriSamThemeTokens.spacing.lg),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    } else {
        items(playlists, key = { "$title-${it.id}" }) { playlist ->
            PlaylistGridCard(playlist, { onPlaylistClick(playlist) }, { onEdit(playlist) })
        }
    }
}

@Composable
private fun PlaylistHero(count: Int, onRefresh: () -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceContainerHigh),
                ),
            ),
        ) {
            Column(Modifier.padding(spacing.xl), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(AriSamDimensions.dp54),
                        shape = MaterialTheme.shapes.large,
                        color = AriSamThemeTokens.tehranAmber,
                        contentColor = AriSamThemeTokens.onTehranAmber,
                    ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) } }
                    Column(Modifier.weight(1f).padding(start = spacing.md)) {
                        Text(stringResource(R.string.playlists_hero_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.playlists_hero_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, stringResource(R.string.retry)) }
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.background.copy(alpha = .38f)) {
                    Text(
                        pluralStringResource(R.plurals.playlists_count, count, count),
                        modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistGridCard(playlist: PlaylistDto, onClick: () -> Unit, onEdit: () -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    PressScaleBox(onClick, Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = androidx.compose.foundation.BorderStroke(AriSamDimensions.dp1, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Box {
                    PlaylistArtwork(playlist, Modifier.fillMaxWidth().aspectRatio(1f))
                    if (playlist.canEdit) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(spacing.xs),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = .55f),
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        ) {
                            IconButton(onClick = onEdit, modifier = Modifier.size(AriSamDimensions.dp38)) {
                                Icon(Icons.Rounded.Edit, stringResource(R.string.playlist_edit), modifier = Modifier.size(AriSamDimensions.dp18))
                            }
                        }
                    }
                }
                Text(playlist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Icon(
                        if (playlist.isPublic) Icons.Rounded.Public else Icons.Rounded.Lock,
                        null,
                        modifier = Modifier.size(AriSamDimensions.dp14),
                        tint = if (playlist.canEdit) AriSamThemeTokens.tehranAmber else MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(if (playlist.canEdit) R.string.playlists_personal else R.string.playlists_public),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(stringResource(R.string.home_song_count, playlist.songCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistEditorSheet(
    playlist: PlaylistDto?,
    isSaving: Boolean,
    hasError: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String?, Boolean) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val spacing = AriSamThemeTokens.spacing
    var name by remember(playlist?.id) { mutableStateOf(playlist?.name.orEmpty()) }
    var description by remember(playlist?.id) { mutableStateOf(playlist?.description.orEmpty()) }
    var isPublic by remember(playlist?.id) { mutableStateOf(playlist?.isPublic ?: false) }
    var confirmDelete by remember(playlist?.id) { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = { if (!isSaving) onDismiss() }, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = spacing.xl).padding(bottom = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(AriSamDimensions.dp48),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) } }
                Column(Modifier.padding(start = spacing.md)) {
                    Text(stringResource(if (playlist == null) R.string.playlist_create else R.string.playlist_edit), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.playlist_editor_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(255) },
                label = { Text(stringResource(R.string.playlist_name)) },
                supportingText = { Text(stringResource(R.string.character_count_format, name.length, 255)) },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(2000) },
                label = { Text(stringResource(R.string.playlist_description)) },
                minLines = 3,
                maxLines = 5,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            )
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.fillMaxWidth().padding(spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isPublic) Icons.Rounded.Public else Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(horizontal = spacing.md)) {
                        Text(stringResource(R.string.playlist_public), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.playlist_visibility_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it }, enabled = !isSaving)
                }
            }
            if (hasError) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                    Row(Modifier.fillMaxWidth().padding(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WarningAmber, null)
                        Text(stringResource(R.string.playlist_action_error), Modifier.padding(start = spacing.sm), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Button(
                onClick = { onSave(name, description, isPublic) },
                enabled = name.isNotBlank() && !isSaving,
                modifier = Modifier.fillMaxWidth().height(AriSamDimensions.dp52),
                shape = MaterialTheme.shapes.large,
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(AriSamDimensions.dp20), strokeWidth = AriSamDimensions.dp2) else Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
            }
            onDelete?.let {
                OutlinedButton(onClick = { confirmDelete = true }, enabled = !isSaving, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                    Icon(Icons.Rounded.DeleteOutline, null)
                    Spacer(Modifier.width(spacing.sm))
                    Text(stringResource(R.string.playlist_delete))
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Rounded.DeleteOutline, null) },
            title = { Text(stringResource(R.string.playlist_delete_confirm_title)) },
            text = { Text(stringResource(R.string.playlist_delete_confirm_hint)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete?.invoke() }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
fun PlaylistDetailRoute(
    onBack: () -> Unit,
    onExplore: () -> Unit,
    onSongClick: (SongDto, List<SongDto>) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val songs = viewModel.songs.collectAsLazyPagingItems()
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PlaylistDetailTopBar(state.playlist?.name, onBack, viewModel::refresh)
            when {
                state.isLoading -> LoadingBlock(Modifier.fillMaxSize())
                state.hasError -> PlaylistFullPageMessage(
                    Icons.Rounded.WarningAmber,
                    stringResource(R.string.playlist_load_error_title),
                    stringResource(R.string.playlist_load_error_hint),
                    stringResource(R.string.retry),
                    viewModel::refresh,
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = AriSamThemeTokens.spacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.sm),
                ) {
                    state.playlist?.let { playlist ->
                        item {
                            PlaylistDetailHero(
                                playlist = playlist,
                                hasSongs = state.playbackQueue.isNotEmpty(),
                                onPlayAll = { state.playbackQueue.firstOrNull()?.let { onSongClick(it, state.playbackQueue) } },
                                onShuffle = {
                                    state.playbackQueue.shuffled().let { queue ->
                                        queue.firstOrNull()?.let { onSongClick(it, queue) }
                                    }
                                },
                            )
                        }
                    }
                    if (state.actionFailed) item { PlaylistInlineError() }
                    item { PlaylistSongsHeading(state.playlist?.songCount ?: 0) }
                    if (songs.loadState.refresh is LoadState.Loading) item { LoadingBlock(Modifier.fillMaxWidth().height(AriSamDimensions.dp160)) }
                    if (songs.loadState.refresh is LoadState.Error) item { PlaylistInlineRetry(songs::retry) }
                    if (songs.loadState.refresh !is LoadState.Loading && songs.itemCount == 0) item {
                        PlaylistEmptySongs(onExplore)
                    }
                    items(songs.itemCount, key = { index -> songs.peek(index)?.id ?: index }) { index ->
                        songs[index]?.let { song ->
                            PlaylistSongRow(
                                song = song,
                                index = index + 1,
                                canEdit = state.playlist?.canEdit == true,
                                onClick = { onSongClick(song, state.playbackQueue.ifEmpty { listOf(song) }) },
                                onRemove = { viewModel.removeSong(song.id) { songs.refresh() } },
                            )
                        }
                    }
                    if (songs.loadState.append is LoadState.Loading) item { LoadingBlock(Modifier.fillMaxWidth().height(AriSamDimensions.dp72)) }
                    if (songs.loadState.append is LoadState.Error) item { PlaylistInlineRetry(songs::retry) }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailTopBar(title: String?, onBack: () -> Unit, onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.xs, vertical = AriSamThemeTokens.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
        Text(title ?: stringResource(R.string.playlist), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, stringResource(R.string.retry)) }
    }
}

@Composable
private fun PlaylistDetailHero(
    playlist: PlaylistDto,
    hasSongs: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
) {
    val spacing = AriSamThemeTokens.spacing
    Column(Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Box(Modifier.size(AriSamDimensions.dp190)) {
            PlaylistArtwork(playlist, Modifier.fillMaxSize())
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(spacing.sm),
                shape = CircleShape,
                color = AriSamThemeTokens.tehranAmber,
                contentColor = AriSamThemeTokens.onTehranAmber,
            ) { Icon(if (playlist.isPublic) Icons.Rounded.Public else Icons.Rounded.Lock, null, Modifier.padding(spacing.sm).size(AriSamDimensions.dp18)) }
        }
        Text(playlist.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        playlist.description?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
            Text(stringResource(if (playlist.canEdit) R.string.playlists_personal else R.string.playlists_public), Modifier.padding(horizontal = spacing.md, vertical = spacing.sm), style = MaterialTheme.typography.labelLarge)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Button(onClick = onPlayAll, enabled = hasSongs, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(spacing.xs))
                Text(stringResource(R.string.play_all))
            }
            OutlinedButton(onClick = onShuffle, enabled = hasSongs, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Shuffle, null)
                Spacer(Modifier.width(spacing.xs))
                Text(stringResource(R.string.shuffle))
            }
        }
    }
}

@Composable
private fun PlaylistSongsHeading(songCount: Long) {
    Row(Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg, vertical = AriSamThemeTokens.spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.playlist_tracks), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.home_song_count, songCount), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlaylistSongRow(song: SongDto, index: Int, canEdit: Boolean, onClick: () -> Unit, onRemove: () -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    PressScaleBox(onClick, Modifier.fillMaxWidth().padding(horizontal = spacing.lg)) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(Modifier.fillMaxWidth().padding(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Text(index.toString().padStart(2, '0'), modifier = Modifier.width(AriSamDimensions.dp30), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AsyncImage(
                    model = song.coverImageUrl,
                    contentDescription = song.title,
                    error = painterResource(R.drawable.arisam_app_icon_dark),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(AriSamDimensions.dp56).clip(MaterialTheme.shapes.medium),
                )
                Column(Modifier.weight(1f).padding(horizontal = spacing.md)) {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (canEdit) IconButton(onClick = onRemove) { Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.playlist_remove_song), tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun PlaylistArtwork(playlist: PlaylistDto, modifier: Modifier) {
    if (!playlist.coverImageUrl.isNullOrBlank()) {
        AsyncImage(
            model = playlist.coverImageUrl,
            contentDescription = playlist.name,
            error = painterResource(R.drawable.arisam_app_icon_dark),
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(MaterialTheme.shapes.large),
        )
    } else {
        Box(
            modifier.clip(MaterialTheme.shapes.large).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, modifier = Modifier.size(AriSamDimensions.dp54), tint = MaterialTheme.colorScheme.onPrimary) }
    }
}

@Composable
private fun PlaylistsLoading() {
    val spacing = AriSamThemeTokens.spacing
    LazyVerticalGrid(
        columns = GridCells.Adaptive(AriSamDimensions.dp164),
        contentPadding = PaddingValues(spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { ShimmerBox(Modifier.fillMaxWidth().height(AriSamDimensions.dp178)) }
        items(6) { ShimmerBox(Modifier.fillMaxWidth().aspectRatio(.78f)) }
    }
}

@Composable
private fun PlaylistFullPageMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(AriSamThemeTokens.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(modifier = Modifier.size(AriSamDimensions.dp72), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(AriSamDimensions.dp34)) }
        }
        Spacer(Modifier.height(AriSamThemeTokens.spacing.lg))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(message, modifier = Modifier.padding(top = AriSamThemeTokens.spacing.sm), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilledTonalButton(onClick = onAction, modifier = Modifier.padding(top = AriSamThemeTokens.spacing.lg)) { Text(actionLabel) }
    }
}

@Composable
private fun PlaylistInlineError() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) { Text(stringResource(R.string.playlist_action_error), Modifier.padding(AriSamThemeTokens.spacing.md)) }
}

@Composable
private fun PlaylistInlineRetry(onRetry: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().padding(AriSamThemeTokens.spacing.lg), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.padding(AriSamThemeTokens.spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.playlist_load_error_hint), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun PlaylistEmptySongs(onExplore: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(AriSamThemeTokens.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.md),
    ) {
        Icon(Icons.Rounded.MusicNote, null, Modifier.size(AriSamDimensions.dp48), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.playlist_songs_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilledTonalButton(onClick = onExplore) { Text(stringResource(R.string.start_exploring)) }
    }
}

@Composable
private fun LoadingBlock(modifier: Modifier = Modifier) = Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
