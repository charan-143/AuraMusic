package com.example.auramusic.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

class FavoritesManager private constructor(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "aura_music_favorites"
        private const val KEY_FAVORITES = "fav_track_ids"
        private const val KEY_PLAY_COUNTS = "play_counts_json"
        private const val KEY_RECENTLY_PLAYED = "recently_played_ids"

        @Volatile
        private var instance: FavoritesManager? = null

        fun getInstance(context: Context): FavoritesManager {
            return instance ?: synchronized(this) {
                instance ?: FavoritesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _recentlyPlayedIds = MutableStateFlow<List<String>>(emptyList())
    val recentlyPlayedIds: StateFlow<List<String>> = _recentlyPlayedIds.asStateFlow()

    private val _playCountsFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
    val playCountsFlow: StateFlow<Map<String, Int>> = _playCountsFlow.asStateFlow()

    private val playCounts = mutableMapOf<String, Int>()

    init {
        loadPersistedData()
    }

    fun isFavorite(trackId: String): Boolean {
        return _favoriteIds.value.contains(trackId)
    }

    fun toggleFavorite(trackId: String): Boolean {
        var isNowFav = false
        _favoriteIds.update { current ->
            if (current.contains(trackId)) {
                isNowFav = false
                current - trackId
            } else {
                isNowFav = true
                current + trackId
            }
        }
        persistFavorites()
        return isNowFav
    }

    fun recordPlay(trackId: String) {
        if (trackId.isBlank()) return

        // 1. Update play count
        val count = (playCounts[trackId] ?: 0) + 1
        playCounts[trackId] = count
        _playCountsFlow.value = playCounts.toMap()
        persistPlayCounts()

        // 2. Update recently played
        recordRecentlyPlayed(trackId)
    }

    fun recordRecentlyPlayed(trackId: String) {
        if (trackId.isBlank()) return
        _recentlyPlayedIds.update { current ->
            val updated = listOf(trackId) + current.filterNot { it == trackId }
            updated.take(50)
        }
        persistRecentlyPlayed()
    }

    fun getPlayCount(trackId: String): Int {
        return playCounts[trackId] ?: 0
    }

    fun getMostPlayedIds(): List<String> {
        return playCounts.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { it.key }
            .take(30)
    }

    private fun loadPersistedData() {
        val favs = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        _favoriteIds.value = favs.toSet()

        val recentString = prefs.getString(KEY_RECENTLY_PLAYED, "[]") ?: "[]"
        try {
            val arr = JSONArray(recentString)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val id = arr.getString(i)
                if (id.isNotBlank()) list.add(id)
            }
            _recentlyPlayedIds.value = list
        } catch (ignored: Exception) {}

        val countString = prefs.getString(KEY_PLAY_COUNTS, "{}") ?: "{}"
        try {
            val obj = JSONObject(countString)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                playCounts[k] = obj.optInt(k, 0)
            }
            _playCountsFlow.value = playCounts.toMap()
        } catch (ignored: Exception) {}
    }

    private fun persistFavorites() {
        prefs.edit().putStringSet(KEY_FAVORITES, _favoriteIds.value).apply()
    }

    private fun persistRecentlyPlayed() {
        val arr = JSONArray()
        _recentlyPlayedIds.value.forEach { arr.put(it) }
        prefs.edit().putString(KEY_RECENTLY_PLAYED, arr.toString()).apply()
    }

    private fun persistPlayCounts() {
        val obj = JSONObject()
        playCounts.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(KEY_PLAY_COUNTS, obj.toString()).apply()
    }
}
