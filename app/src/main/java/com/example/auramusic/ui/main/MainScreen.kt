package com.example.auramusic.ui.main

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auramusic.theme.AuraMusicTheme
import com.example.auramusic.theme.PixelTheme
import com.example.auramusic.ui.components.PixelAddToPlaylistSheet
import com.example.auramusic.ui.components.PixelAlbumCard
import com.example.auramusic.ui.components.PixelAlbumDetailSheet
import com.example.auramusic.ui.components.PixelAtAGlanceHeader
import com.example.auramusic.ui.components.PixelAudioRouteSheet
import com.example.auramusic.ui.components.PixelBottomNavBar
import com.example.auramusic.ui.components.PixelCreatePlaylistDialog
import com.example.auramusic.ui.components.PixelEqualizerSheet
import com.example.auramusic.ui.components.PixelExpandedPlayer
import com.example.auramusic.ui.components.PixelMiniPlayer
import com.example.auramusic.ui.components.PixelNavTab
import com.example.auramusic.ui.components.PixelQueueSheet
import com.example.auramusic.ui.components.PixelSleepTimerDialog
import com.example.auramusic.ui.components.PixelSquircleAlbumArt
import com.example.auramusic.ui.components.PixelTrackTile

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val streamingQuality by viewModel.streamingQuality.collectAsState()

    AuraMusicTheme(isDarkMode = isDarkMode) {
        val colors = PixelTheme.colors

        val playerState by viewModel.playerState.collectAsState()
        val networkStatus by viewModel.networkStatus.collectAsState()
        val currentTab by viewModel.currentTab.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()
        val isSearchingOnline by viewModel.isSearchingOnline.collectAsState()
        val filteredTracks by viewModel.filteredTracks.collectAsState()
        val filteredAlbums by viewModel.filteredAlbums.collectAsState()
        val recommendations by viewModel.recommendations.collectAsState()
        val playlists by viewModel.playlists.collectAsState()
        val favoriteTrackIds by viewModel.favoriteTrackIds.collectAsState()

        val selectedAlbum by viewModel.selectedAlbum.collectAsState()
        val isAlbumSheetVisible by viewModel.isAlbumSheetVisible.collectAsState()

        val trackForPlaylist by viewModel.trackForPlaylist.collectAsState()
        val isAddToPlaylistSheetVisible by viewModel.isAddToPlaylistSheetVisible.collectAsState()
        val isCreatePlaylistDialogVisible by viewModel.isCreatePlaylistDialogVisible.collectAsState()

        val isExpandedPlayer by viewModel.isExpandedPlayer.collectAsState()
        val isQueueVisible by viewModel.isQueueVisible.collectAsState()
        val isAudioRouteSheetVisible by viewModel.isAudioRouteSheetVisible.collectAsState()
        val isSleepTimerDialogVisible by viewModel.isSleepTimerDialogVisible.collectAsState()
        val equalizerState by viewModel.equalizerState.collectAsState()
        val isEqualizerSheetVisible by viewModel.isEqualizerSheetVisible.collectAsState()
        val selectedCategory by viewModel.selectedCategory.collectAsState()

        val sourceFilterChips = listOf("All Tracks", "Spotify", "YouTube Music", "Lossless FLAC", "Albums")
        val genreExploreChips = listOf("All", "Top Hits", "Pop", "Rock", "Lo-Fi", "Hip-Hop", "Electronic", "Ambient", "Classical")

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top At-a-Glance Widget with quick Light/Dark mode switch
                PixelAtAGlanceHeader(
                    isPlaying = playerState.isPlaying,
                    activeTrackTitle = playerState.currentTrack?.title,
                    networkStatusText = if (playerState.isAutoQualityEnabled) {
                        "${networkStatus.signalPercent}% Signal • ${playerState.activeStreamingQualityBadge}"
                    } else {
                        networkStatus.statusText
                    },
                    qualityBadge = if (playerState.isAutoQualityEnabled) playerState.activeStreamingQualityBadge else (playerState.currentTrack?.qualityBadge ?: "24-BIT FLAC"),
                    isTravelMode = playerState.isTravelModeEnabled,
                    isDarkMode = isDarkMode,
                    activeAudioDevice = playerState.activeAudioOutputDevice,
                    onTravelModeToggle = { viewModel.toggleTravelMode() },
                    onThemeToggle = { viewModel.toggleThemeMode() },
                    onAudioRouteClick = { viewModel.openAudioRouteSheet() }
                )

                // Main Scrollable Area per Active Tab
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 150.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ==========================================
                    // 1. HOME TAB (Clean Soundscape & Now Playing)
                    // ==========================================
                    if (currentTab == PixelNavTab.HOME) {
                        val currentTrack = playerState.currentTrack

                        if (currentTrack != null) {
                            // Currently Playing / Selected Hero Card
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(colors.cardBackground)
                                        .border(1.dp, colors.outlineVariant, RoundedCornerShape(26.dp))
                                        .clickable { viewModel.setExpandedPlayer(true) }
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PixelSquircleAlbumArt(
                                            coverArtUrl = currentTrack.coverArtUrl,
                                            isPlaying = playerState.isPlaying,
                                            cornerRadius = 16.dp,
                                            showVinylGrooves = false,
                                            titleFallback = currentTrack.title,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (playerState.isPlaying) "NOW PLAYING" else "CURRENTLY SELECTED",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.1.sp,
                                                color = colors.textSecondary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = currentTrack.title,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = currentTrack.artist,
                                                fontSize = 12.sp,
                                                color = colors.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(colors.activePillBackground)
                                                .clickable { viewModel.togglePlayPause() }
                                        ) {
                                            Icon(
                                                imageVector = if (playerState.isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = colors.activePillText,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (filteredTracks.isEmpty()) {
                            // Clean State Card (Zero hardcoded preset songs)
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(colors.cardBackground)
                                        .border(1.dp, colors.outlineVariant, RoundedCornerShape(28.dp))
                                        .padding(24.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(colors.surfaceContainer)
                                                .border(1.dp, colors.outlineVariant, RoundedCornerShape(20.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = "Clean Soundscape",
                                                tint = colors.textPrimary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "Clean Soundscape",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Preset songs removed. Search online for Spotify & YouTube Music tracks and albums, or connect your local library.",
                                            fontSize = 13.sp,
                                            color = colors.textSecondary,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(22.dp))
                                                    .background(colors.activePillBackground)
                                                    .clickable { viewModel.setNavTab(PixelNavTab.SEARCH) }
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = "Search",
                                                        tint = colors.activePillText,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Search Online",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.activePillText
                                                    )
                                                }
                                            }

                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(22.dp))
                                                    .background(colors.surfaceContainer)
                                                    .border(1.dp, colors.outlineVariant, RoundedCornerShape(22.dp))
                                                    .clickable { viewModel.setNavTab(PixelNavTab.LIBRARY) }
                                            ) {
                                                Text(
                                                    text = "Open Library",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                Text(
                                    text = "SOUNDSCAPE SONGS (${filteredTracks.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp,
                                    color = colors.textSecondary
                                )
                            }
                            items(
                                items = filteredTracks,
                                key = { it.id }
                            ) { track ->
                                val isSelected = playerState.currentTrack?.id == track.id
                                PixelTrackTile(
                                    track = track,
                                    isSelected = isSelected,
                                    isPlaying = playerState.isPlaying,
                                    onClick = { viewModel.playTrack(track) },
                                    onMoreClick = { viewModel.openAddToPlaylist(track) }
                                )
                            }
                        }
                    }

                    // ========================================================
                    // 2. SEARCH & FOR YOU TAB (Combined Discovery & Streaming)
                    // ========================================================
                    else if (currentTab == PixelNavTab.SEARCH) {
                        // Search Bar Input
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(colors.surfaceContainer)
                                    .border(1.dp, colors.outlineVariant, RoundedCornerShape(26.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search Spotify & YouTube Music...",
                                                color = colors.textTertiary,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { viewModel.setSearchQuery(it) },
                                            textStyle = TextStyle(
                                                color = colors.textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal
                                            ),
                                            cursorBrush = SolidColor(colors.textPrimary),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }
                                    if (searchQuery.isNotEmpty()) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = colors.textSecondary,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { viewModel.setSearchQuery("") }
                                        )
                                    }
                                }
                            }
                        }

                        // Source & Format Filter Chips Row (Spotify, YouTube, Lossless, Albums)
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(sourceFilterChips) { filter ->
                                    val isSelected = selectedCategory.equals(filter, ignoreCase = true)
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(if (isSelected) colors.activePillBackground else colors.surfaceContainer)
                                            .border(1.dp, if (isSelected) colors.activePillBackground else colors.outlineVariant, RoundedCornerShape(18.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                viewModel.setCategory(filter)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                        ) {
                                        Text(
                                            text = filter,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.activePillText else colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Genre Quick Chips Row
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(genreExploreChips) { genre ->
                                    val isSelected = searchQuery.equals(genre, ignoreCase = true)
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(if (isSelected) colors.activePillBackground else colors.surfaceContainer)
                                            .border(1.dp, colors.outlineVariant, RoundedCornerShape(18.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                if (genre == "All") viewModel.setSearchQuery("") else viewModel.setSearchQuery(genre)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.activePillText else colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Online Searching Loading Indicator
                        if (isSearchingOnline) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = colors.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Searching online Spotify & YouTube Music...",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }

                        // Online Albums Search Result Row
                        if (filteredAlbums.isNotEmpty()) {
                            item {
                                Text(
                                    text = "ALBUMS (${filteredAlbums.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(
                                        items = filteredAlbums,
                                        key = { it.id }
                                    ) { album ->
                                        PixelAlbumCard(
                                            album = album,
                                            onClick = { viewModel.openAlbum(album) },
                                            onPlayClick = { viewModel.playAlbum(album) }
                                        )
                                    }
                                }
                            }
                        }

                        // Matching Songs List
                        if (filteredTracks.isNotEmpty()) {
                            item {
                                Text(
                                    text = "SONGS (${filteredTracks.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp,
                                    color = colors.textSecondary
                                )
                            }
                            items(
                                items = filteredTracks,
                                key = { it.id }
                            ) { track ->
                                val isSelected = playerState.currentTrack?.id == track.id
                                PixelTrackTile(
                                    track = track,
                                    isSelected = isSelected,
                                    isPlaying = playerState.isPlaying,
                                    onClick = { viewModel.playTrack(track) },
                                    onMoreClick = { viewModel.openAddToPlaylist(track) }
                                )
                            }
                        }

                        // Algorithmic Recommendations ("For You" Carousels)
                        items(
                            items = recommendations,
                            key = { it.id }
                        ) { section ->
                            if (section.albums.isNotEmpty() || section.tracks.isNotEmpty()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text(
                                                text = section.title,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                text = section.subtitle,
                                                fontSize = 11.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.surfaceContainer)
                                                .border(0.5.dp, colors.outlineVariant, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = section.source.displayName.uppercase(),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                        }
                                    }

                                    if (section.albums.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(
                                                items = section.albums,
                                                key = { it.id }
                                            ) { album ->
                                                PixelAlbumCard(
                                                    album = album,
                                                    onClick = { viewModel.openAlbum(album) },
                                                    onPlayClick = { viewModel.playAlbum(album) }
                                                )
                                            }
                                        }
                                    }

                                    if (section.tracks.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            section.tracks.forEach { track ->
                                                val isSelected = playerState.currentTrack?.id == track.id
                                                PixelTrackTile(
                                                    track = track,
                                                    isSelected = isSelected,
                                                    isPlaying = playerState.isPlaying,
                                                    onClick = { viewModel.playTrack(track) },
                                                    onMoreClick = { viewModel.openAddToPlaylist(track) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 3. LIBRARY TAB (Custom Playlists & Albums)
                    // ==========================================
                    else if (currentTab == PixelNavTab.LIBRARY) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = "Your Music Library",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "${playlists.size} playlists & albums created",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(colors.activePillBackground)
                                        .clickable { viewModel.openCreatePlaylistDialog() }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "New",
                                            tint = colors.activePillText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "New",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.activePillText
                                        )
                                    }
                                }
                            }
                        }

                        if (playlists.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(colors.cardBackground)
                                        .border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp))
                                        .padding(24.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "No Playlists Yet",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Create your first custom playlist or album, or add songs from online search.",
                                            fontSize = 12.sp,
                                            color = colors.textSecondary,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = playlists,
                                key = { it.id }
                            ) { pl ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(colors.cardBackground)
                                        .border(1.dp, colors.outlineVariant, RoundedCornerShape(20.dp))
                                        .clickable { viewModel.openPlaylistAsAlbum(pl) }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        PixelSquircleAlbumArt(
                                            coverArtUrl = pl.coverArtUrl,
                                            isPlaying = false,
                                            cornerRadius = 14.dp,
                                            showVinylGrooves = false,
                                            titleFallback = pl.name,
                                            modifier = Modifier.size(52.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = pl.name,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(colors.surfaceContainer)
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = if (pl.isCustomAlbum) "CUSTOM ALBUM" else "PLAYLIST",
                                                        fontSize = 7.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.textPrimary
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${pl.trackCount} tracks • ${pl.formattedDuration}",
                                                fontSize = 11.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (pl.id != "pl_default_fav") {
                                                IconButton(
                                                    onClick = {
                                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                        viewModel.deletePlaylist(pl.id)
                                                        Toast.makeText(context, "Deleted ${pl.name}", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = colors.textTertiary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }

                                            IconButton(onClick = { viewModel.playPlaylist(pl) }) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(colors.activePillBackground)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Play",
                                                        tint = colors.activePillText,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 4. SETTINGS TAB (Preferences & Mode Switch)
                    // ==========================================
                    else if (currentTab == PixelNavTab.SETTINGS) {
                        item {
                            Text(
                                text = "Settings & Preferences",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        // Theme Mode Switch Card
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp))
                                    .padding(18.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(colors.surfaceContainer)
                                                .border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp))
                                        ) {
                                            Icon(
                                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                                contentDescription = "Theme",
                                                tint = colors.textPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = if (isDarkMode) "Dark Mode (OLED Black)" else "Light Mode (Monochrome Light)",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (isDarkMode) "True OLED pitch black theme" else "Crisp porcelain white theme",
                                                fontSize = 12.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }

                                    // Switch Button to switch between Light and Dark mode
                                    Switch(
                                        checked = isDarkMode,
                                        onCheckedChange = { viewModel.toggleThemeMode() },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = colors.activePillText,
                                            checkedTrackColor = colors.activePillBackground,
                                            uncheckedThumbColor = colors.activePillBackground,
                                            uncheckedTrackColor = colors.surfaceContainer
                                        )
                                    )
                                }
                            }
                        }

                        // Audio Streaming Quality Card
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp))
                                    .padding(18.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Audio Streaming Quality",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Select preferred bitrate mode across Spotify, YouTube Music, and FLAC.",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    val qualityOptions = listOf(
                                        "Auto (Signal Adaptive)",
                                        "Lossless Master (24-bit FLAC)",
                                        "Spotify High (320kbps)",
                                        "YouTube Music Opus (256kbps)",
                                        "Data Saver (128kbps / Roaming)"
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        qualityOptions.forEach { option ->
                                            val isSelected = option == streamingQuality
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (isSelected) colors.surfaceContainer else Color.Transparent)
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) colors.outline else colors.outlineVariant,
                                                        RoundedCornerShape(14.dp)
                                                    )
                                                    .clickable {
                                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                        viewModel.setStreamingQuality(option)
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = option,
                                                            fontSize = 13.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = colors.textPrimary
                                                        )
                                                        if (option.startsWith("Auto")) {
                                                            Text(
                                                                text = "Dynamically adapts to signal & network fluctuations",
                                                                fontSize = 10.sp,
                                                                color = colors.textSecondary
                                                            )
                                                        }
                                                    }
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = colors.textPrimary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Live Signal Fluctuation & Adaptive Quality Monitor Box
                                    if (streamingQuality.startsWith("Auto")) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(colors.surfaceContainer)
                                                .border(1.dp, colors.outline, RoundedCornerShape(16.dp))
                                                .padding(14.dp)
                                        ) {
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(if (networkStatus.isSignalFluctuating) colors.outline else colors.textPrimary)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = if (networkStatus.isSignalFluctuating) "Signal Dip Detected" else "Signal Adaptive ABR",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = colors.textPrimary
                                                        )
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(colors.cardBackground)
                                                            .border(1.dp, colors.outlineVariant, RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = playerState.activeStreamingQualityBadge,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = colors.textPrimary
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "${networkStatus.signalPercent}% Signal (${networkStatus.downstreamBandwidthKbps / 1000} Mbps)",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = colors.textSecondary
                                                    )
                                                    Text(
                                                        text = playerState.activeStreamingQualityTitle,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.textPrimary
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Dynamic Signal Strength Level Bar
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(colors.outlineVariant)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(fraction = (networkStatus.signalPercent / 100f).coerceIn(0.1f, 1f))
                                                            .height(4.dp)
                                                            .background(colors.textPrimary)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = playerState.autoQualitySwitchNote,
                                                    fontSize = 11.sp,
                                                    color = colors.textSecondary,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Audio Equalizer & Spatial FX Card
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        viewModel.openEqualizerSheet()
                                    }
                                    .padding(18.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(colors.surfaceContainer)
                                                .border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.GraphicEq,
                                                contentDescription = "Equalizer",
                                                tint = colors.textPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = "Audio Equalizer & Spatial FX",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                text = if (equalizerState.isEnabled) {
                                                    if (equalizerState.isAiMode) "✦ AI Assisted • ${equalizerState.detectedProfileName}" else "Manual Graphic • ${equalizerState.currentPreset.displayName}"
                                                } else "Equalizer Bypassed",
                                                fontSize = 12.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (equalizerState.isEnabled) colors.activePillBackground else colors.surfaceContainer)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = if (equalizerState.isEnabled) (if (equalizerState.isAiMode) "✦ AI ACTIVE" else "MANUAL") else "BYPASS",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (equalizerState.isEnabled) colors.activePillText else colors.textTertiary
                                        )
                                    }
                                }
                            }
                        }

                        // Storage & Cache Management Card
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp))
                                    .padding(18.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Library & Storage Management",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(colors.surfaceContainer)
                                                .border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                    viewModel.rescanDeviceAudio()
                                                    Toast.makeText(context, "Rescanning local device storage...", Toast.LENGTH_SHORT).show()
                                                }
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Rescan",
                                                    tint = colors.textPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Rescan Audio",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textPrimary
                                                )
                                            }
                                        }

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(colors.surfaceContainer)
                                                .border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                    viewModel.clearStreamCache()
                                                    Toast.makeText(context, "Stream cache cleared", Toast.LENGTH_SHORT).show()
                                                }
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.ClearAll,
                                                    contentDescription = "Clear Cache",
                                                    tint = colors.textPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Clear Cache",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Audio Output & Spatial Equalizer Card
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp))
                                    .padding(18.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(colors.surfaceContainer)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.GraphicEq,
                                                    contentDescription = "Equalizer",
                                                    tint = colors.textPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Equalizer & Spatial Audio",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textPrimary
                                                )
                                                Text(
                                                    text = "Active: ${playerState.activeAudioOutputDevice}",
                                                    fontSize = 12.sp,
                                                    color = colors.textSecondary
                                                )
                                            }
                                        }

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(colors.activePillBackground)
                                                .clickable {
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                    viewModel.openAudioRouteSheet()
                                                }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "Configure",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.activePillText
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Sleep Timer Card
                        item {
                            val sleepSec = playerState.sleepTimerRemainingSec
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp))
                                    .padding(18.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(colors.surfaceContainer)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bedtime,
                                                contentDescription = "Sleep Timer",
                                                tint = colors.textPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Sleep Timer",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                text = if (sleepSec != null && sleepSec > 0) "%d:%02d remaining".format(sleepSec / 60, sleepSec % 60) else "Off",
                                                fontSize = 12.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(colors.surfaceContainer)
                                            .border(1.dp, colors.outlineVariant, RoundedCornerShape(16.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                viewModel.openSleepTimerDialog()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = if (sleepSec != null && sleepSec > 0) "Adjust" else "Set Timer",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // About Card
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(colors.surfaceDim)
                                    .padding(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Aura Music v2.4.0",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Google Pixel Material 3 Expressive Monochrome",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Pixel Mini Player Bar
            AnimatedVisibility(
                visible = playerState.currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 68.dp)
            ) {
                val track = playerState.currentTrack
                if (track != null) {
                    PixelMiniPlayer(
                        track = track,
                        isPlaying = playerState.isPlaying,
                        progress = playerState.progress,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onSkipNextClick = { viewModel.skipNext() },
                        onExpandClick = { viewModel.setExpandedPlayer(true) }
                    )
                }
            }

            // Google Pixel Bottom Navigation Tabs Down (4 Tabs: Home, Search, Library, Settings)
            PixelBottomNavBar(
                selectedTab = currentTab,
                onTabSelected = { viewModel.setNavTab(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Fullscreen Google Pixel Expanded Player Screen
            AnimatedVisibility(
                visible = isExpandedPlayer,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                PixelExpandedPlayer(
                    playerState = playerState,
                    isFavorite = playerState.currentTrack?.let { favoriteTrackIds.contains(it.id) } == true,
                    onToggleFavorite = { playerState.currentTrack?.let { viewModel.toggleFavorite(it) } },
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onSkipNextClick = { viewModel.skipNext() },
                    onSkipPreviousClick = { viewModel.skipPrevious() },
                    onSeek = { viewModel.seekToRatio(it) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleRepeat = { viewModel.toggleRepeat() },
                    onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                    onCollapseClick = { viewModel.setExpandedPlayer(false) },
                    onQueueClick = { viewModel.setQueueVisible(true) },
                    onAudioRouteClick = { viewModel.openAudioRouteSheet() },
                    onMoreOptionsClick = { viewModel.openSleepTimerDialog() },
                    onEqualizerClick = { viewModel.openEqualizerSheet() },
                    activeAudioDevice = playerState.activeAudioOutputDevice
                )
            }

            // Up Next Queue Bottom Sheet
            AnimatedVisibility(
                visible = isQueueVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PixelQueueSheet(
                    queue = playerState.queue,
                    currentTrackId = playerState.currentTrack?.id,
                    isPlaying = playerState.isPlaying,
                    onTrackClick = { viewModel.playTrack(it) },
                    onCloseClick = { viewModel.setQueueVisible(false) },
                    onShuffleClick = { viewModel.toggleShuffle() },
                    onClearQueue = { viewModel.clearQueue() },
                    onRemoveTrack = { viewModel.removeFromQueue(it.id) }
                )
            }

            // Audio Output Route & Hardware EQ Bottom Sheet
            PixelAudioRouteSheet(
                visible = isAudioRouteSheetVisible,
                activeDeviceName = playerState.activeAudioOutputDevice,
                audioSessionId = viewModel.getAudioSessionId(),
                onDeviceSelected = { viewModel.setActiveAudioOutputDevice(it) },
                onDismiss = { viewModel.closeAudioRouteSheet() },
                onEqualizerClick = {
                    viewModel.closeAudioRouteSheet()
                    viewModel.openEqualizerSheet()
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Audio Equalizer Bottom Sheet (AI Assisted & Manual Graphic)
            PixelEqualizerSheet(
                visible = isEqualizerSheetVisible,
                equalizerState = equalizerState,
                audioSessionId = viewModel.getAudioSessionId(),
                onToggleEnabled = { viewModel.setEqualizerEnabled(it) },
                onToggleAiMode = { viewModel.setEqualizerAiMode(it) },
                onSelectAiTarget = { viewModel.setEqualizerAiTarget(it) },
                onBandLevelChange = { band, level -> viewModel.setEqualizerBandLevel(band, level) },
                onBassBoostChange = { viewModel.setEqualizerBassBoost(it) },
                onVirtualizerChange = { viewModel.setEqualizerVirtualizer(it) },
                onSelectPreset = { viewModel.applyEqualizerPreset(it) },
                onResetToFlat = { viewModel.resetEqualizerToFlat() },
                onDismiss = { viewModel.closeEqualizerSheet() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Sleep Timer Dialog
            PixelSleepTimerDialog(
                visible = isSleepTimerDialogVisible,
                remainingSeconds = playerState.sleepTimerRemainingSec,
                onSetTimer = { viewModel.setSleepTimer(it) },
                onDismiss = { viewModel.closeSleepTimerDialog() }
            )

            // Online Album Detail Sheet
            selectedAlbum?.let { album ->
                PixelAlbumDetailSheet(
                    album = album,
                    visible = isAlbumSheetVisible,
                    onDismiss = { viewModel.closeAlbumSheet() },
                    onPlayTrack = { viewModel.playTrack(it) },
                    onPlayAll = { viewModel.playAlbum(album) },
                    onShuffleAll = { viewModel.shuffleAlbum(album) },
                    onAddToPlaylistClick = { viewModel.openAddToPlaylist(it) }
                )
            }

            // Add To Playlist Sheet
            trackForPlaylist?.let { track ->
                PixelAddToPlaylistSheet(
                    track = track,
                    playlists = playlists,
                    visible = isAddToPlaylistSheetVisible,
                    onDismiss = { viewModel.closeAddToPlaylistSheet() },
                    onSelectPlaylist = { playlist ->
                        viewModel.addTrackToPlaylist(playlist, track)
                        viewModel.closeAddToPlaylistSheet()
                    },
                    onCreateNewClick = {
                        viewModel.closeAddToPlaylistSheet()
                        viewModel.openCreatePlaylistDialog()
                    }
                )
            }

            // Create Playlist / Custom Album Dialog
            PixelCreatePlaylistDialog(
                visible = isCreatePlaylistDialogVisible,
                onDismiss = { viewModel.closeCreatePlaylistDialog() },
                onCreate = { name, desc, isAlbum ->
                    viewModel.createPlaylist(name, desc, isAlbum)
                }
            )
        }
    }
}
