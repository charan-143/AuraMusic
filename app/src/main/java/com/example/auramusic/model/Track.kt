package com.example.auramusic.model

enum class StreamingSource(val displayName: String, val badge: String) {
    SPOTIFY("Spotify", "SPOTIFY LOSSLESS"),
    YOUTUBE_MUSIC("YouTube Music", "YT LOSSLESS"),
    LOSSLESS_FLAC("Lossless Master", "24-BIT FLAC"),
    LOCAL_STORAGE("Device Storage", "LOCAL FLAC")
}

enum class AudioBitrateMode(val label: String, val kbps: Int) {
    LOSSLESS_MASTER("Studio Lossless", 1411), // 16/24-bit 44.1kHz FLAC
    HIGH_QUALITY("High Quality", 320),        // 320kbps
    ADAPTIVE_ROAMING("Roaming Adaptive", 128)  // 128kbps low-latency ABR for weak cellular
}

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Monochrome Sessions",
    val durationMs: Long = 180000L,
    val audioUrl: String = "",
    val fallbackLowBitrateUrl: String = "",
    val coverArtUrl: String = "",
    val source: StreamingSource = StreamingSource.LOSSLESS_FLAC,
    val qualityBadge: String = "24-BIT FLAC",
    val isLossless: Boolean = true,
    val isCachedOffline: Boolean = false,
    val isLocal: Boolean = false,
    val category: String = "Lossless", // "Spotify", "YouTube Music", "Lossless", "Lofi Chill", "Device Library"
    val waveformData: List<Float> = listOf(
        0.3f, 0.5f, 0.8f, 0.4f, 0.9f, 0.7f, 0.4f, 0.6f, 0.9f, 0.5f,
        0.3f, 0.7f, 0.8f, 0.9f, 0.6f, 0.4f, 0.8f, 0.5f, 0.3f, 0.6f,
        0.8f, 0.9f, 0.7f, 0.4f, 0.8f, 0.6f, 0.3f, 0.5f, 0.7f, 0.4f
    )
) {
    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
