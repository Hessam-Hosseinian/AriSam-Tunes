package com.arisamtunes.feature.player

/*
 * Crossfade approach: dual-player fallback.
 *
 * AriSam Tunes keeps one primary ExoPlayer for normal playback and prepares a
 * secondary ExoPlayer when a future queue item is available. During the final
 * seconds of the current item the primary volume can ramp down while the
 * secondary player ramps up, then the secondary becomes the active player.
 * This file intentionally isolates that behavior so Milestone 30 can wire the
 * fallback without complicating the foreground MediaSession service.
 */

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.arisamtunes.data.catalog.SongDto

@OptIn(UnstableApi::class)
class CrossfadePlaybackCoordinator(
    context: Context,
    cacheDataSourceFactory: CacheDataSource.Factory,
    renderersFactory: DefaultRenderersFactory,
    private val onEnded: () -> Unit = {},
) {
    private val secondaryPlayer = ExoPlayer.Builder(context, renderersFactory)
        .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
        .build()
    private var preparedSongId: String? = null

    init {
        secondaryPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) onEnded()
            }
        })
    }

    fun prepareNext(song: SongDto, uri: String, enabled: Boolean) {
        if (!enabled) {
            clear()
            return
        }
        if (preparedSongId == song.id) return
        preparedSongId = song.id
        secondaryPlayer.volume = 0f
        secondaryPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
        secondaryPlayer.prepare()
    }

    fun startPrepared(song: SongDto): Boolean {
        if (preparedSongId != song.id) return false
        secondaryPlayer.seekTo(0L)
        secondaryPlayer.volume = 0f
        secondaryPlayer.play()
        return true
    }

    fun setVolume(volume: Float) {
        secondaryPlayer.volume = volume.coerceIn(0f, 1f)
    }

    fun currentPositionMillis(): Long = secondaryPlayer.currentPosition.coerceAtLeast(0L)

    fun durationMillis(): Long = secondaryPlayer.duration

    fun play() {
        secondaryPlayer.play()
    }

    fun pause() {
        secondaryPlayer.pause()
    }

    fun seekTo(positionMillis: Long) {
        secondaryPlayer.seekTo(positionMillis.coerceAtLeast(0L))
    }

    fun isPlaying(): Boolean = secondaryPlayer.isPlaying

    fun playbackSpeed(speed: Float) {
        secondaryPlayer.playbackParameters = PlaybackParameters(speed)
    }

    fun clear() {
        secondaryPlayer.stop()
        secondaryPlayer.clearMediaItems()
        preparedSongId = null
    }

    fun release() {
        secondaryPlayer.release()
    }
}
