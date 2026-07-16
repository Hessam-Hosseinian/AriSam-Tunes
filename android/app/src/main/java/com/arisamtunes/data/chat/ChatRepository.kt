package com.arisamtunes.data.chat

import android.util.Base64
import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.map
import androidx.room.withTransaction
import com.arisamtunes.BuildConfig
import com.arisamtunes.data.auth.AuthTokenStore
import com.arisamtunes.data.auth.UserDto
import com.arisamtunes.data.local.AriSamDatabase
import com.arisamtunes.data.local.dao.ChatMessageDao
import com.arisamtunes.data.local.dao.CachedUserProfileDao
import com.arisamtunes.data.local.entity.ChatMessageEntity
import com.arisamtunes.data.local.entity.ChatRemoteKeyEntity
import com.arisamtunes.data.local.entity.CachedUserProfileEntity
import com.arisamtunes.data.social.PublicUserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@Singleton
class ChatRepository @Inject constructor(
    private val client: HttpClient,
    private val tokenStore: AuthTokenStore,
    private val database: AriSamDatabase,
    private val chatMessageDao: ChatMessageDao,
    private val cachedUserProfileDao: CachedUserProfileDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }
    private val _status = MutableStateFlow(ChatConnectionStatus.Disconnected)
    val status: StateFlow<ChatConnectionStatus> = _status
    private val _events = MutableSharedFlow<ChatSocketEnvelopeDto>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<ChatSocketEnvelopeDto> = _events
    private val readReceiptsInFlight = ConcurrentHashMap.newKeySet<String>()
    private val conversationSources = CopyOnWriteArraySet<ConversationsPagingSource>()
    private var connectionJob: Job? = null
    private var activeSession: DefaultClientWebSocketSession? = null
    private var cachedUserId: String? = null

    suspend fun currentUserId(): String =
        cachedUserId ?: tokenStore.load()?.accessToken?.subjectFromJwt()?.also { cachedUserId = it }
            ?: client.get("auth/me").body<UserDto>().id.also { cachedUserId = it }

    /** The logged-in application owns one socket; screens only observe its state. */
    @Synchronized
    fun startRealtime() {
        if (connectionJob?.isActive == true) return
        connectionJob = scope.launch { connectionLoop() }
    }

    @Synchronized
    fun retryRealtime() {
        connectionJob?.cancel()
        activeSession = null
        connectionJob = scope.launch { connectionLoop() }
    }

    fun stopRealtime() {
        val job = synchronized(this) {
            connectionJob.also { connectionJob = null }
        }
        job?.cancel()
        activeSession = null
        cachedUserId = null
        readReceiptsInFlight.clear()
        _status.value = ChatConnectionStatus.Disconnected
    }

    fun conversationsPager(): Flow<PagingData<ChatConversationDto>> = Pager(
        config = PagingConfig(pageSize = 30, initialLoadSize = 30, prefetchDistance = 8),
        pagingSourceFactory = {
            ConversationsPagingSource(this).also { source ->
                conversationSources += source
                source.registerInvalidatedCallback { conversationSources -= source }
            }
        },
    ).flow

    suspend fun conversations(cursor: String? = null, size: Int = 30): ChatConversationListDto {
        val response = client.get("chat/conversations") {
            cursor?.let { parameter("cursor", it) }
            parameter("size", size)
        }.body<ChatConversationListDto>()
        val ownerId = currentUserId()
        database.withTransaction {
            response.items.forEach { chatMessageDao.deletePending(ownerId, it.latestMessage.clientMessageId) }
            chatMessageDao.upsertAll(response.items.map { it.latestMessage.toEntity(ownerId) })
            cachedUserProfileDao.upsertAll(response.items.map { it.user.toCacheEntity() })
        }
        return response
    }

    suspend fun localConversations(offset: Int, size: Int): List<ChatConversationDto> {
        val ownerId = currentUserId()
        val rows = chatMessageDao.conversationPage(ownerId, size, offset)
        val profiles = cachedUserProfileDao.profiles(rows.map { it.latestMessage.conversationUserId }).associateBy { it.userId }
        return rows.mapNotNull { row ->
            profiles[row.latestMessage.conversationUserId]?.let { profile ->
                ChatConversationDto(profile.toPublicUser(), row.latestMessage.toDto(), row.unreadCount)
            }
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    fun messagesPager(userId: String): Flow<PagingData<ChatMessageDto>> = flow {
        val ownerId = currentUserId()
        emitAll(
            Pager(
                config = PagingConfig(pageSize = 40, initialLoadSize = 40, prefetchDistance = 10),
                remoteMediator = ChatHistoryRemoteMediator(ownerId, userId, this@ChatRepository, database, chatMessageDao),
                pagingSourceFactory = { chatMessageDao.pagingSource(ownerId, userId) },
            ).flow.map { data -> data.map(ChatMessageEntity::toDto) },
        )
    }

    suspend fun historyPage(userId: String, cursor: String?, size: Int): ChatMessageListDto =
        client.get("chat/$userId/messages") {
            parameter("cursor", cursor.orEmpty())
            parameter("size", size)
        }.body()

    suspend fun sendText(recipientId: String, text: String) = enqueueMessage(recipientId, "TEXT", text, null)

    suspend fun sendSong(recipientId: String, songId: String) = enqueueMessage(recipientId, "SONG", null, songId)

    suspend fun retryMessage(clientMessageId: String) {
        val ownerId = currentUserId()
        val message = chatMessageDao.messageByClientId(ownerId, clientMessageId) ?: return
        chatMessageDao.markPending(ownerId, clientMessageId, Instant.now().toString())
        sendPendingMessage(message)
    }

    suspend fun setTyping(recipientId: String, typing: Boolean) {
        sendEnvelope(ChatSocketEnvelopeDto(type = if (typing) ChatSocketTypeDto.TYPING_START else ChatSocketTypeDto.TYPING_STOP, recipientId = recipientId))
    }

    fun clearTyping(recipientId: String) {
        scope.launch { setTyping(recipientId, false) }
    }

    suspend fun markMessageRead(messageId: String) {
        val ownerId = currentUserId()
        val now = Instant.now().toString()
        chatMessageDao.markReadPending(ownerId, messageId, now)
        if (!readReceiptsInFlight.add(messageId)) return
        if (!sendEnvelope(ChatSocketEnvelopeDto(type = ChatSocketTypeDto.MESSAGE_READ, messageId = messageId))) {
            readReceiptsInFlight.remove(messageId)
        }
    }

    private suspend fun enqueueMessage(recipientId: String, type: String, body: String?, songId: String?) {
        val ownerId = currentUserId()
        val clientId = UUID.randomUUID().toString()
        val now = Instant.now().toString()
        val pending = ChatMessageEntity(
            ownerUserId = ownerId,
            messageId = clientId,
            clientMessageId = clientId,
            conversationUserId = recipientId,
            senderUserId = ownerId,
            receiverUserId = recipientId,
            body = body.orEmpty(),
            messageType = type,
            songId = songId,
            deliveryState = "PENDING",
            isMine = true,
            createdAt = now,
            updatedAt = now,
            cachedAt = System.currentTimeMillis(),
        )
        chatMessageDao.upsert(pending)
        sendPendingMessage(pending)
    }

    private suspend fun connectionLoop() {
        var attempt = 0
        while (scope.isActive && connectionJob?.isActive == true) {
            val token = tokenStore.load()?.accessToken
            if (token == null) {
                _status.value = ChatConnectionStatus.Disconnected
                delay(2_000)
                continue
            }
            _status.value = if (attempt == 0) ChatConnectionStatus.Connecting else ChatConnectionStatus.Reconnecting
            runCatching {
                client.webSocket(request = {
                    url(chatSocketUrl())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }) {
                    activeSession = this
                    attempt = 0
                    _status.value = ChatConnectionStatus.Connected
                    runPostConnectStep("sync") { synchronizeChanges() }
                    runPostConnectStep("outbox") { flushPendingMessages() }
                    runPostConnectStep("receipts") { flushPendingReadReceipts() }
                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        runCatching { json.decodeFromString<ChatSocketEnvelopeDto>(frame.readText()) }
                            .onSuccess { event ->
                                persistSocketEvent(event)
                                _events.emit(event)
                            }
                    }
                }
            }.onFailure { if (it is CancellationException) throw it }
            activeSession = null
            if (connectionJob?.isActive != true) break
            attempt++
            _status.value = ChatConnectionStatus.Reconnecting
            delay(backoffDelayMillis(attempt))
        }
    }

    private suspend fun runPostConnectStep(name: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            // Sync is supplementary. A REST/cache failure must never tear down
            // an authenticated, healthy real-time socket.
            Log.w(LogTag, "Chat $name step failed; keeping WebSocket connected", error)
        }
    }

    private suspend fun synchronizeChanges() {
        val ownerId = currentUserId()
        var cursor = chatMessageDao.remoteKey(ownerId, SyncKey)?.nextCursor
        var hasMore: Boolean
        do {
            val response = client.get("chat/sync") {
                parameter("cursor", cursor.orEmpty())
                parameter("size", SyncPageSize)
            }.body<ChatSyncDto>()
            val pendingReads = chatMessageDao.pendingReadReceipts(ownerId)
            database.withTransaction {
                chatMessageDao.upsertAll(response.items.map { it.toEntity(ownerId) })
                response.items.forEach { message -> chatMessageDao.deletePending(ownerId, message.clientMessageId) }
                pendingReads.forEach { pending ->
                    chatMessageDao.markReadPending(ownerId, pending.messageId, pending.readAt ?: pending.updatedAt)
                }
                chatMessageDao.upsertRemoteKey(
                    ChatRemoteKeyEntity(ownerId, SyncKey, response.nextCursor, !response.hasMore, System.currentTimeMillis()),
                )
            }
            cursor = response.nextCursor
            hasMore = response.hasMore
        } while (hasMore && cursor != null)
    }

    private suspend fun flushPendingMessages() {
        val ownerId = currentUserId()
        chatMessageDao.pendingMessages(ownerId).forEach { sendPendingMessage(it) }
    }

    private suspend fun flushPendingReadReceipts() {
        val ownerId = currentUserId()
        chatMessageDao.pendingReadReceipts(ownerId).forEach { message ->
            if (readReceiptsInFlight.add(message.messageId) &&
                !sendEnvelope(ChatSocketEnvelopeDto(type = ChatSocketTypeDto.MESSAGE_READ, messageId = message.messageId))
            ) {
                readReceiptsInFlight.remove(message.messageId)
            }
        }
    }

    private suspend fun sendPendingMessage(message: ChatMessageEntity): Boolean = sendEnvelope(
        ChatSocketEnvelopeDto(
            type = ChatSocketTypeDto.SEND_MESSAGE,
            clientMessageId = message.clientMessageId,
            recipientId = message.receiverUserId,
            messageType = ChatMessageTypeDto.valueOf(message.messageType),
            content = message.body.takeIf { message.messageType == "TEXT" },
            songId = message.songId.takeIf { message.messageType == "SONG" },
        ),
    )

    private suspend fun sendEnvelope(envelope: ChatSocketEnvelopeDto): Boolean = runCatching {
        val session = activeSession ?: return false
        session.send(Frame.Text(json.encodeToString(envelope)))
        true
    }.getOrDefault(false)

    private suspend fun persistSocketEvent(event: ChatSocketEnvelopeDto) {
        val ownerId = runCatching { currentUserId() }.getOrNull() ?: return
        if (event.type == ChatSocketTypeDto.ERROR && event.clientMessageId != null) {
            chatMessageDao.markFailed(ownerId, event.clientMessageId, Instant.now().toString())
        }
        event.message?.let { message ->
            database.withTransaction {
                chatMessageDao.deletePending(ownerId, message.clientMessageId)
                chatMessageDao.upsert(message.toEntity(ownerId))
            }
            if (message.status == ChatMessageStatusDto.READ) readReceiptsInFlight.remove(message.id)
            conversationSources.forEach { it.invalidate() }
        }
        if (event.message == null && event.messageId != null && event.type in ReceiptTypes) {
            val now = Instant.now().toString()
            val state = if (event.type == ChatSocketTypeDto.MESSAGE_READ) "READ" else "DELIVERED"
            chatMessageDao.updateReceipt(ownerId, event.messageId, state, null, null, now)
            if (state == "READ") readReceiptsInFlight.remove(event.messageId)
        }
    }

    private fun chatSocketUrl(): String {
        val base = BuildConfig.API_BASE_URL.trimEnd('/').replaceFirst("http://", "ws://").replaceFirst("https://", "wss://")
        return "$base/api/v1/ws/chat"
    }

    private fun backoffDelayMillis(attempt: Int): Long {
        val exponential = min(30_000L, 1_000L * (1L shl attempt.coerceIn(0, 5)))
        return exponential + Random.nextLong(0L, 450L)
    }

    private companion object {
        const val SyncKey = "__sync__"
        const val SyncPageSize = 100
        const val LogTag = "AriSamChat"
        val ReceiptTypes = setOf(ChatSocketTypeDto.MESSAGE_DELIVERED, ChatSocketTypeDto.MESSAGE_READ)
    }
}

