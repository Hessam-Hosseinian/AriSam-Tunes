package com.arisamtunes.catalog

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.nio.file.Path
import kotlin.io.path.isRegularFile

fun Application.configureMediaStreaming() {
    install(PartialContent)
}

fun Route.mediaRoutes(musicRoot: Path = Path.of(System.getenv("MUSIC_DATA_FOLDER") ?: "music_data")) {
    val root = musicRoot.toAbsolutePath().normalize()
    route("/media") {
        get("/audio/{path...}") { call.respondMedia(root, call.parameters.getAll("path")?.joinToString("/")) }
        get("/covers/{path...}") { call.respondMedia(root.resolve("covers"), call.parameters.getAll("path")?.joinToString("/")) }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondMedia(root: Path, requestedPath: String?) {
    if (requestedPath.isNullOrBlank()) {
        respond(HttpStatusCode.NotFound)
        return
    }
    val normalizedRoot = root.toAbsolutePath().normalize()
    val file = normalizedRoot.resolve(requestedPath).normalize()
    if (!file.startsWith(normalizedRoot) || !file.isRegularFile()) {
        respond(HttpStatusCode.NotFound)
        return
    }
    respondFile(file.toFile())
}
