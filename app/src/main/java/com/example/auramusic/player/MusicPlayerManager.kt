package com.example.auramusic.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.auramusic.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var positionTickerJob: Job? = null

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playerState.update { it.copy(isPlaying = isPlaying) }
                    if (isPlaying) {
                        startTicker()
                    } else {
                        stopTicker()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        val duration = exoPlayer.duration.coerceAtLeast(0L)
                        _playerState.update {
                            it.copy(
                                durationMs = if (duration > 0) duration else (it.currentTrack?.durationMs ?: 180000L)
                            )
                        }
                    } else if (playbackState == Player.STATE_ENDED) {
                        skipNext()
                    }
                }
            })
        }
    }

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun setQueue(tracks: List<Track>, initialIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val clampedIndex = initialIndex.coerceIn(0, tracks.lastIndex)
        _playerState.update {
            it.copy(
                queue = tracks,
                currentIndex = clampedIndex,
                currentTrack = tracks[clampedIndex],
                durationMs = tracks[clampedIndex].durationMs,
                currentPositionMs = 0L
            )
        }
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        val queue = newQueue ?: _playerState.value.queue.ifEmpty { listOf(track) }
        val index = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        _playerState.update {
            it.copy(
                currentTrack = track,
                queue = queue,
                currentIndex = index,
                currentPositionMs = 0L,
                durationMs = track.durationMs,
                isPlaying = true
            )
        }

        if (track.audioUrl.isNotBlank()) {
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                val mediaItem = MediaItem.fromUri(Uri.parse(track.audioUrl))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: Exception) {
                // Fallback to simulated playback ticker if stream error occurs
                startTicker()
            }
        } else {
            // Simulated local playback progression with exact audio timings
            startTicker()
        }
    }

    fun togglePlayPause() {
        val currentState = _playerState.value
        if (currentState.currentTrack == null && currentState.queue.isNotEmpty()) {
            playTrack(currentState.queue[0])
            return
        }

        if (currentState.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
        _playerState.update { it.copy(isPlaying = false) }
        stopTicker()
    }

    fun resume() {
        val track = _playerState.value.currentTrack
        if (track != null && track.audioUrl.isNotBlank() && exoPlayer.playbackState != Player.STATE_IDLE) {
            exoPlayer.play()
        } else {
            _playerState.update { it.copy(isPlaying = true) }
            startTicker()
        }
    }

    fun seekToRatio(ratio: Float) {
        val currentDuration = _playerState.value.durationMs.coerceAtLeast(1000L)
        val targetMs = (ratio * currentDuration).toLong().coerceIn(0L, currentDuration)
        seekTo(targetMs)
    }

    fun seekTo(positionMs: Long) {
        if (exoPlayer.playbackState != Player.STATE_IDLE && _playerState.value.currentTrack?.audioUrl?.isNotBlank() == true) {
            exoPlayer.seekTo(positionMs)
        }
        _playerState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun skipNext() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return

        val nextIndex = if (state.isShuffle) {
            state.queue.indices.random()
        } else {
            (state.currentIndex + 1) % state.queue.size
        }
        playTrack(state.queue[nextIndex])
    }

    fun skipPrevious() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return

        // If played more than 3 seconds, restart current track
        if (state.currentPositionMs > 3000L) {
            seekTo(0L)
            return
        }

        val prevIndex = if (state.currentIndex > 0) state.currentIndex - 1 else state.queue.lastIndex
        playTrack(state.queue[prevIndex])
    }

    fun toggleShuffle() {
        _playerState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun toggleRepeat() {
        _playerState.update { it.copy(isRepeatOne = !it.isRepeatOne) }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        _playerState.update { it.copy(playbackSpeed = speed) }
    }

    private fun startTicker() {
        stopTicker()
        positionTickerJob = scope.launch {
            while (isActive) {
                delay(250)
                if (_playerState.value.isPlaying) {
                    if (exoPlayer.isPlaying) {
                        val current = exoPlayer.currentPosition
                        val duration = exoPlayer.duration.coerceAtLeast(_playerState.value.durationMs)
                        _playerState.update {
                            it.copy(currentPositionMs = current, durationMs = duration)
                        }
                    } else {
                        // Simulated progression
                        _playerState.update { state ->
                            val speedFactor = state.playbackSpeed
                            val newPos = (state.currentPositionMs + (250 * speedFactor).toLong())
                            if (newPos >= state.durationMs && state.durationMs > 0) {
                                if (state.isRepeatOne) {
                                    state.copy(currentPositionMs = 0L)
                                } else {
                                    // Trigger skip to next
                                    skipNext()
                                    state
                                }
                            } else {
                                state.copy(currentPositionMs = newPos)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun stopTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    fun release() {
        stopTicker()
        exoPlayer.release()
    }
}
