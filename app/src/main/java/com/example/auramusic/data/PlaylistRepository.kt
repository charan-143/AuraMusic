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
    private val unresolvedTrackIds = mutableMapOf<String, List<String>>()
    private val savedTrackOrder = mutableMapOf<String, List<String>>()

    private fun relinkLocalTrack(saved: Track, library: Collection<Track>): Track {
        if (!saved.isLocal && !saved.audioUrl.startsWith("content:") && !saved.audioUrl.startsWith("file:")) {
            return library.firstOrNull { it.id == saved.id }?.let { saved.copy(audioUrl = it.audioUrl) } ?: saved
        }
        val filename = saved.localFilePath?.let { java.io.File(it).name }
        val match = library.firstOrNull { candidate -> candidate.isLocal &&
            ((candidate.title.equals(saved.title, true) && candidate.artist.equals(saved.artist, true)) ||
                (!filename.isNullOrBlank() && candidate.localFilePath?.let { java.io.File(it).name } == filename)) }
        return if (match == null) saved else saved.copy(audioUrl = match.audioUrl, localFilePath = match.localFilePath,
            isLocal = true, isCachedOffline = false)
    }

    @Synchronized
    fun refreshTracks() { loadPlaylists() }

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
                    savedTrackOrder[id] = (0 until tracksArray.length()).map { tracksArray.getString(it) }
                    val snapshots = obj.optJSONArray("tracks") ?: JSONArray()
                    val savedTracks = (0 until snapshots.length()).mapNotNull { index ->
                        runCatching { TrackJson.decode(snapshots.getJSONObject(index)) }.getOrNull()
                    }.associateBy { it.id }
                    val trackList = mutableListOf<Track>()
                    val missingIds = mutableListOf<String>()
                    for (j in 0 until tracksArray.length()) {
                        val trackId = tracksArray.getString(j)
                        val track = (savedTracks[trackId]?.let { relinkLocalTrack(it, allTracksMap.values) } ?: allTracksMap[trackId])?.let { TrackEdits(context).apply(it) }
                        if (track != null) trackList.add(track) else missingIds.add(trackId)
                    }
                    unresolvedTrackIds[id] = missingIds

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
                description = "Your favorite local and online songs.",
                coverArtUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
                tracks = initialTracks.take(3),
                isCustomAlbum = false
            ),
            Playlist(
                id = "album_custom_user_1",
                name = "Midnight Sessions (My Album)",
                description = "A custom collection created in Aura Music.",
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
                val desiredIds = (p.tracks.map { it.id } + unresolvedTrackIds[p.id].orEmpty()).distinct()
                val orderedIds = savedTrackOrder[p.id].orEmpty().filter { it in desiredIds } +
                    desiredIds.filterNot { it in savedTrackOrder[p.id].orEmpty() }
                savedTrackOrder[p.id] = orderedIds
                orderedIds.forEach { trackIdsArray.put(it) }
                obj.put("trackIds", trackIdsArray)
                obj.put("tracks", JSONArray().apply { p.tracks.forEach { put(TrackJson.encode(it)) } })

                jsonArray.put(obj)
            }
            prefs.edit().putString("saved_playlists", jsonArray.toString()).apply()
        } catch (e: Exception) {
            // Error writing
        }
    }

    @Synchronized
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

    @Synchronized
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

    @Synchronized
    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        unresolvedTrackIds[playlistId] = unresolvedTrackIds[playlistId].orEmpty().filterNot { it == trackId }
        val updated = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                pl.copy(tracks = pl.tracks.filter { it.id != trackId })
            } else pl
        }
        _playlists.value = updated
        savePlaylists(updated)
    }

    @Synchronized
    fun deletePlaylist(playlistId: String) {
        unresolvedTrackIds.remove(playlistId)
        val updated = _playlists.value.filter { it.id != playlistId }
        _playlists.value = updated
        savePlaylists(updated)
    }

    fun isFavorite(trackId: String): Boolean {
        return _favoriteTrackIds.value.contains(trackId)
    }

    @Synchronized
    fun updateTrack(track: Track) {
        val updated = _playlists.value.map { playlist -> playlist.copy(tracks = playlist.tracks.map { if (it.id == track.id) track else it }) }
        _playlists.value = updated
        savePlaylists(updated)
    }

    @Synchronized
    fun exportBackup(): String {
        savePlaylists(_playlists.value)
        val assets = JSONObject()
        _playlists.value.flatMap { listOf(it.coverArtUrl) + it.tracks.map { track -> track.coverArtUrl } }.distinct().forEach { url ->
            val uri = android.net.Uri.parse(url)
            if (uri.scheme == "file" && uri.path?.startsWith(java.io.File(context.filesDir, "custom_artwork").path + java.io.File.separator) == true) {
                val file = java.io.File(requireNotNull(uri.path))
                if (file.isFile && file.length() <= 2 * 1024 * 1024) assets.put(url, android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.NO_WRAP))
            }
        }
        val result = JSONObject().apply {
            put("format", "aura-playlists"); put("version", 1)
            put("playlists", JSONArray(prefs.getString("saved_playlists", "[]")))
            put("favorites", JSONArray(_favoriteTrackIds.value.toList()))
            put("artwork", assets)
        }.toString(2)
        require(result.toByteArray().size <= 10 * 1024 * 1024) { "Backup exceeds 10 MB; use fewer custom covers" }
        return result
    }

    /** Validate the entire document before merging. Existing playlists are never replaced. */
    @Synchronized
    fun importBackup(text: String): Int {
        require(text.toByteArray().size <= 10 * 1024 * 1024) { "Backup is larger than 10 MB" }
        val root = JSONObject(text)
        require(root.getString("format") == "aura-playlists" && root.getInt("version") == 1) { "Unsupported playlist backup" }
        val array = root.getJSONArray("playlists")
        require(array.length() <= 1000) { "Too many playlists" }
        val incoming = (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            require(obj.getString("name").isNotBlank()) { "Playlist name is required" }
            val ids = obj.getJSONArray("trackIds")
            require(ids.length() <= 10000) { "Playlist is too large" }
            val tracks = obj.optJSONArray("tracks") ?: JSONArray()
            for (j in 0 until tracks.length()) TrackJson.decode(tracks.getJSONObject(j))
            JSONObject(obj.toString()).apply { put("id", UUID.randomUUID().toString()) }
        }
        val favorites = root.optJSONArray("favorites") ?: JSONArray()
        val favoriteIds = _favoriteTrackIds.value + (0 until favorites.length()).map { favorites.getString(it) }
        val assets = root.optJSONObject("artwork") ?: JSONObject()
        val decodedAssets = assets.keys().asSequence().associateWith { key ->
            android.util.Base64.decode(assets.getString(key), android.util.Base64.DEFAULT).also { bytes ->
                require(bytes.size <= 2 * 1024 * 1024) { "Artwork is too large" }
                val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                require(options.outWidth > 0 && options.outHeight > 0) { "Invalid backup artwork" }
            }
        }
        val replacements = decodedAssets.mapValues { (_, bytes) ->
            val directory = java.io.File(context.filesDir, "custom_artwork").apply { mkdirs() }
            val file = java.io.File(directory, "${UUID.randomUUID()}.jpg").apply { writeBytes(bytes) }
            android.net.Uri.fromFile(file).toString()
        }
        val library = audioRepository.getMultiSourceTracks()
        incoming.forEach { playlist ->
            val cover = playlist.optString("coverArtUrl")
            replacements[cover]?.let { playlist.put("coverArtUrl", it) }
            val snapshots = playlist.optJSONArray("tracks") ?: JSONArray()
            for (i in 0 until snapshots.length()) {
                val obj = snapshots.getJSONObject(i)
                replacements[obj.optString("coverArtUrl")]?.let { obj.put("coverArtUrl", it) }
                val saved = TrackJson.decode(obj)
                val resolved = relinkLocalTrack(saved, library)
                if (saved.isLocal) {
                    val usable = if (resolved.audioUrl != saved.audioUrl) resolved else resolved.copy(audioUrl = "", isCachedOffline = false)
                    snapshots.put(i, TrackJson.encode(usable))
                }
            }
        }
        savePlaylists(_playlists.value)
        val merged = JSONArray(prefs.getString("saved_playlists", "[]"))
        incoming.forEach { merged.put(it) }
        check(prefs.edit().putString("saved_playlists", merged.toString()).putStringSet("aura_favorite_track_ids", favoriteIds).commit()) { "Could not save imported playlists" }
        _favoriteTrackIds.value = favoriteIds
        loadPlaylists()
        return incoming.size
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
