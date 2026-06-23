package com.arisamtunes.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

private const val API_V1 = "/api/v1"

fun Application.configureRouting() {
    routing {
        route(API_V1) {
            get("/health") {
                call.respondText(
                    text = "{\"status\":\"ok\",\"service\":\"arisam-tunes-backend\"}",
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.OK,
                )
            }
        }
    }
}
