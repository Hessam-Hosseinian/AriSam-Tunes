package com.arisamtunes.feature.downloads

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.arisamtunes.data.catalog.SongDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadWorkScheduler @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun enqueue(song: SongDto) {
        val request = OneTimeWorkRequestBuilder<DownloadSongWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(
                DownloadSongWorker.input(
                    songId = song.id,
                    title = song.title,
                    artistName = song.artistName,
                    album = song.album,
                    audioUrl = song.audioUrl,
                    coverImageUrl = song.coverImageUrl,
                ),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            DownloadSongWorker.WorkNamePrefix + song.id,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
