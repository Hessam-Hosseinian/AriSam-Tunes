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
    private val realtimeVisualizer = RealtimeFftAudioBufferSink(stateRepository::setVisualizerBands)
    private val renderersFactory = FftRenderersFactory(
        appContext,
        DefaultAudioSink.Builder(appContext)
            .setAudioProcessors(arrayOf(TeeAudioProcessor(realtimeVisualizer)))
            .build(),
    )
    private val player = ExoPlayer.Builder(appContext, renderersFactory)
        .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
        .build()
    private val crossfadeCoordinator = CrossfadePlaybackCoordinator(appContext, cacheDataSourceFactory)
    private var sleepTimerJob: Job? = null
    private var mediaSession: MediaSession? = null
    private var lastFallbackVisualizerAtMillis = 0L

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
                stateRepository.setPlaying(isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                stateRepository.setProgress(player.currentPosition.toInt() / 1000)
                if (playbackState == Player.STATE_ENDED) skipToNext()
            }

            override fun onPlayerError(error: PlaybackException) {
                stateRepository.setPlaybackError(error.localizedMessage ?: "Playback failed")
            }
        })
        scope.launch {
            while (true) {
                stateRepository.setProgress(player.currentPosition.toInt() / 1000)
                updateFallbackVisualizer()
                delay(120)
            }
        }
    }

    fun play(song: SongDto, queue: List<SongDto> = emptyList()) {
        scope.launch {
            val playbackQueue = resolvePlaybackQueue(song, queue)
            playResolved(song, playbackQueue)
        }
    }

    private suspend fun playResolved(song: SongDto, queue: List<SongDto>) {
        stateRepository.play(song, queue)
        runCatching {
            localLibraryRepository.recordPlayed(song)
            startPlaybackServiceSafely()
            val playbackSource = localLibraryRepository.playbackSource(song)
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
            player.play()
            queue.nextAfter(song)?.let { crossfadeCoordinator.prepareNext(it, stateRepository.state.value.isCrossfadeEnabled) }
        }.onFailure { error ->
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
        runCatching { player.pause() }
        stateRepository.setPlaying(false)
    }

    private fun safePlay() {
        runCatching { player.play() }
            .onFailure { stateRepository.setPlaybackError(it.localizedMessage ?: "Playback failed") }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            safePause()
        } else {
            safePlay()
            stateRepository.setPlaying(player.isPlaying)
        }
    }

    fun skipToNext() {
        val state = stateRepository.state.value
        val current = state.currentSong ?: return
        val queue = state.queue
        val next = queue.nextAfter(current) ?: return
        scope.launch { playResolved(next, queue) }
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
        scope.launch { playResolved(previous, queue) }
    }

    fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.5f, 2f)
        runCatching {
            player.playbackParameters = PlaybackParameters(safeSpeed)
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
        stateRepository.state.value.queue.drop(1).firstOrNull()?.let { next -> crossfadeCoordinator.prepareNext(next, enabled) }
    }

    fun seekTo(seconds: Int) {
        runCatching {
            player.seekTo(seconds * 1000L)
            stateRepository.seekTo(seconds)
        }.onFailure {
            stateRepository.setPlaybackError(it.localizedMessage ?: "Seek failed")
        }
    }

    fun close() {
        sleepTimerJob?.cancel()
        runCatching {
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
        val hasFreshAudioSignal = now - realtimeVisualizer.lastBandUpdateMillis < FreshSignalMillis
        if (!hasFreshAudioSignal && now - lastFallbackVisualizerAtMillis >= FallbackUpdateIntervalMillis) {
            lastFallbackVisualizerAtMillis = now
            stateRepository.setVisualizerBands(realtimeVisualizer.decayedBands())
        }
    }

    private class FftRenderersFactory(
        context: Context,
        private val audioSink: AudioSink,
    ) : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean,
        ): AudioSink = audioSink
    }

    private companion object {
        const val MediaSessionId = "arisam_tunes_playback"
        const val MaxStreamCacheBytes = 256L * 1024L * 1024L
        const val FallbackUpdateIntervalMillis = 80L
        const val FreshSignalMillis = 180L
        const val RestartPreviousThresholdMillis = 3_000L
    }
}

private fun List<SongDto>.nextAfter(song: SongDto): SongDto? {
    if (size <= 1) return null
    val currentIndex = indexOfFirst { it.id == song.id }
    if (currentIndex < 0) return null
    return this[(currentIndex + 1) % size]
}
