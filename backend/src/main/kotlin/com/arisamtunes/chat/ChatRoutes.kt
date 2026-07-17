package com.arisamtunes.chat

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
import io.ktor.websocket.readText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

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
) {
    authenticate("auth-jwt") {
        route("/chat") {
            get("/conversations") {
                val viewerId = call.userId()
                val size = call.pageSize(default = 30)
                val cursor = call.optionalCursor()
                val page = repository.conversationPeers(viewerId, cursor, size)
                val conversations = withContext(Dispatchers.IO) {
                    page.items.mapNotNull { conversation ->
                        socialRepository.user(conversation.peerId, viewerId)?.let { peer ->
                            ChatConversationResponse(peer, conversation.latestMessage, conversation.unreadCount)
                        }
                    }
                }
                call.respond(ChatConversationListResponse(conversations, page.nextCursor, page.hasMore))
            }
            get("/{userId}/messages") {
                val peerId = call.pathUserId()
                val (page, size) = call.pageRequest()
                val since = call.request.queryParameters["since"]?.let {
                    runCatching { Instant.parse(it) }.getOrElse { throw validation("since must be an ISO-8601 timestamp") }
                }
                val cursorRequested = call.request.queryParameters.contains("cursor")
                if (cursorRequested) {
                    val result = repository.conversationBefore(call.userId(), peerId, call.optionalCursor(), size)
                    call.respond(ChatMessageListResponse(result.items, nextCursor = result.nextCursor, hasMore = result.hasMore))
                } else {
                    val (items, total) = repository.conversation(call.userId(), peerId, page, size, since)
                    call.respond(ChatMessageListResponse(items, PaginationMeta(page, size, total, pages(total, size))))
                }
            }
            get("/{userId}/search") {
                val peerId = call.pathUserId()
                val query = call.request.queryParameters["q"]?.trim().orEmpty()
                if (query.length !in 2..100) throw validation("q must contain between 2 and 100 characters")
                call.respond(ChatMessageListResponse(repository.searchMessages(call.userId(), peerId, query, call.pageSize(default = 30))))
            }
            get("/sync") {
                val size = call.pageSize(default = 100)
                val result = repository.syncChanges(call.userId(), call.optionalCursor(), size)
                call.respond(ChatSyncResponse(result.items, result.nextCursor, result.hasMore))
            }
        }
        webSocket("/ws/chat") {
            val userId = call.userId()
            chatConnections.register(userId, this)
            sendEnvelope(ChatSocketEnvelope(type = ChatSocketType.CONNECTED))
            repository.markPendingDelivered(userId).forEach { delivered ->
                val receipt = ChatSocketEnvelope(
                    type = ChatSocketType.MESSAGE_DELIVERED,
                    messageId = delivered.id,
                    message = delivered,
                )
                chatConnections.sendTo(UUID.fromString(delivered.senderId), receipt)
                chatConnections.sendTo(userId, receipt)
            }
            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val envelope = runCatching { chatJson.decodeFromString<ChatSocketEnvelope>(frame.readText()) }
                        .getOrElse {
                            sendError(if (it is SerializationException) "VALIDATION_ERROR" else "INTERNAL_ERROR")
                            continue
                        }
                    if (envelope.protocolVersion != CHAT_PROTOCOL_VERSION) {
                        sendError("UNSUPPORTED_PROTOCOL_VERSION")
                        continue
                    }
                    try {
                        when (envelope.type) {
                            ChatSocketType.SEND_MESSAGE -> handleSendMessage(userId, envelope, repository)
                            ChatSocketType.EDIT_MESSAGE -> handleMessageMutation(userId, envelope, repository, MessageMutation.Edit)
                            ChatSocketType.DELETE_MESSAGE -> handleMessageMutation(userId, envelope, repository, MessageMutation.Delete)
                            ChatSocketType.ADD_REACTION -> handleMessageMutation(userId, envelope, repository, MessageMutation.AddReaction)
                            ChatSocketType.REMOVE_REACTION -> handleMessageMutation(userId, envelope, repository, MessageMutation.RemoveReaction)
                            ChatSocketType.TYPING_START, ChatSocketType.TYPING_STOP -> forwardTyping(userId, envelope)
                            ChatSocketType.MESSAGE_DELIVERED -> handleReceipt(userId, envelope, repository, read = false)
                            ChatSocketType.MESSAGE_READ -> handleReceipt(userId, envelope, repository, read = true)
                            else -> sendError("UNSUPPORTED_MESSAGE_TYPE")
                        }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Throwable) {
                        sendError("INTERNAL_ERROR", requestType = envelope.type, messageId = envelope.messageId)
                    }
                }
            } finally {
                chatConnections.unregister(userId, this)
            }
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
    val replyToId = envelope.replyToId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    val content = envelope.content?.trim()
    val persistedContent = content.takeIf { type == ChatMessageType.TEXT }

    if (clientMessageId == null || recipientId == null || recipientId == senderId) {
        sendError("VALIDATION_ERROR", envelope.clientMessageId)
        return
    }
    if ((type == ChatMessageType.TEXT && (content.isNullOrBlank() || content.length > MaxMessageLength)) ||
        (type == ChatMessageType.SONG && songId == null)
    ) {
        sendError("VALIDATION_ERROR", envelope.clientMessageId)
        return
    }

    val result = runCatching { repository.saveMessage(senderId, clientMessageId, recipientId, type, persistedContent, songId, replyToId) }
        .getOrElse {
            sendError("INTERNAL_ERROR", envelope.clientMessageId)
            return
        }
    val saved = when (result) {
        is SaveMessageResult.Success -> result
        SaveMessageResult.RecipientNotFound -> return sendError("USER_NOT_FOUND", envelope.clientMessageId)
        SaveMessageResult.SongNotFound -> return sendError("SONG_NOT_FOUND", envelope.clientMessageId)
        SaveMessageResult.ReplyNotFound -> return sendError("REPLY_NOT_FOUND", envelope.clientMessageId)
        SaveMessageResult.IdempotencyConflict -> return sendError("MESSAGE_ID_CONFLICT", envelope.clientMessageId)
    }
    val message = saved.message
    chatConnections.sendTo(senderId, ChatSocketEnvelope(type = ChatSocketType.MESSAGE_SENT, message = message))
    if (!saved.isNew) return
    val recipientSessions = chatConnections.sendTo(recipientId, ChatSocketEnvelope(type = ChatSocketType.MESSAGE_RECEIVED, message = message))
    if (recipientSessions > 0) {
        repository.markDelivered(UUID.fromString(message.id), recipientId)?.let { delivered ->
            val receipt = ChatSocketEnvelope(type = ChatSocketType.MESSAGE_DELIVERED, messageId = delivered.id, message = delivered)
            chatConnections.sendTo(senderId, receipt)
            chatConnections.sendTo(recipientId, receipt)
        }
    }
}

