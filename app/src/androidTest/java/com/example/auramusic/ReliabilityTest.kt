package com.example.auramusic

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.auramusic.cache.OfflineDownloadManager
import com.example.auramusic.cache.DownloadState
import com.example.auramusic.data.AudioRepository
import com.example.auramusic.data.PlaylistRepository
import com.example.auramusic.model.StreamingSource
import com.example.auramusic.model.Track
import com.example.auramusic.player.MusicPlayerManager
import com.example.auramusic.network.ArtworkResolver
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReliabilityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = object : ContextWrapper(instrumentation.targetContext) {
        private val prefix = "reliability_${UUID.randomUUID()}"
        private val directory = File(baseContext.cacheDir, prefix).apply { mkdirs() }
        override fun getApplicationContext(): Context = this
        override fun getFilesDir() = directory
        override fun getCacheDir() = File(directory, "cache").apply { mkdirs() }
        override fun getExternalFilesDir(type: String?) = File(directory, "external").apply { mkdirs() }
        override fun getSharedPreferences(name: String, mode: Int) = baseContext.getSharedPreferences("${prefix}_$name", mode)
    }

    @Test fun playlistRestoresOnlineAndLocalMetadataWithoutScanning() {
        val repository = PlaylistRepository(context, AudioRepository(context))
        val playlist = repository.createPlaylist("Test", "", false)
        val online = Track("online", "Online song", "Artist", audioUrl = "https://example.com/song.m4a",
            source = StreamingSource.JIOSAAVN, coverArtUrl = "https://example.com/cover.jpg")
        val local = Track("device_1", "Local song", "Artist", audioUrl = "content://media/external/audio/media/1", isLocal = true)
        repository.addTrackToPlaylist(playlist.id, online)
        repository.addTrackToPlaylist(playlist.id, local)
        val restored = PlaylistRepository(context, AudioRepository(context)).playlists.value.first { it.id == playlist.id }
        assertEquals(listOf(online, local), restored.tracks)
    }

    @Test fun legacyIdsSurviveSavingBeforeScanAndResolveAfterScan() {
        context.getSharedPreferences("aura_playlists", Context.MODE_PRIVATE).edit().putString("saved_playlists",
            """[{"id":"legacy","name":"Old playlist","trackIds":["device_1"]}]""").commit()
        val audio = AudioRepository(context)
        val repository = PlaylistRepository(context, audio)
        assertTrue(repository.playlists.value.first().tracks.isEmpty())
        repository.createPlaylist("Another playlist", "", false)
        val track = Track("device_1", "Recovered", "Artist", audioUrl = "content://media/external/audio/media/1", isLocal = true)
        audio.addTracks(listOf(track))
        repository.refreshTracks()
        assertEquals(listOf(track), repository.playlists.value.first { it.id == "legacy" }.tracks)
    }

    @Test fun downloadPublishesCompleteFileAndRestoresIndex() {
        val source = File(context.filesDir, "source.mp3").apply { writeBytes(ByteArray(40_000) { (it % 127).toByte() }) }
        val manager = OfflineDownloadManager(context)
        val track = Track("download_test", "Song", "Artist", audioUrl = Uri.fromFile(source).toString())
        manager.downloadTrack(track)
        await { manager.downloadStates.value[track.id] is DownloadState.Downloaded }
        assertArrayEquals(source.readBytes(), File(requireNotNull(manager.getLocalPath(track.id))).readBytes())
        val restored = OfflineDownloadManager(context)
        assertTrue(restored.isDownloaded(track.id))
        assertEquals("Song", restored.downloadedTracks.value.single().title)
        manager.deleteDownload(track.id)
        assertFalse(OfflineDownloadManager(context).isDownloaded(track.id))
    }

    @Test fun repeatOneKeepsRealAudioOnSameTrack() {
        val file = writeWave()
        val first = Track("repeat_a", "First", "Unknown Artist", durationMs = 300, audioUrl = Uri.fromFile(file).toString(), isLocal = true)
        val second = first.copy(id = "repeat_b", title = "Second")
        lateinit var manager: MusicPlayerManager
        instrumentation.runOnMainSync {
            manager = MusicPlayerManager(context)
            manager.setCrossfade(false)
            manager.playTrack(first, listOf(first, second))
            manager.toggleRepeat()
        }
        try {
            await { manager.playerState.value.isPlaying }
            Thread.sleep(1400)
            instrumentation.runOnMainSync {
                assertEquals(first.id, manager.playerState.value.currentTrack?.id)
                assertEquals(androidx.media3.common.Player.REPEAT_MODE_ONE, manager.exoPlayer.repeatMode)
                assertNull(manager.playerState.value.playbackError)
                assertTrue(manager.exoPlayer.isPlaying)
                manager.skipNext()
                assertEquals(second.id, manager.playerState.value.currentTrack?.id)
            }
        } finally { instrumentation.runOnMainSync { manager.release() } }
    }

    @Test fun cancelledDownloadDoesNotBlockRetryOrReappear() {
        val source = File(context.filesDir, "cancel.mp3").apply { writeBytes(ByteArray(64_000) { 7 }) }
        val track = Track("cancel_test", "Song", "Artist", audioUrl = Uri.fromFile(source).toString())
        val manager = OfflineDownloadManager(context)
        manager.downloadTrack(track)
        manager.deleteDownload(track.id)
        Thread.sleep(300)
        assertFalse(manager.isDownloaded(track.id))
        await {
            manager.downloadTrack(track)
            manager.isDownloaded(track.id)
        }
        assertArrayEquals(source.readBytes(), File(requireNotNull(manager.getLocalPath(track.id))).readBytes())
    }

    @Test fun interruptedHttpDownloadResumesFromConfirmedPartialBytes() {
        val requests = mutableListOf<String>()
        val server = ServerSocket(0)
        val finished = CountDownLatch(1)
        val worker = Thread {
            try {
                repeat(2) { index ->
                    server.accept().use { socket ->
                        val reader = socket.getInputStream().bufferedReader()
                        val lines = mutableListOf<String>()
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                            lines.add(line)
                        }
                        synchronized(requests) { requests.add(lines.joinToString("\n")) }
                        val response = if (index == 0)
                            "HTTP/1.1 200 OK\r\nContent-Length: 8\r\nETag: \"stable\"\r\nConnection: close\r\n\r\n1234"
                        else "HTTP/1.1 206 Partial Content\r\nContent-Length: 4\r\nContent-Range: bytes 4-7/8\r\nETag: \"stable\"\r\nConnection: close\r\n\r\n5678"
                        socket.getOutputStream().write(response.toByteArray())
                        socket.getOutputStream().flush()
                    }
                }
            } finally { finished.countDown() }
        }.apply { isDaemon = true; start() }
        val manager = OfflineDownloadManager(context)
        val track = Track("resume_test", "Song", "Artist", audioUrl = "http://127.0.0.1:${server.localPort}/song.mp3")
        try {
            manager.downloadTrack(track)
            await { manager.downloadStates.value[track.id] is DownloadState.Failed }
            Thread.sleep(100)
            manager.downloadTrack(track)
            await { manager.isDownloaded(track.id) }
            assertEquals("12345678", File(requireNotNull(manager.getLocalPath(track.id))).readText())
            assertTrue(finished.await(5, TimeUnit.SECONDS))
            assertTrue(requests[1].contains("Range: bytes=4-", ignoreCase = true))
            assertTrue(requests[1].contains("If-Range: \"stable\"", ignoreCase = true))
        } finally { server.close(); worker.join(1000) }
    }

    @Test fun missingAudioShowsErrorAndNeverSimulatesProgress() {
        lateinit var manager: MusicPlayerManager
        instrumentation.runOnMainSync {
            manager = MusicPlayerManager(context)
            manager.playTrack(Track("missing", "Missing", "Artist"))
        }
        try {
            Thread.sleep(700)
            assertNotNull(manager.playerState.value.playbackError)
            assertFalse(manager.playerState.value.isPlaying)
            assertEquals(0L, manager.playbackProgress.value.currentPositionMs)
        } finally { instrumentation.runOnMainSync { manager.release() } }
    }

    @Test fun artworkLookupCanRecoverAfterTemporaryFailure() = runBlocking {
        val image = File(context.filesDir, "cover.png")
        val uri = Uri.fromFile(image).toString()
        assertNull(ArtworkResolver.resolve(context, "Song", "Unknown Artist", uri))
        val bitmap = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888)
        image.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        // Expire the retry cooldown without a one-minute sleep.
        val prefs = context.getSharedPreferences("resolved_artwork_v2", Context.MODE_PRIVATE)
        prefs.edit().apply { prefs.all.keys.filter { it.startsWith("failed_") }.forEach { putLong(it, 0) } }.commit()
        assertEquals(uri, ArtworkResolver.resolve(context, "Song", "Unknown Artist", uri))
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(50)
        assertTrue("Condition did not complete", condition())
    }

    private fun writeWave(): File {
        val samples = 8000 * 3 / 10
        val buffer = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(samples * 2)
        return File(context.filesDir, "repeat.wav").apply { writeBytes(buffer.array()) }
    }
}
