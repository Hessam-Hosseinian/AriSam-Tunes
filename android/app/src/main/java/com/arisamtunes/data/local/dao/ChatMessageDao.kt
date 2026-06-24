package com.arisamtunes.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.arisamtunes.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationUserId = :userId ORDER BY createdAt DESC")
    fun pagingSource(userId: String): PagingSource<Int, ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE conversationUserId = :userId ORDER BY createdAt DESC LIMIT 1")
    fun observeLatest(userId: String): Flow<ChatMessageEntity?>

    @Query("SELECT MAX(createdAt) FROM chat_messages WHERE conversationUserId = :userId")
    suspend fun latestTimestamp(userId: String): String?

    @Upsert
    suspend fun upsert(message: ChatMessageEntity)

    @Upsert
    suspend fun upsertAll(messages: List<ChatMessageEntity>)

    @Query("UPDATE chat_messages SET deliveryState = :state, deliveredAt = :deliveredAt, readAt = :readAt WHERE messageId = :messageId")
    suspend fun updateReceipt(messageId: String, state: String, deliveredAt: String?, readAt: String?)

    @Query("DELETE FROM chat_messages WHERE conversationUserId = :userId")
    suspend fun clearConversation(userId: String)
}
