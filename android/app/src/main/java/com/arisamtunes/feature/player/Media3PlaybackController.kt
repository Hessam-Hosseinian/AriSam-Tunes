package com.arisamtunes.feature.player

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
import com.arisamtunes.data.catalog.SongDto
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
        })
        scope.launch {
            while (true) {
                stateRepository.setProgress(player.currentPosition.toInt() / 1000)
                delay(1_000)
            }
        }
    }

    fun play(song: SongDto) {
        startPlaybackService()
        stateRepository.play(song)
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(song.audioUrl)
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

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
        stateRepository.setPlaying(player.isPlaying)
    }

    fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.5f, 2f)
        player.playbackParameters = PlaybackParameters(safeSpeed)
        stateRepository.setPlaybackSpeed(safeSpeed)
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
            player.pause()
            stateRepository.setPlaying(false)
            stateRepository.setSleepTimerEndsAt(null)
        }
    }

    fun toggleCrossfade() {
        val enabled = !stateRepository.state.value.isCrossfadeEnabled
        stateRepository.setCrossfadeEnabled(enabled)
        stateRepository.state.value.queue.drop(1).firstOrNull()?.let { next -> crossfadeCoordinator.prepareNext(next, enabled) }
    }

    fun seekTo(seconds: Int) {
        player.seekTo(seconds * 1000L)
        stateRepository.seekTo(seconds)
    }

    fun close() {
        sleepTimerJob?.cancel()
        player.stop()
        player.clearMediaItems()
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

    private fun startPlaybackService() {
        val intent = Intent(appContext, AriSamPlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appContext.startForegroundService(intent) else appContext.startService(intent)
    }

    private companion object {
        const val MediaSessionId = "arisam_tunes_playback"
        const val MaxStreamCacheBytes = 256L * 1024L * 1024L
    }
}
