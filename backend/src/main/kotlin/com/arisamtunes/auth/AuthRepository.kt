package com.arisamtunes.auth

import com.arisamtunes.plugins.DatabaseProvider
import java.sql.ResultSet
import java.time.Instant
import java.sql.Timestamp
import java.util.UUID

class AuthRepository {
    fun createUser(email: String, passwordHash: String, displayName: String): AuthUser = DatabaseProvider.dataSource.connection.use { c ->
        val user = c.prepareStatement("INSERT INTO users(email,password_hash,display_name) VALUES (?,?,?) RETURNING *").use { s ->
            s.setString(1, email); s.setString(2, passwordHash); s.setString(3, displayName)
            s.executeQuery().use { it.next(); it.toUser() }
        }
        c.commit(); user
    }

    fun findByEmail(email: String) = queryUser("SELECT * FROM users WHERE LOWER(email)=LOWER(?)", email)
    fun findById(id: UUID) = queryUser("SELECT * FROM users WHERE id=?", id)

    private fun queryUser(sql: String, value: Any): AuthUser? = DatabaseProvider.dataSource.connection.use { c ->
        c.prepareStatement(sql).use { s ->
            s.setObject(1, value); s.executeQuery().use { if (it.next()) it.toUser() else null }
        }
    }

    fun storeRefresh(userId: UUID, hash: String, expiresAt: Instant) = DatabaseProvider.dataSource.connection.use { c ->
        c.prepareStatement("INSERT INTO refresh_tokens(user_id,token_hash,expires_at) VALUES (?,?,?)").use { s ->
            s.setObject(1, userId); s.setString(2, hash); s.setTimestamp(3, Timestamp.from(expiresAt)); s.executeUpdate()
        }
        c.commit()
    }

    fun consumeRefresh(hash: String): AuthUser? = DatabaseProvider.dataSource.connection.use { c ->
        try {
            val user = c.prepareStatement("SELECT u.* FROM refresh_tokens r JOIN users u ON u.id=r.user_id WHERE r.token_hash=? AND r.revoked_at IS NULL AND r.expires_at>NOW() FOR UPDATE OF r").use { s ->
                s.setString(1, hash); s.executeQuery().use { if (it.next()) it.toUser() else null }
            }
            if (user != null) c.prepareStatement("UPDATE refresh_tokens SET revoked_at=NOW() WHERE token_hash=?").use { s -> s.setString(1, hash); s.executeUpdate() }
            c.commit(); user
        } catch (error: Throwable) { c.rollback(); throw error }
    }

    fun revokeRefresh(hash: String) = DatabaseProvider.dataSource.connection.use { c ->
        c.prepareStatement("UPDATE refresh_tokens SET revoked_at=NOW() WHERE token_hash=? AND revoked_at IS NULL").use { s -> s.setString(1, hash); s.executeUpdate() }
        c.commit()
    }
}

private fun ResultSet.toUser() = AuthUser(
    getObject("id", UUID::class.java), getString("email"), getString("password_hash"),
    getString("display_name"), getString("avatar_url"), getBoolean("is_premium"),
)
