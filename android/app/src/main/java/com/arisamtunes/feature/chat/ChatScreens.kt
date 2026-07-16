package com.arisamtunes.feature.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.components.ShimmerBox
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.chat.ChatConnectionStatus
import com.arisamtunes.data.chat.ChatConversationDto
import com.arisamtunes.data.chat.ChatMessageDto
import com.arisamtunes.data.chat.ChatMessageStatusDto
import com.arisamtunes.data.chat.ChatMessageTypeDto
import com.arisamtunes.data.social.PublicUserDto
import com.arisamtunes.data.preferences.LanguagePreference
import com.arisamtunes.data.preferences.ThemePreference
import com.arisamtunes.feature.settings.SettingsViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun ChatListRoute(
    onConversationClick: (String) -> Unit,
    onUserProfileClick: (String) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val conversations = viewModel.conversations.collectAsLazyPagingItems()
    val starters = viewModel.starters.collectAsLazyPagingItems()
    val results = viewModel.searchResults.collectAsLazyPagingItems()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::updateSearch,
            modifier = Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg, vertical = AriSamThemeTokens.spacing.sm),
            placeholder = { Text(stringResource(R.string.social_search_users)) },
            leadingIcon = { Icon(Icons.Rounded.PersonSearch, null) },
            trailingIcon = {
                if (state.searchQuery.isNotBlank()) IconButton(onClick = viewModel::clearSearch) { Icon(Icons.Rounded.Close, stringResource(R.string.clear)) }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
        )
        if (state.isSearching) UserSearchResults(results, onUserProfileClick, onConversationClick)
        else ConversationInbox(conversations, starters, onConversationClick)
    }
}

@Composable
private fun ConversationInbox(
    conversations: LazyPagingItems<ChatConversationDto>,
    starters: LazyPagingItems<PublicUserDto>,
    onClick: (String) -> Unit,
) {
    when {
        conversations.loadState.refresh is LoadState.Loading && conversations.itemCount == 0 -> ChatListSkeleton()
        conversations.loadState.refresh is LoadState.Error && conversations.itemCount == 0 -> ErrorState(conversations::retry)
        else -> LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AriSamThemeTokens.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.xs),
        ) {
            if (starters.itemCount > 0) {
                item { SectionTitle(stringResource(R.string.chat_start_with_following)) }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = AriSamThemeTokens.spacing.lg), horizontalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.md)) {
                        items(starters.itemCount, key = { starters.peek(it)?.id ?: it }) { index ->
                            starters[index]?.let { user ->
                                PressScaleBox({ onClick(user.id) }) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.xs)) {
                                        UserAvatar(user, Modifier.size(60.dp))
                                        Text(user.displayName, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.fillParentMaxWidth(.24f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (conversations.itemCount > 0) item { SectionTitle(stringResource(R.string.chat_recent_messages)) }
            items(conversations.itemCount, key = { conversations.peek(it)?.user?.id ?: it }) { index ->
                conversations[index]?.let { ConversationRow(it, onClick) }
            }
            if (conversations.itemCount == 0 && conversations.loadState.refresh !is LoadState.Loading) item { EmptyState(R.string.chat_empty) }
            if (conversations.loadState.append is LoadState.Loading) item { ShimmerBox(Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg).height(76.dp)) }
        }
    }
}

