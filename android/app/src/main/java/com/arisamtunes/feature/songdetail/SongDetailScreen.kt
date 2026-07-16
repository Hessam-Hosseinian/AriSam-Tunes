package com.arisamtunes.feature.songdetail

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val state by viewModel.state.collectAsState()
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
    LaunchedEffect(state.playlistActionDone) {
        if (state.playlistActionDone) snackbarHostState.showSnackbar(songAddedMessage)
    }
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.background))),
    ) {
      Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
            }
            Text(
                stringResource(R.string.song_information),
                modifier = Modifier.weight(1f).padding(end = 48.dp),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        when {
            state.isLoading -> Loading()
            state.hasError -> ErrorState(onRetry)
            state.song == null -> EmptyState()
            else -> SongMetadata(state.song, state.isLiked, onPlay, onShare, onToggleLike, onAddToPlaylistClick)
        }
      }
      SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(AriSamThemeTokens.spacing.lg))
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item { SongHero(song, onPlay) }
        item { SongQuickFacts(song) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Button(onClick = onToggleLike, modifier = Modifier.weight(1f).height(50.dp), shape = MaterialTheme.shapes.large) {
                    Icon(if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null)
                    Text(stringResource(if (isLiked) R.string.song_liked else R.string.song_like))
                }
                Button(onClick = onAddToPlaylistClick, modifier = Modifier.weight(1f).height(50.dp), shape = MaterialTheme.shapes.large) {
                    Icon(Icons.Rounded.Add, null)
                    Text(stringResource(R.string.playlist_add_song))
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { onShare(song) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg).height(50.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(Icons.Rounded.Share, null)
                Text(stringResource(R.string.chat_share_song))
            }
        }
        if (song.tags.isNotEmpty() || song.isExplicit || song.isLocal || song.isDemo) {
            item { SongChips(song) }
        }
        item {
            MetadataSection(
                title = R.string.metadata_core,
                rows = facts.take(8),
            )
        }
        item {
            MetadataSection(
                title = R.string.metadata_audio,
                rows = facts.drop(8).take(8),
            )
        }
        item {
            MetadataSection(
                title = R.string.metadata_catalog,
                rows = facts.drop(16),
            )
        }
        val extraRows = song.extraMetadata.metadataRows()
        if (extraRows.isNotEmpty()) {
            item { MetadataSection(R.string.metadata_discovered_extra, extraRows) }
        }
        song.lyrics?.takeIf(String::isNotBlank)?.let { lyrics ->
            item {
                MetadataSection(
                    title = R.string.metadata_lyrics,
                    rows = listOf(MetadataRow(R.string.metadata_lyrics, lyrics)),
                )
            }
        }
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
private fun SongHero(song: SongDto, onPlay: (SongDto) -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    Column(
        Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
      Box(Modifier.size(230.dp)) {
        AsyncImage(
            model = song.coverImageUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraLarge)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge),
        )
        Box(
            Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraLarge).background(
                Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.scrim.copy(alpha = .9f))),
            ),
        )
        PressScaleBox(
            onClick = { onPlay(song) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(58.dp).clip(MaterialTheme.shapes.large)
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, stringResource(R.string.play), tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(song.title, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
          Text(song.artistName, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          song.album?.takeIf(String::isNotBlank)?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
      }
    }
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
private fun MetadataSection(@StringRes title: Int, rows: List<MetadataRow>) {
    val visibleRows = rows.filter { it.value.isNotBlank() }
    if (visibleRows.isEmpty()) return
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(AriSamThemeTokens.spacing.lg), verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.sm)) {
                Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
private fun Loading() = Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun ErrorState(onRetry: () -> Unit) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    TextButton(onClick = onRetry) { Icon(Icons.Rounded.Refresh, null); Text(stringResource(R.string.retry)) }
}

@Composable
private fun EmptyState() = Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.song_detail_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
