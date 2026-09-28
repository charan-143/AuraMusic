package com.example.auramusic.recommendation

import com.example.auramusic.data.AudioRepository
import com.example.auramusic.model.Album
import com.example.auramusic.model.RecommendationSection
import com.example.auramusic.model.StreamingSource
import com.example.auramusic.model.Track
import com.example.auramusic.network.OnlineMusicSearchService

class RecommendationEngine(private val audioRepository: AudioRepository) {

    // Resilient fallback catalogue ensuring Home Tab is NEVER empty, even offline on first launch
    private val curatedFallbackTracks by lazy {
        listOf(
            Track(
                id = "curated_1",
                title = "Believer",
                artist = "Imagine Dragons",
                album = "Evolve",
                durationMs = 204000L,
                audioUrl = OnlineMusicSearchService.decryptMediaUrl("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy3oAkJDMbq8VkXOZU65DcFdrDhsTWIRT7u/8vMld9DHecVMFCBmD5/Rw7tS9a8Gtq"),
                coverArtUrl = "https://c.saavncdn.com/248/Evolve-English-2018-20260605220036-500x500.jpg",
                source = StreamingSource.SPOTIFY,
                qualityBadge = "SPOTIFY 320K",
                isLossless = true,
                category = "Spotify"
            ),
            Track(
                id = "curated_2",
                title = "Time",
                artist = "Hans Zimmer",
                album = "Inception (OST)",
                durationMs = 275000L,
                audioUrl = OnlineMusicSearchService.decryptMediaUrl("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy0sB13nYLqBDMTtFUYrKmMui3nVSjruDwLS/IRFPQXAJAhv8jbZTiwRw7tS9a8Gtq"),
                coverArtUrl = "https://c.saavncdn.com/139/Inception-English-2010-500x500.jpg",
                source = StreamingSource.YOUTUBE_MUSIC,
                qualityBadge = "YT LOSSLESS",
                isLossless = true,
                category = "YouTube Music"
            ),
            Track(
                id = "curated_3",
                title = "Starboy",
                artist = "The Weeknd, Daft Punk",
                album = "Starboy",
                durationMs = 230000L,
                audioUrl = OnlineMusicSearchService.decryptMediaUrl("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDynTibb8fRyG1tbcCQQQR3MOwq4OxvkVNxRxh5wdnqMsXr4IMM0hwwihw7tS9a8Gtq"),
                coverArtUrl = "https://c.saavncdn.com/410/Starboy-English-2016-500x500.jpg",
                source = StreamingSource.SPOTIFY,
                qualityBadge = "320K LOSSLESS",
                isLossless = true,
                category = "Spotify"
            ),
            Track(
                id = "curated_4",
                title = "Viva La Vida",
                artist = "Coldplay",
                album = "Viva La Vida",
                durationMs = 242000L,
                audioUrl = OnlineMusicSearchService.decryptMediaUrl("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyoW+9vqQ1AvpNtBuxtRV/trXO0wRsM9SJAudwVBa8NXR0K5rNf9PdHhw7tS9a8Gtq"),
                coverArtUrl = "https://c.saavncdn.com/816/Viva-La-Vida-English-2008-500x500.jpg",
                source = StreamingSource.LOSSLESS_FLAC,
                qualityBadge = "24-BIT FLAC",
                isLossless = true,
                category = "Lossless"
            )
        )
    }

    private val curatedFallbackAlbums by lazy {
        listOf(
            Album(
                id = "curated_album_1",
                title = "Evolve",
                artist = "Imagine Dragons",
                coverArtUrl = "https://c.saavncdn.com/248/Evolve-English-2018-20260605220036-500x500.jpg",
                year = "2018",
                source = StreamingSource.SPOTIFY,
                qualityBadge = "320K LOSSLESS",
                isLossless = true,
                description = "Multi-platinum studio album featuring Believer and Whatever It Takes.",
                tracks = curatedFallbackTracks.take(2)
            ),
            Album(
                id = "curated_album_2",
                title = "Inception (Original Soundtrack)",
                artist = "Hans Zimmer",
                coverArtUrl = "https://c.saavncdn.com/139/Inception-English-2010-500x500.jpg",
                year = "2010",
                source = StreamingSource.YOUTUBE_MUSIC,
                qualityBadge = "HD OPUS 256K",
                isLossless = true,
                description = "Iconic cinematic masterpiece featuring Time and Dream Is Collapsing.",
                tracks = curatedFallbackTracks.drop(1).take(2)
            )
        )
    }

