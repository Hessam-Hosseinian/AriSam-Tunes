package com.arisamtunes.core.design.preview

import com.arisamtunes.data.catalog.PlaylistDto
import com.arisamtunes.data.catalog.SongDto
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object PreviewCatalogData {
    const val CoverOne = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f"
    const val CoverTwo = "https://images.unsplash.com/photo-1511379938547-c1f69419868d"
    const val CoverThree = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee"

    val songs = listOf(
        song(
            id = "preview-1",
            title = "Tehran Skyline",
            artistName = "Ari Sam",
            album = "Midnight Radio",
            genre = "Pop",
            coverImageUrl = CoverOne,
            durationSeconds = 214,
            popularity = 96,
        ),
        song(
            id = "preview-2",
            title = "Silver Cassette",
            artistName = "Nexus Ensemble",
            album = "After Hours",
            genre = "Electronic",
            coverImageUrl = CoverTwo,
            durationSeconds = 188,
            popularity = 88,
        ),
        song(
            id = "preview-3",
            title = "Blue Window",
            artistName = "Samira North",
            album = "Soft Signals",
            genre = "Indie",
            coverImageUrl = CoverThree,
            durationSeconds = 241,
            popularity = 79,
        ),
    )

    val playlists = listOf(
        PlaylistDto(
            id = "playlist-preview-1",
            name = "Late Night Drive",
            description = "Warm synths and road songs",
            coverImageUrl = CoverOne,
            scope = "GLOBAL",
            isPublic = true,
            songCount = 42,
        ),
        PlaylistDto(
            id = "playlist-preview-2",
            name = "Persian Classics",
            description = "Golden voices for quiet evenings",
            coverImageUrl = CoverThree,
            scope = "USER",
            isPublic = false,
            songCount = 27,
        ),
    )

    fun song(
        id: String,
        title: String,
        artistName: String,
        album: String,
        genre: String,
        coverImageUrl: String,
        durationSeconds: Int,
        popularity: Int,
    ) = SongDto(
        id = id,
        title = title,
        artistName = artistName,
        album = album,
        genre = genre,
        durationSeconds = durationSeconds,
        audioUrl = "https://example.com/$id.mp3",
        coverImageUrl = coverImageUrl,
        releaseYear = 2026,
        language = "fa",
        lyrics = """
            [00:04.000]Every light becomes a rhythm
            [00:14.000]Every street remembers home
            [00:25.000]We keep dancing through the static
            [00:36.000]Till the morning finds the tone
        """.trimIndent(),
        tags = listOf("preview", "featured"),
        popularity = popularity,
        playCount = popularity * 1_000L,
        extraMetadata = JsonObject(
            mapOf(
                "synced_lyrics_lrc" to JsonPrimitive(
                    """
                        [00:04.000]Every light becomes a rhythm
                        [00:14.000]Every street remembers home
                        [00:25.000]We keep dancing through the static
                        [00:36.000]Till the morning finds the tone
                    """.trimIndent(),
                ),
            ),
        ),
    )
}
