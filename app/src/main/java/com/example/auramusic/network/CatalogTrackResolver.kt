package com.example.auramusic.network

import com.example.auramusic.data.TrackMapping
import com.example.auramusic.model.Track
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Resolve title, artist and audio from a single provider result, never from list position. */
class CatalogTrackResolver(
    private val search: suspend (String) -> List<Track> = { OnlineMusicSearchService().searchOnline(it).tracks }
) {
    private val mutex = Mutex()
    private val resolved = mutableMapOf<String, Track>()

    suspend fun resolve(track: Track): Track? {
        if (!TrackMapping.needsResolution(track)) return track
        return mutex.withLock {
            val key = "${track.id}|${track.title}|${track.artist}|${track.album}"
            resolved[key]?.let { return@withLock it }
            val match = TrackMapping.matchingTrack(track, search(track.title))
                ?: TrackMapping.matchingTrack(track, search("${track.title} ${track.artist.substringBefore(',')}"))
            if (match != null) resolved[key] = match
            match
        }
    }
}
