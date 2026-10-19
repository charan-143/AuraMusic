package com.example.auramusic
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import com.example.auramusic.ui.main.*
import com.example.auramusic.ui.components.*
import com.example.auramusic.theme.AuraMusicTheme
import com.example.auramusic.model.*
import com.example.auramusic.network.OnlineSearchResult
import org.junit.*
import java.io.File
import java.util.UUID
class CalmDesignTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val store = ViewModelStore()
    @After fun cleanup() { compose.runOnIdle { store.clear() } }
    private fun model(): MainScreenViewModel {
        val base = compose.activity.applicationContext
        val prefix = "design_${UUID.randomUUID()}"
        val context = object: ContextWrapper(base) {
            val directory = File(base.cacheDir,prefix).apply {mkdirs()}
            override fun getApplicationContext(): Context = this
            override fun getFilesDir() = directory
            override fun getCacheDir() = File(directory,"cache").apply {mkdirs()}
            override fun getExternalFilesDir(type: String?) = File(directory,"downloads").apply {mkdirs()}
            override fun getSharedPreferences(name:String,mode:Int) = base.getSharedPreferences("${prefix}_$name",mode)
        }
        val app = object: Application() {init {attachBaseContext(context)}}
        lateinit var vm: MainScreenViewModel
        compose.runOnUiThread {vm=MainScreenViewModel(app,false);store.put("design",vm)}
        return vm
    }
    @Test fun navigationSearchLibraryAndThemeRemainUsable() {
        val vm=model()
        compose.setContent {
            val dark by vm.isDarkMode.collectAsState()
            val tab by vm.currentTab.collectAsState()
            AuraMusicTheme(dark) {Column {CalmMainContent(vm,Modifier.weight(1f));PixelBottomNavBar(tab,vm::setNavTab)}}
        }
        compose.onNodeWithText("Recommended songs").assertIsDisplayed()
        compose.onNodeWithText("Search",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Browse moods").assertIsDisplayed()
        compose.onNodeWithText("Library",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Your library").assertIsDisplayed()
        compose.onNodeWithText("Playlists").performClick()
        compose.onNodeWithText("New").assertIsDisplayed()
        compose.onNodeWithText("Settings",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Appearance").assertIsDisplayed()
        compose.onAllNodes(isToggleable())[0].performClick()
        compose.runOnIdle {Assert.assertFalse(vm.isDarkMode.value)}
        compose.onNodeWithText("Home",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Recommended songs").assertIsDisplayed()
    }
    @Test fun homeHasOneSongSectionAndMenuOpensCorrectSong() {
        val vm=model()
        val songs=(1..8).map {Track("design_$it","Design song $it","Unknown Artist")}
        compose.runOnIdle {vm.applyDiscoveryResults(OnlineSearchResult(songs,emptyList()))}
        compose.setContent {AuraMusicTheme {Column {CalmMainContent(vm,Modifier.weight(1f));TrackActions(vm)}}}
        compose.onNodeWithText("Recommended songs").assertIsDisplayed()
        compose.onAllNodes(hasContentDescription("More options for", substring=true))[0].performClick()
        var selectedId = ""
        compose.runOnIdle { selectedId = requireNotNull(vm.actionTrack.value).id }
        compose.onNodeWithText("Edit song and artwork").assertIsDisplayed()
        compose.onNodeWithText("Add to favorites").performClick()
        compose.runOnIdle {Assert.assertTrue(vm.isFavorite(selectedId))}
    }
}
