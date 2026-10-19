package com.example.auramusic.player

import com.example.auramusic.model.Track
import org.junit.Assert.*
import org.junit.Test

class QueueEditorTest {
    private val tracks = (1..4).map { Track("$it", "Song $it", "Artist") }
    @Test fun movePreservesEveryTrackAndOrder() {
        assertEquals(listOf("1", "3", "4", "2"), QueueEditor.move(tracks, 1, 3).map { it.id })
    }
    @Test fun playNextMovesExistingTrackWithoutDuplication() {
        assertEquals(listOf("1", "4", "2", "3"), QueueEditor.playNext(tracks, "1", tracks[3]).map { it.id })
    }
    @Test fun currentSongCannotBeInsertedTwice() { assertEquals(tracks, QueueEditor.playNext(tracks, "1", tracks[0])) }
    @Test fun invalidDragLeavesQueueIntact() { assertEquals(tracks, QueueEditor.move(tracks, -1, 3)) }
}
