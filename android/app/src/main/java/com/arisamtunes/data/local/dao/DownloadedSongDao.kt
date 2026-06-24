package com.arisamtunes.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.arisamtunes.data.local.entity.DownloadedSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedSongDao {
    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    fun pagingSource(): PagingSource<Int, DownloadedSongEntity>

    @Query("SELECT * FROM downloaded_songs WHERE songId = :songId LIMIT 1")
    fun observe(songId: String): Flow<DownloadedSongEntity?>

    @Query("SELECT localFilePath FROM downloaded_songs WHERE songId = :songId AND downloadState = 'COMPLETED' LIMIT 1")
    suspend fun completedPath(songId: String): String?

    @Upsert
    suspend fun upsert(song: DownloadedSongEntity)

    @Query("UPDATE downloaded_songs SET downloadState = :state WHERE songId = :songId")
    suspend fun updateState(songId: String, state: String)

    @Query("DELETE FROM downloaded_songs WHERE songId = :songId")
    suspend fun delete(songId: String)
}
