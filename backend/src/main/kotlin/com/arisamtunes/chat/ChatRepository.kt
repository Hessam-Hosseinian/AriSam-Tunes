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
                s.setObject(3, peerId)
                s.setObject(4, userId)
                s.setNullableTimestamp(5, since)
                s.setNullableTimestamp(6, since)
                s.setInt(7, size)
                s.setLong(8, page.toLong() * size)
                s.executeQuery().use { results -> buildList { while (results.next()) add(results.toChatMessage()) } }
            }
            c.commit()
            items to total
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
            items.toCursorPage(size) { message -> ChatCursor(Instant.parse(message.createdAt), UUID.fromString(message.id)) }
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
            items.toCursorPage(size, includeTerminalCursor = true) { message ->
                ChatCursor(Instant.parse(message.updatedAt), UUID.fromString(message.id))
            }
        } }

    suspend fun saveMessage(
        senderId: UUID,
        clientMessageId: UUID,
        recipientId: UUID,
        messageType: ChatMessageType,
        content: String?,
        songId: UUID?,
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
            val inserted = c.prepareStatement(
                """
                INSERT INTO chat_messages(client_message_id, sender_id, recipient_id, message_type, content, song_id, status)
                VALUES (?, ?, ?, ?::chat_message_type, ?, ?, 'SENT')
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
                existing.messageType == messageType && existing.content == content && existing.songId == songId?.toString()
            c.commit()
            if (samePayload) SaveMessageResult.Success(existing, isNew = false) else SaveMessageResult.IdempotencyConflict
        } catch (error: Throwable) {
            c.rollback()
            throw error
        }
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
    data object IdempotencyConflict : SaveMessageResult
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
    status = ChatMessageStatus.valueOf(getString("status")),
    createdAt = getTimestamp("created_at").toInstant().toString(),
    deliveredAt = getNullableTimestamp("delivered_at")?.toInstant()?.toString(),
    readAt = getNullableTimestamp("read_at")?.toInstant()?.toString(),
    updatedAt = getTimestamp("updated_at").toInstant().toString(),
)

private fun ResultSet.getNullableTimestamp(column: String): Timestamp? = getTimestamp(column).also { if (wasNull()) return null }
