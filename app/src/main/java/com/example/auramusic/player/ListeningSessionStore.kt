package com.example.auramusic.player

import android.content.Context
import com.example.auramusic.data.TrackJson
import org.json.JSONArray
import org.json.JSONObject

class ListeningSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("listening_session", Context.MODE_PRIVATE)

    fun save(state: PlayerState, progress: PlaybackProgress) {
        val json = JSONObject().apply {
            put("queue", JSONArray().apply { state.queue.forEach { put(TrackJson.encode(it)) } })
            put("currentId", state.currentTrack?.id ?: "")
            put("position", progress.currentPositionMs.coerceAtLeast(0))
            put("shuffle", state.isShuffle)
            put("repeat", state.isRepeatOne)
            put("speed", state.playbackSpeed)
            put("nextId", state.queuedNextId ?: "")
        }
        prefs.edit().putString("session", json.toString()).apply()
    }

    fun restore(): Pair<PlayerState, PlaybackProgress>? = runCatching {
        val json = JSONObject(prefs.getString("session", null) ?: return null)
        val array = json.getJSONArray("queue")
        val queue = (0 until array.length()).map { TrackJson.decode(array.getJSONObject(it)) }.distinctBy { it.id }
        val index = queue.indexOfFirst { it.id == json.optString("currentId") }
        if (index < 0) return null
        val track = queue[index]
        val position = json.optLong("position").coerceIn(0, track.durationMs.coerceAtLeast(0))
        PlayerState(queue = queue, currentTrack = track, currentIndex = index, currentPositionMs = position,
            durationMs = track.durationMs, isShuffle = json.optBoolean("shuffle"), isRepeatOne = json.optBoolean("repeat"),
            queuedNextId = json.optString("nextId").takeIf { id -> queue.any { it.id == id } },
            playbackSpeed = json.optDouble("speed", 1.0).toFloat().coerceIn(0.5f, 2f)) to
            PlaybackProgress(currentPositionMs = position, durationMs = track.durationMs)
    }.getOrNull()
}
