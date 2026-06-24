package com.arisamtunes.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.arisamtunes.model.ApiException
import com.arisamtunes.model.ErrorCode
import io.ktor.http.HttpStatusCode
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID

class AuthService(private val repository: AuthRepository, private val jwt: JwtService) {
    fun register(request: RegisterRequest): TokenResponse {
        val email = request.email.trim().lowercase()
        validate(email, request.password, request.displayName)
        if (repository.findByEmail(email) != null) throw ApiException(HttpStatusCode.Conflict, ErrorCode.USER_ALREADY_EXISTS, "An account already exists for this email")
        val hash = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())
        return tokens(repository.createUser(email, hash, request.displayName.trim()))
    }

    fun login(request: LoginRequest): TokenResponse {
        val user = repository.findByEmail(request.email.trim())
        if (user == null || !BCrypt.verifyer().verify(request.password.toCharArray(), user.passwordHash).verified) {
            throw ApiException(HttpStatusCode.Unauthorized, ErrorCode.AUTH_INVALID_CREDENTIALS, "Email or password is incorrect")
        }
        return tokens(user)
    }

    fun refresh(rawToken: String): TokenResponse {
        val user = repository.consumeRefresh(hash(rawToken))
            ?: throw ApiException(HttpStatusCode.Unauthorized, ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED, "Refresh token is expired or invalid")
        return tokens(user)
    }

    fun logout(rawToken: String) = repository.revokeRefresh(hash(rawToken))
    fun currentUser(id: UUID) = repository.findById(id)?.response()
        ?: throw ApiException(HttpStatusCode.NotFound, ErrorCode.USER_NOT_FOUND, "User does not exist")

    fun updateProfile(id: UUID, request: UpdateProfileRequest): UserResponse {
        val current = repository.findById(id)
            ?: throw ApiException(HttpStatusCode.NotFound, ErrorCode.USER_NOT_FOUND, "User does not exist")
        val name = request.displayName?.trim() ?: current.displayName
        val avatar = request.avatarUrl?.trim()?.takeIf(String::isNotEmpty) ?: current.avatarUrl
        val bio = request.bio?.trim()?.takeIf(String::isNotEmpty)
        if (name.length !in 2..100 || (bio?.length ?: 0) > 500 || (avatar?.length ?: 0) > 2_000) {
            throw ApiException(HttpStatusCode.BadRequest, ErrorCode.VALIDATION_ERROR, "Profile fields exceed their allowed length")
        }
        if (avatar != null && !avatar.startsWith("http://") && !avatar.startsWith("https://")) {
            throw ApiException(HttpStatusCode.BadRequest, ErrorCode.VALIDATION_ERROR, "Avatar URL must use HTTP or HTTPS")
        }
        return repository.updateProfile(id, name, avatar, bio).response()
    }

    private fun tokens(user: AuthUser): TokenResponse {
        val refresh = ByteArray(48).also(SecureRandom()::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
        repository.storeRefresh(user.id, hash(refresh), Instant.now().plus(30, ChronoUnit.DAYS))
        return TokenResponse(jwt.accessToken(user.id), refresh, user = user.response())
    }

    private fun validate(email: String, password: String, displayName: String) {
        if (!email.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) || password.length < 8 || displayName.trim().length !in 2..100) {
            throw ApiException(HttpStatusCode.BadRequest, ErrorCode.VALIDATION_ERROR, "Provide a valid email, display name, and password of at least 8 characters")
        }
    }

    private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
