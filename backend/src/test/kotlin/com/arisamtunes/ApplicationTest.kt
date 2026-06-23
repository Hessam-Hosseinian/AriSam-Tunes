package com.arisamtunes

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun `health endpoint is available below api v1`() = testApplication {
        application { module() }

        val response = client.get("/api/v1/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.headers["Content-Type"].orEmpty().startsWith("application/json"))
    }

    @Test
    fun `unversioned health endpoint is not exposed`() = testApplication {
        application { module() }

        assertEquals(HttpStatusCode.NotFound, client.get("/health").status)
    }
}
