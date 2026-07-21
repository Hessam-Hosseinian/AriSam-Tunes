package com.arisamtunes.feature.artist

import com.arisamtunes.core.design.spacing.AriSamDimensions
import com.arisamtunes.core.design.colors.AriSamPalette
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.data.catalog.SongDto

private val ArtistAccent = AriSamPalette.amberAccent

@Composable
fun ArtistRoute(
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit,
    onSongClick: (String) -> Unit,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ArtistScreen(state, onBack, onArtistClick, onSongClick, viewModel::load, viewModel::toggleFollow)
}

@Composable
fun ArtistScreen(
    state: ArtistUiState,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit,
    onSongClick: (String) -> Unit,
    onRetry: () -> Unit,
    onToggleFollow: () -> Unit,
) {
    val artist = state.artist
    if (artist == null) {
        ArtistMissingState(onBack)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = AriSamDimensions.dp32),
    ) {
        item { ArtistHero(artist, state.isFollowed, onBack, onToggleFollow) }
        artist.profile.bio?.takeIf(String::isNotBlank)?.let { bio -> item { ArtistBio(bio) } }
        item { ArtistAlbums(state, onSongClick, onRetry) }
        item { OtherArtists(artist.profile.id, state.artists, onArtistClick) }
    }
}

@Composable
private fun ArtistHero(artist: LocalArtistProfile, isFollowed: Boolean, onBack: () -> Unit, onToggleFollow: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(AriSamDimensions.dp360)) {
        AsyncImage(
            model = artist.imageUri,
            contentDescription = artist.profile.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(AriSamPalette.transparent, MaterialTheme.colorScheme.background))),
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(AriSamDimensions.dp12).clip(CircleShape).background(AriSamPalette.black.copy(alpha = .2f)),
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = AriSamPalette.white)
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = AriSamDimensions.dp24, vertical = AriSamDimensions.dp22),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp8)) {
                Text(stringResource(R.string.artist_verified), color = ArtistAccent, style = MaterialTheme.typography.labelLarge)
                Box(modifier = Modifier.size(AriSamDimensions.dp22).clip(CircleShape).background(ArtistAccent), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.artist_verified_symbol), color = AriSamPalette.darkBackground, fontWeight = FontWeight.Bold)
                }
            }
            Text(artist.profile.name, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.artist_role), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            FilledTonalButton(
                onClick = onToggleFollow,
                modifier = Modifier.padding(top = AriSamDimensions.dp10),
            ) {
                Icon(if (isFollowed) Icons.Rounded.Check else Icons.Rounded.PersonAdd, null)
                Spacer(Modifier.width(AriSamDimensions.dp7))
                Text(stringResource(if (isFollowed) R.string.artist_following else R.string.artist_follow))
            }
        }
    }
}

@Composable
private fun ArtistBio(bio: String) {
    Column(
        modifier = Modifier.padding(horizontal = AriSamDimensions.dp20, vertical = AriSamDimensions.dp18).clip(RoundedCornerShape(AriSamDimensions.dp24)).background(MaterialTheme.colorScheme.surfaceContainer).padding(AriSamDimensions.dp20),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp8),
    ) {
        Text(stringResource(R.string.artist_about), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(bio, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 5, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ArtistAlbums(state: ArtistUiState, onSongClick: (String) -> Unit, onRetry: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = AriSamDimensions.dp20), verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp12)) {
        Text(stringResource(R.string.artist_albums), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        when {
            state.isLoading -> Box(Modifier.fillMaxWidth().height(AriSamDimensions.dp112), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ArtistAccent, strokeWidth = AriSamDimensions.dp2)
            }
            state.albums.isEmpty() -> ArtistAlbumsEmpty(state.hasCatalogError, onRetry)
            else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp12)) {
                items(state.albums, key = ArtistAlbum::name) { album -> ArtistAlbumCard(album, state.songs, onSongClick) }
            }
        }
    }
}

@Composable
private fun ArtistAlbumsEmpty(hasError: Boolean, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(AriSamDimensions.dp18)).background(MaterialTheme.colorScheme.surfaceContainer).border(AriSamDimensions.dp1, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(AriSamDimensions.dp18)).clickable(enabled = hasError, onClick = onRetry).padding(AriSamDimensions.dp18),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(stringResource(if (hasError) R.string.artist_albums_error else R.string.artist_albums_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ArtistAlbumCard(album: ArtistAlbum, songs: List<SongDto>, onSongClick: (String) -> Unit) {
    val albumSong = songs.firstOrNull { it.album == album.name }
    PressScaleBox(onClick = { albumSong?.let { onSongClick(it.id) } }) {
        Column(modifier = Modifier.width(AriSamDimensions.dp154), verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp8), horizontalAlignment = Alignment.Start) {
            AsyncImage(
                model = album.coverUrl.ifBlank { null },
                contentDescription = album.name,
                error = painterResource(R.drawable.arisam_app_icon_dark),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(AriSamDimensions.dp154).clip(RoundedCornerShape(AriSamDimensions.dp20)).background(MaterialTheme.colorScheme.surfaceContainer),
            )
            Text(album.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.artist_album_meta, album.trackCount, album.releaseYear?.toString().orEmpty()), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun OtherArtists(currentId: String, artists: List<LocalArtistProfile>, onArtistClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(top = AriSamDimensions.dp28), verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp12)) {
        Text(stringResource(R.string.artist_other_artists), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = AriSamDimensions.dp20))
        LazyRow(contentPadding = PaddingValues(horizontal = AriSamDimensions.dp20), horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp12)) {
            items(artists, key = { it.profile.id }) { artist ->
                ArtistBrowserCard(artist, artist.profile.id == currentId) { onArtistClick(artist.profile.id) }
            }
        }
    }
}

@Composable
private fun ArtistBrowserCard(artist: LocalArtistProfile, selected: Boolean, onClick: () -> Unit) {
    PressScaleBox(onClick = onClick) {
        Row(
            modifier = Modifier.width(AriSamDimensions.dp176).clip(RoundedCornerShape(AriSamDimensions.dp20)).background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer).border(if (selected) AriSamDimensions.dp1_5 else AriSamDimensions.dp0, if (selected) ArtistAccent else AriSamPalette.transparent, RoundedCornerShape(AriSamDimensions.dp20)).padding(AriSamDimensions.dp12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10),
        ) {
            AsyncImage(model = artist.imageUri, contentDescription = artist.profile.name, contentScale = ContentScale.Crop, modifier = Modifier.size(AriSamDimensions.dp48).clip(CircleShape).background(AriSamPalette.neutral600))
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(artist.profile.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(stringResource(if (selected) R.string.artist_current else R.string.artist_open_profile), color = if (selected) ArtistAccent else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ArtistMissingState(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp12)) {
            Icon(Icons.Rounded.MusicNote, null, tint = ArtistAccent, modifier = Modifier.size(AriSamDimensions.dp42))
            Text(stringResource(R.string.artist_missing), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
