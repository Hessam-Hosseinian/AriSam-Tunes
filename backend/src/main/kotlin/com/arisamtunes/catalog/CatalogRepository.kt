package com.arisamtunes.catalog

import com.arisamtunes.plugins.DatabaseProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.sql.ResultSet
import java.util.UUID

class CatalogRepository {
    fun songs(page: Int, size: Int): Pair<List<SongResponse>, Long> = DatabaseProvider.dataSource.connection.use { c ->
        val total = c.prepareStatement("SELECT COUNT(*) FROM songs").use { s ->
            s.executeQuery().use { it.next(); it.getLong(1) }
        }
        val items = c.prepareStatement("SELECT * FROM songs ORDER BY created_at DESC, id LIMIT ? OFFSET ?").use { s ->
            s.setInt(1, size)
            s.setLong(2, page.toLong() * size)
            s.executeQuery().use { results -> buildList { while (results.next()) add(results.toSong()) } }
        }
        items to total
    }

    fun song(id: UUID): SongResponse? = querySong("SELECT * FROM songs WHERE id = ?", id)

    fun featured(orderBy: String, limit: Int = 20): List<SongResponse> = DatabaseProvider.dataSource.connection.use { c ->
        val allowedOrder = when (orderBy) {
            "trending" -> "popularity DESC, play_count DESC, created_at DESC"
            "new" -> "created_at DESC"
            "popular" -> "play_count DESC, popularity DESC, created_at DESC"
            else -> error("Unsupported catalog order")
        }
        c.prepareStatement("SELECT * FROM songs ORDER BY $allowedOrder, id LIMIT ?").use { s ->
            s.setInt(1, limit)
            s.executeQuery().use { results -> buildList { while (results.next()) add(results.toSong()) } }
        }
    }

    fun artists(): List<ArtistResponse> = DatabaseProvider.dataSource.connection.use { c ->
        c.prepareStatement(ARTIST_SELECT + " GROUP BY a.id ORDER BY a.name").use { s ->
            s.executeQuery().use { results -> buildList { while (results.next()) add(results.toArtist()) } }
        }
    }

    fun artist(id: UUID): ArtistResponse? = DatabaseProvider.dataSource.connection.use { c ->
        c.prepareStatement(ARTIST_SELECT + " WHERE a.id = ? GROUP BY a.id").use { s ->
            s.setObject(1, id)
            s.executeQuery().use { if (it.next()) it.toArtist() else null }
        }
    }

    fun artistSongs(id: UUID, page: Int, size: Int): Pair<List<SongResponse>, Long> = DatabaseProvider.dataSource.connection.use { c ->
        val total = c.prepareStatement("SELECT COUNT(*) FROM songs WHERE artist_id = ?").use { s ->
            s.setObject(1, id)
            s.executeQuery().use { it.next(); it.getLong(1) }
        }
        val items = c.prepareStatement("SELECT * FROM songs WHERE artist_id = ? ORDER BY created_at DESC, id LIMIT ? OFFSET ?").use { s ->
            s.setObject(1, id)
            s.setInt(2, size)
            s.setLong(3, page.toLong() * size)
            s.executeQuery().use { results -> buildList { while (results.next()) add(results.toSong()) } }
        }
        items to total
    }

    private fun querySong(sql: String, value: Any): SongResponse? = DatabaseProvider.dataSource.connection.use { c ->
        c.prepareStatement(sql).use { s ->
            s.setObject(1, value)
            s.executeQuery().use { if (it.next()) it.toSong() else null }
        }
    }

    private companion object {
        const val ARTIST_SELECT = """
            SELECT a.*, COUNT(s.id) AS song_count
            FROM artists a LEFT JOIN songs s ON s.artist_id = a.id
        """
    }
}

private val json = Json { ignoreUnknownKeys = true }
private fun ResultSet.jsonObject(column: String) = json.parseToJsonElement(getString(column) ?: "{}").let { it as? JsonObject ?: JsonObject(emptyMap()) }
private fun ResultSet.nullableInt(column: String): Int? = getInt(column).let { if (wasNull()) null else it }
private fun ResultSet.nullableLong(column: String): Long? = getLong(column).let { if (wasNull()) null else it }

internal fun ResultSet.toSong() = SongResponse(
    id = getObject("id", UUID::class.java).toString(),
    artistId = getObject("artist_id", UUID::class.java)?.toString(),
    title = getString("title"), artistName = getString("artist_name"), album = getString("album"),
    albumArtist = getString("album_artist"), trackNumber = nullableInt("track_number"), discNumber = nullableInt("disc_number"),
    genre = getString("genre"), durationSeconds = getInt("duration_seconds"), bitrateKbps = nullableInt("bitrate_kbps"),
    sampleRateHz = nullableInt("sample_rate_hz"), channels = getString("channels"), codec = getString("codec"),
    fileFormat = getString("file_format"), releaseYear = nullableInt("release_year"), releaseDate = getDate("release_date")?.toLocalDate()?.toString(),
    language = getString("language"), lyrics = getString("lyrics"), composer = getString("composer"), producer = getString("producer"),
    copyright = getString("copyright"), publisher = getString("publisher"), mood = getString("mood"),
    tags = (getArray("tags")?.array as? Array<*>)?.mapNotNull { it?.toString() }.orEmpty(), isExplicit = getBoolean("is_explicit"),
    popularity = getInt("popularity"), playCount = getLong("play_count"), isLocal = getBoolean("is_local"), isDemo = getBoolean("is_demo"),
    sourceFileName = getString("source_file_name"), sourceRelativePath = getString("source_relative_path"), audioFileSize = nullableLong("audio_file_size"),
    coverFileName = getString("cover_file_name"), coverRelativePath = getString("cover_relative_path"), audioUrl = getString("audio_url"),
    coverImageUrl = getString("cover_image_url"), artistImageUrl = getString("artist_image_url"), albumCoverUrl = getString("album_cover_url"),
    extraMetadata = jsonObject("extra_metadata"), createdAt = getTimestamp("created_at").toInstant().toString(), updatedAt = getTimestamp("updated_at").toInstant().toString(),
)

private fun ResultSet.toArtist() = ArtistResponse(
    id = getObject("id", UUID::class.java).toString(), name = getString("name"), imageUrl = getString("image_url"),
    biography = getString("biography"), songCount = getLong("song_count"), extraMetadata = jsonObject("extra_metadata"),
    createdAt = getTimestamp("created_at").toInstant().toString(),
)
