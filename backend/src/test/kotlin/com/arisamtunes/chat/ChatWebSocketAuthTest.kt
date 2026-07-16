package com.arisamtunes.chat

import com.arisamtunes.module
import com.arisamtunes.auth.JwtService
import com.arisamtunes.config.JwtConfig
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertNotNull

class ChatWebSocketAuthTest {
    @Test
    fun `websocket rejects a token passed in the query string`() = testApplication {
        environment { config = chatTestConfig() }
        application { module() }
        val websocketClient = createClient { install(WebSockets) }
        val token = JwtService(TestJwtConfig).accessToken(UUID.randomUUID())

        val failure = runCatching {
            websocketClient.webSocket("/api/v1/ws/chat?token=$token") { }
        }.exceptionOrNull()

        assertNotNull(failure)
    }

    private companion object {
        val TestJwtConfig = JwtConfig(
            secret = "unit-test-secret-unit-test-secret",
            issuer = "test",
            audience = "test",
            realm = "test",
        )
    }
}

private fun chatTestConfig() = MapApplicationConfig(
    "database.enabled" to "false",
    "database.host" to "localhost",
    "database.port" to "5432",
    "database.name" to "test",
    "database.user" to "test",
    "database.password" to "test",
    "app.publicBaseUrl" to "http://localhost",
    "jwt.secret" to "unit-test-secret-unit-test-secret",
    "jwt.issuer" to "test",
    "jwt.audience" to "test",
    "jwt.realm" to "test",
)
