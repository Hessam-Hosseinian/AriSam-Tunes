package com.arisamtunes.feature.social

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
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
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
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
import com.arisamtunes.data.social.PublicUserDto

@Composable
fun SocialProfileRoute(
    onBack: (() -> Unit)? = null,
    onFollowersClick: (String) -> Unit,
    onFollowingClick: (String) -> Unit,
    onPlaylistClick: (PlaylistDto) -> Unit,
    onMessageClick: (String) -> Unit,
    viewModel: SocialProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val playlists = viewModel.playlists.collectAsLazyPagingItems()
    val snackbar = remember { SnackbarHostState() }
    val actionFailed = stringResource(R.string.social_action_failed)
    val avatarUpdated = stringResource(R.string.profile_photo_updated)
    val avatarUploadFailed = stringResource(R.string.profile_photo_upload_failed)
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::uploadAvatar)
    }
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            snackbar.showSnackbar(
                when (effect) {
                    SocialEffect.ActionFailed -> actionFailed
                    SocialEffect.AvatarUpdated -> avatarUpdated
                    SocialEffect.AvatarUploadFailed -> avatarUploadFailed
                },
            )
        }
    }
    Box(Modifier.fillMaxSize()) {
    when {
        state.isLoading -> Loading()
        state.hasError && state.user == null -> ErrorState(viewModel::refresh)
        else -> state.user?.let { user ->
            SocialProfileContent(
                user = user,
                playlists = playlists,
                isUpdatingFollow = state.isUpdatingFollow,
                isUploadingAvatar = state.isUploadingAvatar,
                isOwnProfile = state.isOwnProfile,
                onBack = onBack,
                onFollowClick = viewModel::toggleFollow,
                onFollowersClick = { onFollowersClick(user.id) },
                onFollowingClick = { onFollowingClick(user.id) },
                onPlaylistClick = onPlaylistClick,
                onMessageClick = { onMessageClick(user.id) },
                onAvatarClick = {
                    avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
        } ?: EmptyState(R.string.social_profile_empty)
    }
    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SocialProfileContent(
    user: PublicUserDto,
    playlists: LazyPagingItems<PlaylistDto>,
    isUpdatingFollow: Boolean,
    isUploadingAvatar: Boolean,
    isOwnProfile: Boolean,
    onBack: (() -> Unit)?,
    onFollowClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onPlaylistClick: (PlaylistDto) -> Unit,
    onMessageClick: () -> Unit,
    onAvatarClick: () -> Unit,
) {
    val spacing = AriSamThemeTokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.background)),
        ),
        contentPadding = PaddingValues(bottom = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item {
            ProfileHero(user, onBack, isOwnProfile, isUploadingAvatar, onAvatarClick)
        }
        item {
            ProfileStats(user, onFollowersClick, onFollowingClick)
        }
        item {
            Column(Modifier.padding(horizontal = spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                user.bio?.takeIf(String::isNotBlank)?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
                if (isOwnProfile) {
                    Text(stringResource(R.string.social_own_profile_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                } else {
                    if (user.isFollowing) {
                        OutlinedButton(onClick = onFollowClick, enabled = !isUpdatingFollow, modifier = Modifier.fillMaxWidth().height(50.dp), shape = MaterialTheme.shapes.large) {
                            Text(stringResource(R.string.social_unfollow))
                        }
                    } else {
                        Button(onClick = onFollowClick, enabled = !isUpdatingFollow, modifier = Modifier.fillMaxWidth().height(50.dp), shape = MaterialTheme.shapes.large) {
                            if (isUpdatingFollow) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text(stringResource(R.string.social_follow), fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(onClick = onMessageClick, modifier = Modifier.fillMaxWidth().height(50.dp), shape = MaterialTheme.shapes.large) {
                        Icon(Icons.Rounded.Chat, null)
                        Text(stringResource(R.string.chat_tap_to_message))
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.social_public_playlists), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text(pluralStringResource(R.plurals.social_playlist_count, playlists.itemCount, playlists.itemCount), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        if (playlists.loadState.refresh is LoadState.Loading) {
            items(4) { ShimmerBox(Modifier.fillMaxWidth().padding(horizontal = spacing.lg).height(180.dp)) }
        } else if (playlists.itemCount == 0) {
            item { EmptyState(R.string.social_no_public_playlists) }
        } else {
            items((playlists.itemCount + 1) / 2) { rowIndex ->
                val firstIndex = rowIndex * 2
                val secondIndex = firstIndex + 1
                val rowPlaylists = listOfNotNull(
                    playlists[firstIndex],
                    if (secondIndex < playlists.itemCount) playlists[secondIndex] else null,
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = spacing.lg), horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    rowPlaylists.forEach { playlist ->
                        PressScaleBox({ onPlaylistClick(playlist) }, Modifier.weight(1f)) { PlaylistCard(playlist) }
                    }
                    if (rowPlaylists.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProfileHero(
    user: PublicUserDto,
    onBack: (() -> Unit)?,
    isOwnProfile: Boolean,
    isUploadingAvatar: Boolean,
    onAvatarClick: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(280.dp)) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.background))))
        Box(Modifier.size(190.dp).align(Alignment.TopEnd).background(MaterialTheme.colorScheme.primary.copy(alpha = .08f), CircleShape))
        Box(Modifier.size(130.dp).align(Alignment.BottomStart).background(MaterialTheme.colorScheme.secondary.copy(alpha = .1f), CircleShape))
        onBack?.let {
            Surface(Modifier.padding(12.dp).size(42.dp), CircleShape, Color.Black.copy(alpha = .25f)) {
                IconButton(onClick = it) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = Color.White) }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                if (isOwnProfile) {
                    PressScaleBox(onAvatarClick, enabled = !isUploadingAvatar) {
                        Avatar(user, Modifier.size(118.dp).border(4.dp, Color.White.copy(alpha = .92f), CircleShape))
                    }
                } else {
                    Avatar(user, Modifier.size(118.dp).border(4.dp, Color.White.copy(alpha = .92f), CircleShape))
                }
                if (isUploadingAvatar) {
                    Box(
                        Modifier.size(118.dp).clip(CircleShape).background(Color.Black.copy(alpha = .52f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(Modifier.size(30.dp), color = Color.White, strokeWidth = 3.dp)
                    }
                } else if (isOwnProfile) {
                    Surface(
                        onClick = onAvatarClick,
                        modifier = Modifier.align(Alignment.BottomStart).size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        border = androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.surface),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PhotoCamera, stringResource(R.string.profile_change_photo), Modifier.size(18.dp))
                        }
                    }
                }
                if (user.isPremium) {
                    Surface(Modifier.align(Alignment.BottomEnd).size(32.dp), CircleShape, AriSamThemeTokens.tehranAmber, border = androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.surface)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Verified, stringResource(R.string.premium_active), tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
            Text(user.displayName, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (isOwnProfile) {
                Text(
                    stringResource(if (isUploadingAvatar) R.string.profile_photo_uploading else R.string.profile_change_photo),
                    color = Color.White.copy(alpha = .72f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun ProfileStats(user: PublicUserDto, onFollowersClick: () -> Unit, onFollowingClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ProfileStat(user.followersCount, stringResource(R.string.social_followers), onFollowersClick)
            Box(Modifier.width(1.dp).height(42.dp).background(MaterialTheme.colorScheme.outlineVariant))
            ProfileStat(user.followingCount, stringResource(R.string.social_following), onFollowingClick)
        }
    }
}

@Composable
private fun ProfileStat(value: Long, label: String, onClick: () -> Unit) {
    PressScaleBox(onClick) {
        Column(Modifier.padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(label, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
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
    val users = viewModel.users.collectAsLazyPagingItems()
    val snackbar = remember { SnackbarHostState() }
    val actionFailed = stringResource(R.string.social_action_failed)
    LaunchedEffect(Unit) { viewModel.effects.collect { snackbar.showSnackbar(actionFailed) } }
    Box(Modifier.fillMaxSize()) {
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
            users.loadState.refresh is LoadState.Loading -> Loading()
            users.loadState.refresh is LoadState.Error -> ErrorState(users::retry)
            users.itemCount == 0 -> EmptyState(R.string.social_users_empty)
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = AriSamThemeTokens.spacing.xl)) {
                items(users.itemCount, key = { users.peek(it)?.id ?: it }) { index ->
                    val user = users[index] ?: return@items
                    PressScaleBox({ onUserClick(user) }, Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(user.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(stringResource(R.string.social_followers_count, user.followersCount), maxLines = 1) },
                            leadingContent = { Avatar(user, Modifier.size(52.dp)) },
                            trailingContent = {
                                TextButton(onClick = { viewModel.toggleFollow(user) }, enabled = user.id !in state.updatingUserIds) {
                                    if (user.id in state.updatingUserIds) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    else Text(stringResource(if (user.isFollowing) R.string.social_unfollow else R.string.social_follow))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Avatar(user: PublicUserDto, modifier: Modifier) {
    if (!user.avatarUrl.isNullOrBlank()) {
        AsyncImage(user.avatarUrl, user.displayName, contentScale = ContentScale.Crop, modifier = modifier.clip(CircleShape))
    } else {
        Box(
            modifier.clip(CircleShape).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp)) }
    }
}

@Composable
private fun PlaylistCard(playlist: PlaylistDto) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!playlist.coverImageUrl.isNullOrBlank()) {
            AsyncImage(playlist.coverImageUrl, playlist.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.large))
        } else {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.large)
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary))),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(42.dp)) }
        }
        Text(playlist.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(stringResource(R.string.home_song_count, playlist.songCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
      }
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
