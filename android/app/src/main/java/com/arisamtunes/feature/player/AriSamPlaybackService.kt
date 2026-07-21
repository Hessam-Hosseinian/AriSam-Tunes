package com.arisamtunes.feature.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class AriSamPlaybackService : MediaSessionService() {
    @Inject lateinit var playbackController: Media3PlaybackController

    override fun onCreate() {
        super.onCreate()
        addSession(playbackController.mediaSession(this))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        playbackController.mediaSession(this)

    override fun onDestroy() {
        playbackController.currentMediaSession()?.let(::removeSession)
        // The controller is application-scoped and is also used by PlayerViewModel.
        // Releasing it here makes a later service restart reuse a released player/cache.
        playbackController.releaseMediaSession()
        super.onDestroy()
    }
}
