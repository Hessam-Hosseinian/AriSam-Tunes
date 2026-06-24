package com.arisamtunes.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class LoginDto(val email: String, val password: String)
@Serializable data class RegisterDto(val email: String, val password: String, @SerialName("display_name") val displayName: String)
@Serializable data class RefreshDto(@SerialName("refresh_token") val refreshToken: String)
@Serializable data class TokenDto(@SerialName("access_token") val accessToken: String, @SerialName("refresh_token") val refreshToken: String)
@Serializable data class UserDto(val id: String, val email: String)
@Serializable data class ErrorEnvelopeDto(val error: ErrorDto)
@Serializable data class ErrorDto(val code: String, val message: String)
