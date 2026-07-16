package com.arisamtunes.catalog

import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class SharedSongPageTest {
    @Test
    fun `shared page contains player and lyrics and escapes catalog content`() {
        val page = sharedSongHtml(song(title = "A <Song>", lyrics = "line <one>\nline two"), "https://music.example")

        assertContains(page, "<audio id=\"audio\"")
        assertContains(page, "A &lt;Song&gt;")
        assertContains(page, "line &lt;one&gt;\nline two")
        assertContains(page, "https://music.example/media/covers/cover.jpg")
        assertFalse(page.contains("A <Song>"))
    }

    private fun song(title: String, lyrics: String) = SongResponse(
        id = "00000000-0000-0000-0000-000000000001",
        artistId = null,
        title = title,
        artistName = "Artist",
        album = "Album",
        albumArtist = null,
        trackNumber = null,
        discNumber = null,
        genre = null,
        durationSeconds = 120,
        bitrateKbps = null,
        sampleRateHz = null,
        channels = null,
        codec = null,
        fileFormat = "MP3",
        releaseYear = null,
        releaseDate = null,
        language = null,
        lyrics = lyrics,
        composer = null,
        producer = null,
        copyright = null,
        publisher = null,
        mood = null,
        tags = emptyList(),
        isExplicit = false,
        popularity = 0,
        playCount = 0,
        isLocal = false,
        isDemo = false,
        sourceFileName = "song.mp3",
        sourceRelativePath = "Artist/song.mp3",
        audioFileSize = null,
        coverFileName = "cover.jpg",
        coverRelativePath = "covers/cover.jpg",
        audioUrl = "http://localhost/media/audio/song.mp3",
        coverImageUrl = "http://localhost/media/covers/cover.jpg",
        artistImageUrl = null,
        albumCoverUrl = null,
        extraMetadata = JsonObject(emptyMap()),
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z",
    )
}
