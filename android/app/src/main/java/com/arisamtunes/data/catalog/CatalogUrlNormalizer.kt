package com.arisamtunes.data.catalog

import com.arisamtunes.BuildConfig

/**
 * Seeded catalog rows can contain media URLs generated for the backend host
 * (for example localhost). A physical Android device cannot resolve the
 * developer PC through localhost, so playback and artwork must use the same
 * configured API host as the Ktor client.
 */
internal object CatalogUrlNormalizer {
    private val apiBase = BuildConfig.API_BASE_URL.trimEnd('/')
    private val apiOrigin = apiBase.substringBefore("/api/v1").trimEnd('/')
    private val localHosts = setOf("localhost", "127.0.0.1", "0.0.0.0", "10.0.2.2")

    fun song(song: SongDto): SongDto = song.copy(
        audioUrl = mediaUrl(song.audioUrl),
        coverImageUrl = mediaUrl(song.coverImageUrl),
        artistImageUrl = song.artistImageUrl?.let(::mediaUrl),
        albumCoverUrl = song.albumCoverUrl?.let(::mediaUrl),
    )

    fun playlist(playlist: PlaylistDto): PlaylistDto = playlist.copy(
        coverImageUrl = playlist.coverImageUrl?.let(::mediaUrl),
    )

    private fun mediaUrl(value: String): String {
        val uri = runCatching { java.net.URI(value) }.getOrNull() ?: return value
        val host = uri.host?.lowercase() ?: return value
        if (host !in localHosts) return value
        val rawPath = uri.rawPath ?: return value
        val query = uri.rawQuery?.let { "?$it" }.orEmpty()
        return "$apiOrigin$rawPath$query"
    }
}
