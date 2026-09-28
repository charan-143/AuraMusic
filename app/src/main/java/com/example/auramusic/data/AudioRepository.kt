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
import kotlin.math.absoluteValue

class AudioRepository(private val context: Context) {

    // Clean dynamic music repository without hardcoded dummy songs
    private val dynamicTracks = mutableListOf<Track>()
    private val dynamicAlbums = mutableListOf<Album>()

    private val colorfulFallbacks = listOf(
        "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
    )

    fun getMultiSourceTracks(): List<Track> = synchronized(dynamicTracks) {
        dynamicTracks.toList()
    }

    fun getAllAlbums(): List<Album> = synchronized(dynamicAlbums) {
        dynamicAlbums.toList()
    }

    fun addTracks(tracks: List<Track>) = synchronized(dynamicTracks) {
        for (track in tracks) {
            if (dynamicTracks.none { it.id == track.id }) {
                dynamicTracks.add(track)
            }
        }
    }

    fun addAlbums(albums: List<Album>) = synchronized(dynamicAlbums) {
        for (album in albums) {
            if (dynamicAlbums.none { it.id == album.id }) {
                dynamicAlbums.add(album)
            }
        }
    }

    fun clearTracks() = synchronized(dynamicTracks) {
        dynamicTracks.clear()
    }

    fun getAlbumsBySource(source: StreamingSource): List<Album> = synchronized(dynamicAlbums) {
        dynamicAlbums.filter { it.source == source }
    }

    fun searchAlbums(query: String): List<Album> = synchronized(dynamicAlbums) {
        if (query.isBlank()) return emptyList()
        dynamicAlbums.filter { album ->
            album.title.contains(query, ignoreCase = true) ||
            album.artist.contains(query, ignoreCase = true) ||
            album.source.displayName.contains(query, ignoreCase = true) ||
            album.qualityBadge.contains(query, ignoreCase = true)
        }
    }

    /**
     * Scans and loads audio files physically stored on the device via Android MediaStore.
     */
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
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
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
                val albumIdColumn = it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn) ?: "Unknown Track"
                    val artist = it.getString(artistColumn) ?: "Unknown Artist"
                    val album = it.getString(albumColumn) ?: "Unknown Album"
                    val duration = it.getLong(durationColumn)
                    val albumId = if (albumIdColumn >= 0) it.getLong(albumIdColumn) else -1L

                    val fallbackArt = colorfulFallbacks[(id.hashCode().absoluteValue) % colorfulFallbacks.size]
                    val artUri = if (albumId >= 0) {
                        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                    } else fallbackArt

                    if (duration > 10000) {
                        val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                        val track = Track(
                            id = "device_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            audioUrl = contentUri.toString(),
                            coverArtUrl = artUri,
                            isLocal = true,
                            isLossless = true,
                            source = StreamingSource.LOCAL_STORAGE,
                            qualityBadge = "LOCAL FLAC",
                            category = "Device Library"
                        )
                        deviceTracks.add(track)
                    }
                }
            }
        } catch (e: Exception) {
            // Permission or querying exception handled safely
        }

        if (deviceTracks.isNotEmpty()) {
            addTracks(deviceTracks)
        }

        deviceTracks
    }
}
