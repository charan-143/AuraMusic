package com.example.auramusic

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.example.auramusic.audio.EqualizerState
import com.example.auramusic.model.*
import com.example.auramusic.player.PlayerState
import com.example.auramusic.theme.AuraMusicTheme
import com.example.auramusic.ui.components.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PlayerDesignTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val directory = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "design-preview").apply { mkdirs() }
        java.io.File(directory, "$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun speedPresetsAndTransitionsKeepTheirCallbacks() {
        var chosen = 1f
        var crossfade = false
        compose.setContent {
            var speed by remember { mutableFloatStateOf(1f) }
            var enabled by remember { mutableStateOf(false) }
            AuraMusicTheme { PixelPlaybackSpeedSheet(speed, enabled, 3, { chosen = it; speed = it }, { crossfade = it; enabled = it }, {}, {}) }
        }
        capture("speed")
        compose.onNodeWithText("1.5×").performClick()
        compose.runOnIdle { assertEquals(1.5f, chosen) }
        compose.onNodeWithText("1.0×").performClick()
        compose.runOnIdle { assertEquals(1f, chosen) }
        compose.onNodeWithText("Track transitions").performClick()
        compose.onNodeWithContentDescription("Crossfade").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(crossfade) }
        compose.onNodeWithContentDescription("Crossfade duration").performScrollTo().assertIsDisplayed()
    }

    @Test fun manualEqualizerRevealsBandsAndReset() {
        var reset = false
        compose.setContent {
            var state by remember { mutableStateOf(EqualizerState()) }
            AuraMusicTheme(false) { PixelEqualizerSheet(true, state, 0, { state = state.copy(isEnabled = it) }, { state = state.copy(isAiMode = it) }, {}, { _, _ -> }, {}, {}, {}, { reset = true }, {}) }
        }
        compose.onNodeWithText("Manual").performClick()
        compose.onNodeWithContentDescription("60Hz").assertIsDisplayed()
        capture("equalizer-light")
        compose.onNodeWithText("Reset to flat").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(reset) }
        compose.onNodeWithContentDescription("Sound effects").performScrollTo().performClick()
        compose.onNodeWithText("Manual").assertIsNotEnabled()
    }

    @Test fun lyricsSeekAndManualFollowAreExplicit() {
        var sought = -1L
        val lyrics = TrackLyrics("test", "Long song title", "Artist", lines = (0..30).map { LyricLine(it * 1000L, "Lyric line $it") })
        compose.setContent { AuraMusicTheme { Box(Modifier.height(400.dp)) { PixelSyncedLyricsView(lyrics, 0, { sought = it }, {}) } } }
        capture("lyrics")
        compose.onNodeWithText("Lyric line 1").performClick()
        compose.runOnIdle { assertEquals(1000L, sought) }
        compose.onNodeWithText("Following").performClick()
        compose.onNodeWithText("Follow song").assertIsDisplayed().performClick()
        compose.onNodeWithText("Following").assertIsDisplayed()
    }

    @Test fun unavailableLyricsOfferReturnToArtwork() {
        var back = false
        compose.setContent { AuraMusicTheme { PixelSyncedLyricsView(TrackLyrics("test", "Song", "Artist"), 0, {}, { back = true }) } }
        compose.onNodeWithText("Back to artwork").performClick()
        compose.runOnIdle { assertTrue(back) }
    }

    @Test fun compactPlayerKeepsToolsAndOutputAccessible() {
        var output = false
        val track = Track("test", "Yeshanagula (From The Paradise) (Telugu)", "Anirudh Ravichander, Singer Prabha")
        compose.setContent { AuraMusicTheme { Box(Modifier.width(320.dp).height(560.dp)) { PixelExpandedPlayer(PlayerState(currentTrack = track), onPlayPauseClick = {}, onSkipNextClick = {}, onSkipPreviousClick = {}, onSeek = {}, onToggleShuffle = {}, onToggleRepeat = {}, onSpeedChange = {}, onCollapseClick = {}, onQueueClick = {}, onAudioOutputClick = { output = true }) } } }
        compose.onNodeWithText("Speed").performScrollTo().assertIsDisplayed()
        capture("player-compact")
        compose.onNodeWithContentDescription("Song options").performClick()
        compose.onNodeWithText("Audio output").performClick()
        compose.runOnIdle { assertTrue(output) }
    }

    @Test fun soundAdjustsDuringDragAndHeaderStaysAlignedAcrossModes() {
        var changed = 0
        compose.setContent {
            var state by remember { mutableStateOf(EqualizerState()) }
            AuraMusicTheme {
                PixelEqualizerSheet(true, state, 0,
                    { state = state.copy(isEnabled = it) }, { state = state.copy(isAiMode = it) }, {},
                    { index, level -> changed = level; state = state.copy(bands = state.bands.map { if (it.index == index) it.copy(levelMilliBels = level) else it }) },
                    {}, {}, { state = state.copy(currentPreset = it) }, {}, {})
            }
        }
        val headerTop = compose.onNodeWithContentDescription("Close Equalizer").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithText("Manual").performClick()
        compose.runOnIdle { }
        assertEquals(headerTop, compose.onNodeWithContentDescription("Close Equalizer").fetchSemanticsNode().boundsInRoot.top, 1f)
        val slider = compose.onNodeWithContentDescription("60Hz")
        slider.performTouchInput { down(center); moveTo(androidx.compose.ui.geometry.Offset(width * 0.8f, centerY), 100) }
        compose.runOnIdle { assertTrue("Sound must update before the finger is lifted", changed > 0) }
        slider.performTouchInput { up() }
        compose.onNodeWithText("Flat").performClick()
        compose.onNodeWithText("Rock").performClick()
        compose.onNodeWithText("Rock").assertIsDisplayed()
        compose.onNodeWithText("More sound controls").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Stereo width").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Close Equalizer").assertIsDisplayed()
        capture("sound-fixed")
    }

    @Test fun adaptiveExtrasExplainWhyManualControlsAreDisabled() {
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 1.5f)) {
                AuraMusicTheme {
                    PixelEqualizerSheet(true, EqualizerState(), 0, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {})
                }
            }
        }
        compose.onNodeWithText("More sound controls").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Bass boost").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Stereo width").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Close Equalizer").assertIsDisplayed()
        capture("sound-large-font")
    }

    @Test fun portraitPlayerPinsToolsAndCentersTransport() {
        compose.setContent {
            AuraMusicTheme {
                Box(Modifier.fillMaxSize()) {
                    PixelExpandedPlayer(PlayerState(currentTrack = Track("alignment", "A long song title that uses two lines", "Artist")),
                        onPlayPauseClick = {}, onSkipNextClick = {}, onSkipPreviousClick = {}, onSeek = {},
                        onToggleShuffle = {}, onToggleRepeat = {}, onSpeedChange = {}, onCollapseClick = {}, onQueueClick = {})
                }
            }
        }
        compose.onNodeWithText("Speed").assertIsDisplayed()
        val tools = compose.onNodeWithTag("player-tools").fetchSemanticsNode().boundsInRoot
        val transport = compose.onNodeWithTag("player-transport").fetchSemanticsNode().boundsInRoot
        val play = compose.onNodeWithContentDescription("Play").fetchSemanticsNode().boundsInRoot
        val previous = compose.onNodeWithContentDescription("Previous song").fetchSemanticsNode().boundsInRoot
        val next = compose.onNodeWithContentDescription("Next song").fetchSemanticsNode().boundsInRoot
        assertEquals(tools.center.x, play.center.x, 2f)
        assertEquals(play.center.x - previous.center.x, next.center.x - play.center.x, 2f)
        assertTrue(transport.bottom <= tools.top)
        assertEquals(tools.left, transport.left, 1f)
        capture("player-aligned-portrait")
    }
}

