package com.arisamtunes.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.chat.ChatConnectionStatus
import com.arisamtunes.data.chat.ChatConversationDto
import com.arisamtunes.data.chat.ChatMessageDto
import com.arisamtunes.data.social.PublicUserDto

@Composable
fun ChatListRoute(
    onConversationClick: (String) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    when {
        state.isLoading -> Loading()
        state.hasError -> ErrorState(viewModel::refresh)
        state.conversations.isNotEmpty() -> ConversationList(state.conversations, onConversationClick)
        state.starters.isNotEmpty() -> StarterList(state.starters, onConversationClick)
        else -> EmptyState(R.string.chat_empty)
    }
}

@Composable
private fun ConversationList(items: List<ChatConversationDto>, onClick: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(items, key = { it.user.id }) { conversation ->
            PressScaleBox({ onClick(conversation.user.id) }, Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(conversation.user.displayName, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(conversation.latestMessage.content.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = { Icon(Icons.Rounded.Chat, null, tint = MaterialTheme.colorScheme.primary) },
                )
            }
        }
    }
}

@Composable
private fun StarterList(items: List<PublicUserDto>, onClick: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                stringResource(R.string.chat_start_with_following),
                modifier = Modifier.padding(AriSamThemeTokens.spacing.lg),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        items(items, key = PublicUserDto::id) { user ->
            PressScaleBox({ onClick(user.id) }, Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(user.displayName, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(stringResource(R.string.chat_tap_to_message)) },
                    leadingContent = { Icon(Icons.Rounded.Chat, null, tint = MaterialTheme.colorScheme.primary) },
                )
            }
        }
    }
}

@Composable
fun ChatDetailRoute(
    onBack: () -> Unit,
    viewModel: ChatDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
            Column(Modifier.weight(1f)) {
                Text(state.peer?.displayName ?: stringResource(R.string.chat), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(state.status.labelRes()), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
            AssistChip(onClick = {}, label = { Text(stringResource(state.status.labelRes())) })
        }
        when {
            state.isLoading -> Loading()
            state.hasError -> ErrorState(viewModel::refresh)
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(AriSamThemeTokens.spacing.lg),
                ) {
                    items(state.messages.asReversed(), key = ChatMessageDto::id) { message ->
                        MessageBubble(message = message, isMine = message.senderId == state.meId)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.draft,
                        onValueChange = viewModel::updateDraft,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.chat_message_hint)) },
                        singleLine = false,
                        maxLines = 4,
                    )
                    IconButton(onClick = viewModel::send, enabled = state.draft.isNotBlank()) {
                        Icon(Icons.Rounded.Send, stringResource(R.string.send))
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessageDto, isMine: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(.78f),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(message.content.orEmpty())
                Text(message.status.lowercase(), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun ChatConnectionStatus.labelRes() = when (this) {
    ChatConnectionStatus.Disconnected -> R.string.chat_status_offline
    ChatConnectionStatus.Connecting -> R.string.chat_status_connecting
    ChatConnectionStatus.Connected -> R.string.chat_status_connected
    ChatConnectionStatus.Reconnecting -> R.string.chat_status_reconnecting
}

@Composable
private fun Loading() = Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun ErrorState(onRetry: () -> Unit) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    TextButton(onClick = onRetry) { Icon(Icons.Rounded.Refresh, null); Text(stringResource(R.string.retry)) }
}

@Composable
private fun EmptyState(message: Int) = Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.Chat, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(message), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
