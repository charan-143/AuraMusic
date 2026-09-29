package com.example.auramusic.cache

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.auramusic.model.StreamingSource
import com.example.auramusic.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

sealed class DownloadState {
    object NotDownloaded : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data class Downloaded(val localPath: String) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

class OfflineDownloadManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "OfflineDownloadManager"
        private const val INDEX_FILE_NAME = "downloaded_tracks_index.json"

        @Volatile
        private var instance: OfflineDownloadManager? = null

        fun getInstance(context: Context): OfflineDownloadManager {
            return instance ?: synchronized(this) {
                instance ?: OfflineDownloadManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val activeDownloadJobs = ConcurrentHashMap<String, Job>()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val _downloadedTracks = MutableStateFlow<List<Track>>(emptyList())
    val downloadedTracks: StateFlow<List<Track>> = _downloadedTracks.asStateFlow()

    private val downloadDir: File by lazy {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File(context.filesDir, "downloads")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    init {
        loadPersistedDownloads()
    }

    fun isDownloaded(trackId: String): Boolean {
        return _downloadStates.value[trackId] is DownloadState.Downloaded
    }

    fun getLocalPath(trackId: String): String? {
        val state = _downloadStates.value[trackId]
        return if (state is DownloadState.Downloaded) state.localPath else null
    }

    fun downloadTrack(track: Track) {
        if (track.audioUrl.isBlank()) return
        if (isDownloaded(track.id)) return
        if (activeDownloadJobs.containsKey(track.id)) return

        val job = scope.launch {
            _downloadStates.update { it + (track.id to DownloadState.Downloading(0f)) }

            val sanitizedId = track.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")

            // 1. If content:// URI (from device storage)
            if (track.audioUrl.startsWith("content://") || track.audioUrl.startsWith("file://")) {
                try {
                    val targetFile = File(downloadDir, "offline_${sanitizedId}.mp3")
                    val uri = android.net.Uri.parse(track.audioUrl)
                    val input = if (track.audioUrl.startsWith("content://")) {
                        context.contentResolver.openInputStream(uri)
                    } else {
                        java.io.FileInputStream(File(uri.path ?: track.audioUrl))
                    }

                    input?.use { inStream ->
                        FileOutputStream(targetFile).use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }

                    val completedTrack = track.copy(
                        isCachedOffline = true,
                        localFilePath = targetFile.absolutePath,
                        qualityBadge = "OFFLINE"
                    )

                    _downloadStates.update { it + (track.id to DownloadState.Downloaded(targetFile.absolutePath)) }
                    _downloadedTracks.update { current ->
                        val existing = current.filterNot { it.id == track.id }
                        existing + completedTrack
                    }

                    persistDownloads()
                    Log.i(TAG, "Successfully cached local track: ${track.title} to ${targetFile.absolutePath}")
                } catch (e: Exception) {
                    Log.e(TAG, "Local caching failed for ${track.title}", e)
                    _downloadStates.update { it + (track.id to DownloadState.Failed(e.message ?: "Caching failed")) }
                } finally {
                    activeDownloadJobs.remove(track.id)
                }
                return@launch
            }

            // 2. Online HTTP/HTTPS stream download with redirect follow
            var connection: HttpURLConnection? = null
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                val targetFile = File(downloadDir, "offline_${sanitizedId}.m4a")

                var currentUrl = track.audioUrl
                var redirects = 0
                var conn: HttpURLConnection? = null

                while (redirects < 5) {
                    val url = URL(currentUrl)
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 30000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "AuraMusic/2.4.0 (Android; Material 3 Expressive)")
                    }

                    val code = conn.responseCode
                    if (code in 301..308) {
                        val location = conn.getHeaderField("Location")
                        conn.disconnect()
                        if (!location.isNullOrBlank()) {
                            currentUrl = if (location.startsWith("http")) location else URL(url, location).toString()
                            redirects++
                            continue
                        }
                    }
                    break
                }

                connection = conn ?: throw Exception("Failed to open connection")
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP ${connection.responseCode} while downloading track")
                }

                val totalBytes = connection.contentLength.toLong()
                inputStream = connection.inputStream
                outputStream = FileOutputStream(targetFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloadedBytes = 0L
                var lastReportedProgress = 0f

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    if (totalBytes > 0) {
                        val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        if (progress - lastReportedProgress >= 0.05f || progress >= 1f) {
                            lastReportedProgress = progress
                            _downloadStates.update { it + (track.id to DownloadState.Downloading(progress)) }
                        }
                    }
                }

                outputStream.flush()

                val completedTrack = track.copy(
                    isCachedOffline = true,
                    localFilePath = targetFile.absolutePath,
                    qualityBadge = "320K OFFLINE"
                )

                _downloadStates.update { it + (track.id to DownloadState.Downloaded(targetFile.absolutePath)) }
                _downloadedTracks.update { current ->
                    val existing = current.filterNot { it.id == track.id }
                    existing + completedTrack
                }

                persistDownloads()
                Log.i(TAG, "Successfully downloaded: ${track.title} to ${targetFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for ${track.title}", e)
                _downloadStates.update { it + (track.id to DownloadState.Failed(e.message ?: "Download failed")) }
            } finally {
                try { inputStream?.close() } catch (ignored: Exception) {}
                try { outputStream?.close() } catch (ignored: Exception) {}
                connection?.disconnect()
                activeDownloadJobs.remove(track.id)
            }
        }

