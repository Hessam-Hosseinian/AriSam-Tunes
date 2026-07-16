package com.arisamtunes.plugins

import com.arisamtunes.model.HealthResponse
import com.arisamtunes.auth.authRoutes
import com.arisamtunes.auth.profileRoutes
import com.arisamtunes.catalog.catalogRoutes
import com.arisamtunes.catalog.mediaRoutes
import com.arisamtunes.catalog.sharedSongRoutes
import com.arisamtunes.chat.chatRoutes
import com.arisamtunes.playlist.playlistRoutes
import com.arisamtunes.social.socialRoutes
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

private const val API_V1 = "/api/v1"

fun Application.configureRouting() {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        mediaRoutes()
        sharedSongRoutes()
        route(API_V1) {
            swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
            authRoutes(authService)
            profileRoutes(authService)
            catalogRoutes()
            playlistRoutes()
            socialRoutes()
            chatRoutes()
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
