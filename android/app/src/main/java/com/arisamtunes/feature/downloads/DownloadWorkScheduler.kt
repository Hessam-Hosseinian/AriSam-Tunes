package com.arisamtunes.feature.downloads

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import com.arisamtunes.data.local.LocalLibraryRepository
import com.arisamtunes.data.local.entity.DownloadedSongEntity
import com.arisamtunes.data.preferences.UserPreferencesStore
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.catalog.CatalogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

enum class DownloadEnqueueResult {
    Queued,
    AlreadyQueued,
    AlreadyDownloaded,
    PremiumRequired,
}

@Singleton
class DownloadWorkScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesStore: UserPreferencesStore,
    private val localLibraryRepository: LocalLibraryRepository,
    private val catalogRepository: CatalogRepository,
) {
    suspend fun cancel(songId: String) {
        val ownerUserId = preferencesStore.preferences.first().currentUserId ?: return
        WorkManager.getInstance(context).cancelUniqueWork(downloadWorkName(ownerUserId, songId))
    }

    suspend fun enqueue(song: SongDto): DownloadEnqueueResult {
        val preferences = preferencesStore.preferences.first()
        val ownerUserId = preferences.currentUserId ?: return DownloadEnqueueResult.PremiumRequired
        val workManager = WorkManager.getInstance(context)
        val workName = downloadWorkName(ownerUserId, song.id)
        val hasActiveWork = workManager.getWorkInfosForUniqueWorkFlow(workName)
            .first()
            .any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED }
        val existing = localLibraryRepository.download(song.id)
        val destination = downloadDestination(context, ownerUserId, song.id)
        val decision = downloadEnqueueDecision(
            isPremium = preferences.isPremium,
            existing = existing,
            localFileExists = existing?.localFilePath?.let(::File)?.isFile == true,
            hasActiveWork = hasActiveWork,
        )
        if (decision != DownloadEnqueueResult.Queued) return decision

        val downloadSong = runCatching { catalogRepository.song(song.id) }.getOrDefault(song)

        localLibraryRepository.saveDownload(
            song = downloadSong,
            localFilePath = destination.absolutePath,
            state = LocalLibraryRepository.DownloadStateQueued,
            progress = 0,
        )
        val request = OneTimeWorkRequestBuilder<DownloadSongWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .setInputData(
                DownloadSongWorker.input(
                    songId = downloadSong.id,
                    title = downloadSong.title,
                    artistName = downloadSong.artistName,
                    album = downloadSong.album,
                    audioUrl = downloadSong.audioUrl,
                    coverImageUrl = downloadSong.coverImageUrl,
                    ownerUserId = ownerUserId,
                ),
            )
            .build()
        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return DownloadEnqueueResult.Queued
    }
}

internal fun downloadEnqueueDecision(
    isPremium: Boolean,
    existing: DownloadedSongEntity?,
    localFileExists: Boolean,
    hasActiveWork: Boolean = false,
): DownloadEnqueueResult = when {
    !isPremium -> DownloadEnqueueResult.PremiumRequired
    existing?.downloadState == LocalLibraryRepository.DownloadStateCompleted && localFileExists ->
        DownloadEnqueueResult.AlreadyDownloaded
    hasActiveWork && (
        existing?.downloadState == LocalLibraryRepository.DownloadStateQueued ||
            existing?.downloadState == LocalLibraryRepository.DownloadStateRunning
        ) ->
        DownloadEnqueueResult.AlreadyQueued
    else -> DownloadEnqueueResult.Queued
}

internal fun downloadDestination(context: Context, ownerUserId: String, songId: String): File {
    val safeName = MessageDigest.getInstance("SHA-256")
        .digest("$ownerUserId:$songId".toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }
    return File(File(context.filesDir, "downloads").apply(File::mkdirs), "$safeName.audio")
}

internal fun downloadWorkName(ownerUserId: String, songId: String): String =
    DownloadSongWorker.WorkNamePrefix + ownerUserId + "-" + songId
