package com.arisamtunes.feature.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.arisamtunes.data.catalog.SongDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class Media3PlaybackController @Inject constructor(
    @ApplicationContext context: Context,
    private val stateRepository: PlayerStateRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val player = ExoPlayer.Builder(context).build()

    init {
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
        stateRepository.play(song)
        player.setMediaItem(MediaItem.fromUri(song.audioUrl))
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
        stateRepository.setPlaying(player.isPlaying)
    }

    fun seekTo(seconds: Int) {
        player.seekTo(seconds * 1000L)
        stateRepository.seekTo(seconds)
    }

    fun close() {
        player.stop()
        player.clearMediaItems()
        stateRepository.close()
    }
}
