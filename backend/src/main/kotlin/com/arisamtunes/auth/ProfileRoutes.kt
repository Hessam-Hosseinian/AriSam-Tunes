package com.arisamtunes.auth

import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID

fun Route.profileRoutes(service: AuthService) {
    authenticate("auth-jwt") {
        route("/users/me") {
            get { call.respond(service.currentUser(call.userId())) }
            put { call.respond(service.updateProfile(call.userId(), call.receive())) }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.userId(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)
