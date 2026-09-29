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
import com.example.auramusic.model.StreamingSource
import com.example.auramusic.model.Track
import com.example.auramusic.network.NetworkQualityObserver
import com.example.auramusic.network.OnlineMusicSearchService
import com.example.auramusic.player.MusicPlayerManager
import com.example.auramusic.player.PlaybackProgress
import com.example.auramusic.player.PlayerState
import com.example.auramusic.recommendation.RecommendationEngine
import com.example.auramusic.ui.components.PixelNavTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("aura_music_settings", Context.MODE_PRIVATE)

    private val audioRepository = AudioRepository(application)
    private val playerManager = MusicPlayerManager.getInstance(application)
    private val networkObserver = NetworkQualityObserver(application)
    private val playlistRepository = PlaylistRepository(application, audioRepository)
    private val recommendationEngine = RecommendationEngine(audioRepository, application)
    private val onlineSearchService = OnlineMusicSearchService()

    val playerState: StateFlow<PlayerState> = playerManager.playerState
    val playbackProgress: StateFlow<PlaybackProgress> = playerManager.playbackProgress
    val networkStatus = networkObserver.networkStatus
    val playlists: StateFlow<List<Playlist>> = playlistRepository.playlists
    val favoriteTrackIds: StateFlow<Set<String>> = combine(
        playlistRepository.favoriteTrackIds,
        playerManager.favoritesManager.favoriteIds
    ) { rIds: Set<String>, mIds: Set<String> ->
        rIds + mIds
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Theme Mode: Light Mode vs Dark Mode (Defaults to OLED Dark)
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("pref_is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleThemeMode() {
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        prefs.edit().putBoolean("pref_is_dark_mode", next).apply()
    }

    // Audio Streaming Quality Preference: defaults to Auto (Signal Adaptive)
    private val _streamingQuality = MutableStateFlow(
        prefs.getString("pref_streaming_quality", "Auto (Signal Adaptive)") ?: "Auto (Signal Adaptive)"
    )
    val streamingQuality: StateFlow<String> = _streamingQuality.asStateFlow()

    fun setStreamingQuality(quality: String) {
        _streamingQuality.value = quality
        prefs.edit().putString("pref_streaming_quality", quality).apply()
        val isAuto = quality.startsWith("Auto")
        playerManager.setAutoQualityEnabled(isAuto)
        if (isAuto) {
            val net = networkObserver.networkStatus.value
            playerManager.applyAdaptiveQuality(
                net.autoAudioQuality,
                net.downstreamBandwidthKbps,
                net.signalPercent,
                net.isSignalFluctuating,
                net.fluctuationNote
            )
        } else {
            playerManager.setManualQuality(quality)
        }
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

    private val _isPlaybackSpeedSheetVisible = MutableStateFlow(false)
    val isPlaybackSpeedSheetVisible: StateFlow<Boolean> = _isPlaybackSpeedSheetVisible.asStateFlow()

    fun openPlaybackSpeedSheet() { _isPlaybackSpeedSheetVisible.value = true }
    fun closePlaybackSpeedSheet() { _isPlaybackSpeedSheetVisible.value = false }
    fun setCrossfade(enabled: Boolean, durationSec: Int = 3) { playerManager.setCrossfade(enabled, durationSec) }

    // Offline Downloads
    val downloadStates = playerManager.offlineDownloadManager.downloadStates
    fun downloadTrack(track: Track) {
        audioRepository.addTracks(listOf(track))
        _allTracks.value = audioRepository.getMultiSourceTracks()
        playerManager.offlineDownloadManager.downloadTrack(track)
    }
    fun deleteDownloadedTrack(trackId: String) { playerManager.offlineDownloadManager.deleteDownload(trackId) }
    fun isTrackDownloaded(trackId: String): Boolean = playerManager.offlineDownloadManager.isDownloaded(trackId)

    fun loadDeviceTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tracks = audioRepository.loadDeviceAudio()
                if (tracks.isNotEmpty()) {
                    _allTracks.value = audioRepository.getMultiSourceTracks()
                }
            } catch (ignored: Exception) {}
        }
    }

    // Library Filters & Smart Auto-Playlists
    private val _libraryFilter = MutableStateFlow("All")
    val libraryFilter: StateFlow<String> = _libraryFilter.asStateFlow()
    val libraryFilters = listOf("All", "Favorites", "Downloaded", "Most Played", "Folders")
    fun setLibraryFilter(filter: String) { _libraryFilter.value = filter }

    val mostPlayedCounts: StateFlow<Map<String, Int>> = playerManager.favoritesManager.playCountsFlow
    val recentlyPlayedIds: StateFlow<List<String>> = playerManager.favoritesManager.recentlyPlayedIds

    val favoriteTracks: StateFlow<List<Track>> by lazy {
        combine(_allTracks, favoriteTrackIds, playlistRepository.playlists) { tracks, favIds, plists ->
            val favPlaylistTracks = plists.firstOrNull { it.id == "pl_default_fav" }?.tracks ?: emptyList()
            val fromAll = tracks.filter { it.id in favIds }
            (fromAll + favPlaylistTracks).distinctBy { it.id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val downloadedTracks: StateFlow<List<Track>> by lazy {
        combine(
            playerManager.offlineDownloadManager.downloadedTracks,
            _allTracks,
            downloadStates
        ) { offlineTracks, allTracks, states ->
            val localTracks = allTracks.filter { it.isLocal }
            val statesDownloaded = allTracks.filter { states[it.id] is com.example.auramusic.cache.DownloadState.Downloaded }
            (offlineTracks + localTracks + statesDownloaded).distinctBy { it.id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val mostPlayedTracks: StateFlow<List<Track>> by lazy {
        combine(_allTracks, mostPlayedCounts) { tracks, counts ->
            val withPlays = tracks.filter { (counts[it.id] ?: 0) > 0 }.sortedByDescending { counts[it.id] ?: 0 }
            if (withPlays.isNotEmpty()) withPlays else tracks.take(12)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val folderTracks: StateFlow<Map<String, List<Track>>> by lazy {
        _allTracks.map { tracks ->
            tracks.filter { it.isLocal }.groupBy {
                val path = it.localFilePath
                if (!path.isNullOrBlank()) {
                    java.io.File(path).parentFile?.name ?: "Device Storage"
                } else {
                    "Device Audio"
                }
            }.filter { it.value.isNotEmpty() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    }

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

    private val lyricsService = com.example.auramusic.network.LyricsService()

    private val _currentTrackLyrics = MutableStateFlow<com.example.auramusic.model.TrackLyrics?>(null)
    val currentTrackLyrics: StateFlow<com.example.auramusic.model.TrackLyrics?> = _currentTrackLyrics.asStateFlow()

    private val _isLyricsViewActive = MutableStateFlow(false)
    val isLyricsViewActive: StateFlow<Boolean> = _isLyricsViewActive.asStateFlow()

    fun toggleLyricsView() {
        _isLyricsViewActive.value = !_isLyricsViewActive.value
    }

    fun setLyricsView(active: Boolean) {
        _isLyricsViewActive.value = active
    }

    fun seekToPosition(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    private var lyricsJob: Job? = null

    private fun loadLyricsForTrack(track: Track) {
        lyricsJob?.cancel()
        _currentTrackLyrics.value = com.example.auramusic.model.TrackLyrics(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            isLoading = true
        )
        lyricsJob = viewModelScope.launch {
            val lyrics = lyricsService.getLyrics(track)
            _currentTrackLyrics.value = lyrics
        }
    }

    init {
        // Build initial discovery recommendations
        refreshRecommendations(null)

        // Fetch dynamic trending music from online catalog
        loadTrendingMusic()

        // Load physical audio files from device storage (Download, Music, etc.)
        loadDeviceTracks()

        // Initialize Audio Streaming Quality mode
        val initialQuality = _streamingQuality.value
        val isAuto = initialQuality.startsWith("Auto")
        playerManager.setAutoQualityEnabled(isAuto)
        if (isAuto) {
            val net = networkObserver.networkStatus.value
            playerManager.applyAdaptiveQuality(
                net.autoAudioQuality,
                net.downstreamBandwidthKbps,
                net.signalPercent,
                net.isSignalFluctuating,
                net.fluctuationNote
            )
        } else {
            playerManager.setManualQuality(initialQuality)
        }

        // Observe network changes & signal fluctuations for dynamic bitrate adaptation
        viewModelScope.launch {
            networkObserver.networkStatus.collect { netStatus ->
                playerManager.updateNetworkStatus(netStatus.statusText)
                if (_streamingQuality.value.startsWith("Auto")) {
                    playerManager.applyAdaptiveQuality(
                        netStatus.autoAudioQuality,
                        netStatus.downstreamBandwidthKbps,
                        netStatus.signalPercent,
                        netStatus.isSignalFluctuating,
                        netStatus.fluctuationNote
                    )
                }
            }
        }

        // When current track ID actually changes, refresh recommendations & fetch live synced lyrics
        viewModelScope.launch {
            var lastTrack: Track? = null
            var lastTrackStartTime = 0L
            playerState.collect { state ->
                val track = state.currentTrack
                if (track?.id != lastTrack?.id) {
                    val prev = lastTrack
                    if (prev != null) {
                        val playedDuration = System.currentTimeMillis() - lastTrackStartTime
                        if (playedDuration >= 45000L || state.progress >= 0.70f) {
                            recommendationEngine.recordPlaybackCompletion(prev)
                        } else if (playedDuration < 20000L) {
                            recommendationEngine.recordSkip(prev, playedDuration, prev.durationMs)
                        }
                    }

                    lastTrack = track
                    lastTrackStartTime = System.currentTimeMillis()

                    if (track != null) {
                        recommendationEngine.recordPlaybackStart(track)
                        refreshRecommendations(track)
                        loadLyricsForTrack(track)
                    } else {
                        _currentTrackLyrics.value = null
                    }
                }
            }
        }

        // Scan device storage for any local audio files
        loadDeviceTracks()
    }

    fun loadTrendingMusic() {
        viewModelScope.launch {
            try {
                val trending = onlineSearchService.fetchTrendingMusic()
                if (trending.tracks.isNotEmpty() || trending.albums.isNotEmpty()) {
                    _allTracks.value = (_allTracks.value + trending.tracks).distinctBy { it.id }
                    _allAlbums.value = (_allAlbums.value + trending.albums).distinctBy { it.id }
                    refreshRecommendations(playerState.value.currentTrack)
                }
            } catch (e: Exception) {
                // Fallback handled in RecommendationEngine
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

    private var recommendationJob: Job? = null

    private fun refreshRecommendations(track: Track?) {
        recommendationJob?.cancel()
        recommendationJob = viewModelScope.launch(Dispatchers.Default) {
            val mood = _selectedCategory.value
            val onlineTracksOnly = _allTracks.value.filter { 
                !it.isLocal && !it.isCachedOffline && it.source != StreamingSource.LOCAL_STORAGE 
            }
            val onlineAlbumsOnly = _allAlbums.value.filter { 
                it.source != StreamingSource.LOCAL_STORAGE 
            }
            val effectiveActiveTrack = if (track?.isLocal == true || track?.isCachedOffline == true || track?.source == StreamingSource.LOCAL_STORAGE) null else track

            val rawSections = recommendationEngine.getOnlineRecommendationSections(
                activeTrack = effectiveActiveTrack,
                providedTracks = onlineTracksOnly,
                providedAlbums = onlineAlbumsOnly
            )

            val updatedSections = if (mood.isNotBlank() && mood != "All Tracks" && mood != "All Recommendations" && mood != "All" && mood != "My Playlists" && mood != "Albums") {
                val moodTracks = recommendationEngine.getTracksForMoodChip(mood, onlineTracksOnly)
                if (moodTracks.isNotEmpty()) {
                    listOf(
                        RecommendationSection(
                            id = "rec_mood_${mood.lowercase().replace(" ", "_")}",
                            title = "$mood Curation",
                            subtitle = "Harmonically tuned algorithmic flow matching your $mood vibe",
                            source = effectiveActiveTrack?.source ?: com.example.auramusic.model.StreamingSource.SPOTIFY,
                            albums = emptyList(),
                            tracks = moodTracks.take(8)
                        )
                    ) + rawSections
                } else {
                    rawSections
                }
            } else {
                rawSections
            }
            _recommendations.value = updatedSections
        }
    }

    /**
     * Generates an intelligent, continuous 15-track smart radio queue matching the seed track
     * and immediately begins playback.
     */
    fun startTrackRadio(seedTrack: Track) {
        viewModelScope.launch(Dispatchers.Default) {
            val pool = _allTracks.value.ifEmpty { audioRepository.getMultiSourceTracks() }
            val radioQueue = recommendationEngine.generateRadioQueue(seedTrack, pool, count = 15)
            withContext(Dispatchers.Main) {
                playerManager.setQueue(radioQueue, 0)
                playerManager.playTrack(seedTrack, radioQueue)
            }
        }
    }

    // Playback and Selection
    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        audioRepository.addTracks(listOf(track))
        _allTracks.value = audioRepository.getMultiSourceTracks()
        val currentQueue = newQueue ?: filteredTracks.value.ifEmpty { 
            val recTracks = _recommendations.value.flatMap { it.tracks }
            if (recTracks.isNotEmpty()) recTracks else listOf(track)
        }
        val index = currentQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playerManager.setQueue(currentQueue, index)
        playerManager.playTrack(track, currentQueue)
    }

    // Album Actions
    fun openAlbum(album: Album) {
        _selectedAlbum.value = album
        _isAlbumSheetVisible.value = true
        if (album.tracks.isEmpty() || album.tracks.size <= 1) {
            viewModelScope.launch {
                val fullTracks = onlineSearchService.fetchAlbumTracks(album.id)
                if (fullTracks.isNotEmpty()) {
                    _selectedAlbum.value = album.copy(tracks = fullTracks)
                }
            }
        }
    }

    fun closeAlbumSheet() {
        _isAlbumSheetVisible.value = false
    }

    fun playAlbum(album: Album) {
        if (album.tracks.isNotEmpty()) {
            playerManager.setQueue(album.tracks, 0)
            playerManager.playTrack(album.tracks.first(), album.tracks)
        } else {
            viewModelScope.launch {
                val fullTracks = onlineSearchService.fetchAlbumTracks(album.id)
                if (fullTracks.isNotEmpty()) {
                    _selectedAlbum.value = album.copy(tracks = fullTracks)
                    playerManager.setQueue(fullTracks, 0)
                    playerManager.playTrack(fullTracks.first(), fullTracks)
                }
            }
        }
    }

    fun shuffleAlbum(album: Album) {
        if (album.tracks.isNotEmpty()) {
            val shuffled = album.tracks.shuffled()
            playerManager.setQueue(shuffled, 0)
            playerManager.playTrack(shuffled.first(), shuffled)
        } else {
            viewModelScope.launch {
                val fullTracks = onlineSearchService.fetchAlbumTracks(album.id)
                if (fullTracks.isNotEmpty()) {
                    val shuffled = fullTracks.shuffled()
                    _selectedAlbum.value = album.copy(tracks = fullTracks)
                    playerManager.setQueue(shuffled, 0)
                    playerManager.playTrack(shuffled.first(), shuffled)
                }
            }
        }
    }

    fun searchTrending(term: String) {
        setNavTab(PixelNavTab.SEARCH)
        setSearchQuery(term)
    }

    fun searchGenre(genre: String) {
        setNavTab(PixelNavTab.SEARCH)
        setSearchQuery(genre)
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
        refreshRecommendations(playerState.value.currentTrack)
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

    fun resetAudioOutputDeviceToAuto() {
        playerManager.resetAudioOutputDeviceToAuto()
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
    fun toggleFavorite(track: Track): Boolean {
        audioRepository.addTracks(listOf(track))
        _allTracks.value = audioRepository.getMultiSourceTracks()
        playerManager.favoritesManager.toggleFavorite(track.id)
        return playlistRepository.toggleFavorite(track)
    }

    fun isFavorite(trackId: String): Boolean =
        playerManager.favoritesManager.isFavorite(trackId) || playlistRepository.isFavorite(trackId)

    // Playlist deletion
    fun deletePlaylist(playlistId: String) = playlistRepository.deletePlaylist(playlistId)

    // Queue mutations
    fun removeFromQueue(trackId: String) = playerManager.removeFromQueue(trackId)

    fun clearQueue() = playerManager.clearQueue()

    fun addToQueue(track: Track) = playerManager.addToQueue(track)

    // Equalizer Controls
    val equalizerState = playerManager.equalizerManager.state

    private val _isEqualizerSheetVisible = MutableStateFlow(false)
    val isEqualizerSheetVisible: StateFlow<Boolean> = _isEqualizerSheetVisible.asStateFlow()

    fun openEqualizerSheet() {
        _isEqualizerSheetVisible.value = true
    }

    fun closeEqualizerSheet() {
        _isEqualizerSheetVisible.value = false
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        playerManager.equalizerManager.setEnabled(enabled)
    }

    fun setEqualizerAiMode(isAiMode: Boolean) {
        playerManager.equalizerManager.setAiMode(isAiMode)
    }

    fun setEqualizerAiTarget(target: com.example.auramusic.audio.AiAudioTarget) {
        playerManager.equalizerManager.setAiTarget(target)
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMilliBels: Int) {
        playerManager.equalizerManager.setBandLevel(bandIndex, levelMilliBels)
    }

    fun setEqualizerBassBoost(strength: Int) {
        playerManager.equalizerManager.setBassBoost(strength)
    }

    fun setEqualizerVirtualizer(strength: Int) {
        playerManager.equalizerManager.setVirtualizer(strength)
    }

    fun applyEqualizerPreset(preset: com.example.auramusic.audio.EqualizerPreset) {
        playerManager.equalizerManager.applyPreset(preset)
    }

    fun resetEqualizerToFlat() {
        playerManager.equalizerManager.resetToFlat()
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        playerManager.release()
    }
}
