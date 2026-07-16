package com.arisamtunes.feature.player

import com.arisamtunes.data.catalog.SongDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class PlayerState(
    val currentSong: SongDto? = null,
    val crossfadeSong: SongDto? = null,
    val isPlaying: Boolean = false,
    val progressSeconds: Int = 0,
    val progressMillis: Long = 0L,
    val crossfadeProgressSeconds: Int = 0,
    val crossfadeProgressMillis: Long = 0L,
    val queue: List<SongDto> = emptyList(),
    val originalQueue: List<SongDto> = emptyList(),
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val playbackSpeed: Float = 1f,
    val sleepTimerEndsAtMillis: Long? = null,
    val isCrossfadeEnabled: Boolean = true,
    val playbackError: String? = null,
    val visualizerBands: List<Float> = List(32) { 0.08f },
)

@Singleton
class PlayerStateRepository @Inject constructor() {
    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    fun play(song: SongDto, queue: List<SongDto> = emptyList(), replaceQueue: Boolean = false) {
        _state.update { current ->
            val playbackQueue = when {
                replaceQueue -> queue.normalizeQueue(song)
                current.queue.isNotEmpty() -> current.queue
                else -> queue.normalizeQueue(song)
            }
            current.copy(
                currentSong = song,
                crossfadeSong = null,
                isPlaying = true,
                progressSeconds = 0,
                progressMillis = 0L,
                crossfadeProgressSeconds = 0,
                crossfadeProgressMillis = 0L,
                queue = playbackQueue,
                originalQueue = if (replaceQueue || current.originalQueue.isEmpty()) playbackQueue else current.originalQueue,
                isShuffleEnabled = if (replaceQueue) false else current.isShuffleEnabled,
                playbackError = null,
            )
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
        seekToMillis(seconds.toLong() * 1_000L)
    }

    fun seekToMillis(positionMillis: Long) {
        _state.update { state ->
            val safeMillis = positionMillis.coerceIn(0L, state.currentSong.durationMillis())
            state.copy(progressSeconds = (safeMillis / 1_000L).toInt(), progressMillis = safeMillis)
        }
    }

    fun setProgress(seconds: Int) {
        _state.update { state ->
            val safeSeconds = seconds.coerceIn(0, state.currentSong?.durationSeconds ?: 0)
            state.copy(progressSeconds = safeSeconds, progressMillis = safeSeconds * 1_000L)
        }
    }

    fun setProgressMillis(millis: Long) {
        _state.update { state ->
            val safeMillis = millis.coerceIn(0L, (state.currentSong?.durationSeconds ?: 0) * 1_000L)
            state.copy(progressSeconds = (safeMillis / 1_000L).toInt(), progressMillis = safeMillis)
        }
    }

    fun setCrossfadePreview(song: SongDto?, seconds: Int = 0, millis: Long = seconds * 1_000L) {
        _state.update { state ->
            val safeMillis = millis.coerceIn(0L, (song?.durationSeconds ?: 0) * 1_000L)
            state.copy(
                crossfadeSong = song,
                crossfadeProgressSeconds = (safeMillis / 1_000L).toInt(),
                crossfadeProgressMillis = safeMillis,
            )
        }
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

    fun setPlaybackSpeed(speed: Float) {
        _state.update { state -> state.copy(playbackSpeed = speed.coerceIn(0.5f, 2f)) }
    }

    fun setSleepTimerEndsAt(endsAtMillis: Long?) {
        _state.update { state -> state.copy(sleepTimerEndsAtMillis = endsAtMillis) }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        _state.update { state -> state.copy(isCrossfadeEnabled = enabled) }
    }

    fun toggleShuffle() {
        _state.update { state ->
            val current = state.currentSong
            if (!state.isShuffleEnabled) {
                state.copy(
                    queue = listOfNotNull(current) + state.queue.filterNot { it.id == current?.id }.shuffled(),
                    originalQueue = state.queue,
                    isShuffleEnabled = true,
                )
            } else {
                state.copy(queue = state.originalQueue.ifEmpty { state.queue }, isShuffleEnabled = false)
            }
        }
    }

    fun setRepeatMode(mode: Int) {
        _state.update { state -> state.copy(repeatMode = mode.coerceIn(0, 2)) }
    }

    fun close() {
        _state.value = PlayerState()
    }

    companion object {
        fun emptyVisualizerBands(): List<Float> = List(32) { 0.05f }
        private const val VisualizerUpdateThreshold = 0.003f
    }
}

internal fun List<SongDto>.normalizeQueue(current: SongDto): List<SongDto> =
    (listOf(current) + this).distinctBy(SongDto::id)

internal fun List<SongDto>.nextFor(song: SongDto, repeatMode: Int): SongDto? {
    if (isEmpty()) return null
    if (repeatMode == 2) return song
    val currentIndex = indexOfFirst { it.id == song.id }
    if (currentIndex < 0) return null
    return when {
        currentIndex < lastIndex -> this[currentIndex + 1]
        repeatMode == 1 -> first()
        else -> null
    }
}

internal fun List<SongDto>.previousFor(song: SongDto, repeatMode: Int): SongDto? {
    if (isEmpty()) return null
    if (repeatMode == 2) return song
    val currentIndex = indexOfFirst { it.id == song.id }
    if (currentIndex < 0) return null
    return when {
        currentIndex > 0 -> this[currentIndex - 1]
        repeatMode == 1 -> last()
        else -> null
    }
}

private fun SongDto?.durationMillis(): Long =
    (this?.durationSeconds ?: 0).toLong().coerceAtLeast(0L) * 1_000L
