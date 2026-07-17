package com.arisamtunes.feature.suggestions

import com.arisamtunes.data.catalog.SongDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicSuggestionRankerTest {
    @Test
    fun `selected songs stay first and similar metadata outranks popularity`() {
        val seed = song("seed", "A", genre = "rock", language = "en", tags = listOf("guitar"))
        val similar = song("similar", "B", genre = "rock", language = "en", tags = listOf("guitar"), popularity = 20)
        val unrelated = song("popular", "C", genre = "pop", language = "fa", popularity = 100)

        val result = MusicSuggestionRanker.buildPlaylist(
            allSongs = listOf(seed, unrelated, similar),
            selectedSongIds = setOf(seed.id),
            targetSize = 3,
        )

        assertEquals(listOf("seed", "similar", "popular"), result.map(SongDto::id))
    }

    @Test
    fun `recommendations are diversified across artists`() {
        val seed = song("seed", "Seed", genre = "pop")
        val sameArtist = (1..6).map { song("a$it", "A", genre = "pop", popularity = 100 - it) }
        val alternatives = (1..4).map { song("b$it", "B$it", genre = "pop", popularity = 50 - it) }

        val result = MusicSuggestionRanker.buildPlaylist(
            allSongs = listOf(seed) + sameArtist + alternatives,
            selectedSongIds = setOf(seed.id),
            targetSize = 8,
        )

        assertTrue(result.count { it.artistName == "A" } <= 4)
        assertEquals(result.size, result.distinctBy(SongDto::id).size)
    }

    private fun song(
        id: String,
        artist: String,
        genre: String? = null,
        language: String? = null,
        tags: List<String> = emptyList(),
        popularity: Int = 0,
    ) = SongDto(
        id = id,
        title = id,
        artistName = artist,
        genre = genre,
        language = language,
        tags = tags,
        popularity = popularity,
        audioUrl = "https://example.com/$id.mp3",
        coverImageUrl = "https://example.com/$id.jpg",
    )
}
