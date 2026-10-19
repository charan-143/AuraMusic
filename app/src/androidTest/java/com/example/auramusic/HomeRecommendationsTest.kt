package com.example.auramusic

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.auramusic.model.Album
import com.example.auramusic.model.StreamingSource
import com.example.auramusic.model.Track
import com.example.auramusic.network.OnlineSearchResult
import com.example.auramusic.ui.main.MainScreenViewModel
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HomeRecommendationsTest {
    @Test fun playingAndUpdatingASongKeepsTheDiscoveryCatalogAndHomeLists() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = object : ContextWrapper(instrumentation.targetContext) {
            private val prefix = "home_${UUID.randomUUID()}"
            private val directory = File(baseContext.cacheDir, prefix).apply { mkdirs() }
            override fun getApplicationContext(): Context = this
            override fun getFilesDir() = directory
            override fun getCacheDir() = File(directory, "cache").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?) = File(directory, "downloads").apply { mkdirs() }
            override fun getSharedPreferences(name: String, mode: Int) = baseContext.getSharedPreferences("${prefix}_$name", mode)
        }
        val application = object : Application() { init { attachBaseContext(context) } }
        val tracks = (1..4).map { index -> Track("home_$index", "Home song $index", "Unknown Artist", source = StreamingSource.JIOSAAVN) }
        val album = Album("home_album", "Discovery album", "Unknown Artist", "", source = StreamingSource.JIOSAAVN, tracks = tracks)
        val store = ViewModelStore()
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        lateinit var viewModel: MainScreenViewModel
        instrumentation.runOnMainSync {
            viewModel = MainScreenViewModel(application, loadCatalogOnStart = false)
            store.put("home", viewModel)
            scope.launch { viewModel.filteredTracks.collect { } }
            viewModel.applyDiscoveryResults(OnlineSearchResult(tracks, listOf(album)))
        }
        fun hasCatalog() = viewModel.filteredTracks.value.map { it.id }.containsAll(tracks.map { it.id }) &&
            viewModel.allAlbums.value.any { it.id == album.id }
        fun hasRecommendations() = viewModel.recommendations.value.flatMap { it.tracks }.map { it.id }.distinct().size > 1
        try {
            await { hasCatalog() && hasRecommendations() }
            instrumentation.runOnMainSync { viewModel.playTrack(tracks.first(), tracks) }
            await { viewModel.recommendations.value.any { it.id == "rec_sonic_neighbors" } }
            assertTrue("Starting playback must retain the full discovery catalog", hasCatalog())
            assertTrue("Home must still recommend songs other than the selected song", hasRecommendations())
            assertTrue(viewModel.recommendations.value.flatMap { it.tracks }.any { it.id != tracks.first().id })
            instrumentation.runOnMainSync {
                viewModel.toggleFavorite(tracks.first())
                viewModel.saveTrackEdits(tracks.first().copy(title = "Edited home song"))
            }
            await { hasCatalog() }
            lateinit var scan: Job
            instrumentation.runOnMainSync { scan = viewModel.loadDeviceTracks() }
            runBlocking { withTimeout(15000) { scan.join() } }
            assertTrue("A device scan must not replace the online discovery catalog", hasCatalog())
            assertTrue(hasRecommendations())
            instrumentation.runOnMainSync {
                viewModel.playTrack(tracks[1], tracks)
                viewModel.playTrack(tracks[2], tracks)
            }
            await { viewModel.recommendations.value.any { it.title == "More Like ${tracks[2].title}" } }
            assertTrue("Rapid track changes must not publish an older, smaller catalog", hasCatalog())
            assertTrue(hasRecommendations())
        } finally {
            scope.cancel()
            instrumentation.runOnMainSync { store.clear() }
        }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(50)
        assertTrue("Expected recommendations did not arrive", condition())
    }
}
