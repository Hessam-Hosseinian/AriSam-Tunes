package com.arisamtunes.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Upsert
import com.arisamtunes.data.local.entity.ChatMessageEntity
import com.arisamtunes.data.local.entity.ChatRemoteKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    data class ConversationRow(
        @Embedded val latestMessage: ChatMessageEntity,
        val unreadCount: Long,
    )
    @Query("SELECT * FROM chat_messages WHERE ownerUserId = :ownerUserId AND conversationUserId = :userId ORDER BY createdAt DESC, messageId DESC")
    fun pagingSource(ownerUserId: String, userId: String): PagingSource<Int, ChatMessageEntity>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE ownerUserId = :ownerUserId AND conversationUserId = :userId AND (createdAt > :createdAt OR (createdAt = :createdAt AND messageId > :messageId))")
    suspend fun messagePosition(ownerUserId: String, userId: String, createdAt: String, messageId: String): Int

    @Query("SELECT * FROM chat_messages WHERE ownerUserId = :ownerUserId AND conversationUserId = :userId ORDER BY createdAt ASC, messageId ASC")
    fun observeConversation(ownerUserId: String, userId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE ownerUserId = :ownerUserId AND conversationUserId = :userId ORDER BY createdAt ASC, messageId ASC")
    suspend fun conversationSnapshot(ownerUserId: String, userId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE ownerUserId = :ownerUserId AND conversationUserId = :userId ORDER BY createdAt DESC, messageId DESC LIMIT 1")
    fun observeLatest(ownerUserId: String, userId: String): Flow<ChatMessageEntity?>

    @Query("SELECT MAX(updatedAt) FROM chat_messages WHERE ownerUserId = :ownerUserId")
    suspend fun latestUpdatedAt(ownerUserId: String): String?

    @Upsert
    suspend fun upsert(message: ChatMessageEntity)

    @Upsert
    suspend fun upsertAll(messages: List<ChatMessageEntity>)

    @Query("UPDATE chat_messages SET deliveryState = :state, deliveredAt = COALESCE(:deliveredAt, deliveredAt), readAt = COALESCE(:readAt, readAt), readReceiptPending = 0, updatedAt = :updatedAt WHERE ownerUserId = :ownerUserId AND messageId = :messageId")
    suspend fun updateReceipt(ownerUserId: String, messageId: String, state: String, deliveredAt: String?, readAt: String?, updatedAt: String)

    @Query("SELECT * FROM chat_messages WHERE ownerUserId = :ownerUserId AND deliveryState = 'PENDING' ORDER BY cachedAt ASC")
    suspend fun pendingMessages(ownerUserId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE ownerUserId = :ownerUserId AND clientMessageId = :clientMessageId LIMIT 1")
    suspend fun messageByClientId(ownerUserId: String, clientMessageId: String): ChatMessageEntity?

    @Query("SELECT * FROM chat_messages WHERE ownerUserId = :ownerUserId AND messageId = :messageId LIMIT 1")
    suspend fun messageById(ownerUserId: String, messageId: String): ChatMessageEntity?

    @Query("UPDATE chat_messages SET deliveryState = 'PENDING', updatedAt = :updatedAt WHERE ownerUserId = :ownerUserId AND clientMessageId = :clientMessageId")
    suspend fun markPending(ownerUserId: String, clientMessageId: String, updatedAt: String)

    @Query("DELETE FROM chat_messages WHERE ownerUserId = :ownerUserId AND clientMessageId = :clientMessageId AND deliveryState IN ('PENDING', 'FAILED')")
    suspend fun deletePending(ownerUserId: String, clientMessageId: String)

    @Query("UPDATE chat_messages SET deliveryState = 'FAILED', updatedAt = :updatedAt WHERE ownerUserId = :ownerUserId AND clientMessageId = :clientMessageId AND deliveryState = 'PENDING'")
    suspend fun markFailed(ownerUserId: String, clientMessageId: String, updatedAt: String)

    @Query("UPDATE chat_messages SET deliveryState = 'READ', readAt = :readAt, readReceiptPending = 1, updatedAt = :readAt WHERE ownerUserId = :ownerUserId AND messageId = :messageId")
    suspend fun markReadPending(ownerUserId: String, messageId: String, readAt: String)

    @Query("SELECT * FROM chat_messages WHERE ownerUserId = :ownerUserId AND readReceiptPending = 1 ORDER BY updatedAt ASC")
    suspend fun pendingReadReceipts(ownerUserId: String): List<ChatMessageEntity>

    @Query("DELETE FROM chat_messages WHERE ownerUserId = :ownerUserId AND conversationUserId = :userId")
    suspend fun clearConversation(ownerUserId: String, userId: String)

    @Query("SELECT * FROM chat_remote_keys WHERE ownerUserId = :ownerUserId AND conversationUserId = :userId")
    suspend fun remoteKey(ownerUserId: String, userId: String): ChatRemoteKeyEntity?

    @Upsert
    suspend fun upsertRemoteKey(key: ChatRemoteKeyEntity)

    @Query("DELETE FROM chat_remote_keys WHERE ownerUserId = :ownerUserId AND conversationUserId = :userId")
    suspend fun clearRemoteKey(ownerUserId: String, userId: String)

    @Query("""
        SELECT message.*,
            (SELECT COUNT(*) FROM chat_messages unread
             WHERE unread.ownerUserId = :ownerUserId
               AND unread.conversationUserId = message.conversationUserId
               AND unread.isMine = 0 AND unread.deliveryState != 'READ') AS unreadCount
        FROM chat_messages message
        WHERE message.ownerUserId = :ownerUserId
          AND message.messageId = (
              SELECT newest.messageId FROM chat_messages newest
              WHERE newest.ownerUserId = :ownerUserId
                AND newest.conversationUserId = message.conversationUserId
              ORDER BY newest.createdAt DESC, newest.messageId DESC LIMIT 1
          )
        ORDER BY message.createdAt DESC, message.messageId DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun conversationPage(ownerUserId: String, limit: Int, offset: Int): List<ConversationRow>
}
