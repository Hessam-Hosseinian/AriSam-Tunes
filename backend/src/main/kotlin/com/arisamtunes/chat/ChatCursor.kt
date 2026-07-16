package com.arisamtunes.chat

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import java.util.UUID

data class ChatCursor(val timestamp: Instant, val id: UUID) {
    fun encode(): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("$timestamp|$id".toByteArray(StandardCharsets.UTF_8))

    companion object {
        fun decode(value: String): ChatCursor? = runCatching {
            val decoded = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
            val separator = decoded.lastIndexOf('|')
            require(separator > 0 && separator < decoded.lastIndex)
            ChatCursor(
                timestamp = Instant.parse(decoded.substring(0, separator)),
                id = UUID.fromString(decoded.substring(separator + 1)),
            )
        }.getOrNull()
    }
}

data class CursorPage<T>(
    val items: List<T>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

internal fun <T> List<T>.toCursorPage(
    size: Int,
    includeTerminalCursor: Boolean = false,
    cursorOf: (T) -> ChatCursor,
): CursorPage<T> {
    val hasMore = this.size > size
    val visibleItems = take(size)
    return CursorPage(
        items = visibleItems,
        nextCursor = visibleItems.lastOrNull()?.let(cursorOf)?.encode().takeIf { hasMore || includeTerminalCursor },
        hasMore = hasMore,
    )
}
