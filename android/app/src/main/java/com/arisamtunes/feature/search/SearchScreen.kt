package com.arisamtunes.feature.search

import com.arisamtunes.core.design.spacing.AriSamDimensions
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.components.ShimmerBox
import com.arisamtunes.core.design.preview.PreviewCatalogData
import com.arisamtunes.core.design.theme.AriSamTheme
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.local.SearchHistoryEntry
import java.io.IOException
import kotlinx.coroutines.flow.flowOf

@Composable
fun SearchRoute(
    onSongClick: (SongDto) -> Unit,
    onArtistClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val results = viewModel.results.collectAsLazyPagingItems()
    SearchScreen(
        state = state,
        results = results,
        onQueryChange = viewModel::updateQuery,
        onFilterChange = viewModel::updateFilter,
        onRecentSearchClick = viewModel::selectRecentSearch,
        onRecentSearchDelete = viewModel::deleteRecentSearch,
        onRecentSearchesClear = viewModel::clearRecentSearches,
        onSongClick = onSongClick,
        onArtistClick = onArtistClick,
    )
}

@Composable
private fun SearchScreen(
    state: SearchUiState,
    results: LazyPagingItems<SongDto>,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onRecentSearchClick: (SearchHistoryEntry) -> Unit,
    onRecentSearchDelete: (Long) -> Unit,
    onRecentSearchesClear: () -> Unit,
    onSongClick: (SongDto) -> Unit,
    onArtistClick: (String) -> Unit,
) {
    val spacing = AriSamThemeTokens.spacing
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.md, bottom = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg)
                    .height(AriSamDimensions.dp60),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Clear, stringResource(R.string.clear_search))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(AriSamDimensions.dp18),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )
            SearchFilterRail(state.filter, onFilterChange)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize(),
        ) {
            when {
                state.query.isBlank() -> SearchStartState(
                    recentSearches = state.recentSearches,
                    onSearchClick = onRecentSearchClick,
                    onSearchDelete = onRecentSearchDelete,
                    onClear = onRecentSearchesClear,
                )
                results.loadState.refresh is LoadState.Loading -> SearchLoadingState()
                results.loadState.refresh is LoadState.Error && state.artists.isEmpty() -> {
                    val error = (results.loadState.refresh as LoadState.Error).error
                    SearchErrorState(results::retry, isOffline = error.isOfflineFailure())
                }
                results.itemCount == 0 && state.artists.isEmpty() -> SearchEmptyState { onQueryChange("") }
                else -> SearchResults(state.artists, results, onSongClick, onArtistClick)
            }
        }
    }
}

@Composable
private fun SearchFilterRail(selectedFilter: SearchFilter, onFilterChange: (SearchFilter) -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    LazyRow(
        contentPadding = PaddingValues(horizontal = spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        items(SearchFilter.entries, key = { it.name }) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = {
                    Text(
                        stringResource(filter.labelRes()),
                        fontWeight = if (selectedFilter == filter) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedFilter == filter,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun SearchResults(
    artists: List<SearchArtistResult>,
    results: LazyPagingItems<SongDto>,
    onSongClick: (SongDto) -> Unit,
    onArtistClick: (String) -> Unit,
) {
    val spacing = AriSamThemeTokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, top = spacing.md, bottom = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item {
            Text(
                text = stringResource(R.string.search_results_count, results.itemCount + artists.size),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = spacing.xs),
            )
        }
        if (artists.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.search_filter_artists),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = spacing.xs),
                )
            }
            items(artists, key = { "artist-${it.id}" }) { artist ->
                SearchArtistRow(artist) { onArtistClick(artist.id) }
            }
        }
        items(results.itemCount, key = { index -> results.peek(index)?.id ?: index }) { index ->
            results[index]?.let { song -> SearchResultRow(song, onSongClick) }
        }
        if (results.loadState.append is LoadState.Loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(spacing.md), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(AriSamDimensions.dp24), color = MaterialTheme.colorScheme.primary, strokeWidth = AriSamDimensions.dp2)
                }
            }
        }
        if (results.loadState.append is LoadState.Error) {
            item { SearchInlineRetry(results::retry) }
        }
    }
}

