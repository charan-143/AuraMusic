package com.example.auramusic.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.auramusic.model.StreamingSource
import com.example.auramusic.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioRepository(private val context: Context) {

    // Curated Lossless FLAC, Spotify, and YouTube Music streams
    private val multiSourceTracks = listOf(
        // 1. Lossless Studio Master FLAC
        Track(
            id = "lossless_1",
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
            category = "Lossless FLAC",
            waveformData = listOf(0.2f, 0.4f, 0.7f, 0.9f, 0.8f, 0.6f, 0.4f, 0.7f, 0.9f, 0.5f, 0.8f, 0.6f, 0.3f, 0.7f, 0.9f, 0.4f)
        ),
        // 2. Spotify 320k Stream
        Track(
            id = "spotify_1",
            title = "Starboy Echoes (Spotify Stream)",
            artist = "The Weeknd & Daft Punk",
            album = "Starboy [Spotify Premium 320k]",
            durationMs = 230000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.SPOTIFY,
            qualityBadge = "SPOTIFY 320K",
            isLossless = false,
            category = "Spotify",
            waveformData = listOf(0.4f, 0.7f, 0.8f, 0.9f, 0.7f, 0.5f, 0.8f, 0.9f, 0.6f, 0.8f, 0.7f, 0.9f, 0.5f, 0.7f, 0.6f, 0.4f)
        ),
        // 3. YouTube Music Opus 256k Stream
        Track(
            id = "ytm_1",
            title = "Interstellar Main Theme (YT Music)",
            artist = "Hans Zimmer",
            album = "Interstellar OST [YT Opus 256k]",
            durationMs = 246000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.YOUTUBE_MUSIC,
            qualityBadge = "YT OPUS 256K",
            isLossless = false,
            category = "YouTube Music",
            waveformData = listOf(0.1f, 0.3f, 0.6f, 0.8f, 0.9f, 0.7f, 0.5f, 0.8f, 0.9f, 0.6f, 0.7f, 0.8f, 0.5f, 0.8f, 0.7f, 0.3f)
        ),
        // 4. Lossless Deep Ambient FLAC
        Track(
            id = "lossless_2",
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
            category = "Lossless FLAC",
            waveformData = listOf(0.1f, 0.3f, 0.5f, 0.6f, 0.8f, 0.9f, 0.7f, 0.5f, 0.8f, 0.4f, 0.6f, 0.5f, 0.3f, 0.6f, 0.7f, 0.3f)
        ),
        // 5. Spotify Lofi Beats
        Track(
            id = "spotify_2",
            title = "Coffee Cold Breeze (Spotify Lofi)",
            artist = "ChilledCow Lab",
            album = "Lofi Study Beats [Spotify 320k]",
            durationMs = 195000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.SPOTIFY,
            qualityBadge = "SPOTIFY 320K",
            isLossless = false,
            category = "Spotify",
            waveformData = listOf(0.3f, 0.6f, 0.7f, 0.8f, 0.9f, 0.5f, 0.4f, 0.6f, 0.8f, 0.7f, 0.5f, 0.8f, 0.9f, 0.4f, 0.6f, 0.5f)
        ),
        // 6. YouTube Music Electronic Chill
        Track(
            id = "ytm_2",
            title = "Resonance Sunset (YT Music)",
            artist = "HOME Synthwave",
            album = "Odyssey [YouTube Music HD]",
            durationMs = 212000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=600&auto=format&fit=crop&q=80",
            source = StreamingSource.YOUTUBE_MUSIC,
            qualityBadge = "YT OPUS 256K",
            isLossless = false,
            category = "YouTube Music",
            waveformData = listOf(0.5f, 0.8f, 0.9f, 0.7f, 0.4f, 0.8f, 0.9f, 0.6f, 0.7f, 0.8f, 0.5f, 0.9f, 0.4f, 0.8f, 0.6f, 0.7f)
        )
    )

    fun getMultiSourceTracks(): List<Track> = multiSourceTracks

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
