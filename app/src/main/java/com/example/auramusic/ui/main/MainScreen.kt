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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auramusic.theme.MonochromeBlack
import com.example.auramusic.theme.MonochromeLightGrey
import com.example.auramusic.theme.MonochromeMuted
import com.example.auramusic.theme.MonochromeOutline
import com.example.auramusic.theme.MonochromeOutlineVariant
import com.example.auramusic.theme.MonochromeSilver
import com.example.auramusic.theme.MonochromeSurface
import com.example.auramusic.theme.MonochromeSurfaceContainer
import com.example.auramusic.theme.MonochromeWhite
import com.example.auramusic.theme.PixelMotion
import com.example.auramusic.ui.components.PixelAtAGlanceHeader
import com.example.auramusic.ui.components.PixelExpandedPlayer
import com.example.auramusic.ui.components.PixelFilterChips
import com.example.auramusic.ui.components.PixelMiniPlayer
import com.example.auramusic.ui.components.PixelQueueSheet
import com.example.auramusic.ui.components.PixelSquircleAlbumArt
import com.example.auramusic.ui.components.PixelTrackTile

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val tracks by viewModel.filteredTracks.collectAsStateWithLifecycle()
    val isExpandedPlayer by viewModel.isExpandedPlayer.collectAsStateWithLifecycle()
    val isQueueVisible by viewModel.isQueueVisible.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MonochromeBlack)
    ) {
        // Main Home Screen Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Google Pixel At-a-Glance Widget Header
            PixelAtAGlanceHeader(
                isPlaying = playerState.isPlaying,
                activeTrackTitle = playerState.currentTrack?.title
            )

            // 2. Pixel Styled Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
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
                                text = "Search tracks, artists, ambient sounds...",
                                color = MonochromeMuted,
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            textStyle = TextStyle(
                                color = MonochromeWhite,
                                fontSize = 14.sp,
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

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Category Filter Chips
            PixelFilterChips(
                categories = viewModel.categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.setCategory(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Hero Featured Track / Quick Play Card
            val currentTrack = playerState.currentTrack
            if (currentTrack != null && searchQuery.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MonochromeWhite
                            )
                            Text(
                                text = "${currentTrack.artist} • ${currentTrack.album}",
                                fontSize = 12.sp,
                                color = MonochromeSilver
                            )
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

            // 5. Track Library Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Soundscape Library (${tracks.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MonochromeWhite
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.loadDeviceTracks() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload Device Files",
                            tint = MonochromeSilver,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { viewModel.toggleShuffle() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playerState.isShuffle) MonochromeWhite else MonochromeMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 6. Track Library List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                items(tracks, key = { it.id }) { track ->
                    val isSelected = track.id == playerState.currentTrack?.id
                    PixelTrackTile(
                        track = track,
                        isSelected = isSelected,
                        isPlaying = playerState.isPlaying,
                        onClick = { viewModel.playTrack(track) },
                        onMoreClick = { viewModel.setQueueVisible(true) }
                    )
                }

                // Bottom padding space for floating mini player
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }

        // 7. Floating Pixel Mini Player
        if (playerState.currentTrack != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                PixelMiniPlayer(
                    track = playerState.currentTrack!!,
                    isPlaying = playerState.isPlaying,
                    progress = playerState.progress,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onSkipNextClick = { viewModel.skipNext() },
                    onExpandClick = { viewModel.setExpandedPlayer(true) }
                )
            }
        }

        // 8. Animated Google Pixel Expanded Player
        AnimatedVisibility(
            visible = isExpandedPlayer,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = PixelMotion.IntOffsetSheetSpring
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = PixelMotion.IntOffsetSheetSpring
            ) + fadeOut()
        ) {
            PixelExpandedPlayer(
                playerState = playerState,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onSkipNextClick = { viewModel.skipNext() },
                onSkipPreviousClick = { viewModel.skipPrevious() },
                onSeek = { ratio -> viewModel.seekToRatio(ratio) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
                onCollapseClick = { viewModel.setExpandedPlayer(false) },
                onQueueClick = { viewModel.setQueueVisible(true) }
            )
        }

        // 9. Queue Bottom Sheet
        AnimatedVisibility(
            visible = isQueueVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = PixelMotion.IntOffsetSheetSpring
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = PixelMotion.IntOffsetSheetSpring
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PixelQueueSheet(
                queue = playerState.queue,
                currentTrackId = playerState.currentTrack?.id,
                isPlaying = playerState.isPlaying,
                onTrackClick = { track ->
                    viewModel.playTrack(track)
                    viewModel.setQueueVisible(false)
                },
                onCloseClick = { viewModel.setQueueVisible(false) },
                onShuffleClick = { viewModel.toggleShuffle() }
            )
        }
    }
}
