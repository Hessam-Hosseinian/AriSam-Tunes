package com.arisamtunes.feature.social

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.catalog.PlaylistDto
import com.arisamtunes.data.social.PublicUserDto

@Composable
fun SocialProfileRoute(
    onBack: (() -> Unit)? = null,
    onFollowersClick: (String) -> Unit,
    onFollowingClick: (String) -> Unit,
    onPlaylistClick: (PlaylistDto) -> Unit,
    viewModel: SocialProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    when {
        state.isLoading -> Loading()
        state.hasError && state.user == null -> ErrorState(viewModel::refresh)
        else -> state.user?.let { user ->
            SocialProfileContent(
                user = user,
                playlists = state.playlists,
                isUpdatingFollow = state.isUpdatingFollow,
                isOwnProfile = state.isOwnProfile,
                onBack = onBack,
                onFollowClick = viewModel::toggleFollow,
                onFollowersClick = { onFollowersClick(user.id) },
                onFollowingClick = { onFollowingClick(user.id) },
                onPlaylistClick = onPlaylistClick,
            )
        } ?: EmptyState(R.string.social_profile_empty)
    }
}

@Composable
private fun SocialProfileContent(
    user: PublicUserDto,
    playlists: List<PlaylistDto>,
    isUpdatingFollow: Boolean,
    isOwnProfile: Boolean,
    onBack: (() -> Unit)?,
    onFollowClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onPlaylistClick: (PlaylistDto) -> Unit,
) {
    val spacing = AriSamThemeTokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                onBack?.let { IconButton(onClick = it) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } }
                Text(stringResource(R.string.social_profile), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth().padding(horizontal = spacing.lg)) {
                Column(Modifier.padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                        Avatar(user, Modifier.size(88.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(user.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (user.isPremium) Icon(Icons.Rounded.Verified, stringResource(R.string.premium_active), tint = MaterialTheme.colorScheme.primary)
                            }
                            user.bio?.takeIf(String::isNotBlank)?.let {
                                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        ElevatedAssistChip(onClick = onFollowersClick, label = { Text(stringResource(R.string.social_followers_count, user.followersCount)) }, leadingIcon = { Icon(Icons.Rounded.Groups, null) })
                        AssistChip(onClick = onFollowingClick, label = { Text(stringResource(R.string.social_following_count, user.followingCount)) })
                    }
                    if (!user.isFollowing && user.followersCount == 0L && user.followingCount == 0L) {
                        Text(stringResource(R.string.social_new_profile_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    if (isOwnProfile) {
                        Text(stringResource(R.string.social_own_profile_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    } else if (user.isFollowing) {
                        OutlinedButton(onClick = onFollowClick, enabled = !isUpdatingFollow, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.social_unfollow))
                        }
                    } else {
                        Button(onClick = onFollowClick, enabled = !isUpdatingFollow, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.social_follow))
                        }
                    }
                }
            }
        }
        item {
            Text(
                stringResource(R.string.social_public_playlists),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = spacing.lg),
            )
        }
        if (playlists.isEmpty()) {
            item { EmptyState(R.string.social_no_public_playlists) }
        } else {
            item {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier.fillMaxWidth().height(260.dp).padding(horizontal = spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    items(playlists, key = PlaylistDto::id) { playlist ->
                        PressScaleBox({ onPlaylistClick(playlist) }) { PlaylistCard(playlist) }
                    }
                }
            }
        }
    }
}

@Composable
fun SocialUsersRoute(
    onBack: () -> Unit,
    onUserClick: (PublicUserDto) -> Unit,
    viewModel: SocialUsersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
            Text(
                stringResource(if (state.title == SocialListKind.Followers) R.string.social_followers else R.string.social_following),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        when {
            state.isLoading -> Loading()
            state.hasError -> ErrorState(viewModel::refresh)
            state.users.isEmpty() -> EmptyState(R.string.social_users_empty)
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = AriSamThemeTokens.spacing.xl)) {
                items(state.users, key = PublicUserDto::id) { user ->
                    PressScaleBox({ onUserClick(user) }, Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(user.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(stringResource(R.string.social_followers_count, user.followersCount), maxLines = 1) },
                            leadingContent = { Avatar(user, Modifier.size(52.dp)) },
                            trailingContent = { if (user.isFollowing) Text(stringResource(R.string.social_following_badge), color = MaterialTheme.colorScheme.primary) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Avatar(user: PublicUserDto, modifier: Modifier) {
    if (!user.avatarUrl.isNullOrBlank()) {
        AsyncImage(user.avatarUrl, user.displayName, contentScale = ContentScale.Crop, modifier = modifier.clip(MaterialTheme.shapes.extraLarge))
    } else {
        Box(
            modifier.clip(MaterialTheme.shapes.extraLarge).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp)) }
    }
}

@Composable
private fun PlaylistCard(playlist: PlaylistDto) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!playlist.coverImageUrl.isNullOrBlank()) {
            AsyncImage(playlist.coverImageUrl, playlist.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.large))
        } else {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.large)
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary))),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(42.dp)) }
        }
        Text(playlist.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(stringResource(R.string.home_song_count, playlist.songCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Loading() = Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun ErrorState(onRetry: () -> Unit) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    TextButton(onClick = onRetry) { Icon(Icons.Rounded.Refresh, null); Text(stringResource(R.string.retry)) }
}

@Composable
private fun EmptyState(message: Int) = Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.Groups, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(message), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
