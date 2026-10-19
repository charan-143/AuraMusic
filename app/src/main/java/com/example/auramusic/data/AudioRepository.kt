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
    private val edits = TrackEdits(context)

    // Clean dynamic music repository without hardcoded dummy songs
    private val dynamicTracks = mutableListOf<Track>()
    private val dynamicAlbums = mutableListOf<Album>()


    fun getMultiSourceTracks(): List<Track> = synchronized(dynamicTracks) {
        dynamicTracks.map { edits.apply(it) }
    }

    fun getAllAlbums(): List<Album> = synchronized(dynamicAlbums) {
        dynamicAlbums.toList()
    }

    fun addTracks(tracks: List<Track>) = synchronized(dynamicTracks) {
        val ids = dynamicTracks.mapTo(HashSet()) { it.id }
        for (track in tracks) {
            if (ids.add(track.id)) {
                dynamicTracks.add(track)
            }
        }
    }

    fun addAlbums(albums: List<Album>) = synchronized(dynamicAlbums) {
        val ids = dynamicAlbums.mapTo(HashSet()) { it.id }
        for (album in albums) {
            if (ids.add(album.id)) {
                dynamicAlbums.add(album)
            }
        }
    }

    fun clearTracks() = synchronized(dynamicTracks) {
        dynamicTracks.clear()
    }

    fun updateTrack(track: Track) = synchronized(dynamicTracks) {
        val index = dynamicTracks.indexOfFirst { it.id == track.id }
        if (index >= 0) dynamicTracks[index] = track else dynamicTracks.add(track)
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
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.DURATION} >= 15000"
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
                val dataColumn = it.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn) ?: "Unknown Track"
                    val artist = it.getString(artistColumn) ?: "Unknown Artist"
                    val album = it.getString(albumColumn) ?: "Unknown Album"
                    val duration = it.getLong(durationColumn)
                    val albumId = if (albumIdColumn >= 0) it.getLong(albumIdColumn) else -1L
                    val filePath = if (dataColumn >= 0) it.getString(dataColumn) else null

                    val artUri = if (albumId >= 0) {
                        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                    } else ""

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
                            isLossless = filePath?.endsWith(".flac", ignoreCase = true) == true || filePath?.endsWith(".wav", ignoreCase = true) == true,
                            source = StreamingSource.LOCAL_STORAGE,
                            qualityBadge = "LOCAL AUDIO",
                            category = "Device Library",
                            localFilePath = filePath
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
