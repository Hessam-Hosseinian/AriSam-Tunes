package com.arisamtunes.chat

import com.arisamtunes.auth.JwtService
import com.arisamtunes.config.JwtConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.Route
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import java.util.UUID

private val chatJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    @OptIn(ExperimentalSerializationApi::class)
    namingStrategy = JsonNamingStrategy.SnakeCase
}

private val chatConnections = ChatConnectionRegistry(chatJson)

fun Application.configureChatWebSockets() {
    install(WebSockets) {
        pingPeriodMillis = 25_000
        timeoutMillis = 45_000
        maxFrameSize = 64 * 1024
        masking = false
    }
}

fun Route.chatRoutes(
    repository: ChatRepository = ChatRepository(),
    jwtConfig: JwtConfig,
) {
    val jwtService = JwtService(jwtConfig)
    webSocket("/ws/chat") {
        val userId = call.request.queryParameters["token"]
            ?.let { token -> runCatching { jwtService.verifier.verify(token) }.getOrNull() }
            ?.takeIf { payload -> payload.getClaim("type").asString() == "access" }
            ?.subject
            ?.let { subject -> runCatching { UUID.fromString(subject) }.getOrNull() }

        if (userId == null) {
            sendError("AUTH_TOKEN_INVALID")
            close()
            return@webSocket
        }

        chatConnections.register(userId, this)
        sendEnvelope(ChatSocketEnvelope(type = "connected"))
        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val envelope = runCatching { chatJson.decodeFromString<ChatSocketEnvelope>(frame.readText()) }
                    .getOrElse {
                        sendError(if (it is SerializationException) "VALIDATION_ERROR" else "INTERNAL_ERROR")
                        continue
                    }
                when (envelope.type) {
                    "send_message" -> handleSendMessage(userId, envelope, repository)
                    else -> sendError("UNSUPPORTED_MESSAGE_TYPE")
                }
            }
        } finally {
            chatConnections.unregister(userId, this)
        }
    }
}

private suspend fun io.ktor.server.websocket.DefaultWebSocketServerSession.handleSendMessage(
    senderId: UUID,
    envelope: ChatSocketEnvelope,
    repository: ChatRepository,
) {
    val clientMessageId = envelope.clientMessageId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    val recipientId = envelope.recipientId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    val type = envelope.messageType ?: ChatMessageType.TEXT
    val songId = envelope.songId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    val content = envelope.content?.trim()

    if (clientMessageId == null || recipientId == null || recipientId == senderId) {
        sendError("VALIDATION_ERROR")
        return
    }
    if ((type == ChatMessageType.TEXT && content.isNullOrBlank()) || (type == ChatMessageType.SONG && songId == null)) {
        sendError("VALIDATION_ERROR")
        return
    }

    val message = runCatching { repository.saveMessage(senderId, clientMessageId, recipientId, type, content, songId) }
        .getOrElse {
            sendError("INTERNAL_ERROR")
            return
        }

    val sent = ChatSocketEnvelope(type = "message_sent", message = message)
    sendEnvelope(sent)
    chatConnections.sendTo(recipientId, ChatSocketEnvelope(type = "message_received", message = message))
}

private suspend fun io.ktor.server.websocket.DefaultWebSocketServerSession.sendEnvelope(envelope: ChatSocketEnvelope) {
    send(Frame.Text(chatJson.encodeToString(envelope)))
}

private suspend fun io.ktor.server.websocket.DefaultWebSocketServerSession.sendError(code: String) {
    sendEnvelope(ChatSocketEnvelope(type = "error", error = code))
}
