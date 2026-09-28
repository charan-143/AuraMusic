package com.example.auramusic.player

import com.example.auramusic.model.AudioBitrateMode
import com.example.auramusic.model.Track

data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val isRepeatOne: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = 0,
    val volume: Float = 1.0f,
    val isTravelModeEnabled: Boolean = true,
    val bitrateMode: AudioBitrateMode = AudioBitrateMode.LOSSLESS_MASTER,
    val networkStatusText: String = "5G Lossless Connected",
    val sleepTimerRemainingSec: Int? = null,
    val activeAudioOutputDevice: String = "Phone Speaker",
    val activeAudioDeviceType: String = "SPEAKER",
    val isAudioRouteAutoDetected: Boolean = true,
    val isAutoQualityEnabled: Boolean = true,
    val activeStreamingQualityBadge: String = "AUTO 24-BIT",
    val activeStreamingQualityTitle: String = "24-bit FLAC (Lossless)",
    val signalStrengthPercent: Int = 95,
    val linkBandwidthKbps: Int = 15000,
    val autoQualitySwitchNote: String = "Auto: 24-bit FLAC active",
    val isSignalFluctuating: Boolean = false,
    val isEqualizerEnabled: Boolean = true,
    val equalizerProfileBadge: String = "AI ✦ CINEMATIC"
) {
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val bufferedProgress: Float
        get() = if (durationMs > 0) (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedCurrentPosition: String
        get() {
            val totalSeconds = (currentPositionMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
