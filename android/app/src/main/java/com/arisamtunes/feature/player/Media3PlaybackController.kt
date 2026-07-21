package com.arisamtunes.feature.player

import android.Manifest
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
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
import androidx.media3.datasource.DefaultDataSource
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
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.cancel
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
    private val controllerJob = SupervisorJob()
    private val scope = CoroutineScope(controllerJob + Dispatchers.Main.immediate)
    private val streamCache = SimpleCache(
        File(appContext.cacheDir, "media3-stream-cache"),
        LeastRecentlyUsedCacheEvictor(MaxStreamCacheBytes),
        StandaloneDatabaseProvider(appContext),
    )
    private val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(streamCache)
        // DefaultDataSource handles file:// downloads while delegating network
        // streams to OkHttp. Using OkHttp directly made local files fail offline.
        .setUpstreamDataSourceFactory(DefaultDataSource.Factory(appContext, OkHttpDataSource.Factory(OkHttpClient())))
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    private val realtimeVisualizers = CopyOnWriteArrayList<RealtimeFftAudioBufferSink>()
    private val renderersFactory = FftRenderersFactory(
        appContext,
        onVisualizerCreated = realtimeVisualizers::add,
        onBands = stateRepository::setVisualizerBands,
    )
    private val mediaAudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()
    private val player = ExoPlayer.Builder(appContext, renderersFactory)
        .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
        .setAudioAttributes(mediaAudioAttributes, true)
        .build()
    private val crossfadeCoordinator = CrossfadePlaybackCoordinator(appContext, cacheDataSourceFactory, renderersFactory)
    private var sleepTimerJob: Job? = null
    private var transitionJob: Job? = null
    private var mediaSession: MediaSession? = null
    private var lastFallbackVisualizerAtMillis = 0L
    private var resumeCrossfadeAfterAudioFocusGain = false
    private var isCallInterruptionActive = false
    private var isApplyingCallPause = false
    private var resumePlaybackAfterCall = false
    private var resumeCrossfadeAfterCall = false
    private var isPhoneStateInterruptionActive = false
    private var isAudioModeInterruptionActive = false
    private var isPhoneCallMonitoringEnabled = false
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val telephonyManager = appContext.getSystemService(TelephonyManager::class.java)
    private var telephonyCallback: TelephonyCallback? = null
    @Suppress("DEPRECATION")
    private var phoneStateListener: PhoneStateListener? = null
    private val audioModeChangedListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        AudioManager.OnModeChangedListener(::handleAudioModeChanged)
    } else {
        null
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioModeChangedListener?.let { listener ->
                audioManager.addOnModeChangedListener(appContext.mainExecutor, listener)
            }
        }
        enablePhoneCallMonitoring()
        player.setHandleAudioBecomingNoisy(true)
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (stateRepository.state.value.crossfadeSong == null) {
                    stateRepository.setPlaying(isPlaying)
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady) return
                if (
                    isCallInterruptionActive &&
                    !isApplyingCallPause &&
                    reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
                ) {
                    resumePlaybackAfterCall = false
                    resumeCrossfadeAfterCall = false
                }
                when (reason) {
                    Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS ->
                        pauseCrossfadeForAudioInterruption(resumeWhenFocusReturns = false)

                    Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY ->
                        pauseCrossfadeForAudioInterruption(resumeWhenFocusReturns = false)
                }
            }

            override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
                when (playbackSuppressionReason) {
                    Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS ->
                        pauseCrossfadeForAudioInterruption(resumeWhenFocusReturns = true)

                    Player.PLAYBACK_SUPPRESSION_REASON_NONE -> resumeCrossfadeAfterAudioInterruption()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && player.duration != C.TIME_UNSET && player.duration > 0L) {
                    val durationSeconds = ((player.duration + 999L) / 1_000L).toInt()
                    val songId = stateRepository.state.value.currentSong?.id
                    stateRepository.updateCurrentSongDuration(durationSeconds)
                    if (songId != null) {
                        scope.launch { localLibraryRepository.updateDownloadDuration(songId, durationSeconds) }
                    }
                }
                stateRepository.setProgress(player.currentPosition.toInt() / 1000)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId?.takeIf(String::isNotBlank) ?: return
                val state = stateRepository.state.value
                val song = state.queue.firstOrNull { it.id == mediaId } ?: return
                if (state.currentSong?.id == mediaId) return
                if (transitionJob?.isActive == true && state.crossfadeSong?.id == mediaId) return
                transitionJob?.cancel()
                crossfadeCoordinator.clear()
                stateRepository.play(song, state.queue)
                scope.launch {
                    localLibraryRepository.recordPlayed(song)
                    prepareCrossfadeNext(song, state.queue)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                stateRepository.setPlaybackError(PlaybackError.Generic)
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
            playResolved(song, playbackQueue, startPositionMillis = 0L, cancelTransition = true, replaceQueue = true)
            if (song.durationSeconds <= 0) hydrateLegacyDownloadMetadata(song)
        }
    }

    private fun hydrateLegacyDownloadMetadata(song: SongDto) {
        scope.launch {
            val hydrated = runCatching { catalogRepository.song(song.id) }.getOrNull() ?: return@launch
            localLibraryRepository.refreshDownloadMetadata(hydrated)
            stateRepository.updateCurrentSongMetadata(hydrated)
        }
    }

    private suspend fun playResolved(
        song: SongDto,
        queue: List<SongDto>,
        startPositionMillis: Long,
        cancelTransition: Boolean,
        initialVolume: Float = 1f,
        replaceQueue: Boolean = false,
    ) {
        stateRepository.play(song, queue, replaceQueue = replaceQueue)
        runCatching {
            if (cancelTransition) {
                transitionJob?.cancel()
                crossfadeCoordinator.clear()
            }
            localLibraryRepository.recordPlayed(song)
            startPlaybackServiceSafely()
            val normalizedQueue = queue.normalizeQueue(song)
            val mediaItems = normalizedQueue.map { queuedSong -> queuedSong.toMediaItem(localLibraryRepository.playbackSource(queuedSong)) }
            val selectedIndex = normalizedQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            player.volume = initialVolume.coerceIn(0f, 1f)
            player.setMediaItems(mediaItems, selectedIndex, startPositionMillis.coerceAtLeast(0L))
            player.prepare()
            player.play()
            applyRepeatMode(stateRepository.state.value.repeatMode)
            prepareCrossfadeNext(song, normalizedQueue)
        }.onFailure { error ->
            player.volume = 1f
            crossfadeCoordinator.clear()
            stateRepository.setPlaybackError(PlaybackError.Generic)
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
        val state = stateRepository.state.value
        state.currentSong?.let { play(it, state.queue) }
    }

    fun refreshPlaybackState() {
        stateRepository.setPlaying(player.isPlaying)
        stateRepository.setProgress(player.currentPosition.toInt() / 1000)
    }

    fun clearError() {
        stateRepository.setPlaybackError(null)
    }

    fun safePause() {
        resumeCrossfadeAfterAudioFocusGain = false
        resumePlaybackAfterCall = false
        resumeCrossfadeAfterCall = false
        runCatching {
            if (transitionJob?.isActive == true) {
                player.pause()
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
            } else {
                player.volume = 1f
                player.play()
            }
        }
            .onFailure { stateRepository.setPlaybackError(PlaybackError.Generic) }
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
        val next = queue.nextFor(current, state.repeatMode) ?: return
        transitionJob?.cancel()
        crossfadeCoordinator.clear()
        stateRepository.setCrossfadePreview(null)
        val nextIndex = player.indexOfMediaId(next.id)
        if (nextIndex >= 0) {
            player.seekTo(nextIndex, 0L)
            player.play()
        } else {
            scope.launch { playResolved(next, queue, startPositionMillis = 0L, cancelTransition = true) }
        }
    }

    fun skipToPrevious() {
        val state = stateRepository.state.value
        val current = state.currentSong ?: return
        if (player.currentPosition > RestartPreviousThresholdMillis) {
            seekTo(0)
            return
        }
        val queue = state.queue
        val previous = queue.previousFor(current, state.repeatMode) ?: return
        transitionJob?.cancel()
        crossfadeCoordinator.clear()
        stateRepository.setCrossfadePreview(null)
        val previousIndex = player.indexOfMediaId(previous.id)
        if (previousIndex >= 0) {
            player.seekTo(previousIndex, 0L)
            player.play()
        } else {
            scope.launch { playResolved(previous, queue, startPositionMillis = 0L, cancelTransition = true) }
        }
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
            stateRepository.setPlaybackError(PlaybackError.Speed)
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
            crossfadeCoordinator.clear()
            stateRepository.setCrossfadePreview(null)
        }
        val state = stateRepository.state.value
        state.currentSong?.let { current ->
            scope.launch { prepareCrossfadeNext(current, state.queue) }
        }
    }

    fun toggleShuffle() {
        stateRepository.toggleShuffle()
        val state = stateRepository.state.value
        state.currentSong?.let { current ->
            scope.launch {
                val position = player.currentPosition.coerceAtLeast(0L)
                rebuildMediaQueue(current, state.queue, position)
                prepareCrossfadeNext(current, state.queue)
            }
        }
    }

    fun cycleRepeatMode() {
        val mode = (stateRepository.state.value.repeatMode + 1) % 3
        stateRepository.setRepeatMode(mode)
        applyRepeatMode(mode)
        if (mode == 2) {
            transitionJob?.cancel()
            player.volume = 1f
            crossfadeCoordinator.clear()
            stateRepository.setCrossfadePreview(null)
        } else {
            val state = stateRepository.state.value
            state.currentSong?.let { current -> scope.launch { prepareCrossfadeNext(current, state.queue) } }
        }
    }

    fun seekTo(seconds: Int) {
        seekToMillis(seconds.toLong() * 1_000L)
    }

    fun seekToMillis(positionMillis: Long) {
        runCatching {
            val state = stateRepository.state.value
            val safePositionMillis = positionMillis.coerceIn(0L, (state.crossfadeSong ?: state.currentSong).durationMillis())
            if (state.crossfadeSong != null) {
                crossfadeCoordinator.seekTo(safePositionMillis)
                state.crossfadeSong?.let {
                    stateRepository.setCrossfadePreview(it, millis = safePositionMillis)
                }
            } else {
                player.seekTo(safePositionMillis)
            }
            stateRepository.seekToMillis(safePositionMillis)
        }.onFailure {
            stateRepository.setPlaybackError(PlaybackError.Seek)
        }
    }

    fun close() {
        sleepTimerJob?.cancel()
        transitionJob?.cancel()
        resumeCrossfadeAfterAudioFocusGain = false
        resumePlaybackAfterCall = false
        resumeCrossfadeAfterCall = false
        crossfadeCoordinator.clear()
        runCatching {
            player.volume = 1f
            player.stop()
            player.clearMediaItems()
        }
        stateRepository.close()
    }

    private suspend fun rebuildMediaQueue(current: SongDto, queue: List<SongDto>, positionMillis: Long) {
        val normalizedQueue = queue.normalizeQueue(current)
        val mediaItems = normalizedQueue.map { song -> song.toMediaItem(localLibraryRepository.playbackSource(song)) }
        val selectedIndex = normalizedQueue.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
        val resumePlayback = player.playWhenReady
        player.setMediaItems(mediaItems, selectedIndex, positionMillis.coerceAtLeast(0L))
        player.prepare()
        player.playWhenReady = resumePlayback
        applyRepeatMode(stateRepository.state.value.repeatMode)
    }

    private fun applyRepeatMode(mode: Int) {
        player.repeatMode = when (mode) {
            1 -> Player.REPEAT_MODE_ALL
            2 -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun mediaSession(context: Context): MediaSession =
        mediaSession ?: MediaSession.Builder(context, player)
            .setId(MediaSessionId)
            .setSessionActivity(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, com.arisamtunes.MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
            .also { mediaSession = it }

    fun currentMediaSession(): MediaSession? = mediaSession

    fun releaseMediaSession() {
        mediaSession?.release()
        mediaSession = null
    }

    fun release() {
        sleepTimerJob?.cancel()
        transitionJob?.cancel()
        controllerJob.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioModeChangedListener?.let(audioManager::removeOnModeChangedListener)
        }
        disablePhoneCallMonitoring()
        runCatching { crossfadeCoordinator.release() }
        runCatching { player.release() }
        runCatching { streamCache.release() }
        realtimeVisualizers.clear()
        releaseMediaSession()
    }

    private fun startPlaybackServiceSafely() {
        val intent = Intent(appContext, AriSamPlaybackService::class.java)
        runCatching {
            androidx.core.content.ContextCompat.startForegroundService(appContext, intent)
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

    fun enablePhoneCallMonitoring() {
        if (
            isPhoneCallMonitoringEnabled ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
        ) return

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) = handlePhoneCallState(state)
                }
                telephonyManager.registerTelephonyCallback(appContext.mainExecutor, callback)
                telephonyCallback = callback
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Android API")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handlePhoneCallState(state)
                    }
                }
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                phoneStateListener = listener
            }
            isPhoneCallMonitoringEnabled = true
        }.onFailure { error ->
            Log.w("Media3PlaybackController", "Phone call monitoring could not be enabled", error)
        }
    }

    private fun disablePhoneCallMonitoring() {
        if (!isPhoneCallMonitoringEnabled) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let(telephonyManager::unregisterTelephonyCallback)
            } else {
                @Suppress("DEPRECATION")
                phoneStateListener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
            }
        }
        telephonyCallback = null
        phoneStateListener = null
        isPhoneCallMonitoringEnabled = false
    }

    private fun handlePhoneCallState(state: Int) {
        val isCallActive = state == TelephonyManager.CALL_STATE_RINGING ||
            state == TelephonyManager.CALL_STATE_OFFHOOK
        updateCallInterruption(phoneStateActive = isCallActive)
    }

    private fun handleAudioModeChanged(mode: Int) {
        val isCallMode = mode == AudioManager.MODE_RINGTONE ||
            mode == AudioManager.MODE_IN_CALL ||
            mode == AudioManager.MODE_IN_COMMUNICATION ||
            mode == AudioManager.MODE_CALL_SCREENING ||
            mode == AudioManager.MODE_CALL_REDIRECT ||
            mode == AudioManager.MODE_COMMUNICATION_REDIRECT
        updateCallInterruption(audioModeActive = isCallMode)
    }

    private fun updateCallInterruption(
        phoneStateActive: Boolean = isPhoneStateInterruptionActive,
        audioModeActive: Boolean = isAudioModeInterruptionActive,
    ) {
        isPhoneStateInterruptionActive = phoneStateActive
        isAudioModeInterruptionActive = audioModeActive
        val callInterruptionActive = phoneStateActive || audioModeActive
        if (callInterruptionActive == isCallInterruptionActive) return

        isCallInterruptionActive = callInterruptionActive
        if (callInterruptionActive) {
            resumePlaybackAfterCall = player.playWhenReady
            resumeCrossfadeAfterCall = crossfadeCoordinator.isPlaying()
            if (resumePlaybackAfterCall || resumeCrossfadeAfterCall) {
                isApplyingCallPause = true
                try {
                    player.pause()
                    crossfadeCoordinator.pause()
                } finally {
                    isApplyingCallPause = false
                }
                stateRepository.setPlaying(false)
            }
            return
        }

        val shouldResumePrimary = resumePlaybackAfterCall
        val shouldResumeCrossfade = resumeCrossfadeAfterCall && transitionJob?.isActive == true
        resumePlaybackAfterCall = false
        resumeCrossfadeAfterCall = false
        if (shouldResumePrimary) player.play()
        if (shouldResumeCrossfade) crossfadeCoordinator.play()
    }

    /**
     * The primary ExoPlayer owns audio focus. The secondary crossfade deck does
     * not request focus itself, so it must follow interruptions reported by the
     * primary player (including an incoming phone call).
     */
    private fun pauseCrossfadeForAudioInterruption(resumeWhenFocusReturns: Boolean) {
        val secondaryWasPlaying = crossfadeCoordinator.isPlaying()
        resumeCrossfadeAfterAudioFocusGain = resumeWhenFocusReturns && secondaryWasPlaying
        if (secondaryWasPlaying) crossfadeCoordinator.pause()
    }

    private fun resumeCrossfadeAfterAudioInterruption() {
        val shouldResume = resumeCrossfadeAfterAudioFocusGain &&
            transitionJob?.isActive == true &&
            player.playWhenReady
        resumeCrossfadeAfterAudioFocusGain = false
        if (shouldResume) crossfadeCoordinator.play()
    }

    private fun maybeStartCrossfade() {
        val state = stateRepository.state.value
        if (!state.isCrossfadeEnabled || state.repeatMode == 2 || transitionJob?.isActive == true || state.crossfadeSong != null) return
        val current = state.currentSong ?: return
        val next = state.queue.nextFor(current, state.repeatMode) ?: return
        val duration = player.duration
        val position = player.currentPosition.coerceAtLeast(0L)
        if (!player.isPlaying || duration <= CrossfadeLeadMillis || position < duration - CrossfadeLeadMillis) return
        transitionJob = scope.launch {
            runCatching {
                prepareCrossfadeNext(current, state.queue)
                if (!crossfadeCoordinator.startPrepared(next)) {
                    skipToNext()
                    return@launch
                }
                stateRepository.setCrossfadePreview(next, seconds = 0)
                blendVolumes(primaryToSecondary = true, durationMillis = CrossfadeDurationMillis)
                val handoffPosition = crossfadeCoordinator.currentPositionMillis()
                preparePrimaryDeck(next, handoffPosition)
                blendVolumes(primaryToSecondary = false, durationMillis = CrossfadeHandoffMillis)
                crossfadeCoordinator.clear()
                stateRepository.play(next, state.queue)
                localLibraryRepository.recordPlayed(next)
                stateRepository.setProgressMillis(player.currentPosition.coerceAtLeast(0L))
                stateRepository.setCrossfadePreview(null)
                prepareCrossfadeNext(next, state.queue)
            }.onFailure {
                player.volume = 1f
                crossfadeCoordinator.clear()
                stateRepository.setCrossfadePreview(null)
                stateRepository.setPlaybackError(PlaybackError.Crossfade)
            }
        }
    }

    private suspend fun prepareCrossfadeNext(current: SongDto, queue: List<SongDto>) {
        if (!stateRepository.state.value.isCrossfadeEnabled) return
        val next = queue.nextFor(current, stateRepository.state.value.repeatMode) ?: return
        val nextSource = localLibraryRepository.playbackSource(next)
        crossfadeCoordinator.prepareNext(next, nextSource, enabled = true)
    }

    private fun preparePrimaryDeck(song: SongDto, positionMillis: Long) {
        val index = player.indexOfMediaId(song.id)
        check(index >= 0) { "Crossfade target is missing from the MediaSession queue" }
        player.volume = 0f
        player.seekTo(index, positionMillis.coerceAtLeast(0L))
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

    private fun activePositionMillis(): Long = player.currentPosition.coerceAtLeast(0L)

    private fun previewPositionMillis(): Long = crossfadeCoordinator.currentPositionMillis()

    private fun activeIsPlaying(): Boolean = player.isPlaying

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
        const val CrossfadeLeadMillis = 10_000L
        const val CrossfadeDurationMillis = 8_000L
        const val CrossfadeHandoffMillis = 320L
    }
}

private fun SongDto.toMediaItem(uri: String): MediaItem =
    MediaItem.Builder()
        .setUri(uri)
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistName)
                .setAlbumTitle(album)
                .setArtworkUri(android.net.Uri.parse(coverImageUrl))
                .build(),
        )
        .build()

private fun Player.indexOfMediaId(mediaId: String): Int {
    for (index in 0 until mediaItemCount) {
        if (getMediaItemAt(index).mediaId == mediaId) return index
    }
    return -1
}

private fun SongDto?.durationMillis(): Long =
    (this?.durationSeconds ?: 0).toLong().coerceAtLeast(0L) * 1_000L
