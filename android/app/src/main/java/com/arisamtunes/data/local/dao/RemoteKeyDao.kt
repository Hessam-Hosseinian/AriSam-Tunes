package com.arisamtunes.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.arisamtunes.data.local.entity.RemoteKeyEntity

@Dao
interface RemoteKeyDao {
    @Query("SELECT * FROM remote_keys WHERE scope = :scope AND cacheKey = :cacheKey AND itemId = :itemId LIMIT 1")
    suspend fun remoteKey(scope: String, cacheKey: String, itemId: String): RemoteKeyEntity?

    @Query("SELECT * FROM remote_keys WHERE scope = :scope AND cacheKey = :cacheKey ORDER BY currentPage DESC LIMIT 1")
    suspend fun latestFor(scope: String, cacheKey: String): RemoteKeyEntity?

    @Upsert
    suspend fun upsertAll(keys: List<RemoteKeyEntity>)

    @Query("DELETE FROM remote_keys WHERE scope = :scope AND cacheKey = :cacheKey")
    suspend fun clear(scope: String, cacheKey: String)

    @Query("DELETE FROM remote_keys WHERE createdAt < :olderThan")
    suspend fun prune(olderThan: Long)
}
