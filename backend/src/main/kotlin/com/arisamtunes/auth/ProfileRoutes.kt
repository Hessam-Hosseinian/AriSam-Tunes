package com.arisamtunes.auth

import com.arisamtunes.model.ApiException
import com.arisamtunes.model.ErrorCode
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray

private const val MaxAvatarBytes = 5 * 1024 * 1024
private val SupportedAvatarTypes = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")

fun Route.profileRoutes(
    service: AuthService,
    publicBaseUrl: String,
    avatarStorage: AvatarStorage = AvatarStorage(),
) {
    authenticate("auth-jwt") {
        route("/users/me") {
            get { call.respond(service.currentUser(call.userId())) }
            put { call.respond(service.updateProfile(call.userId(), call.receive())) }
            put("/premium") { call.respond(service.updatePremium(call.userId(), call.receive())) }
            post("/avatar") {
                val userId = call.userId()
                val upload = call.receiveAvatar()
                val fileName = avatarStorage.save(userId, upload.contentType, upload.bytes)
                val avatarUrl = "${publicBaseUrl.trimEnd('/')}/media/avatars/$fileName"
                val updated = runCatching {
                    service.updateProfile(userId, UpdateProfileRequest(avatarUrl = avatarUrl))
                }.getOrElse { failure ->
                    avatarStorage.delete(fileName)
                    throw failure
                }
                call.respond(updated)
            }
        }
    }
}

private data class AvatarUpload(val contentType: String, val bytes: ByteArray)

private suspend fun io.ktor.server.application.ApplicationCall.receiveAvatar(): AvatarUpload {
    var upload: AvatarUpload? = null
    receiveMultipart().forEachPart { part ->
        try {
            if (part is PartData.FileItem && part.name == "avatar" && upload == null) {
                val contentType = part.contentType?.withoutParameters()?.toString()?.lowercase().orEmpty()
                if (contentType !in SupportedAvatarTypes) {
                    throw ApiException(HttpStatusCode.UnsupportedMediaType, ErrorCode.VALIDATION_ERROR, "Avatar must be JPEG, PNG, or WebP")
                }
                val bytes = part.provider().readRemaining((MaxAvatarBytes + 1).toLong()).readByteArray()
                if (bytes.isEmpty() || bytes.size > MaxAvatarBytes) {
                    throw ApiException(HttpStatusCode.PayloadTooLarge, ErrorCode.VALIDATION_ERROR, "Avatar must be between 1 byte and 5 MB")
                }
                upload = AvatarUpload(contentType, bytes)
            }
        } finally {
            part.release()
        }
    }
    return upload ?: throw ApiException(HttpStatusCode.BadRequest, ErrorCode.VALIDATION_ERROR, "Multipart field 'avatar' is required")
}

private fun io.ktor.server.application.ApplicationCall.userId(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)
