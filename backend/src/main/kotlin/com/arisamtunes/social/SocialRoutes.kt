package com.arisamtunes.social

import com.arisamtunes.model.ApiException
import com.arisamtunes.model.ErrorCode
import com.arisamtunes.model.PaginationMeta
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.socialRoutes(repository: SocialRepository = SocialRepository()) {
    route("/users") {
        authenticate("auth-jwt") {
            get("/me/following") {
                val (page, size) = call.pageRequest()
                val (items, total) = repository.following(call.userId(), call.userId(), page, size)
                call.respond(PublicUserListResponse(items, PaginationMeta(page, size, total, pages(total, size))))
            }
            get("/me/followers") {
                val (page, size) = call.pageRequest()
                val (items, total) = repository.followers(call.userId(), call.userId(), page, size)
                call.respond(PublicUserListResponse(items, PaginationMeta(page, size, total, pages(total, size))))
            }
            post("/{id}/follow") {
                val targetId = call.userPathId()
                if (targetId == call.userId()) throw validation("You cannot follow yourself")
                val user = repository.follow(call.userId(), targetId) ?: throw notFound()
                call.respond(FollowResponse(user))
            }
            delete("/{id}/follow") {
                val targetId = call.userPathId()
                if (targetId == call.userId()) throw validation("You cannot unfollow yourself")
                val user = repository.unfollow(call.userId(), targetId) ?: throw notFound()
                call.respond(FollowResponse(user))
            }
        }
        authenticate("auth-jwt", optional = true) {
            get("/search") {
                val query = call.request.queryParameters["q"]?.trim().orEmpty()
                if (query.length !in 1..100) throw validation("q must be between 1 and 100 characters")
                val (page, size) = call.pageRequest()
                val (items, total) = repository.searchUsers(query, call.optionalUserId(), page, size)
                call.respond(PublicUserListResponse(items, PaginationMeta(page, size, total, pages(total, size))))
            }
            get("/{id}") {
                call.rejectReservedUserAlias()
                call.respond(repository.user(call.userPathId(), call.optionalUserId()) ?: throw notFound())
            }
            get("/{id}/following") {
                call.rejectReservedUserAlias()
                val id = call.userPathId()
                if (repository.user(id, call.optionalUserId()) == null) throw notFound()
                val (page, size) = call.pageRequest()
                val (items, total) = repository.following(id, call.optionalUserId(), page, size)
                call.respond(PublicUserListResponse(items, PaginationMeta(page, size, total, pages(total, size))))
            }
            get("/{id}/followers") {
                call.rejectReservedUserAlias()
                val id = call.userPathId()
                if (repository.user(id, call.optionalUserId()) == null) throw notFound()
                val (page, size) = call.pageRequest()
                val (items, total) = repository.followers(id, call.optionalUserId(), page, size)
                call.respond(PublicUserListResponse(items, PaginationMeta(page, size, total, pages(total, size))))
            }
            get("/{id}/playlists") {
                call.rejectReservedUserAlias()
                val owner = repository.user(call.userPathId(), call.optionalUserId()) ?: throw notFound()
                call.respond(PublicPlaylistListResponse(owner, repository.publicPlaylists(UUID.fromString(owner.id))))
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.userId(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)

private fun io.ktor.server.application.ApplicationCall.optionalUserId(): UUID? =
    principal<JWTPrincipal>()?.payload?.subject?.let(UUID::fromString)

private fun io.ktor.server.application.ApplicationCall.userPathId(): UUID =
    runCatching { UUID.fromString(parameters["id"]) }.getOrElse {
        throw validation("id must be a valid UUID")
    }

private fun io.ktor.server.application.ApplicationCall.pageRequest(): Pair<Int, Int> {
    val page = request.queryParameters["page"]?.toIntOrNull() ?: 0
    val size = request.queryParameters["size"]?.toIntOrNull() ?: 20
    if (page < 0 || size !in 1..100) throw validation("page must be at least 0 and size must be between 1 and 100")
    return page to size
}

private fun io.ktor.server.application.ApplicationCall.rejectReservedUserAlias() {
    if (parameters["id"] == "me") throw validation("Use /users/me for the authenticated profile")
}

private fun pages(total: Long, size: Int) = if (total == 0L) 0 else ((total - 1) / size + 1).toInt()
private fun notFound() = ApiException(HttpStatusCode.NotFound, ErrorCode.USER_NOT_FOUND, "User does not exist")
private fun validation(message: String) = ApiException(HttpStatusCode.BadRequest, ErrorCode.VALIDATION_ERROR, message)
