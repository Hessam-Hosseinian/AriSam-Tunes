package com.arisamtunes

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
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
        application { module() }

        val response = client.get("/api/v1/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.headers["Content-Type"].orEmpty().startsWith("application/json"))
        assertEquals("ok", Json.decodeFromString<HealthResponse>(response.bodyAsText()).status)
    }

    @Test
    fun `unversioned health endpoint is not exposed`() = testApplication {
        application { module() }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(
            ErrorCode.VALIDATION_ERROR,
            Json.decodeFromString<ErrorEnvelope>(response.bodyAsText()).error.code,
        )
    }
}
