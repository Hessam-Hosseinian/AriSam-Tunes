package com.arisamtunes.data.local

import com.arisamtunes.data.catalog.SongDto
import java.io.File
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFirstPlaybackTest {
    @Test
    fun validOfflineFile_isPreferredOverNetworkStream() {
        val localFile = File.createTempFile("arisam-local-first", ".audio")
        try {
            val source = selectPlaybackSource(NetworkUrl, localFile.absolutePath)

            assertTrue(source.startsWith("file:"))
            assertEquals(localFile.toURI().toString(), source)
        } finally {
            localFile.delete()
        }
    }

    @Test
    fun missingOfflineFile_fallsBackToFullNetworkStream() {
        val source = selectPlaybackSource(NetworkUrl, "/missing/arisam-song.audio")

        assertEquals(NetworkUrl, source)
    }

    @Test
    fun absentOfflineRecord_usesNetworkStreamWithoutPremiumGate() {
        val source = selectPlaybackSource(NetworkUrl, completedPath = null)

        assertEquals(NetworkUrl, source)
    }

    @Test
    fun downloadedSong_roundTripPreservesDurationLyricsAndSyncedMetadata() {
        val song = SongDto(
            id = "song-with-lyrics",
            title = "Song",
            artistName = "Artist",
            durationSeconds = 245,
            audioUrl = NetworkUrl,
            coverImageUrl = "https://example.com/cover.jpg",
            lyrics = "[00:01.00]First line",
            extraMetadata = JsonObject(mapOf("synced_lyrics_lrc" to JsonPrimitive("[00:01.00]First line"))),
        )

        val entity = song.toDownloadedSongEntity(
            ownerUserId = "account-a",
            localFilePath = "/downloads/song.audio",
            state = LocalLibraryRepository.DownloadStateCompleted,
        )
        val restored = entity.toSongDto()

        assertEquals("account-a", entity.ownerUserId)
        assertEquals(245, restored.durationSeconds)
        assertEquals(song.lyrics, restored.lyrics)
        assertEquals(song.extraMetadata, restored.extraMetadata)
    }

    private companion object {
        const val NetworkUrl = "https://example.com/full-song.mp3"
    }
}
