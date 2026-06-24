package com.arisamtunes.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.arisamtunes.data.local.entity.RecentlyPlayedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentlyPlayedDao {
    @Query("SELECT * FROM recently_played ORDER BY playedAt DESC")
    fun pagingSource(): PagingSource<Int, RecentlyPlayedEntity>

    @Query("SELECT * FROM recently_played ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<RecentlyPlayedEntity>>

    @Upsert
    suspend fun upsert(item: RecentlyPlayedEntity)

    @Query("DELETE FROM recently_played WHERE songId = :songId")
    suspend fun delete(songId: String)

    @Query("DELETE FROM recently_played")
    suspend fun clear()
}
