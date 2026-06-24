package com.arisamtunes.auth

import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

class InMemoryRateLimiter(private val clock: Clock = Clock.systemUTC()) {
    private data class Window(var startedAt: Long, var count: Int)
    private val windows = ConcurrentHashMap<String, Window>()

    fun allow(key: String, limit: Int): Boolean {
        val now = clock.millis()
        val window = windows.compute(key) { _, current ->
            if (current == null || now - current.startedAt >= 60_000) Window(now, 1)
            else current.apply { count++ }
        }!!
        return window.count <= limit
    }
}