    /**
     * Generates algorithmic recommendation sections for both Songs and Albums.
     * Incorporates dynamically loaded online tracks/albums with graceful resilient fallbacks.
     */
    fun getOnlineRecommendationSections(
        activeTrack: Track?,
        providedTracks: List<Track> = emptyList(),
        providedAlbums: List<Album> = emptyList()
    ): List<RecommendationSection> {
        val allAlbums = when {
            providedAlbums.isNotEmpty() -> providedAlbums
            audioRepository.getAllAlbums().isNotEmpty() -> audioRepository.getAllAlbums()
            else -> curatedFallbackAlbums
        }

        val allTracks = when {
            providedTracks.isNotEmpty() -> providedTracks
            audioRepository.getMultiSourceTracks().isNotEmpty() -> audioRepository.getMultiSourceTracks()
            else -> curatedFallbackTracks
        }

        // 1. Personalized "For You" Section
        val forYouAlbums = if (activeTrack != null) {
            allAlbums.sortedByDescending { it.source == activeTrack.source || it.artist == activeTrack.artist }
                .take(6)
        } else {
            allAlbums.take(6)
        }
        val forYouTracks = if (activeTrack != null) {
            allTracks.filter { it.id != activeTrack.id && (it.source == activeTrack.source || it.category == activeTrack.category) }
                .ifEmpty { allTracks.take(6) }
        } else {
            allTracks.shuffled().take(6)
        }

        val forYouSection = RecommendationSection(
            id = "rec_for_you",
            title = "Recommended For You",
            subtitle = if (activeTrack != null) "Curated from ${activeTrack.title}" else "Personalized algorithmic mix",
            source = activeTrack?.source ?: StreamingSource.SPOTIFY,
            albums = forYouAlbums,
            tracks = forYouTracks
        )

        // 2. Spotify Releases & Hits
        val spotifyAlbums = allAlbums.filter { it.source == StreamingSource.SPOTIFY }.ifEmpty { allAlbums.take(3) }
        val spotifyTracks = allTracks.filter { it.source == StreamingSource.SPOTIFY }.ifEmpty { allTracks.take(4) }
        val spotifySection = RecommendationSection(
            id = "rec_spotify_trending",
            title = "Trending on Spotify",
            subtitle = "Popular 320kbps full-length streams & albums",
            source = StreamingSource.SPOTIFY,
            albums = spotifyAlbums,
            tracks = spotifyTracks
        )

        // 3. YouTube Music Releases
        val ytmAlbums = allAlbums.filter { it.source == StreamingSource.YOUTUBE_MUSIC }.ifEmpty { allAlbums.drop(1).take(3) }
        val ytmTracks = allTracks.filter { it.source == StreamingSource.YOUTUBE_MUSIC }.ifEmpty { allTracks.drop(2).take(4) }
        val ytmSection = RecommendationSection(
            id = "rec_ytm_hot",
            title = "Hot on YouTube Music",
            subtitle = "High-definition Opus master albums and soundscapes",
            source = StreamingSource.YOUTUBE_MUSIC,
            albums = ytmAlbums,
            tracks = ytmTracks
        )

        // 4. Audiophile Lossless Masterworks
        val losslessAlbums = allAlbums.filter { it.isLossless }.ifEmpty { allAlbums.take(2) }
        val losslessTracks = allTracks.filter { it.isLossless }.ifEmpty { allTracks.take(4) }
        val losslessSection = RecommendationSection(
            id = "rec_lossless_masters",
            title = "Lossless Masterworks",
            subtitle = "Studio 320kbps recordings in pure acoustic fidelity",
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
