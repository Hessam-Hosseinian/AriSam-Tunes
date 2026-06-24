package com.arisamtunes.social

import com.arisamtunes.playlist.PlaylistResponse
import com.arisamtunes.playlist.PlaylistScope
import com.arisamtunes.plugins.DatabaseProvider
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.UUID

class SocialRepository {
    fun searchUsers(query: String, viewerId: UUID?, page: Int, size: Int): Pair<List<PublicUserResponse>, Long> =
        DatabaseProvider.dataSource.connection.use { c ->
            val search = "%${query.trim().lowercase()}%"
            val total = c.prepareStatement(
                """
                SELECT COUNT(*)
                FROM users
                WHERE LOWER(display_name) LIKE ? OR LOWER(email) LIKE ?
                """.trimIndent(),
            ).use { s ->
                s.setString(1, search)
                s.setString(2, search)
                s.executeQuery().use { it.next(); it.getLong(1) }
            }
            val items = c.prepareStatement(
                USER_SELECT + """
                WHERE LOWER(u.display_name) LIKE ? OR LOWER(u.email) LIKE ?
                ORDER BY u.display_name ASC, u.created_at DESC
                LIMIT ? OFFSET ?
                """.trimIndent(),
            ).use { s ->
                s.setViewer(1, viewerId)
                s.setString(2, search)
                s.setString(3, search)
                s.setInt(4, size)
                s.setLong(5, page.toLong() * size)
                s.executeQuery().use { results -> buildList { while (results.next()) add(results.toPublicUser()) } }
            }
            c.commit()
            items to total
        }

    fun user(id: UUID, viewerId: UUID?): PublicUserResponse? = queryUser(
        USER_SELECT + "WHERE u.id = ?",
        viewerId,
        id,
    )

    fun follow(followerId: UUID, followedId: UUID): PublicUserResponse? = DatabaseProvider.dataSource.connection.use { c ->
        try {
            if (!c.userExists(followedId)) {
                c.commit()
                return@use null
            }
            c.prepareStatement(
                """
                INSERT INTO user_follows(follower_id, followed_id)
                VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """.trimIndent(),
            ).use { s ->
                s.setObject(1, followerId)
                s.setObject(2, followedId)
                s.executeUpdate()
            }
            c.commit()
            user(followedId, followerId)
        } catch (error: Throwable) {
            c.rollback()
            throw error
        }
    }

    fun unfollow(followerId: UUID, followedId: UUID): PublicUserResponse? = DatabaseProvider.dataSource.connection.use { c ->
        try {
            c.prepareStatement("DELETE FROM user_follows WHERE follower_id = ? AND followed_id = ?").use { s ->
                s.setObject(1, followerId)
                s.setObject(2, followedId)
                s.executeUpdate()
            }
            c.commit()
            user(followedId, followerId)
        } catch (error: Throwable) {
            c.rollback()
            throw error
        }
    }

    fun following(userId: UUID, viewerId: UUID?, page: Int, size: Int): Pair<List<PublicUserResponse>, Long> =
        usersByFollow(
            totalSql = "SELECT COUNT(*) FROM user_follows WHERE follower_id = ?",
            listSql = USER_SELECT + """
                JOIN user_follows f ON f.followed_id = u.id
                WHERE f.follower_id = ?
                ORDER BY f.created_at DESC
                LIMIT ? OFFSET ?
            """.trimIndent(),
            userId = userId,
            viewerId = viewerId,
            page = page,
            size = size,
        )

    fun followers(userId: UUID, viewerId: UUID?, page: Int, size: Int): Pair<List<PublicUserResponse>, Long> =
        usersByFollow(
            totalSql = "SELECT COUNT(*) FROM user_follows WHERE followed_id = ?",
            listSql = USER_SELECT + """
                JOIN user_follows f ON f.follower_id = u.id
                WHERE f.followed_id = ?
                ORDER BY f.created_at DESC
                LIMIT ? OFFSET ?
            """.trimIndent(),
            userId = userId,
            viewerId = viewerId,
            page = page,
            size = size,
        )