@Composable
private fun SearchArtistRow(artist: SearchArtistResult, onClick: () -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    PressScaleBox(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AriSamDimensions.dp18))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(AriSamDimensions.dp1, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(AriSamDimensions.dp18))
                .padding(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = artist.imageUrl,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.arisam_app_icon_dark),
                error = painterResource(R.drawable.arisam_app_icon_dark),
                modifier = Modifier.size(AriSamDimensions.dp62).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
            Spacer(Modifier.width(spacing.md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp3)) {
                Text(
                    artist.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.artist_open_profile),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Box(
                modifier = Modifier.size(AriSamDimensions.dp40).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun SearchResultRow(song: SongDto, onSongClick: (SongDto) -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    PressScaleBox(onClick = { onSongClick(song) }, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AriSamDimensions.dp18))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(AriSamDimensions.dp1, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(AriSamDimensions.dp18))
                .padding(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = song.coverImageUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.arisam_app_icon_dark),
                error = painterResource(R.drawable.arisam_app_icon_dark),
                modifier = Modifier.size(AriSamDimensions.dp62).clip(RoundedCornerShape(AriSamDimensions.dp14)),
            )
            Spacer(Modifier.width(spacing.md))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp3)) {
                Text(
                    song.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(song.artistName, song.album).joinToString(" • "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(spacing.sm))
            Box(
                modifier = Modifier
                    .size(AriSamDimensions.dp40)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, stringResource(R.string.play), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun SearchStartState(
    recentSearches: List<SearchHistoryEntry>,
    onSearchClick: (SearchHistoryEntry) -> Unit,
    onSearchDelete: (Long) -> Unit,
    onClear: () -> Unit,
) {
    if (recentSearches.isEmpty()) {
        SearchMessage(
            icon = Icons.Rounded.Search,
            title = R.string.search_start_title,
            body = R.string.search_hint,
        )
        return
    }

    val spacing = AriSamThemeTokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.search_history_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.search_history_clear), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        items(recentSearches, key = SearchHistoryEntry::id) { entry ->
            PressScaleBox(onClick = { onSearchClick(entry) }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AriSamDimensions.dp16))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(AriSamDimensions.dp1, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(AriSamDimensions.dp16))
                        .padding(start = spacing.md, top = spacing.sm, bottom = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(spacing.md))
                    Text(
                        text = entry.query,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = { onSearchDelete(entry.id) }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.search_history_remove),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyState(onClearSearch: () -> Unit) {
    SearchMessage(
        icon = Icons.Rounded.MusicNote,
        title = R.string.search_empty_title,
        body = R.string.search_empty,
        actionLabel = R.string.clear_search,
        onAction = onClearSearch,
    )
}

@Composable
private fun SearchMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: Int,
    body: Int,
    actionLabel: Int? = null,
    onAction: (() -> Unit)? = null,
) {
    val spacing = AriSamThemeTokens.spacing
    Box(Modifier.fillMaxSize().padding(spacing.xl), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(AriSamDimensions.dp72)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(AriSamDimensions.dp1, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(AriSamDimensions.dp34))
            }
            Spacer(Modifier.height(spacing.xs))
            Text(
                stringResource(title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) {
                    Text(stringResource(actionLabel))
                }
            }
        }
    }
}

@Composable
private fun SearchLoadingState() {
    val spacing = AriSamThemeTokens.spacing
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        repeat(6) { ShimmerBox(Modifier.fillMaxWidth().height(AriSamDimensions.dp80)) }
    }
}

@Composable
private fun SearchErrorState(retry: () -> Unit, isOffline: Boolean) {
    val spacing = AriSamThemeTokens.spacing
    Box(Modifier.fillMaxSize().padding(spacing.xl), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Icon(
                if (isOffline) Icons.Rounded.WifiOff else Icons.Rounded.ErrorOutline,
                null,
                tint = if (isOffline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(AriSamDimensions.dp42),
            )
            Text(
                stringResource(if (isOffline) R.string.error_offline_title else R.string.search_error_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = retry,
                colors = ButtonDefaults.buttonColors(),
            ) {
                Icon(Icons.Rounded.Refresh, null)
                Spacer(Modifier.width(spacing.sm))
                Text(stringResource(R.string.retry_search))
            }
        }
    }
}

private fun Throwable.isOfflineFailure(): Boolean = generateSequence(this) { it.cause }
    .any { it is IOException }

@Composable
private fun SearchInlineRetry(retry: () -> Unit) {
    Button(
        onClick = retry,
        colors = ButtonDefaults.buttonColors(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Rounded.Refresh, null)
        Spacer(Modifier.width(AriSamDimensions.dp8))
        Text(stringResource(R.string.retry_search))
    }
}

private fun SearchFilter.labelRes() = when (this) {
    SearchFilter.All -> R.string.search_filter_all
    SearchFilter.Song -> R.string.search_filter_songs
    SearchFilter.Artist -> R.string.search_filter_artists
    SearchFilter.Album -> R.string.search_filter_albums
    SearchFilter.Genre -> R.string.search_filter_genres
}

@Preview(name = "Search - Results", showBackground = true)
@Composable
private fun SearchResultsPreview() {
    val results = flowOf(PagingData.from(PreviewCatalogData.songs)).collectAsLazyPagingItems()
    AriSamTheme(darkTheme = true) {
        SearchScreen(
            state = SearchUiState(query = "tehran", filter = SearchFilter.All),
            results = results,
            onQueryChange = {},
            onFilterChange = {},
            onRecentSearchClick = {},
            onRecentSearchDelete = {},
            onRecentSearchesClear = {},
            onSongClick = {},
            onArtistClick = {},
        )
    }
}

@Preview(name = "Search - Empty Query", showBackground = true)
@Composable
private fun SearchEmptyQueryPreview() {
    val results = flowOf(PagingData.empty<SongDto>()).collectAsLazyPagingItems()
    AriSamTheme(darkTheme = true) {
        SearchScreen(
            state = SearchUiState(),
            results = results,
            onQueryChange = {},
            onFilterChange = {},
            onRecentSearchClick = {},
            onRecentSearchDelete = {},
            onRecentSearchesClear = {},
            onSongClick = {},
            onArtistClick = {},
        )
    }
}
