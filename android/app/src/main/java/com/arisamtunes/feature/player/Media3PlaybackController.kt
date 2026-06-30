package com.arisamtunes.feature.player

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import com.arisamtunes.data.catalog.CatalogRepository
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.local.LocalLibraryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class Media3PlaybackController @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val stateRepository: PlayerStateRepository,
    private val localLibraryRepository: LocalLibraryRepository,
    private val catalogRepository: CatalogRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val streamCache = SimpleCache(
        File(appContext.cacheDir, "media3-stream-cache"),
        LeastRecentlyUsedCacheEvictor(MaxStreamCacheBytes),
        StandaloneDatabaseProvider(appContext),
    )
    private val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(streamCache)
        .setUpstreamDataSourceFactory(OkHttpDataSource.Factory(OkHttpClient()))
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    private val realtimeVisualizers = CopyOnWriteArrayList<RealtimeFftAudioBufferSink>()
    private val renderersFactory = FftRenderersFactory(
        appContext,
        onVisualizerCreated = realtimeVisualizers::add,
        onBands = stateRepository::setVisualizerBands,
    )
    private val player = ExoPlayer.Builder(appContext, renderersFactory)
        .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
        .build()
    private val crossfadeCoordinator = CrossfadePlaybackCoordinator(appContext, cacheDataSourceFactory, renderersFactory) {
        if (secondaryActiveSong != null) skipToNext()
    }
    private var sleepTimerJob: Job? = null
    private var transitionJob: Job? = null
    private var mediaSession: MediaSession? = null
    private var lastFallbackVisualizerAtMillis = 0L
    private var secondaryActiveSong: SongDto? = null

    init {
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true,
        )
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (secondaryActiveSong == null && stateRepository.state.value.crossfadeSong == null) {
                    stateRepository.setPlaying(isPlaying)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (secondaryActiveSong == null) {
                    stateRepository.setProgress(player.currentPosition.toInt() / 1000)
                }
                if (playbackState == Player.STATE_ENDED && transitionJob?.isActive != true && secondaryActiveSong == null) skipToNext()
            }

            override fun onPlayerError(error: PlaybackException) {
                stateRepository.setPlaybackError(error.localizedMessage ?: "Playback failed")
            }
        })
        scope.launch {
            while (true) {
                val activeProgress = activePositionMillis()
                stateRepository.setProgressMillis(activeProgress)
                updateFallbackVisualizer()
                maybeStartCrossfade()
                delay(120)
            }
        }
    }

    fun play(song: SongDto, queue: List<SongDto> = emptyList()) {
        scope.launch {
            val playbackQueue = resolvePlaybackQueue(song, queue)
            playResolved(song, playbackQueue, startPositionMillis = 0L, cancelTransition = true)
        }
    }

    private suspend fun playResolved(
        song: SongDto,
        queue: List<SongDto>,
        startPositionMillis: Long,
        cancelTransition: Boolean,
        initialVolume: Float = 1f,
    ) {
        stateRepository.play(song, queue)
        runCatching {
            if (cancelTransition) {
                transitionJob?.cancel()
                crossfadeCoordinator.clear()
                secondaryActiveSong = null
            }
            localLibraryRepository.recordPlayed(song)
            startPlaybackServiceSafely()
            val playbackSource = localLibraryRepository.playbackSource(song)
            player.volume = initialVolume.coerceIn(0f, 1f)
            player.setMediaItem(
                MediaItem.Builder()
                    .setUri(playbackSource)
                    .setMediaId(song.id)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artistName)
                            .setAlbumTitle(song.album)
                            .setArtworkUri(android.net.Uri.parse(song.coverImageUrl))
                            .build(),
                    )
                    .build(),
            )
            player.prepare()
            if (startPositionMillis > 0L) player.seekTo(startPositionMillis)
            player.play()
            prepareCrossfadeNext(song, queue)
        }.onFailure { error ->
            player.volume = 1f
            crossfadeCoordinator.clear()
            stateRepository.setPlaybackError(error.localizedMessage ?: "Playback failed")
        }
    }

    private suspend fun resolvePlaybackQueue(song: SongDto, requestedQueue: List<SongDto>): List<SongDto> {
        val source = requestedQueue.ifEmpty {
            runCatching {
                catalogRepository.songs(page = 0, size = 100).items
            }.getOrDefault(emptyList())
        }
        val unique = (listOf(song) + source).distinctBy(SongDto::id)
        val selectedIndex = unique.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        return unique.drop(selectedIndex) + unique.take(selectedIndex)
    }

    fun retry() {
        stateRepository.state.value.currentSong?.let(::play)
    }

    fun refreshPlaybackState() {
        stateRepository.setPlaying(player.isPlaying)
        stateRepository.setProgress(player.currentPosition.toInt() / 1000)
    }

    fun clearError() {
        stateRepository.setPlaybackError(null)
    }

    fun safePause() {
        runCatching {
            if (transitionJob?.isActive == true) {
                player.pause()
                crossfadeCoordinator.pause()
            } else if (secondaryActiveSong != null) {
                crossfadeCoordinator.pause()
            } else {
                player.pause()
            }
        }
        stateRepository.setPlaying(false)
    }

    private fun safePlay() {
        runCatching {
            if (transitionJob?.isActive == true) {
                player.play()
                crossfadeCoordinator.play()
            } else if (secondaryActiveSong != null) {
                crossfadeCoordinator.play()
            } else {
                player.volume = 1f
                player.play()
            }
        }
            .onFailure { stateRepository.setPlaybackError(it.localizedMessage ?: "Playback failed") }
    }

    fun togglePlayPause() {
        if (activeIsPlaying()) {
            safePause()
        } else {
            safePlay()
            stateRepository.setPlaying(activeIsPlaying())
        }
    }

    fun skipToNext() {
        val state = stateRepository.state.value
        val current = state.currentSong ?: return
        val queue = state.queue
        val next = queue.nextAfter(current) ?: return
        scope.launch { playResolved(next, queue, startPositionMillis = 0L, cancelTransition = true) }
    }

    fun skipToPrevious() {
        val state = stateRepository.state.value
        val current = state.currentSong ?: return
        if (player.currentPosition > RestartPreviousThresholdMillis) {
            seekTo(0)
            return
        }
        val queue = state.queue
        val currentIndex = queue.indexOfFirst { it.id == current.id }
        if (queue.size <= 1 || currentIndex < 0) return
        val previous = queue[Math.floorMod(currentIndex - 1, queue.size)]
        scope.launch { playResolved(previous, queue, startPositionMillis = 0L, cancelTransition = true) }
    }

    fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.5f, 2f)
        runCatching {
            player.playbackParameters = PlaybackParameters(safeSpeed)
            // Keep both decks aligned so playback speed survives crossfade swaps.
            // Secondary no-ops if it has no prepared media.
            runCatching { crossfadeCoordinator.playbackSpeed(safeSpeed) }
            stateRepository.setPlaybackSpeed(safeSpeed)
        }.onFailure {
            stateRepository.setPlaybackError(it.localizedMessage ?: "Could not change playback speed")
        }
    }

    fun cyclePlaybackSpeed() {
        val current = stateRepository.state.value.playbackSpeed
        val next = when {
            current < 1f -> 1f
            current < 1.25f -> 1.25f
            current < 1.5f -> 1.5f
            current < 2f -> 2f
            else -> 0.75f
        }
        setPlaybackSpeed(next)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            stateRepository.setSleepTimerEndsAt(null)
            return
        }
        val durationMillis = minutes * 60_000L
        val endsAt = System.currentTimeMillis() + durationMillis
        stateRepository.setSleepTimerEndsAt(endsAt)
        sleepTimerJob = scope.launch {
            delay(durationMillis)
            safePause()
            stateRepository.setSleepTimerEndsAt(null)
        }
    }

    fun toggleCrossfade() {
        val enabled = !stateRepository.state.value.isCrossfadeEnabled
        stateRepository.setCrossfadeEnabled(enabled)
        if (!enabled) {
            transitionJob?.cancel()
            player.volume = 1f
            if (secondaryActiveSong == null) crossfadeCoordinator.clear()
            stateRepository.setCrossfadePreview(null)
        }
        val state = stateRepository.state.value
        state.currentSong?.let { current ->
            scope.launch { prepareCrossfadeNext(current, state.queue) }
        }
    }

    fun seekTo(seconds: Int) {
        runCatching {
            val positionMillis = seconds * 1000L
            val previewOnSecondary = stateRepository.state.value.crossfadeSong != null && secondaryActiveSong == null
            val previewOnPrimary = stateRepository.state.value.crossfadeSong != null && secondaryActiveSong != null
            if (previewOnSecondary || secondaryActiveSong != null && !previewOnPrimary) {
                crossfadeCoordinator.seekTo(positionMillis)
                stateRepository.state.value.crossfadeSong?.let { stateRepository.setCrossfadePreview(it, seconds) }
            } else {
                player.seekTo(positionMillis)
            }
            stateRepository.seekTo(seconds)
        }.onFailure {
            stateRepository.setPlaybackError(it.localizedMessage ?: "Seek failed")
        }
    }

    fun close() {
        sleepTimerJob?.cancel()
        transitionJob?.cancel()
        crossfadeCoordinator.clear()
        secondaryActiveSong = null
        runCatching {
            player.volume = 1f
            player.stop()
            player.clearMediaItems()
        }
        stateRepository.close()
    }

    fun mediaSession(context: Context): MediaSession =
        mediaSession ?: MediaSession.Builder(context, player)
            .setId(MediaSessionId)
            .build()
            .also { mediaSession = it }

    fun releaseMediaSession() {
        mediaSession?.release()
        mediaSession = null
    }

    private fun startPlaybackServiceSafely() {
        val intent = Intent(appContext, AriSamPlaybackService::class.java)
        runCatching {
            // Avoid startForegroundService here: MediaSessionService may not promote
            // itself within Android's 5s window when playback starts from Compose,
            // which can crash the app. A normal start keeps foreground playback
            // stable while the MediaSession remains available to system controls.
            appContext.startService(intent)
        }.onFailure { error ->
            Log.w("Media3PlaybackController", "Playback service could not be started", error)
        }
    }

    private fun updateFallbackVisualizer() {
        val now = android.os.SystemClock.elapsedRealtime()
        val hasFreshAudioSignal = realtimeVisualizers.any { now - it.lastBandUpdateMillis < FreshSignalMillis }
        if (!hasFreshAudioSignal && now - lastFallbackVisualizerAtMillis >= FallbackUpdateIntervalMillis) {
            lastFallbackVisualizerAtMillis = now
            realtimeVisualizers.firstOrNull()?.let { stateRepository.setVisualizerBands(it.decayedBands()) }
        }
    }

    private fun maybeStartCrossfade() {
        val state = stateRepository.state.value
        if (!state.isCrossfadeEnabled || transitionJob?.isActive == true) return
        val current = state.currentSong ?: return
        val next = state.queue.nextAfter(current) ?: return
        val duration = activeDurationMillis()
        val position = activePositionMillis()
        val isPlaying = if (secondaryActiveSong != null) crossfadeCoordinator.isPlaying() else player.isPlaying
        if (!isPlaying || duration <= CrossfadeDurationMillis || position < duration - CrossfadeDurationMillis) return
        transitionJob = scope.launch {
            runCatching {
                if (secondaryActiveSong == null) {
                    prepareCrossfadeNext(current, state.queue)
                    if (!crossfadeCoordinator.startPrepared(next)) {
                        playResolved(next, state.queue, startPositionMillis = 0L, cancelTransition = true)
                        return@launch
                    }
                    stateRepository.setCrossfadePreview(next, seconds = 0)
                    blendVolumes(primaryToSecondary = true, durationMillis = CrossfadeDurationMillis)
                    secondaryActiveSong = next
                    player.stop()
                    player.clearMediaItems()
                    player.volume = 1f
                } else {
                    preparePrimaryDeck(next)
                    stateRepository.setCrossfadePreview(next, seconds = 0)
                    blendVolumes(primaryToSecondary = false, durationMillis = CrossfadeDurationMillis)
                    crossfadeCoordinator.clear()
                    secondaryActiveSong = null
                }
                stateRepository.play(next, state.queue)
                localLibraryRepository.recordPlayed(next)
                stateRepository.setProgress((activePositionMillis() / 1000L).toInt())
                stateRepository.setCrossfadePreview(null)
                prepareCrossfadeNext(next, state.queue)
            }.onFailure {
                player.volume = 1f
                crossfadeCoordinator.clear()
                secondaryActiveSong = null
                stateRepository.setCrossfadePreview(null)
                stateRepository.setPlaybackError(it.localizedMessage ?: "Crossfade failed")
            }
        }
    }

    private suspend fun prepareCrossfadeNext(current: SongDto, queue: List<SongDto>) {
        if (!stateRepository.state.value.isCrossfadeEnabled || secondaryActiveSong != null) return
        val next = queue.nextAfter(current) ?: return
        val nextSource = localLibraryRepository.playbackSource(next)
        crossfadeCoordinator.prepareNext(next, nextSource, enabled = true)
    }

    private suspend fun preparePrimaryDeck(song: SongDto) {
        val source = localLibraryRepository.playbackSource(song)
        player.volume = 0f
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(source)
                .setMediaId(song.id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artistName)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(android.net.Uri.parse(song.coverImageUrl))
                        .build(),
                )
                .build(),
        )
        player.prepare()
        player.play()
    }

    private suspend fun blendVolumes(primaryToSecondary: Boolean, durationMillis: Long) {
        val steps = 40
        val stepDelay = durationMillis / steps
        repeat(steps + 1) { step ->
            val fraction = step / steps.toFloat()
            if (primaryToSecondary) {
                player.volume = 1f - fraction
                crossfadeCoordinator.setVolume(fraction)
            } else {
                crossfadeCoordinator.setVolume(1f - fraction)
                player.volume = fraction
            }
            stateRepository.setCrossfadePreview(
                stateRepository.state.value.crossfadeSong,
                seconds = (previewPositionMillis() / 1000L).toInt(),
                millis = previewPositionMillis(),
            )
            delay(stepDelay)
        }
        if (primaryToSecondary) {
            player.volume = 0f
            crossfadeCoordinator.setVolume(1f)
        } else {
            crossfadeCoordinator.setVolume(0f)
            player.volume = 1f
        }
    }

    private fun activePositionMillis(): Long = if (secondaryActiveSong != null) crossfadeCoordinator.currentPositionMillis() else player.currentPosition.coerceAtLeast(0L)

    private fun activeDurationMillis(): Long = if (secondaryActiveSong != null) crossfadeCoordinator.durationMillis() else player.duration

    private fun previewPositionMillis(): Long = if (secondaryActiveSong == null) crossfadeCoordinator.currentPositionMillis() else player.currentPosition.coerceAtLeast(0L)

    private fun activeIsPlaying(): Boolean = if (secondaryActiveSong != null) crossfadeCoordinator.isPlaying() else player.isPlaying

    private class FftRenderersFactory(
        context: Context,
        private val onVisualizerCreated: (RealtimeFftAudioBufferSink) -> Unit,
        private val onBands: (List<Float>) -> Unit,
    ) : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean,
        ): AudioSink {
            val visualizer = RealtimeFftAudioBufferSink(onBands)
            onVisualizerCreated(visualizer)
            return DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf(TeeAudioProcessor(visualizer)))
                .build()
        }
    }

    private companion object {
        const val MediaSessionId = "arisam_tunes_playback"
        const val MaxStreamCacheBytes = 256L * 1024L * 1024L
        const val FallbackUpdateIntervalMillis = 80L
        const val FreshSignalMillis = 180L
        const val RestartPreviousThresholdMillis = 3_000L
        const val CrossfadeDurationMillis = 10_000L
    }
}

private fun List<SongDto>.nextAfter(song: SongDto): SongDto? {
    if (size <= 1) return null
    val currentIndex = indexOfFirst { it.id == song.id }
    if (currentIndex < 0) return null
    return this[(currentIndex + 1) % size]
}