private enum class MessageMutation { Edit, Delete, AddReaction, RemoveReaction }

private suspend fun io.ktor.server.websocket.DefaultWebSocketServerSession.handleMessageMutation(
    userId: UUID,
    envelope: ChatSocketEnvelope,
    repository: ChatRepository,
    mutation: MessageMutation,
) {
    val messageId = envelope.messageId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        ?: return sendError("VALIDATION_ERROR", requestType = envelope.type, messageId = envelope.messageId)
    val updated = when (mutation) {
        MessageMutation.Edit -> {
            val content = envelope.content?.trim().orEmpty()
            if (content.isBlank() || content.length > MaxMessageLength) return sendError("VALIDATION_ERROR", requestType = envelope.type, messageId = envelope.messageId)
            repository.editMessage(userId, messageId, content)
        }
        MessageMutation.Delete -> repository.deleteMessage(userId, messageId)
        MessageMutation.AddReaction, MessageMutation.RemoveReaction -> {
            val reaction = envelope.reaction?.trim().orEmpty()
            if (reaction.isBlank() || reaction.length > 16) return sendError("VALIDATION_ERROR", requestType = envelope.type, messageId = envelope.messageId)
            repository.setReaction(userId, messageId, reaction, mutation == MessageMutation.AddReaction)
        }
    } ?: return sendError("MESSAGE_NOT_FOUND", requestType = envelope.type, messageId = envelope.messageId)
    val messageIdValue = UUID.fromString(updated.id)
    val senderId = UUID.fromString(updated.senderId)
    val recipientId = UUID.fromString(updated.recipientId)
    repository.messageForViewer(messageIdValue, senderId)?.let { senderView ->
        chatConnections.sendTo(senderId, ChatSocketEnvelope(type = ChatSocketType.MESSAGE_UPDATED, messageId = senderView.id, message = senderView))
    }
    repository.messageForViewer(messageIdValue, recipientId)?.let { recipientView ->
        chatConnections.sendTo(recipientId, ChatSocketEnvelope(type = ChatSocketType.MESSAGE_UPDATED, messageId = recipientView.id, message = recipientView))
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
    val event = ChatSocketEnvelope(type = if (read) ChatSocketType.MESSAGE_READ else ChatSocketType.MESSAGE_DELIVERED, messageId = updated.id, message = updated)
    chatConnections.sendTo(recipientId, event)
    chatConnections.sendTo(UUID.fromString(updated.senderId), event)
}

private suspend fun io.ktor.server.websocket.DefaultWebSocketServerSession.sendEnvelope(envelope: ChatSocketEnvelope) {
    send(Frame.Text(chatJson.encodeToString(envelope)))
}

private suspend fun io.ktor.server.websocket.DefaultWebSocketServerSession.sendError(
    code: String,
    clientMessageId: String? = null,
    requestType: ChatSocketType? = null,
    messageId: String? = null,
) {
    sendEnvelope(
        ChatSocketEnvelope(
            type = ChatSocketType.ERROR,
            requestType = requestType,
            error = code,
            clientMessageId = clientMessageId,
            messageId = messageId,
        ),
    )
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

private fun io.ktor.server.application.ApplicationCall.pageSize(default: Int): Int {
    val size = request.queryParameters["size"]?.toIntOrNull() ?: default
    if (size !in 1..100) throw validation("size must be between 1 and 100")
    return size
}

private fun io.ktor.server.application.ApplicationCall.optionalCursor(): ChatCursor? {
    val raw = request.queryParameters["cursor"]?.takeIf(String::isNotBlank) ?: return null
    return ChatCursor.decode(raw) ?: throw validation("cursor is invalid")
}

private fun pages(total: Long, size: Int) = if (total == 0L) 0 else ((total - 1) / size + 1).toInt()
private fun validation(message: String) = ApiException(HttpStatusCode.BadRequest, ErrorCode.VALIDATION_ERROR, message)
private const val MaxMessageLength = 4_000
