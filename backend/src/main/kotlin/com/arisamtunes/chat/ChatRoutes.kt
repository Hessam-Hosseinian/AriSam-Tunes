package com.arisamtunes.chat

import com.arisamtunes.auth.JwtService
import com.arisamtunes.config.JwtConfig
import com.arisamtunes.model.ApiException
import com.arisamtunes.model.ErrorCode
import com.arisamtunes.model.PaginationMeta
import com.arisamtunes.social.SocialRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
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
import java.time.Instant
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
    socialRepository: SocialRepository = SocialRepository(),
    jwtConfig: JwtConfig,
) {
    val jwtService = JwtService(jwtConfig)
    authenticate("auth-jwt") {
        route("/chat") {
            get("/conversations") {
                val viewerId = call.userId()
                val conversations = repository.conversationPeers(viewerId).mapNotNull { (peerId, latest) ->
                    socialRepository.user(peerId, viewerId)?.let { peer -> ChatConversationResponse(peer, latest) }
                }
                call.respond(ChatConversationListResponse(conversations))
            }
            get("/{userId}/messages") {
                val peerId = call.pathUserId()
                val (page, size) = call.pageRequest()
                val since = call.request.queryParameters["since"]?.let {
                    runCatching { Instant.parse(it) }.getOrElse { throw validation("since must be an ISO-8601 timestamp") }
                }
                val (items, total) = repository.conversation(call.userId(), peerId, page, size, since)
                call.respond(ChatMessageListResponse(items, PaginationMeta(page, size, total, pages(total, size))))
            }
        }
    }
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
                    "typing_start", "typing_stop" -> forwardTyping(userId, envelope)
                    "message_delivered" -> handleReceipt(userId, envelope, repository, read = false)
                    "message_read" -> handleReceipt(userId, envelope, repository, read = true)
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
    val recipientSessions = chatConnections.sendTo(recipientId, ChatSocketEnvelope(type = "message_received", message = message))
    if (recipientSessions > 0) {
        repository.markDelivered(UUID.fromString(message.id), recipientId)?.let { delivered ->
            val receipt = ChatSocketEnvelope(type = "message_delivered", messageId = delivered.id, message = delivered)
            sendEnvelope(receipt)
            chatConnections.sendTo(recipientId, receipt)
        }
    }
}

private suspend fun io.ktor.server.websocket.DefaultWebSocketServerSession.forwardTyping(
    senderId: UUID,
    envelope: ChatSocketEnvelope,
) {
    val recipientId = envelope.recipientId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    if (recipientId == null || recipientId == senderId) {
        sendError("VALIDATION_ERROR")
        return
    }
    chatConnections.sendTo(
        recipientId,
        ChatSocketEnvelope(type = envelope.type, senderId = senderId.toString(), recipientId = recipientId.toString()),
    )
}

private suspend fun io.ktor.server.websocket.DefaultWebSocketServerSession.handleReceipt(
    recipientId: UUID,
    envelope: ChatSocketEnvelope,
    repository: ChatRepository,
    read: Boolean,
) {
    val messageId = envelope.messageId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    if (messageId == null) {
        sendError("VALIDATION_ERROR")
        return
    }
    val updated = runCatching {
        if (read) repository.markRead(messageId, recipientId) else repository.markDelivered(messageId, recipientId)
    }.getOrElse {
        sendError("INTERNAL_ERROR")
        return
    }
    if (updated == null) {
        sendError("MESSAGE_NOT_FOUND")
        return
    }
    val event = ChatSocketEnvelope(type = if (read) "message_read" else "message_delivered", messageId = updated.id, message = updated)
    sendEnvelope(event)
    chatConnections.sendTo(UUID.fromString(updated.senderId), event)
}

private suspend fun io.ktor.server.websocket.DefaultWebSocketServerSession.sendEnvelope(envelope: ChatSocketEnvelope) {
    send(Frame.Text(chatJson.encodeToString(envelope)))
}

private suspend fun io.ktor.server.websocket.DefaultWebSocketServerSession.sendError(code: String) {
    sendEnvelope(ChatSocketEnvelope(type = "error", error = code))
}

private fun io.ktor.server.application.ApplicationCall.userId(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)

private fun io.ktor.server.application.ApplicationCall.pathUserId(): UUID =
    runCatching { UUID.fromString(parameters["userId"]) }.getOrElse { throw validation("userId must be a valid UUID") }

private fun io.ktor.server.application.ApplicationCall.pageRequest(): Pair<Int, Int> {
    val page = request.queryParameters["page"]?.toIntOrNull() ?: 0
    val size = request.queryParameters["size"]?.toIntOrNull() ?: 30
    if (page < 0 || size !in 1..100) throw validation("page must be at least 0 and size must be between 1 and 100")
    return page to size
}

private fun pages(total: Long, size: Int) = if (total == 0L) 0 else ((total - 1) / size + 1).toInt()
private fun validation(message: String) = ApiException(HttpStatusCode.BadRequest, ErrorCode.VALIDATION_ERROR, message)
