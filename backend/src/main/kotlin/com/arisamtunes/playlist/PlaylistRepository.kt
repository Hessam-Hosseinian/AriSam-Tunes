package com.arisamtunes.playlist

import com.arisamtunes.catalog.SongResponse
import com.arisamtunes.catalog.toSong
import com.arisamtunes.plugins.DatabaseProvider
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID

class PlaylistRepository {
    fun visibleTo(userId: UUID): List<PlaylistResponse> = queryPlaylists(
        // A user's private playlists remain visible only to their owner, but a
        // playlist explicitly marked public must be discoverable regardless of
        // whether it is a seeded GLOBAL/LOCAL playlist or another user's USER
        // playlist.
        PLAYLIST_SELECT + " WHERE p.owner_id = ? OR p.is_public = TRUE GROUP BY p.id ORDER BY p.updated_at DESC",
        userId,
    ).markEditableBy(userId)

    fun byScope(scope: PlaylistScope): List<PlaylistResponse> = queryPlaylists(
        PLAYLIST_SELECT + " WHERE p.scope = ?::playlist_scope AND p.is_public = TRUE GROUP BY p.id ORDER BY p.updated_at DESC",
        scope.name,
    )

    fun findVisible(id: UUID, userId: UUID?): PlaylistResponse? = DatabaseProvider.dataSource.connection.use { c ->
        c.prepareStatement(
            PLAYLIST_SELECT + " WHERE p.id = ? AND (p.is_public = TRUE OR p.owner_id = ?) GROUP BY p.id",
        ).use { s ->
            s.setObject(1, id); s.setObject(2, userId)
            s.executeQuery().use { if (it.next()) it.toPlaylist().markEditableBy(userId) else null }
        }
    }

    fun create(ownerId: UUID, request: CreatePlaylistRequest): PlaylistResponse = DatabaseProvider.dataSource.connection.use { c ->
        try {
            val id = c.prepareStatement(
                "INSERT INTO playlists(owner_id,name,description,cover_image_url,scope,is_public) VALUES (?,?,?,?,'USER',?) RETURNING id",
            ).use { s ->
                s.setObject(1, ownerId); s.setString(2, request.name.trim()); s.setString(3, request.description?.trim())
                s.setString(4, request.coverImageUrl?.trim()); s.setBoolean(5, request.isPublic)
                s.executeQuery().use { it.next(); it.getObject(1, UUID::class.java) }
            }
            c.commit()
            findVisible(id, ownerId) ?: error("Created playlist could not be loaded")
        } catch (error: Throwable) { c.rollback(); throw error }
    }