private class ConversationsPagingSource(private val repository: ChatRepository) : PagingSource<String, ChatConversationDto>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, ChatConversationDto> {
        val size = params.loadSize.coerceAtMost(100)
        val localOffset = params.key?.takeIf { it.startsWith(LocalPrefix) }?.removePrefix(LocalPrefix)?.toIntOrNull()
        if (localOffset != null) return localPage(localOffset, size)
        return runCatching {
            val response = repository.conversations(params.key, size)
            LoadResult.Page(response.items, prevKey = null, nextKey = response.nextCursor.takeIf { response.hasMore })
        }.getOrElse { localPage(0, size) }
    }

    private suspend fun localPage(offset: Int, size: Int): LoadResult<String, ChatConversationDto> {
        val items = repository.localConversations(offset, size)
        return LoadResult.Page(items, prevKey = null, nextKey = if (items.size == size) "$LocalPrefix${offset + size}" else null)
    }

    override fun getRefreshKey(state: PagingState<String, ChatConversationDto>): String? = null

    private companion object { const val LocalPrefix = "local:" }
}

@OptIn(ExperimentalPagingApi::class)
private class ChatHistoryRemoteMediator(
    private val ownerId: String,
    private val peerId: String,
    private val repository: ChatRepository,
    private val database: AriSamDatabase,
    private val dao: ChatMessageDao,
) : RemoteMediator<Int, ChatMessageEntity>() {
    override suspend fun load(loadType: LoadType, state: PagingState<Int, ChatMessageEntity>): MediatorResult {
        if (loadType == LoadType.PREPEND) return MediatorResult.Success(endOfPaginationReached = true)
        val key = dao.remoteKey(ownerId, peerId)
        if (loadType == LoadType.APPEND && (key?.reachedEnd == true || key == null)) {
            return MediatorResult.Success(endOfPaginationReached = key?.reachedEnd == true)
        }
        val cursor = if (loadType == LoadType.REFRESH) null else key?.nextCursor
        return runCatching { repository.historyPage(peerId, cursor, state.config.pageSize.coerceAtMost(100)) }
            .fold(
                onSuccess = { response ->
                    database.withTransaction {
                        response.items.forEach { dao.deletePending(ownerId, it.clientMessageId) }
                        dao.upsertAll(response.items.map { it.toEntity(ownerId) })
                        dao.upsertRemoteKey(
                            ChatRemoteKeyEntity(ownerId, peerId, response.nextCursor, !response.hasMore, System.currentTimeMillis()),
                        )
                    }
                    MediatorResult.Success(endOfPaginationReached = !response.hasMore)
                },
                onFailure = MediatorResult::Error,
            )
    }
}

