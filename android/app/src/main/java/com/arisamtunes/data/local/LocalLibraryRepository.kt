package com.arisamtunes.data.local

import androidx.paging.PagingSource
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.local.dao.CachedSongDao
import com.arisamtunes.data.local.dao.DownloadedSongDao
import com.arisamtunes.data.local.dao.LikedSongDao
import com.arisamtunes.data.local.dao.RecentlyPlayedDao
import com.arisamtunes.data.local.entity.CachedSongEntity
import com.arisamtunes.data.local.entity.DownloadedSongEntity
import com.arisamtunes.data.local.entity.LikedSongEntity
import com.arisamtunes.data.local.entity.RecentlyPlayedEntity
import com.arisamtunes.data.preferences.UserPreferencesStore
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class LocalLibraryRepository @Inject constructor(
    private val cachedSongDao: CachedSongDao,
    private val likedSongDao: LikedSongDao,
    private val downloadedSongDao: DownloadedSongDao,
    private val recentlyPlayedDao: RecentlyPlayedDao,
    private val preferencesStore: UserPreferencesStore,
) {
    fun cachedSongs(): PagingSource<Int, CachedSongEntity> = cachedSongDao.pagingSource()

    fun searchCachedSongs(query: String, type: String = "all"): PagingSource<Int, CachedSongEntity> = cachedSongDao.searchPagingSource(query, type)

    suspend fun cacheSongs(songs: List<SongDto>) = cachedSongDao.upsertAll(songs.map { it.toCachedSongEntity() })

    fun likedSongs(): PagingSource<Int, LikedSongEntity> = likedSongDao.pagingSource()

    fun observeLikedSongs(): Flow<List<LikedSongEntity>> = likedSongDao.observeAll()

    fun observeIsLiked(songId: String): Flow<Boolean> = likedSongDao.observeIsLiked(songId)

    suspend fun setLiked(song: SongDto, liked: Boolean) {
        if (liked) likedSongDao.upsert(song.toLikedSongEntity()) else likedSongDao.unlike(song.id)
    }

    suspend fun toggleLiked(song: SongDto) {
        val isLiked = likedSongDao.observeIsLiked(song.id).first()
        setLiked(song, !isLiked)
    }

    suspend fun removeLiked(songId: String) = likedSongDao.unlike(songId)

    fun downloads(ownerUserId: String, sortOrder: String = "NEWEST"): PagingSource<Int, DownloadedSongEntity> =
        downloadedSongDao.pagingSource(ownerUserId, sortOrder)

    fun observeDownload(songId: String): Flow<DownloadedSongEntity?> = preferencesStore.preferences
        .flatMapLatest { preferences ->
            preferences.currentUserId?.let { downloadedSongDao.observe(it, songId) } ?: flowOf(null)
        }

    suspend fun download(songId: String): DownloadedSongEntity? = ownerUserId()
        ?.let { downloadedSongDao.get(it, songId) }

    suspend fun saveDownload(
        song: SongDto,
        localFilePath: String,
        state: String = DownloadStateCompleted,
        progress: Int = if (state == DownloadStateCompleted) 100 else 0,
    ) {
        val ownerUserId = ownerUserId() ?: return
        downloadedSongDao.upsert(
            song.toDownloadedSongEntity(ownerUserId, localFilePath, state, progress),
        )
    }

    suspend fun refreshDownloadMetadata(song: SongDto) {
        val ownerUserId = ownerUserId() ?: return
        val existing = downloadedSongDao.get(ownerUserId, song.id) ?: return
        val refreshed = song.toDownloadedSongEntity(
            ownerUserId = ownerUserId,
            localFilePath = existing.localFilePath,
            state = existing.downloadState,
            progress = existing.downloadProgress,
            failureReason = existing.failureReason,
            downloadedAt = existing.downloadedAt,
        ).copy(
            mimeType = existing.mimeType ?: song.fileFormat?.lowercase()?.let { "audio/$it" },
            fileSizeBytes = existing.fileSizeBytes ?: song.audioFileSize,
        )
        downloadedSongDao.upsert(refreshed)
    }

    suspend fun updateDownloadDuration(songId: String, durationSeconds: Int) {
        val ownerUserId = ownerUserId() ?: return
        if (durationSeconds > 0) downloadedSongDao.updateDuration(ownerUserId, songId, durationSeconds)
    }

    suspend fun hasPlayableDownload(songId: String): Boolean = ownerUserId()
        ?.let { downloadedSongDao.completedPath(it, songId) }
        ?.let(::File)
        ?.isFile == true

    suspend fun deleteDownload(songId: String) {
        ownerUserId()?.let { downloadedSongDao.delete(it, songId) }
    }

    /** Removes the database record only after its downloaded file is gone. */
    suspend fun deleteDownloadAndFile(songId: String): Boolean {
        val ownerUserId = ownerUserId() ?: return false
        val download = downloadedSongDao.get(ownerUserId, songId) ?: return true
        val file = File(download.localFilePath)
        if (file.exists() && !file.delete()) return false
        downloadedSongDao.delete(ownerUserId, songId)
        return true
    }

    suspend fun playbackSource(song: SongDto): String = selectPlaybackSource(
        networkUrl = song.audioUrl,
        completedPath = ownerUserId()?.let { downloadedSongDao.completedPath(it, song.id) },
    )

    fun completedDownloads(): Flow<List<DownloadedSongEntity>> = preferencesStore.preferences
        .flatMapLatest { preferences ->
            preferences.currentUserId?.let(downloadedSongDao::observeCompleted) ?: flowOf(emptyList())
        }
        .map { downloads -> downloads.filter { File(it.localFilePath).isFile } }

    suspend fun claimLegacyDownloads(ownerUserId: String) {
        if (ownerUserId.isBlank()) return
        downloadedSongDao.claimLegacy(ownerUserId, LegacyDownloadOwnerId)
        downloadedSongDao.deleteLegacy(LegacyDownloadOwnerId)
    }

    fun recentlyPlayed(): PagingSource<Int, RecentlyPlayedEntity> = recentlyPlayedDao.pagingSource()

    fun observeRecent(limit: Int = 30): Flow<List<RecentlyPlayedEntity>> = recentlyPlayedDao.observeRecent(limit)

    fun observeAllRecentlyPlayed(): Flow<List<RecentlyPlayedEntity>> = recentlyPlayedDao.observeAll()

    suspend fun removeRecentlyPlayed(songId: String) = recentlyPlayedDao.delete(songId)

    suspend fun recordPlayed(song: SongDto, positionSeconds: Int = 0) {
        recentlyPlayedDao.upsert(song.toRecentlyPlayedEntity(positionSeconds))
    }

    companion object {
        const val DownloadStateCompleted = "COMPLETED"
        const val DownloadStateQueued = "QUEUED"
        const val DownloadStateRunning = "RUNNING"
        const val DownloadStateFailed = "FAILED"
    }

    private suspend fun ownerUserId(): String? {
        val ownerUserId = preferencesStore.preferences.first().currentUserId ?: return null
        claimLegacyDownloads(ownerUserId)
        return ownerUserId
    }
}

internal fun selectPlaybackSource(
    networkUrl: String,
    completedPath: String?,
    isFile: (File) -> Boolean = File::isFile,
): String = completedPath
    ?.let(::File)
    ?.takeIf(isFile)
    ?.toURI()
    ?.toString()
    ?: networkUrl
