package com.arisamtunes.seed

import kotlinx.serialization.json.Json
import org.postgresql.util.PGobject
import java.net.URLEncoder
import java.nio.file.Files
import java.sql.Connection
import java.sql.Types
import java.util.UUID
import javax.sql.DataSource
import kotlin.io.path.nameWithoutExtension

class MusicSeeder(
    private val dataSource: DataSource,
    private val publicBaseUrl: String,
    private val discovery: MusicDiscovery = MusicDiscovery(),
    private val extractor: MetadataExtractor = MetadataExtractor(),
) {
    fun seed() {
        val discovered = discovery.discover()
        val deduplicated = MusicContentDeduplicator().deduplicate(discovered)
        val sources = deduplicated.unique
        val summary = SeedSummary(
            totalFound = discovered.size,
            duplicatesSkipped = deduplicated.duplicates.size,
        )
        deduplicated.duplicates.forEach { duplicate ->
            println("⚠️ Duplicate skipped: ${duplicate.duplicate.relativePath} | same content as ${duplicate.original.relativePath}")
        }
        val extracted = sources.mapNotNull { source ->
            runCatching { extractor.extract(source) }
                .onSuccess { song ->
                    if (song.usedFallbackFields.isEmpty()) summary.fullyExtracted++ else summary.partiallyExtracted++
                    when (song.coverSource) { CoverSource.EMBEDDED -> summary.embeddedCovers++; CoverSource.FOLDER -> summary.folderCovers++; CoverSource.NONE -> summary.noCovers++ }
                    println("✅ Extracted: ${source.relativePath} | title=${song.title} | artist=${song.artist} | album=${song.album.orEmpty()} | duration=${song.durationSeconds}s | cover=${song.coverSource.name.lowercase()}")
                    if (song.usedFallbackFields.isNotEmpty()) println("⚠️ Partial: ${source.relativePath} | missing fields used fallback: ${song.usedFallbackFields.joinToString()}")
                }
                .onFailure { error -> summary.failed++; println("❌ Failed: ${source.relativePath} | error=${error.message}") }
                .getOrNull()
        }.toMutableList()
        if (extracted.size < 50) extracted += generateDemos(50 - extracted.size, summary)

        dataSource.connection.use { connection ->
            try {
                removeDuplicateSeedRows(connection, deduplicated.duplicates)
                extracted.forEach { upsert(connection, it) }
                connection.commit()
            }
            catch (error: Throwable) { connection.rollback(); throw error }
        }
        printSummary(summary, extracted.size)
    }

    private fun removeDuplicateSeedRows(connection: Connection, duplicates: List<DuplicateAudio>) {
        if (duplicates.isEmpty()) return
        connection.prepareStatement("DELETE FROM songs WHERE source_relative_path = ?").use { statement ->
            duplicates.forEach { duplicate ->
                statement.setString(1, duplicate.duplicate.relativePath.toString())
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun generateDemos(count: Int, summary: SeedSummary): List<ExtractedSong> {
        if (count == 0) return emptyList()
        val root = configuredMusicRoot().resolve("demo"); Files.createDirectories(root)
        return (1..count).map { index ->
            val path = root.resolve("demo_track_${index.toString().padStart(2, '0')}.mp3")
            if (!Files.exists(path)) {
                val process = ProcessBuilder("ffmpeg", "-v", "error", "-f", "lavfi", "-i", "anullsrc=r=44100:cl=stereo", "-t", "2", "-q:a", "9", "-y", path.toString()).start()
                check(process.waitFor() == 0) { "Could not generate demo audio $path" }
            }
            summary.demoGenerated++; summary.noCovers++
            val source = DiscoveredAudio(path.toAbsolutePath(), configuredMusicRoot().relativize(path), "mp3", Files.size(path))
            ExtractedSong(source, "Demo Track ${index.toString().padStart(2, '0')}", "AriSam Demo", durationSeconds = 2, usedFallbackFields = setOf("demo"), isDemo = true)
        }
    }

    private fun upsert(connection: Connection, song: ExtractedSong) {
        val artistId = artistId(connection, song.artist)
        val sql = """INSERT INTO songs(
            artist_id,title,artist_name,album,album_artist,track_number,disc_number,genre,duration_seconds,bitrate_kbps,sample_rate_hz,channels,codec,file_format,
            release_year,language,lyrics,composer,copyright,publisher,mood,is_demo,source_file_name,source_relative_path,audio_file_size,cover_file_name,cover_relative_path,
            audio_url,cover_image_url,extra_metadata,updated_at
        ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())
        ON CONFLICT (source_relative_path) DO UPDATE SET
            artist_id=EXCLUDED.artist_id,title=EXCLUDED.title,artist_name=EXCLUDED.artist_name,album=EXCLUDED.album,album_artist=EXCLUDED.album_artist,
            track_number=EXCLUDED.track_number,disc_number=EXCLUDED.disc_number,genre=EXCLUDED.genre,duration_seconds=EXCLUDED.duration_seconds,
            bitrate_kbps=EXCLUDED.bitrate_kbps,sample_rate_hz=EXCLUDED.sample_rate_hz,channels=EXCLUDED.channels,codec=EXCLUDED.codec,file_format=EXCLUDED.file_format,
            release_year=EXCLUDED.release_year,language=EXCLUDED.language,lyrics=EXCLUDED.lyrics,composer=EXCLUDED.composer,copyright=EXCLUDED.copyright,
            publisher=EXCLUDED.publisher,mood=EXCLUDED.mood,is_demo=EXCLUDED.is_demo,audio_file_size=EXCLUDED.audio_file_size,cover_file_name=EXCLUDED.cover_file_name,
            cover_relative_path=EXCLUDED.cover_relative_path,audio_url=EXCLUDED.audio_url,cover_image_url=EXCLUDED.cover_image_url,extra_metadata=EXCLUDED.extra_metadata,updated_at=NOW()"""
        connection.prepareStatement(sql).use { statement ->
            var i = 1
            fun set(value: Any?, type: Int = Types.VARCHAR) { if (value == null) statement.setNull(i++, type) else statement.setObject(i++, value) }
            set(artistId, Types.OTHER); set(song.title); set(song.artist); set(song.album); set(song.albumArtist); set(song.trackNumber, Types.INTEGER); set(song.discNumber, Types.INTEGER)
            set(song.genre); set(song.durationSeconds); set(song.bitrateKbps, Types.INTEGER); set(song.sampleRateHz, Types.INTEGER); set(song.channels); set(song.codec); set(song.source.extension.uppercase())
            set(song.year, Types.INTEGER); set(song.language); set(song.lyrics); set(song.composer); set(song.copyright); set(song.publisher); set(song.mood); set(song.isDemo, Types.BOOLEAN)
            set(song.source.absolutePath.fileName.toString()); set(song.source.relativePath.toString()); set(song.source.sizeBytes, Types.BIGINT); set(song.coverFileName)
            set(song.coverFileName?.let { "covers/$it" }); set("$publicBaseUrl/media/audio/${encodePath(song.source.relativePath.toString())}")
            set(song.coverFileName?.let { "$publicBaseUrl/media/covers/${encodePath(it)}" } ?: "$publicBaseUrl/media/covers/default.svg")
            set(PGobject().apply { type = "jsonb"; value = Json.encodeToString(song.extraMetadata) }, Types.OTHER)
            statement.executeUpdate()
        }
    }

    private fun artistId(connection: Connection, name: String): UUID {
        connection.prepareStatement("INSERT INTO artists(name) VALUES (?) ON CONFLICT DO NOTHING").use { it.setString(1, name); it.executeUpdate() }
        return connection.prepareStatement("SELECT id FROM artists WHERE LOWER(name)=LOWER(?)").use { statement ->
            statement.setString(1, name); statement.executeQuery().use { it.next(); it.getObject(1, UUID::class.java) }
        }
    }

    private fun encodePath(value: String) = value.split('/', '\\').joinToString("/") { URLEncoder.encode(it, Charsets.UTF_8).replace("+", "%20") }
    private fun printSummary(summary: SeedSummary, total: Int) = println("""
        ════════════════════════════════════════
        SEED SUMMARY
        Total audio files found: ${summary.totalFound}
        Duplicate audio files skipped: ${summary.duplicatesSkipped}
        Successfully extracted (full tags): ${summary.fullyExtracted}
        Partially extracted (some fallback): ${summary.partiallyExtracted}
        Failed to read: ${summary.failed}
        Demo songs generated: ${summary.demoGenerated}
        Total songs seeded: $total
        Cover art source breakdown:
          Embedded art: ${summary.embeddedCovers} songs
          Folder art:   ${summary.folderCovers} songs
          No cover:     ${summary.noCovers} songs
        ════════════════════════════════════════
    """.trimIndent())
}
