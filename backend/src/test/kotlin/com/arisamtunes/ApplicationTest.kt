package com.arisamtunes

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.ktor.server.config.MapApplicationConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.arisamtunes.model.ErrorCode
import com.arisamtunes.model.ErrorEnvelope
import com.arisamtunes.model.HealthResponse
import kotlinx.serialization.json.Json

class ApplicationTest {
    @Test
    fun `health endpoint is available below api v1`() = testApplication {
        environment { config = testConfig() }
        application { module() }

        val response = client.get("/api/v1/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.headers["Content-Type"].orEmpty().startsWith("application/json"))
        assertEquals("ok", Json.decodeFromString<HealthResponse>(response.bodyAsText()).status)
    }

    @Test
    fun `unversioned health endpoint is not exposed`() = testApplication {
        environment { config = testConfig() }
        application { module() }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(
            ErrorCode.VALIDATION_ERROR,
            Json.decodeFromString<ErrorEnvelope>(response.bodyAsText()).error.code,
        )
    }

    @Test
    fun `swagger ui is available for api debugging`() = testApplication {
        environment { config = testConfig() }
        application { module() }

        val response = client.get("/swagger")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Swagger UI"))
    }

    @Test
    fun `swagger ui is also available below api v1`() = testApplication {
        environment { config = testConfig() }
        application { module() }

        val response = client.get("/api/v1/swagger")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Swagger UI"))
    }
}

private fun testConfig() = MapApplicationConfig(
    "database.enabled" to "false",
    "database.host" to "localhost", "database.port" to "5432", "database.name" to "test",
    "database.user" to "test", "database.password" to "test", "app.publicBaseUrl" to "http://localhost",
    "jwt.secret" to "unit-test-secret-unit-test-secret", "jwt.issuer" to "test", "jwt.audience" to "test", "jwt.realm" to "test",
)
