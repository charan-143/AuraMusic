package com.example.auramusic.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auramusic.data.AudioRepository
import com.example.auramusic.data.PlaylistRepository
import com.example.auramusic.model.Album
import com.example.auramusic.model.AudioBitrateMode
import com.example.auramusic.model.Playlist
import com.example.auramusic.model.RecommendationSection
import com.example.auramusic.model.Track
import com.example.auramusic.network.NetworkQualityObserver
import com.example.auramusic.player.MusicPlayerManager
import com.example.auramusic.player.PlayerState
import com.example.auramusic.recommendation.RecommendationEngine
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
    private val networkObserver = NetworkQualityObserver(application)
    private val playlistRepository = PlaylistRepository(application, audioRepository)
    private val recommendationEngine = RecommendationEngine(audioRepository)

    val playerState: StateFlow<PlayerState> = playerManager.playerState
    val networkStatus = networkObserver.networkStatus
    val playlists: StateFlow<List<Playlist>> = playlistRepository.playlists

    private val _currentTab = MutableStateFlow(com.example.auramusic.ui.components.PixelNavTab.HOME)
    val currentTab: StateFlow<com.example.auramusic.ui.components.PixelNavTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All Tracks")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _allTracks = MutableStateFlow<List<Track>>(emptyList())
    private val _allAlbums = MutableStateFlow<List<Album>>(emptyList())
    val allAlbums: StateFlow<List<Album>> = _allAlbums.asStateFlow()

    private val _recommendations = MutableStateFlow<List<RecommendationSection>>(emptyList())
    val recommendations: StateFlow<List<RecommendationSection>> = _recommendations.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<Album?>(null)
    val selectedAlbum: StateFlow<Album?> = _selectedAlbum.asStateFlow()

    private val _isAlbumSheetVisible = MutableStateFlow(false)
    val isAlbumSheetVisible: StateFlow<Boolean> = _isAlbumSheetVisible.asStateFlow()

    private val _trackForPlaylist = MutableStateFlow<Track?>(null)
    val trackForPlaylist: StateFlow<Track?> = _trackForPlaylist.asStateFlow()

    private val _isAddToPlaylistSheetVisible = MutableStateFlow(false)
    val isAddToPlaylistSheetVisible: StateFlow<Boolean> = _isAddToPlaylistSheetVisible.asStateFlow()

    private val _isCreatePlaylistDialogVisible = MutableStateFlow(false)
    val isCreatePlaylistDialogVisible: StateFlow<Boolean> = _isCreatePlaylistDialogVisible.asStateFlow()

    private val _isExpandedPlayer = MutableStateFlow(false)
    val isExpandedPlayer: StateFlow<Boolean> = _isExpandedPlayer.asStateFlow()

    private val _isQueueVisible = MutableStateFlow(false)
    val isQueueVisible: StateFlow<Boolean> = _isQueueVisible.asStateFlow()

    val categories = listOf(
        "All Tracks",
        "Recommendations",
        "Albums",
        "My Playlists",
        "Lossless FLAC",
        "Spotify",
        "YouTube Music",
        "Device Library"
    )

    // Filtered Tracks based on category, search query, or playlist
    val filteredTracks: StateFlow<List<Track>> = combine(
        _allTracks,
        _searchQuery,
        _selectedCategory
    ) { tracks, query, category ->
        tracks.filter { track ->
            val matchesCategory = when (category) {
                "All Tracks", "Recommendations", "Albums", "My Playlists" -> true
                else -> track.category.equals(category, ignoreCase = true) ||
                        track.source.displayName.equals(category, ignoreCase = true)
            }

            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                track.title.contains(query, ignoreCase = true) ||
                        track.artist.contains(query, ignoreCase = true) ||
                        track.album.contains(query, ignoreCase = true) ||
                        track.qualityBadge.contains(query, ignoreCase = true)
            }

            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Albums matching search or category
    val filteredAlbums: StateFlow<List<Album>> = combine(
        _allAlbums,
        _searchQuery,
        _selectedCategory
    ) { albums, query, category ->
        albums.filter { album ->
            val matchesCategory = when (category) {
                "All Tracks", "Recommendations", "Albums" -> true
                "Spotify" -> album.source.displayName.contains("Spotify", ignoreCase = true)
                "YouTube Music" -> album.source.displayName.contains("YouTube", ignoreCase = true)
                "Lossless FLAC" -> album.isLossless
                else -> true
            }

            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                album.title.contains(query, ignoreCase = true) ||
                        album.artist.contains(query, ignoreCase = true) ||
                        album.qualityBadge.contains(query, ignoreCase = true) ||
                        album.source.displayName.contains(query, ignoreCase = true)
            }

            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initialize multi-source tracks and albums
        val initialTracks = audioRepository.getMultiSourceTracks()
        val initialAlbums = audioRepository.getAllAlbums()
        _allTracks.value = initialTracks
        _allAlbums.value = initialAlbums

        // Build recommendations
        refreshRecommendations(null)

        playerManager.setQueue(initialTracks, 0)

        // Observe network changes
        viewModelScope.launch {
            networkObserver.networkStatus.collect { netStatus ->
                playerManager.updateNetworkStatus(netStatus.statusText)
            }
        }

        // When current track changes, refresh recommendations based on it
        viewModelScope.launch {
            playerState.collect { state ->
                state.currentTrack?.let { refreshRecommendations(it) }
            }
        }

        // Try scanning device storage
        loadDeviceTracks()
    }

    fun refreshRecommendations(activeTrack: Track?) {
        _recommendations.value = recommendationEngine.getOnlineRecommendationSections(activeTrack)
    }

    fun loadDeviceTracks() {
        viewModelScope.launch {
            val deviceTracks = audioRepository.loadDeviceAudio()
            if (deviceTracks.isNotEmpty()) {
                val combined = audioRepository.getMultiSourceTracks() + deviceTracks
                _allTracks.value = combined
                playerManager.setQueue(combined, playerState.value.currentIndex)
            }
        }
    }

    fun playTrack(track: Track) {
        val currentFiltered = filteredTracks.value.ifEmpty { _allTracks.value }
        playerManager.playTrack(track, currentFiltered)
    }

    // Album Actions
    fun openAlbum(album: Album) {
        _selectedAlbum.value = album
        _isAlbumSheetVisible.value = true
    }

    fun closeAlbumSheet() {
        _isAlbumSheetVisible.value = false
    }

    fun playAlbum(album: Album) {
        if (album.tracks.isNotEmpty()) {
            playerManager.setQueue(album.tracks, 0)
            playerManager.playTrack(album.tracks.first(), album.tracks)
        }
    }

    fun shuffleAlbum(album: Album) {
        if (album.tracks.isNotEmpty()) {
            val shuffled = album.tracks.shuffled()
            playerManager.setQueue(shuffled, 0)
            playerManager.playTrack(shuffled.first(), shuffled)
        }
    }

    // Playlist Actions
    fun openAddToPlaylist(track: Track) {
        _trackForPlaylist.value = track
        _isAddToPlaylistSheetVisible.value = true
    }

    fun closeAddToPlaylistSheet() {
        _isAddToPlaylistSheetVisible.value = false
        _trackForPlaylist.value = null
    }

    fun addTrackToPlaylist(playlist: Playlist, track: Track) {
        playlistRepository.addTrackToPlaylist(playlist.id, track)
    }

    fun openCreatePlaylistDialog() {
        _isCreatePlaylistDialogVisible.value = true
    }

    fun closeCreatePlaylistDialog() {
        _isCreatePlaylistDialogVisible.value = false
    }

    fun createPlaylist(name: String, description: String, isAlbum: Boolean) {
        val created = playlistRepository.createPlaylist(name, description, isAlbum)
        _trackForPlaylist.value?.let { track ->
            playlistRepository.addTrackToPlaylist(created.id, track)
        }
    }

    fun playPlaylist(playlist: Playlist) {
        if (playlist.tracks.isNotEmpty()) {
            playerManager.setQueue(playlist.tracks, 0)
            playerManager.playTrack(playlist.tracks.first(), playlist.tracks)
        }
    }

    fun openPlaylistAsAlbum(playlist: Playlist) {
        val album = Album(
            id = playlist.id,
            title = playlist.name,
            artist = if (playlist.isCustomAlbum) "Custom Album" else "User Playlist",
            coverArtUrl = playlist.coverArtUrl,
            year = "2024",
            source = playlist.tracks.firstOrNull()?.source ?: com.example.auramusic.model.StreamingSource.SPOTIFY,
            qualityBadge = if (playlist.isCustomAlbum) "CUSTOM ALBUM" else "PLAYLIST",
            isLossless = playlist.tracks.any { it.isLossless },
            description = playlist.description,
            tracks = playlist.tracks,
            isUserCreated = true
        )
        openAlbum(album)
    }

    // Player Controls
    fun togglePlayPause() = playerManager.togglePlayPause()

    fun skipNext() = playerManager.skipNext()

    fun skipPrevious() = playerManager.skipPrevious()

    fun seekToRatio(ratio: Float) = playerManager.seekToRatio(ratio)

    fun toggleShuffle() = playerManager.toggleShuffle()

    fun toggleRepeat() = playerManager.toggleRepeat()

    fun toggleTravelMode() = playerManager.toggleTravelMode()

    fun setBitrateMode(mode: AudioBitrateMode) = playerManager.setBitrateMode(mode)

    fun setPlaybackSpeed(speed: Float) = playerManager.setPlaybackSpeed(speed)

    fun setNavTab(tab: com.example.auramusic.ui.components.PixelNavTab) {
        _currentTab.value = tab
        when (tab) {
            com.example.auramusic.ui.components.PixelNavTab.HOME -> {
                _selectedCategory.value = "All Tracks"
            }
            com.example.auramusic.ui.components.PixelNavTab.FOR_YOU -> {
                _selectedCategory.value = "Recommendations"
            }
            com.example.auramusic.ui.components.PixelNavTab.SEARCH -> {
                // Keep search active
            }
            com.example.auramusic.ui.components.PixelNavTab.LIBRARY -> {
                _selectedCategory.value = "My Playlists"
            }
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank() && _currentTab.value != com.example.auramusic.ui.components.PixelNavTab.SEARCH) {
            _currentTab.value = com.example.auramusic.ui.components.PixelNavTab.SEARCH
        }
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
