package com.arisamtunes.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.arisamtunes.data.local.entity.CachedSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedSongDao {
    @Query("SELECT * FROM cached_songs ORDER BY cachedAt DESC")
    fun pagingSource(): PagingSource<Int, CachedSongEntity>

    @Query(
        """
        SELECT * FROM cached_songs
        WHERE title LIKE '%' || :query || '%'
           OR artistName LIKE '%' || :query || '%'
           OR album LIKE '%' || :query || '%'
           OR genre LIKE '%' || :query || '%'
        ORDER BY popularity DESC, title
        """,
    )
    fun searchPagingSource(query: String): PagingSource<Int, CachedSongEntity>

    @Query("SELECT * FROM cached_songs WHERE id = :id LIMIT 1")
    fun observe(id: String): Flow<CachedSongEntity?>

    @Upsert
    suspend fun upsert(song: CachedSongEntity)

    @Upsert
    suspend fun upsertAll(songs: List<CachedSongEntity>)

    @Query("DELETE FROM cached_songs")
    suspend fun clear()

    @Query("DELETE FROM cached_songs WHERE cachedAt < :olderThan")
    suspend fun prune(olderThan: Long)
}
