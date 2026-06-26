package com.arisamtunes.chat

import com.arisamtunes.plugins.DatabaseProvider
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

class ChatRepository {
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
