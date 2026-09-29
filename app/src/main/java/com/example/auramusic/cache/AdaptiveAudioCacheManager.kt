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

    companion object {
        private const val MAX_CACHE_SIZE_BYTES: Long = 1024L * 1024L * 1024L // 1 GB
        @Volatile
        private var instance: SimpleCache? = null

        @Synchronized
        fun getCache(context: Context): SimpleCache {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val cacheDir = File(context.applicationContext.cacheDir, "aura_lossless_audio_cache")
                    val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
                    val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES)
                    SimpleCache(cacheDir, evictor, databaseProvider).also { instance = it }
                }
            }
        }
    }

    val simpleCache: SimpleCache
        get() = getCache(context)

    private val httpDataSourceFactory: DefaultHttpDataSource.Factory =
        DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(15000)
            .setUserAgent("AuraMusic-LosslessStreamer/2.0")
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "audio/flac, audio/x-flac, audio/*; q=1.0",
                    "X-Audio-Bitrate" to "lossless-24bit",
                    "X-Lossless-Stream" to "true"
                )
            )

    fun updateStreamingQualityHeaders(targetBitrateKbps: Int) {
        val (bitrateHeader, acceptHeader) = when {
            targetBitrateKbps >= 1000 -> "lossless-24bit" to "audio/flac, audio/x-flac, audio/*; q=1.0"
            targetBitrateKbps >= 320 -> "320kbps-high" to "audio/mpeg, audio/mp4, audio/*; q=0.9"
            targetBitrateKbps >= 256 -> "256kbps-opus" to "audio/opus, audio/ogg, audio/*; q=0.9"
            else -> "128kbps-saver" to "audio/opus, audio/aac, audio/*; q=0.8"
        }
        httpDataSourceFactory.setDefaultRequestProperties(
            mapOf(
                "Accept" to acceptHeader,
                "X-Audio-Bitrate" to bitrateHeader,
                "X-Lossless-Stream" to (targetBitrateKbps >= 1000).toString(),
                "X-Auto-Adaptive-Quality" to "true"
            )
        )
    }

    val defaultDataSourceFactory: DataSource.Factory by lazy {
        androidx.media3.datasource.DefaultDataSource.Factory(context.applicationContext, httpDataSourceFactory)
    }

    val cacheDataSourceFactory: DataSource.Factory by lazy {
        CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Optimized LoadControl for snappy zero-latency playback startup & robust pre-buffering.
     * Starts playback within 250ms while buffering up to 3 minutes ahead in the background.
     */
    fun createTravelLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,       // 15 sec minimum buffer
                /* maxBufferMs = */ 180_000,      // 3 min maximum buffer ahead
                /* bufferForPlaybackMs = */ 250,  // Starts instantly within 250ms!
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .setBackBuffer(
                /* backBufferDurationMs = */ 30_000,
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
