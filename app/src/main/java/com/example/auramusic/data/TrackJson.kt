package com.example.auramusic.data

import com.example.auramusic.model.StreamingSource
import com.example.auramusic.model.Track
import org.json.JSONObject

/** Full snapshots keep online and local playlist entries available before a library scan. */
object TrackJson {
    fun encode(track: Track): JSONObject = JSONObject().apply {
        put("id", track.id)
        put("resolvedCatalogId", track.resolvedCatalogId)
        put("title", track.title)
        put("artist", track.artist)
        put("album", track.album)
        put("durationMs", track.durationMs)
        put("audioUrl", track.audioUrl)
        put("fallbackLowBitrateUrl", track.fallbackLowBitrateUrl)
        put("coverArtUrl", track.coverArtUrl)
        put("source", track.source.name)
        put("qualityBadge", track.qualityBadge)
        put("isLossless", track.isLossless)
        put("isLocal", track.isLocal)
        put("isCachedOffline", track.isCachedOffline)
        put("localFilePath", track.localFilePath ?: "")
        put("category", track.category)
    }

    fun decode(obj: JSONObject): Track {
        val audioUrl = obj.optString("audioUrl", "")
        val host = runCatching { java.net.URI(audioUrl).host.orEmpty() }.getOrDefault("")
        val isSaavn = host == "saavncdn.com" || host.endsWith(".saavncdn.com")
        return TrackMapping.sanitize(Track(
        id = obj.getString("id"), title = obj.optString("title", "Unknown Track"),
        resolvedCatalogId = obj.optString("resolvedCatalogId", ""),
        artist = obj.optString("artist", "Unknown Artist"), album = obj.optString("album", ""),
        durationMs = obj.optLong("durationMs", 0), audioUrl = audioUrl,
        fallbackLowBitrateUrl = obj.optString("fallbackLowBitrateUrl", ""),
        coverArtUrl = obj.optString("coverArtUrl", ""),
        source = if (isSaavn) StreamingSource.JIOSAAVN else runCatching { StreamingSource.valueOf(obj.optString("source")) }.getOrDefault(StreamingSource.LOCAL_STORAGE),
        qualityBadge = if (isSaavn) "AAC" else obj.optString("qualityBadge", "Audio"),
        isLossless = !isSaavn && obj.optBoolean("isLossless", false),
        isLocal = obj.optBoolean("isLocal"), isCachedOffline = obj.optBoolean("isCachedOffline"),
        localFilePath = obj.optString("localFilePath", "").takeIf { it.isNotBlank() },
        category = obj.optString("category", "")
    ))
    }
}
