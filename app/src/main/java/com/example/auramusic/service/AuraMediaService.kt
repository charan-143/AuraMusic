package com.example.auramusic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.media.session.MediaSession as PlatformMediaSession
import android.os.Build
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.auramusic.MainActivity
import com.example.auramusic.player.MusicPlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AuraMediaService : MediaSessionService() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "auramusic_playback_channel"

        const val ACTION_PLAY = "com.example.auramusic.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.auramusic.ACTION_PAUSE"
        const val ACTION_PREV = "com.example.auramusic.ACTION_PREV"
        const val ACTION_NEXT = "com.example.auramusic.ACTION_NEXT"
    }

    private var observerJob: Job? = null
    private var lastArtworkUrl: String? = null
    private var cachedArtwork: Bitmap? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            val session = MusicPlayerManager.getInstance(applicationContext).mediaSession
            addSession(session)
        } catch (e: Exception) {
            // safe fallback
        }

        // Live observe player state to keep Quick Settings media carousel in sync
        observerJob = CoroutineScope(Dispatchers.Main).launch {
            MusicPlayerManager.getInstance(applicationContext).playerState.collect { state ->
                val track = state.currentTrack
                if (track != null && track.coverArtUrl.isNotBlank() && track.coverArtUrl != lastArtworkUrl) {
                    lastArtworkUrl = track.coverArtUrl
                    loadArtwork(track.coverArtUrl)
                }
                postMediaNotification(state.isPlaying)
            }
        }
    }

    private fun loadArtwork(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val loader = ImageLoader(applicationContext)
                val request = ImageRequest.Builder(applicationContext)
                    .data(url)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        cachedArtwork = bitmap
                        val player = MusicPlayerManager.getInstance(applicationContext)
                        postMediaNotification(player.playerState.value.isPlaying)
                    }
                }
            } catch (e: Exception) {
                // safe fallback
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val player = MusicPlayerManager.getInstance(applicationContext)
        when (intent?.action) {
            ACTION_PLAY -> player.resume()
            ACTION_PAUSE -> player.pause()
            ACTION_PREV -> player.skipPrevious()
            ACTION_NEXT -> player.skipNext()
        }

        val isPlaying = player.playerState.value.isPlaying
        val notification = buildMediaNotification(isPlaying)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            // safe fallback
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun postMediaNotification(isPlaying: Boolean) {
        try {
            val notification = buildMediaNotification(isPlaying)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // safe fallback
        }
    }

    private fun buildMediaNotification(isPlaying: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val player = MusicPlayerManager.getInstance(applicationContext)
        val currentTrack = player.playerState.value.currentTrack
        val title = currentTrack?.title ?: "AuraMusic"
        val artist = currentTrack?.artist ?: "Lossless Audio"

        // Media control action intents
        val prevPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AuraMediaService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPausePendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, AuraMediaService::class.java).apply {
                action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, AuraMediaService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Native Android Notification.MediaStyle: binds notification directly into Quick Settings Media Player!
        val mediaStyle = Notification.MediaStyle()
        try {
            val tokenCompat = player.mediaSession.sessionCompatToken
            val platformToken = tokenCompat.token as? PlatformMediaSession.Token
            if (platformToken != null) {
                mediaStyle.setMediaSession(platformToken)
            }
        } catch (e: Exception) {
            // fallback
        }
        // Actions 0, 1, 2 show in compact / lock screen widget
        mediaStyle.setShowActionsInCompactView(0, 1, 2)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        val playPauseIconRes = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        builder
            .setStyle(mediaStyle)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(currentTrack?.album ?: "AuraMusic")
            .setContentIntent(pendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_media_previous),
                    "Previous",
                    prevPendingIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, playPauseIconRes),
                    if (isPlaying) "Pause" else "Play",
                    playPausePendingIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_media_next),
                    "Next",
                    nextPendingIntent
                ).build()
            )

        if (cachedArtwork != null) {
            builder.setLargeIcon(cachedArtwork)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AuraMusic Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AuraMusic background audio playback & Quick Settings controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return MusicPlayerManager.getInstance(applicationContext).mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = MusicPlayerManager.getInstance(applicationContext).exoPlayer
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
            stopSelf()
        }
    }

    override fun onDestroy() {
        observerJob?.cancel()
        observerJob = null
        super.onDestroy()
    }
}
