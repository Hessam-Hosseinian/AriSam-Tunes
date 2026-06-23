package com.arisamtunes.feature.player

import androidx.lifecycle.ViewModel
import com.arisamtunes.data.catalog.SongDto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlayerStateRepository,
    private val playbackController: Media3PlaybackController,
) : ViewModel() {
    val state = repository.state

    fun play(song: SongDto) = playbackController.play(song)

    fun togglePlayPause() = playbackController.togglePlayPause()

    fun retryPlayback() = playbackController.retry()

    fun setFftVisualizerPermissionGranted(granted: Boolean) = playbackController.setFftVisualizerPermissionGranted(granted)

    fun cyclePlaybackSpeed() = playbackController.cyclePlaybackSpeed()

    fun setSleepTimer(minutes: Int) = playbackController.setSleepTimer(minutes)

    fun toggleCrossfade() = playbackController.toggleCrossfade()

    fun seekTo(seconds: Int) = playbackController.seekTo(seconds)

    fun close() = playbackController.close()
}
