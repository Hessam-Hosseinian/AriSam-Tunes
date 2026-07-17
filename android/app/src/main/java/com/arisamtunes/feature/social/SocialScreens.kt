package com.arisamtunes.feature.social

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.social.PublicUserDto

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
