package com.arisamtunes.feature.chat

import com.arisamtunes.core.design.spacing.AriSamDimensions
import com.arisamtunes.core.design.colors.AriSamPalette
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AddReaction
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.components.ShimmerBox
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.chat.ChatConnectionStatus
import com.arisamtunes.data.chat.ChatMessageDto
import com.arisamtunes.data.chat.ChatMessageStatusDto
import com.arisamtunes.data.chat.ChatMessageTypeDto
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private val QuickReactions = listOf("❤️", "🔥", "😂", "👏", "😮", "😢")
private val NocturneViolet = AriSamPalette.indigoAccent
private val NocturneIndigo = AriSamPalette.indigo700
private val NocturneCyan = AriSamPalette.teal400
private val NocturneAmber = AriSamPalette.amberAccent

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ChatExperienceRoute(
    onBack: () -> Unit,
    onPeerProfileClick: (String) -> Unit,
    onPlaySong: (SongDto, List<SongDto>) -> Unit,
    currentSong: SongDto? = null,
    miniPlayer: @Composable (Set<String>) -> Unit = {},
    viewModel: ChatDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = viewModel.messages.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val pendingDelete = remember { androidx.compose.runtime.mutableStateOf<ChatMessageDto?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    val loadedMessages = messages.itemSnapshotList.items.associateBy(ChatMessageDto::id)
    var bottomContentHeight by remember { mutableIntStateOf(0) }
    val bottomContentPadding = with(density) { bottomContentHeight.toDp() } + AriSamDimensions.dp12
    val effectMessages = mapOf(
        ChatEffect.SendFailed to stringResource(R.string.chat_send_failed),
        ChatEffect.EditFailed to stringResource(R.string.chat_edit_failed),
        ChatEffect.DeleteFailed to stringResource(R.string.chat_delete_failed),
        ChatEffect.ReactionFailed to stringResource(R.string.chat_reaction_failed),
        ChatEffect.SearchFailed to stringResource(R.string.chat_search_failed),
        ChatEffect.RetryFailed to stringResource(R.string.chat_retry_failed),
        ChatEffect.InvalidSong to stringResource(R.string.chat_song_unavailable),
    )
    val backdropBase = MaterialTheme.colorScheme.background
    val backdropSurface = MaterialTheme.colorScheme.surface

    LaunchedEffect(viewModel, lifecycleOwner, effectMessages) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                effectMessages[effect]?.let { snackbarHostState.showSnackbar(it) }
            }
        }
    }

    LaunchedEffect(messages.itemSnapshotList.items.firstOrNull()?.id) {
        if (messages.itemCount > 0) {
            listState.animateScrollToItem(0)
        }
    }

    // Keep the latest message attached to the composer as the IME or a
    // multi-line draft changes the available height.
    LaunchedEffect(isImeVisible, bottomContentHeight) {
        if (isImeVisible && messages.itemCount > 0) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(state.isPeerTyping) {
        if (state.isPeerTyping && messages.itemCount > 0) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(state.scrollToMessageId, state.scrollToMessageIndex, messages.itemCount) {
        val targetId = state.scrollToMessageId ?: return@LaunchedEffect
        val loadedIndex = (0 until messages.itemCount).firstOrNull { messages.peek(it)?.id == targetId }
        val index = loadedIndex ?: state.scrollToMessageIndex ?: return@LaunchedEffect
        if (index !in 0 until messages.itemCount) return@LaunchedEffect
        messages[index] // Trigger Paging to load the target placeholder.
        val loaded = withTimeoutOrNull(8_000) {
            snapshotFlow { messages.peek(index)?.id }.first { it == targetId }
        }
        if (loaded == targetId) {
            listState.scrollToItem(index)
            viewModel.consumeScrollTarget()
        } else {
            viewModel.searchJumpFailed()
        }
    }

    LaunchedEffect(listState, messages.itemSnapshotList.items) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val byId = messages.itemSnapshotList.items.associateBy(ChatMessageDto::id)
            layout.visibleItemsInfo.mapNotNull { item ->
                val isInsideViewport = item.offset < layout.viewportEndOffset &&
                    item.offset + item.size > layout.viewportStartOffset
                if (isInsideViewport) (item.key as? String)?.let(byId::get) else null
            }
        }
            .distinctUntilChanged()
            .debounce(200)
            .collect(viewModel::markVisibleMessagesRead)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        backdropSurface,
                        backdropBase,
                        backdropBase,
                    ),
                ),
            )
            .drawBehind {
                drawCircle(
                    color = NocturneViolet.copy(alpha = .13f),
                    radius = size.width * .62f,
                    center = Offset(size.width * 1.04f, size.height * .12f),
                )
                drawCircle(
                    color = NocturneCyan.copy(alpha = .08f),
                    radius = size.width * .54f,
                    center = Offset(-size.width * .16f, size.height * .7f),
                )
                drawCircle(
                    color = NocturneAmber.copy(alpha = .035f),
                    radius = size.width * .36f,
                    center = Offset(size.width * .82f, size.height * .9f),
                )
            },
    ) {
        Column(Modifier.fillMaxSize().imePadding()) {
            ConversationHeader(
                state = state,
                onBack = onBack,
                onSearch = viewModel::toggleSearch,
                onPeerProfileClick = onPeerProfileClick,
            )
            AnimatedVisibility(
                visible = state.isSearchOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                ConversationSearch(state, viewModel::updateSearch) { result ->
                    keyboardController?.hide()
                    viewModel.openSearchResult(result)
                }
            }
            AnimatedVisibility(state.status != ChatConnectionStatus.Connected) {
                CompactConnectionBanner(state.status, viewModel::retryConnection)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                reverseLayout = true,
                contentPadding = PaddingValues(start = AriSamDimensions.dp16, top = AriSamDimensions.dp12, end = AriSamDimensions.dp16, bottom = AriSamDimensions.dp12),
                verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10),
            ) {
                items(messages.itemCount, key = { messages.peek(it)?.id ?: it }) { index ->
                    messages[index]?.let { message ->
                        LaunchedEffect(message.id, message.replyToId, message.songId) { viewModel.onMessagePresented(message) }
                        val nextOlderMessage = if (index + 1 < messages.itemCount) messages.peek(index + 1) else null
                        Column(verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10)) {
                            if (nextOlderMessage == null || !isSameMessageDay(message.createdAt, nextOlderMessage.createdAt)) {
                                DateSeparator(message.createdAt)
                            }
                            ExperienceMessageBubble(
                                message = message,
                                isMine = message.senderId == state.meId,
                                repliedMessage = message.replyToId?.let { state.replyPreviews[it] ?: loadedMessages[it] },
                                isHighlighted = message.id == state.highlightedMessageId,
                                song = state.songCards[message.songId],
                                songUnavailable = message.songId in state.unavailableSongIds,
                                onPlaySong = { selected ->
                                    val queue = messages.itemSnapshotList.items.asReversed()
                                        .mapNotNull { item -> item.songId?.let(state.songCards::get) }
                                        .plus(selected)
                                        .distinctBy(SongDto::id)
                                    onPlaySong(selected, queue)
                                },
                                onLongPress = { viewModel.selectMessage(message) },
                                onReaction = { viewModel.toggleReaction(message, it) },
                                onRetry = { viewModel.retry(message) },
                            )
                        }
                    }
                }
                if (messages.loadState.append is LoadState.Loading) item { CircularProgressIndicator(Modifier.size(AriSamDimensions.dp22)) }
                if (messages.loadState.append is LoadState.Error) item { ConversationPagingRetry(messages::retry) }
                if (messages.loadState.refresh is LoadState.Loading && messages.itemCount == 0) {
                    items(6) { ShimmerBox(Modifier.fillMaxWidth(.72f).height(AriSamDimensions.dp68)) }
                }
                if (messages.loadState.refresh is LoadState.Error && messages.itemCount == 0) {
                    item(key = "conversation-refresh-error") { ConversationPagingRetry(messages::retry) }
                }
                if (messages.itemCount == 0 && messages.loadState.refresh is LoadState.NotLoading) {
                    item { NewConversationHero(state.peer?.displayName ?: stringResource(R.string.chat)) }
                }
            }
            Column(Modifier.onSizeChanged { bottomContentHeight = it.height }) {
                AnimatedVisibility(
                    visible = state.isPeerTyping,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = AriSamDimensions.dp16, vertical = AriSamDimensions.dp4)) {
                        ModernTypingBubble(state.peer?.displayName.orEmpty())
                    }
                }
                miniPlayer(state.songCards.keys)
                ExperienceComposer(
                    draft = state.draft,
                    replyTo = state.replyTo,
                    editing = state.editingMessage,
                    currentSong = currentSong,
                    isSending = state.isSending,
                    onDraftChange = viewModel::updateDraft,
                    onCancelContext = viewModel::cancelComposerContext,
                    onSendSong = { currentSong?.let { viewModel.sendSong(it.id) } },
                    onSend = viewModel::send,
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).imePadding().padding(bottom = bottomContentPadding),
        )

        state.selectedMessage?.let { message ->
            ModalBottomSheet(onDismissRequest = { viewModel.selectMessage(null) }) {
                MessageActionSheet(
                    message = message,
                    isMine = message.senderId == state.meId,
                    onReaction = { viewModel.toggleReaction(message, it) },
                    onReply = { viewModel.replyTo(message) },
                    onEdit = { viewModel.edit(message) },
                    onDelete = { pendingDelete.value = message; viewModel.selectMessage(null) },
                )
            }
        }
        pendingDelete.value?.let { message ->
            AlertDialog(
                onDismissRequest = { pendingDelete.value = null },
                title = { Text(stringResource(R.string.chat_delete_title)) },
                text = { Text(stringResource(R.string.chat_delete_description)) },
                confirmButton = { TextButton(onClick = { viewModel.delete(message); pendingDelete.value = null }) { Text(stringResource(R.string.delete)) } },
                dismissButton = { TextButton(onClick = { pendingDelete.value = null }) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }
}

@Composable
private fun ConversationPagingRetry(onRetry: () -> Unit) {
    TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.Refresh, null)
        Text(stringResource(R.string.retry), modifier = Modifier.padding(start = AriSamDimensions.dp6))
    }
}

