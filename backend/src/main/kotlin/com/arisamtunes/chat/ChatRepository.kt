package com.arisamtunes.chat

import com.arisamtunes.plugins.DatabaseProvider
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository {
    data class ConversationPeer(val peerId: UUID, val latestMessage: ChatMessageResponse, val unreadCount: Long)

    suspend fun conversationPeers(userId: UUID, cursor: ChatCursor?, size: Int): CursorPage<ConversationPeer> =
        withContext(Dispatchers.IO) { DatabaseProvider.dataSource.connection.use { c ->
        val cursorCondition = if (cursor == null) "" else "WHERE (created_at, id) < (?, ?)"
        val items = c.prepareStatement(
            """
            WITH ranked AS (
                SELECT
                    CASE WHEN sender_id = ? THEN recipient_id ELSE sender_id END AS peer_id,
                    chat_messages.*,
                    ROW_NUMBER() OVER (
                        PARTITION BY CASE WHEN sender_id = ? THEN recipient_id ELSE sender_id END
                        ORDER BY created_at DESC, id DESC
                    ) AS row_number
                FROM chat_messages
                WHERE sender_id = ? OR recipient_id = ?
            ), latest AS (
                SELECT * FROM ranked WHERE row_number = 1
            )
            SELECT latest.*,
                (SELECT COUNT(*) FROM chat_messages unread
                 WHERE unread.sender_id = latest.peer_id AND unread.recipient_id = ? AND unread.status <> 'READ') AS unread_count
            FROM latest
            $cursorCondition
            ORDER BY created_at DESC, id DESC
            LIMIT ?
            """.trimIndent(),
        ).use { s ->
            s.setObject(1, userId)
            s.setObject(2, userId)
            s.setObject(3, userId)
            s.setObject(4, userId)
            s.setObject(5, userId)
            var index = 6
            if (cursor != null) {
                s.setTimestamp(index++, Timestamp.from(cursor.timestamp))
                s.setObject(index++, cursor.id)
            }
            s.setInt(index, size + 1)
            s.executeQuery().use { results ->
                buildList {
                    while (results.next()) add(ConversationPeer(results.getObject("peer_id", UUID::class.java), results.toChatMessage(), results.getLong("unread_count")))
                }
            }
        }
        c.commit()
        items.toCursorPage(size) { peer -> ChatCursor(Instant.parse(peer.latestMessage.createdAt), UUID.fromString(peer.latestMessage.id)) }
    } }

    suspend fun conversation(userId: UUID, peerId: UUID, page: Int, size: Int, since: Instant?): Pair<List<ChatMessageResponse>, Long> =
        withContext(Dispatchers.IO) { DatabaseProvider.dataSource.connection.use { c ->
            val total = c.prepareStatement(
                """
                SELECT COUNT(*)
                FROM chat_messages
                WHERE ((sender_id = ? AND recipient_id = ?) OR (sender_id = ? AND recipient_id = ?))
                  AND (?::timestamptz IS NULL OR created_at > ?::timestamptz)
                """.trimIndent(),
            ).use { s ->
                s.setObject(1, userId)
                s.setObject(2, peerId)
                s.setObject(3, peerId)
                s.setObject(4, userId)
                s.setNullableTimestamp(5, since)
                s.setNullableTimestamp(6, since)
                s.executeQuery().use { it.next(); it.getLong(1) }
            }
            val order = if (since == null) "DESC" else "ASC"
            val items = c.prepareStatement(
                """
                SELECT *
                FROM chat_messages
                WHERE ((sender_id = ? AND recipient_id = ?) OR (sender_id = ? AND recipient_id = ?))
                  AND (?::timestamptz IS NULL OR created_at > ?::timestamptz)
                ORDER BY created_at $order
                LIMIT ? OFFSET ?
                """.trimIndent(),
            ).use { s ->
                s.setObject(1, userId)
                s.setObject(2, peerId)
                s.setObject(3, peerId)
                s.setObject(4, userId)
                s.setNullableTimestamp(5, since)
                s.setNullableTimestamp(6, since)
                s.setInt(7, size)
                s.setLong(8, page.toLong() * size)
                s.executeQuery().use { results -> buildList { while (results.next()) add(results.toChatMessage()) } }
            }
            c.commit()
            c.attachReactions(items, userId) to total
        } }

    suspend fun conversationBefore(userId: UUID, peerId: UUID, cursor: ChatCursor?, size: Int): CursorPage<ChatMessageResponse> =
        withContext(Dispatchers.IO) { DatabaseProvider.dataSource.connection.use { c ->
            val cursorCondition = if (cursor == null) "" else "AND (created_at, id) < (?, ?)"
            val items = c.prepareStatement(
                """
                SELECT *
                FROM chat_messages
                WHERE ((sender_id = ? AND recipient_id = ?) OR (sender_id = ? AND recipient_id = ?))
                $cursorCondition
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """.trimIndent(),
            ).use { s ->
                s.setObject(1, userId)
                s.setObject(2, peerId)
                s.setObject(3, peerId)
                s.setObject(4, userId)
                var index = 5
                if (cursor != null) {
                    s.setTimestamp(index++, Timestamp.from(cursor.timestamp))
                    s.setObject(index++, cursor.id)
                }
                s.setInt(index, size + 1)
                s.executeQuery().use { results -> buildList { while (results.next()) add(results.toChatMessage()) } }
            }
            c.commit()
            val page = items.toCursorPage(size) { message -> ChatCursor(Instant.parse(message.createdAt), UUID.fromString(message.id)) }
            page.copy(items = c.attachReactions(page.items, userId))
        } }

    suspend fun syncChanges(userId: UUID, cursor: ChatCursor?, size: Int): CursorPage<ChatMessageResponse> =
        withContext(Dispatchers.IO) { DatabaseProvider.dataSource.connection.use { c ->
            val cursorCondition = if (cursor == null) "" else "AND (updated_at, id) > (?, ?)"
            val items = c.prepareStatement(
                """
                SELECT *
                FROM chat_messages
                WHERE (sender_id = ? OR recipient_id = ?)
                $cursorCondition
                ORDER BY updated_at ASC, id ASC
                LIMIT ?
                """.trimIndent(),
            ).use { s ->
                s.setObject(1, userId)
                s.setObject(2, userId)
                var index = 3
                if (cursor != null) {
                    s.setTimestamp(index++, Timestamp.from(cursor.timestamp))
                    s.setObject(index++, cursor.id)
                }
                s.setInt(index, size + 1)
                s.executeQuery().use { results -> buildList { while (results.next()) add(results.toChatMessage()) } }
            }
            c.commit()
            val page = items.toCursorPage(size, includeTerminalCursor = true) { message ->
                ChatCursor(Instant.parse(message.updatedAt), UUID.fromString(message.id))
            }
            page.copy(items = c.attachReactions(page.items, userId))
        } }

    suspend fun saveMessage(
        senderId: UUID,
        clientMessageId: UUID,
        recipientId: UUID,
        messageType: ChatMessageType,
        content: String?,
        songId: UUID?,
        replyToId: UUID?,
    ): SaveMessageResult = withContext(Dispatchers.IO) { DatabaseProvider.dataSource.connection.use { c ->
        try {
            if (!c.exists("users", recipientId)) {
                c.rollback()
                return@use SaveMessageResult.RecipientNotFound
            }
            if (messageType == ChatMessageType.SONG && (songId == null || !c.exists("songs", songId))) {
                c.rollback()
                return@use SaveMessageResult.SongNotFound
            }
            if (replyToId != null && !c.isConversationMessage(replyToId, senderId, recipientId)) {
                c.rollback()
                return@use SaveMessageResult.ReplyNotFound
            }
            val inserted = c.prepareStatement(
                """
                INSERT INTO chat_messages(client_message_id, sender_id, recipient_id, message_type, content, song_id, reply_to_id, status)
                VALUES (?, ?, ?, ?::chat_message_type, ?, ?, ?, 'SENT')
                ON CONFLICT (sender_id, client_message_id) DO NOTHING
                RETURNING *
                """.trimIndent(),
            ).use { s ->
                s.setObject(1, clientMessageId)
                s.setObject(2, senderId)
                s.setObject(3, recipientId)
                s.setString(4, messageType.name)
                s.setString(5, content)
                s.setObject(6, songId)
                s.setObject(7, replyToId)
                s.executeQuery().use { results ->
                    if (results.next()) results.toChatMessage() else null
                }
            }
            if (inserted != null) {
                c.commit()
                return@use SaveMessageResult.Success(inserted, isNew = true)
            }
            val existing = c.prepareStatement(
                "SELECT * FROM chat_messages WHERE sender_id = ? AND client_message_id = ?",
            ).use { s ->
                s.setObject(1, senderId)
                s.setObject(2, clientMessageId)
                s.executeQuery().use { results -> if (results.next()) results.toChatMessage() else null }
            } ?: error("Conflicting chat message could not be loaded")
            val samePayload = existing.recipientId == recipientId.toString() &&
                existing.messageType == messageType && existing.content == content && existing.songId == songId?.toString() &&
                existing.replyToId == replyToId?.toString()
            c.commit()
            if (samePayload) SaveMessageResult.Success(existing, isNew = false) else SaveMessageResult.IdempotencyConflict
        } catch (error: Throwable) {
            c.rollback()
            throw error
        }
    } }

    suspend fun searchMessages(userId: UUID, peerId: UUID, query: String, size: Int): List<ChatMessageResponse> =
        withContext(Dispatchers.IO) { DatabaseProvider.dataSource.connection.use { c ->
            val items = c.prepareStatement(
                """
                SELECT * FROM chat_messages
                WHERE ((sender_id = ? AND recipient_id = ?) OR (sender_id = ? AND recipient_id = ?))
                  AND deleted_at IS NULL AND content ILIKE ?
                ORDER BY created_at DESC LIMIT ?
                """.trimIndent(),
            ).use { s ->
                s.setObject(1, userId); s.setObject(2, peerId)
                s.setObject(3, peerId); s.setObject(4, userId)
                s.setString(5, "%${query.replace("%", "\\%").replace("_", "\\_")}%")
                s.setInt(6, size)
                s.executeQuery().use { results -> buildList { while (results.next()) add(results.toChatMessage()) } }
            }
            c.commit()
            c.attachReactions(items, userId)
        } }

    suspend fun editMessage(senderId: UUID, messageId: UUID, content: String): ChatMessageResponse? =
        mutateOwnedMessage(senderId, messageId, """
            UPDATE chat_messages SET content = ?, edited_at = NOW(), updated_at = NOW()
            WHERE id = ? AND sender_id = ? AND message_type = 'TEXT' AND deleted_at IS NULL
            RETURNING *
        """.trimIndent(), content)

    suspend fun deleteMessage(senderId: UUID, messageId: UUID): ChatMessageResponse? =
        mutateOwnedMessage(senderId, messageId, """
            UPDATE chat_messages SET content = NULL, song_id = NULL, deleted_at = NOW(), updated_at = NOW()
            WHERE id = ? AND sender_id = ? AND deleted_at IS NULL
            RETURNING *
        """.trimIndent(), null)

    private suspend fun mutateOwnedMessage(senderId: UUID, messageId: UUID, sql: String, content: String?): ChatMessageResponse? =
        withContext(Dispatchers.IO) { DatabaseProvider.dataSource.connection.use { c ->
            try {
                val message = c.prepareStatement(sql).use { s ->
                    var index = 1
                    if (content != null) s.setString(index++, content)
                    s.setObject(index++, messageId)
                    s.setObject(index, senderId)
                    s.executeQuery().use { results -> if (results.next()) results.toChatMessage() else null }
                }
                c.commit()
                message?.let { c.attachReactions(listOf(it), senderId).first() }
            } catch (error: Throwable) {
                c.rollback(); throw error
            }
        } }

    suspend fun setReaction(userId: UUID, messageId: UUID, reaction: String, add: Boolean): ChatMessageResponse? =
        withContext(Dispatchers.IO) { DatabaseProvider.dataSource.connection.use { c ->
            try {
                val message = c.prepareStatement(
                    "SELECT * FROM chat_messages WHERE id = ? AND (sender_id = ? OR recipient_id = ?) AND deleted_at IS NULL",
                ).use { s ->
                    s.setObject(1, messageId); s.setObject(2, userId); s.setObject(3, userId)
                    s.executeQuery().use { results -> if (results.next()) results.toChatMessage() else null }
                } ?: return@use null.also { c.rollback() }
                if (add) {
                    c.prepareStatement("INSERT INTO chat_message_reactions(message_id, user_id, reaction) VALUES (?, ?, ?) ON CONFLICT DO NOTHING").use { s ->
                        s.setObject(1, messageId); s.setObject(2, userId); s.setString(3, reaction); s.executeUpdate()
                    }
                } else {
                    c.prepareStatement("DELETE FROM chat_message_reactions WHERE message_id = ? AND user_id = ? AND reaction = ?").use { s ->
                        s.setObject(1, messageId); s.setObject(2, userId); s.setString(3, reaction); s.executeUpdate()
                    }
                }
                c.prepareStatement("UPDATE chat_messages SET updated_at = NOW() WHERE id = ?").use { s ->
                    s.setObject(1, messageId); s.executeUpdate()
                }
                c.commit()
                val refreshed = c.prepareStatement("SELECT * FROM chat_messages WHERE id = ?").use { s ->
                    s.setObject(1, messageId)
                    s.executeQuery().use { results -> results.next(); results.toChatMessage() }
                }
                c.attachReactions(listOf(refreshed), userId).first()
            } catch (error: Throwable) {
                c.rollback(); throw error
            }
        } }

    suspend fun messageForViewer(messageId: UUID, viewerId: UUID): ChatMessageResponse? =
        withContext(Dispatchers.IO) { DatabaseProvider.dataSource.connection.use { c ->
            val message = c.prepareStatement(
                "SELECT * FROM chat_messages WHERE id = ? AND (sender_id = ? OR recipient_id = ?)",
            ).use { s ->
                s.setObject(1, messageId)
                s.setObject(2, viewerId)
                s.setObject(3, viewerId)
                s.executeQuery().use { results -> if (results.next()) results.toChatMessage() else null }
            }
            c.commit()
            message?.let { c.attachReactions(listOf(it), viewerId).first() }
        } }

    suspend fun markDelivered(messageId: UUID, recipientId: UUID): ChatMessageResponse? =
        updateReceipt(
            sql = """
                UPDATE chat_messages
                SET status = CASE WHEN status = 'SENT' THEN 'DELIVERED'::chat_message_status ELSE status END,
                    delivered_at = COALESCE(delivered_at, NOW()),
                    updated_at = NOW()
                WHERE id = ? AND recipient_id = ? AND status = 'SENT'
                RETURNING *
            """.trimIndent(),
            messageId = messageId,
            recipientId = recipientId,
        )

    suspend fun markRead(messageId: UUID, recipientId: UUID): ChatMessageResponse? =
        updateReceipt(
            sql = """
                UPDATE chat_messages
                SET status = 'READ',
                    delivered_at = COALESCE(delivered_at, NOW()),
                    read_at = COALESCE(read_at, NOW()),
                    updated_at = NOW()
                WHERE id = ? AND recipient_id = ? AND status <> 'READ'
                RETURNING *
            """.trimIndent(),
            messageId = messageId,
            recipientId = recipientId,
        )

    private suspend fun updateReceipt(sql: String, messageId: UUID, recipientId: UUID): ChatMessageResponse? =
        withContext(Dispatchers.IO) { DatabaseProvider.dataSource.connection.use { c ->
            try {
                val updated = c.prepareStatement(sql).use { s ->
                    s.setObject(1, messageId)
                    s.setObject(2, recipientId)
                    s.executeQuery().use { results -> if (results.next()) results.toChatMessage() else null }
                }
                val message = updated ?: c.prepareStatement(
                    "SELECT * FROM chat_messages WHERE id = ? AND recipient_id = ?",
                ).use { s ->
                    s.setObject(1, messageId)
                    s.setObject(2, recipientId)
                    s.executeQuery().use { results -> if (results.next()) results.toChatMessage() else null }
                }
                c.commit()
                message
            } catch (error: Throwable) {
                c.rollback()
                throw error
            }
        } }

    suspend fun markPendingDelivered(recipientId: UUID): List<ChatMessageResponse> =
        withContext(Dispatchers.IO) { DatabaseProvider.dataSource.connection.use { c ->
            try {
                val messages = c.prepareStatement(
                    """
                    UPDATE chat_messages
                    SET status = 'DELIVERED', delivered_at = COALESCE(delivered_at, NOW()), updated_at = NOW()
                    WHERE recipient_id = ? AND status = 'SENT'
                    RETURNING *
                    """.trimIndent(),
                ).use { s ->
                    s.setObject(1, recipientId)
                    s.executeQuery().use { results -> buildList { while (results.next()) add(results.toChatMessage()) } }
                }
                c.commit()
                messages
            } catch (error: Throwable) {
                c.rollback()
                throw error
            }
        } }
}

