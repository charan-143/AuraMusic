package com.example.auramusic.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.auramusic.theme.AuraMusicTheme
import com.example.auramusic.ui.components.PixelAtAGlanceHeader
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun atAGlanceHeader_rendersSuccessfully() {
        composeTestRule.setContent {
            AuraMusicTheme {
                PixelAtAGlanceHeader(
                    isPlaying = false,
                    activeTrackTitle = "Midnight Monochrome"
                )
            }
        }
        composeTestRule.onNodeWithText("Pixel Buds Pro").assertExists()
    }
}
