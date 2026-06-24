package com.arisamtunes.plugins

import com.arisamtunes.model.HealthResponse
import com.arisamtunes.auth.authRoutes
import com.arisamtunes.auth.profileRoutes
import com.arisamtunes.catalog.catalogRoutes
import com.arisamtunes.catalog.mediaRoutes
import com.arisamtunes.chat.chatRoutes
import com.arisamtunes.config.AppConfig
import com.arisamtunes.playlist.playlistRoutes
import com.arisamtunes.social.socialRoutes
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

private const val API_V1 = "/api/v1"

fun Application.configureRouting() {
    val appConfig = AppConfig.from(environment.config)
    routing {
        mediaRoutes()
        route(API_V1) {
            authRoutes(authService)
            profileRoutes(authService)
            catalogRoutes()
            playlistRoutes()
            socialRoutes()
            chatRoutes(jwtConfig = appConfig.jwt)
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
