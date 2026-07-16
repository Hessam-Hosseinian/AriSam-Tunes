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
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalLibraryRepository @Inject constructor(
    private val cachedSongDao: CachedSongDao,
    private val likedSongDao: LikedSongDao,
    private val downloadedSongDao: DownloadedSongDao,
    private val recentlyPlayedDao: RecentlyPlayedDao,
) {
    fun cachedSongs(): PagingSource<Int, CachedSongEntity> = cachedSongDao.pagingSource()

    fun searchCachedSongs(query: String, type: String = "all"): PagingSource<Int, CachedSongEntity> = cachedSongDao.searchPagingSource(query, type)

    suspend fun cacheSongs(songs: List<SongDto>) = cachedSongDao.upsertAll(songs.map { it.toCachedSongEntity() })

    fun likedSongs(): PagingSource<Int, LikedSongEntity> = likedSongDao.pagingSource()

    fun observeIsLiked(songId: String): Flow<Boolean> = likedSongDao.observeIsLiked(songId)

    suspend fun setLiked(song: SongDto, liked: Boolean) {
        if (liked) likedSongDao.upsert(song.toLikedSongEntity()) else likedSongDao.unlike(song.id)
    }

    suspend fun toggleLiked(song: SongDto) {
        val isLiked = likedSongDao.observeIsLiked(song.id).first()
        setLiked(song, !isLiked)
    }

    fun downloads(): PagingSource<Int, DownloadedSongEntity> = downloadedSongDao.pagingSource()

    fun observeDownload(songId: String): Flow<DownloadedSongEntity?> = downloadedSongDao.observe(songId)

    suspend fun saveDownload(song: SongDto, localFilePath: String, state: String = DownloadStateCompleted) =
        downloadedSongDao.upsert(song.toDownloadedSongEntity(localFilePath, state))

    suspend fun deleteDownload(songId: String) = downloadedSongDao.delete(songId)

    suspend fun playbackSource(song: SongDto): String =
        downloadedSongDao.completedPath(song.id)
            ?.let(::File)
            ?.takeIf(File::isFile)
            ?.toURI()
            ?.toString()
            ?: song.audioUrl

    fun recentlyPlayed(): PagingSource<Int, RecentlyPlayedEntity> = recentlyPlayedDao.pagingSource()

    fun observeRecent(limit: Int = 30): Flow<List<RecentlyPlayedEntity>> = recentlyPlayedDao.observeRecent(limit)

    suspend fun recordPlayed(song: SongDto, positionSeconds: Int = 0) {
        recentlyPlayedDao.upsert(song.toRecentlyPlayedEntity(positionSeconds))
    }

    companion object {
        const val DownloadStateCompleted = "COMPLETED"
        const val DownloadStateQueued = "QUEUED"
        const val DownloadStateRunning = "RUNNING"
        const val DownloadStateFailed = "FAILED"
    }
}
