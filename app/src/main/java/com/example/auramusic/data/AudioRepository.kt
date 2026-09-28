package com.example.auramusic.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.auramusic.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioRepository(private val context: Context) {

    // Curated royalty-free monochrome-themed soundscapes
    private val curatedTracks = listOf(
        Track(
            id = "curated_1",
            title = "Midnight Monochrome",
            artist = "Aura Sound Lab",
            album = "Pixel Resonance Vol. 1",
            durationMs = 214000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
            category = "Lofi Chill",
            waveformData = listOf(0.2f, 0.4f, 0.7f, 0.9f, 0.8f, 0.6f, 0.4f, 0.7f, 0.9f, 0.5f, 0.8f, 0.6f, 0.3f, 0.7f, 0.9f, 0.4f)
        ),
        Track(
            id = "curated_2",
            title = "Silent Pixel Drift",
            artist = "Tensor Dreams",
            album = "Material Echoes",
            durationMs = 186000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=600&auto=format&fit=crop&q=80",
            category = "Deep Ambient",
            waveformData = listOf(0.1f, 0.3f, 0.5f, 0.6f, 0.8f, 0.9f, 0.7f, 0.5f, 0.8f, 0.4f, 0.6f, 0.5f, 0.3f, 0.6f, 0.7f, 0.3f)
        ),
        Track(
            id = "curated_3",
            title = "OLED Dark Resonance",
            artist = "Noir Synth",
            album = "Pitch Black Horizons",
            durationMs = 242000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=600&auto=format&fit=crop&q=80",
            category = "Electronic",
            waveformData = listOf(0.5f, 0.8f, 0.9f, 0.7f, 0.4f, 0.8f, 0.9f, 0.6f, 0.7f, 0.8f, 0.5f, 0.9f, 0.4f, 0.8f, 0.6f, 0.7f)
        ),
        Track(
            id = "curated_4",
            title = "Spring Physics Walk",
            artist = "Kotlin Pulse",
            album = "Pixel Resonance Vol. 1",
            durationMs = 195000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
            category = "Lofi Chill",
            waveformData = listOf(0.3f, 0.6f, 0.7f, 0.8f, 0.9f, 0.5f, 0.4f, 0.6f, 0.8f, 0.7f, 0.5f, 0.8f, 0.9f, 0.4f, 0.6f, 0.5f)
        ),
        Track(
            id = "curated_5",
            title = "Subtle Shadows",
            artist = "Minimalist Echo",
            album = "Material Echoes",
            durationMs = 228000L,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
            coverArtUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
            category = "Deep Ambient",
            waveformData = listOf(0.2f, 0.3f, 0.6f, 0.8f, 0.7f, 0.5f, 0.3f, 0.6f, 0.7f, 0.8f, 0.4f, 0.5f, 0.6f, 0.3f, 0.5f, 0.2f)
        )
    )

    fun getCuratedTracks(): List<Track> = curatedTracks

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

                    if (duration > 10000) { // filter out ringtones and short clips
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
