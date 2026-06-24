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
        WHERE (:type = 'all' AND (
                title LIKE '%' || :query || '%'
             OR artistName LIKE '%' || :query || '%'
             OR albumArtist LIKE '%' || :query || '%'
             OR album LIKE '%' || :query || '%'
             OR genre LIKE '%' || :query || '%'
             OR composer LIKE '%' || :query || '%'
             OR producer LIKE '%' || :query || '%'
        ))
        OR (:type = 'title' AND title LIKE '%' || :query || '%')
        OR (:type = 'artist' AND (artistName LIKE '%' || :query || '%' OR albumArtist LIKE '%' || :query || '%'))
        OR (:type = 'album' AND album LIKE '%' || :query || '%')
        OR (:type = 'genre' AND genre LIKE '%' || :query || '%')
        ORDER BY popularity DESC, title
        """,
    )
    fun searchPagingSource(query: String, type: String): PagingSource<Int, CachedSongEntity>

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