@Composable
private fun ConversationHeader(
    state: ChatDetailUiState,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onPeerProfileClick: (String) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = AriSamDimensions.dp12, top = AriSamDimensions.dp10, end = AriSamDimensions.dp12, bottom = AriSamDimensions.dp6),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AriSamDimensions.dp30))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = .9f))
                .border(AriSamDimensions.dp1, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f), RoundedCornerShape(AriSamDimensions.dp30))
                .padding(AriSamDimensions.dp7),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderAction(
                onClick = onBack,
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                description = stringResource(R.string.back),
            )
            PressScaleBox(
                onClick = { state.peer?.id?.let(onPeerProfileClick) },
                modifier = Modifier.weight(1f),
                enabled = state.peer != null,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.padding(start = AriSamDimensions.dp7), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(AriSamDimensions.dp52)
                                .background(
                                    Brush.linearGradient(listOf(NocturneViolet, NocturneCyan)),
                                    CircleShape,
                                ),
                        )
                        state.peer?.let { UserAvatar(it, Modifier.size(AriSamDimensions.dp46)) }
                            ?: Surface(Modifier.size(AriSamDimensions.dp46), CircleShape, MaterialTheme.colorScheme.surfaceContainerHigh) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null) }
                            }
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .size(AriSamDimensions.dp14)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .padding(AriSamDimensions.dp2)
                                .background(
                                    if (state.status == ChatConnectionStatus.Connected) NocturneCyan else NocturneAmber,
                                    CircleShape,
                                ),
                        )
                    }
                    Column(Modifier.weight(1f).padding(horizontal = AriSamDimensions.dp11)) {
                        Text(
                            state.peer?.displayName ?: stringResource(R.string.chat),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AnimatedContent(state.isPeerTyping, label = "typing-status") { typing ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp5)) {
                                val peerOnline = state.peerPresence?.isOnline == true
                                val presenceKnown = state.peerPresence != null
                                Box(
                                    Modifier
                                        .size(AriSamDimensions.dp6)
                                        .background(
                                            when {
                                                typing || peerOnline -> NocturneCyan
                                                presenceKnown -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f)
                                                else -> NocturneAmber
                                            },
                                            CircleShape,
                                        ),
                                )
                                Text(
                                    when {
                                        typing -> stringResource(R.string.chat_typing)
                                        peerOnline -> stringResource(R.string.chat_online)
                                        presenceKnown -> stringResource(R.string.chat_offline)
                                        else -> stringResource(R.string.chat_presence_checking)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (typing || peerOnline) NocturneCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            HeaderAction(
                onClick = onSearch,
                icon = if (state.isSearchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                description = stringResource(R.string.search),
                active = state.isSearchOpen,
            )
        }
    }
}

@Composable
private fun HeaderAction(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(AriSamDimensions.dp44),
        shape = CircleShape,
        color = if (active) NocturneViolet.copy(alpha = .18f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .72f),
        contentColor = if (active) NocturneViolet else MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, description, Modifier.size(AriSamDimensions.dp21)) }
    }
}

