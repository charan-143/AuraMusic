package com.example.auramusic.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.auramusic.player.MusicPlayerManager

class AuraMediaService : MediaSessionService() {

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "auramusic_playback_channel",
                "AuraMusic Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AuraMusic media player controls and system media session"
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
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
