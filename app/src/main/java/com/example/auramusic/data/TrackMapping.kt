package com.example.auramusic.data

import com.example.auramusic.model.Track
import com.example.auramusic.model.StreamingSource
import java.util.Locale

/** Old curated cards reused unrelated audio. Their URLs and downloaded bytes are not authoritative. */
object TrackMapping {
    fun isLegacyId(id: String): Boolean = id.startsWith("curated_") || id.startsWith("album_track_")

    fun needsResolution(track: Track): Boolean = isLegacyId(track.id) && !track.resolvedCatalogId.startsWith("saavn_song_")

    fun sanitize(track: Track): Track = if (needsResolution(track)) track.copy(
        audioUrl = "", fallbackLowBitrateUrl = "", localFilePath = null, isCachedOffline = false
    ) else track

    private fun text(value: String) = value.lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")

    private fun title(value: String) = text(value.replace(Regex("\\s*\\(from\\b.*$", RegexOption.IGNORE_CASE), ""))

    fun matchingTrack(wanted: Track, candidates: List<Track>): Track? {
        val primaryArtist = text(wanted.artist.substringBefore(','))
        if (primaryArtist.isBlank() || primaryArtist == "unknown artist") return null
        val matching = candidates.filter { candidate ->
            candidate.source == StreamingSource.JIOSAAVN && candidate.id.startsWith("saavn_song_") &&
                candidate.audioUrl.isNotBlank() && title(candidate.title) == title(wanted.title) &&
                candidate.artist.split(',').any { text(it) == primaryArtist }
        }
        val album = text(wanted.album)
        return matching.maxByOrNull { candidate ->
            val candidateAlbum = text(candidate.album)
            when { candidateAlbum == album -> 3; album.isNotBlank() && candidateAlbum.startsWith(album) -> 2; else -> 0 }
        }?.let { match ->
            match.copy(id = wanted.id, resolvedCatalogId = match.id, isFavorite = wanted.isFavorite)
        }
    }
}
