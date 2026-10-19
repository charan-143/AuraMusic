package com.example.auramusic.network

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/** Bounded concurrent lookups shared by thumbnails, the player, and media notifications. */
object ArtworkResolver {
    private val permits = Semaphore(3)
    private val locks = ConcurrentHashMap<String, Mutex>()
    private const val RETRY_DELAY_MS = 60_000L

    suspend fun resolve(context: Context, title: String, artist: String,
                        originalUrl: String? = null, audioUri: String? = null): String? = withContext(Dispatchers.IO) {
        val key = MessageDigest.getInstance("SHA-256")
            .digest("$title|$artist|$audioUri|$originalUrl".toByteArray())
            .joinToString("") { "%02x".format(it) }
        locks.getOrPut(key) { Mutex() }.withLock {
            permits.withPermit {
                if (com.example.auramusic.data.TrackEdits(context).isSelectedCover(originalUrl) && isImage(context, originalUrl!!)) return@withPermit originalUrl
                val cache = context.applicationContext.getSharedPreferences("resolved_artwork_v2", Context.MODE_PRIVATE)
                val saved = cache.getString(key, null)
                if (!saved.isNullOrBlank() && isImage(context, saved)) return@withPermit saved
                if (System.currentTimeMillis() - cache.getLong("failed_$key", 0) < RETRY_DELAY_MS) return@withPermit null
                val embedded = embeddedArtwork(context, audioUri, key)
                val original = if (embedded == null) originalUrl?.takeIf { it.isNotBlank() && isImage(context, it) } else null
                val result = embedded ?: original ?: OnlineMusicSearchService().findArtwork(title, artist)
                    ?.takeIf { isImage(context, it) }
                if (result != null) cache.edit().putString(key, result).remove("failed_$key").apply()
                else cache.edit().remove(key).putLong("failed_$key", System.currentTimeMillis()).apply()
                result
            }
        }
    }

    private suspend fun isImage(context: Context, url: String): Boolean = try {
        context.imageLoader.execute(ImageRequest.Builder(context).data(url).size(512).build()) is SuccessResult
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) { false }

    private fun embeddedArtwork(context: Context, audioUri: String?, key: String): String? {
        val uri = audioUri?.let { Uri.parse(it) } ?: return null
        if (uri.scheme !in listOf("content", "file")) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return null
            val directory = File(context.filesDir, "artwork").apply { mkdirs() }
            val image = File(directory, "$key.jpg")
            val atomic = android.util.AtomicFile(image)
            val output = atomic.startWrite()
            try { output.write(bytes); atomic.finishWrite(output) }
            catch (error: Exception) { atomic.failWrite(output); throw error }
            Uri.fromFile(image).toString()
        } catch (_: Exception) { null }
        finally { runCatching { retriever.release() } }
    }
}
