package com.example.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer
    private var equalizerController: EqualizerController? = null

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "aura_audio_playback_channel"
        const val NOTIFICATION_ID = 1001

        @Volatile
        var activeEqualizerController: EqualizerController? = null
            private set
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // 1. Configure audio attributes for music playback with automatic Audio Focus handling
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // 2. Initialize ExoPlayer with audio focus management and noisy audio handling (e.g. unplugging headphones)
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()

        // 3. Connect EqualizerController to ExoPlayer's native audioSessionId
        val eq = activeEqualizerController ?: EqualizerController().also { activeEqualizerController = it }
        equalizerController = eq
        val sessionId = exoPlayer.audioSessionId
        if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != 0) {
            eq.bindToAudioSession(sessionId)
        }

        // 4. Create PendingIntent to reopen MainActivity from the media notification
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 5. Expose audioSessionId in Session Extras
        val sessionExtras = Bundle().apply {
            putInt("AUDIO_SESSION_ID", exoPlayer.audioSessionId)
        }

        // 6. Build MediaSession attached to ExoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(MediaSessionCallback())
            .setExtras(sessionExtras)
            .build()

        // 7. Configure DefaultMediaNotificationProvider with our channel
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setNotificationId(NOTIFICATION_ID)
            .build()
        setMediaNotificationProvider(notificationProvider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        equalizerController?.release()
        equalizerController = null
        activeEqualizerController = null

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private class MediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(
                    MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
                )
                .build()
        }
    }
}
