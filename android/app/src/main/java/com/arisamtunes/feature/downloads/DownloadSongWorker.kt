package com.arisamtunes.feature.downloads

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.arisamtunes.data.local.AriSamDatabase
import com.arisamtunes.data.local.LocalLibraryRepository
import com.arisamtunes.data.local.entity.DownloadedSongEntity
import java.io.IOException
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class DownloadSongWorker(
    private val appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songId = inputData.getString(KeySongId) ?: return@withContext Result.failure()
        val title = inputData.getString(KeyTitle) ?: return@withContext Result.failure()
        val artistName = inputData.getString(KeyArtistName) ?: return@withContext Result.failure()
        val audioUrl = inputData.getString(KeyAudioUrl) ?: return@withContext Result.failure()
        val coverImageUrl = inputData.getString(KeyCoverImageUrl)
        val album = inputData.getString(KeyAlbum)

        val database = Room.databaseBuilder(appContext, AriSamDatabase::class.java, "arisam_tunes.db").build()
        val dao = database.downloadedSongDao()
        val downloadsDir = File(appContext.filesDir, "downloads").apply { mkdirs() }
        val output = File(downloadsDir, "$songId.audio")
        val temporaryOutput = File(downloadsDir, "$songId.audio.part")
        val outcome = runCatching {
            dao.upsert(
                DownloadedSongEntity(
                    songId = songId,
                    title = title,
                    artistName = artistName,
                    album = album,
                    audioUrl = audioUrl,
                    coverImageUrl = coverImageUrl,
                    localFilePath = output.absolutePath,
                    downloadState = LocalLibraryRepository.DownloadStateRunning,
                    downloadedAt = System.currentTimeMillis(),
                ),
            )
            temporaryOutput.delete()
            client.newCall(Request.Builder().url(audioUrl).build()).execute().use { response ->
                if (!response.isSuccessful) throw DownloadHttpException(response.code)
                val body = response.body ?: throw IOException("Download response had no body")
                body.byteStream().use { input ->
                    temporaryOutput.outputStream().use { outputStream -> input.copyTo(outputStream) }
                }
                if (!temporaryOutput.renameTo(output)) {
                    temporaryOutput.copyTo(output, overwrite = true)
                    temporaryOutput.delete()
                }
                dao.upsert(
                    DownloadedSongEntity(
                        songId = songId,
                        title = title,
                        artistName = artistName,
                        album = album,
                        audioUrl = audioUrl,
                        coverImageUrl = coverImageUrl,
                        localFilePath = output.absolutePath,
                        mimeType = body.contentType()?.toString(),
                        fileSizeBytes = output.length(),
                        downloadState = LocalLibraryRepository.DownloadStateCompleted,
                        downloadedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
        outcome.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                temporaryOutput.delete()
                dao.updateState(songId, LocalLibraryRepository.DownloadStateFailed)
                if (error is IOException) Result.retry() else Result.failure()
            },
        )
    }

    companion object {
        const val WorkNamePrefix = "download-song-"
        const val KeySongId = "song_id"
        const val KeyTitle = "title"
        const val KeyArtistName = "artist_name"
        const val KeyAlbum = "album"
        const val KeyAudioUrl = "audio_url"
        const val KeyCoverImageUrl = "cover_image_url"
        private val client = OkHttpClient()

        fun input(
            songId: String,
            title: String,
            artistName: String,
            album: String?,
            audioUrl: String,
            coverImageUrl: String?,
        ): Data = Data.Builder()
            .putString(KeySongId, songId)
            .putString(KeyTitle, title)
            .putString(KeyArtistName, artistName)
            .putString(KeyAlbum, album)
            .putString(KeyAudioUrl, audioUrl)
            .putString(KeyCoverImageUrl, coverImageUrl)
            .build()
    }
}

private class DownloadHttpException(statusCode: Int) : RuntimeException("Download failed with HTTP $statusCode")
