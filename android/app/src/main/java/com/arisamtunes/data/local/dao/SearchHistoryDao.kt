package com.arisamtunes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.arisamtunes.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY lastSearchedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE query LIKE '%' || :query || '%' ORDER BY lastSearchedAt DESC LIMIT :limit")
    fun observeMatching(query: String, limit: Int = 20): Flow<List<SearchHistoryEntity>>

    @Query(
        """
        UPDATE search_history
        SET query = :query, resultCount = :resultCount, lastSearchedAt = :lastSearchedAt
        WHERE query = :query COLLATE NOCASE AND filter = :filter
        """,
    )
    suspend fun refreshExisting(
        query: String,
        filter: String,
        resultCount: Long?,
        lastSearchedAt: Long,
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: SearchHistoryEntity): Long

    @Transaction
    suspend fun record(item: SearchHistoryEntity) {
        val updatedRows = refreshExisting(
            query = item.query,
            filter = item.filter,
            resultCount = item.resultCount,
            lastSearchedAt = item.lastSearchedAt,
        )
        if (updatedRows == 0) insert(item)
    }

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM search_history")
    suspend fun clear()
}
