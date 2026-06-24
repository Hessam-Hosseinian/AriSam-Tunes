package com.arisamtunes.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.catalog.PlaylistDto
import com.arisamtunes.data.catalog.SongDto

@Composable
fun PlaylistsRoute(onPlaylistClick: (PlaylistDto) -> Unit, viewModel: PlaylistsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openCreate) {
                Icon(Icons.Rounded.Add, stringResource(R.string.playlist_create))
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> Loading()
                state.hasError && state.items.isEmpty() -> ErrorState(viewModel::refresh)
                state.items.isEmpty() -> EmptyState(R.string.playlists_empty)
                else -> PlaylistsGrid(state.items, onPlaylistClick, viewModel::openEdit)
            }
        }
        if (state.showEditor) {
            PlaylistEditorDialog(
                playlist = state.editingPlaylist,
                isSaving = state.isSaving,
                onDismiss = viewModel::closeEditor,
                onSave = viewModel::savePlaylist,
                onDelete = state.editingPlaylist?.let { playlist -> { viewModel.deletePlaylist(playlist) } },
            )
        }
    }
}

@Composable
private fun PlaylistsGrid(items: List<PlaylistDto>, onClick: (PlaylistDto) -> Unit, onEdit: (PlaylistDto) -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp), modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.lg), horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        items(items, key = PlaylistDto::id) { playlist ->
            PressScaleBox({ onClick(playlist) }) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    PlaylistArtwork(playlist, Modifier.fillMaxWidth().aspectRatio(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            playlist.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (playlist.scope == "USER") {
                            IconButton(onClick = { onEdit(playlist) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.Edit, stringResource(R.string.playlist_edit))
                            }
                        }
                    }
                    Text(stringResource(R.string.home_song_count, playlist.songCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PlaylistEditorDialog(
    playlist: PlaylistDto?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String?, Boolean) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember(playlist?.id) { mutableStateOf(playlist?.name.orEmpty()) }
    var description by remember(playlist?.id) { mutableStateOf(playlist?.description.orEmpty()) }
    var isPublic by remember(playlist?.id) { mutableStateOf(playlist?.isPublic ?: false) }
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(if (playlist == null) R.string.playlist_create else R.string.playlist_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.playlist_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.playlist_description)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.playlist_public))
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, description, isPublic) }, enabled = name.isNotBlank() && !isSaving) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onDelete?.let {
                    TextButton(onClick = it, enabled = !isSaving) { Text(stringResource(R.string.delete)) }
                }
                TextButton(onClick = onDismiss, enabled = !isSaving) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Composable
fun PlaylistDetailRoute(
    onBack: () -> Unit,
    onSongClick: (SongDto) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val songs = viewModel.songs.collectAsLazyPagingItems()
    val canEdit = state.playlist?.scope == "USER"
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
            Text(state.playlist?.name ?: stringResource(R.string.playlist), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        when {
            state.isLoading -> Loading()
            state.hasError -> ErrorState(viewModel::refresh)
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = AriSamThemeTokens.spacing.xl)) {
                state.playlist?.let { playlist ->
                    item {
                        Row(Modifier.fillMaxWidth().padding(AriSamThemeTokens.spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                            PlaylistArtwork(playlist, Modifier.size(112.dp))
                            Column(Modifier.padding(start = AriSamThemeTokens.spacing.lg), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(playlist.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                playlist.description?.takeIf(String::isNotBlank)?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3) }
                                Text(stringResource(R.string.home_song_count, playlist.songCount), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                if (songs.loadState.refresh is LoadState.Loading) item { Loading() }
                if (songs.loadState.refresh is LoadState.Error) item { ErrorState(songs::retry) }
                if (songs.loadState.refresh !is LoadState.Loading && songs.itemCount == 0) item { EmptyState(R.string.playlist_songs_empty) }
                items(songs.itemCount, key = { index -> songs.peek(index)?.id ?: index }) { index ->
                    songs[index]?.let { song ->
                        PressScaleBox({ onSongClick(song) }, Modifier.fillMaxWidth()) {
                            ListItem(
                                headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = { Text(song.artistName, maxLines = 1) },
                                leadingContent = {
                                    AsyncImage(song.coverImageUrl, song.title, contentScale = ContentScale.Crop, modifier = Modifier.size(54.dp).clip(MaterialTheme.shapes.medium))
                                },
                                trailingContent = if (canEdit) {
                                    {
                                        IconButton(onClick = {
                                            viewModel.removeSong(song.id) { songs.refresh() }
                                        }) {
                                            Icon(Icons.Rounded.Delete, stringResource(R.string.playlist_remove_song))
                                        }
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
                if (songs.loadState.append is LoadState.Loading) item { Loading() }
            }
        }
    }
}

@Composable
private fun PlaylistArtwork(playlist: PlaylistDto, modifier: Modifier) {
    if (!playlist.coverImageUrl.isNullOrBlank()) {
        AsyncImage(playlist.coverImageUrl, playlist.name, contentScale = ContentScale.Crop, modifier = modifier.clip(MaterialTheme.shapes.large))
    } else {
        Box(
            modifier.clip(MaterialTheme.shapes.large).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.onPrimary) }
    }
}

@Composable
private fun Loading() = Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun ErrorState(onRetry: () -> Unit) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    TextButton(onClick = onRetry) { Icon(Icons.Rounded.Refresh, null); Text(stringResource(R.string.retry)) }
}

@Composable
private fun EmptyState(message: Int) = Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(message), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
