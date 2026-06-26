package com.arisamtunes.chat

import com.arisamtunes.plugins.DatabaseProvider
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class ChatRepository {
    fun conversation(userId: UUID, peerId: UUID, page: Int, size: Int, since: Instant?): Pair<List<ChatMessageResponse>, Long> =
        DatabaseProvider.dataSource.connection.use { c ->
            val total = c.prepareStatement(
                """
                SELECT COUNT(*)
                FROM chat_messages
                WHERE ((sender_id = ? AND recipient_id = ?) OR (sender_id = ? AND recipient_id = ?))
                  AND (? IS NULL OR created_at > ?)
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
                  AND (? IS NULL OR created_at > ?)
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
            items to total
        }

    fun saveMessage(
        senderId: UUID,
        clientMessageId: UUID,
        recipientId: UUID,
        messageType: ChatMessageType,
        content: String?,
        songId: UUID?,
    ): ChatMessageResponse = DatabaseProvider.dataSource.connection.use { c ->
        try {
            val message = c.prepareStatement(
                """
                INSERT INTO chat_messages(client_message_id, sender_id, recipient_id, message_type, content, song_id, status)
                VALUES (?, ?, ?, ?::chat_message_type, ?, ?, 'SENT')
                ON CONFLICT (client_message_id) DO UPDATE
                    SET client_message_id = EXCLUDED.client_message_id
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
                    results.next()
                    results.toChatMessage()
                }
            }
            c.commit()
            message
        } catch (error: Throwable) {
            c.rollback()
            throw error
        }
    }

    fun markDelivered(messageId: UUID, recipientId: UUID): ChatMessageResponse? =
        updateReceipt(
            sql = """
                UPDATE chat_messages
                SET status = CASE WHEN status = 'SENT' THEN 'DELIVERED'::chat_message_status ELSE status END,
                    delivered_at = COALESCE(delivered_at, NOW())
                WHERE id = ? AND recipient_id = ?
                RETURNING *
            """.trimIndent(),
            messageId = messageId,
            recipientId = recipientId,
        )

    fun markRead(messageId: UUID, recipientId: UUID): ChatMessageResponse? =
        updateReceipt(
            sql = """
                UPDATE chat_messages
                SET status = 'READ',
                    delivered_at = COALESCE(delivered_at, NOW()),
                    read_at = COALESCE(read_at, NOW())
                WHERE id = ? AND recipient_id = ?
                RETURNING *
            """.trimIndent(),
            messageId = messageId,
            recipientId = recipientId,
        )

    private fun updateReceipt(sql: String, messageId: UUID, recipientId: UUID): ChatMessageResponse? =
        DatabaseProvider.dataSource.connection.use { c ->
            try {
                val message = c.prepareStatement(sql).use { s ->
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
)

private fun ResultSet.getNullableTimestamp(column: String): Timestamp? = getTimestamp(column).also { if (wasNull()) return null }
