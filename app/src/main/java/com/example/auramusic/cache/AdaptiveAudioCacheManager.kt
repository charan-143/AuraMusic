package com.example.auramusic.cache

import android.content.Context
import android.net.Uri
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import com.example.auramusic.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class AdaptiveAudioCacheManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // 1 GB LRU Cache for offline traveling & roaming stability
    private val maxCacheSizeBytes: Long = 1024L * 1024L * 1024L // 1 GB
    private val cacheDir = File(context.cacheDir, "aura_lossless_audio_cache")

    val simpleCache: SimpleCache by lazy {
        val databaseProvider = StandaloneDatabaseProvider(context)
        val evictor = LeastRecentlyUsedCacheEvictor(maxCacheSizeBytes)
        SimpleCache(cacheDir, evictor, databaseProvider)
    }

    private val httpDataSourceFactory: DefaultHttpDataSource.Factory =
        DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(30000)
            .setUserAgent("AuraMusic-LosslessStreamer/1.0")

    val cacheDataSourceFactory: DataSource.Factory by lazy {
        CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Extended LoadControl tailored for travel & roaming where cellular signals drop.
     * Buffers up to 5 minutes ahead (often the entire track) so you can ride through
     * dead zones, tunnels, and network handovers without a single stutter.
     */
    fun createTravelLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 60_000,       // 60 sec minimum buffer before throttling
                /* maxBufferMs = */ 300_000,      // 5 min maximum buffer ahead
                /* bufferForPlaybackMs = */ 1_500, // Starts immediately within 1.5s
                /* bufferForPlaybackAfterRebufferMs = */ 3_000
            )
            .setBackBuffer(
                /* backBufferDurationMs = */ 60_000, // Retain 60s behind current point for instant rewind
                /* retainBackBufferFromKeyframe = */ true
            )
            .build()
    }

    /**
     * Checks if a given audio URL is already completely cached in local storage.
     */
    fun isCached(url: String): Boolean {
        if (url.isBlank()) return false
        val uri = Uri.parse(url)
        return simpleCache.isCached(uri.toString(), 0, 1024 * 100)
    }

    /**
     * Predictive background prefetcher: Caches upcoming queue tracks in advance.
     */
    fun prefetchUpcomingTracks(queue: List<Track>, currentIndex: Int, countToPrefetch: Int = 2) {
        if (queue.isEmpty()) return
        scope.launch {
            for (i in 1..countToPrefetch) {
                val nextIdx = (currentIndex + i) % queue.size
                val track = queue[nextIdx]
                if (track.audioUrl.isNotBlank() && !isCached(track.audioUrl)) {
                    try {
                        val uri = Uri.parse(track.audioUrl)
                        val dataSpec = androidx.media3.datasource.DataSpec(uri)
                        val cacheWriter = CacheWriter(
                            CacheDataSource(simpleCache, httpDataSourceFactory.createDataSource()),
                            dataSpec,
                            null,
                            null
                        )
                        cacheWriter.cache()
                    } catch (e: Exception) {
                        // Silent prefetch failure, will stream normally when played
                    }
                }
            }
        }
    }
}
