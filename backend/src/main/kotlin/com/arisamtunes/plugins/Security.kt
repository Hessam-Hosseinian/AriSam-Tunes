package com.arisamtunes.plugins

import com.arisamtunes.auth.AuthRepository
import com.arisamtunes.auth.AuthService
import com.arisamtunes.auth.JwtService
import com.arisamtunes.config.AppConfig
import com.arisamtunes.model.ErrorBody
import com.arisamtunes.model.ErrorCode
import com.arisamtunes.model.ErrorEnvelope
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import java.util.Date

lateinit var authService: AuthService

fun Application.configureSecurity() {
    val config = AppConfig.from(environment.config).jwt
    val jwtService = JwtService(config)
    authService = AuthService(AuthRepository(), jwtService)
    install(Authentication) {
        jwt("auth-jwt") {
            realm = config.realm
            verifier(jwtService.verifier)
            validate { credential ->
                if (credential.payload.getClaim("type").asString() == "access" && credential.payload.subject != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                val raw = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                val expired = runCatching { com.auth0.jwt.JWT.decode(raw).expiresAt.before(Date()) }.getOrDefault(false)
                val code = when { raw == null -> ErrorCode.AUTH_UNAUTHORIZED; expired -> ErrorCode.AUTH_TOKEN_EXPIRED; else -> ErrorCode.AUTH_TOKEN_INVALID }
                val message = when (code) { ErrorCode.AUTH_UNAUTHORIZED -> "Authentication is required"; ErrorCode.AUTH_TOKEN_EXPIRED -> "Access token has expired"; else -> "Access token is invalid" }
                call.respond(HttpStatusCode.Unauthorized, ErrorEnvelope(ErrorBody(code, message)))
            }
        }
    }
}
