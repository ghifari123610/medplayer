package com.example.playback

import androidx.media3.common.Player
import com.example.data.model.AudioTrack

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val currentTrack: AudioTrack? = null,
    val currentMediaItemIndex: Int = 0,
    val playlist: List<AudioTrack> = emptyList(),
    val playbackState: Int = Player.STATE_IDLE,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playbackSpeed: Float = 1.0f,
    val audioFocusStatus: String = "Focus Active (Ready)",
    val isServiceConnected: Boolean = false,
    val audioSessionId: Int = 0
) {
    val progress: Float
        get() = if (durationMs > 0) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val isBuffering: Boolean
        get() = playbackState == Player.STATE_BUFFERING

    val isReady: Boolean
        get() = playbackState == Player.STATE_READY

    val isEnded: Boolean
        get() = playbackState == Player.STATE_ENDED
}
