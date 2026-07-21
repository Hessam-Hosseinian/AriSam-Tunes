package com.arisamtunes.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisamtunes.data.catalog.CatalogRepository
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.catalog.SongSpectrumDto
import com.arisamtunes.data.local.LocalLibraryRepository
import com.arisamtunes.feature.downloads.DownloadWorkScheduler
import com.arisamtunes.feature.downloads.DownloadEnqueueResult
import com.arisamtunes.data.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlayerStateRepository,
    private val playbackController: Media3PlaybackController,
    private val catalogRepository: CatalogRepository,
    private val localLibraryRepository: LocalLibraryRepository,
    private val downloadWorkScheduler: DownloadWorkScheduler,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val state = repository.state
    val isPremium = preferencesRepository.preferences
        .map { it.isPremium }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    private val _downloadResults = MutableSharedFlow<DownloadEnqueueResult>(extraBufferCapacity = 1)
    val downloadResults = _downloadResults.asSharedFlow()

    fun play(song: SongDto) = playbackController.play(song)

    fun play(song: SongDto, queue: List<SongDto>) = playbackController.play(song, queue)

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

    fun setPlaybackSpeed(speed: Float) = playbackController.setPlaybackSpeed(speed)

    fun setSleepTimer(minutes: Int) = playbackController.setSleepTimer(minutes)

    fun toggleCrossfade() = playbackController.toggleCrossfade()

    fun toggleShuffle() = playbackController.toggleShuffle()

    fun cycleRepeatMode() = playbackController.cycleRepeatMode()

    fun observeIsLiked(songId: String) = localLibraryRepository.observeIsLiked(songId)

    fun observeDownload(songId: String) = localLibraryRepository.observeDownload(songId)

    fun toggleLike(song: SongDto) = viewModelScope.launch { localLibraryRepository.toggleLiked(song) }

    fun download(song: SongDto) = viewModelScope.launch {
        _downloadResults.emit(downloadWorkScheduler.enqueue(song))
    }

    fun seekTo(seconds: Int) = playbackController.seekTo(seconds)

    fun seekToMillis(positionMillis: Long) = playbackController.seekToMillis(positionMillis)

    suspend fun songSpectrum(songId: String): SongSpectrumDto? =
        runCatching { catalogRepository.songSpectrum(songId) }.getOrNull()

    fun close() = playbackController.close()
}
