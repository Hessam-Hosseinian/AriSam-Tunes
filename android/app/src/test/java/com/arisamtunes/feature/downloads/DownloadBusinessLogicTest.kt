package com.arisamtunes.feature.downloads

import com.arisamtunes.data.local.LocalLibraryRepository
import com.arisamtunes.data.local.entity.DownloadedSongEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadBusinessLogicTest {
    @Test
    fun workNames_areIsolatedBetweenAccounts() {
        val first = downloadWorkName("account-a", "song-id")
        val second = downloadWorkName("account-b", "song-id")

        org.junit.Assert.assertNotEquals(first, second)
    }

    @Test
    fun regularListener_cannotQueueOfflineDownload() {
        assertEquals(
            DownloadEnqueueResult.PremiumRequired,
            downloadEnqueueDecision(isPremium = false, existing = null, localFileExists = false),
        )
    }

    @Test
    fun premiumListener_doesNotDuplicateCompletedOrActiveDownloads() {
        assertEquals(
            DownloadEnqueueResult.AlreadyDownloaded,
            downloadEnqueueDecision(true, download(LocalLibraryRepository.DownloadStateCompleted), true),
        )
        assertEquals(
            DownloadEnqueueResult.AlreadyQueued,
            downloadEnqueueDecision(
                isPremium = true,
                existing = download(LocalLibraryRepository.DownloadStateRunning),
                localFileExists = false,
                hasActiveWork = true,
            ),
        )
    }

    @Test
    fun premiumListener_canRetryFailedOrPhysicallyMissingDownload() {
        assertEquals(
            DownloadEnqueueResult.Queued,
            downloadEnqueueDecision(true, download(LocalLibraryRepository.DownloadStateFailed), false),
        )
        assertEquals(
            DownloadEnqueueResult.Queued,
            downloadEnqueueDecision(true, download(LocalLibraryRepository.DownloadStateCompleted), false),
        )
        assertEquals(
            DownloadEnqueueResult.Queued,
            downloadEnqueueDecision(
                isPremium = true,
                existing = download(LocalLibraryRepository.DownloadStateQueued),
                localFileExists = false,
                hasActiveWork = false,
            ),
        )
    }

    private fun download(state: String) = DownloadedSongEntity(
        ownerUserId = "account-a",
        songId = "song-id",
        title = "Song",
        artistName = "Artist",
        audioUrl = "https://example.com/song.mp3",
        localFilePath = "/downloads/song.audio",
        downloadState = state,
        downloadedAt = 1L,
    )
}
