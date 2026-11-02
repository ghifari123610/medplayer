package com.example.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.model.AudioTrack
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicController(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressTrackingJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var currentPlaylistTracks: List<AudioTrack> = emptyList()

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                try {
                    val controller = controllerFuture?.get()
                    if (controller != null) {
                        mediaController = controller
                        controller.addListener(PlayerListener())
                        _playbackState.update { it.copy(isServiceConnected = true) }
                        syncFullState()
                        startProgressTracking()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun startProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = scope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = if (controller.duration == C.TIME_UNSET) 0L else controller.duration.coerceAtLeast(0L)
                    val buffered = controller.bufferedPosition.coerceAtLeast(0L)
                    _playbackState.update { state ->
                        state.copy(
                            currentPositionMs = pos,
                            durationMs = if (dur > 0L) dur else state.durationMs,
                            bufferedPositionMs = buffered
                        )
                    }
                }
                delay(300)
            }
        }
    }

    private fun syncFullState() {
        val controller = mediaController ?: return
        val currentMediaItem = controller.currentMediaItem
        val currentTrack = currentMediaItem?.let { item ->
            currentPlaylistTracks.find { it.id == item.mediaId }
                ?: AudioTrack.fromMediaItem(item, controller.duration.coerceAtLeast(0L))
        }

        val pos = controller.currentPosition.coerceAtLeast(0L)
        val dur = if (controller.duration == C.TIME_UNSET) 0L else controller.duration.coerceAtLeast(0L)
        val focusStatus = when {
            controller.playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS ->
                "Focus Lost: Paused (Call/Notification Alert)"
            controller.isPlaying ->
                "Focus Active: Exclusive Playback"
            else ->
                "Focus Ready: Idle / Paused"
        }

        val sessionId = controller.sessionExtras.getInt("AUDIO_SESSION_ID", 0)

        _playbackState.update { state ->
            state.copy(
                isPlaying = controller.isPlaying,
                currentPositionMs = pos,
                durationMs = if (dur > 0L) dur else (currentTrack?.durationMs ?: 0L),
                bufferedPositionMs = controller.bufferedPosition.coerceAtLeast(0L),
                currentTrack = currentTrack,
                currentMediaItemIndex = controller.currentMediaItemIndex,
                playbackState = controller.playbackState,
                isShuffleEnabled = controller.shuffleModeEnabled,
                repeatMode = controller.repeatMode,
                playbackSpeed = controller.playbackParameters.speed,
                audioFocusStatus = focusStatus,
                isServiceConnected = true,
                audioSessionId = sessionId
            )
        }
    }

    fun setPlaylist(tracks: List<AudioTrack>, startIndex: Int = 0, playWhenReady: Boolean = true) {
        val controller = mediaController ?: return
        currentPlaylistTracks = tracks
        val mediaItems = tracks.map { it.toMediaItem() }
        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.playWhenReady = playWhenReady
        _playbackState.update { it.copy(playlist = tracks) }
        syncFullState()
    }

    fun playTrackAtIndex(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekToDefaultPosition(index)
            controller.play()
            syncFullState()
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_IDLE || controller.mediaItemCount == 0) {
                // If playlist empty, load default sample playlist
                setPlaylist(AudioTrack.sampleTracks, startIndex = 0, playWhenReady = true)
            } else {
                controller.play()
            }
        }
        syncFullState()
    }

    fun play() {
        val controller = mediaController ?: return
        if (controller.playbackState == Player.STATE_IDLE || controller.mediaItemCount == 0) {
            setPlaylist(AudioTrack.sampleTracks, startIndex = 0, playWhenReady = true)
        } else {
            controller.play()
        }
        syncFullState()
    }

    fun pause() {
        mediaController?.pause()
        syncFullState()
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        controller.seekTo(positionMs.coerceAtLeast(0L))
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun seekRelative(offsetMs: Long) {
        val controller = mediaController ?: return
        val current = controller.currentPosition
        val target = (current + offsetMs).coerceIn(0L, controller.duration.coerceAtLeast(0L))
        controller.seekTo(target)
        _playbackState.update { it.copy(currentPositionMs = target) }
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else {
            // Loop back to start if repeat is enabled or standard cycle
            controller.seekToDefaultPosition(0)
        }
        syncFullState()
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000L) {
            // Restart current track if more than 3 seconds in
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
        syncFullState()
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val newState = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = newState
        _playbackState.update { it.copy(isShuffleEnabled = newState) }
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _playbackState.update { it.copy(repeatMode = nextMode) }
    }

    fun setPlaybackSpeed(speed: Float) {
        val controller = mediaController ?: return
        controller.playbackParameters = PlaybackParameters(speed)
        _playbackState.update { it.copy(playbackSpeed = speed) }
    }

    fun release() {
        progressTrackingJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }

    private inner class PlayerListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncFullState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncFullState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncFullState()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            syncFullState()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _playbackState.update { it.copy(playbackSpeed = playbackParameters.speed) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _playbackState.update { it.copy(repeatMode = repeatMode) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playbackState.update { it.copy(isShuffleEnabled = shuffleModeEnabled) }
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackState.update {
                it.copy(
                    audioFocusStatus = "Player Error: ${error.message ?: "Playback failure"}"
                )
            }
        }
    }
}
