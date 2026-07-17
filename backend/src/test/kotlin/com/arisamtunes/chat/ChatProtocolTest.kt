package com.arisamtunes.chat

import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChatProtocolTest {
    private val json = Json { explicitNulls = false; encodeDefaults = true }

    @Test
    fun `cursor round trips timestamp and id`() {
        val cursor = ChatCursor(Instant.parse("2026-07-15T10:15:30.123Z"), UUID.fromString("2c22c8dc-cb92-4ede-89d0-abba781ec03e"))

        assertEquals(cursor, ChatCursor.decode(cursor.encode()))
    }

    @Test
    fun `invalid cursor is rejected without throwing`() {
        assertNull(ChatCursor.decode("not-a-cursor"))
    }

    @Test
    fun `cursor page emits a continuation only when more rows exist`() {
        val ids = listOf(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            UUID.fromString("00000000-0000-0000-0000-000000000003"),
        )
        val page = ids.toCursorPage(size = 2) { id -> ChatCursor(Instant.EPOCH, id) }

        assertEquals(ids.take(2), page.items)
        assertTrue(page.hasMore)
        assertEquals(ids[1], ChatCursor.decode(checkNotNull(page.nextCursor))?.id)

        val terminal = ids.take(2).toCursorPage(size = 2) { id -> ChatCursor(Instant.EPOCH, id) }
        assertFalse(terminal.hasMore)
        assertNull(terminal.nextCursor)
    }

    @Test
    fun `socket protocol serializes typed event and version`() {
        val encoded = json.encodeToString(
            ChatSocketEnvelope(
                type = ChatSocketType.SEND_MESSAGE,
                clientMessageId = "00000000-0000-0000-0000-000000000001",
                recipientId = "00000000-0000-0000-0000-000000000002",
                content = "hello",
            ),
        )

        assertTrue(encoded.contains("\"type\":\"send_message\""))
        assertTrue(encoded.contains("\"protocol_version\":1"))
        assertEquals(ChatSocketType.SEND_MESSAGE, json.decodeFromString<ChatSocketEnvelope>(encoded).type)
    }

    @Test
    fun `socket protocol carries reply edit and reaction events`() {
        val envelope = ChatSocketEnvelope(
            type = ChatSocketType.ADD_REACTION,
            messageId = "00000000-0000-0000-0000-000000000001",
            replyToId = "00000000-0000-0000-0000-000000000002",
            reaction = "🔥",
        )

        val encoded = json.encodeToString(envelope)

        assertTrue(encoded.contains("\"type\":\"add_reaction\""))
        assertTrue(encoded.contains("\"reply_to_id\""))
        assertEquals("🔥", json.decodeFromString<ChatSocketEnvelope>(encoded).reaction)
    }
}
