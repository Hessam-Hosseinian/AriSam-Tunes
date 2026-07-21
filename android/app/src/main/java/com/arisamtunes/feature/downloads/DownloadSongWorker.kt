package com.arisamtunes.feature.downloads

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.arisamtunes.data.local.AriSamDatabase
import com.arisamtunes.data.local.LocalLibraryRepository
import com.arisamtunes.data.local.entity.DownloadedSongEntity
import com.arisamtunes.data.preferences.UserPreferencesStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@HiltWorker
class DownloadSongWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val database: AriSamDatabase,
    private val preferencesStore: UserPreferencesStore,
    private val client: OkHttpClient,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songId = inputData.getString(KeySongId) ?: return@withContext Result.failure()
        val title = inputData.getString(KeyTitle) ?: return@withContext Result.failure()
        val artistName = inputData.getString(KeyArtistName) ?: return@withContext Result.failure()
        val audioUrl = inputData.getString(KeyAudioUrl) ?: return@withContext Result.failure()
        val ownerUserId = inputData.getString(KeyOwnerUserId) ?: return@withContext Result.failure()
        val coverImageUrl = inputData.getString(KeyCoverImageUrl)
        val album = inputData.getString(KeyAlbum)

        val dao = database.downloadedSongDao()
        val output = downloadDestination(appContext, ownerUserId, songId)
        val temporaryOutput = File(output.parentFile, "${output.name}.part")
        val preferences = preferencesStore.preferences.first()
        if (!preferences.isPremium || preferences.currentUserId != ownerUserId) {
            dao.updateState(ownerUserId, songId, LocalLibraryRepository.DownloadStateFailed, 0, FailurePremiumRequired)
            return@withContext Result.failure(Data.Builder().putString(KeyFailureReason, FailurePremiumRequired).build())
        }

        val outcome = runCatching {
            val runningDownload = dao.get(ownerUserId, songId)?.copy(
                downloadState = LocalLibraryRepository.DownloadStateRunning,
                downloadProgress = 0,
                failureReason = null,
            ) ?: DownloadedSongEntity(
                    ownerUserId = ownerUserId,
                    songId = songId,
                    title = title,
                    artistName = artistName,
                    album = album,
                    audioUrl = audioUrl,
                    coverImageUrl = coverImageUrl,
                    localFilePath = output.absolutePath,
                    downloadState = LocalLibraryRepository.DownloadStateRunning,
                    downloadProgress = 0,
                    downloadedAt = System.currentTimeMillis(),
            )
            dao.upsert(runningDownload)
            temporaryOutput.delete()
            client.newCall(Request.Builder().url(audioUrl).build()).execute().use { response ->
                if (!response.isSuccessful) throw DownloadHttpException(response.code)
                val body = response.body ?: throw IOException("Download response had no body")
                val contentLength = body.contentLength().takeIf { it > 0L }
                body.byteStream().use { input ->
                    temporaryOutput.outputStream().use { outputStream ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var copied = 0L
                        var lastProgress = -1
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            outputStream.write(buffer, 0, count)
                            copied += count
                            val progress = contentLength
                                ?.let { ((copied * 100L) / it).toInt().coerceIn(0, 99) }
                                ?: 0
                            if (progress != lastProgress) {
                                lastProgress = progress
                                dao.updateProgress(ownerUserId, songId, progress)
                                setProgress(Data.Builder().putInt(KeyProgress, progress).build())
                            }
                        }
                    }
                }
                if (!temporaryOutput.renameTo(output)) {
                    temporaryOutput.copyTo(output, overwrite = true)
                    temporaryOutput.delete()
                }
                check(output.isFile && output.length() > 0L) { "Downloaded file is empty" }
                dao.upsert(
                    runningDownload.copy(
                        mimeType = body.contentType()?.toString(),
                        fileSizeBytes = output.length(),
                        downloadState = LocalLibraryRepository.DownloadStateCompleted,
                        downloadProgress = 100,
                        failureReason = null,
                        downloadedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
        outcome.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                if (error is CancellationException) throw error
                temporaryOutput.delete()
                val reason = error.downloadFailureReason()
                dao.updateState(ownerUserId, songId, LocalLibraryRepository.DownloadStateFailed, 0, reason)
                if (error.isRetryable() && runAttemptCount < MaxAutomaticRetries) {
                    Result.retry()
                } else {
                    Result.failure(Data.Builder().putString(KeyFailureReason, reason).build())
                }
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
        const val KeyOwnerUserId = "owner_user_id"
        const val KeyProgress = "progress"
        const val KeyFailureReason = "failure_reason"
        const val FailurePremiumRequired = "PREMIUM_REQUIRED"
        const val FailureNetwork = "NETWORK_ERROR"
        const val FailureHttp = "HTTP_ERROR"
        const val FailureStorage = "STORAGE_ERROR"
        private const val MaxAutomaticRetries = 3
        fun input(
            songId: String,
            title: String,
            artistName: String,
            album: String?,
            audioUrl: String,
            coverImageUrl: String?,
            ownerUserId: String,
        ): Data = Data.Builder()
            .putString(KeySongId, songId)
            .putString(KeyTitle, title)
            .putString(KeyArtistName, artistName)
            .putString(KeyAlbum, album)
            .putString(KeyAudioUrl, audioUrl)
            .putString(KeyCoverImageUrl, coverImageUrl)
            .putString(KeyOwnerUserId, ownerUserId)
            .build()
    }
}

private class DownloadHttpException(val statusCode: Int) : IOException("Download failed with HTTP $statusCode")

private fun Throwable.isRetryable(): Boolean = when (this) {
    is DownloadHttpException -> statusCode == 408 || statusCode == 429 || statusCode >= 500
    is IOException -> true
    else -> false
}

private fun Throwable.downloadFailureReason(): String = when (this) {
    is DownloadHttpException -> "${DownloadSongWorker.FailureHttp}_$statusCode"
    is IOException -> DownloadSongWorker.FailureNetwork
    else -> DownloadSongWorker.FailureStorage
}
