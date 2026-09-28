package com.example.auramusic.ui.main

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auramusic.model.Album
import com.example.auramusic.model.Playlist
import com.example.auramusic.theme.MonochromeBlack
import com.example.auramusic.theme.MonochromeLightGrey
import com.example.auramusic.theme.MonochromeMuted
import com.example.auramusic.theme.MonochromeOutline
import com.example.auramusic.theme.MonochromeOutlineVariant
import com.example.auramusic.theme.MonochromeSilver
import com.example.auramusic.theme.MonochromeSurface
import com.example.auramusic.theme.MonochromeSurfaceContainer
import com.example.auramusic.theme.MonochromeSurfaceHigh
import com.example.auramusic.theme.MonochromeWhite
import com.example.auramusic.ui.components.PixelAddToPlaylistSheet
import com.example.auramusic.ui.components.PixelAlbumCard
import com.example.auramusic.ui.components.PixelAlbumDetailSheet
import com.example.auramusic.ui.components.PixelAtAGlanceHeader
import com.example.auramusic.ui.components.PixelBottomNavBar
import com.example.auramusic.ui.components.PixelCreatePlaylistDialog
import com.example.auramusic.ui.components.PixelExpandedPlayer
import com.example.auramusic.ui.components.PixelFilterChips
import com.example.auramusic.ui.components.PixelMiniPlayer
import com.example.auramusic.ui.components.PixelNavTab
import com.example.auramusic.ui.components.PixelQueueSheet
import com.example.auramusic.ui.components.PixelSquircleAlbumArt
import com.example.auramusic.ui.components.PixelTrackTile

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val tracks by viewModel.filteredTracks.collectAsStateWithLifecycle()
    val filteredAlbums by viewModel.filteredAlbums.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()
    val isAlbumSheetVisible by viewModel.isAlbumSheetVisible.collectAsStateWithLifecycle()
    val trackForPlaylist by viewModel.trackForPlaylist.collectAsStateWithLifecycle()
    val isAddToPlaylistSheetVisible by viewModel.isAddToPlaylistSheetVisible.collectAsStateWithLifecycle()
    val isCreatePlaylistDialogVisible by viewModel.isCreatePlaylistDialogVisible.collectAsStateWithLifecycle()
    val isExpandedPlayer by viewModel.isExpandedPlayer.collectAsStateWithLifecycle()
    val isQueueVisible by viewModel.isQueueVisible.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MonochromeBlack)
    ) {
        // Main Screen View
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Google Pixel At-a-Glance Widget Header (visible on Home)
            if (currentTab == PixelNavTab.HOME) {
                PixelAtAGlanceHeader(
                    isPlaying = playerState.isPlaying,
                    activeTrackTitle = playerState.currentTrack?.title,
                    networkStatusText = playerState.networkStatusText,
                    qualityBadge = playerState.currentTrack?.qualityBadge ?: "24-BIT FLAC",
                    isTravelMode = playerState.isTravelModeEnabled,
                    onTravelModeToggle = { viewModel.toggleTravelMode() }
                )
            } else {
                // Secondary Tab Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Column {
                        Text(
                            text = when (currentTab) {
                                PixelNavTab.FOR_YOU -> "Recommendations"
                                PixelNavTab.SEARCH -> "Explore & Search"
                                PixelNavTab.LIBRARY -> "Your Library"
                                else -> "Aura Music"
                            },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MonochromeWhite
                        )
                        Text(
                            text = when (currentTab) {
                                PixelNavTab.FOR_YOU -> "Curated Spotify, YouTube Music & Lossless streams"
                                PixelNavTab.SEARCH -> "Find songs, albums, and artists"
                                PixelNavTab.LIBRARY -> "Playlists, custom albums & cached music"
                                else -> "Monochrome Audio"
                            },
                            fontSize = 12.sp,
                            color = MonochromeSilver
                        )
                    }

                    if (currentTab == PixelNavTab.LIBRARY) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MonochromeSurfaceContainer)
                                .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(16.dp))
                                .clickable { viewModel.openCreatePlaylistDialog() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New",
                                    tint = MonochromeWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "New",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MonochromeWhite
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar (shown on SEARCH tab or when search is active)
            if (currentTab == PixelNavTab.SEARCH || currentTab == PixelNavTab.HOME) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(26.dp))
                            .background(MonochromeSurfaceContainer)
                            .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(26.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MonochromeSilver,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search Spotify & YT Music albums, songs...",
                                        color = MonochromeMuted,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    textStyle = TextStyle(
                                        color = MonochromeWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    cursorBrush = SolidColor(MonochromeWhite),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MonochromeSilver,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.setSearchQuery("") }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // "+ Create Album / Playlist" Quick Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MonochromeSurfaceContainer)
                            .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(20.dp))
                            .clickable { viewModel.openCreatePlaylistDialog() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Album or Playlist",
                            tint = MonochromeWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category Filter Chips on HOME or SEARCH
            if (currentTab == PixelNavTab.HOME || currentTab == PixelNavTab.SEARCH) {
                PixelFilterChips(
                    categories = listOf("All Tracks", "Lossless FLAC", "Spotify", "YouTube Music", "Device Library"),
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.setCategory(it) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 150.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. SEARCH ACTIVE OR SEARCH TAB
                if (searchQuery.isNotBlank() || currentTab == PixelNavTab.SEARCH) {
                    if (filteredAlbums.isNotEmpty()) {
                        item {
                            Text(
                                text = "MATCHING ONLINE ALBUMS (${filteredAlbums.size})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                color = MonochromeSilver
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(filteredAlbums) { album ->
                                    PixelAlbumCard(
                                        album = album,
                                        onClick = { viewModel.openAlbum(album) },
                                        onPlayClick = { viewModel.playAlbum(album) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "MATCHING SONGS (${tracks.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            color = MonochromeSilver
                        )
                    }

                    items(tracks) { track ->
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
                // 2. FOR YOU / RECOMMENDATIONS TAB
                else if (currentTab == PixelNavTab.FOR_YOU) {
                    items(recommendations) { section ->
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
                                        color = MonochromeWhite
                                    )
                                    Text(
                                        text = section.subtitle,
                                        fontSize = 11.sp,
                                        color = MonochromeMuted
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MonochromeSurfaceContainer)
                                        .border(0.5.dp, MonochromeOutlineVariant, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = section.source.displayName.uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MonochromeWhite
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Recommended Albums
                            if (section.albums.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(section.albums) { album ->
                                        PixelAlbumCard(
                                            album = album,
                                            onClick = { viewModel.openAlbum(album) },
                                            onPlayClick = { viewModel.playAlbum(album) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Recommended Songs
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
                // 3. LIBRARY TAB
                else if (currentTab == PixelNavTab.LIBRARY) {
                    item {
                        Text(
                            text = "CUSTOM ALBUMS & PLAYLISTS (${playlists.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            color = MonochromeSilver
                        )
                    }

                    items(playlists) { pl ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(MonochromeSurface)
                                .border(1.dp, MonochromeOutline, RoundedCornerShape(18.dp))
                                .clickable { viewModel.openPlaylistAsAlbum(pl) }
                                .padding(14.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MonochromeSurfaceContainer)
                                    .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(14.dp))
                            ) {
                                Icon(
                                    imageVector = if (pl.isCustomAlbum) Icons.Default.Album else Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = MonochromeWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = pl.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MonochromeWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MonochromeSurfaceContainer)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (pl.isCustomAlbum) "CUSTOM ALBUM" else "PLAYLIST",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MonochromeWhite
                                        )
                                    }
                                }
                                Text(
                                    text = "${pl.trackCount} tracks • ${pl.formattedDuration}",
                                    fontSize = 11.sp,
                                    color = MonochromeMuted
                                )
                                if (pl.description.isNotBlank()) {
                                    Text(
                                        text = pl.description,
                                        fontSize = 10.sp,
                                        color = MonochromeSilver,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.playPlaylist(pl) }) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MonochromeWhite)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = MonochromeBlack,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                // 4. HOME TAB
                else {
                    // Featured Now Playing Hero Card
                    val currentTrack = playerState.currentTrack
                    if (currentTrack != null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MonochromeSurface)
                                    .border(1.dp, MonochromeOutline, RoundedCornerShape(24.dp))
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
                                        showVinylGrooves = true,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "CURRENTLY SELECTED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.1.sp,
                                            color = MonochromeSilver
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = currentTrack.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MonochromeWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${currentTrack.artist} • ${currentTrack.album}",
                                                fontSize = 11.sp,
                                                color = MonochromeSilver,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MonochromeSurfaceContainer)
                                                    .border(0.5.dp, MonochromeOutlineVariant, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = currentTrack.qualityBadge,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MonochromeWhite
                                                )
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.togglePlayPause() }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(MonochromeWhite)
                                        ) {
                                            Icon(
                                                imageVector = if (playerState.isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                                contentDescription = "Play/Pause",
                                                tint = MonochromeBlack,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Online Albums Row
                    if (filteredAlbums.isNotEmpty()) {
                        item {
                            Text(
                                text = "FEATURED ALBUMS FROM SPOTIFY & YT MUSIC",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                color = MonochromeSilver
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(filteredAlbums) { album ->
                                    PixelAlbumCard(
                                        album = album,
                                        onClick = { viewModel.openAlbum(album) },
                                        onPlayClick = { viewModel.playAlbum(album) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "STREAMING SOUNDSCAPE (${tracks.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            color = MonochromeSilver
                        )
                    }

                    items(tracks) { track ->
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

        // Floating Pixel Mini Player Bar (Sits right above Bottom Navigation Bar)
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

        // Google Pixel Navigation Tabs Down (Bottom Navigation Bar)
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
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onSkipNextClick = { viewModel.skipNext() },
                onSkipPreviousClick = { viewModel.skipPrevious() },
                onSeek = { viewModel.seekToRatio(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                onCollapseClick = { viewModel.setExpandedPlayer(false) },
                onQueueClick = { viewModel.setQueueVisible(true) }
            )
        }

        // Queue Bottom Sheet
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
                onTrackClick = { track -> viewModel.playTrack(track) },
                onCloseClick = { viewModel.setQueueVisible(false) },
                onShuffleClick = { viewModel.toggleShuffle() }
            )
        }

        // Album Detail Sheet
        PixelAlbumDetailSheet(
            album = selectedAlbum,
            visible = isAlbumSheetVisible,
            onDismiss = { viewModel.closeAlbumSheet() },
            onPlayTrack = { track ->
                viewModel.playTrack(track)
            },
            onPlayAll = {
                selectedAlbum?.let { viewModel.playAlbum(it) }
            },
            onShuffleAll = {
                selectedAlbum?.let { viewModel.shuffleAlbum(it) }
            },
            onAddToPlaylistClick = { track ->
                viewModel.openAddToPlaylist(track)
            }
        )

        // Add To Playlist Sheet
        PixelAddToPlaylistSheet(
            track = trackForPlaylist,
            playlists = playlists,
            visible = isAddToPlaylistSheetVisible,
            onDismiss = { viewModel.closeAddToPlaylistSheet() },
            onSelectPlaylist = { pl ->
                trackForPlaylist?.let { tr -> viewModel.addTrackToPlaylist(pl, tr) }
            },
            onCreateNewClick = {
                viewModel.closeAddToPlaylistSheet()
                viewModel.openCreatePlaylistDialog()
            }
        )

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
