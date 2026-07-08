package com.arisamtunes.feature.player

import com.arisamtunes.data.catalog.SongDto
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerBehaviorTest {
    private val first = song("1", durationSeconds = 120)
    private val second = song("2")
    private val third = song("3")

    @Test
    fun normalizeQueue_placesCurrentFirstAndRemovesDuplicates() {
        assertEquals(
            listOf("2", "1", "3"),
            listOf(first, second, first, third).normalizeQueue(second).map(SongDto::id),
        )
    }

    @Test
    fun shuffleOff_restoresQueueFromCurrentPlaybackSession() {
        val repository = PlayerStateRepository()
        repository.play(second, listOf(first, second, third), replaceQueue = true)

        repository.toggleShuffle()
        repository.toggleShuffle()

        assertEquals(listOf("2", "1", "3"), repository.state.value.queue.map(SongDto::id))
        assertEquals(listOf("2", "1", "3"), repository.state.value.originalQueue.map(SongDto::id))
    }

    @Test
    fun explicitNewQueue_replacesOldShuffleBaseline() {
        val repository = PlayerStateRepository()
        repository.play(first, listOf(first, second), replaceQueue = true)
        repository.toggleShuffle()
        repository.play(third, listOf(third, second), replaceQueue = true)

        repository.toggleShuffle()
        repository.toggleShuffle()

        assertEquals(listOf("3", "2"), repository.state.value.queue.map(SongDto::id))
    }

    @Test
    fun repeatAndQueueBoundaries_areRespected() {
        val queue = listOf(first, second, third)

        assertNull(queue.nextFor(third, repeatMode = 0))
        assertEquals(first, queue.nextFor(third, repeatMode = 1))
        assertEquals(second, queue.nextFor(second, repeatMode = 2))
        assertNull(queue.previousFor(first, repeatMode = 0))
        assertEquals(third, queue.previousFor(first, repeatMode = 1))
        assertEquals(first, queue.previousFor(first, repeatMode = 2))
    }

    @Test
    fun progressAndSeek_areClampedWithoutOverflow() {
        val repository = PlayerStateRepository()
        repository.play(first, listOf(first), replaceQueue = true)

        repository.setProgressMillis(Long.MAX_VALUE)
        assertEquals(120_000L, repository.state.value.progressMillis)
        assertEquals(120, repository.state.value.progressSeconds)

        repository.seekTo(-20)
        assertEquals(0L, repository.state.value.progressMillis)
    }

    @Test
    fun syncedLyricsJson_acceptsArrayAndWrappedMillisecondShapes() {
        val array = parseSyncedLyricsJson("""[{"time":1.5,"text":"One"}]""")
        val wrapped = parseSyncedLyricsJson(
            """{"lines":[{"startTimeMs":2500,"words":"Two"}]}""",
        )

        assertEquals(TimedLyricLine(1_500L, "One"), array.single())
        assertEquals(TimedLyricLine(2_500L, "Two"), wrapped.single())
    }

    @Test
    fun lrcLyrics_areParsedAndSorted() {
        val parsed = parseTimedLyrics(
            song("lyrics").copy(lyrics = "[00:02.50]Second\n[00:01.250]First"),
        )

        assertEquals(listOf("First", "Second"), parsed.map(TimedLyricLine::text))
        assertEquals(listOf(1_250L, 2_500L), parsed.map(TimedLyricLine::startMillis))
    }

    private fun song(id: String, durationSeconds: Int = 180) = SongDto(
        id = id,
        title = "Song $id",
        artistName = "Artist",
        durationSeconds = durationSeconds,
        audioUrl = "https://example.com/$id.mp3",
        coverImageUrl = "https://example.com/$id.jpg",
        extraMetadata = JsonObject(emptyMap()),
    )
}
