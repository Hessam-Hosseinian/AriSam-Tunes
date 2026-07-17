package com.arisamtunes.chat

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.send
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ChatConnectionRegistry(private val json: Json) {
    private val sessions = ConcurrentHashMap<UUID, MutableSet<WebSocketSession>>()
    private val presenceSubscribers = ConcurrentHashMap<UUID, MutableSet<UUID>>()
    private val lastSeenAt = ConcurrentHashMap<UUID, String>()

    fun register(userId: UUID, session: WebSocketSession): Boolean {
        var becameOnline = false
        sessions.compute(userId) { _, current ->
            val userSessions = current ?: ConcurrentHashMap.newKeySet()
            becameOnline = userSessions.isEmpty()
            userSessions.add(session)
            userSessions
        }
        if (becameOnline) lastSeenAt.remove(userId)
        return becameOnline
    }

    fun unregister(userId: UUID, session: WebSocketSession): Boolean {
        var becameOffline = false
        sessions.computeIfPresent(userId) { _, userSessions ->
            userSessions.remove(session)
            if (userSessions.isEmpty()) {
                becameOffline = true
                null
            } else {
                userSessions
            }
        }
        if (becameOffline) lastSeenAt[userId] = Instant.now().toString()
        return becameOffline
    }

    fun subscribeToPresence(subscriberId: UUID, targetId: UUID) {
        presenceSubscribers.computeIfAbsent(targetId) { ConcurrentHashMap.newKeySet() }.add(subscriberId)
    }

    fun unsubscribeFromPresence(subscriberId: UUID, targetId: UUID) {
        presenceSubscribers[targetId]?.let { subscribers ->
            subscribers.remove(subscriberId)
            if (subscribers.isEmpty()) presenceSubscribers.remove(targetId, subscribers)
        }
    }

    fun removePresenceSubscriptions(subscriberId: UUID) {
        presenceSubscribers.forEach { (targetId, subscribers) ->
            subscribers.remove(subscriberId)
            if (subscribers.isEmpty()) presenceSubscribers.remove(targetId, subscribers)
        }
    }

    suspend fun sendPresenceTo(subscriberId: UUID, targetId: UUID) {
        sendTo(subscriberId, presenceEnvelope(targetId))
    }

    suspend fun publishPresence(targetId: UUID) {
        val envelope = presenceEnvelope(targetId)
        presenceSubscribers[targetId]?.toList().orEmpty().forEach { subscriberId ->
            sendTo(subscriberId, envelope)
        }
    }

    private fun presenceEnvelope(userId: UUID) = ChatSocketEnvelope(
        type = ChatSocketType.PRESENCE_UPDATED,
        userId = userId.toString(),
        isOnline = sessions[userId]?.isNotEmpty() == true,
        lastSeenAt = lastSeenAt[userId],
    )

    suspend fun sendTo(userId: UUID, envelope: ChatSocketEnvelope): Int {
        val encoded = json.encodeToString(envelope)
        var delivered = 0
        sessions[userId]?.toList().orEmpty().forEach { session ->
            if (runCatching { session.send(Frame.Text(encoded)) }.isSuccess) {
                delivered++
            } else {
                unregister(userId, session)
            }
        }
        return delivered
    }
}