sealed interface SaveMessageResult {
    data class Success(val message: ChatMessageResponse, val isNew: Boolean) : SaveMessageResult
    data object RecipientNotFound : SaveMessageResult
    data object SongNotFound : SaveMessageResult
    data object ReplyNotFound : SaveMessageResult
    data object IdempotencyConflict : SaveMessageResult
}

private fun Connection.isConversationMessage(messageId: UUID, firstUserId: UUID, secondUserId: UUID): Boolean =
    prepareStatement(
        "SELECT EXISTS(SELECT 1 FROM chat_messages WHERE id = ? AND ((sender_id = ? AND recipient_id = ?) OR (sender_id = ? AND recipient_id = ?)))",
    ).use { s ->
        s.setObject(1, messageId); s.setObject(2, firstUserId); s.setObject(3, secondUserId)
        s.setObject(4, secondUserId); s.setObject(5, firstUserId)
        s.executeQuery().use { it.next(); it.getBoolean(1) }
    }

private fun Connection.attachReactions(messages: List<ChatMessageResponse>, viewerId: UUID): List<ChatMessageResponse> {
    if (messages.isEmpty()) return messages
    val ids = messages.map { UUID.fromString(it.id) }
    val reactions = prepareStatement(
        """
        SELECT message_id, reaction, COUNT(*) AS reaction_count, BOOL_OR(user_id = ?) AS reacted_by_me
        FROM chat_message_reactions WHERE message_id = ANY (?)
        GROUP BY message_id, reaction ORDER BY MIN(created_at)
        """.trimIndent(),
    ).use { s ->
        s.setObject(1, viewerId)
        s.setArray(2, createArrayOf("uuid", ids.toTypedArray()))
        s.executeQuery().use { results ->
            buildMap<UUID, MutableList<ChatReactionResponse>> {
                while (results.next()) {
                    val id = results.getObject("message_id", UUID::class.java)
                    getOrPut(id) { mutableListOf() }.add(
                        ChatReactionResponse(results.getString("reaction"), results.getInt("reaction_count"), results.getBoolean("reacted_by_me")),
                    )
                }
            }
        }
    }
    return messages.map { it.copy(reactions = reactions[UUID.fromString(it.id)].orEmpty()) }
}

