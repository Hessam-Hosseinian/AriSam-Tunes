package com.arisamtunes.plugins

import com.arisamtunes.model.ApiException
import com.arisamtunes.model.ErrorBody
import com.arisamtunes.model.ErrorCode
import com.arisamtunes.model.ErrorEnvelope
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureSerialization() {
    val applicationLog = environment.log

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
                namingStrategy = JsonNamingStrategy.SnakeCase
            },
        )
    }

    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respond(cause.status, cause.toEnvelope())
        }
        exception<Throwable> { call, cause ->
            applicationLog.error("Unhandled request failure", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorEnvelope(ErrorBody(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred")),
            )
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                ErrorEnvelope(ErrorBody(ErrorCode.VALIDATION_ERROR, "The requested resource was not found")),
            )
        }
    }
}

private fun ApiException.toEnvelope() = ErrorEnvelope(ErrorBody(code, message))
