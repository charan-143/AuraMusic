package com.example.auramusic.cache

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.AtomicFile
import android.util.Log
import com.example.auramusic.data.TrackJson
import com.example.auramusic.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Network
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed class DownloadState {
    object NotDownloaded : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data class Downloaded(val localPath: String) : DownloadState()
    data class Failed(val error: String) : DownloadState()
    data class Paused(val waitingForWifi: Boolean = false) : DownloadState()
}

class OfflineDownloadManager internal constructor(
    private val context: Context,
    private val catalogResolver: com.example.auramusic.network.CatalogTrackResolver = com.example.auramusic.network.CatalogTrackResolver(),
    private val wifiAvailable: (() -> Boolean)? = null
) {
    companion object {
        @Volatile private var instance: OfflineDownloadManager? = null
        fun getInstance(context: Context): OfflineDownloadManager = instance ?: synchronized(this) {
            instance ?: OfflineDownloadManager(context.applicationContext).also { instance = it }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val jobs = mutableMapOf<String, Job>()
    private val requests = mutableMapOf<String, Track>()
    private val invalidLegacyFiles = mutableMapOf<String, String>()
    private val preferences = context.getSharedPreferences("download_preferences", Context.MODE_PRIVATE)
    private val _wifiOnly = MutableStateFlow(preferences.getBoolean("wifi_only", false))
    val wifiOnly = _wifiOnly.asStateFlow()
    private val _items = MutableStateFlow<List<Track>>(emptyList())
    val items = _items.asStateFlow()
    private val _storageBytes = MutableStateFlow(0L)
    val storageBytes = _storageBytes.asStateFlow()
    private val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { enforceWifiPolicy() }
        override fun onLost(network: Network) { enforceWifiPolicy() }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { enforceWifiPolicy() }
    }
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()
    private val _downloadedTracks = MutableStateFlow<List<Track>>(emptyList())
    val downloadedTracks: StateFlow<List<Track>> = _downloadedTracks.asStateFlow()
    private val downloadDir: File by lazy {
        (context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File(context.filesDir, "downloads"))
            .also { it.mkdirs() }
    }
    private val index by lazy { AtomicFile(File(context.filesDir, "downloaded_tracks_index.json")) }

    init {
        loadPersistedDownloads()
        runCatching {
            val saved = JSONArray(preferences.getString("requests", "[]"))
            for (i in 0 until saved.length()) {
                val track = TrackJson.decode(saved.getJSONObject(i))
                if (!isDownloaded(track.id)) {
                    requests[track.id] = track
                    _downloadStates.update { it + (track.id to DownloadState.Paused(saved.getJSONObject(i).optBoolean("waitingForWifi"))) }
                }
            }
        }
        _downloadedTracks.value.forEach { requests[it.id] = it }
        refreshItems()
        if (wifiAvailable == null) runCatching { connectivity.registerDefaultNetworkCallback(networkCallback) }
        enforceWifiPolicy()
    }

    private fun hasWifi() = wifiAvailable?.invoke() ?: (connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true)

    @Synchronized
    private fun checkWifiPolicy(track: Track) {
        if (_wifiOnly.value && (track.audioUrl.startsWith("http") || com.example.auramusic.data.TrackMapping.needsResolution(track)) && !hasWifi()) {
            _downloadStates.update { it + (track.id to DownloadState.Paused(true)) }
            throw CancellationException("Waiting for Wi-Fi")
        }
    }

    fun setWifiOnly(enabled: Boolean) {
        _wifiOnly.value = enabled
        preferences.edit().putBoolean("wifi_only", enabled).apply()
        enforceWifiPolicy()
    }

    @Synchronized
    private fun enforceWifiPolicy() {
        requests.values.toList().forEach { track ->
            val state = _downloadStates.value[track.id]
            val remote = (track.audioUrl.startsWith("http") || com.example.auramusic.data.TrackMapping.needsResolution(track))
            if (remote && _wifiOnly.value && !hasWifi() && state is DownloadState.Downloading) {
                _downloadStates.update { it + (track.id to DownloadState.Paused(true)) }
                jobs[track.id]?.cancel()
            } else if (state is DownloadState.Paused && state.waitingForWifi && (!_wifiOnly.value || hasWifi())) {
                resumeDownload(track.id)
            }
        }
    }

    @Synchronized
    fun pauseDownload(trackId: String) {
        if (isDownloaded(trackId)) return
        _downloadStates.update { it + (trackId to DownloadState.Paused()) }
        jobs[trackId]?.cancel()
        refreshItems()
    }

    fun resumeDownload(trackId: String) {
        scope.launch {
            val previous = synchronized(this@OfflineDownloadManager) { jobs[trackId] }
            previous?.join()
            val track = synchronized(this@OfflineDownloadManager) { requests[trackId] } ?: return@launch
            downloadTrack(track)
        }
    }

    @Synchronized
    private fun refreshItems() {
        _items.value = requests.values.toList()
        _storageBytes.value = downloadDir.listFiles()?.filter { it.isFile && (it.name.endsWith(".part") || it.name.startsWith("offline_")) }?.sumOf { it.length() } ?: 0L
        preferences.edit().putString("requests", JSONArray().apply { requests.values.filterNot { isDownloaded(it.id) }.forEach { track ->
            put(TrackJson.encode(track).put("waitingForWifi", (_downloadStates.value[track.id] as? DownloadState.Paused)?.waitingForWifi == true))
        } }.toString()).apply()
    }

    @Synchronized
    fun updateTrackMetadata(track: Track) {
        if (requests.containsKey(track.id)) requests[track.id] = track
        _downloadedTracks.update { list -> list.map { if (it.id == track.id) it.copy(title = track.title, artist = track.artist, album = track.album, coverArtUrl = track.coverArtUrl) else it } }
        persistDownloads(); refreshItems()
    }

    @Synchronized
    fun deleteDownloads(ids: Set<String>) { ids.forEach { deleteDownload(it) } }

    fun isDownloaded(trackId: String) = _downloadStates.value[trackId] is DownloadState.Downloaded
    fun getLocalPath(trackId: String) = (_downloadStates.value[trackId] as? DownloadState.Downloaded)?.localPath

    private fun target(track: Track): File {
        val hash = MessageDigest.getInstance("SHA-256").digest(track.id.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val path = track.localFilePath ?: Uri.parse(track.audioUrl).path.orEmpty()
        val ext = if (com.example.auramusic.data.TrackMapping.isLegacyId(track.id)) "m4a" else path.substringAfterLast('.', "mp3").lowercase()
            .takeIf { it in setOf("flac", "wav", "mp3", "m4a", "mp4", "ogg", "opus", "aac") } ?: "mp3"
        return File(downloadDir, "offline_$hash.$ext")
    }

    @Synchronized
    fun downloadTrack(track: Track) {
        if ((track.audioUrl.isBlank() && !com.example.auramusic.data.TrackMapping.needsResolution(track)) || isDownloaded(track.id) || jobs.containsKey(track.id)) return
        requests[track.id] = track
        if (_wifiOnly.value && (track.audioUrl.startsWith("http") || com.example.auramusic.data.TrackMapping.needsResolution(track)) && !hasWifi()) {
            _downloadStates.update { it + (track.id to DownloadState.Paused(true)) }
            refreshItems()
            return
        }
        val job = scope.launch(start = CoroutineStart.LAZY) {
            var track = track
            val needsRepair = com.example.auramusic.data.TrackMapping.needsResolution(track)
            val finalFile = target(track)
            val partial = File(finalFile.path + ".part")
            val validator = File(finalFile.path + ".etag")
            var connection: HttpURLConnection? = null
            try {
                ensureActive()
                _downloadStates.update { it + (track.id to DownloadState.Downloading(0f)) }
                if (needsRepair) {
                    checkWifiPolicy(track)
                    track = catalogResolver.resolve(track) ?: error("No matching recording found. Search for the song and artist.")
                    ensureActive()
                    synchronized(this@OfflineDownloadManager) { ensureActive(); requests[track.id] = track }
                    partial.delete()
                    validator.delete()
                }
                checkWifiPolicy(track)
                val uri = Uri.parse(track.audioUrl)
                if (uri.scheme in listOf("content", "file")) {
                    val input = if (uri.scheme == "content") context.contentResolver.openInputStream(uri)
                        else FileInputStream(File(requireNotNull(uri.path)))
                    requireNotNull(input) { "Cannot read this audio file" }.use { source ->
                        FileOutputStream(partial).use { output ->
                            DownloadTransfer.copy(source, output, -1, { ensureActive() })
                        }
                    }
                } else {
                    require(uri.scheme in listOf("http", "https")) { "Unsupported download address" }
                    // Resume only when the server supplied a stable ETag. Otherwise restart safely.
                    val etag = validator.takeIf { it.exists() }?.readText().orEmpty()
                    val offset = if (etag.isNotBlank() && !etag.startsWith("W/")) partial.length() else 0L
                    connection = (URL(track.audioUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 15000
                        instanceFollowRedirects = true
                        setRequestProperty("Accept-Encoding", "identity")
                        setRequestProperty("User-Agent", "AuraMusic/1.0")
                        if (offset > 0) {
                            setRequestProperty("Range", "bytes=$offset-")
                            setRequestProperty("If-Range", etag)
                        }
                    }
                    val conn = requireNotNull(connection)
                    val code = conn.responseCode
                    val resume = code == HttpURLConnection.HTTP_PARTIAL && offset > 0 &&
                        conn.getHeaderField("Content-Range").orEmpty().startsWith("bytes $offset-")
                    require(code == HttpURLConnection.HTTP_OK || resume) { "HTTP $code; tap to retry" }
                    val newTag = conn.getHeaderField("ETag").orEmpty()
                    if (newTag.isNotBlank()) validator.writeText(newTag) else validator.delete()
                    val base = if (resume) offset else 0L
                    val expected = conn.contentLengthLong
                    val total = if (expected >= 0) base + expected else -1L
                    var lastProgress = -1f
                    conn.inputStream.use { source ->
                        FileOutputStream(partial, resume).use { output ->
                            DownloadTransfer.copy(source, output, expected, { ensureActive(); checkWifiPolicy(track) }) { bytes ->
                                val progress = if (total > 0) ((base + bytes).toFloat() / total).coerceIn(0f, 1f) else 0f
                                if (progress - lastProgress >= 0.02f) {
                                    lastProgress = progress
                                    synchronized(this@OfflineDownloadManager) {
                                        ensureActive()
                                        if (_downloadStates.value[track.id] !is DownloadState.Paused) _downloadStates.update { it + (track.id to DownloadState.Downloading(progress)) }
                                        refreshItems()
                                    }
                                }
                            }
                            output.fd.sync()
                        }
                    }
                }
                ensureActive()
                require(partial.length() > 0) { "Downloaded file is empty" }
                // Serialize publication with deletion: a cancelled job cannot resurrect a removed song.
                synchronized(this@OfflineDownloadManager) {
                    ensureActive()
                    check(partial.renameTo(finalFile)) { "Cannot finish download" }
                    invalidLegacyFiles.remove(track.id)?.takeIf { it != finalFile.path }?.let { File(it).delete() }
                    validator.delete()
                    val completed = track.copy(isCachedOffline = true, localFilePath = finalFile.path,
                        qualityBadge = "OFFLINE")
                    _downloadedTracks.update { tracks -> tracks.filterNot { it.id == track.id } + completed }
                    persistDownloads()
                    _downloadStates.update { it + (track.id to DownloadState.Downloaded(finalFile.path)) }
                    requests[track.id] = completed
                    refreshItems()
                }
            } catch (cancelled: CancellationException) {
                val keepPartial = synchronized(this@OfflineDownloadManager) { _downloadStates.value[track.id] is DownloadState.Paused }
                if (!keepPartial) { partial.delete(); validator.delete() }
                throw cancelled
            } catch (error: Exception) {
                // Keep resumable network data, but discard incomplete local copies.
                if (track.audioUrl.startsWith("content:") || track.audioUrl.startsWith("file:")) partial.delete()
                synchronized(this@OfflineDownloadManager) {
                    if (isActive) _downloadStates.update { it + (track.id to DownloadState.Failed(error.message ?: "Download failed")) }
                }
            } finally {
                connection?.disconnect()
                synchronized(this@OfflineDownloadManager) {
                    jobs.remove(track.id)
                    if (!requests.containsKey(track.id)) { partial.delete(); validator.delete() }
                    refreshItems()
                }
            }
        }
        jobs[track.id] = job
        job.invokeOnCompletion {
            synchronized(this) { if (jobs[track.id] === job) jobs.remove(track.id) }
        }
        job.start()
        refreshItems()
    }

    @Synchronized
    fun deleteDownload(trackId: String) {
        // Keep the cancelled job registered until its streams close, preventing overlapping retries.
        jobs[trackId]?.cancel()
        val request = requests.remove(trackId)
        val track = _downloadedTracks.value.firstOrNull { it.id == trackId }
        val path = track?.localFilePath ?: getLocalPath(trackId)
        if (path != null) File(path).delete()
        invalidLegacyFiles.remove(trackId)?.let { File(it).delete() }
        if (request != null && jobs[trackId] == null) {
            File(target(request).path + ".part").delete()
            File(target(request).path + ".etag").delete()
        }
        _downloadStates.update { it - trackId }
        _downloadedTracks.update { tracks -> tracks.filterNot { it.id == trackId } }
        persistDownloads()
        refreshItems()
    }

    @Synchronized
    private fun loadPersistedDownloads() {
        try {
            val text = index.openRead().bufferedReader().use { it.readText() }
            val array = JSONArray(text)
            val invalid = mutableMapOf<String, DownloadState>()
            val tracks = (0 until array.length()).mapNotNull { i ->
                val obj = array.getJSONObject(i)
                val path = obj.optString("localFilePath", "").ifBlank { obj.optString("localPath", "") }
                val decoded = TrackJson.decode(obj)
                val file = File(path)
                if (com.example.auramusic.data.TrackMapping.needsResolution(decoded)) {
                    requests[decoded.id] = decoded
                    if (file.isFile) invalidLegacyFiles[decoded.id] = path
                    invalid[decoded.id] = DownloadState.Failed("Old song mapping was incorrect. Download again to get the matching recording.")
                    null
                } else if (path.isBlank() || !file.isFile || file.length() == 0L) null else {
                    decoded.copy(localFilePath = path, isCachedOffline = true, qualityBadge = "OFFLINE")
                }
            }
            _downloadedTracks.value = tracks
            _downloadStates.value = tracks.associate { it.id to DownloadState.Downloaded(requireNotNull(it.localFilePath)) } + invalid
        } catch (_: java.io.FileNotFoundException) {
            // First run.
        } catch (error: Exception) {
            Log.e("OfflineDownloadManager", "Cannot read download index", error)
        }
    }

    @Synchronized
    private fun persistDownloads() {
        var stream: FileOutputStream? = null
        try {
            val array = JSONArray().apply { _downloadedTracks.value.forEach { put(TrackJson.encode(it)) } }
            stream = index.startWrite()
            stream.write(array.toString().toByteArray(Charsets.UTF_8))
            index.finishWrite(stream)
        } catch (error: Exception) {
            index.failWrite(stream)
            Log.e("OfflineDownloadManager", "Cannot save download index", error)
        }
    }
}
