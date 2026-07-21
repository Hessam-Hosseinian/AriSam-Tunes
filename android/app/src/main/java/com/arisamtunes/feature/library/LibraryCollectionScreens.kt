package com.arisamtunes.feature.library

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.catalog.CatalogRepository
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.local.LocalLibraryRepository
import com.arisamtunes.data.local.entity.LikedSongEntity
import com.arisamtunes.data.local.entity.RecentlyPlayedEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryCollectionKind { Liked, Recent }

data class LocalSongListItem(
    val songId: String,
    val title: String,
    val artistName: String,
    val album: String?,
    val audioUrl: String,
    val coverImageUrl: String?,
    val durationSeconds: Int,
)

@HiltViewModel
class LibraryCollectionViewModel @Inject constructor(
    private val repository: LocalLibraryRepository,
    private val catalogRepository: CatalogRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val kind = when (savedStateHandle.get<String>("kind")) {
        "recent" -> LibraryCollectionKind.Recent
        else -> LibraryCollectionKind.Liked
    }

    val songs = when (kind) {
        LibraryCollectionKind.Liked -> repository.observeLikedSongs()
            .map { songs -> songs.map(LikedSongEntity::toListItem) }
        LibraryCollectionKind.Recent -> repository.observeAllRecentlyPlayed()
            .map { songs -> songs.map(RecentlyPlayedEntity::toListItem) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val resolvingQueue = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isResolvingQueue = resolvingQueue.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false,
    )
    private var queueJob: Job? = null

    fun remove(songId: String) = viewModelScope.launch {
        when (kind) {
            LibraryCollectionKind.Liked -> repository.removeLiked(songId)
            LibraryCollectionKind.Recent -> repository.removeRecentlyPlayed(songId)
        }
    }

    fun playAll(shuffle: Boolean, onQueueReady: (SongDto, List<SongDto>) -> Unit) {
        if (queueJob?.isActive == true) return
        queueJob = viewModelScope.launch {
            resolvingQueue.value = true
            try {
                val queue = songs.value.mapNotNull { item ->
                    item.toSongDtoOrNull() ?: runCatching { catalogRepository.song(item.songId) }.getOrNull()
                }.let { if (shuffle) it.shuffled() else it }
                queue.firstOrNull()?.let { onQueueReady(it, queue) }
            } finally {
                resolvingQueue.value = false
            }
        }
    }
}

@Composable
fun LibraryCollectionRoute(
    onBack: () -> Unit,
    onExplore: () -> Unit,
    onSongClick: (String) -> Unit,
    onPlayQueue: (SongDto, List<SongDto>) -> Unit,
    viewModel: LibraryCollectionViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val isResolvingQueue by viewModel.isResolvingQueue.collectAsStateWithLifecycle()
    LibraryCollectionScreen(
        kind = viewModel.kind,
        songs = songs,
        isResolvingQueue = isResolvingQueue,
        onBack = onBack,
        onExplore = onExplore,
        onSongClick = onSongClick,
        onPlayAll = { viewModel.playAll(shuffle = false, onQueueReady = onPlayQueue) },
        onShuffle = { viewModel.playAll(shuffle = true, onQueueReady = onPlayQueue) },
        onRemove = viewModel::remove,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryCollectionScreen(
    kind: LibraryCollectionKind,
    songs: List<LocalSongListItem>,
    isResolvingQueue: Boolean,
    onBack: () -> Unit,
    onExplore: () -> Unit,
    onSongClick: (String) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onRemove: (String) -> Unit,
) {
    val colors = collectionColors(kind)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom))),
        contentPadding = PaddingValues(bottom = AriSamThemeTokens.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.sm),
    ) {
        item(key = "collection_hero", contentType = "header") {
            CollectionHero(
                kind = kind,
                songCount = songs.size,
                isResolvingQueue = isResolvingQueue,
                onBack = onBack,
                onPlayAll = onPlayAll,
                onShuffle = onShuffle,
            )
        }
        if (songs.isEmpty()) {
            item(key = "collection_empty", contentType = "state") {
                CollectionEmptyState(kind = kind, onExplore = onExplore)
            }
        } else {
            items(songs, key = LocalSongListItem::songId, contentType = { "local_song" }) { song ->
                val dismissState = rememberSwipeToDismissBoxState()
                LaunchedEffect(dismissState.currentValue) {
                    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                        onRemove(song.songId)
                    }
                }
                SwipeToDismissBox(
                    state = dismissState,
                    modifier = Modifier
                        .padding(horizontal = AriSamThemeTokens.spacing.lg)
                        .animateItem(
                            fadeInSpec = tween(220),
                            placementSpec = tween(280),
                            fadeOutSpec = tween(180),
                        ),
                    backgroundContent = { DismissBackground(dismissState.dismissDirection) },
                ) {
                    CollectionSongRow(
                        song = song,
                        accent = colors.accent,
                        onClick = { onSongClick(song.songId) },
                        onRemove = { onRemove(song.songId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionHero(
    kind: LibraryCollectionKind,
    songCount: Int,
    isResolvingQueue: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
) {
    val colors = collectionColors(kind)
    val title = if (kind == LibraryCollectionKind.Liked) R.string.home_liked_songs else R.string.home_recently_played
    val subtitle = if (kind == LibraryCollectionKind.Liked) R.string.library_liked_subtitle else R.string.library_recent_subtitle
    val icon = if (kind == LibraryCollectionKind.Liked) Icons.Rounded.Favorite else Icons.Rounded.History
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 38.dp, bottomEnd = 38.dp))
            .background(Brush.linearGradient(listOf(colors.heroStart, colors.heroEnd)))
            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 26.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = .2f)),
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = Color.White)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = .14f),
                contentColor = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .2f)),
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(38.dp)) }
            }
            Text(stringResource(title), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(subtitle), color = Color.White.copy(alpha = .76f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Text(stringResource(R.string.library_song_count, songCount), color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPlayAll,
                    enabled = songCount > 0 && !isResolvingQueue,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = colors.heroStart),
                ) {
                    if (isResolvingQueue) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.heroStart)
                    else Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.play_all))
                }
                OutlinedButton(
                    onClick = onShuffle,
                    enabled = songCount > 0 && !isResolvingQueue,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .42f)),
                ) {
                    Icon(Icons.Rounded.Shuffle, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.shuffle))
                }
            }
        }
    }
}

