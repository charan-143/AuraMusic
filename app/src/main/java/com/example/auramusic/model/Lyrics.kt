package com.example.auramusic.model

/**
 * Represents a single line of time-synchronized lyrics in milliseconds.
 */
data class LyricLine(
    val timestampMs: Long,
    val text: String
) {
    val formattedTimestamp: String
        get() {
            val totalSeconds = (timestampMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }
}

/**
 * Represents the complete lyrics for a music track, supporting time-synchronized LRC
 * and plain-text lyrics with fallback capabilities.
 */
data class TrackLyrics(
    val trackId: String,
    val title: String,
    val artist: String,
    val isSynced: Boolean = true,
    val lines: List<LyricLine> = emptyList(),
    val plainLyrics: String = "",
    val source: String = "LRCLIB Synced",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val hasLines: Boolean
        get() = lines.isNotEmpty()
}
