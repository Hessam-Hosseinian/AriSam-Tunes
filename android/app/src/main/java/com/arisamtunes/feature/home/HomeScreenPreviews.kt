package com.arisamtunes.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arisamtunes.core.design.preview.PreviewCatalogData
import com.arisamtunes.core.design.theme.AriSamTheme

@Preview(name = "Home - Loaded", showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AriSamTheme(darkTheme = true) {
        HomeScreen(
            state = HomeUiState(
                isLoading = false,
                trending = PreviewCatalogData.songs,
                popular = PreviewCatalogData.songs.reversed(),
                newReleases = PreviewCatalogData.songs,
                globalPlaylists = PreviewCatalogData.playlists,
                localPlaylists = PreviewCatalogData.playlists.take(1),
            ),
            onRetry = {},
            onSongClick = {},
            onPlaylistClick = {},
            onQuickAction = {},
        )
    }
}

@Preview(name = "Home - Loading", showBackground = true)
@Composable
private fun HomeLoadingPreview() {
    AriSamTheme(darkTheme = true) {
        HomeScreen(
            state = HomeUiState(),
            onRetry = {},
            onSongClick = {},
            onPlaylistClick = {},
            onQuickAction = {},
        )
    }
}
