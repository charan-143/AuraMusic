package com.example.auramusic.recommendation

import com.example.auramusic.data.AudioRepository
import com.example.auramusic.model.Album
import com.example.auramusic.model.RecommendationSection
import com.example.auramusic.model.StreamingSource
import com.example.auramusic.model.Track

class RecommendationEngine(private val audioRepository: AudioRepository) {

    /**
     * Generates algorithmic recommendation sections for both Songs and Albums.
     * Incorporates Spotify, YouTube Music, and Studio Lossless Master catalogs.
     */
    fun getOnlineRecommendationSections(activeTrack: Track?): List<RecommendationSection> {
        val allAlbums = audioRepository.getAllAlbums()
        val allTracks = audioRepository.getMultiSourceTracks()

        // 1. Personalized "For You" Section
        val forYouAlbums = if (activeTrack != null) {
            allAlbums.sortedByDescending { it.source == activeTrack.source || it.artist == activeTrack.artist }
                .take(3)
        } else {
            allAlbums.take(3)
        }
        val forYouTracks = if (activeTrack != null) {
            allTracks.filter { it.id != activeTrack.id && (it.source == activeTrack.source || it.category == activeTrack.category) }
                .ifEmpty { allTracks.take(4) }
        } else {
            allTracks.shuffled().take(4)
        }

        val forYouSection = RecommendationSection(
            id = "rec_for_you",
            title = "Recommended For You",
            subtitle = if (activeTrack != null) "Based on ${activeTrack.title}" else "Curated algorithmic mix",
            source = activeTrack?.source ?: StreamingSource.SPOTIFY,
            albums = forYouAlbums,
            tracks = forYouTracks
        )

        // 2. Spotify Trending Releases (Albums & Tracks)
        val spotifyAlbums = allAlbums.filter { it.source == StreamingSource.SPOTIFY }
        val spotifyTracks = allTracks.filter { it.source == StreamingSource.SPOTIFY }
        val spotifySection = RecommendationSection(
            id = "rec_spotify_trending",
            title = "Trending on Spotify",
            subtitle = "Popular 320kbps Hi-Fi streams & albums",
            source = StreamingSource.SPOTIFY,
            albums = spotifyAlbums,
            tracks = spotifyTracks
        )

        // 3. YouTube Music Hot Charts (Albums & Tracks)
        val ytmAlbums = allAlbums.filter { it.source == StreamingSource.YOUTUBE_MUSIC }
        val ytmTracks = allTracks.filter { it.source == StreamingSource.YOUTUBE_MUSIC }
        val ytmSection = RecommendationSection(
            id = "rec_ytm_hot",
            title = "Hot on YouTube Music",
            subtitle = "High-definition Opus 256k master albums",
            source = StreamingSource.YOUTUBE_MUSIC,
            albums = ytmAlbums,
            tracks = ytmTracks
        )

        // 4. Audiophile Lossless Masterworks
        val losslessAlbums = allAlbums.filter { it.source == StreamingSource.LOSSLESS_FLAC }
        val losslessTracks = allTracks.filter { it.source == StreamingSource.LOSSLESS_FLAC }
        val losslessSection = RecommendationSection(
            id = "rec_lossless_masters",
            title = "Lossless Masterworks",
            subtitle = "Direct studio recordings in uncompressed 24-bit FLAC",
            source = StreamingSource.LOSSLESS_FLAC,
            albums = losslessAlbums,
            tracks = losslessTracks
        )

        return listOf(forYouSection, spotifySection, ytmSection, losslessSection)
    }

    /**
     * Finds related songs and albums for a specific track.
     */
    fun getRelatedContent(track: Track): Pair<List<Album>, List<Track>> {
        val relatedAlbums = audioRepository.getAllAlbums().filter {
            it.source == track.source || it.artist.contains(track.artist, ignoreCase = true)
        }
        val relatedTracks = audioRepository.getMultiSourceTracks().filter {
            it.id != track.id && (it.source == track.source || it.category == track.category)
        }
        return Pair(relatedAlbums, relatedTracks)
    }
}
