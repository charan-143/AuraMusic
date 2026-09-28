package com.example.auramusic.model

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val coverArtUrl: String,
    val year: String = "2024",
    val source: StreamingSource = StreamingSource.SPOTIFY,
    val qualityBadge: String = "SPOTIFY 320K",
    val isLossless: Boolean = false,
    val tracks: List<Track> = emptyList(),
    val isUserCreated: Boolean = false,
    val description: String = ""
) {
    val trackCount: Int
        get() = tracks.size

    val formattedDuration: String
        get() {
            val totalSeconds = (tracks.sumOf { it.durationMs } / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            return "$minutes mins"
        }
}
