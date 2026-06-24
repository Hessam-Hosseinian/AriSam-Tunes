package com.arisamtunes.auth

import com.arisamtunes.model.ApiException
import com.arisamtunes.model.ErrorCode
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.authRoutes(service: AuthService) {
    val limiter = InMemoryRateLimiter()
    route("/auth") {
        post("/register") {
            call.enforceRate(limiter, "register", 10)
            call.respond(HttpStatusCode.Created, service.register(call.receive()))
        }
        post("/login") {
            call.enforceRate(limiter, "login", 20)
            call.respond(service.login(call.receive()))
        }
        post("/refresh") {
            call.enforceRate(limiter, "refresh", 30)
            call.respond(service.refresh(call.receive<RefreshRequest>().refreshToken))
        }
        authenticate("auth-jwt") {
            post("/logout") {
                service.logout(call.receive<LogoutRequest>().refreshToken)
                call.respond(HttpStatusCode.NoContent)
            }
            get("/me") {
                val subject = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(service.currentUser(UUID.fromString(subject)))
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.enforceRate(limiter: InMemoryRateLimiter, route: String, limit: Int) {
    val ip = request.headers["X-Forwarded-For"]?.substringBefore(',')?.trim() ?: request.headers["X-Real-IP"] ?: "local"
    if (!limiter.allow("$route:$ip", limit)) throw ApiException(HttpStatusCode.TooManyRequests, ErrorCode.RATE_LIMITED, "Too many requests; try again shortly")
}
