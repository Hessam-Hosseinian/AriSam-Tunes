package com.arisamtunes.data.chat

import com.arisamtunes.data.social.PublicUserDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ChatMessageTypeDto { TEXT, SONG }

@Serializable
enum class ChatMessageStatusDto { PENDING, SENT, DELIVERED, READ, FAILED }

@Serializable
enum class ChatSocketTypeDto {
    @SerialName("connected") CONNECTED,
    @SerialName("send_message") SEND_MESSAGE,
    @SerialName("message_sent") MESSAGE_SENT,
    @SerialName("message_received") MESSAGE_RECEIVED,
    @SerialName("message_delivered") MESSAGE_DELIVERED,
    @SerialName("message_read") MESSAGE_READ,
    @SerialName("typing_start") TYPING_START,
    @SerialName("typing_stop") TYPING_STOP,
    @SerialName("edit_message") EDIT_MESSAGE,
    @SerialName("delete_message") DELETE_MESSAGE,
    @SerialName("add_reaction") ADD_REACTION,
    @SerialName("remove_reaction") REMOVE_REACTION,
    @SerialName("message_updated") MESSAGE_UPDATED,
    @SerialName("error") ERROR,
}

@Serializable
data class ChatReactionDto(
    val reaction: String,
    val count: Int,
    @SerialName("reacted_by_me") val reactedByMe: Boolean = false,
)

@Serializable
data class ChatMessageDto(
    val id: String,
    @SerialName("client_message_id") val clientMessageId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("message_type") val messageType: ChatMessageTypeDto = ChatMessageTypeDto.TEXT,
    val content: String? = null,
    @SerialName("song_id") val songId: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    val status: ChatMessageStatusDto = ChatMessageStatusDto.SENT,
    @SerialName("created_at") val createdAt: String,
    @SerialName("delivered_at") val deliveredAt: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    val reactions: List<ChatReactionDto> = emptyList(),
    @SerialName("updated_at") val updatedAt: String = createdAt,
)

@Serializable
data class ChatMessageListDto(
    val items: List<ChatMessageDto>,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class ChatSyncDto(
    val items: List<ChatMessageDto>,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class ChatConversationDto(
    val user: PublicUserDto,
    @SerialName("latest_message") val latestMessage: ChatMessageDto,
    @SerialName("unread_count") val unreadCount: Long = 0,
)

@Serializable
data class ChatConversationListDto(
    val items: List<ChatConversationDto>,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class ChatSocketEnvelopeDto(
    val type: ChatSocketTypeDto,
    @SerialName("request_type") val requestType: ChatSocketTypeDto? = null,
    @SerialName("protocol_version") val protocolVersion: Int = 1,
    @SerialName("message_id") val messageId: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("client_message_id") val clientMessageId: String? = null,
    @SerialName("recipient_id") val recipientId: String? = null,
    @SerialName("message_type") val messageType: ChatMessageTypeDto? = null,
    val content: String? = null,
    @SerialName("song_id") val songId: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    val reaction: String? = null,
    val message: ChatMessageDto? = null,
    val error: String? = null,
)

enum class ChatConnectionStatus { Disconnected, Connecting, Connected, Reconnecting }