private fun Connection.exists(table: String, id: UUID): Boolean {
    require(table == "users" || table == "songs")
    return prepareStatement("SELECT EXISTS(SELECT 1 FROM $table WHERE id = ?)").use { statement ->
        statement.setObject(1, id)
        statement.executeQuery().use { results -> results.next(); results.getBoolean(1) }
    }
}

private fun java.sql.PreparedStatement.setNullableTimestamp(index: Int, value: Instant?) {
    if (value == null) setNull(index, java.sql.Types.TIMESTAMP_WITH_TIMEZONE) else setTimestamp(index, Timestamp.from(value))
}

private fun ResultSet.toChatMessage() = ChatMessageResponse(
    id = getObject("id", UUID::class.java).toString(),
    clientMessageId = getObject("client_message_id", UUID::class.java).toString(),
    senderId = getObject("sender_id", UUID::class.java).toString(),
    recipientId = getObject("recipient_id", UUID::class.java).toString(),
    messageType = ChatMessageType.valueOf(getString("message_type")),
    content = getString("content"),
    songId = getObject("song_id", UUID::class.java)?.toString(),
    replyToId = getObject("reply_to_id", UUID::class.java)?.toString(),
    status = ChatMessageStatus.valueOf(getString("status")),
    createdAt = getTimestamp("created_at").toInstant().toString(),
    deliveredAt = getNullableTimestamp("delivered_at")?.toInstant()?.toString(),
    readAt = getNullableTimestamp("read_at")?.toInstant()?.toString(),
    editedAt = getNullableTimestamp("edited_at")?.toInstant()?.toString(),
    deletedAt = getNullableTimestamp("deleted_at")?.toInstant()?.toString(),
    updatedAt = getTimestamp("updated_at").toInstant().toString(),
)

private fun ResultSet.getNullableTimestamp(column: String): Timestamp? = getTimestamp(column).also { if (wasNull()) return null }
