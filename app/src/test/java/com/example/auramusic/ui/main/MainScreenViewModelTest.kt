package com.example.auramusic.ui.main

import com.example.auramusic.model.Track
import com.example.auramusic.player.PlayerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreenViewModelTest {

    @Test
    fun track_formattedDuration_calculatesCorrectly() {
        val track = Track(
            id = "t1",
            title = "Midnight Monochrome",
            artist = "Aura Sound Lab",
            durationMs = 214000L // 3 min 34 sec
        )
        assertEquals("3:34", track.formattedDuration)
    }

    @Test
    fun playerState_progress_calculatesAccurately() {
        val state = PlayerState(
            currentPositionMs = 60000L,
            durationMs = 180000L
        )
        assertEquals(0.333f, state.progress, 0.01f)
    }

    @Test
    fun playerState_initialValues_areSensible() {
        val state = PlayerState()
        assertFalse(state.isPlaying)
        assertFalse(state.isShuffle)
        assertEquals(0f, state.progress, 0.001f)
        assertEquals("0:00", state.formattedCurrentPosition)
    }
}
