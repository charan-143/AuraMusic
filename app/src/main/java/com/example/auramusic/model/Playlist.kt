package com.example.auramusic.model

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val coverArtUrl: String = "",
    val tracks: List<Track> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isCustomAlbum: Boolean = false // Allows user to create custom album or playlist
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
