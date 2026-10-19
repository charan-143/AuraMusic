package com.example.auramusic.player

import com.example.auramusic.model.Track

object QueueEditor {
    fun move(queue: List<Track>, from: Int, to: Int): List<Track> {
        if (from !in queue.indices || to !in queue.indices || from == to) return queue
        return queue.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun playNext(queue: List<Track>, currentId: String?, track: Track): List<Track> {
        if (track.id == currentId) return queue
        val remaining = queue.filterNot { it.id == track.id }.toMutableList()
        val current = remaining.indexOfFirst { it.id == currentId }
        remaining.add((current + 1).coerceIn(0, remaining.size), track)
        return remaining
    }
}
