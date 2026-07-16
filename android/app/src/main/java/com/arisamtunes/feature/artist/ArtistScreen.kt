package com.arisamtunes.feature.artist

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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.data.catalog.SongDto

private val ArtistBackground = Color(0xFF0C1821)
private val ArtistSurface = Color(0xFF1E2A3A)
private val ArtistAccent = Color(0xFFFFC857)
private val ArtistText = Color(0xFFF0F9FF)
private val ArtistSecondaryText = Color(0xFFBAE6FD)

@Composable
fun ArtistRoute(
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit,
    onSongClick: (String) -> Unit,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    ArtistScreen(state, onBack, onArtistClick, onSongClick, viewModel::load)
}

@Composable
fun ArtistScreen(
    state: ArtistUiState,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit,
    onSongClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val artist = state.artist
    if (artist == null) {
        ArtistMissingState(onBack)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ArtistBackground),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item { ArtistHero(artist, onBack) }
        artist.profile.bio?.takeIf(String::isNotBlank)?.let { bio -> item { ArtistBio(bio) } }
        item { ArtistAlbums(state, onSongClick, onRetry) }
        item { OtherArtists(artist.profile.id, state.artists, onArtistClick) }
    }
}

@Composable
private fun ArtistHero(artist: LocalArtistProfile, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
        AsyncImage(
            model = artist.imageUri,
            contentDescription = artist.profile.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, ArtistBackground))),
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(12.dp).clip(CircleShape).background(Color.Black.copy(alpha = .2f)),
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = Color.White)
        }
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.artist_verified), color = ArtistAccent, style = MaterialTheme.typography.labelLarge)
                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(ArtistAccent), contentAlignment = Alignment.Center) {
                    Text("✓", color = ArtistBackground, fontWeight = FontWeight.Bold)
                }
            }
            Text(artist.profile.name, color = ArtistText, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.artist_role), color = ArtistSecondaryText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ArtistBio(bio: String) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp).clip(RoundedCornerShape(24.dp)).background(ArtistSurface).padding(20.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.artist_about), color = ArtistText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(bio, color = ArtistSecondaryText, style = MaterialTheme.typography.bodyMedium, maxLines = 5, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ArtistAlbums(state: ArtistUiState, onSongClick: (String) -> Unit, onRetry: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.artist_albums), color = ArtistText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        when {
            state.isLoading -> Box(Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ArtistAccent, strokeWidth = 2.dp)
            }
            state.albums.isEmpty() -> ArtistAlbumsEmpty(state.hasCatalogError, onRetry)
            else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.albums, key = ArtistAlbum::name) { album -> ArtistAlbumCard(album, state.songs, onSongClick) }
            }
        }
    }
}

@Composable
private fun ArtistAlbumsEmpty(hasError: Boolean, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF132433)).border(1.dp, Color(0xFF0369A1), RoundedCornerShape(18.dp)).clickable(enabled = hasError, onClick = onRetry).padding(18.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(stringResource(if (hasError) R.string.artist_albums_error else R.string.artist_albums_empty), color = ArtistSecondaryText, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ArtistAlbumCard(album: ArtistAlbum, songs: List<SongDto>, onSongClick: (String) -> Unit) {
    val albumSong = songs.firstOrNull { it.album == album.name }
    PressScaleBox(onClick = { albumSong?.let { onSongClick(it.id) } }) {
        Column(modifier = Modifier.width(154.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
            AsyncImage(
                model = album.coverUrl.ifBlank { null },
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(154.dp).clip(RoundedCornerShape(20.dp)).background(ArtistSurface),
            )
            Text(album.name, color = ArtistText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.artist_album_meta, album.trackCount, album.releaseYear?.toString().orEmpty()), color = ArtistSecondaryText, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun OtherArtists(currentId: String, artists: List<LocalArtistProfile>, onArtistClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.artist_other_artists), color = ArtistText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
            modifier = Modifier.width(176.dp).clip(RoundedCornerShape(20.dp)).background(if (selected) Color(0xFF23475F) else ArtistSurface).border(if (selected) 1.5.dp else 0.dp, if (selected) ArtistAccent else Color.Transparent, RoundedCornerShape(20.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(model = artist.imageUri, contentDescription = artist.profile.name, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF595959)))
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(artist.profile.name, color = ArtistText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(stringResource(if (selected) R.string.artist_current else R.string.artist_open_profile), color = if (selected) ArtistAccent else ArtistSecondaryText, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ArtistMissingState(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(ArtistBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.MusicNote, null, tint = ArtistAccent, modifier = Modifier.size(42.dp))
            Text(stringResource(R.string.artist_missing), color = ArtistText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = ArtistSecondaryText) }
        }
    }
}
