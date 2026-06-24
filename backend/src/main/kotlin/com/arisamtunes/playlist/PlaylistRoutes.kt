package com.arisamtunes.playlist

import com.arisamtunes.model.ApiException
import com.arisamtunes.model.ErrorCode
import com.arisamtunes.model.PaginationMeta
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID

fun Route.playlistRoutes(repository: PlaylistRepository = PlaylistRepository()) {
    route("/playlists") {
        get("/global") { call.respond(PlaylistListResponse(repository.byScope(PlaylistScope.GLOBAL))) }
        get("/local") { call.respond(PlaylistListResponse(repository.byScope(PlaylistScope.LOCAL))) }
        authenticate("auth-jwt", optional = true) {
            get("/{id}") {
                val playlist = repository.findVisible(call.playlistId(), call.optionalUserId())
                    ?: throw notFound()
                call.respond(playlist)
            }
            get("/{id}/songs") {
                val id = call.playlistId()
                val playlist = repository.findVisible(id, call.optionalUserId()) ?: throw notFound()
                val (page, size) = call.pageRequest()
                val (items, total) = repository.songs(id, page, size)
                call.respond(PlaylistSongsResponse(playlist, items, PaginationMeta(page, size, total, pages(total, size))))
            }
        }
        authenticate("auth-jwt") {
            get { call.respond(PlaylistListResponse(repository.visibleTo(call.userId()))) }
            post {
                val request = call.receive<CreatePlaylistRequest>().validated()
                call.respond(HttpStatusCode.Created, repository.create(call.userId(), request))
            }
            put("/{id}") {
                val request = call.receive<UpdatePlaylistRequest>().validated()
                call.respond(repository.update(call.playlistId(), call.userId(), request) ?: throw notFound())
            }
            delete("/{id}") {
                if (!repository.delete(call.playlistId(), call.userId())) throw notFound()
                call.respond(HttpStatusCode.NoContent)
            }
            post("/{id}/songs") {
                val request = call.receive<PlaylistSongRequest>().validated()
                call.respond(repository.addSong(call.playlistId(), call.userId(), UUID.fromString(request.songId)) ?: throw notFound())
            }
            delete("/{id}/songs/{songId}") {
                call.respond(repository.removeSong(call.playlistId(), call.userId(), call.songId()) ?: throw notFound())
            }
        }
    }
}

private fun CreatePlaylistRequest.validated(): CreatePlaylistRequest {
    validateFields(name, description, coverImageUrl)
    return this
}
private fun UpdatePlaylistRequest.validated(): UpdatePlaylistRequest {
    validateFields(name, description, coverImageUrl)
    return this
}
private fun PlaylistSongRequest.validated(): PlaylistSongRequest {
    runCatching { UUID.fromString(songId) }.getOrElse {
        throw ApiException(HttpStatusCode.BadRequest, ErrorCode.VALIDATION_ERROR, "songId must be a valid UUID")
    }
    return this
}
private fun validateFields(name: String, description: String?, coverUrl: String?) {
    if (name.isBlank() || name.length > 255 || (description?.length ?: 0) > 2000 || !validUrl(coverUrl))
        throw ApiException(HttpStatusCode.BadRequest, ErrorCode.VALIDATION_ERROR, "Provide a valid playlist name, description, and cover URL")
}
private fun validUrl(value: String?) = value.isNullOrBlank() || value.startsWith("http://") || value.startsWith("https://")
private fun notFound() = ApiException(HttpStatusCode.NotFound, ErrorCode.PLAYLIST_NOT_FOUND, "Playlist does not exist")
private fun pages(total: Long, size: Int) = if (total == 0L) 0 else ((total - 1) / size + 1).toInt()

private fun io.ktor.server.application.ApplicationCall.userId() =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)
private fun io.ktor.server.application.ApplicationCall.optionalUserId() =
    principal<JWTPrincipal>()?.payload?.subject?.let(UUID::fromString)
private fun io.ktor.server.application.ApplicationCall.playlistId() =
    runCatching { UUID.fromString(parameters["id"]) }.getOrElse {
        throw ApiException(HttpStatusCode.BadRequest, ErrorCode.VALIDATION_ERROR, "id must be a valid UUID")
    }
private fun io.ktor.server.application.ApplicationCall.songId() =
    runCatching { UUID.fromString(parameters["songId"]) }.getOrElse {
        throw ApiException(HttpStatusCode.BadRequest, ErrorCode.VALIDATION_ERROR, "songId must be a valid UUID")
    }
private fun io.ktor.server.application.ApplicationCall.pageRequest(): Pair<Int, Int> {
    val page = request.queryParameters["page"]?.toIntOrNull() ?: 0
    val size = request.queryParameters["size"]?.toIntOrNull() ?: 20
    if (page < 0 || size !in 1..100) throw ApiException(HttpStatusCode.BadRequest, ErrorCode.VALIDATION_ERROR, "page must be at least 0 and size must be between 1 and 100")
    return page to size
}