    fun publicPlaylists(ownerId: UUID): List<PlaylistResponse> = DatabaseProvider.dataSource.connection.use { c ->
        c.prepareStatement(
            """
            SELECT p.*, COUNT(ps.song_id) AS song_count
            FROM playlists p
            LEFT JOIN playlist_songs ps ON ps.playlist_id = p.id
            WHERE p.owner_id = ? AND p.is_public = TRUE
            GROUP BY p.id
            ORDER BY p.updated_at DESC
            """.trimIndent(),
        ).use { s ->
            s.setObject(1, ownerId)
            s.executeQuery().use { results -> buildList { while (results.next()) add(results.toPlaylist()) } }
        }.also { c.commit() }
    }

    private fun queryUser(sql: String, viewerId: UUID?, value: UUID): PublicUserResponse? =
        DatabaseProvider.dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setViewer(1, viewerId)
                s.setObject(2, value)
                s.executeQuery().use { results -> if (results.next()) results.toPublicUser() else null }
            }.also { c.commit() }
        }

    private fun usersByFollow(
        totalSql: String,
        listSql: String,
        userId: UUID,
        viewerId: UUID?,
        page: Int,
        size: Int,
    ): Pair<List<PublicUserResponse>, Long> = DatabaseProvider.dataSource.connection.use { c ->
        val total = c.prepareStatement(totalSql).use { s ->
            s.setObject(1, userId)
            s.executeQuery().use { it.next(); it.getLong(1) }
        }
        val items = c.prepareStatement(listSql).use { s ->
            s.setViewer(1, viewerId)
            s.setObject(2, userId)
            s.setInt(3, size)
            s.setLong(4, page.toLong() * size)
            s.executeQuery().use { results -> buildList { while (results.next()) add(results.toPublicUser()) } }
        }
        c.commit()
        items to total
    }

    private fun java.sql.Connection.userExists(id: UUID): Boolean =
        prepareStatement("SELECT EXISTS(SELECT 1 FROM users WHERE id = ?)").use { s ->
            s.setObject(1, id)
            s.executeQuery().use { it.next(); it.getBoolean(1) }
        }

    private companion object {
        const val USER_SELECT = """
            SELECT
                u.id,
                u.display_name,
                u.avatar_url,
                u.bio,
                u.is_premium,
                (
                    SELECT COUNT(*)
                    FROM user_follows followers
                    WHERE followers.followed_id = u.id
                ) AS followers_count,
                (
                    SELECT COUNT(*)
                    FROM user_follows following
                    WHERE following.follower_id = u.id
                ) AS following_count,
                EXISTS(
                    SELECT 1
                    FROM user_follows viewer_follow
                    WHERE viewer_follow.follower_id = ? AND viewer_follow.followed_id = u.id
                ) AS is_following
            FROM users u
            
        """
    }
}

private fun PreparedStatement.setViewer(index: Int, value: UUID?) {
    if (value == null) setNull(index, Types.OTHER) else setObject(index, value)
}

private fun ResultSet.toPublicUser() = PublicUserResponse(
    id = getObject("id", UUID::class.java).toString(),
    displayName = getString("display_name"),
    avatarUrl = getString("avatar_url"),
    bio = getString("bio"),
    isPremium = getBoolean("is_premium"),
    followersCount = getLong("followers_count"),
    followingCount = getLong("following_count"),
    isFollowing = getBoolean("is_following"),
)

private fun ResultSet.toPlaylist() = PlaylistResponse(
    id = getObject("id", UUID::class.java).toString(),
    ownerId = getObject("owner_id", UUID::class.java)?.toString(),
    name = getString("name"),
    description = getString("description"),
    coverImageUrl = getString("cover_image_url"),
    scope = PlaylistScope.valueOf(getString("scope")),
    isPublic = getBoolean("is_public"),
    songCount = getLong("song_count"),
    createdAt = getTimestamp("created_at").toInstant().toString(),
    updatedAt = getTimestamp("updated_at").toInstant().toString(),
)
