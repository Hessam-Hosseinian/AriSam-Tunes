package com.arisamtunes.playlist

import com.arisamtunes.catalog.SongResponse
import com.arisamtunes.catalog.toSong
import com.arisamtunes.plugins.DatabaseProvider
import java.sql.ResultSet
import java.util.UUID

class PlaylistRepository {
    fun visibleTo(userId: UUID): List<PlaylistResponse> = queryPlaylists(
        PLAYLIST_SELECT + " WHERE p.owner_id = ? OR (p.is_public = TRUE AND p.scope <> 'USER') GROUP BY p.id ORDER BY p.updated_at DESC",
        userId,
    )

    fun byScope(scope: PlaylistScope): List<PlaylistResponse> = queryPlaylists(
        PLAYLIST_SELECT + " WHERE p.scope = ?::playlist_scope AND p.is_public = TRUE GROUP BY p.id ORDER BY p.updated_at DESC",
        scope.name,
    )

    fun findVisible(id: UUID, userId: UUID?): PlaylistResponse? = DatabaseProvider.dataSource.connection.use { c ->
        c.prepareStatement(
            PLAYLIST_SELECT + " WHERE p.id = ? AND (p.is_public = TRUE OR p.owner_id = ?) GROUP BY p.id",
        ).use { s ->
            s.setObject(1, id); s.setObject(2, userId)
            s.executeQuery().use { if (it.next()) it.toPlaylist() else null }
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

    fun update(id: UUID, ownerId: UUID, request: UpdatePlaylistRequest): PlaylistResponse? = DatabaseProvider.dataSource.connection.use { c ->
        try {
            val changed = c.prepareStatement(
                "UPDATE playlists SET name=?,description=?,cover_image_url=?,is_public=?,updated_at=NOW() WHERE id=? AND owner_id=? AND scope='USER'",
            ).use { s ->
                s.setString(1, request.name.trim()); s.setString(2, request.description?.trim()); s.setString(3, request.coverImageUrl?.trim())
                s.setBoolean(4, request.isPublic); s.setObject(5, id); s.setObject(6, ownerId); s.executeUpdate()
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

private fun ResultSet.toPlaylist() = PlaylistResponse(
    id = getObject("id", UUID::class.java).toString(), ownerId = getObject("owner_id", UUID::class.java)?.toString(),
    name = getString("name"), description = getString("description"), coverImageUrl = getString("cover_image_url"),
    scope = PlaylistScope.valueOf(getString("scope")), isPublic = getBoolean("is_public"), songCount = getLong("song_count"),
    createdAt = getTimestamp("created_at").toInstant().toString(), updatedAt = getTimestamp("updated_at").toInstant().toString(),
)