@Composable
private fun ConversationSearch(state: ChatDetailUiState, onQuery: (String) -> Unit, onResult: (ChatMessageDto) -> Unit) {
    Surface(
        modifier = Modifier.padding(horizontal = AriSamDimensions.dp16, vertical = AriSamDimensions.dp6),
        shape = RoundedCornerShape(AriSamDimensions.dp26),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
        border = BorderStroke(AriSamDimensions.dp1, NocturneViolet.copy(alpha = .24f)),
        shadowElevation = AriSamDimensions.dp10,
    ) {
        Column(Modifier.fillMaxWidth().padding(AriSamDimensions.dp10)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = NocturneViolet) },
                placeholder = { Text(stringResource(R.string.chat_search_hint)) },
                shape = RoundedCornerShape(AriSamDimensions.dp20),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .78f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .58f),
                    focusedBorderColor = NocturneViolet,
                    unfocusedBorderColor = AriSamPalette.transparent,
                ),
            )
            AnimatedVisibility(state.isSearching) {
                CircularProgressIndicator(
                    Modifier.align(Alignment.CenterHorizontally).padding(top = AriSamDimensions.dp10).size(AriSamDimensions.dp20),
                    color = NocturneViolet,
                    strokeWidth = AriSamDimensions.dp2,
                )
            }
            state.searchResults.take(5).forEach { result ->
                Surface(
                    onClick = { onResult(result) },
                    modifier = Modifier.padding(top = AriSamDimensions.dp6),
                    shape = RoundedCornerShape(AriSamDimensions.dp16),
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .52f),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = AriSamDimensions.dp12, vertical = AriSamDimensions.dp10),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(AriSamDimensions.dp7).background(NocturneCyan, CircleShape))
                        Column(Modifier.weight(1f).padding(horizontal = AriSamDimensions.dp10)) {
                            Text(result.content.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                messageTime(result.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactConnectionBanner(status: ChatConnectionStatus, onRetry: () -> Unit) {
    val waiting = status == ChatConnectionStatus.Connecting || status == ChatConnectionStatus.Reconnecting
    val label = when (status) {
        ChatConnectionStatus.Connecting -> stringResource(R.string.chat_connecting_hint)
        ChatConnectionStatus.Reconnecting -> stringResource(R.string.chat_reconnecting_hint)
        ChatConnectionStatus.Disconnected -> stringResource(R.string.chat_offline_queue_hint)
        ChatConnectionStatus.Connected -> return
    }
    Surface(
        modifier = Modifier.padding(horizontal = AriSamDimensions.dp18, vertical = AriSamDimensions.dp5),
        shape = CircleShape,
        color = NocturneAmber.copy(alpha = .16f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(AriSamDimensions.dp1, NocturneAmber.copy(alpha = .34f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = AriSamDimensions.dp13, vertical = AriSamDimensions.dp6), verticalAlignment = Alignment.CenterVertically) {
            if (waiting) CircularProgressIndicator(Modifier.size(AriSamDimensions.dp16), color = NocturneAmber, strokeWidth = AriSamDimensions.dp2)
            else Icon(Icons.Rounded.WifiOff, null, Modifier.size(AriSamDimensions.dp16), tint = NocturneAmber)
            Text(label, Modifier.weight(1f).padding(horizontal = AriSamDimensions.dp8), style = MaterialTheme.typography.labelMedium)
            if (status == ChatConnectionStatus.Disconnected) {
                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry), color = NocturneAmber) }
            }
        }
    }
}

@Composable
private fun ExperienceMessageBubble(
    message: ChatMessageDto,
    isMine: Boolean,
    repliedMessage: ChatMessageDto?,
    isHighlighted: Boolean,
    song: SongDto?,
    songUnavailable: Boolean,
    onPlaySong: (SongDto) -> Unit,
    onLongPress: () -> Unit,
    onReaction: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val deleted = message.deletedAt != null
    val highlightBorder by animateColorAsState(
        if (isHighlighted) NocturneAmber else AriSamPalette.transparent,
        label = "message-highlight",
    )
    val bubbleShape = if (isMine) {
        AbsoluteRoundedCornerShape(AriSamDimensions.dp24, AriSamDimensions.dp24, AriSamDimensions.dp7, AriSamDimensions.dp24)
    } else {
        AbsoluteRoundedCornerShape(AriSamDimensions.dp24, AriSamDimensions.dp24, AriSamDimensions.dp24, AriSamDimensions.dp7)
    }
    val messageColor = if (isMine) AriSamPalette.white else MaterialTheme.colorScheme.onSurface
    val secondaryMessageColor = if (isMine) AriSamPalette.white.copy(alpha = .7f) else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onLongPress),
        horizontalAlignment = if (isMine) AbsoluteAlignment.Right else AbsoluteAlignment.Left,
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = AriSamDimensions.dp76, max = AriSamDimensions.dp336)
                .clip(bubbleShape)
                .background(
                    if (isMine) {
                        Brush.linearGradient(listOf(NocturneViolet, NocturneIndigo))
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = .98f),
                                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .9f),
                            ),
                        )
                    },
                )
                .border(
                    width = if (isHighlighted) AriSamDimensions.dp2 else AriSamDimensions.dp1,
                    color = when {
                        isHighlighted -> highlightBorder
                        isMine -> AriSamPalette.white.copy(alpha = .1f)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = .72f)
                    },
                    shape = bubbleShape,
                ),
        ) {
            Column(Modifier.padding(horizontal = AriSamDimensions.dp14, vertical = AriSamDimensions.dp11)) {
                message.replyToId?.let {
                    val replyText = when {
                        repliedMessage?.deletedAt != null -> stringResource(R.string.chat_deleted_message)
                        repliedMessage?.messageType == ChatMessageTypeDto.SONG -> stringResource(R.string.chat_shared_song)
                        !repliedMessage?.content.isNullOrBlank() -> repliedMessage?.content.orEmpty()
                        else -> stringResource(R.string.chat_message)
                    }
                    Surface(
                        shape = RoundedCornerShape(AriSamDimensions.dp12),
                        color = if (isMine) AriSamPalette.white.copy(alpha = .11f) else NocturneViolet.copy(alpha = .1f),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(AriSamDimensions.dp8), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(AriSamDimensions.dp3).height(AriSamDimensions.dp30).background(if (isMine) NocturneCyan else NocturneViolet, CircleShape))
                            Text(
                                replyText,
                                Modifier.padding(start = AriSamDimensions.dp8),
                                style = MaterialTheme.typography.labelMedium,
                                color = messageColor.copy(alpha = .86f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(AriSamDimensions.dp9))
                }
                when {
                    deleted -> Text(
                        stringResource(R.string.chat_deleted_message),
                        fontStyle = FontStyle.Italic,
                        color = secondaryMessageColor,
                    )
                    message.messageType == ChatMessageTypeDto.SONG -> NocturneSongCard(song, songUnavailable, isMine, onPlaySong)
                    else -> Text(message.content.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = messageColor)
                }
                Row(Modifier.align(AbsoluteAlignment.Right).padding(top = AriSamDimensions.dp6), verticalAlignment = Alignment.CenterVertically) {
                    if (message.editedAt != null && !deleted) {
                        Text(stringResource(R.string.chat_edited), style = MaterialTheme.typography.labelSmall, color = secondaryMessageColor)
                    }
                    Text(
                        messageTime(message.createdAt),
                        Modifier.padding(start = AriSamDimensions.dp5),
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryMessageColor,
                    )
                    if (isMine) Box(Modifier.padding(start = AriSamDimensions.dp3)) { MessageDeliveryIcon(message.status, isMine = true) }
                }
            }
        }
        if (message.reactions.isNotEmpty()) {
            Row(Modifier.padding(top = AriSamDimensions.dp4, start = AriSamDimensions.dp8, end = AriSamDimensions.dp8), horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp5)) {
                message.reactions.forEach { reaction ->
                    Surface(
                        onClick = { onReaction(reaction.reaction) },
                        shape = CircleShape,
                        color = if (reaction.reactedByMe) NocturneViolet.copy(alpha = .18f) else MaterialTheme.colorScheme.surface.copy(alpha = .9f),
                        border = BorderStroke(
                            AriSamDimensions.dp1,
                            if (reaction.reactedByMe) NocturneViolet.copy(alpha = .42f) else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Text(
                            "${reaction.reaction} ${reaction.count}",
                            Modifier.padding(horizontal = AriSamDimensions.dp9, vertical = AriSamDimensions.dp4),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
        if (message.status == ChatMessageStatusDto.FAILED) TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun NocturneSongCard(
    song: SongDto?,
    unavailable: Boolean,
    isMine: Boolean,
    onPlay: (SongDto) -> Unit,
) {
    val primaryContent = if (isMine) AriSamPalette.white else MaterialTheme.colorScheme.onSurface
    val secondaryContent = if (isMine) AriSamPalette.white.copy(alpha = .68f) else MaterialTheme.colorScheme.onSurfaceVariant
    val container = if (isMine) AriSamPalette.white.copy(alpha = .1f) else NocturneViolet.copy(alpha = .08f)
    val outline = if (isMine) AriSamPalette.white.copy(alpha = .14f) else NocturneViolet.copy(alpha = .2f)

    when {
        unavailable -> Surface(
            shape = RoundedCornerShape(AriSamDimensions.dp18),
            color = container,
            border = BorderStroke(AriSamDimensions.dp1, outline),
        ) {
            Row(
                Modifier.widthIn(min = AriSamDimensions.dp248, max = AriSamDimensions.dp296).padding(AriSamDimensions.dp9),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(AriSamDimensions.dp52),
                    shape = RoundedCornerShape(AriSamDimensions.dp15),
                    color = if (isMine) AriSamPalette.white.copy(alpha = .1f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = secondaryContent,
                ) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicOff, null, Modifier.size(AriSamDimensions.dp24)) }
                }
                Column(Modifier.padding(start = AriSamDimensions.dp10)) {
                    Text(
                        stringResource(R.string.chat_song_unavailable),
                        color = primaryContent,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.chat_song_unavailable_detail),
                        color = secondaryContent,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        song == null -> Row(
            Modifier.widthIn(min = AriSamDimensions.dp228).padding(vertical = AriSamDimensions.dp5),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(Modifier.size(AriSamDimensions.dp18), color = NocturneCyan, strokeWidth = AriSamDimensions.dp2)
            Text(
                stringResource(R.string.chat_shared_song_loading),
                Modifier.padding(start = AriSamDimensions.dp10),
                color = primaryContent,
            )
        }
        else -> PressScaleBox({ onPlay(song) }) {
            Surface(
                shape = RoundedCornerShape(AriSamDimensions.dp18),
                color = container,
                border = BorderStroke(AriSamDimensions.dp1, outline),
            ) {
                Row(
                    Modifier.widthIn(min = AriSamDimensions.dp252, max = AriSamDimensions.dp300).padding(AriSamDimensions.dp7),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        AsyncImage(
                            model = song.coverImageUrl,
                            contentDescription = song.title,
                            error = painterResource(R.drawable.arisam_app_icon_dark),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(AriSamDimensions.dp58).clip(RoundedCornerShape(AriSamDimensions.dp15)),
                        )
                        Box(
                            Modifier
                                .align(Alignment.Center)
                                .size(AriSamDimensions.dp30)
                                .background(AriSamPalette.black.copy(alpha = .54f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.PlayArrow, stringResource(R.string.play), tint = AriSamPalette.white, modifier = Modifier.size(AriSamDimensions.dp19))
                        }
                    }
                    Column(Modifier.weight(1f).padding(horizontal = AriSamDimensions.dp10)) {
                        Text(
                            song.title,
                            color = primaryContent,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            song.artistName,
                            color = secondaryContent,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(Modifier.padding(top = AriSamDimensions.dp6), horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp3), verticalAlignment = Alignment.Bottom) {
                            listOf(5, 10, 7, 13, 8).forEach { height ->
                                Box(Modifier.width(AriSamDimensions.dp3).height(height.dp).background(NocturneCyan, CircleShape))
                            }
                        }
                    }
                    Icon(Icons.Rounded.MusicNote, null, tint = secondaryContent, modifier = Modifier.size(AriSamDimensions.dp20))
                }
            }
        }
    }
}

@Composable
private fun ExperienceComposer(
    draft: String,
    replyTo: ChatMessageDto?,
    editing: ChatMessageDto?,
    currentSong: SongDto?,
    isSending: Boolean,
    onDraftChange: (String) -> Unit,
    onCancelContext: () -> Unit,
    onSendSong: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = AriSamDimensions.dp12, vertical = AriSamDimensions.dp9),
        shape = RoundedCornerShape(AriSamDimensions.dp30),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .94f),
        border = BorderStroke(AriSamDimensions.dp1, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .72f)),
        shadowElevation = AriSamDimensions.dp16,
    ) {
        Column {
            AnimatedVisibility(replyTo != null || editing != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = AriSamDimensions.dp9, top = AriSamDimensions.dp9, end = AriSamDimensions.dp9)
                        .clip(RoundedCornerShape(AriSamDimensions.dp18))
                        .background(NocturneViolet.copy(alpha = .1f))
                        .border(AriSamDimensions.dp1, NocturneViolet.copy(alpha = .18f), RoundedCornerShape(AriSamDimensions.dp18))
                        .padding(start = AriSamDimensions.dp11, top = AriSamDimensions.dp7, bottom = AriSamDimensions.dp7),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(AriSamDimensions.dp3).height(AriSamDimensions.dp34).background(if (editing != null) NocturneAmber else NocturneCyan, CircleShape))
                    Icon(
                        if (editing != null) Icons.Rounded.Edit else Icons.AutoMirrored.Rounded.Reply,
                        null,
                        Modifier.padding(start = AriSamDimensions.dp9).size(AriSamDimensions.dp19),
                        tint = if (editing != null) NocturneAmber else NocturneCyan,
                    )
                    Column(Modifier.weight(1f).padding(horizontal = AriSamDimensions.dp9)) {
                        Text(
                            stringResource(if (editing != null) R.string.chat_editing_message else R.string.chat_replying_to),
                            style = MaterialTheme.typography.labelLarge,
                            color = NocturneViolet,
                        )
                        Text(
                            (editing ?: replyTo)?.content ?: stringResource(R.string.chat_shared_song),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onCancelContext) { Icon(Icons.Rounded.Close, stringResource(R.string.cancel)) }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(AriSamDimensions.dp8),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp7),
            ) {
                val songEnabled = currentSong != null && editing == null && !isSending
                Surface(
                    onClick = onSendSong,
                    enabled = songEnabled,
                    modifier = Modifier.size(AriSamDimensions.dp50),
                    shape = RoundedCornerShape(AriSamDimensions.dp17),
                    color = if (songEnabled) NocturneCyan.copy(alpha = .14f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (songEnabled) NocturneCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .48f),
                    border = BorderStroke(AriSamDimensions.dp1, if (songEnabled) NocturneCyan.copy(alpha = .26f) else AriSamPalette.transparent),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.MusicNote, stringResource(R.string.chat_share_current_song), Modifier.size(AriSamDimensions.dp22))
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.chat_message_hint)) },
                    maxLines = 5,
                    shape = RoundedCornerShape(AriSamDimensions.dp19),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .7f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .56f),
                        focusedBorderColor = NocturneViolet.copy(alpha = .7f),
                        unfocusedBorderColor = AriSamPalette.transparent,
                    ),
                )
                val sendEnabled = draft.isNotBlank() && !isSending
                PressScaleBox(onClick = onSend, enabled = sendEnabled) {
                    Box(
                        Modifier
                            .size(AriSamDimensions.dp50)
                            .clip(RoundedCornerShape(AriSamDimensions.dp17))
                            .background(
                                if (sendEnabled) {
                                    Brush.linearGradient(listOf(NocturneViolet, NocturneIndigo))
                                } else {
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceContainerHighest,
                                            MaterialTheme.colorScheme.surfaceContainerHighest,
                                        ),
                                    )
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(Modifier.size(AriSamDimensions.dp22), color = AriSamPalette.white, strokeWidth = AriSamDimensions.dp2)
                        } else {
                            Icon(
                                Icons.AutoMirrored.Rounded.Send,
                                stringResource(R.string.send),
                                tint = if (sendEnabled) AriSamPalette.white else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageActionSheet(
    message: ChatMessageDto,
    isMine: Boolean,
    onReaction: (String) -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = AriSamDimensions.dp20, vertical = AriSamDimensions.dp10)) {
        Box(Modifier.align(Alignment.CenterHorizontally).width(AriSamDimensions.dp42).height(AriSamDimensions.dp4).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
        Text(
            stringResource(R.string.chat_react),
            Modifier.padding(top = AriSamDimensions.dp18),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        Row(Modifier.fillMaxWidth().padding(vertical = AriSamDimensions.dp16), horizontalArrangement = Arrangement.SpaceBetween) {
            QuickReactions.forEach { reaction ->
                Surface(
                    onClick = { onReaction(reaction) },
                    shape = RoundedCornerShape(AriSamDimensions.dp16),
                    color = NocturneViolet.copy(alpha = .1f),
                    border = BorderStroke(AriSamDimensions.dp1, NocturneViolet.copy(alpha = .16f)),
                ) { Text(reaction, Modifier.padding(AriSamDimensions.dp10)) }
            }
        }
        ActionRow(Icons.AutoMirrored.Rounded.Reply, R.string.chat_reply, onReply)
        if (isMine && message.messageType == ChatMessageTypeDto.TEXT && message.deletedAt == null) ActionRow(Icons.Rounded.Edit, R.string.chat_edit, onEdit)
        if (isMine && message.deletedAt == null) ActionRow(Icons.Rounded.DeleteOutline, R.string.delete, onDelete, destructive = true)
        Spacer(Modifier.height(AriSamDimensions.dp24))
    }
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: Int, onClick: () -> Unit, destructive: Boolean = false) {
    Surface(onClick = onClick, color = AriSamPalette.transparent) {
        Row(Modifier.fillMaxWidth().padding(vertical = AriSamDimensions.dp13), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Text(stringResource(label), Modifier.padding(start = AriSamDimensions.dp14), color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ModernTypingBubble(name: String) {
    val transition = rememberInfiniteTransition(label = "nocturne-typing")
    val alphas = List(3) { index ->
        transition.animateFloat(
            initialValue = .28f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(480, delayMillis = index * 120), RepeatMode.Reverse),
            label = "typing-dot-$index",
        )
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Surface(
            shape = RoundedCornerShape(AriSamDimensions.dp20, AriSamDimensions.dp20, AriSamDimensions.dp20, AriSamDimensions.dp7),
            color = MaterialTheme.colorScheme.surface.copy(alpha = .94f),
            border = BorderStroke(AriSamDimensions.dp1, NocturneCyan.copy(alpha = .22f)),
        ) {
            Row(
                Modifier.padding(horizontal = AriSamDimensions.dp13, vertical = AriSamDimensions.dp10),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp5),
            ) {
                alphas.forEach { alpha ->
                    Box(
                        Modifier
                            .size(AriSamDimensions.dp7)
                            .graphicsLayer {
                                this.alpha = alpha.value
                                translationY = AriSamDimensions.negative2.toPx() * alpha.value
                            }
                            .background(NocturneCyan, CircleShape),
                    )
                }
                Text(
                    stringResource(R.string.chat_typing_person, name),
                    Modifier.padding(start = AriSamDimensions.dp5),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NewConversationHero(name: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = AriSamDimensions.dp40, vertical = AriSamDimensions.dp56),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(AriSamDimensions.dp112)
                    .background(NocturneViolet.copy(alpha = .08f), CircleShape)
                    .border(AriSamDimensions.dp1, NocturneViolet.copy(alpha = .12f), CircleShape),
            )
            Box(
                Modifier
                    .size(AriSamDimensions.dp82)
                    .background(Brush.linearGradient(listOf(NocturneViolet, NocturneIndigo)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AddReaction, null, Modifier.size(AriSamDimensions.dp36), tint = AriSamPalette.white)
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(AriSamDimensions.dp34)
                    .background(NocturneCyan, CircleShape)
                    .border(AriSamDimensions.dp4, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.MusicNote, null, Modifier.size(AriSamDimensions.dp16), tint = AriSamPalette.tealInk)
            }
        }
        Text(
            stringResource(R.string.chat_start_conversation_with, name),
            Modifier.padding(top = AriSamDimensions.dp22),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            stringResource(R.string.chat_empty_conversation_hint),
            Modifier.padding(top = AriSamDimensions.dp8),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun MessageDeliveryIcon(status: ChatMessageStatusDto, isMine: Boolean = false) = when (status) {
    ChatMessageStatusDto.PENDING -> Icon(
        Icons.Rounded.AccessTime,
        null,
        Modifier.size(AriSamDimensions.dp14),
        tint = if (isMine) AriSamPalette.white.copy(alpha = .65f) else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    ChatMessageStatusDto.FAILED -> Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(AriSamDimensions.dp14), tint = NocturneAmber)
    ChatMessageStatusDto.READ -> Icon(Icons.Rounded.DoneAll, null, Modifier.size(AriSamDimensions.dp14), tint = NocturneCyan)
    else -> Icon(
        Icons.Rounded.Done,
        null,
        Modifier.size(AriSamDimensions.dp14),
        tint = if (isMine) AriSamPalette.white.copy(alpha = .72f) else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
