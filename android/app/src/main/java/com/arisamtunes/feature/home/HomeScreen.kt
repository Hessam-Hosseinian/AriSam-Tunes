package com.arisamtunes.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
        contentPadding = PaddingValues(top = spacing.md, bottom = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.xxl),
    ) {
        if (state.hasError) item { ErrorCard(onRetry) }
        if (state.trending.isNotEmpty()) item { SpotlightCarousel(state.trending, onSongClick) }
        item { QuickAccess(onQuickAction) }
        if (state.popular.isNotEmpty()) {
            item { ArtworkSongSection(R.string.home_most_popular, state.popular, onSongClick) }
        }
        if (state.newReleases.isNotEmpty()) {
            item { NewReleaseSection(state.newReleases, onSongClick) }
        }
        if (state.globalPlaylists.isNotEmpty()) {
            item { PlaylistSection(R.string.home_global_playlists, state.globalPlaylists, onPlaylistClick) }
        }
        if (state.localPlaylists.isNotEmpty()) {
            item { PlaylistSection(R.string.home_local_playlists, state.localPlaylists, onPlaylistClick) }
        }
    }
}

@Composable
private fun HomeLoadingSkeleton() {
    val spacing = AriSamThemeTokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xxl),
    ) {
        item { ShimmerBox(Modifier.fillMaxWidth().height(258.dp)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                ShimmerBox(Modifier.width(154.dp).height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    repeat(2) { ShimmerBox(Modifier.weight(1f).height(86.dp)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    repeat(2) { ShimmerBox(Modifier.weight(1f).height(86.dp)) }
                }
            }
        }
        items(2) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                ShimmerBox(Modifier.width(170.dp).height(24.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    items(3) { ShimmerBox(Modifier.width(164.dp).height(210.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SpotlightCarousel(songs: List<SongDto>, onSongClick: (SongDto) -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    val pages = songs.take(8)
    val pagerState = rememberPagerState(pageCount = { pages.size })
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = spacing.lg),
            pageSpacing = spacing.md,
        ) { page ->
            val song = pages[page]
            PressScaleBox(
                onClick = { onSongClick(song) },
                modifier = Modifier.fillMaxWidth().height(258.dp),
            ) {
                AsyncImage(
                    model = song.coverImageUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraLarge),
                )
                Box(
                    Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraLarge).background(
                        Brush.verticalGradient(
                            0f to Color(0xFF0369A1).copy(alpha = .08f),
                            .48f to Color(0xFF0C1821).copy(alpha = .18f),
                            1f to Color(0xFF0C1821).copy(alpha = .96f),
                        ),
                    ),
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(spacing.lg),
                    shape = CircleShape,
                    color = AriSamThemeTokens.tehranAmber,
                    contentColor = Color(0xFF0C1821),
                ) {
                    Text(
                        text = stringResource(R.string.home_trending_now),
                        modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        song.title,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        song.artistName,
                        color = Color.White.copy(alpha = .76f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(spacing.lg).size(54.dp)
                        .clip(CircleShape).background(AriSamThemeTokens.tehranAmber),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        stringResource(R.string.play),
                        tint = Color(0xFF0C1821),
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            pages.indices.forEach { index ->
                Box(
                    Modifier.padding(horizontal = 3.dp)
                        .width(if (index == pagerState.currentPage) 22.dp else 6.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
            }
        }
    }
}

@Composable
private fun QuickAccess(onClick: (HomeQuickAction) -> Unit) {
    val actions = listOf(
        QuickActionData(HomeQuickAction.Liked, R.string.home_liked_songs, Icons.Rounded.Favorite),
        QuickActionData(HomeQuickAction.Recent, R.string.home_recently_played, Icons.Rounded.History),
        QuickActionData(HomeQuickAction.Playlists, R.string.home_my_playlists, Icons.AutoMirrored.Rounded.QueueMusic),
        QuickActionData(HomeQuickAction.Artists, R.string.home_top_artists, Icons.Rounded.Groups),
    )
    val spacing = AriSamThemeTokens.spacing
    Column(
        modifier = Modifier.padding(horizontal = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        SectionTitle(R.string.home_quick_actions)
        actions.chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                rowActions.forEach { action ->
                    QuickAction(action, onClick, Modifier.weight(1f))
                }
            }
        }
    }
}

private data class QuickActionData(
    val action: HomeQuickAction,
    val label: Int,
    val icon: ImageVector,
)

@Composable
private fun QuickAction(
    data: QuickActionData,
    onClick: (HomeQuickAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AriSamThemeTokens.spacing
    PressScaleBox(onClick = { onClick(data.action) }, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().height(82.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                .padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = .12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(data.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
            }
            Text(
                stringResource(data.label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ArtworkSongSection(title: Int, songs: List<SongDto>, onClick: (SongDto) -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Box(Modifier.padding(horizontal = spacing.lg)) { SectionTitle(title) }
        LazyRow(
            contentPadding = PaddingValues(horizontal = spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            items(songs, key = { it.id }) { song -> ArtworkSongCard(song) { onClick(song) } }
        }
    }
}

@Composable
private fun ArtworkSongCard(song: SongDto, onClick: () -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    PressScaleBox(onClick, Modifier.width(164.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Box {
                AsyncImage(
                    song.coverImageUrl,
                    song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.large),
                )
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(spacing.sm).size(38.dp)
                        .clip(CircleShape).background(AriSamThemeTokens.tehranAmber),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        stringResource(R.string.play),
                        tint = Color(0xFF0C1821),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                song.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NewReleaseSection(songs: List<SongDto>, onClick: (SongDto) -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    Column(
        modifier = Modifier.padding(horizontal = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        SectionTitle(R.string.home_new_releases)
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(vertical = spacing.xs)) {
                songs.take(5).forEach { song ->
                    PressScaleBox(onClick = { onClick(song) }, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                song.coverImageUrl,
                                song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(58.dp).clip(MaterialTheme.shapes.medium),
                            )
                            Spacer(Modifier.width(spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    song.artistName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Box(
                                Modifier.size(38.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = .12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.PlayArrow, stringResource(R.string.play), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistSection(
    title: Int,
    playlists: List<PlaylistDto>,
    onClick: (PlaylistDto) -> Unit,
) {
    val spacing = AriSamThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Box(Modifier.padding(horizontal = spacing.lg)) { SectionTitle(title) }
        LazyRow(
            contentPadding = PaddingValues(horizontal = spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            items(playlists, key = { it.id }) { playlist ->
                PressScaleBox({ onClick(playlist) }, Modifier.width(180.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Box(
                            Modifier.fillMaxWidth().height(132.dp).clip(MaterialTheme.shapes.large)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primaryContainer,
                                            Color(0xFF0C1821),
                                        ),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!playlist.coverImageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    playlist.coverImageUrl,
                                    playlist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                Box(
                                    Modifier.fillMaxSize().background(
                                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .48f))),
                                    ),
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Rounded.QueueMusic,
                                    null,
                                    tint = Color.White.copy(alpha = .92f),
                                    modifier = Modifier.size(54.dp),
                                )
                            }
                            IconButton(onClick = {}, modifier = Modifier.align(Alignment.TopEnd)) {
                                Icon(Icons.Rounded.MoreVert, null, tint = Color.White)
                            }
                        }
                        Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            stringResource(R.string.home_song_count, playlist.songCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun ErrorCard(onRetry: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_load_error), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(AriSamThemeTokens.spacing.sm))
            Button(onClick = onRetry) {
                Icon(Icons.Rounded.Refresh, null)
                Text(stringResource(R.string.retry))
            }
        }
    }
}
