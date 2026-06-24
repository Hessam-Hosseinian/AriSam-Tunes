package com.arisamtunes.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.arisamtunes.social.PublicUserResponse

@Serializable
enum class ChatMessageType { TEXT, SONG }

@Serializable
enum class ChatMessageStatus { SENT, DELIVERED, READ }

@Serializable
data class ChatMessageResponse(
    val id: String,
    @SerialName("client_message_id") val clientMessageId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("message_type") val messageType: ChatMessageType,
    val content: String? = null,
    @SerialName("song_id") val songId: String? = null,
    val status: ChatMessageStatus,
    @SerialName("created_at") val createdAt: String,
    @SerialName("delivered_at") val deliveredAt: String? = null,
    @SerialName("read_at") val readAt: String? = null,
)

@Serializable
data class ChatSocketEnvelope(
    val type: String,
    @SerialName("message_id") val messageId: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("client_message_id") val clientMessageId: String? = null,
    @SerialName("recipient_id") val recipientId: String? = null,
    @SerialName("message_type") val messageType: ChatMessageType? = null,
    val content: String? = null,
    @SerialName("song_id") val songId: String? = null,
    val message: ChatMessageResponse? = null,
    val error: String? = null,
)

@Serializable
data class ChatMessageListResponse(
    val items: List<ChatMessageResponse>,
    val pagination: com.arisamtunes.model.PaginationMeta,
)

@Serializable
data class ChatConversationResponse(
    val user: PublicUserResponse,
    @SerialName("latest_message") val latestMessage: ChatMessageResponse,
)

@Serializable
data class ChatConversationListResponse(val items: List<ChatConversationResponse>)
