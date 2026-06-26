package com.arisamtunes.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class RegisterRequest(val email: String, val password: String, @SerialName("display_name") val displayName: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class LogoutRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long = 900,
    val user: UserResponse,
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String?,
    val bio: String?,
    @SerialName("is_premium") val isPremium: Boolean,
)

data class AuthUser(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String?,
    val isPremium: Boolean,
) {
    fun response() = UserResponse(id.toString(), email, displayName, avatarUrl, bio, isPremium)
}

@Serializable
data class UpdateProfileRequest(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
)

@Serializable
data class PremiumStatusRequest(
    @SerialName("is_premium") val isPremium: Boolean,
)
