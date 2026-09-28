package com.example.auramusic.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.auramusic.model.Album
import com.example.auramusic.model.StreamingSource
import com.example.auramusic.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioRepository(private val context: Context) {

    // Curated online albums with multiple tracks each
    private val spotifyAlbum1Tracks = listOf(
        Track(
            id = "spotify_sb_1",
            title = "Starboy Echoes",
            artist = "The Weeknd & Daft Punk",
            album = "Starboy [Spotify Premium 320k]",
            durationMs = 230000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.SPOTIFY,
            qualityBadge = "SPOTIFY 320K",
            isLossless = false,
            category = "Spotify"
        ),
        Track(
            id = "spotify_sb_2",
            title = "I Feel It Coming",
            artist = "The Weeknd & Daft Punk",
            album = "Starboy [Spotify Premium 320k]",
            durationMs = 269000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.SPOTIFY,
            qualityBadge = "SPOTIFY 320K",
            isLossless = false,
            category = "Spotify"
        ),
        Track(
            id = "spotify_sb_3",
            title = "Secrets of Night",
            artist = "The Weeknd",
            album = "Starboy [Spotify Premium 320k]",
            durationMs = 224000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.SPOTIFY,
            qualityBadge = "SPOTIFY 320K",
            isLossless = false,
            category = "Spotify"
        )
    )

    private val spotifyAlbum2Tracks = listOf(
        Track(
            id = "spotify_lofi_1",
            title = "Coffee Cold Breeze",
            artist = "ChilledCow Lab",
            album = "Lofi Study Beats [Spotify 320k]",
            durationMs = 195000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.SPOTIFY,
            qualityBadge = "SPOTIFY 320K",
            isLossless = false,
            category = "Spotify"
        ),
        Track(
            id = "spotify_lofi_2",
            title = "Rainy Window Glow",
            artist = "ChilledCow Lab",
            album = "Lofi Study Beats [Spotify 320k]",
            durationMs = 184000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.SPOTIFY,
            qualityBadge = "SPOTIFY 320K",
            isLossless = false,
            category = "Spotify"
        )
    )

    private val ytmAlbum1Tracks = listOf(
        Track(
            id = "ytm_is_1",
            title = "Interstellar Main Theme",
            artist = "Hans Zimmer",
            album = "Interstellar OST [YT Opus 256k]",
            durationMs = 246000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.YOUTUBE_MUSIC,
            qualityBadge = "YT OPUS 256K",
            isLossless = false,
            category = "YouTube Music"
        ),
        Track(
            id = "ytm_is_2",
            title = "Cornfield Chase (YT HD)",
            artist = "Hans Zimmer",
            album = "Interstellar OST [YT Opus 256k]",
            durationMs = 191000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.YOUTUBE_MUSIC,
            qualityBadge = "YT OPUS 256K",
            isLossless = false,
            category = "YouTube Music"
        ),
        Track(
            id = "ytm_is_3",
            title = "No Time for Caution",
            artist = "Hans Zimmer",
            album = "Interstellar OST [YT Opus 256k]",
            durationMs = 244000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.YOUTUBE_MUSIC,
            qualityBadge = "YT OPUS 256K",
            isLossless = false,
            category = "YouTube Music"
        )
    )

    private val ytmAlbum2Tracks = listOf(
        Track(
            id = "ytm_ody_1",
            title = "Resonance Sunset",
            artist = "HOME Synthwave",
            album = "Odyssey [YouTube Music HD]",
            durationMs = 212000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.YOUTUBE_MUSIC,
            qualityBadge = "YT OPUS 256K",
            isLossless = false,
            category = "YouTube Music"
        ),
        Track(
            id = "ytm_ody_2",
            title = "Decay Horizons",
            artist = "HOME Synthwave",
            album = "Odyssey [YouTube Music HD]",
            durationMs = 198000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.YOUTUBE_MUSIC,
            qualityBadge = "YT OPUS 256K",
            isLossless = false,
            category = "YouTube Music"
        )
    )

    private val losslessAlbum1Tracks = listOf(
        Track(
            id = "lossless_as_1",
            title = "Midnight Monochrome (Lossless Master)",
            artist = "Aura Sound Lab",
            album = "Audiophile Sessions [24-bit/96kHz]",
            durationMs = 214000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.LOSSLESS_FLAC,
            qualityBadge = "24-BIT FLAC",
            isLossless = true,
            isCachedOffline = true,
            category = "Lossless FLAC"
        ),
        Track(
            id = "lossless_as_2",
            title = "OLED Dark Resonance (Master)",
            artist = "Aura Sound Lab",
            album = "Audiophile Sessions [24-bit/96kHz]",
            durationMs = 242000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.LOSSLESS_FLAC,
            qualityBadge = "24-BIT FLAC",
            isLossless = true,
            isCachedOffline = true,
            category = "Lossless FLAC"
        ),
        Track(
            id = "lossless_as_3",
            title = "Subtle Shadows (Acoustic Master)",
            artist = "Aura Sound Lab",
            album = "Audiophile Sessions [24-bit/96kHz]",
            durationMs = 228000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.LOSSLESS_FLAC,
            qualityBadge = "24-BIT FLAC",
            isLossless = true,
            isCachedOffline = true,
            category = "Lossless FLAC"
        )
    )

    private val losslessAlbum2Tracks = listOf(
        Track(
            id = "lossless_me_1",
            title = "Silent Pixel Drift (Pure FLAC)",
            artist = "Tensor Dreams",
            album = "Material Echoes [Hi-Res FLAC]",
            durationMs = 186000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.LOSSLESS_FLAC,
            qualityBadge = "24-BIT FLAC",
            isLossless = true,
            isCachedOffline = true,
            category = "Lossless FLAC"
        ),
        Track(
            id = "lossless_me_2",
            title = "Spring Physics Walk (Lossless)",
            artist = "Tensor Dreams",
            album = "Material Echoes [Hi-Res FLAC]",
            durationMs = 195000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.LOSSLESS_FLAC,
            qualityBadge = "24-BIT FLAC",
            isLossless = true,
            isCachedOffline = true,
            category = "Lossless FLAC"
        )
    )

    // Curated online albums
    private val onlineAlbums = listOf(
        Album(
            id = "album_spotify_starboy",
            title = "Starboy",
            artist = "The Weeknd",
            coverArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            year = "2016",
            source = StreamingSource.SPOTIFY,
            qualityBadge = "SPOTIFY 320K",
            isLossless = false,
            description = "High-energy synth-pop & R&B streaming in 320kbps Ogg/AAC directly from Spotify Hi-Fi.",
            tracks = spotifyAlbum1Tracks
        ),
        Album(
            id = "album_spotify_lofi",
            title = "Lofi Study Beats",
            artist = "ChilledCow Lab",
            coverArtUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
            year = "2024",
            source = StreamingSource.SPOTIFY,
            qualityBadge = "SPOTIFY 320K",
            isLossless = false,
            description = "Chill, nostalgic lofi hip hop beats for deep focus, reading, and night relaxation.",
            tracks = spotifyAlbum2Tracks
        ),
        Album(
            id = "album_ytm_interstellar",
            title = "Interstellar OST",
            artist = "Hans Zimmer",
            coverArtUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80",
            year = "2014",
            source = StreamingSource.YOUTUBE_MUSIC,
            qualityBadge = "YT OPUS 256K",
            isLossless = false,
            description = "Epic cinematic organ and orchestral compositions streaming in YouTube Music Opus 256kbps HD.",
            tracks = ytmAlbum1Tracks
        ),
        Album(
            id = "album_ytm_odyssey",
            title = "Odyssey",
            artist = "HOME Synthwave",
            coverArtUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=600&auto=format&fit=crop&q=80",
            year = "2018",
            source = StreamingSource.YOUTUBE_MUSIC,
            qualityBadge = "YT OPUS 256K",
            isLossless = false,
            description = "Warm analog synthesizers, tape drift, and melancholic retrowave melodies on YouTube Music.",
            tracks = ytmAlbum2Tracks
        ),
        Album(
            id = "album_lossless_audiophile",
            title = "Audiophile Sessions",
            artist = "Aura Sound Lab",
            coverArtUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
            year = "2024",
            source = StreamingSource.LOSSLESS_FLAC,
            qualityBadge = "24-BIT FLAC",
            isLossless = true,
            description = "Direct studio master recordings sampled at 24-bit/96kHz for uncompressed acoustic clarity.",
            tracks = losslessAlbum1Tracks
        ),
        Album(
            id = "album_lossless_echoes",
            title = "Material Echoes",
            artist = "Tensor Dreams",
            coverArtUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=600&auto=format&fit=crop&q=80",
            year = "2024",
            source = StreamingSource.LOSSLESS_FLAC,
            qualityBadge = "24-BIT FLAC",
            isLossless = true,
            description = "Subtle monochrome textures, modular synthesizers, and ambient acoustic field recordings in FLAC.",
            tracks = losslessAlbum2Tracks
        )
    )

    // Flat list of all multi-source streaming tracks
    private val allMultiSourceTracks: List<Track> = onlineAlbums.flatMap { it.tracks }

    fun getMultiSourceTracks(): List<Track> = allMultiSourceTracks

    fun getAllAlbums(): List<Album> = onlineAlbums

    fun getAlbumsBySource(source: StreamingSource): List<Album> =
        onlineAlbums.filter { it.source == source }

    fun searchAlbums(query: String): List<Album> {
        if (query.isBlank()) return emptyList()
        return onlineAlbums.filter { album ->
            album.title.contains(query, ignoreCase = true) ||
            album.artist.contains(query, ignoreCase = true) ||
            album.source.displayName.contains(query, ignoreCase = true) ||
            album.qualityBadge.contains(query, ignoreCase = true)
        }
    }

    suspend fun loadDeviceAudio(): List<Track> = withContext(Dispatchers.IO) {
        val deviceTracks = mutableListOf<Track>()
        val resolver: ContentResolver = context.contentResolver

        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor: Cursor? = resolver.query(collection, projection, selection, null, sortOrder)
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn) ?: "Unknown Track"
                    val artist = it.getString(artistColumn) ?: "Unknown Artist"
                    val album = it.getString(albumColumn) ?: "Unknown Album"
                    val duration = it.getLong(durationColumn)

                    if (duration > 10000) {
                        val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                        deviceTracks.add(
                            Track(
                                id = "device_$id",
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = duration,
                                audioUrl = contentUri.toString(),
                                isLocal = true,
                                isLossless = true,
                                source = StreamingSource.LOCAL_STORAGE,
                                qualityBadge = "LOCAL FLAC",
                                category = "Device Library"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Permission or querying exception
        }

        deviceTracks
    }
}
