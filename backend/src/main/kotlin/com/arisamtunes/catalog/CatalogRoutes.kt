package com.arisamtunes.catalog

import com.arisamtunes.model.ApiException
import com.arisamtunes.model.ErrorCode
import com.arisamtunes.model.PaginationMeta
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.util.UUID

fun Route.catalogRoutes(
    repository: CatalogRepository = CatalogRepository(),
    spectrumRepository: SpectrumRepository = SpectrumRepository(),
) {
    route("/songs") {
        get {
            val (page, size) = call.pageRequest()
            val (items, total) = repository.songs(page, size)
            call.respond(SongPageResponse(items, pagination(page, size, total)))
        }
        get("/trending") { call.respond(repository.featured("trending")) }
        get("/new") { call.respond(repository.featured("new")) }
        get("/popular") { call.respond(repository.featured("popular")) }
        get("/search") {
            val query = call.request.queryParameters["q"]?.trim().orEmpty()
            val type = call.request.queryParameters["type"]?.lowercase()?.trim().orEmpty().ifBlank { "all" }
            if (query.isBlank() || query.length > 200 || type !in setOf("all", "title", "artist", "album", "genre")) {
                throw ApiException(HttpStatusCode.BadRequest, ErrorCode.VALIDATION_ERROR, "q is required and type must be all, title, artist, album, or genre")
            }
            val (page, size) = call.pageRequest()
            val (items, total) = repository.search(query, type, page, size)
            call.respond(SongPageResponse(items, pagination(page, size, total)))
        }
        get("/{id}") {
            val id = call.uuidParameter("id")
            call.respond(repository.song(id) ?: throw ApiException(HttpStatusCode.NotFound, ErrorCode.SONG_NOT_FOUND, "Song does not exist"))
        }
        get("/{id}/spectrum") {
            val id = call.uuidParameter("id")
            if (repository.song(id) == null) throw ApiException(HttpStatusCode.NotFound, ErrorCode.SONG_NOT_FOUND, "Song does not exist")
            call.respond(spectrumRepository.spectrum(id) ?: SongSpectrumResponse(id.toString(), bands = 24, frameDurationMs = 100, frames = emptyList()))
        }
    }
    route("/artists") {
        get { call.respond(ArtistListResponse(repository.artists())) }
        get("/{id}") {
            val id = call.uuidParameter("id")
            call.respond(repository.artist(id) ?: throw ApiException(HttpStatusCode.NotFound, ErrorCode.ARTIST_NOT_FOUND, "Artist does not exist"))
        }
        get("/{id}/songs") {
            val id = call.uuidParameter("id")
            if (repository.artist(id) == null) throw ApiException(HttpStatusCode.NotFound, ErrorCode.ARTIST_NOT_FOUND, "Artist does not exist")
            val (page, size) = call.pageRequest()
            val (items, total) = repository.artistSongs(id, page, size)
            call.respond(SongPageResponse(items, pagination(page, size, total)))
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.pageRequest(): Pair<Int, Int> {
    val page = request.queryParameters["page"]?.toIntOrNull() ?: 0
    val size = request.queryParameters["size"]?.toIntOrNull() ?: 20
    if (page < 0 || size !in 1..100) throw ApiException(HttpStatusCode.BadRequest, ErrorCode.VALIDATION_ERROR, "page must be at least 0 and size must be between 1 and 100")
    return page to size
}

private fun io.ktor.server.application.ApplicationCall.uuidParameter(name: String): UUID =
    runCatching { UUID.fromString(parameters[name]) }.getOrElse {
        throw ApiException(HttpStatusCode.BadRequest, ErrorCode.VALIDATION_ERROR, "$name must be a valid UUID")
    }

private fun pagination(page: Int, size: Int, total: Long) = PaginationMeta(
    page = page, size = size, totalItems = total, totalPages = if (total == 0L) 0 else ((total - 1) / size + 1).toInt(),
)
