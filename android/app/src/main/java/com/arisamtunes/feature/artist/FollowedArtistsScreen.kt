package com.arisamtunes.feature.artist

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
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.local.FollowedArtistsRepository
import com.arisamtunes.data.local.entity.FollowedArtistEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FollowedArtistsViewModel @Inject constructor(
    private val repository: FollowedArtistsRepository,
) : ViewModel() {
    val artists = repository.observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun unfollow(artist: FollowedArtistEntity) = viewModelScope.launch {
        repository.setFollowed(artist.artistId, artist.name, artist.imageUri, followed = false)
    }
}

@Composable
fun FollowedArtistsRoute(
    onBack: () -> Unit,
    onExplore: () -> Unit,
    onArtistClick: (String) -> Unit,
    viewModel: FollowedArtistsViewModel = hiltViewModel(),
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.background))),
        contentPadding = PaddingValues(bottom = AriSamThemeTokens.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.sm),
    ) {
        item(key = "followed_artists_header") {
            FollowedArtistsHeader(artists.size, onBack)
        }
        if (artists.isEmpty()) {
            item(key = "followed_artists_empty") { FollowedArtistsEmpty(onExplore) }
        } else {
            items(artists, key = FollowedArtistEntity::artistId) { artist ->
                FollowedArtistRow(
                    artist = artist,
                    onClick = { onArtistClick(artist.artistId) },
                    onUnfollow = { viewModel.unfollow(artist) },
                    modifier = Modifier
                        .padding(horizontal = AriSamThemeTokens.spacing.lg)
                        .animateItem(
                            fadeInSpec = tween(220),
                            placementSpec = tween(280),
                            fadeOutSpec = tween(180),
                        ),
                )
            }
        }
    }
}

@Composable
private fun FollowedArtistsHeader(count: Int, onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 38.dp, bottomEnd = 38.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF075985), Color(0xFF3B1E65))))
            .padding(16.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = .2f))) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = Color.White)
        }
        Column(
            Modifier.fillMaxWidth().padding(top = 58.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Groups, null, Modifier.size(38.dp), tint = Color.White)
            }
            Text(stringResource(R.string.followed_artists_title), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.followed_artists_subtitle), color = Color.White.copy(alpha = .74f), textAlign = TextAlign.Center)
            Text(stringResource(R.string.followed_artists_count, count), color = Color.White.copy(alpha = .68f), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun FollowedArtistRow(
    artist: FollowedArtistEntity,
    onClick: () -> Unit,
    onUnfollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PressScaleBox(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .72f)),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsyncImage(
                    model = artist.imageUri,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(62.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                )
                Column(Modifier.weight(1f)) {
                    Text(artist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.artist_role), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = onUnfollow) {
                    Icon(Icons.Rounded.PersonRemove, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.social_unfollow))
                }
            }
        }
    }
}

@Composable
private fun FollowedArtistsEmpty(onExplore: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 58.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(82.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFF38C6F4).copy(alpha = .12f)).border(1.dp, Color(0xFF38C6F4).copy(alpha = .3f), RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Groups, null, Modifier.size(40.dp), tint = Color(0xFF38C6F4))
        }
        Text(stringResource(R.string.followed_artists_empty), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(stringResource(R.string.followed_artists_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Button(onClick = onExplore) {
            Text(stringResource(R.string.start_exploring))
        }
    }
}
