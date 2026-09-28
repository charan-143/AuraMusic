package com.example.auramusic.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auramusic.data.AudioRepository
import com.example.auramusic.model.Track
import com.example.auramusic.player.MusicPlayerManager
import com.example.auramusic.player.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRepository = AudioRepository(application)
    private val playerManager = MusicPlayerManager(application)

    val playerState: StateFlow<PlayerState> = playerManager.playerState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All Tracks")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _allTracks = MutableStateFlow<List<Track>>(emptyList())

    private val _isExpandedPlayer = MutableStateFlow(false)
    val isExpandedPlayer: StateFlow<Boolean> = _isExpandedPlayer.asStateFlow()

    private val _isQueueVisible = MutableStateFlow(false)
    val isQueueVisible: StateFlow<Boolean> = _isQueueVisible.asStateFlow()

    val categories = listOf("All Tracks", "Lofi Chill", "Deep Ambient", "Electronic", "Device Library")

    val filteredTracks: StateFlow<List<Track>> = combine(
        _allTracks,
        _searchQuery,
        _selectedCategory
    ) { tracks, query, category ->
        tracks.filter { track ->
            val matchesCategory = if (category == "All Tracks") {
                true
            } else {
                track.category.equals(category, ignoreCase = true)
            }

            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                track.title.contains(query, ignoreCase = true) ||
                        track.artist.contains(query, ignoreCase = true) ||
                        track.album.contains(query, ignoreCase = true)
            }

            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initialize with curated tracks
        val curated = audioRepository.getCuratedTracks()
        _allTracks.value = curated
        playerManager.setQueue(curated, 0)

        // Try scanning device storage
        loadDeviceTracks()
    }

    fun loadDeviceTracks() {
        viewModelScope.launch {
            val deviceTracks = audioRepository.loadDeviceAudio()
            if (deviceTracks.isNotEmpty()) {
                val combined = audioRepository.getCuratedTracks() + deviceTracks
                _allTracks.value = combined
                playerManager.setQueue(combined, playerState.value.currentIndex)
            }
        }
    }

    fun playTrack(track: Track) {
        val currentFiltered = filteredTracks.value.ifEmpty { _allTracks.value }
        playerManager.playTrack(track, currentFiltered)
    }

    fun togglePlayPause() = playerManager.togglePlayPause()

    fun skipNext() = playerManager.skipNext()

    fun skipPrevious() = playerManager.skipPrevious()

    fun seekToRatio(ratio: Float) = playerManager.seekToRatio(ratio)

    fun toggleShuffle() = playerManager.toggleShuffle()

    fun toggleRepeat() = playerManager.toggleRepeat()

    fun setPlaybackSpeed(speed: Float) = playerManager.setPlaybackSpeed(speed)

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setExpandedPlayer(expanded: Boolean) {
        _isExpandedPlayer.value = expanded
    }

    fun setQueueVisible(visible: Boolean) {
        _isQueueVisible.value = visible
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
