package com.arisamtunes.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
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
import com.arisamtunes.core.design.preview.PreviewCatalogData
import com.arisamtunes.core.design.theme.AriSamTheme
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.catalog.SongDto
import kotlinx.coroutines.flow.flowOf

@Composable
fun SearchRoute(onSongClick: (SongDto) -> Unit, viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val results = viewModel.results.collectAsLazyPagingItems()
    SearchScreen(state, results, viewModel::updateQuery, viewModel::updateFilter, onSongClick)
}

@Composable
private fun SearchScreen(
    state: SearchUiState,
    results: LazyPagingItems<SongDto>,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onSongClick: (SongDto) -> Unit,
) {
    val spacing = AriSamThemeTokens.spacing
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
            singleLine = true,
            label = { Text(stringResource(R.string.search_music)) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Clear, stringResource(R.string.clear_search))
                }
            },
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            items(SearchFilter.entries) { filter ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(stringResource(filter.labelRes())) },
                )
            }
        }
        when {
            state.query.isBlank() -> SearchHint()
            results.loadState.refresh is LoadState.Loading -> LoadingBox()
            results.loadState.refresh is LoadState.Error -> ErrorBox(results::retry)
            results.itemCount == 0 -> EmptyBox()
            else -> SearchResults(results, onSongClick)
        }
    }
}

@Composable
private fun SearchResults(results: LazyPagingItems<SongDto>, onSongClick: (SongDto) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = AriSamThemeTokens.spacing.sm)) {
        items(results.itemCount, key = { index -> results.peek(index)?.id ?: index }) { index ->
            results[index]?.let { song ->
                PressScaleBox(onClick = { onSongClick(song) }, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(listOfNotNull(song.artistName, song.album).joinToString(" • "), maxLines = 1) },
                        leadingContent = {
                            AsyncImage(
                                model = song.coverImageUrl, contentDescription = song.title, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(58.dp).clip(MaterialTheme.shapes.medium),
                            )
                        },
                    )
                }
            }
        }
        if (results.loadState.append is LoadState.Loading) item { LoadingBox(modifier = Modifier.fillMaxWidth().padding(16.dp)) }
        if (results.loadState.append is LoadState.Error) item { ErrorBox(results::retry) }
    }
}

@Composable
private fun SearchHint() = CenterMessage(R.string.search_hint, true)
@Composable
private fun EmptyBox() = CenterMessage(R.string.search_empty, false)

@Composable
private fun CenterMessage(message: Int, showIcon: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showIcon) Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(message), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingBox(modifier: Modifier = Modifier.fillMaxSize()) = Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun ErrorBox(retry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        TextButton(onClick = retry) { Icon(Icons.Rounded.Refresh, null); Text(stringResource(R.string.retry_search)) }
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
    AriSamTheme {
        SearchScreen(
            state = SearchUiState(query = "tehran", filter = SearchFilter.All),
            results = results,
            onQueryChange = {},
            onFilterChange = {},
            onSongClick = {},
        )
    }
}

@Preview(name = "Search - Empty Query", showBackground = true)
@Composable
private fun SearchEmptyQueryPreview() {
    val results = flowOf(PagingData.empty<SongDto>()).collectAsLazyPagingItems()
    AriSamTheme {
        SearchScreen(
            state = SearchUiState(),
            results = results,
            onQueryChange = {},
            onFilterChange = {},
            onSongClick = {},
        )
    }
}
