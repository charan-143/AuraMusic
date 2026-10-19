package com.example.auramusic.data

import android.content.Context
import com.example.auramusic.model.Track
import org.json.JSONObject

/** Library overrides; audio files and their embedded tags are never rewritten. */
class TrackEdits(context: Context) {
    private val prefs = context.getSharedPreferences("track_edits", Context.MODE_PRIVATE)

    fun apply(track: Track): Track = runCatching {
        val value = JSONObject(prefs.getString(track.id, null) ?: return track)
        track.copy(title = value.getString("title"), artist = value.getString("artist"),
            album = value.getString("album"), coverArtUrl = value.getString("cover"))
    }.getOrDefault(track)

    fun save(track: Track) {
        require(track.title.isNotBlank() && track.artist.isNotBlank()) { "Title and artist are required" }
        prefs.edit().putString(track.id, JSONObject().apply {
            put("title", track.title.trim()); put("artist", track.artist.trim())
            put("album", track.album.trim()); put("cover", track.coverArtUrl)
        }.toString()).apply()
    }

    fun isSelectedCover(url: String?): Boolean = !url.isNullOrBlank() && prefs.all.values.any { value ->
        runCatching { JSONObject(value as String).optString("cover") == url }.getOrDefault(false)
    }
}
