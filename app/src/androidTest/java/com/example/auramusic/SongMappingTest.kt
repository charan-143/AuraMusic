package com.example.auramusic

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.auramusic.cache.*
import com.example.auramusic.data.*
import com.example.auramusic.model.*
import com.example.auramusic.network.CatalogTrackResolver
import com.example.auramusic.player.MusicPlayerManager
import com.example.auramusic.recommendation.RecommendationEngine
import kotlinx.coroutines.*
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SongMappingTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private fun isolated(): Context = object: ContextWrapper(instrumentation.targetContext) {
        private val prefix="mapping_${UUID.randomUUID()}"
        private val directory=File(baseContext.cacheDir,prefix).apply {mkdirs()}
        override fun getApplicationContext(): Context=this
        override fun getFilesDir()=directory
        override fun getCacheDir()=File(directory,"cache").apply {mkdirs()}
        override fun getExternalFilesDir(type:String?)=File(directory,"downloads").apply {mkdirs()}
        override fun getSharedPreferences(name:String,mode:Int)=baseContext.getSharedPreferences("${prefix}_$name",mode)
    }
    private fun old()=Track("curated_yeshanagula","Yeshanagula","Anirudh Ravichander",album="The Paradise",
        audioUrl="file:///missing/wrong-song.wav",durationMs=5000,source=StreamingSource.JIOSAAVN)
    private fun correct(context:Context)=Track("saavn_song_fixture","Yeshanagula","Anirudh Ravichander",album="The Paradise",
        audioUrl=Uri.fromFile(wave(context)).toString(),durationMs=5000,source=StreamingSource.JIOSAAVN)

    @Test fun snapshotInvalidatesOldPointersButKeepsResolvedMapping() {
        val legacy=old().copy(localFilePath="/wrong.mp4",isCachedOffline=true)
        val decoded=TrackJson.decode(TrackJson.encode(legacy))
        assertEquals("",decoded.audioUrl);assertNull(decoded.localFilePath);assertFalse(decoded.isCachedOffline)
        val resolved=legacy.copy(audioUrl="https://correct.example/song.mp4",resolvedCatalogId="saavn_song_real",localFilePath=null,isCachedOffline=false)
        assertEquals(resolved,TrackJson.decode(TrackJson.encode(resolved)))
    }
    @Test fun recommendationEngineDoesNotInventPlayableSongsWhenCatalogIsEmpty() {
        val context=isolated()
        val engine=RecommendationEngine(AudioRepository(context),context)
        assertTrue(engine.getOnlineRecommendationSections(null).flatMap {it.tracks}.isEmpty())
        assertTrue(engine.getTracksForMoodChip("All",emptyList()).isEmpty())
    }
    @Test fun legacyPlaybackUsesMatchedRecordingAndCancelledLookupCannotReplaceNewSelection() = runBlocking {
        val context=isolated();val actual=correct(context)
        val gate=CompletableDeferred<Unit>()
        val resolver=CatalogTrackResolver { query -> if(query=="Delayed") gate.await();listOf(actual) }
        lateinit var manager:MusicPlayerManager
        instrumentation.runOnMainSync {manager=MusicPlayerManager(context,resolver);manager.setCrossfade(false);manager.playTrack(old(),listOf(old()))}
        try {
            await {manager.playerState.value.currentTrack?.resolvedCatalogId==actual.id && manager.playerState.value.isPlaying}
            assertEquals(actual.audioUrl,manager.playerState.value.currentTrack?.audioUrl)
            assertEquals(old().id,manager.playerState.value.currentTrack?.id)
            instrumentation.runOnMainSync {
                manager.playTrack(old().copy(id="curated_delayed",title="Delayed"))
                manager.playTrack(actual.copy(id="normal_selection"))
            }
            gate.complete(Unit)
            delay(300)
            assertEquals("normal_selection",manager.playerState.value.currentTrack?.id)
            val next=actual.copy(id="queued_next")
            instrumentation.runOnMainSync {
                manager.setCrossfade(true)
                manager.playTrack(actual,listOf(actual,next))
            }
            await {manager.playerState.value.isPlaying}
            instrumentation.runOnMainSync {
                manager.skipNext()
                manager.playTrack(actual.copy(id="latest_card"))
            }
            delay(350)
            assertEquals("latest_card",manager.playerState.value.currentTrack?.id)
        } finally {instrumentation.runOnMainSync {manager.release()}}
    }
    @Test fun oldDownloadIsQuarantinedThenReplacedWithMatchedBytes() = runBlocking {
        val context=isolated();val actual=correct(context)
        val stale=File(context.getExternalFilesDir(null),"offline_old.mp4").apply {writeText("wrong recording")}
        val legacy=old().copy(localFilePath=stale.path,isCachedOffline=true)
        File(context.filesDir,"downloaded_tracks_index.json").writeText(JSONArray().put(TrackJson.encode(legacy)).toString())
        val manager=OfflineDownloadManager(context, catalogResolver=CatalogTrackResolver {listOf(actual)}, wifiAvailable={true})
        assertFalse(manager.isDownloaded(legacy.id))
        assertTrue(manager.downloadStates.value[legacy.id] is DownloadState.Failed)
        manager.resumeDownload(legacy.id)
        await {manager.isDownloaded(legacy.id)}
        val downloaded=manager.downloadedTracks.value.single()
        assertEquals(actual.id,downloaded.resolvedCatalogId)
        assertArrayEquals(File(Uri.parse(actual.audioUrl).path!!).readBytes(),File(manager.getLocalPath(legacy.id)!!).readBytes())
        assertFalse(stale.exists())
        val restored=OfflineDownloadManager(context, catalogResolver=CatalogTrackResolver {error("Verified offline download must not search")}, wifiAvailable={true})
        assertTrue(restored.isDownloaded(legacy.id))
        restored.deleteDownload(legacy.id)
    }
    private suspend fun await(condition:()->Boolean) {
        withTimeout(10000) {while(!condition()) delay(50)}
    }
    private fun wave(context:Context):File {
        val samples=8000*5
        val bytes=ByteBuffer.allocate(44+samples*2).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36+samples*2).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16).put("data".toByteArray()).putInt(samples*2)
        return File(context.filesDir,"matched.wav").apply {writeBytes(bytes.array())}
    }
}
