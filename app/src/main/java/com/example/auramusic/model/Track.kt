package com.example.auramusic.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Monochrome Sessions",
    val durationMs: Long = 180000L,
    val audioUrl: String = "",
    val coverArtUrl: String = "",
    val isLocal: Boolean = false,
    val category: String = "Lofi Chill", // "Lofi Chill", "Deep Ambient", "Electronic", "Device Library"
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
