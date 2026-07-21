package com.arisamtunes.seed

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class MusicDiscoveryTest {
    @Test
    fun `discovers supported audio recursively and ignores other files`() {
        val root = Files.createTempDirectory("arisam-music-discovery")
        Files.createDirectories(root.resolve("artist/album"))
        Files.write(root.resolve("track.MP3"), byteArrayOf(1))
        Files.write(root.resolve("artist/album/song.m4a"), byteArrayOf(2))
        Files.writeString(root.resolve("metadata.json"), "{}")

        val result = MusicDiscovery(root).discover()

        assertEquals(2, result.size)
        assertEquals(listOf("artist/album/song.m4a", "track.MP3"), result.map { it.relativePath.toString() })
    }

    @Test
    fun `deduplicates identical audio content without dropping same-sized unique files`() {
        val root = Files.createTempDirectory("arisam-music-deduplication")
        val original = root.resolve("original.mp3")
        val duplicate = root.resolve("duplicate.mp3")
        val sameSizeButUnique = root.resolve("unique.mp3")
        Files.write(original, byteArrayOf(1, 2, 3, 4))
        Files.write(duplicate, byteArrayOf(1, 2, 3, 4))
        Files.write(sameSizeButUnique, byteArrayOf(4, 3, 2, 1))

        val result = MusicContentDeduplicator().deduplicate(MusicDiscovery(root).discover())

        assertEquals(listOf("duplicate.mp3", "unique.mp3"), result.unique.map { it.relativePath.toString() })
        assertEquals(1, result.duplicates.size)
        assertEquals("original.mp3", result.duplicates.single().duplicate.relativePath.toString())
        assertEquals("duplicate.mp3", result.duplicates.single().original.relativePath.toString())
    }
}
