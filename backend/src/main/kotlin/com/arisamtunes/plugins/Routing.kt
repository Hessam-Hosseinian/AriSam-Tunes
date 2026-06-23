package com.arisamtunes.plugins

import com.arisamtunes.model.HealthResponse
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

private const val API_V1 = "/api/v1"

fun Application.configureRouting() {
    routing {
        route(API_V1) {
            get("/health") {
                call.respond(
                    HealthResponse(
                        status = "ok",
                        service = "arisam-tunes-backend",
                    ),
                )
            }
        }
    }
}
