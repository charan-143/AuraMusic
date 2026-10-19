package com.example.auramusic

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.auramusic.model.Track
import com.example.auramusic.player.QueueEditor
import com.example.auramusic.theme.AuraMusicTheme
import com.example.auramusic.ui.components.PixelQueueSheet
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QueueInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun handleDragReordersQueueAndRemovalCanBeUndone() {
        val original = (1..3).map { Track("$it", "Song $it", "Artist") }
        val queue = mutableStateOf(original)
        val canUndo = mutableStateOf(false)
        compose.setContent {
            AuraMusicTheme {
                PixelQueueSheet(queue.value, "1", false, {}, {}, {},
                    onMove = { from, to -> queue.value = QueueEditor.move(queue.value, from, to) },
                    onRemoveTrack = { track -> queue.value = queue.value.filterNot { it.id == track.id }; canUndo.value = true },
                    canUndo = canUndo.value, onUndo = { queue.value = original; canUndo.value = false })
            }
        }
        compose.onNodeWithContentDescription("Reorder Song 1", useUnmergedTree = true).performTouchInput {
            down(center)
            advanceEventTime(700)
            repeat(12) { moveBy(Offset(0f, 20f)); advanceEventTime(16) }
            up()
        }
        compose.runOnIdle { assertEquals(listOf("2", "1", "3"), queue.value.map { it.id }) }
        compose.onAllNodesWithContentDescription("Remove from Queue")[0].performClick()
        compose.onNodeWithText("Undo removal").performClick()
        compose.runOnIdle { assertEquals(original, queue.value) }
    }
}
