package com.arisamtunes.data.chat

import android.util.Base64
import com.arisamtunes.BuildConfig
import com.arisamtunes.data.auth.AuthTokenStore
import com.arisamtunes.data.auth.UserDto
import com.arisamtunes.data.local.dao.ChatMessageDao
import com.arisamtunes.data.local.entity.ChatMessageEntity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.Json

@Singleton
class ChatRepository @Inject constructor(
    private val client: HttpClient,
    private val tokenStore: AuthTokenStore,
    private val chatMessageDao: ChatMessageDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }
    private var activeRecipientId: String? = null
    private val _status = MutableStateFlow(ChatConnectionStatus.Disconnected)
    val status: StateFlow<ChatConnectionStatus> = _status
    private val _events = MutableSharedFlow<ChatSocketEnvelopeDto>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<ChatSocketEnvelopeDto> = _events
    private var activeSession: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession? = null
    private var cachedUserId: String? = null

    suspend fun currentUserId(): String =
        cachedUserId ?: tokenStore.load()?.accessToken?.subjectFromJwt()?.also { cachedUserId = it }
            ?: client.get("auth/me").body<UserDto>().id.also { cachedUserId = it }

    suspend fun conversations(): List<ChatConversationDto> =
        client.get("chat/conversations").body<ChatConversationListDto>().items.also { conversations ->
            val meId = runCatching { currentUserId() }.getOrNull()
            if (meId != null) chatMessageDao.upsertAll(conversations.map { it.latestMessage.toEntity(meId) })
        }

    suspend fun messages(userId: String, page: Int = 0, size: Int = 50): List<ChatMessageDto> {
        syncMessages(userId, page, size)
        return chatMessageDao.conversationSnapshot(userId).map(ChatMessageEntity::toDto)
    }

    fun observeMessages(userId: String) = chatMessageDao.observeConversation(userId).map { messages ->
        messages.map(ChatMessageEntity::toDto)
    }

    suspend fun syncMessages(userId: String, page: Int = 0, size: Int = 50) {
        val meId = currentUserId()
        val since = if (page == 0) chatMessageDao.latestTimestamp(userId) else null
        val remoteMessages = client.get("chat/$userId/messages") {
            parameter("page", page)
            parameter("size", size)
            if (since != null) parameter("since", since)
        }.body<ChatMessageListDto>().items
        chatMessageDao.upsertAll(remoteMessages.map { it.toEntity(meId) })
    }

    fun connect(recipientId: String) {
        activeRecipientId = recipientId
        scope.launch {
            var attempt = 0
            while (activeRecipientId == recipientId) {
                val token = tokenStore.load()?.accessToken
                if (token == null) {
                    _status.value = ChatConnectionStatus.Disconnected
                    return@launch
                }
                _status.value = if (attempt == 0) ChatConnectionStatus.Connecting else ChatConnectionStatus.Reconnecting
                runCatching {
                    client.webSocket(urlString = chatSocketUrl(token)) {
                        attempt = 0
                        _status.value = ChatConnectionStatus.Connected
                        activeSession = this
                        launch { runCatching { syncMessages(recipientId) } }
                        launch { runCatching { flushPendingMessages() } }
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                runCatching { json.decodeFromString<ChatSocketEnvelopeDto>(frame.readText()) }
                                    .onSuccess { event ->
                                        persistSocketEvent(event)
                                        _events.emit(event)
                                    }
                            }
                        }
                        activeSession = null
                    }
                }
                if (activeRecipientId != recipientId) break
                attempt++
                _status.value = ChatConnectionStatus.Reconnecting
                kotlinx.coroutines.delay(backoffDelayMillis(attempt))
            }
        }
    }

    fun disconnect(recipientId: String) {
        if (activeRecipientId == recipientId) activeRecipientId = null
        activeSession = null
        _status.value = ChatConnectionStatus.Disconnected
    }

    suspend fun sendText(recipientId: String, text: String) {
        val meId = currentUserId()
        val clientMessageId = UUID.randomUUID().toString()
        val pending = ChatMessageEntity(
            messageId = clientMessageId,
            conversationUserId = recipientId,
            senderUserId = meId,
            receiverUserId = recipientId,
            body = text,
            messageType = "TEXT",
            deliveryState = "PENDING",
            isMine = true,
            createdAt = Instant.now().toString(),
            cachedAt = System.currentTimeMillis(),
        )
        chatMessageDao.upsert(pending)
        sendPendingMessage(pending)
    }

    private suspend fun flushPendingMessages() {
        chatMessageDao.pendingMessages().forEach { pending ->
            sendPendingMessage(pending)
        }
    }

    private suspend fun sendPendingMessage(message: ChatMessageEntity) {
        activeSession?.send(
            Frame.Text(
                json.encodeToString(
                    ChatSocketEnvelopeDto(
                        type = "send_message",
                        clientMessageId = message.messageId,
                        recipientId = message.receiverUserId,
                        messageType = message.messageType,
                        content = message.body.takeIf { message.messageType == "TEXT" },
                    ),
                ),
            ),
        )
    }

    private suspend fun persistSocketEvent(event: ChatSocketEnvelopeDto) {
        val meId = runCatching { currentUserId() }.getOrNull() ?: return
        event.message?.let { message ->
            if (message.clientMessageId.isNotBlank()) chatMessageDao.deletePending(message.clientMessageId)
            chatMessageDao.upsert(message.toEntity(meId))
        }
        if (event.message == null && event.messageId != null && event.type in ReceiptTypes) {
            val state = if (event.type == "message_read") "READ" else "DELIVERED"
            chatMessageDao.updateReceipt(event.messageId, state, deliveredAt = null, readAt = null)
        }
    }

    private fun chatSocketUrl(token: String): String {
        val base = BuildConfig.API_BASE_URL.trimEnd('/').replaceFirst("http://", "ws://").replaceFirst("https://", "wss://")
        return "$base/api/v1/ws/chat?token=$token"
    }

    private fun backoffDelayMillis(attempt: Int): Long {
        val exponential = min(30_000L, 1_000L * (1L shl attempt.coerceIn(0, 5)))
        return exponential + Random.nextLong(0L, 450L)
    }

    private companion object {
        val ReceiptTypes = setOf("message_delivered", "message_read")
    }
}

