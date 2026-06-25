package com.arisamtunes.seed

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

private val SupportedAudioExtensions = setOf("mp3", "m4a", "aac", "flac", "ogg", "wav", "wma", "aiff", "aif")

data class DiscoveredAudio(
    val absolutePath: Path,
    val relativePath: Path,
    val extension: String,
    val sizeBytes: Long,
)

class MusicDiscovery(private val root: Path = configuredMusicRoot()) {
    fun discover(): List<DiscoveredAudio> {
        if (!Files.exists(root)) return emptyList()
        return Files.walk(root).use { paths ->
            paths.filter(Path::isRegularFile)
                .filter { it.extension.lowercase() in SupportedAudioExtensions }
                .map { path ->
                    DiscoveredAudio(
                        absolutePath = path.toAbsolutePath().normalize(),
                        relativePath = root.toAbsolutePath().normalize().relativize(path.toAbsolutePath().normalize()),
                        extension = path.extension.lowercase(),
                        sizeBytes = Files.size(path),
                    )
                }
                .sorted(compareBy { it.relativePath.toString().lowercase() })
                .toList()
        }
    }
}

fun configuredMusicRoot(): Path = Paths.get(System.getenv("MUSIC_DATA_FOLDER") ?: "music_data")
    .toAbsolutePath()
    .normalize()
