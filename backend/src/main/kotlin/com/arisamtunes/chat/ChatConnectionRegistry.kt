package com.arisamtunes.chat

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.send
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ChatConnectionRegistry(private val json: Json) {
    private val sessions = ConcurrentHashMap<UUID, MutableSet<WebSocketSession>>()

    fun register(userId: UUID, session: WebSocketSession) {
        sessions.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun unregister(userId: UUID, session: WebSocketSession) {
        sessions[userId]?.let { userSessions ->
            userSessions.remove(session)
            if (userSessions.isEmpty()) sessions.remove(userId, userSessions)
        }
    }

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
