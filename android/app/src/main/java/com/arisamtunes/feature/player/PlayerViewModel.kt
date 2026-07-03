package com.arisamtunes.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisamtunes.data.catalog.CatalogRepository
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.local.LocalLibraryRepository
import com.arisamtunes.feature.downloads.DownloadWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlayerStateRepository,
    private val playbackController: Media3PlaybackController,
    private val catalogRepository: CatalogRepository,
    private val localLibraryRepository: LocalLibraryRepository,
    private val downloadWorkScheduler: DownloadWorkScheduler,
) : ViewModel() {
    val state = repository.state

    fun play(song: SongDto) = playbackController.play(song)

    fun play(songId: String, onPlayed: () -> Unit) = viewModelScope.launch {
        runCatching { catalogRepository.song(songId) }
            .onSuccess {
                playbackController.play(it)
                onPlayed()
            }
    }

    fun togglePlayPause() = playbackController.togglePlayPause()

    fun skipToNext() = playbackController.skipToNext()

    fun skipToPrevious() = playbackController.skipToPrevious()

    fun retryPlayback() = playbackController.retry()

    fun cyclePlaybackSpeed() = playbackController.cyclePlaybackSpeed()

    fun setSleepTimer(minutes: Int) = playbackController.setSleepTimer(minutes)

    fun toggleCrossfade() = playbackController.toggleCrossfade()

    fun toggleShuffle() = playbackController.toggleShuffle()

    fun cycleRepeatMode() = playbackController.cycleRepeatMode()

    fun observeIsLiked(songId: String) = localLibraryRepository.observeIsLiked(songId)

    fun toggleLike(song: SongDto) = viewModelScope.launch { localLibraryRepository.toggleLiked(song) }

    fun download(song: SongDto) = downloadWorkScheduler.enqueue(song)

    fun seekTo(seconds: Int) = playbackController.seekTo(seconds)

    fun close() = playbackController.close()
}