package com.arisamtunes.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.arisamtunes.data.local.entity.DownloadedSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedSongDao {
    @Query(
        """SELECT * FROM downloaded_songs
           WHERE ownerUserId = :ownerUserId
           ORDER BY
             CASE WHEN :sortOrder = 'TITLE' THEN title END COLLATE NOCASE ASC,
             CASE WHEN :sortOrder = 'ARTIST' THEN artistName END COLLATE NOCASE ASC,
             downloadedAt DESC""",
    )
    fun pagingSource(ownerUserId: String, sortOrder: String): PagingSource<Int, DownloadedSongEntity>

    @Query("SELECT * FROM downloaded_songs WHERE ownerUserId = :ownerUserId AND songId = :songId LIMIT 1")
    fun observe(ownerUserId: String, songId: String): Flow<DownloadedSongEntity?>

    @Query("SELECT * FROM downloaded_songs WHERE ownerUserId = :ownerUserId AND songId = :songId LIMIT 1")
    suspend fun get(ownerUserId: String, songId: String): DownloadedSongEntity?

    @Query("SELECT localFilePath FROM downloaded_songs WHERE ownerUserId = :ownerUserId AND songId = :songId AND downloadState = 'COMPLETED' LIMIT 1")
    suspend fun completedPath(ownerUserId: String, songId: String): String?

    @Query("SELECT * FROM downloaded_songs WHERE ownerUserId = :ownerUserId AND downloadState = 'COMPLETED' ORDER BY downloadedAt DESC")
    fun observeCompleted(ownerUserId: String): Flow<List<DownloadedSongEntity>>

    @Upsert
    suspend fun upsert(song: DownloadedSongEntity)

    @Query("UPDATE downloaded_songs SET downloadState = :state, downloadProgress = :progress, failureReason = :failureReason WHERE ownerUserId = :ownerUserId AND songId = :songId")
    suspend fun updateState(ownerUserId: String, songId: String, state: String, progress: Int, failureReason: String? = null)

    @Query("UPDATE downloaded_songs SET downloadProgress = :progress WHERE ownerUserId = :ownerUserId AND songId = :songId")
    suspend fun updateProgress(ownerUserId: String, songId: String, progress: Int)

    @Query("UPDATE downloaded_songs SET durationSeconds = :durationSeconds WHERE ownerUserId = :ownerUserId AND songId = :songId")
    suspend fun updateDuration(ownerUserId: String, songId: String, durationSeconds: Int)

    @Query("DELETE FROM downloaded_songs WHERE ownerUserId = :ownerUserId AND songId = :songId")
    suspend fun delete(ownerUserId: String, songId: String)

    @Query("UPDATE OR IGNORE downloaded_songs SET ownerUserId = :ownerUserId WHERE ownerUserId = :legacyOwnerUserId")
    suspend fun claimLegacy(ownerUserId: String, legacyOwnerUserId: String)

    @Query("DELETE FROM downloaded_songs WHERE ownerUserId = :legacyOwnerUserId")
    suspend fun deleteLegacy(legacyOwnerUserId: String)
}
