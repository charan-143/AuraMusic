package com.example.auramusic.data

import android.content.Context
import android.content.SharedPreferences
import com.example.auramusic.model.Playlist
import com.example.auramusic.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PlaylistRepository(private val context: Context, private val audioRepository: AudioRepository) {

    private val prefs: SharedPreferences = context.getSharedPreferences("aura_playlists", Context.MODE_PRIVATE)

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _favoriteTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteTrackIds: StateFlow<Set<String>> = _favoriteTrackIds.asStateFlow()

    init {
        loadFavorites()
        loadPlaylists()
    }

    private fun loadFavorites() {
        val saved = prefs.getStringSet("aura_favorite_track_ids", emptySet()) ?: emptySet()
        _favoriteTrackIds.value = saved.toSet()
    }

    private fun loadPlaylists() {
        val jsonString = prefs.getString("saved_playlists", null)
        if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                val loaded = mutableListOf<Playlist>()
                val allTracksMap = audioRepository.getMultiSourceTracks().associateBy { it.id }

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val name = obj.getString("name")
                    val desc = obj.optString("description", "")
                    val cover = obj.optString("coverArtUrl", "")
                    val isAlbum = obj.optBoolean("isCustomAlbum", false)
                    val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                    val tracksArray = obj.getJSONArray("trackIds")
                    val trackList = mutableListOf<Track>()
                    for (j in 0 until tracksArray.length()) {
                        val trackId = tracksArray.getString(j)
                        allTracksMap[trackId]?.let { trackList.add(it) }
                    }

                    loaded.add(
                        Playlist(
                            id = id,
                            name = name,
                            description = desc,
                            coverArtUrl = if (cover.isNotBlank()) cover else (trackList.firstOrNull()?.coverArtUrl ?: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80"),
                            tracks = trackList,
                            createdAt = createdAt,
                            isCustomAlbum = isAlbum
                        )
                    )
                }
                _playlists.value = loaded
                return
            } catch (e: Exception) {
                // Parse error fallback
            }
        }

        // Initialize with default custom album and playlist
        val initialTracks = audioRepository.getMultiSourceTracks()
        val defaultPlaylists = listOf(
            Playlist(
                id = "pl_default_fav",
                name = "Monochrome Favorites",
                description = "Hand-picked high-fidelity favorites from Spotify, YouTube Music, and Lossless Master.",
                coverArtUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
                tracks = initialTracks.take(3),
                isCustomAlbum = false
            ),
            Playlist(
                id = "album_custom_user_1",
                name = "Midnight Sessions (My Album)",
                description = "Custom compiled studio album created in Aura Music with uncompressed FLAC & Spotify streams.",
                coverArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
                tracks = initialTracks.takeLast(3),
                isCustomAlbum = true
            )
        )
        _playlists.value = defaultPlaylists
        savePlaylists(defaultPlaylists)
    }

    private fun savePlaylists(list: List<Playlist>) {
        try {
            val jsonArray = JSONArray()
            for (p in list) {
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("name", p.name)
                obj.put("description", p.description)
                obj.put("coverArtUrl", p.coverArtUrl)
                obj.put("isCustomAlbum", p.isCustomAlbum)
                obj.put("createdAt", p.createdAt)

                val trackIdsArray = JSONArray()
                p.tracks.forEach { trackIdsArray.put(it.id) }
                obj.put("trackIds", trackIdsArray)

                jsonArray.put(obj)
            }
            prefs.edit().putString("saved_playlists", jsonArray.toString()).apply()
        } catch (e: Exception) {
            // Error writing
        }
    }

    fun createPlaylist(name: String, description: String, isCustomAlbum: Boolean): Playlist {
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { if (isCustomAlbum) "New Album" else "New Playlist" },
            description = description,
            coverArtUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=600&auto=format&fit=crop&q=80",
            tracks = emptyList(),
            createdAt = System.currentTimeMillis(),
            isCustomAlbum = isCustomAlbum
        )
        val updated = listOf(newPlaylist) + _playlists.value
        _playlists.value = updated
        savePlaylists(updated)
        return newPlaylist
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        val updated = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                if (pl.tracks.any { it.id == track.id }) pl
                else pl.copy(
                    tracks = pl.tracks + track,
                    coverArtUrl = if (pl.coverArtUrl.isBlank()) track.coverArtUrl else pl.coverArtUrl
                )
            } else pl
        }
        _playlists.value = updated
        savePlaylists(updated)
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        val updated = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                pl.copy(tracks = pl.tracks.filter { it.id != trackId })
            } else pl
        }
        _playlists.value = updated
        savePlaylists(updated)
    }

    fun deletePlaylist(playlistId: String) {
        val updated = _playlists.value.filter { it.id != playlistId }
        _playlists.value = updated
        savePlaylists(updated)
    }

    fun isFavorite(trackId: String): Boolean {
        return _favoriteTrackIds.value.contains(trackId)
    }

    fun toggleFavorite(track: Track): Boolean {
        val current = _favoriteTrackIds.value.toMutableSet()
        val isNowFav = if (current.contains(track.id)) {
            current.remove(track.id)
            removeTrackFromPlaylist("pl_default_fav", track.id)
            false
        } else {
            current.add(track.id)
            addTrackToPlaylist("pl_default_fav", track)
            true
        }
        _favoriteTrackIds.value = current
        prefs.edit().putStringSet("aura_favorite_track_ids", current).apply()
        return isNowFav
    }
}