        activeDownloadJobs[track.id] = job
    }

    fun deleteDownload(trackId: String) {
        activeDownloadJobs[trackId]?.cancel()
        activeDownloadJobs.remove(trackId)

        val track = _downloadedTracks.value.firstOrNull { it.id == trackId }
        val filePath = track?.localFilePath ?: getLocalPath(trackId)

        if (filePath != null) {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        }

        _downloadStates.update { it - trackId }
        _downloadedTracks.update { current -> current.filterNot { it.id == trackId } }
        scope.launch { persistDownloads() }
    }

    private fun loadPersistedDownloads() {
        val indexFile = File(context.filesDir, INDEX_FILE_NAME)
        if (!indexFile.exists()) return

        try {
            val content = indexFile.readText()
            val jsonArray = JSONArray(content)
            val list = mutableListOf<Track>()
            val stateMap = mutableMapOf<String, DownloadState>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id")
                val localPath = obj.optString("localPath")

                if (id.isNotBlank() && localPath.isNotBlank() && File(localPath).exists()) {
                    val track = Track(
                        id = id,
                        title = obj.optString("title", "Unknown"),
                        artist = obj.optString("artist", "Unknown"),
                        album = obj.optString("album", "Downloaded Album"),
                        durationMs = obj.optLong("durationMs", 180000L),
                        audioUrl = obj.optString("audioUrl", ""),
                        coverArtUrl = obj.optString("coverArtUrl", ""),
                        source = StreamingSource.LOCAL_STORAGE,
                        qualityBadge = "320K OFFLINE",
                        isLossless = true,
                        isCachedOffline = true,
                        isLocal = true,
                        localFilePath = localPath,
                        category = "Downloaded"
                    )
                    list.add(track)
                    stateMap[id] = DownloadState.Downloaded(localPath)
                }
            }

            _downloadedTracks.value = list
            _downloadStates.value = stateMap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load offline index", e)
        }
    }

    private suspend fun persistDownloads() = withContext(Dispatchers.IO) {
        try {
            val indexFile = File(context.filesDir, INDEX_FILE_NAME)
            val jsonArray = JSONArray()

            for (track in _downloadedTracks.value) {
                val obj = JSONObject().apply {
                    put("id", track.id)
                    put("title", track.title)
                    put("artist", track.artist)
                    put("album", track.album)
                    put("durationMs", track.durationMs)
                    put("audioUrl", track.audioUrl)
                    put("coverArtUrl", track.coverArtUrl)
                    put("localPath", track.localFilePath ?: "")
                }
                jsonArray.put(obj)
            }

            indexFile.writeText(jsonArray.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist offline index", e)
        }
    }
}