private fun ChatMessageDto.toEntity(ownerId: String): ChatMessageEntity {
    val mine = senderId == ownerId
    return ChatMessageEntity(
        ownerUserId = ownerId,
        messageId = id,
        clientMessageId = clientMessageId,
        conversationUserId = if (mine) recipientId else senderId,
        senderUserId = senderId,
        receiverUserId = recipientId,
        body = content.orEmpty(),
        messageType = messageType.name,
        songId = songId,
        deliveryState = status.name,
        isMine = mine,
        createdAt = createdAt,
        sentAt = createdAt,
        deliveredAt = deliveredAt,
        readAt = readAt,
        readReceiptPending = false,
        updatedAt = updatedAt,
        cachedAt = System.currentTimeMillis(),
    )
}

private fun ChatMessageEntity.toDto() = ChatMessageDto(
    id = messageId,
    clientMessageId = clientMessageId,
    senderId = senderUserId,
    recipientId = receiverUserId,
    messageType = runCatching { ChatMessageTypeDto.valueOf(messageType) }.getOrDefault(ChatMessageTypeDto.TEXT),
    content = body,
    songId = songId,
    status = runCatching { ChatMessageStatusDto.valueOf(deliveryState) }.getOrDefault(ChatMessageStatusDto.PENDING),
    createdAt = createdAt,
    deliveredAt = deliveredAt,
    readAt = readAt,
    updatedAt = updatedAt,
)

private fun PublicUserDto.toCacheEntity() = CachedUserProfileEntity(
    userId = id,
    displayName = displayName,
    avatarUrl = avatarUrl,
    bio = bio,
    followerCount = followersCount,
    followingCount = followingCount,
    isFollowing = isFollowing,
    cachedAt = System.currentTimeMillis(),
)

private fun CachedUserProfileEntity.toPublicUser() = PublicUserDto(
    id = userId,
    displayName = displayName,
    avatarUrl = avatarUrl,
    bio = bio,
    followersCount = followerCount,
    followingCount = followingCount,
    isFollowing = isFollowing,
)

private fun String.subjectFromJwt(): String? = runCatching {
    val payload = split('.').getOrNull(1) ?: return null
    val normalized = payload.padEnd(payload.length + (4 - payload.length % 4) % 4, '=')
    val decoded = Base64.decode(normalized, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING).decodeToString()
    Json.decodeFromString<JsonObject>(decoded)["sub"]?.jsonPrimitive?.content
}.getOrNull()
