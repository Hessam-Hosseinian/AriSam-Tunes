package com.arisamtunes.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.arisamtunes.data.local.entity.LikedSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LikedSongDao {
    @Query("SELECT * FROM liked_songs ORDER BY likedAt DESC")
    fun pagingSource(): PagingSource<Int, LikedSongEntity>

    @Query("SELECT * FROM liked_songs ORDER BY likedAt DESC")
    fun observeAll(): Flow<List<LikedSongEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE songId = :songId)")
    fun observeIsLiked(songId: String): Flow<Boolean>

    @Upsert
    suspend fun upsert(song: LikedSongEntity)

    @Query("DELETE FROM liked_songs WHERE songId = :songId")
    suspend fun unlike(songId: String)
}
