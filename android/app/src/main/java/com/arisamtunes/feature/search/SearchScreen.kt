package com.arisamtunes.feature.search

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
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import kotlinx.coroutines.flow.flowOf

private val SearchBackground = Brush.verticalGradient(
    listOf(Color(0xFF081721), Color(0xFF0B2230), Color(0xFF102B3B)),
)

@Composable
fun SearchRoute(
    onSongClick: (SongDto) -> Unit,
    onArtistClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val results = viewModel.results.collectAsLazyPagingItems()
    SearchScreen(state, results, viewModel::updateQuery, viewModel::updateFilter, onSongClick, onArtistClick)
}

@Composable
private fun SearchScreen(
    state: SearchUiState,
    results: LazyPagingItems<SongDto>,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onSongClick: (SongDto) -> Unit,
    onArtistClick: (String) -> Unit,
) {
    val spacing = AriSamThemeTokens.spacing
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SearchBackground),
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
                    .height(60.dp),
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
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0A1D29),
                    unfocusedContainerColor = Color(0xFF0A1D29).copy(alpha = .84f),
                    focusedBorderColor = Color(0xFF0797DB),
                    unfocusedBorderColor = Color.White.copy(alpha = .14f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLeadingIconColor = Color(0xFF8ED8FF),
                    unfocusedLeadingIconColor = Color.White.copy(alpha = .54f),
                    focusedTrailingIconColor = Color(0xFF8ED8FF),
                    unfocusedTrailingIconColor = Color.White.copy(alpha = .54f),
                    focusedPlaceholderColor = Color.White.copy(alpha = .42f),
                    unfocusedPlaceholderColor = Color.White.copy(alpha = .42f),
                    cursorColor = Color(0xFF8ED8FF),
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
                state.query.isBlank() -> SearchStartState()
                results.loadState.refresh is LoadState.Loading -> SearchLoadingState()
                results.loadState.refresh is LoadState.Error && state.artists.isEmpty() -> SearchErrorState(results::retry)
                results.itemCount == 0 && state.artists.isEmpty() -> SearchEmptyState()
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
                    containerColor = Color(0xFF0A1D29).copy(alpha = .72f),
                    labelColor = Color.White.copy(alpha = .62f),
                    selectedContainerColor = Color(0xFF0797DB),
                    selectedLabelColor = Color.White,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedFilter == filter,
                    borderColor = Color.White.copy(alpha = .12f),
                    selectedBorderColor = Color(0xFF8ED8FF).copy(alpha = .58f),
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
                color = Color.White.copy(alpha = .58f),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = spacing.xs),
            )
        }
        if (artists.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.search_filter_artists),
                    color = Color.White,
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
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF8ED8FF), strokeWidth = 2.dp)
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
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF102B3B))
                .border(1.dp, Color(0xFF8ED8FF).copy(alpha = .18f), RoundedCornerShape(18.dp))
                .padding(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = artist.imageUrl,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(62.dp).clip(CircleShape).background(Color(0xFF0A1D29)),
            )
            Spacer(Modifier.width(spacing.md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    artist.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.artist_open_profile),
                    color = Color.White.copy(alpha = .56f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF0797DB).copy(alpha = .18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Person, null, tint = Color(0xFF8ED8FF))
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
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF0A1D29).copy(alpha = .88f))
                .border(1.dp, Color.White.copy(alpha = .1f), RoundedCornerShape(18.dp))
                .padding(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = song.coverImageUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(62.dp).clip(RoundedCornerShape(14.dp)),
            )
            Spacer(Modifier.width(spacing.md))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    song.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(song.artistName, song.album).joinToString(" • "),
                    color = Color.White.copy(alpha = .56f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(spacing.sm))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0797DB).copy(alpha = .18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, stringResource(R.string.play), tint = Color(0xFF8ED8FF))
            }
        }
    }
}

@Composable
private fun SearchStartState() {
    SearchMessage(
        icon = Icons.Rounded.Search,
        title = R.string.search_start_title,
        body = R.string.search_hint,
    )
}

@Composable
private fun SearchEmptyState() {
    SearchMessage(
        icon = Icons.Rounded.MusicNote,
        title = R.string.search_empty_title,
        body = R.string.search_empty,
    )
}

@Composable
private fun SearchMessage(icon: androidx.compose.ui.graphics.vector.ImageVector, title: Int, body: Int) {
    val spacing = AriSamThemeTokens.spacing
    Box(Modifier.fillMaxSize().padding(spacing.xl), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0797DB).copy(alpha = .14f))
                    .border(1.dp, Color(0xFF8ED8FF).copy(alpha = .24f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = Color(0xFF8ED8FF), modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(spacing.xs))
            Text(
                stringResource(title),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(body),
                color = Color.White.copy(alpha = .54f),
                style = MaterialTheme.typography.bodyMedium,
            )
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
        repeat(6) { ShimmerBox(Modifier.fillMaxWidth().height(80.dp)) }
    }
}

@Composable
private fun SearchErrorState(retry: () -> Unit) {
    val spacing = AriSamThemeTokens.spacing
    Box(Modifier.fillMaxSize().padding(spacing.xl), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Icon(Icons.Rounded.ErrorOutline, null, tint = Color(0xFFFB7185), modifier = Modifier.size(42.dp))
            Text(
                stringResource(R.string.search_error_title),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = retry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0797DB), contentColor = Color.White),
            ) {
                Icon(Icons.Rounded.Refresh, null)
                Spacer(Modifier.width(spacing.sm))
                Text(stringResource(R.string.retry_search))
            }
        }
    }
}

@Composable
private fun SearchInlineRetry(retry: () -> Unit) {
    Button(
        onClick = retry,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0797DB), contentColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Rounded.Refresh, null)
        Spacer(Modifier.width(8.dp))
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
            onSongClick = {},
            onArtistClick = {},
        )
    }
}
