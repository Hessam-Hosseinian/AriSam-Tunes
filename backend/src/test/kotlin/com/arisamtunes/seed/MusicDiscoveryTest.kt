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
}
