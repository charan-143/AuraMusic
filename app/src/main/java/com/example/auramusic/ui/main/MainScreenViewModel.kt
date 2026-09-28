package com.example.auramusic.ui.main

import android.app.Application
import android.content.Context
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
import com.example.auramusic.network.OnlineMusicSearchService
import com.example.auramusic.player.MusicPlayerManager
import com.example.auramusic.player.PlayerState
import com.example.auramusic.recommendation.RecommendationEngine
import com.example.auramusic.ui.components.PixelNavTab
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("aura_music_settings", Context.MODE_PRIVATE)

    private val audioRepository = AudioRepository(application)
    private val playerManager = MusicPlayerManager(application)
    private val networkObserver = NetworkQualityObserver(application)
    private val playlistRepository = PlaylistRepository(application, audioRepository)
    private val recommendationEngine = RecommendationEngine(audioRepository)
    private val onlineSearchService = OnlineMusicSearchService()

    val playerState: StateFlow<PlayerState> = playerManager.playerState
    val networkStatus = networkObserver.networkStatus
    val playlists: StateFlow<List<Playlist>> = playlistRepository.playlists
    val favoriteTrackIds: StateFlow<Set<String>> = playlistRepository.favoriteTrackIds

    // Theme Mode: Light Mode vs Dark Mode (Defaults to OLED Dark)
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("pref_is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleThemeMode() {
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        prefs.edit().putBoolean("pref_is_dark_mode", next).apply()
    }

    // Audio Streaming Quality Preference
    private val _streamingQuality = MutableStateFlow(
        prefs.getString("pref_streaming_quality", "Lossless Master (24-bit FLAC)") ?: "Lossless Master (24-bit FLAC)"
    )
    val streamingQuality: StateFlow<String> = _streamingQuality.asStateFlow()

    fun setStreamingQuality(quality: String) {
        _streamingQuality.value = quality
        prefs.edit().putString("pref_streaming_quality", quality).apply()
    }

    // 4 Bottom Navigation Tabs: Home, Search (Combined Search + For You), Library, Settings
    private val _currentTab = MutableStateFlow(PixelNavTab.HOME)
    val currentTab: StateFlow<PixelNavTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchingOnline = MutableStateFlow(false)
    val isSearchingOnline: StateFlow<Boolean> = _isSearchingOnline.asStateFlow()

    private val _onlineSearchTracks = MutableStateFlow<List<Track>>(emptyList())
    private val _onlineSearchAlbums = MutableStateFlow<List<Album>>(emptyList())

    private var searchJob: Job? = null

    private val _selectedCategory = MutableStateFlow("All Tracks")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Clean dynamic tracks & albums (no hardcoded presets)
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

    private val _isAudioRouteSheetVisible = MutableStateFlow(false)
    val isAudioRouteSheetVisible: StateFlow<Boolean> = _isAudioRouteSheetVisible.asStateFlow()

    private val _isSleepTimerDialogVisible = MutableStateFlow(false)
    val isSleepTimerDialogVisible: StateFlow<Boolean> = _isSleepTimerDialogVisible.asStateFlow()

    val categories = listOf(
        "All Tracks",
        "Albums",
        "My Playlists",
        "Spotify",
        "YouTube Music",
        "Lossless FLAC",
        "Device Library"
    )

    // Filtered Tracks: combines local catalog + live online search results
    val filteredTracks: StateFlow<List<Track>> = combine(
        _allTracks,
        _onlineSearchTracks,
        _searchQuery,
        _selectedCategory
    ) { localTracks, onlineTracks, query, category ->
        val pool = if (query.isNotBlank()) {
            val localMatches = localTracks.filter { track ->
                track.title.contains(query, ignoreCase = true) ||
                track.artist.contains(query, ignoreCase = true) ||
                track.album.contains(query, ignoreCase = true) ||
                track.qualityBadge.contains(query, ignoreCase = true)
            }
            (localMatches + onlineTracks).distinctBy { "${it.title.lowercase().trim()}_${it.artist.lowercase().trim()}" }
        } else {
            localTracks
        }

        when (category) {
            "All Tracks", "All", "My Playlists" -> pool
            "Albums" -> emptyList()
            "Spotify" -> pool.filter { it.source == com.example.auramusic.model.StreamingSource.SPOTIFY }
            "YouTube Music" -> pool.filter { it.source == com.example.auramusic.model.StreamingSource.YOUTUBE_MUSIC }
            "Lossless FLAC" -> pool.filter { it.isLossless }
            else -> pool.filter {
                it.category.equals(category, ignoreCase = true) ||
                it.source.displayName.equals(category, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Albums: combines local albums + live online search results
    val filteredAlbums: StateFlow<List<Album>> = combine(
        _allAlbums,
        _onlineSearchAlbums,
        _searchQuery,
        _selectedCategory
    ) { localAlbums, onlineAlbums, query, category ->
        val pool = if (query.isNotBlank()) {
            val localMatches = localAlbums.filter { album ->
                album.title.contains(query, ignoreCase = true) ||
                album.artist.contains(query, ignoreCase = true) ||
                album.qualityBadge.contains(query, ignoreCase = true) ||
                album.source.displayName.contains(query, ignoreCase = true)
            }
            (localMatches + onlineAlbums).distinctBy { "${it.title.lowercase().trim()}_${it.artist.lowercase().trim()}" }
        } else {
            localAlbums
        }

        when (category) {
            "All Tracks", "All", "Albums" -> pool
            "Spotify" -> pool.filter { it.source.displayName.contains("Spotify", ignoreCase = true) }
            "YouTube Music" -> pool.filter { it.source.displayName.contains("YouTube", ignoreCase = true) }
            "Lossless FLAC" -> pool.filter { it.isLossless }
            else -> pool
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Build initial discovery recommendations
        refreshRecommendations(null)

        // Observe network changes for dynamic bitrate adaption
        viewModelScope.launch {
            networkObserver.networkStatus.collect { netStatus ->
                playerManager.updateNetworkStatus(netStatus.statusText)
            }
        }

        // When current track ID actually changes, refresh recommendations
        viewModelScope.launch {
            var lastTrackId: String? = null
            playerState.collect { state ->
                val track = state.currentTrack
                if (track?.id != lastTrackId) {
                    lastTrackId = track?.id
                    if (track != null) {
                        refreshRecommendations(track)
                    }
                }
            }
        }

        // Scan device storage for any local audio files
        loadDeviceTracks()
    }

    private fun loadDeviceTracks() {
        viewModelScope.launch {
            val deviceAudio = audioRepository.loadDeviceAudio()
            if (deviceAudio.isNotEmpty()) {
                val updated = (audioRepository.getMultiSourceTracks() + deviceAudio).distinctBy { it.id }
                _allTracks.value = updated
                playerManager.setQueue(updated, 0)
                refreshRecommendations(playerState.value.currentTrack)
            }
        }
    }

    fun rescanDeviceAudio() {
        loadDeviceTracks()
    }

    fun clearStreamCache() {
        viewModelScope.launch {
            try {
                getApplication<Application>().cacheDir.deleteRecursively()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun refreshRecommendations(track: Track?) {
        _recommendations.value = recommendationEngine.getOnlineRecommendationSections(track)
    }

    // Playback and Selection
    fun playTrack(track: Track) {
        val currentQueue = filteredTracks.value.ifEmpty { listOf(track) }
        val index = currentQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playerManager.setQueue(currentQueue, index)
        playerManager.playTrack(track, currentQueue)
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

    // Navigation and Search
    fun setNavTab(tab: PixelNavTab) {
        _currentTab.value = tab
        when (tab) {
            PixelNavTab.HOME -> {
                _selectedCategory.value = "All Tracks"
            }
            PixelNavTab.SEARCH -> {
                // Focus search / discovery mode
            }
            PixelNavTab.LIBRARY -> {
                _selectedCategory.value = "My Playlists"
            }
            PixelNavTab.SETTINGS -> {
                // Settings tab
            }
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.isNotBlank()) {
            if (_currentTab.value != PixelNavTab.SEARCH) {
                _currentTab.value = PixelNavTab.SEARCH
            }

            if (query.trim().length >= 2) {
                searchJob = viewModelScope.launch {
                    delay(200)
                    _isSearchingOnline.value = true
                    try {
                        val result = onlineSearchService.searchOnline(query.trim())
                        _onlineSearchTracks.value = result.tracks
                        _onlineSearchAlbums.value = result.albums
                    } catch (e: Exception) {
                        // ignore network error
                    } finally {
                        _isSearchingOnline.value = false
                    }
                }
            } else {
                _onlineSearchTracks.value = emptyList()
                _onlineSearchAlbums.value = emptyList()
                _isSearchingOnline.value = false
            }
        } else {
            _onlineSearchTracks.value = emptyList()
            _onlineSearchAlbums.value = emptyList()
            _isSearchingOnline.value = false
        }
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

    fun setExpandedPlayer(expanded: Boolean) {
        _isExpandedPlayer.value = expanded
    }

    fun setQueueVisible(visible: Boolean) {
        _isQueueVisible.value = visible
    }

    // Audio Route Sheet Controls
    fun openAudioRouteSheet() {
        _isAudioRouteSheetVisible.value = true
    }

    fun closeAudioRouteSheet() {
        _isAudioRouteSheetVisible.value = false
    }

    fun setActiveAudioOutputDevice(name: String) {
        playerManager.setActiveAudioOutputDevice(name)
    }

    fun getAudioSessionId(): Int = playerManager.getAudioSessionId()

    // Sleep Timer Controls
    fun openSleepTimerDialog() {
        _isSleepTimerDialogVisible.value = true
    }

    fun closeSleepTimerDialog() {
        _isSleepTimerDialogVisible.value = false
    }

    fun setSleepTimer(minutes: Int) {
        playerManager.setSleepTimer(minutes)
    }

    // Favorites
    fun toggleFavorite(track: Track): Boolean = playlistRepository.toggleFavorite(track)

    fun isFavorite(trackId: String): Boolean = playlistRepository.isFavorite(trackId)

    // Playlist deletion
    fun deletePlaylist(playlistId: String) = playlistRepository.deletePlaylist(playlistId)

    // Queue mutations
    fun removeFromQueue(trackId: String) = playerManager.removeFromQueue(trackId)

    fun clearQueue() = playerManager.clearQueue()

    fun addToQueue(track: Track) = playerManager.addToQueue(track)

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        playerManager.release()
    }
}