    fun createGenerated(ownerId: UUID, request: CreateGeneratedPlaylistRequest): PlaylistResponse =
        DatabaseProvider.dataSource.connection.use { connection ->
            try {
                val playlistId = connection.prepareStatement(
                    "INSERT INTO playlists(owner_id,name,description,cover_image_url,scope,is_public) VALUES (?,?,?,?,'USER',FALSE) RETURNING id",
                ).use { statement ->
                    statement.setObject(1, ownerId)
                    statement.setString(2, request.name.trim())
                    statement.setString(3, request.description?.trim())
                    statement.setString(4, request.coverImageUrl?.trim())
                    statement.executeQuery().use { results ->
                        results.next()
                        results.getObject(1, UUID::class.java)
                    }
                }
                val songIds = request.songIds.map(UUID::fromString)
                val existingSongCount = connection.prepareStatement(
                    "SELECT COUNT(*) FROM songs WHERE id = ANY(?)",
                ).use { statement ->
                    statement.setArray(1, connection.createArrayOf("uuid", songIds.toTypedArray()))
                    statement.executeQuery().use { results -> results.next(); results.getInt(1) }
                }
                if (existingSongCount != songIds.size) {
                    throw IllegalArgumentException("Generated playlist contains a missing song")
                }
                connection.prepareStatement(
                    "INSERT INTO playlist_songs(playlist_id,song_id,position) VALUES (?,?,?)",
                ).use { statement ->
                    songIds.forEachIndexed { position, songId ->
                        statement.setObject(1, playlistId)
                        statement.setObject(2, songId)
                        statement.setInt(3, position)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
                connection.commit()
                checkNotNull(findVisible(playlistId, ownerId))
            } catch (error: Throwable) {
                connection.rollback()
                throw error
            }
        }

    fun update(id: UUID, ownerId: UUID, request: UpdatePlaylistRequest): PlaylistResponse? = DatabaseProvider.dataSource.connection.use { c ->
        try {
            val changed = c.prepareStatement(
                """
                UPDATE playlists
                SET name=?, description=?,
                    cover_image_url=CASE WHEN ? THEN NULL WHEN ? IS NOT NULL THEN ? ELSE cover_image_url END,
                    is_public=?, updated_at=NOW()
                WHERE id=? AND owner_id=? AND scope='USER'
                """.trimIndent(),
            ).use { s ->
                val coverImageUrl = request.coverImageUrl?.trim()
                s.setString(1, request.name.trim()); s.setString(2, request.description?.trim())
                s.setBoolean(3, request.clearCoverImage); s.setString(4, coverImageUrl); s.setString(5, coverImageUrl)
                s.setBoolean(6, request.isPublic); s.setObject(7, id); s.setObject(8, ownerId); s.executeUpdate()
            }
            c.commit()
            if (changed == 0) null else findVisible(id, ownerId)
        } catch (error: Throwable) { c.rollback(); throw error }
    }

    fun delete(id: UUID, ownerId: UUID): Boolean = DatabaseProvider.dataSource.connection.use { c ->
        val changed = c.prepareStatement("DELETE FROM playlists WHERE id=? AND owner_id=? AND scope='USER'").use { s ->
            s.setObject(1, id); s.setObject(2, ownerId); s.executeUpdate()
        }
        c.commit(); changed > 0
    }

    fun addSong(id: UUID, ownerId: UUID, songId: UUID): AddSongResult = DatabaseProvider.dataSource.connection.use { c ->
        try {
            // READ COMMITTED lets a waiter observe the position committed by the
            // transaction that previously held the playlist row lock.
            c.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
            // Lock the playlist row so concurrent additions cannot calculate the
            // same next position and violate (playlist_id, position).
            val playlistExists = c.prepareStatement("SELECT 1 FROM playlists WHERE id=? AND owner_id=? AND scope='USER' FOR UPDATE").use { s ->
                s.setObject(1, id); s.setObject(2, ownerId); s.executeQuery().use { it.next() }
            }
            if (!playlistExists) {
                c.rollback()
                return AddSongResult.PlaylistNotFound
            }
            val songExists = c.prepareStatement("SELECT 1 FROM songs WHERE id=?").use { s ->
                s.setObject(1, songId); s.executeQuery().use { it.next() }
            }
            if (!songExists) {
                c.rollback()
                return AddSongResult.SongNotFound
            }
            val inserted = c.prepareStatement(
                """
                INSERT INTO playlist_songs(playlist_id, song_id, position)
                SELECT ?, ?, COALESCE(MAX(position) + 1, 0)
                FROM playlist_songs
                WHERE playlist_id = ?
                ON CONFLICT (playlist_id, song_id) DO NOTHING
                """.trimIndent(),
            ).use { s ->
                s.setObject(1, id); s.setObject(2, songId); s.setObject(3, id); s.executeUpdate()
            }
            if (inserted > 0) {
                c.prepareStatement("UPDATE playlists SET updated_at=NOW() WHERE id=?").use { s ->
                    s.setObject(1, id); s.executeUpdate()
                }
            }
            c.commit()
            AddSongResult.Success(checkNotNull(findVisible(id, ownerId)))
        } catch (error: Throwable) { c.rollback(); throw error }
    }

    fun removeSong(id: UUID, ownerId: UUID, songId: UUID): PlaylistResponse? = DatabaseProvider.dataSource.connection.use { c ->
        try {
            c.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
            val playlistExists = c.prepareStatement("SELECT 1 FROM playlists WHERE id=? AND owner_id=? AND scope='USER' FOR UPDATE").use { s ->
                s.setObject(1, id); s.setObject(2, ownerId); s.executeQuery().use { it.next() }
            }
            if (!playlistExists) {
                c.rollback()
                return null
            }
            val removed = c.prepareStatement("DELETE FROM playlist_songs WHERE playlist_id=? AND song_id=?").use { s ->
                s.setObject(1, id); s.setObject(2, songId); s.executeUpdate()
            }
            if (removed > 0) {
                c.prepareStatement("UPDATE playlists SET updated_at=NOW() WHERE id=?").use { s ->
                    s.setObject(1, id); s.executeUpdate()
                }
            }
            c.commit()
            findVisible(id, ownerId)
        } catch (error: Throwable) { c.rollback(); throw error }
    }

    fun songs(playlistId: UUID, page: Int, size: Int): Pair<List<SongResponse>, Long> = DatabaseProvider.dataSource.connection.use { c ->
        val total = c.prepareStatement("SELECT COUNT(*) FROM playlist_songs WHERE playlist_id=?").use { s ->
            s.setObject(1, playlistId); s.executeQuery().use { it.next(); it.getLong(1) }
        }
        val items = c.prepareStatement(
            "SELECT s.* FROM playlist_songs ps JOIN songs s ON s.id=ps.song_id WHERE ps.playlist_id=? ORDER BY ps.position LIMIT ? OFFSET ?",
        ).use { s ->
            s.setObject(1, playlistId); s.setInt(2, size); s.setLong(3, page.toLong() * size)
            s.executeQuery().use { results -> buildList { while (results.next()) add(results.toSong()) } }
        }
        items to total
    }

    private fun queryPlaylists(sql: String, value: Any): List<PlaylistResponse> = DatabaseProvider.dataSource.connection.use { c ->
        c.prepareStatement(sql).use { s ->
            s.setObject(1, value)
            s.executeQuery().use { results -> buildList { while (results.next()) add(results.toPlaylist()) } }
        }
    }

    private companion object {
        const val PLAYLIST_SELECT = """
            SELECT p.*, COUNT(ps.song_id) AS song_count
            FROM playlists p LEFT JOIN playlist_songs ps ON ps.playlist_id=p.id
        """
    }
}

sealed interface AddSongResult {
    data class Success(val playlist: PlaylistResponse) : AddSongResult
    data object PlaylistNotFound : AddSongResult
    data object SongNotFound : AddSongResult
}

private fun List<PlaylistResponse>.markEditableBy(userId: UUID): List<PlaylistResponse> =
    map { it.markEditableBy(userId) }

private fun PlaylistResponse.markEditableBy(userId: UUID?): PlaylistResponse =
    copy(canEdit = userId != null && scope == PlaylistScope.USER && ownerId == userId.toString())

private fun ResultSet.toPlaylist() = PlaylistResponse(
    id = getObject("id", UUID::class.java).toString(), ownerId = getObject("owner_id", UUID::class.java)?.toString(),
    name = getString("name"), description = getString("description"), coverImageUrl = getString("cover_image_url"),
    scope = PlaylistScope.valueOf(getString("scope")), isPublic = getBoolean("is_public"), songCount = getLong("song_count"),
    createdAt = getTimestamp("created_at").toInstant().toString(), updatedAt = getTimestamp("updated_at").toInstant().toString(),
)
