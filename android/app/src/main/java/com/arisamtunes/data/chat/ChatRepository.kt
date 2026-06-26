package com.arisamtunes.data.chat

import com.arisamtunes.BuildConfig
import com.arisamtunes.data.auth.AuthTokenStore
import com.arisamtunes.data.auth.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class ChatRepository @Inject constructor(
    private val client: HttpClient,
    private val tokenStore: AuthTokenStore,
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

    suspend fun currentUserId(): String = client.get("auth/me").body<UserDto>().id

    suspend fun conversations(): List<ChatConversationDto> = client.get("chat/conversations").body<ChatConversationListDto>().items

    suspend fun messages(userId: String, page: Int = 0, size: Int = 50): List<ChatMessageDto> =
        client.get("chat/$userId/messages") {
            parameter("page", page)
            parameter("size", size)
        }.body<ChatMessageListDto>().items.reversed()

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
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                runCatching { json.decodeFromString<ChatSocketEnvelopeDto>(frame.readText()) }
                                    .onSuccess { _events.emit(it) }
                            }
                        }
                        activeSession = null
                    }
                }
                if (activeRecipientId != recipientId) break
                attempt++
                kotlinx.coroutines.delay((1_500L * attempt).coerceAtMost(6_000L))
            }
        }
    }

    fun disconnect(recipientId: String) {
        if (activeRecipientId == recipientId) activeRecipientId = null
        activeSession = null
        _status.value = ChatConnectionStatus.Disconnected
    }

    suspend fun sendText(recipientId: String, text: String) {
        activeSession?.send(
            Frame.Text(
                json.encodeToString(
                    ChatSocketEnvelopeDto(
                        type = "send_message",
                        clientMessageId = UUID.randomUUID().toString(),
                        recipientId = recipientId,
                        messageType = "TEXT",
                        content = text,
                    ),
                ),
            ),
        )
    }

    private fun chatSocketUrl(token: String): String {
        val base = BuildConfig.API_BASE_URL.trimEnd('/').replaceFirst("http://", "ws://").replaceFirst("https://", "wss://")
        return "$base/api/v1/ws/chat?token=$token"
    }
}
