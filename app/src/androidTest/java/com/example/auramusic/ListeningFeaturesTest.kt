package com.example.auramusic

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.auramusic.cache.OfflineDownloadManager
import com.example.auramusic.cache.DownloadState
import com.example.auramusic.data.*
import com.example.auramusic.model.Track
import com.example.auramusic.player.MusicPlayerManager
import java.io.File
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListeningFeaturesTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun isolated(): Context = object : ContextWrapper(instrumentation.targetContext) {
        private val prefix = "features_${UUID.randomUUID()}"
        private val directory = File(baseContext.cacheDir, prefix).apply { mkdirs() }
        override fun getApplicationContext(): Context = this
        override fun getFilesDir() = directory
        override fun getCacheDir() = File(directory, "cache").apply { mkdirs() }
        override fun getExternalFilesDir(type: String?) = File(directory, "external").apply { mkdirs() }
        override fun getSharedPreferences(name: String, mode: Int) = baseContext.getSharedPreferences("${prefix}_$name", mode)
    }

    @Test fun sessionRestoresPausedAndResumesAtSavedPosition() {
        val context = isolated()
        val file = wave(context)
        val first = Track("session_a", "First", "Unknown Artist", durationMs = 5000, audioUrl = Uri.fromFile(file).toString(), isLocal = true)
        val second = first.copy(id = "session_b", title = "Second")
        lateinit var manager: MusicPlayerManager
        instrumentation.runOnMainSync {
            manager = MusicPlayerManager(context)
            manager.setCrossfade(false)
            manager.playTrack(first, listOf(first, second))
        }
        try {
            await { manager.playerState.value.isPlaying }
            instrumentation.runOnMainSync {
                manager.pause(); manager.seekTo(1234); manager.setPlaybackSpeed(1.5f)
                manager.saveListeningSession(); manager.release()
                manager = MusicPlayerManager(context)
                assertFalse(manager.playerState.value.isPlaying)
                assertEquals(listOf(first.id, second.id), manager.playerState.value.queue.map { it.id })
                assertEquals(1234L, manager.playbackProgress.value.currentPositionMs)
                assertEquals(1.5f, manager.playerState.value.playbackSpeed)
                manager.resume()
            }
            Thread.sleep(400)
            instrumentation.runOnMainSync {
                assertNull(manager.playerState.value.playbackError)
                assertTrue(manager.exoPlayer.currentPosition >= 1234)
            }
        } finally { instrumentation.runOnMainSync { manager.release() } }
    }

    @Test fun queueEditsPreserveCurrentSongAndSupportUndo() {
        val context = isolated()
        lateinit var manager: MusicPlayerManager
        instrumentation.runOnMainSync {
            manager = MusicPlayerManager(context)
            try {
                val tracks = (1..4).map { Track("$it", "Song $it", "Artist") }
                manager.setQueue(tracks)
                manager.moveQueueItem(0, 2)
                assertEquals("1", manager.playerState.value.currentTrack?.id)
                assertEquals(2, manager.playerState.value.currentIndex)
                manager.playNext(tracks[1])
                assertEquals(listOf("3", "1", "2", "4"), manager.playerState.value.queue.map { it.id })
                manager.removeFromQueue("4")
                assertTrue(manager.playerState.value.canUndoQueueRemoval)
                manager.undoQueueRemoval()
                assertEquals(listOf("3", "1", "2", "4"), manager.playerState.value.queue.map { it.id })
                manager.toggleShuffle()
                manager.playNext(tracks[3])
                manager.skipNext()
                assertEquals("4", manager.playerState.value.currentTrack?.id)
            } finally { manager.release() }
        }
    }

    @Test fun metadataEditsSurviveRescanAndPlaylistReload() {
        val context = isolated()
        val original = Track("edited", "Old title", "Old artist", album = "Old album")
        val audio = AudioRepository(context).apply { addTracks(listOf(original)) }
        val playlists = PlaylistRepository(context, audio)
        val list = playlists.createPlaylist("Edited playlist", "", false)
        playlists.addTrackToPlaylist(list.id, original)
        val edited = original.copy(title = "New title", artist = "New artist", album = "New album", coverArtUrl = "https://example.com/cover.jpg")
        TrackEdits(context).save(edited)
        assertEquals(edited, audio.getMultiSourceTracks().single())
        val reloaded = PlaylistRepository(context, AudioRepository(context))
        assertEquals(edited, reloaded.playlists.value.first { it.id == list.id }.tracks.single())
    }

    @Test fun backupTransfersMetadataAndCustomCoverToAnotherInstall() {
        val oldContext = isolated()
        val source = PlaylistRepository(oldContext, AudioRepository(oldContext))
        val file = File(oldContext.filesDir, "custom_artwork/cover.jpg").apply { parentFile?.mkdirs() }
        val bitmap = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        val playlist = source.createPlaylist("Portable", "", false)
        source.addTrackToPlaylist(playlist.id, Track("online", "Edited song", "Artist", album = "Album", audioUrl = "https://example.com/song.mp3", coverArtUrl = Uri.fromFile(file).toString()))
        val destination = isolated()
        val target = PlaylistRepository(destination, AudioRepository(destination))
        target.importBackup(source.exportBackup())
        val restored = target.playlists.value.first { it.name == "Portable" }.tracks.single()
        assertEquals("Edited song", restored.title)
        assertNotEquals(Uri.fromFile(file).toString(), restored.coverArtUrl)
        assertTrue(File(requireNotNull(Uri.parse(restored.coverArtUrl).path)).isFile)
        val before = target.playlists.value
        assertThrows(IllegalArgumentException::class.java) { target.importBackup("{\"format\":\"wrong\",\"version\":1}") }
        assertEquals(before, target.playlists.value)
    }

    @Test fun wifiOnlyWaitsThenStartsWhenPolicyAllows() {
        val context = isolated()
        val server = ServerSocket(0)
        val accepted = CountDownLatch(1)
        val worker = Thread {
            try {
                server.accept().use { socket ->
                    accepted.countDown()
                    val reader = socket.getInputStream().bufferedReader()
                    while (!reader.readLine().isNullOrBlank()) { }
                    socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 8\r\nConnection: close\r\n\r\n12345678".toByteArray())
                }
            } catch (_: Exception) { }
        }.apply { isDaemon = true; start() }
        val manager = OfflineDownloadManager(context) { false }
        val track = Track("wifi", "Wi-Fi track", "Artist", audioUrl = "http://127.0.0.1:${server.localPort}/song.mp3")
        try {
            manager.setWifiOnly(true)
            manager.downloadTrack(track)
            assertEquals(DownloadState.Paused(true), manager.downloadStates.value[track.id])
            assertFalse(accepted.await(200, TimeUnit.MILLISECONDS))
            manager.setWifiOnly(false)
            await { manager.isDownloaded(track.id) }
            assertTrue(manager.storageBytes.value >= 8)
        } finally { server.close(); worker.join(1000) }
    }

    @Test fun pausedRequestsSurviveRestartAndBulkDeletionRemovesFiles() {
        val context = isolated()
        val manager = OfflineDownloadManager(context) { false }
        manager.setWifiOnly(true)
        manager.downloadTrack(Track("pending", "Pending", "Artist", audioUrl = "https://example.com/song.mp3"))
        manager.pauseDownload("pending")
        val reloaded = OfflineDownloadManager(context) { false }
        assertEquals(DownloadState.Paused(), reloaded.downloadStates.value["pending"])
        val file = File(context.filesDir, "local.mp3").apply { writeBytes(ByteArray(1024) { 1 }) }
        val tracks = listOf("one", "two").map { Track(it, it, "Artist", audioUrl = Uri.fromFile(file).toString()) }
        tracks.forEach { manager.downloadTrack(it) }
        await { tracks.all { manager.isDownloaded(it.id) } }
        val downloadedFiles = tracks.map { File(requireNotNull(manager.getLocalPath(it.id))) }
        manager.deleteDownloads(setOf("one", "two", "pending"))
        assertTrue(manager.items.value.isEmpty())
        assertTrue(downloadedFiles.none { it.exists() })
        assertTrue(file.exists())
    }

    @Test fun pausePreservesPartialTransferAndResumeUsesRange() {
        val context = isolated()
        val server = ServerSocket(0)
        val releaseFirst = CountDownLatch(1)
        val requests = mutableListOf<String>()
        val worker = Thread {
            try {
                repeat(2) { index ->
                    server.accept().use { socket ->
                        val reader = socket.getInputStream().bufferedReader()
                        val headers = mutableListOf<String>()
                        while (true) { val line = reader.readLine() ?: break; if (line.isEmpty()) break; headers.add(line) }
                        synchronized(requests) { requests.add(headers.joinToString("\n")) }
                        val response = if (index == 0) "HTTP/1.1 200 OK\r\nContent-Length: 8\r\nETag: \"paused\"\r\nConnection: close\r\n\r\n1234"
                            else "HTTP/1.1 206 Partial Content\r\nContent-Length: 4\r\nContent-Range: bytes 4-7/8\r\nETag: \"paused\"\r\nConnection: close\r\n\r\n5678"
                        socket.getOutputStream().write(response.toByteArray()); socket.getOutputStream().flush()
                        if (index == 0) releaseFirst.await(10, TimeUnit.SECONDS)
                    }
                }
            } catch (_: Exception) { }
        }.apply { isDaemon = true; start() }
        val manager = OfflineDownloadManager(context) { true }
        val track = Track("pause_http", "Pause", "Artist", audioUrl = "http://127.0.0.1:${server.localPort}/song.mp3")
        try {
            manager.downloadTrack(track)
            await { (manager.downloadStates.value[track.id] as? DownloadState.Downloading)?.progress == 0.5f }
            manager.pauseDownload(track.id)
            assertEquals(DownloadState.Paused(), manager.downloadStates.value[track.id])
            releaseFirst.countDown()
            manager.resumeDownload(track.id)
            await { manager.isDownloaded(track.id) }
            assertEquals("12345678", File(requireNotNull(manager.getLocalPath(track.id))).readText())
            assertTrue(requests[1].contains("Range: bytes=4-", ignoreCase = true))
        } finally { releaseFirst.countDown(); server.close(); worker.join(1000) }
    }

    @Test fun disablingNormalizationPreservesPcmSamplesExactly() {
        val processor = com.example.auramusic.audio.NormalizationAudioProcessor()
        processor.enabled = false
        processor.configure(androidx.media3.common.audio.AudioProcessor.AudioFormat(44100, 2, androidx.media3.common.C.ENCODING_PCM_16BIT))
        processor.flush()
        val bytes = byteArrayOf(0, 0, 127, 126, -128, -127, 16, 32)
        val input = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).apply { flip() }
        processor.queueInput(input)
        val actual = ByteArray(bytes.size)
        processor.output.get(actual)
        assertArrayEquals(bytes, actual)
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(50)
        assertTrue("Condition did not complete", condition())
    }

    private fun wave(context: Context): File {
        val samples = 8000 * 5
        val buffer = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16).put("data".toByteArray()).putInt(samples * 2)
        return File(context.filesDir, "session.wav").apply { writeBytes(buffer.array()) }
    }
}