@Composable
private fun CollectionSongRow(
    song: LocalSongListItem,
    accent: Color,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    PressScaleBox(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (song.coverImageUrl.isNullOrBlank()) {
                    Box(Modifier.size(58.dp).clip(MaterialTheme.shapes.medium).background(accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.MusicNote, null, tint = accent)
                    }
                } else {
                    AsyncImage(
                        model = song.coverImageUrl,
                        contentDescription = song.title,
                        error = painterResource(R.drawable.arisam_app_icon_dark),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(58.dp).clip(MaterialTheme.shapes.medium),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(song.artistName, song.album).joinToString(" • "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.library_remove_item), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun DismissBackground(direction: SwipeToDismissBoxValue) {
    val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    Box(
        Modifier.fillMaxSize().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 22.dp),
        contentAlignment = alignment,
    ) {
        Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.library_remove_item), tint = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun CollectionEmptyState(kind: LibraryCollectionKind, onExplore: () -> Unit) {
    val colors = collectionColors(kind)
    val icon = if (kind == LibraryCollectionKind.Liked) Icons.Rounded.Favorite else Icons.Rounded.History
    val title = if (kind == LibraryCollectionKind.Liked) R.string.library_liked_empty_title else R.string.library_recent_empty_title
    val hint = if (kind == LibraryCollectionKind.Liked) R.string.library_liked_empty_hint else R.string.library_recent_empty_hint
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 58.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(78.dp).clip(RoundedCornerShape(26.dp)).background(colors.accent.copy(alpha = .13f)).border(1.dp, colors.accent.copy(alpha = .28f), RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(38.dp), tint = colors.accent)
        }
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(stringResource(hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Button(
            onClick = onExplore,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = Color(0xFF0C1821),
            ),
        ) {
            Text(stringResource(R.string.start_exploring))
        }
    }
}

private data class CollectionColors(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val accent: Color,
)

@Composable
private fun collectionColors(kind: LibraryCollectionKind) = when (kind) {
    LibraryCollectionKind.Liked -> CollectionColors(
        backgroundTop = MaterialTheme.colorScheme.background,
        backgroundBottom = MaterialTheme.colorScheme.surfaceContainer,
        heroStart = Color(0xFF8E2D57),
        heroEnd = Color(0xFF321B42),
        accent = Color(0xFFFF7AAA),
    )
    LibraryCollectionKind.Recent -> CollectionColors(
        backgroundTop = MaterialTheme.colorScheme.background,
        backgroundBottom = MaterialTheme.colorScheme.surfaceContainer,
        heroStart = Color(0xFF075985),
        heroEnd = Color(0xFF123246),
        accent = Color(0xFF58D3FF),
    )
}

private fun LikedSongEntity.toListItem() = LocalSongListItem(songId, title, artistName, album, audioUrl, coverImageUrl, durationSeconds)

private fun RecentlyPlayedEntity.toListItem() = LocalSongListItem(songId, title, artistName, album, audioUrl, coverImageUrl, durationSeconds)

private fun LocalSongListItem.toSongDtoOrNull(): SongDto? = audioUrl.takeIf(String::isNotBlank)?.let { playableUrl ->
    SongDto(
        id = songId,
        title = title,
        artistName = artistName,
        album = album,
        durationSeconds = durationSeconds,
        audioUrl = playableUrl,
        coverImageUrl = coverImageUrl.orEmpty(),
    )
}
