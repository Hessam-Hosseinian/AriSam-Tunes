package com.arisamtunes.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.arisamtunes.social.PublicUserResponse

const val CHAT_PROTOCOL_VERSION = 1

@Serializable
enum class ChatMessageType { TEXT, SONG }

@Serializable
enum class ChatMessageStatus { SENT, DELIVERED, READ }

@Serializable
enum class ChatSocketType {
    @SerialName("connected") CONNECTED,
    @SerialName("send_message") SEND_MESSAGE,
    @SerialName("message_sent") MESSAGE_SENT,
    @SerialName("message_received") MESSAGE_RECEIVED,
    @SerialName("message_delivered") MESSAGE_DELIVERED,
    @SerialName("message_read") MESSAGE_READ,
    @SerialName("typing_start") TYPING_START,
    @SerialName("typing_stop") TYPING_STOP,
    @SerialName("presence_subscribe") PRESENCE_SUBSCRIBE,
    @SerialName("presence_unsubscribe") PRESENCE_UNSUBSCRIBE,
    @SerialName("presence_updated") PRESENCE_UPDATED,
    @SerialName("edit_message") EDIT_MESSAGE,
    @SerialName("delete_message") DELETE_MESSAGE,
    @SerialName("add_reaction") ADD_REACTION,
    @SerialName("remove_reaction") REMOVE_REACTION,
    @SerialName("message_updated") MESSAGE_UPDATED,
    @SerialName("error") ERROR,
}

@Serializable
data class ChatReactionResponse(
    val reaction: String,
    val count: Int,
    @SerialName("reacted_by_me") val reactedByMe: Boolean = false,
)

@Serializable
data class ChatMessageResponse(
    val id: String,
    @SerialName("client_message_id") val clientMessageId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("message_type") val messageType: ChatMessageType,
    val content: String? = null,
    @SerialName("song_id") val songId: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    val status: ChatMessageStatus,
    @SerialName("created_at") val createdAt: String,
    @SerialName("delivered_at") val deliveredAt: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    val reactions: List<ChatReactionResponse> = emptyList(),
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ChatSocketEnvelope(
    val type: ChatSocketType,
    @SerialName("request_type") val requestType: ChatSocketType? = null,
    @SerialName("protocol_version") val protocolVersion: Int = CHAT_PROTOCOL_VERSION,
    @SerialName("message_id") val messageId: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("client_message_id") val clientMessageId: String? = null,
    @SerialName("recipient_id") val recipientId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("is_online") val isOnline: Boolean? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("message_type") val messageType: ChatMessageType? = null,
    val content: String? = null,
    @SerialName("song_id") val songId: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    val reaction: String? = null,
    val message: ChatMessageResponse? = null,
    val error: String? = null,
)

@Serializable
data class ChatMessageListResponse(
    val items: List<ChatMessageResponse>,
    val pagination: com.arisamtunes.model.PaginationMeta? = null,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class ChatSyncResponse(
    val items: List<ChatMessageResponse>,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class ChatConversationResponse(
    val user: PublicUserResponse,
    @SerialName("latest_message") val latestMessage: ChatMessageResponse,
    @SerialName("unread_count") val unreadCount: Long = 0,
)

@Serializable
data class ChatConversationListResponse(
    val items: List<ChatConversationResponse>,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
)
