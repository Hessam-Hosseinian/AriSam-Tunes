package com.arisamtunes.seed

import com.mpatric.mp3agic.Mp3File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.nameWithoutExtension

class MetadataExtractor(
    private val musicRoot: Path = configuredMusicRoot(),
    private val sidecars: SidecarReader = SidecarReader(),
) {
    fun extract(source: DiscoveredAudio): ExtractedSong {
        val ffprobe = probe(source.absolutePath)
        val mp3 = if (source.extension == "mp3") runCatching { Mp3File(source.absolutePath.toString()) }.getOrNull() else null
        val mp3Tag = mp3?.let { if (it.hasId3v2Tag()) it.id3v2Tag else if (it.hasId3v1Tag()) it.id3v1Tag else null }
        val embedded = ffprobe.tags.toMutableMap().apply {
            mapOf(
                "title" to mp3Tag?.title, "artist" to mp3Tag?.artist, "album" to mp3Tag?.album,
                "track" to mp3Tag?.track, "year" to mp3Tag?.year, "genre" to mp3Tag?.genreDescription,
                "comment" to mp3Tag?.comment,
            ).forEach { (key, value) -> value.clean()?.let { put(key, it) } }
            mp3?.id3v2Tag?.let { tag ->
                mapOf("composer" to tag.composer, "lyrics" to tag.lyrics).forEach { (key, value) -> value.clean()?.let { put(key, it) } }
            }
        }
        val sidecar = sidecars.read(source)
        val fallback = linkedSetOf<String>()
        fun value(vararg keys: String): String? = keys.firstNotNullOfOrNull { embedded[it.lowercase()].clean() }
            ?: keys.firstNotNullOfOrNull { sidecar[it.lowercase()].clean() }

        var title = value("title")
        var artist = value("artist", "artist_name")
        var album = value("album")
        if (album == null && source.relativePath.nameCount >= 3) { album = source.relativePath.parent.fileName.toString(); fallback += "album" }
        if (artist == null && source.relativePath.nameCount >= 3) { artist = source.relativePath.parent.parent.fileName.toString(); fallback += "artist" }
        val fileMatch = Regex("^\\s*(\\d+)\\s*[-–.]\\s*(.+)$").matchEntire(source.absolutePath.fileName.nameWithoutExtension)
        if (title == null) { title = fileMatch?.groupValues?.get(2) ?: source.absolutePath.fileName.nameWithoutExtension; fallback += "title" }
        if (artist == null) { artist = "Unknown Artist"; fallback += "artist" }
        val track = value("track", "track_number")?.substringBefore('/')?.toIntOrNull()
            ?: fileMatch?.groupValues?.get(1)?.toIntOrNull()?.also { fallback += "track_number" }
        val cover = extractCover(mp3, source, title)

        return ExtractedSong(
            source = source, title = title, artist = artist, albumArtist = value("album_artist", "albumartist"), album = album,
            trackNumber = track, discNumber = value("disc", "disc_number")?.substringBefore('/')?.toIntOrNull(),
            year = value("date", "year", "release_year")?.take(4)?.toIntOrNull(), genre = value("genre"), comment = value("comment"),
            composer = value("composer"), lyrics = value("lyrics"), language = value("language"), mood = value("mood"),
            bpm = value("bpm")?.toIntOrNull(), musicalKey = value("key"), grouping = value("grouping"),
            publisher = value("publisher", "label", "record_label"), copyright = value("copyright"), encoder = value("encoder"),
            officialArtistUrl = value("url", "artist_url"), durationSeconds = mp3?.lengthInSeconds?.toInt() ?: ffprobe.durationSeconds,
            bitrateKbps = mp3?.bitrate ?: ffprobe.bitrateKbps, sampleRateHz = mp3?.sampleRate ?: ffprobe.sampleRateHz,
            channels = mp3?.channelMode ?: ffprobe.channels, codec = ffprobe.codec,
            coverFileName = cover.first, coverSource = cover.second, usedFallbackFields = fallback,
            extraMetadata = buildJsonObject {
                embedded.forEach { (key, content) -> put(key, JsonPrimitive(content)) }
                sidecar.forEach { (key, content) -> if (key !in embedded) put(key, JsonPrimitive(content)) }
            },
        )
    }

    private fun probe(path: Path): ProbeResult {
        val process = ProcessBuilder(
            "ffprobe", "-v", "error", "-show_entries", "format=format_name,duration,bit_rate:format_tags:stream=codec_name,sample_rate,channels",
            "-of", "json", path.toString(),
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "ffprobe failed for ${path.fileName}: $output" }
        val root = Json.parseToJsonElement(output) as JsonObject
        val format = root["format"] as? JsonObject
        val stream = (root["streams"] as? kotlinx.serialization.json.JsonArray)?.firstOrNull() as? JsonObject
        val tags = (format?.get("tags") as? JsonObject).orEmpty().mapValues { (_, value) -> (value as JsonPrimitive).content }.mapKeys { it.key.lowercase() }
        fun JsonObject?.text(key: String) = (this?.get(key) as? JsonPrimitive)?.content
        return ProbeResult(
            tags, format.text("duration")?.toDoubleOrNull()?.toInt() ?: 0,
            format.text("bit_rate")?.toLongOrNull()?.div(1000)?.toInt(), stream.text("sample_rate")?.toIntOrNull(),
            stream.text("channels"), stream.text("codec_name") ?: format.text("format_name"),
        )
    }

    private fun extractCover(mp3: Mp3File?, source: DiscoveredAudio, title: String): Pair<String?, CoverSource> {
        val data = mp3?.id3v2Tag?.albumImage
        if (data != null) return saveCover(data, mp3.id3v2Tag.albumImageMimeType, source, title) to CoverSource.EMBEDDED
        val name = "${sanitize(title)}_${shortHash(source.relativePath.toString())}_cover.jpg"
        val target = musicRoot.resolve("covers").resolve(name)
        Files.createDirectories(target.parent)
        val extracted = runCatching {
            ProcessBuilder("ffmpeg", "-v", "error", "-y", "-i", source.absolutePath.toString(), "-map", "0:v:0", "-frames:v", "1", target.toString())
                .redirectErrorStream(true).start().let { it.inputStream.readAllBytes(); it.waitFor() == 0 && Files.size(target) > 0 }
        }.getOrDefault(false)
        if (extracted) return name to CoverSource.EMBEDDED else Files.deleteIfExists(target)
        val folder = listOf("cover.jpg", "folder.jpg", "album.jpg", "front.jpg", "artwork.jpg", "cover.png", "folder.png")
            .map(source.absolutePath.parent::resolve).firstOrNull(Files::isRegularFile)
        return if (folder != null) folder.fileName.toString() to CoverSource.FOLDER else null to CoverSource.NONE
    }

    private fun saveCover(data: ByteArray, mime: String?, source: DiscoveredAudio, title: String): String {
        val extension = if (mime?.contains("png", true) == true) "png" else "jpg"
        val name = "${sanitize(title)}_${shortHash(source.relativePath.toString())}_cover.$extension"
        val target = musicRoot.resolve("covers").resolve(name); Files.createDirectories(target.parent); Files.write(target, data); return name
    }
    private fun String?.clean() = this?.trim()?.takeIf(String::isNotEmpty)
    private fun sanitize(value: String) = value.replace(Regex("[^\\p{L}\\p{N}._-]+"), "_").take(80).ifBlank { "track" }
    private fun shortHash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).take(4).joinToString("") { "%02x".format(it) }
}

private data class ProbeResult(
    val tags: Map<String, String>, val durationSeconds: Int, val bitrateKbps: Int?,
    val sampleRateHz: Int?, val channels: String?, val codec: String?,
)
