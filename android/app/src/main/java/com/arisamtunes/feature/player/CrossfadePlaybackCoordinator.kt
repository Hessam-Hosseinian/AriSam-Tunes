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
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.arisamtunes.data.catalog.SongDto

@OptIn(UnstableApi::class)
class CrossfadePlaybackCoordinator(
    context: Context,
    cacheDataSourceFactory: CacheDataSource.Factory,
) {
    private val secondaryPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
        .build()

    fun prepareNext(song: SongDto, enabled: Boolean) {
        if (!enabled) {
            secondaryPlayer.clearMediaItems()
            return
        }
        secondaryPlayer.volume = 0f
        secondaryPlayer.setMediaItem(MediaItem.fromUri(song.audioUrl))
        secondaryPlayer.prepare()
    }

    fun release() {
        secondaryPlayer.release()
    }
}
