package com.arisamtunes.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.arisamtunes.core.design.components.ShimmerBox
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.catalog.PlaylistDto
import com.arisamtunes.data.catalog.SongDto

enum class HomeQuickAction { Liked, Recent, Playlists, Artists }

@Composable
fun HomeRoute(
    onSongClick: (SongDto) -> Unit,
    onPlaylistClick: (PlaylistDto) -> Unit,
    onQuickAction: (HomeQuickAction) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    HomeScreen(state, viewModel::refresh, onSongClick, onPlaylistClick, onQuickAction)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRetry: () -> Unit,
    onSongClick: (SongDto) -> Unit,
    onPlaylistClick: (PlaylistDto) -> Unit,
    onQuickAction: (HomeQuickAction) -> Unit,
) {
    val spacing = AriSamThemeTokens.spacing
    if (state.isLoading && state.trending.isEmpty()) {
        HomeLoadingSkeleton()
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = spacing.md, bottom = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        if (state.hasError) item { ErrorCard(onRetry) }
        if (state.trending.isNotEmpty()) item { HeroCarousel(state.trending, onSongClick) }
        item { QuickActions(onQuickAction) }
        if (state.popular.isNotEmpty()) item { SongSection(R.string.home_most_popular, state.popular, onSongClick) }
        if (state.newReleases.isNotEmpty()) item { SongSection(R.string.home_new_releases, state.newReleases, onSongClick) }
        if (state.globalPlaylists.isNotEmpty()) item { PlaylistSection(R.string.home_global_playlists, state.globalPlaylists, onPlaylistClick) }
        if (state.localPlaylists.isNotEmpty()) item { PlaylistSection(R.string.home_local_playlists, state.localPlaylists, onPlaylistClick) }
    }
}

@Composable
private fun HomeLoadingSkeleton() {
    val spacing = AriSamThemeTokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        item { ShimmerBox(Modifier.fillMaxWidth().height(220.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), modifier = Modifier.fillMaxWidth()) {
                repeat(3) { ShimmerBox(Modifier.weight(1f).height(84.dp)) }
            }
        }
        items(3) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                ShimmerBox(Modifier.width(160.dp).height(24.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    items(4) { ShimmerBox(Modifier.width(172.dp).height(220.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HeroCarousel(songs: List<SongDto>, onSongClick: (SongDto) -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    val pages = songs.take(8)
    val pagerState = rememberPagerState(pageCount = { pages.size })
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = spacing.lg),
            pageSpacing = spacing.md,
        ) { page ->
            val song = pages[page]
            PressScaleBox(onClick = { onSongClick(song) }, modifier = Modifier.fillMaxWidth().height(196.dp)) {
                AsyncImage(
                    model = song.coverImageUrl, contentDescription = song.title,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.large),
                )
                Box(
                    Modifier.fillMaxSize().clip(MaterialTheme.shapes.large).background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .86f))),
                    ),
                )
                Column(Modifier.align(Alignment.BottomStart).padding(spacing.lg)) {
                    Text(stringResource(R.string.home_trending_now), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
                    Text(song.title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artistName, color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.bodyMedium)
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(spacing.lg).size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.PlayArrow, stringResource(R.string.play), tint = MaterialTheme.colorScheme.onPrimary) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            pages.indices.forEach { index ->
                Box(
                    Modifier.padding(horizontal = 3.dp).size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                        .clip(CircleShape).background(if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                )
            }
        }
    }
}

@Composable
private fun QuickActions(onClick: (HomeQuickAction) -> Unit) {
    val actions = listOf(
        Triple(HomeQuickAction.Liked, R.string.home_liked_songs, Icons.Rounded.Favorite),
        Triple(HomeQuickAction.Recent, R.string.home_recently_played, Icons.Rounded.History),
        Triple(HomeQuickAction.Playlists, R.string.home_my_playlists, Icons.AutoMirrored.Rounded.QueueMusic),
        Triple(HomeQuickAction.Artists, R.string.home_top_artists, Icons.Rounded.Groups),
    )
    val spacing = AriSamThemeTokens.spacing
    Column(Modifier.padding(horizontal = spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        SectionTitle(R.string.home_quick_actions)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            items(actions) { (action, label, icon) -> QuickAction(action, label, icon, onClick) }
        }
    }
}

@Composable
private fun QuickAction(action: HomeQuickAction, label: Int, icon: ImageVector, onClick: (HomeQuickAction) -> Unit) {
    PressScaleBox(onClick = { onClick(action) }, modifier = Modifier.width(128.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.sm),
        ) {
            Box(
                Modifier.size(62.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(27.dp))
            }
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun SongSection(title: Int, songs: List<SongDto>, onClick: (SongDto) -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Box(Modifier.padding(horizontal = spacing.lg)) { SectionTitle(title) }
        LazyRow(contentPadding = PaddingValues(horizontal = spacing.lg), horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            items(songs, key = { it.id }) { song -> SongCard(song) { onClick(song) } }
        }
    }
}

@Composable
private fun SongCard(song: SongDto, onClick: () -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    PressScaleBox(onClick, Modifier.width(140.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Box {
                AsyncImage(song.coverImageUrl, song.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium))
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(8.dp).size(34.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = .9f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PlayArrow, stringResource(R.string.play), modifier = Modifier.size(22.dp))
                }
            }
            Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PlaylistSection(title: Int, playlists: List<PlaylistDto>, onClick: (PlaylistDto) -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Box(Modifier.padding(horizontal = spacing.lg)) { SectionTitle(title) }
        LazyRow(contentPadding = PaddingValues(horizontal = spacing.lg), horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            items(playlists, key = { it.id }) { playlist ->
                PressScaleBox({ onClick(playlist) }, Modifier.width(164.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            IconButton(onClick = {}, modifier = Modifier.align(Alignment.TopEnd)) {
                                Icon(Icons.Rounded.MoreVert, null)
                            }
                        }
                        Column {
                            Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(R.string.home_song_count, playlist.songCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: Int) { Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

@Composable
private fun ErrorCard(onRetry: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_load_error), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(AriSamThemeTokens.spacing.sm))
            Button(onClick = onRetry) { Icon(Icons.Rounded.Refresh, null); Text(stringResource(R.string.retry)) }
        }
    }
}