private fun ChatMessageDto.toEntity(meId: String): ChatMessageEntity {
    val mine = senderId == meId
    val peerId = if (mine) recipientId else senderId
    return ChatMessageEntity(
        messageId = id,
        conversationUserId = peerId,
        senderUserId = senderId,
        receiverUserId = recipientId,
        body = content.orEmpty(),
        messageType = messageType,
        deliveryState = status,
        isMine = mine,
        createdAt = createdAt,
        sentAt = createdAt,
        deliveredAt = deliveredAt,
        readAt = readAt,
        cachedAt = System.currentTimeMillis(),
    )
}

private fun ChatMessageEntity.toDto(): ChatMessageDto = ChatMessageDto(
    id = messageId,
    clientMessageId = if (deliveryState == "PENDING") messageId else messageId,
    senderId = senderUserId,
    recipientId = receiverUserId,
    messageType = messageType,
    content = body,
    status = deliveryState,
    createdAt = createdAt,
    deliveredAt = deliveredAt,
    readAt = readAt,
)

private fun String.subjectFromJwt(): String? = runCatching {
    val payload = split('.').getOrNull(1) ?: return null
    val normalized = payload.padEnd(payload.length + (4 - payload.length % 4) % 4, '=')
    val decoded = Base64.decode(normalized, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING).decodeToString()
    Json.decodeFromString<JsonObject>(decoded)["sub"]?.jsonPrimitive?.content
}.getOrNull()
