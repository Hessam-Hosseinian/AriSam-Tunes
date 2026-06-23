package com.arisamtunes.model

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorEnvelope(val error: ErrorBody)

@Serializable
data class ErrorBody(
    val code: ErrorCode,
    val message: String,
)

@Serializable
enum class ErrorCode {
    @SerialName("AUTH_INVALID_CREDENTIALS") AUTH_INVALID_CREDENTIALS,
    @SerialName("AUTH_TOKEN_EXPIRED") AUTH_TOKEN_EXPIRED,
    @SerialName("AUTH_TOKEN_INVALID") AUTH_TOKEN_INVALID,
    @SerialName("AUTH_REFRESH_TOKEN_EXPIRED") AUTH_REFRESH_TOKEN_EXPIRED,
    @SerialName("AUTH_UNAUTHORIZED") AUTH_UNAUTHORIZED,
    @SerialName("USER_NOT_FOUND") USER_NOT_FOUND,
    @SerialName("USER_ALREADY_EXISTS") USER_ALREADY_EXISTS,
    @SerialName("SONG_NOT_FOUND") SONG_NOT_FOUND,
    @SerialName("PLAYLIST_NOT_FOUND") PLAYLIST_NOT_FOUND,
    @SerialName("ARTIST_NOT_FOUND") ARTIST_NOT_FOUND,
    @SerialName("PREMIUM_REQUIRED") PREMIUM_REQUIRED,
    @SerialName("VALIDATION_ERROR") VALIDATION_ERROR,
    @SerialName("RATE_LIMITED") RATE_LIMITED,
    @SerialName("INTERNAL_ERROR") INTERNAL_ERROR,
}

class ApiException(
    val status: HttpStatusCode,
    val code: ErrorCode,
    override val message: String,
) : RuntimeException(message)
