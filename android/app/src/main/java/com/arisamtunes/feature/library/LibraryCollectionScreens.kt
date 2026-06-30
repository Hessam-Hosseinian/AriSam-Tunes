package com.arisamtunes.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.map
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.local.LocalLibraryRepository
import com.arisamtunes.data.local.entity.LikedSongEntity
import com.arisamtunes.data.local.entity.RecentlyPlayedEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class LibraryCollectionKind { Liked, Recent }

data class LocalSongListItem(
    val songId: String,
    val title: String,
    val artistName: String,
    val album: String?,
    val coverImageUrl: String?,
)

@HiltViewModel
class LibraryCollectionViewModel @Inject constructor(
    repository: LocalLibraryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val kind = when (savedStateHandle.get<String>("kind")) {
        "recent" -> LibraryCollectionKind.Recent
        else -> LibraryCollectionKind.Liked
    }

    val songs: Flow<PagingData<LocalSongListItem>> = when (kind) {
        LibraryCollectionKind.Liked -> Pager(PagingConfig(pageSize = 30)) {
            repository.likedSongs()
        }.flow.map { paging -> paging.map(LikedSongEntity::toListItem) }

        LibraryCollectionKind.Recent -> Pager(PagingConfig(pageSize = 30)) {
            repository.recentlyPlayed()
        }.flow.map { paging -> paging.map(RecentlyPlayedEntity::toListItem) }
    }.cachedIn(viewModelScope)
}

@Composable
fun LibraryCollectionRoute(
    onBack: () -> Unit,
    onSongClick: (String) -> Unit,
    viewModel: LibraryCollectionViewModel = hiltViewModel(),
) {
    val songs = viewModel.songs.collectAsLazyPagingItems()
    val title = when (viewModel.kind) {
        LibraryCollectionKind.Liked -> R.string.home_liked_songs
        LibraryCollectionKind.Recent -> R.string.home_recently_played
    }
    val empty = when (viewModel.kind) {
        LibraryCollectionKind.Liked -> R.string.library_liked_empty
        LibraryCollectionKind.Recent -> R.string.library_recent_empty
    }
    val icon = when (viewModel.kind) {
        LibraryCollectionKind.Liked -> Icons.Rounded.Favorite
        LibraryCollectionKind.Recent -> Icons.Rounded.History
    }

    Column(Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AriSamThemeTokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.sm),
        ) {
            when {
                songs.loadState.refresh is LoadState.Loading -> item { LoadingState() }
                songs.itemCount == 0 -> item { EmptyState(icon, empty) }
                else -> items(songs.itemCount, key = { index -> songs.peek(index)?.songId ?: index }) { index ->
                    songs[index]?.let { song ->
                        PressScaleBox({ onSongClick(song.songId) }, Modifier.fillMaxWidth()) {
                            ListItem(
                                headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = {
                                    Text(
                                        listOfNotNull(song.artistName, song.album).joinToString(" • "),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                leadingContent = {
                                    if (song.coverImageUrl.isNullOrBlank()) {
                                        Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                    } else {
                                        AsyncImage(
                                            model = song.coverImageUrl,
                                            contentDescription = song.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(52.dp).clip(MaterialTheme.shapes.small),
                                        )
                                    }
                                },
                                trailingContent = {
                                    IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, null) }
                                },
                                colors = androidx.compose.material3.ListItemDefaults.colors(
                                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() = Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, text: Int) {
    Box(Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.md)) {
            Icon(icon, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(text), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun LikedSongEntity.toListItem() = LocalSongListItem(songId, title, artistName, album, coverImageUrl)

private fun RecentlyPlayedEntity.toListItem() = LocalSongListItem(songId, title, artistName, album, coverImageUrl)
