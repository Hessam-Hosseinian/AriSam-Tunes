package com.arisamtunes.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.arisamtunes.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY lastSearchedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE query LIKE '%' || :query || '%' ORDER BY lastSearchedAt DESC LIMIT :limit")
    fun observeMatching(query: String, limit: Int = 20): Flow<List<SearchHistoryEntity>>

    @Upsert
    suspend fun upsert(item: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM search_history")
    suspend fun clear()
}
