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
import androidx.media3.exoplayer.ExoPlayer
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
    private val player = ExoPlayer.Builder(appContext)
        .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
        .build()
    private val crossfadeCoordinator = CrossfadePlaybackCoordinator(appContext, cacheDataSourceFactory)
    private var sleepTimerJob: Job? = null
    private var mediaSession: MediaSession? = null
    private var visualizerBands = PlayerStateRepository.emptyVisualizerBands()
    private var lastFallbackVisualizerAtMillis = 0L
    private var sourceSpectrumSongId: String? = null
    private var sourceSpectrumFrameDurationMs = 100
    private var sourceSpectrumFrames: List<List<Float>> = emptyList()

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

    fun play(song: SongDto) {
        stateRepository.play(song)
        scope.launch {
            runCatching {
                startPlaybackServiceSafely()
                val playbackSource = localLibraryRepository.playbackSource(song)
                loadSourceSpectrum(song)
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
            }.onFailure { error ->
                stateRepository.setPlaybackError(error.localizedMessage ?: "Playback failed")
            }
        }
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
        if (now - lastFallbackVisualizerAtMillis >= FallbackUpdateIntervalMillis) {
            lastFallbackVisualizerAtMillis = now
            stateRepository.setVisualizerBands(sourceSpectrumBands() ?: nextVisualizerBands())
        }
    }

    private suspend fun loadSourceSpectrum(song: SongDto) {
        if (sourceSpectrumSongId == song.id && sourceSpectrumFrames.isNotEmpty()) return
        runCatching { catalogRepository.songSpectrum(song.id) }
            .onSuccess { spectrum ->
                sourceSpectrumSongId = song.id
                sourceSpectrumFrameDurationMs = spectrum.frameDurationMs.coerceAtLeast(50)
                sourceSpectrumFrames = spectrum.frames
                visualizerBands = spectrum.frames.firstOrNull() ?: PlayerStateRepository.emptyVisualizerBands()
                stateRepository.setVisualizerBands(visualizerBands)
            }
            .onFailure {
                sourceSpectrumSongId = null
                sourceSpectrumFrames = emptyList()
            }
    }

    private fun sourceSpectrumBands(): List<Float>? {
        val frames = sourceSpectrumFrames.takeIf { it.isNotEmpty() } ?: return null
        val index = (player.currentPosition / sourceSpectrumFrameDurationMs).toInt().coerceIn(0, frames.lastIndex)
        val target = frames[index]
        visualizerBands = target.mapIndexed { band, value ->
            val previous = visualizerBands.getOrNull(band) ?: 0.08f
            (previous * 0.35f + value * 0.65f).coerceIn(0.04f, 1f)
        }
        return visualizerBands
    }

    private fun nextVisualizerBands(): List<Float> {
        val song = stateRepository.state.value.currentSong
        if (!player.isPlaying || song == null) {
            visualizerBands = visualizerBands.map { (it * 0.82f).coerceAtLeast(0.06f) }
            return visualizerBands
        }

        val t = player.currentPosition / 1000f
        val seed = song.id.hashCode()
        val speed = stateRepository.state.value.playbackSpeed
        visualizerBands = visualizerBands.mapIndexed { index, previous ->
            val lane = index / 36f
            val bassEnvelope = kotlin.math.abs(kotlin.math.sin((t * 3.2f * speed + seed * 0.0007f).toDouble())).toFloat()
            val snareEnvelope = kotlin.math.abs(kotlin.math.sin((t * 7.4f * speed + index * 0.37f).toDouble())).toFloat()
            val shimmer = kotlin.math.abs(kotlin.math.sin((t * (11.0f + index % 5) * speed + seed * 0.00013f + index).toDouble())).toFloat()
            val profile = when {
                index < 7 -> 0.42f + bassEnvelope * 0.55f
                index < 18 -> 0.24f + snareEnvelope * 0.44f
                else -> 0.12f + shimmer * 0.34f
            }
            val songColor = (((seed shr (index % 16)) and 0xF) / 15f) * 0.14f
            val stereoTilt = 0.86f + kotlin.math.sin((lane * Math.PI * 2 + t).toDouble()).toFloat() * 0.10f
            val target = ((profile + songColor) * stereoTilt).coerceIn(0.08f, 1f)
            (previous * 0.58f + target * 0.42f).coerceIn(0.06f, 1f)
        }
        return visualizerBands
    }

    private companion object {
        const val MediaSessionId = "arisam_tunes_playback"
        const val MaxStreamCacheBytes = 256L * 1024L * 1024L
        const val FallbackUpdateIntervalMillis = 96L
    }
}
