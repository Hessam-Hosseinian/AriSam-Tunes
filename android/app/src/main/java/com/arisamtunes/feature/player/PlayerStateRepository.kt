package com.arisamtunes.feature.player

import com.arisamtunes.data.catalog.SongDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class PlayerState(
    val currentSong: SongDto? = null,
    val isPlaying: Boolean = false,
    val progressSeconds: Int = 0,
    val queue: List<SongDto> = emptyList(),
    val playbackSpeed: Float = 1f,
    val sleepTimerEndsAtMillis: Long? = null,
    val isCrossfadeEnabled: Boolean = true,
    val playbackError: String? = null,
    val visualizerBands: List<Float> = List(36) { 0.08f },
    val isFftVisualizerActive: Boolean = false,
    val visualizerPermissionRequired: Boolean = false,
)

@Singleton
class PlayerStateRepository @Inject constructor() {
    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    fun play(song: SongDto, queue: List<SongDto> = emptyList()) {
        _state.update { current ->
            current.copy(currentSong = song, isPlaying = true, progressSeconds = 0, queue = queue.ifEmpty { listOf(song) }, playbackError = null)
        }
    }

    fun togglePlayPause() {
        _state.update { state -> state.copy(isPlaying = !state.isPlaying) }
    }

    fun setPlaying(isPlaying: Boolean) {
        _state.update { state -> state.copy(isPlaying = isPlaying) }
    }

    fun setPlaybackError(message: String?) {
        _state.update { state -> state.copy(playbackError = message, isPlaying = false) }
    }

    fun seekTo(seconds: Int) {
        _state.update { state -> state.copy(progressSeconds = seconds.coerceIn(0, state.currentSong?.durationSeconds ?: 0)) }
    }

    fun setProgress(seconds: Int) {
        _state.update { state -> state.copy(progressSeconds = seconds.coerceIn(0, state.currentSong?.durationSeconds ?: 0)) }
    }

    fun setVisualizerBands(bands: List<Float>) {
        _state.update { state ->
            if (state.visualizerBands.size == bands.size && state.visualizerBands.zip(bands).all { (old, new) -> kotlin.math.abs(old - new) < VisualizerUpdateThreshold }) {
                state
            } else {
                state.copy(visualizerBands = bands)
            }
        }
    }

    fun setFftVisualizerActive(active: Boolean) {
        _state.update { state -> state.copy(isFftVisualizerActive = active) }
    }

    fun setVisualizerPermissionRequired(required: Boolean) {
        _state.update { state -> state.copy(visualizerPermissionRequired = required) }
    }

    fun setPlaybackSpeed(speed: Float) {
        _state.update { state -> state.copy(playbackSpeed = speed.coerceIn(0.5f, 2f)) }
    }

    fun setSleepTimerEndsAt(endsAtMillis: Long?) {
        _state.update { state -> state.copy(sleepTimerEndsAtMillis = endsAtMillis) }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        _state.update { state -> state.copy(isCrossfadeEnabled = enabled) }
    }

    fun close() {
        _state.value = PlayerState()
    }

    companion object {
        fun emptyVisualizerBands(): List<Float> = List(36) { 0.08f }
        private const val VisualizerUpdateThreshold = 0.018f
    }
}
