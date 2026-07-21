package com.arisamtunes.seed

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
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

data class DuplicateAudio(
    val original: DiscoveredAudio,
    val duplicate: DiscoveredAudio,
)

data class DeduplicatedAudio(
    val unique: List<DiscoveredAudio>,
    val duplicates: List<DuplicateAudio>,
)

class MusicContentDeduplicator {
    fun deduplicate(sources: List<DiscoveredAudio>): DeduplicatedAudio {
        val duplicates = mutableListOf<DuplicateAudio>()
        val duplicatePaths = mutableSetOf<Path>()

        sources.groupBy(DiscoveredAudio::sizeBytes)
            .values
            .filter { candidates -> candidates.size > 1 }
            .forEach { candidates ->
                val originalsByHash = mutableMapOf<String, DiscoveredAudio>()
                candidates.forEach { source ->
                    val hash = sha256(source.absolutePath)
                    val original = originalsByHash.putIfAbsent(hash, source)
                    if (original != null) {
                        duplicates += DuplicateAudio(original = original, duplicate = source)
                        duplicatePaths.add(source.absolutePath)
                    }
                }
            }

        return DeduplicatedAudio(
            unique = sources.filterNot { source -> source.absolutePath in duplicatePaths },
            duplicates = duplicates,
        )
    }

    private fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }
}

fun configuredMusicRoot(): Path = Paths.get(System.getenv("MUSIC_DATA_FOLDER") ?: "music_data")
    .toAbsolutePath()
    .normalize()
