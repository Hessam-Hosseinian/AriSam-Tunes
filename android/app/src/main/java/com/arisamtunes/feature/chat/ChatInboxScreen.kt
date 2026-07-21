package com.arisamtunes.feature.chat

import com.arisamtunes.core.design.spacing.AriSamDimensions
import com.arisamtunes.core.design.colors.AriSamPalette
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.components.ShimmerBox
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.chat.ChatConversationDto
import com.arisamtunes.data.chat.ChatMessageTypeDto
import com.arisamtunes.data.social.PublicUserDto
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private val InboxCardShape = RoundedCornerShape(AriSamDimensions.dp28)
private val InboxRowShape = RoundedCornerShape(AriSamDimensions.dp24)
private val ChatCyan = AriSamPalette.cyan500
private val ChatViolet = AriSamPalette.violetSoft
private val ChatMint = AriSamPalette.emerald300

@Composable
fun ChatListRoute(
    onConversationClick: (String) -> Unit,
    onUserProfileClick: (String) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val conversations = viewModel.conversations.collectAsLazyPagingItems()
    val starters = viewModel.starters.collectAsLazyPagingItems()
    val results = viewModel.searchResults.collectAsLazyPagingItems()
    val motion = rememberInfiniteTransition(label = "chatInboxMotion")
    val phase by motion.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(7_000, easing = LinearEasing)),
        label = "chatInboxPhase",
    )
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = .055f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        InboxAuroraBackground(phase)
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(420)) + slideInVertically(
                    tween(560, easing = FastOutSlowInEasing),
                ) { -it / 4 },
            ) {
                ChatInboxHero(phase)
            }
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(380, delayMillis = 100)) + slideInVertically(
                    tween(520, delayMillis = 100, easing = FastOutSlowInEasing),
                ) { it / 3 },
            ) {
                ChatPeopleSearch(
                    query = state.searchQuery,
                    onQueryChange = viewModel::updateSearch,
                    onClear = viewModel::clearSearch,
                )
            }
            AnimatedContent(
                targetState = state.isSearching,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(260)) + slideInVertically(tween(360, easing = FastOutSlowInEasing)) { it / 10 }) togetherWith
                        (fadeOut(tween(160)) + slideOutVertically(tween(220)) { -it / 12 })
                },
                label = "chatInboxMode",
            ) { isSearching ->
                if (isSearching) {
                    AnimatedUserSearchResults(
                        results = results,
                        isPending = state.isSearchPending,
                        onProfile = onUserProfileClick,
                        onMessage = onConversationClick,
                    )
                } else {
                    AnimatedConversationInbox(
                        conversations = conversations,
                        starters = starters,
                        phase = phase,
                        onClick = onConversationClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun InboxAuroraBackground(phase: Float) {
    val primary = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .size(AriSamDimensions.dp230)
                .graphicsLayer {
                    translationX = 150f + sin(phase) * 70f
                    translationY = -110f + sin(phase * .7f) * 40f
                }
                .blur(AriSamDimensions.dp72)
                .background(primary.copy(alpha = .15f), CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .size(AriSamDimensions.dp190)
                .graphicsLayer {
                    translationX = -110f + sin(phase * .8f) * 45f
                    translationY = sin(phase * 1.1f) * 100f
                }
                .blur(AriSamDimensions.dp80)
                .background(ChatViolet.copy(alpha = .11f), CircleShape),
        )
    }
}

@Composable
private fun ChatInboxHero(phase: Float) {
    val spacing = AriSamThemeTokens.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        shape = InboxCardShape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .88f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            AriSamDimensions.dp1,
            Brush.linearGradient(
                listOf(ChatCyan.copy(alpha = .7f), ChatViolet.copy(alpha = .45f), AriSamPalette.transparent),
            ),
        ),
        shadowElevation = AriSamDimensions.dp6,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = AriSamDimensions.dp20, vertical = AriSamDimensions.dp17),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp16),
        ) {
            Box(Modifier.size(AriSamDimensions.dp66), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val pulse = .88f + ((sin(phase * 1.35f) + 1f) / 2f) * .12f
                    drawCircle(
                        brush = Brush.radialGradient(listOf(ChatCyan.copy(alpha = .3f), AriSamPalette.transparent)),
                        radius = size.minDimension * .5f * pulse,
                    )
                    drawCircle(
                        color = ChatCyan.copy(alpha = .28f),
                        radius = size.minDimension * .39f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = AriSamDimensions.dp1_5.toPx()),
                    )
                }
                Surface(
                    modifier = Modifier.size(AriSamDimensions.dp46),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .16f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Headphones, null, tint = ChatCyan, modifier = Modifier.size(AriSamDimensions.dp25))
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp3)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp6)) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = ChatMint, modifier = Modifier.size(AriSamDimensions.dp14))
                    Text(
                        stringResource(R.string.chat_inbox_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = ChatMint,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Text(
                    stringResource(R.string.chat_inbox_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.chat_inbox_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            AnimatedEqualizer(phase)
        }
    }
}

@Composable
private fun AnimatedEqualizer(phase: Float) {
    Canvas(Modifier.size(width = AriSamDimensions.dp38, height = AriSamDimensions.dp42)) {
        val bars = 5
        val barWidth = AriSamDimensions.dp3.toPx()
        val gap = (size.width - bars * barWidth) / (bars - 1)
        repeat(bars) { index ->
            val wave = ((sin(phase * 2f + index * .9f) + 1f) / 2f)
            val barHeight = size.height * (.22f + wave * .68f)
            val x = index * (barWidth + gap) + barWidth / 2f
            drawLine(
                brush = Brush.verticalGradient(listOf(ChatCyan, ChatViolet)),
                start = Offset(x, (size.height - barHeight) / 2f),
                end = Offset(x, (size.height + barHeight) / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ChatPeopleSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val spacing = AriSamThemeTokens.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val focusScale by animateFloatAsState(
        targetValue = if (focused) 1.012f else 1f,
        animationSpec = spring(dampingRatio = .72f, stiffness = 520f),
        label = "chatSearchFocus",
    )
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.xs)
            .graphicsLayer { scaleX = focusScale; scaleY = focusScale },
        interactionSource = interactionSource,
        placeholder = {
            Text(
                stringResource(R.string.chat_people_search_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            AnimatedContent(query.isBlank(), label = "chatSearchIcon") { empty ->
                Icon(
                    if (empty) Icons.Rounded.PersonSearch else Icons.Rounded.Search,
                    null,
                    tint = if (focused) ChatCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotBlank(),
                enter = fadeIn() + scaleIn(initialScale = .7f),
                exit = fadeOut() + scaleOut(targetScale = .7f),
            ) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.clear))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(AriSamDimensions.dp22),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .9f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .74f),
            focusedBorderColor = ChatCyan.copy(alpha = .86f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .66f),
            cursorColor = ChatCyan,
        ),
    )
}

@Composable
private fun AnimatedConversationInbox(
    conversations: LazyPagingItems<ChatConversationDto>,
    starters: LazyPagingItems<PublicUserDto>,
    phase: Float,
    onClick: (String) -> Unit,
) {
    when {
        conversations.loadState.refresh is LoadState.Loading && conversations.itemCount == 0 -> AnimatedChatSkeleton()
        conversations.loadState.refresh is LoadState.Error && conversations.itemCount == 0 -> AnimatedInboxError(conversations::retry)
        else -> LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = AriSamDimensions.dp8, bottom = AriSamDimensions.dp30),
            verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp9),
        ) {
            when {
                starters.loadState.refresh is LoadState.Loading && starters.itemCount == 0 -> item(
                    key = "starter_loading",
                    contentType = "starter_state",
                ) { StarterPeopleSkeleton() }

                starters.itemCount > 0 -> {
                    item(key = "starter_heading", contentType = "heading") {
                        InboxSectionHeading(
                            icon = Icons.Rounded.GraphicEq,
                            title = stringResource(R.string.chat_start_with_following),
                            subtitle = stringResource(R.string.chat_start_with_following_subtitle),
                        )
                    }
                    item(key = "starter_people", contentType = "starter_people") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = AriSamThemeTokens.spacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp15),
                        ) {
                            items(
                                count = starters.itemCount,
                                key = starters.itemKey(PublicUserDto::id),
                                contentType = starters.itemContentType { "starter_user" },
                            ) { index ->
                                starters[index]?.let { user ->
                                    StarterPerson(
                                        user = user,
                                        index = index,
                                        phase = phase,
                                        onClick = { onClick(user.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (conversations.itemCount > 0) {
                item(key = "recent_heading", contentType = "heading") {
                    InboxSectionHeading(
                        icon = Icons.Rounded.ChatBubble,
                        title = stringResource(R.string.chat_recent_messages),
                        subtitle = stringResource(R.string.chat_recent_messages_subtitle),
                    )
                }
            }
            items(
                count = conversations.itemCount,
                key = conversations.itemKey { it.user.id },
                contentType = conversations.itemContentType { "conversation" },
            ) { index ->
                conversations[index]?.let { conversation ->
                    AnimatedConversationRow(
                        conversation = conversation,
                        index = index,
                        onClick = onClick,
                    )
                }
            }
            if (conversations.itemCount == 0 && conversations.loadState.refresh !is LoadState.Loading) {
                item(key = "empty_inbox", contentType = "state") { AnimatedInboxEmpty() }
            }
            when (conversations.loadState.append) {
                is LoadState.Loading -> item(key = "append_loading", contentType = "state") {
                    ShimmerBox(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AriSamThemeTokens.spacing.lg)
                            .height(AriSamDimensions.dp88),
                    )
                }
                is LoadState.Error -> item(key = "append_error", contentType = "state") {
                    TextButton(onClick = conversations::retry, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Refresh, null)
                        Spacer(Modifier.width(AriSamDimensions.dp6))
                        Text(stringResource(R.string.retry))
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun InboxSectionHeading(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = AriSamThemeTokens.spacing.lg, vertical = AriSamDimensions.dp10),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp11),
    ) {
        Surface(shape = CircleShape, color = ChatCyan.copy(alpha = .12f)) {
            Box(Modifier.size(AriSamDimensions.dp36), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = ChatCyan, modifier = Modifier.size(AriSamDimensions.dp19))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StarterPerson(
    user: PublicUserDto,
    index: Int,
    phase: Float,
    onClick: () -> Unit,
) {
    var visible by remember(user.id) { mutableStateOf(false) }
    LaunchedEffect(user.id) {
        delay((index.coerceAtMost(7) * 45L))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + scaleIn(spring(dampingRatio = .72f, stiffness = 430f), initialScale = .72f),
    ) {
        PressScaleBox(onClick) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp7),
                modifier = Modifier.width(AriSamDimensions.dp76),
            ) {
                OrbitingUserAvatar(user = user, phase = phase + index * .68f)
                Text(
                    user.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun OrbitingUserAvatar(user: PublicUserDto, phase: Float) {
    Box(Modifier.size(AriSamDimensions.dp69), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val pulse = .94f + ((sin(phase) + 1f) / 2f) * .06f
            drawCircle(
                brush = Brush.sweepGradient(listOf(ChatCyan, ChatViolet, ChatMint, ChatCyan)),
                radius = size.minDimension * .48f * pulse,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = AriSamDimensions.dp2.toPx()),
            )
            val orbitRadius = size.minDimension * .44f
            drawCircle(
                color = ChatMint,
                radius = AriSamDimensions.dp2_6.toPx(),
                center = Offset(
                    center.x + kotlin.math.cos(phase) * orbitRadius,
                    center.y + sin(phase) * orbitRadius,
                ),
            )
        }
        UserAvatar(
            user,
            Modifier
                .size(AriSamDimensions.dp57)
                .border(AriSamDimensions.dp2, MaterialTheme.colorScheme.surface, CircleShape),
        )
    }
}

@Composable
private fun AnimatedConversationRow(
    conversation: ChatConversationDto,
    index: Int,
    onClick: (String) -> Unit,
) {
    var visible by remember(conversation.user.id) { mutableStateOf(false) }
    LaunchedEffect(conversation.user.id) {
        delay((index.coerceAtMost(8) * 42L))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(
            tween(440, easing = FastOutSlowInEasing),
        ) { it / 3 },
    ) {
        ConversationCard(conversation, onClick)
    }
}

@Composable
private fun ConversationCard(
    conversation: ChatConversationDto,
    onClick: (String) -> Unit,
) {
    val hasUnread = conversation.unreadCount > 0
    val elevation by animateDpAsState(
        targetValue = if (hasUnread) AriSamDimensions.dp5 else AriSamDimensions.dp1,
        animationSpec = spring(stiffness = 420f),
        label = "conversationElevation",
    )
    PressScaleBox(
        onClick = { onClick(conversation.user.id) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AriSamThemeTokens.spacing.lg),
    ) {
        Surface(
            shape = InboxRowShape,
            color = if (hasUnread) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = .4f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .88f)
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(
                AriSamDimensions.dp1,
                if (hasUnread) ChatCyan.copy(alpha = .48f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .42f),
            ),
            shadowElevation = elevation,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AriSamDimensions.dp14, vertical = AriSamDimensions.dp13),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ConversationAvatar(conversation.user, hasUnread)
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = AriSamDimensions.dp13),
                    verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp5),
                ) {
                    Text(
                        conversation.user.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp6)) {
                        if (conversation.latestMessage.messageType == ChatMessageTypeDto.SONG) {
                            Icon(Icons.Rounded.MusicNote, null, tint = ChatViolet, modifier = Modifier.size(AriSamDimensions.dp16))
                        }
                        Text(
                            if (conversation.latestMessage.messageType == ChatMessageTypeDto.SONG) {
                                stringResource(R.string.chat_shared_song)
                            } else {
                                conversation.latestMessage.content.orEmpty()
                            },
                            modifier = Modifier.weight(1f, fill = false),
                            color = if (hasUnread) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp9)) {
                    Text(
                        messageTime(conversation.latestMessage.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasUnread) ChatCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                    )
                    AnimatedVisibility(
                        visible = hasUnread,
                        enter = scaleIn(spring(dampingRatio = .65f, stiffness = 520f)) + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ChatCyan,
                            shadowElevation = AriSamDimensions.dp3,
                        ) {
                            Text(
                                conversation.unreadCount.coerceAtMost(99).toString(),
                                modifier = Modifier.padding(horizontal = AriSamDimensions.dp8, vertical = AriSamDimensions.dp3),
                                color = AriSamPalette.chatInk,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationAvatar(user: PublicUserDto, hasUnread: Boolean) {
    Box(Modifier.size(AriSamDimensions.dp61), contentAlignment = Alignment.Center) {
        if (hasUnread) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.sweepGradient(listOf(ChatCyan, ChatViolet, ChatCyan)),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = AriSamDimensions.dp2.toPx()),
                )
            }
        }
        UserAvatar(user, Modifier.size(if (hasUnread) AriSamDimensions.dp52 else AriSamDimensions.dp56))
        if (hasUnread) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(AriSamDimensions.dp13)
                    .clip(CircleShape)
                    .background(ChatMint)
                    .border(AriSamDimensions.dp2, MaterialTheme.colorScheme.surfaceContainer, CircleShape),
            )
        }
    }
}

@Composable
private fun AnimatedUserSearchResults(
    results: LazyPagingItems<PublicUserDto>,
    isPending: Boolean,
    onProfile: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    when {
        isPending -> AnimatedChatSkeleton(compact = true)
        results.loadState.refresh is LoadState.Loading -> AnimatedChatSkeleton(compact = true)
        results.loadState.refresh is LoadState.Error -> AnimatedInboxError(results::retry)
        results.itemCount == 0 -> AnimatedSearchEmpty()
        else -> LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = AriSamDimensions.dp12, bottom = AriSamDimensions.dp30),
            verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp9),
        ) {
            item(key = "search_heading", contentType = "heading") {
                InboxSectionHeading(
                    icon = Icons.Rounded.PersonSearch,
                    title = stringResource(R.string.chat_people_results_title),
                    subtitle = stringResource(R.string.chat_people_results_subtitle),
                )
            }
            items(
                count = results.itemCount,
                key = results.itemKey(PublicUserDto::id),
                contentType = results.itemContentType { "search_user" },
            ) { index ->
                results[index]?.let { user ->
                    AnimatedSearchUserRow(
                        user = user,
                        index = index,
                        onProfile = { onProfile(user.id) },
                        onMessage = { onMessage(user.id) },
                    )
                }
            }
            if (results.loadState.append is LoadState.Loading) {
                item(key = "search_append_loading", contentType = "state") {
                    ShimmerBox(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AriSamThemeTokens.spacing.lg)
                            .height(AriSamDimensions.dp82),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedSearchUserRow(
    user: PublicUserDto,
    index: Int,
    onProfile: () -> Unit,
    onMessage: () -> Unit,
) {
    var visible by remember(user.id) { mutableStateOf(false) }
    LaunchedEffect(user.id) {
        delay(index.coerceAtMost(8) * 38L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it / 3 },
    ) {
        PressScaleBox(
            onClick = onProfile,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AriSamThemeTokens.spacing.lg),
        ) {
            Surface(
                shape = InboxRowShape,
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .88f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(AriSamDimensions.dp1, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .42f)),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(AriSamDimensions.dp13),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchResultAvatar(user)
                    Column(Modifier.weight(1f).padding(horizontal = AriSamDimensions.dp13)) {
                        Text(user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            stringResource(R.string.social_followers_count, user.followersCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PressScaleBox(onMessage) {
                        Surface(
                            shape = CircleShape,
                            color = ChatCyan.copy(alpha = .14f),
                            border = BorderStroke(AriSamDimensions.dp1, ChatCyan.copy(alpha = .32f)),
                        ) {
                            Row(
                                Modifier.padding(horizontal = AriSamDimensions.dp12, vertical = AriSamDimensions.dp9),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp4),
                            ) {
                                Icon(Icons.Rounded.ChatBubble, stringResource(R.string.chat_tap_to_message), tint = ChatCyan, modifier = Modifier.size(AriSamDimensions.dp17))
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = ChatCyan, modifier = Modifier.size(AriSamDimensions.dp15))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultAvatar(user: PublicUserDto) {
    Box(
        Modifier
            .size(AriSamDimensions.dp55)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(ChatCyan.copy(alpha = .8f), ChatViolet.copy(alpha = .8f))))
            .padding(AriSamDimensions.dp2),
    ) {
        if (user.avatarUrl.isNullOrBlank()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(user.displayName.take(1).uppercase(), fontWeight = FontWeight.Black, color = ChatCyan)
            }
        } else {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = user.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        }
    }
}

@Composable
private fun AnimatedChatSkeleton(compact: Boolean = false) {
    val motion = rememberInfiniteTransition(label = "chatSkeletonMotion")
    val pulse by motion.animateFloat(
        initialValue = .65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850), repeatMode = RepeatMode.Reverse),
        label = "chatSkeletonPulse",
    )
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = AriSamThemeTokens.spacing.lg, vertical = AriSamDimensions.dp14)
            .graphicsLayer { alpha = pulse },
        verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp11),
    ) {
        if (!compact) {
            ShimmerBox(Modifier.fillMaxWidth(.52f).height(AriSamDimensions.dp22))
            Row(horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp14)) {
                repeat(4) { ShimmerBox(Modifier.size(AriSamDimensions.dp64).clip(CircleShape)) }
            }
            Spacer(Modifier.height(AriSamDimensions.dp4))
        }
        repeat(if (compact) 6 else 5) {
            ShimmerBox(Modifier.fillMaxWidth().height(AriSamDimensions.dp86).clip(InboxRowShape))
        }
    }
}

@Composable
private fun StarterPeopleSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10)) {
        InboxSectionHeading(
            icon = Icons.Rounded.GraphicEq,
            title = stringResource(R.string.chat_start_with_following),
            subtitle = stringResource(R.string.chat_start_with_following_subtitle),
        )
        Row(
            Modifier.padding(horizontal = AriSamThemeTokens.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp14),
        ) {
            repeat(4) { ShimmerBox(Modifier.size(AriSamDimensions.dp64).clip(CircleShape)) }
        }
    }
}

@Composable
private fun AnimatedInboxEmpty() {
    EmptyInboxArtwork(
        icon = Icons.Rounded.Headphones,
        title = stringResource(R.string.chat_empty_title),
        message = stringResource(R.string.chat_empty),
    )
}

@Composable
private fun AnimatedSearchEmpty() {
    EmptyInboxArtwork(
        icon = Icons.Rounded.PersonSearch,
        title = stringResource(R.string.chat_people_empty_title),
        message = stringResource(R.string.social_search_empty),
    )
}

@Composable
private fun EmptyInboxArtwork(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
) {
    val motion = rememberInfiniteTransition(label = "emptyChatOrbit")
    val orbit by motion.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(5_000, easing = LinearEasing)),
        label = "emptyChatOrbitPhase",
    )
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = AriSamDimensions.dp36, vertical = AriSamDimensions.dp28),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10),
    ) {
        Box(Modifier.size(AriSamDimensions.dp112), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color = ChatCyan.copy(alpha = .18f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = AriSamDimensions.dp1.toPx()),
                )
                val radius = size.minDimension * .45f
                repeat(3) { index ->
                    val angle = orbit + index * (2f * PI / 3f).toFloat()
                    drawCircle(
                        color = listOf(ChatCyan, ChatViolet, ChatMint)[index],
                        radius = (3 + index).dp.toPx(),
                        center = Offset(center.x + kotlin.math.cos(angle) * radius, center.y + sin(angle) * radius),
                    )
                }
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
                Box(Modifier.size(AriSamDimensions.dp68), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = ChatCyan, modifier = Modifier.size(AriSamDimensions.dp32))
                }
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun AnimatedInboxError(onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(AriSamDimensions.dp32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error.copy(alpha = .12f)) {
            Box(Modifier.size(AriSamDimensions.dp72), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Refresh, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(AriSamDimensions.dp30))
            }
        }
        Spacer(Modifier.height(AriSamDimensions.dp12))
        Text(
            stringResource(R.string.chat_inbox_error_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(onClick = onRetry) {
            Icon(Icons.Rounded.Refresh, null)
            Spacer(Modifier.width(AriSamDimensions.dp6))
            Text(stringResource(R.string.retry))
        }
    }
}
