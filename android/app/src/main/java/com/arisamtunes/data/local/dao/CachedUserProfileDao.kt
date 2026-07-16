package com.arisamtunes.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.arisamtunes.data.local.entity.CachedUserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedUserProfileDao {
    @Query("SELECT * FROM cached_user_profiles WHERE userId = :userId LIMIT 1")
    fun observe(userId: String): Flow<CachedUserProfileEntity?>

    @Query("SELECT * FROM cached_user_profiles WHERE displayName LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' ORDER BY displayName LIMIT :limit")
    fun observeSearch(query: String, limit: Int = 30): Flow<List<CachedUserProfileEntity>>

    @Upsert
    suspend fun upsert(profile: CachedUserProfileEntity)

    @Upsert
    suspend fun upsertAll(profiles: List<CachedUserProfileEntity>)

    @Query("SELECT * FROM cached_user_profiles WHERE userId IN (:userIds)")
    suspend fun profiles(userIds: List<String>): List<CachedUserProfileEntity>

    @Query("SELECT * FROM cached_user_profiles WHERE userId = :userId LIMIT 1")
    suspend fun profile(userId: String): CachedUserProfileEntity?

    @Query("DELETE FROM cached_user_profiles WHERE cachedAt < :olderThan")
    suspend fun prune(olderThan: Long)
}
