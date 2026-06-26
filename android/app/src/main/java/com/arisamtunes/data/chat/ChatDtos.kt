package com.arisamtunes.data.chat

import com.arisamtunes.data.social.PublicUserDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageDto(
    val id: String,
    @SerialName("client_message_id") val clientMessageId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("message_type") val messageType: String = "TEXT",
    val content: String? = null,
    @SerialName("song_id") val songId: String? = null,
    val status: String = "SENT",
    @SerialName("created_at") val createdAt: String,
    @SerialName("delivered_at") val deliveredAt: String? = null,
    @SerialName("read_at") val readAt: String? = null,
)

@Serializable
data class ChatMessageListDto(val items: List<ChatMessageDto>)

@Serializable
data class ChatConversationDto(
    val user: PublicUserDto,
    @SerialName("latest_message") val latestMessage: ChatMessageDto,
)

@Serializable
data class ChatConversationListDto(val items: List<ChatConversationDto>)

@Serializable
data class ChatSocketEnvelopeDto(
    val type: String,
    @SerialName("message_id") val messageId: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("client_message_id") val clientMessageId: String? = null,
    @SerialName("recipient_id") val recipientId: String? = null,
    @SerialName("message_type") val messageType: String? = null,
    val content: String? = null,
    @SerialName("song_id") val songId: String? = null,
    val message: ChatMessageDto? = null,
    val error: String? = null,
)

enum class ChatConnectionStatus { Disconnected, Connecting, Connected, Reconnecting }