@Composable
private fun UserSearchResults(results: LazyPagingItems<PublicUserDto>, onProfile: (String) -> Unit, onMessage: (String) -> Unit) {
    when {
        results.loadState.refresh is LoadState.Loading -> ChatListSkeleton()
        results.loadState.refresh is LoadState.Error -> ErrorState(results::retry)
        results.itemCount == 0 -> EmptyState(R.string.social_search_empty)
        else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = AriSamThemeTokens.spacing.xl)) {
            items(results.itemCount, key = { results.peek(it)?.id ?: it }) { index ->
                results[index]?.let { user ->
                    PressScaleBox({ onProfile(user.id) }, Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg, vertical = AriSamThemeTokens.spacing.xs)) {
                        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                            Row(Modifier.fillMaxWidth().padding(AriSamThemeTokens.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                                UserAvatar(user, Modifier.size(52.dp))
                                Column(Modifier.weight(1f).padding(horizontal = AriSamThemeTokens.spacing.md)) {
                                    Text(user.displayName, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(stringResource(R.string.social_followers_count, user.followersCount), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { onMessage(user.id) }) { Icon(Icons.Rounded.Chat, stringResource(R.string.chat_tap_to_message)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: ChatConversationDto, onClick: (String) -> Unit) {
    PressScaleBox({ onClick(conversation.user.id) }, Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg)) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(Modifier.fillMaxWidth().padding(AriSamThemeTokens.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(conversation.user, Modifier.size(56.dp))
                Column(Modifier.weight(1f).padding(horizontal = AriSamThemeTokens.spacing.md), verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.xs)) {
                    Text(conversation.user.displayName, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        if (conversation.latestMessage.messageType == ChatMessageTypeDto.SONG) stringResource(R.string.chat_shared_song) else conversation.latestMessage.content.orEmpty(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(messageTime(conversation.latestMessage.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (conversation.unreadCount > 0) Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Text(conversation.unreadCount.coerceAtMost(99).toString(), Modifier.padding(horizontal = AriSamThemeTokens.spacing.sm, vertical = AriSamThemeTokens.spacing.xxs), color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatDetailRoute(
    onBack: () -> Unit,
    onPlaySong: (SongDto) -> Unit,
    currentSong: SongDto? = null,
    miniPlayer: @Composable () -> Unit = {},
    viewModel: ChatDetailViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val preferences by settingsViewModel.preferences.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val isDark = when (preferences.theme) {
        ThemePreference.System -> systemDark
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }
    val systemLanguage = LocalConfiguration.current.locales[0].language
    val isPersian = preferences.language == LanguagePreference.Persian ||
        (preferences.language == LanguagePreference.System && systemLanguage == "fa")
    val messages = viewModel.messages.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sendFailed = stringResource(R.string.chat_send_failed)
    val songUnavailable = stringResource(R.string.chat_song_unavailable)
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            snackbarHostState.showSnackbar(if (effect is ChatEffect.SendFailed) sendFailed else songUnavailable)
        }
    }
    LaunchedEffect(messages.itemCount) {
        if (messages.itemCount > 0 && listState.firstVisibleItemIndex <= 2) listState.animateScrollToItem(0)
    }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.surfaceContainer,
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.primary.copy(alpha = .06f),
                ),
            ),
        ),
    ) {
    Column(Modifier.fillMaxSize().imePadding()) {
        ChatTopBar(
            state = state,
            onBack = onBack,
            onRetryPeer = viewModel::refreshPeer,
            isDark = isDark,
            isPersian = isPersian,
            onToggleTheme = { settingsViewModel.setTheme(if (isDark) ThemePreference.Light else ThemePreference.Dark) },
            onToggleLanguage = { settingsViewModel.setLanguage(if (isPersian) LanguagePreference.English else LanguagePreference.Persian) },
        )
        AnimatedVisibility(state.status != ChatConnectionStatus.Connected) {
            ConnectionBanner(state.status, viewModel::retryConnection)
        }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = AriSamThemeTokens.spacing.lg, vertical = AriSamThemeTokens.spacing.md),
                verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.md),
            ) {
                if (state.isPeerTyping) {
                    item(key = "typing-indicator") { TypingBubble(state.peer) }
                }
                items(messages.itemCount, key = { messages.peek(it)?.id ?: it }) { index ->
                    messages[index]?.let { message ->
                        LaunchedEffect(message.id, message.status) { viewModel.onMessagePresented(message) }
                        val nextOlderMessage = if (index + 1 < messages.itemCount) messages.peek(index + 1) else null
                        Column(verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.md)) {
                            if (nextOlderMessage == null || !isSameMessageDay(message.createdAt, nextOlderMessage.createdAt)) {
                                DateSeparator(message.createdAt)
                            }
                            MessageBubble(
                                message,
                                message.senderId == state.meId,
                                state.songCards[message.songId],
                                message.songId in state.unavailableSongIds,
                                onPlaySong,
                            ) { viewModel.retry(message) }
                        }
                    }
                }
                if (messages.loadState.append is LoadState.Loading) item { CircularProgressIndicator(Modifier.size(24.dp)) }
                if (messages.loadState.refresh is LoadState.Loading && messages.itemCount == 0) items(5) { ShimmerBox(Modifier.fillMaxWidth(.72f).height(64.dp)) }
                if (messages.itemCount == 0 && messages.loadState.refresh !is LoadState.Loading) {
                    item(key = "empty-chat") {
                        ChatEmptyState(
                            peerName = state.peer?.displayName ?: stringResource(R.string.chat),
                            modifier = Modifier.fillParentMaxHeight(),
                        )
                    }
                }
            }
            miniPlayer()
            ChatComposer(
                draft = state.draft,
                onDraftChange = viewModel::updateDraft,
                onSend = viewModel::send,
                onShareSong = currentSong?.let { song -> { viewModel.sendSong(song.id) } },
            )
    }
    SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).imePadding())
    }
}

@Composable
private fun ChatTopBar(
    state: ChatDetailUiState,
    onBack: () -> Unit,
    onRetryPeer: () -> Unit,
    isDark: Boolean,
    isPersian: Boolean,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.md, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                }
                Box(Modifier.padding(start = AriSamThemeTokens.spacing.sm)) {
                    state.peer?.let {
                        UserAvatar(it, Modifier.size(46.dp).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape).padding(2.dp))
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).size(13.dp),
                        shape = CircleShape,
                        color = if (state.status == ChatConnectionStatus.Connected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                    ) {}
                }
                Column(Modifier.weight(1f).padding(horizontal = AriSamThemeTokens.spacing.md)) {
                    Text(state.peer?.displayName ?: stringResource(R.string.chat), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    AnimatedContent(state.isPeerTyping, label = "peer-status") { typing ->
                        Text(
                            if (typing) stringResource(R.string.chat_typing) else stringResource(state.status.labelRes()),
                            color = if (typing || state.status == ChatConnectionStatus.Connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (state.hasPeerError) {
                        HeaderAction(onClick = onRetryPeer) { Icon(Icons.Rounded.Refresh, stringResource(R.string.retry), Modifier.size(18.dp)) }
                    }
                    HeaderAction(onClick = onToggleLanguage) {
                        Text(if (isPersian) "EN" else "فا", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                    }
                    HeaderAction(onClick = onToggleTheme) {
                        Icon(if (isDark) Icons.Rounded.WbSunny else Icons.Rounded.DarkMode, null, Modifier.size(19.dp))
                    }
                    HeaderAction(onClick = {}, enabled = false) {
                        Icon(Icons.Rounded.Phone, null, Modifier.size(19.dp))
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun HeaderAction(onClick: () -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = MaterialTheme.shapes.large,
        color = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        IconButton(onClick = onClick, enabled = enabled) { content() }
    }
}

@Composable
private fun ConnectionBanner(status: ChatConnectionStatus, onRetry: () -> Unit) {
    val isOffline = status == ChatConnectionStatus.Disconnected
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = if (isOffline) MaterialTheme.colorScheme.error else AriSamThemeTokens.tehranAmber,
        contentColor = if (isOffline) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.md, vertical = AriSamThemeTokens.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            if (isOffline) Icon(Icons.Rounded.WifiOff, null, Modifier.size(18.dp))
            else CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                stringResource(if (isOffline) R.string.chat_offline_queue_hint else R.string.chat_reconnecting_hint),
                modifier = Modifier.weight(1f).padding(horizontal = AriSamThemeTokens.spacing.sm),
                style = MaterialTheme.typography.bodySmall,
            )
            if (status != ChatConnectionStatus.Connecting) TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun ChatComposer(draft: String, onDraftChange: (String) -> Unit, onSend: () -> Unit, onShareSong: (() -> Unit)?) {
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.md, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.sm),
            ) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    IconButton(onClick = { onShareSong?.invoke() }, enabled = onShareSong != null) {
                        Icon(Icons.Rounded.MusicNote, stringResource(R.string.chat_share_current_song), Modifier.size(21.dp))
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.chat_message_hint)) },
                    maxLines = 4,
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
                PressScaleBox(onSend) {
                    Surface(
                        Modifier.size(50.dp),
                        MaterialTheme.shapes.large,
                        if (draft.isBlank()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary,
                        contentColor = if (draft.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                        tonalElevation = if (draft.isBlank()) 0.dp else 5.dp,
                    ) {
                        IconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                            Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.send))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessageDto, isMine: Boolean, song: SongDto?, songUnavailable: Boolean, onPlaySong: (SongDto) -> Unit, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape = if (isMine) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp),
            modifier = Modifier.widthIn(min = 72.dp, max = 330.dp),
            border = if (isMine) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 2.dp,
        ) {
            Column(Modifier.padding(horizontal = AriSamThemeTokens.spacing.md, vertical = 11.dp)) {
                if (message.messageType == ChatMessageTypeDto.SONG) SharedSongCard(song, songUnavailable, isMine, onPlaySong)
                else Text(message.content.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(messageTime(message.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isMine) AnimatedContent(message.status, label = "message-status") { status -> MessageStatusIcon(status) }
            if (message.status == ChatMessageStatusDto.FAILED) TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun SharedSongCard(song: SongDto?, unavailable: Boolean, isMine: Boolean, onPlay: (SongDto) -> Unit) {
    val secondaryContent = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = .78f) else MaterialTheme.colorScheme.onSurfaceVariant
    if (unavailable) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = .14f) else MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = .2f) else MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(Modifier.widthIn(min = 240.dp, max = 290.dp).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = .12f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = secondaryContent,
                ) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicOff, null, Modifier.size(23.dp)) }
                }
                Column(Modifier.padding(horizontal = AriSamThemeTokens.spacing.sm)) {
                    Text(stringResource(R.string.chat_song_unavailable), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.chat_song_unavailable_detail), style = MaterialTheme.typography.bodySmall, color = secondaryContent)
                }
            }
        }
    } else if (song == null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.MusicNote, null)
            Text(stringResource(R.string.chat_shared_song_loading), Modifier.padding(horizontal = AriSamThemeTokens.spacing.sm))
        }
    } else PressScaleBox({ onPlay(song) }) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = .12f) else MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = .18f) else MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(Modifier.widthIn(min = 240.dp, max = 290.dp).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    AsyncImage(song.coverImageUrl, song.title, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.medium))
                    Surface(Modifier.align(Alignment.Center).size(26.dp), CircleShape, MaterialTheme.colorScheme.scrim.copy(alpha = .55f), contentColor = androidx.compose.ui.graphics.Color.White) {
                        Icon(Icons.Rounded.PlayArrow, stringResource(R.string.play), Modifier.padding(4.dp))
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = AriSamThemeTokens.spacing.sm)) {
                    Text(song.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artistName, style = MaterialTheme.typography.bodySmall, color = secondaryContent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Rounded.MusicNote, null, tint = secondaryContent, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun TypingBubble(peer: PublicUserDto?) {
    val transition = rememberInfiniteTransition(label = "typing-dots")
    val dotAlphas = List(3) { index ->
        transition.animateFloat(
            initialValue = .3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(460, delayMillis = index * 120), RepeatMode.Reverse),
            label = "typing-dot-$index",
        )
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (peer != null) UserAvatar(peer, Modifier.size(28.dp))
        else Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer))
        Surface(
            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 2.dp,
        ) {
            Row(Modifier.padding(horizontal = 15.dp, vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                dotAlphas.forEach { alpha ->
                    Box(
                        Modifier.size(7.dp).graphicsLayer {
                            this.alpha = alpha.value
                            translationY = -2.dp.toPx() * alpha.value
                        }.background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun DateSeparator(value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Surface(
            modifier = Modifier.padding(horizontal = AriSamThemeTokens.spacing.sm),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                messageDateLabel(value),
                Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ChatEmptyState(peerName: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Headphones, null, Modifier.size(38.dp))
            }
        }
        Text(
            stringResource(R.string.chat_start_conversation_with, peerName),
            modifier = Modifier.padding(top = AriSamThemeTokens.spacing.lg),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            stringResource(R.string.chat_empty_conversation_hint),
            modifier = Modifier.padding(top = AriSamThemeTokens.spacing.sm),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.padding(top = AriSamThemeTokens.spacing.lg),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Row(
                Modifier.padding(horizontal = AriSamThemeTokens.spacing.lg, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.sm),
            ) {
                Icon(Icons.Rounded.MusicNote, null, Modifier.size(18.dp))
                Text(stringResource(R.string.chat_be_first_to_vibe), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(status: ChatMessageStatusDto) = when (status) {
    ChatMessageStatusDto.PENDING -> Icon(Icons.Rounded.AccessTime, stringResource(R.string.chat_status_sending), Modifier.size(16.dp))
    ChatMessageStatusDto.FAILED -> Icon(Icons.Rounded.ErrorOutline, stringResource(R.string.chat_status_failed), Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
    ChatMessageStatusDto.READ -> Icon(Icons.Rounded.DoneAll, stringResource(R.string.chat_status_read), Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
    else -> Icon(Icons.Rounded.Done, stringResource(R.string.chat_status_sent), Modifier.size(16.dp))
}

@Composable
fun ShareSongRoute(onBack: () -> Unit, viewModel: ShareSongViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val friends = viewModel.friends.collectAsLazyPagingItems()
    LaunchedEffect(Unit) { viewModel.effects.collect { if (it is ShareSongEffect.Sent) onBack() } }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(AriSamThemeTokens.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
            Text(stringResource(R.string.chat_share_song), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (state.isLoading) {
            ChatListSkeleton()
            return@Column
        }
        if (state.hasError && state.song == null) {
            ErrorState(viewModel::loadSong)
            return@Column
        }
        state.song?.let { song -> Surface(Modifier.fillMaxWidth().padding(AriSamThemeTokens.spacing.lg), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(Modifier.padding(AriSamThemeTokens.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(song.coverImageUrl, song.title, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.medium))
                Column(Modifier.padding(horizontal = AriSamThemeTokens.spacing.md)) { Text(song.title, fontWeight = FontWeight.Bold); Text(song.artistName) }
            }
        } }
        AnimatedVisibility(state.hasError) {
            Text(stringResource(R.string.chat_send_failed), Modifier.padding(horizontal = AriSamThemeTokens.spacing.lg), color = MaterialTheme.colorScheme.error)
        }
        Text(stringResource(R.string.chat_choose_friend), Modifier.padding(horizontal = AriSamThemeTokens.spacing.lg, vertical = AriSamThemeTokens.spacing.sm), style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = AriSamThemeTokens.spacing.xl)) {
            if (friends.loadState.refresh is LoadState.Loading) items(5) { ShimmerBox(Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg).height(68.dp)) }
            if (friends.loadState.refresh is LoadState.Error) item { ErrorState(friends::retry) }
            if (friends.loadState.refresh !is LoadState.Loading && friends.itemCount == 0) item { EmptyState(R.string.social_users_empty) }
            items(friends.itemCount, key = { friends.peek(it)?.id ?: it }) { index -> friends[index]?.let { user ->
                PressScaleBox({ viewModel.sendTo(user) }, Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg, vertical = AriSamThemeTokens.spacing.xs)) {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                        Row(Modifier.fillMaxWidth().padding(AriSamThemeTokens.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(user, Modifier.size(48.dp)); Text(user.displayName, Modifier.weight(1f).padding(horizontal = AriSamThemeTokens.spacing.md), fontWeight = FontWeight.Bold)
                            Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.send))
                        }
                    }
                }
            } }
        }
    }
}

@Composable
private fun UserAvatar(user: PublicUserDto, modifier: Modifier) {
    if (!user.avatarUrl.isNullOrBlank()) AsyncImage(user.avatarUrl, user.displayName, contentScale = ContentScale.Crop, modifier = modifier.clip(CircleShape))
    else Box(modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Text(user.displayName.take(1).uppercase(), fontWeight = FontWeight.Bold)
    }
}

@Composable private fun SectionTitle(text: String) = Text(text, Modifier.padding(horizontal = AriSamThemeTokens.spacing.lg, vertical = AriSamThemeTokens.spacing.md), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

@Composable private fun ChatListSkeleton() = Column(Modifier.fillMaxSize().padding(AriSamThemeTokens.spacing.lg), verticalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.sm)) { repeat(7) { ShimmerBox(Modifier.fillMaxWidth().height(76.dp)) } }

@Composable private fun ErrorState(onRetry: () -> Unit) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { TextButton(onClick = onRetry) { Icon(Icons.Rounded.Refresh, null); Text(stringResource(R.string.retry)) } }

@Composable private fun EmptyState(message: Int) = Box(Modifier.fillMaxWidth().padding(AriSamThemeTokens.spacing.xxl), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Rounded.Chat, null, Modifier.size(48.dp)); Text(stringResource(message), color = MaterialTheme.colorScheme.onSurfaceVariant) } }

private fun messageTime(value: String): String = runCatching { DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(value)) }.getOrDefault("")

private fun messageDate(value: String): LocalDate? = runCatching {
    Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate()
}.getOrNull()

private fun isSameMessageDay(first: String, second: String): Boolean = messageDate(first) == messageDate(second)

@Composable
private fun messageDateLabel(value: String): String {
    val date = messageDate(value) ?: return ""
    val today = LocalDate.now(ZoneId.systemDefault())
    return when (date) {
        today -> stringResource(R.string.chat_today)
        today.minusDays(1) -> stringResource(R.string.chat_yesterday)
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).format(date)
    }
}

private fun ChatConnectionStatus.labelRes() = when (this) {
    ChatConnectionStatus.Disconnected -> R.string.chat_status_offline
    ChatConnectionStatus.Connecting -> R.string.chat_status_connecting
    ChatConnectionStatus.Connected -> R.string.chat_status_connected
    ChatConnectionStatus.Reconnecting -> R.string.chat_status_reconnecting
}
