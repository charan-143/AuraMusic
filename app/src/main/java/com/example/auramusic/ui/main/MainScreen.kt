package com.example.auramusic.ui.main

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Bluetooth
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
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speaker
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
import androidx.compose.runtime.remember
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
import com.example.auramusic.ui.components.PixelPlaybackSpeedSheet
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
        val currentTrackLyrics by viewModel.currentTrackLyrics.collectAsState()
        val isLyricsViewActive by viewModel.isLyricsViewActive.collectAsState()
        val isPlaybackSpeedSheetVisible by viewModel.isPlaybackSpeedSheetVisible.collectAsState()
        val downloadStates by viewModel.downloadStates.collectAsState()
        val libraryFilter by viewModel.libraryFilter.collectAsState()
        val favoriteTracks by viewModel.favoriteTracks.collectAsState()
        val downloadedTracks by viewModel.downloadedTracks.collectAsState()
        val mostPlayedTracks by viewModel.mostPlayedTracks.collectAsState()
        val folderTracks by viewModel.folderTracks.collectAsState()

        // System Back Navigation Handling across all sheets, dialogs, player, and tabs
        BackHandler(enabled = isPlaybackSpeedSheetVisible) {
            viewModel.closePlaybackSpeedSheet()
        }
        BackHandler(enabled = !isPlaybackSpeedSheetVisible && isCreatePlaylistDialogVisible) {
            viewModel.closeCreatePlaylistDialog()
        }
        BackHandler(enabled = !isPlaybackSpeedSheetVisible && !isCreatePlaylistDialogVisible && isAddToPlaylistSheetVisible) {
            viewModel.closeAddToPlaylistSheet()
        }
        BackHandler(enabled = !isCreatePlaylistDialogVisible && !isAddToPlaylistSheetVisible && isSleepTimerDialogVisible) {
            viewModel.closeSleepTimerDialog()
        }
        BackHandler(enabled = !isCreatePlaylistDialogVisible && !isAddToPlaylistSheetVisible && !isSleepTimerDialogVisible && isAlbumSheetVisible) {
            viewModel.closeAlbumSheet()
        }
        BackHandler(enabled = !isCreatePlaylistDialogVisible && !isAddToPlaylistSheetVisible && !isSleepTimerDialogVisible && !isAlbumSheetVisible && isEqualizerSheetVisible) {
            viewModel.closeEqualizerSheet()
        }
        BackHandler(enabled = !isCreatePlaylistDialogVisible && !isAddToPlaylistSheetVisible && !isSleepTimerDialogVisible && !isAlbumSheetVisible && !isEqualizerSheetVisible && isAudioRouteSheetVisible) {
            viewModel.closeAudioRouteSheet()
        }
        BackHandler(enabled = !isCreatePlaylistDialogVisible && !isAddToPlaylistSheetVisible && !isSleepTimerDialogVisible && !isAlbumSheetVisible && !isEqualizerSheetVisible && !isAudioRouteSheetVisible && isQueueVisible) {
            viewModel.setQueueVisible(false)
        }
        BackHandler(enabled = !isCreatePlaylistDialogVisible && !isAddToPlaylistSheetVisible && !isSleepTimerDialogVisible && !isAlbumSheetVisible && !isEqualizerSheetVisible && !isAudioRouteSheetVisible && !isQueueVisible && isExpandedPlayer && isLyricsViewActive) {
            viewModel.setLyricsView(false)
        }
        BackHandler(enabled = !isCreatePlaylistDialogVisible && !isAddToPlaylistSheetVisible && !isSleepTimerDialogVisible && !isAlbumSheetVisible && !isEqualizerSheetVisible && !isAudioRouteSheetVisible && !isQueueVisible && isExpandedPlayer && !isLyricsViewActive) {
            viewModel.setExpandedPlayer(false)
        }
        BackHandler(enabled = !isCreatePlaylistDialogVisible && !isAddToPlaylistSheetVisible && !isSleepTimerDialogVisible && !isAlbumSheetVisible && !isEqualizerSheetVisible && !isAudioRouteSheetVisible && !isQueueVisible && !isExpandedPlayer && currentTab != PixelNavTab.HOME) {
            viewModel.setNavTab(PixelNavTab.HOME)
        }

        Box(modifier = modifier.fillMaxSize().background(colors.background)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
                CalmMainContent(viewModel, Modifier.weight(1f))
                playerState.currentTrack?.let { track ->
                    PixelMiniPlayer(
                        track = track, isPlaying = playerState.isPlaying,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onSkipNextClick = { viewModel.skipNext() },
                        onExpandClick = { viewModel.setExpandedPlayer(true) }
                    )
                }
                PixelBottomNavBar(selectedTab = currentTab, onTabSelected = { viewModel.setNavTab(it) })
            }

            // Dimming Scrim for Expanded Player Bottom Attached Screen
            AnimatedVisibility(
                visible = isExpandedPlayer,
                enter = fadeIn(animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.setExpandedPlayer(false)
                        }
                )
            }

            // Google Pixel Bottom-Attached Expanded Player Screen
            AnimatedVisibility(
                visible = isExpandedPlayer,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = tween(180)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PixelExpandedPlayer(
                    playerState = playerState,
                    playbackProgressFlow = viewModel.playbackProgress,
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
                    onMoreOptionsClick = { playerState.currentTrack?.let { viewModel.openTrackActions(it) } },
                    onEqualizerClick = { viewModel.openEqualizerSheet() },
                    lyrics = currentTrackLyrics,
                    isLyricsActive = isLyricsViewActive,
                    onToggleLyrics = { viewModel.toggleLyricsView() },
                    onSeekToTimestamp = { viewModel.seekToPosition(it) },
                    isDownloaded = playerState.currentTrack?.let { viewModel.isTrackDownloaded(it.id) } ?: false,
                    downloadProgress = playerState.currentTrack?.let { t ->
                        val dState = downloadStates[t.id]
                        if (dState is com.example.auramusic.cache.DownloadState.Downloading) dState.progress else null
                    },
                    onDownloadClick = {
                        playerState.currentTrack?.let { t ->
                            if (downloadStates[t.id] is com.example.auramusic.cache.DownloadState.Downloading) {
                                viewModel.deleteDownloadedTrack(t.id)
                                Toast.makeText(context, "Download cancelled", Toast.LENGTH_SHORT).show()
                            } else if (viewModel.isTrackDownloaded(t.id)) {
                                viewModel.deleteDownloadedTrack(t.id)
                                Toast.makeText(context, "Removed from offline storage", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.downloadTrack(t)
                                Toast.makeText(context, "Downloading for offline listening...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    downloadError = playerState.currentTrack?.let { (downloadStates[it.id] as? com.example.auramusic.cache.DownloadState.Failed)?.error },
                    onOpenSpeedSheet = { viewModel.openPlaybackSpeedSheet() },
                    onAudioOutputClick = { viewModel.openAudioRouteSheet() }
                )
            }

            // Dimming Scrim for Up Next Queue Sheet
            AnimatedVisibility(
                visible = isQueueVisible,
                enter = fadeIn(animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.setQueueVisible(false)
                        }
                )
            }

            // Up Next Queue Bottom Attached Sheet
            AnimatedVisibility(
                visible = isQueueVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = tween(180)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PixelQueueSheet(
                    queue = playerState.queue,
                    currentTrackId = playerState.currentTrack?.id,
                    isPlaying = playerState.isPlaying,
                    onTrackClick = { viewModel.playTrack(it, playerState.queue) },
                    onCloseClick = { viewModel.setQueueVisible(false) },
                    onShuffleClick = { viewModel.toggleShuffle() },
                    onClearQueue = { viewModel.clearQueue() },
                    onRemoveTrack = { viewModel.removeFromQueue(it.id) },
                    onMove = { from, to -> viewModel.moveQueueItem(from, to) },
                    onMoreTrack = { viewModel.openTrackActions(it) },
                    canUndo = playerState.canUndoQueueRemoval,
                    onUndo = { viewModel.undoQueueRemoval() }
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

            // Equalizer Sheet Scrim
            AnimatedVisibility(
                visible = isEqualizerSheetVisible,
                enter = fadeIn(animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.closeEqualizerSheet()
                        }
                )
            }

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

            playerState.playbackError?.let { message ->
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { viewModel.dismissPlaybackError() },
                    title = { Text("Playback unavailable") },
                    text = { Text(message) },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { viewModel.retryPlayback() }) { Text("Retry") }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            viewModel.dismissPlaybackError()
                            viewModel.skipNext()
                        }) { Text("Skip") }
                    }
                )
            }

            com.example.auramusic.ui.components.TrackActions(viewModel)

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
                    onPlayTrack = { viewModel.playTrack(it, album.tracks.ifEmpty { listOf(it) }) },
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

            if (isPlaybackSpeedSheetVisible) {
                PixelPlaybackSpeedSheet(
                    currentSpeed = playerState.playbackSpeed,
                    isCrossfadeEnabled = playerState.isCrossfadeEnabled,
                    crossfadeDurationSec = playerState.crossfadeDurationSec,
                    onSpeedSelected = { viewModel.setPlaybackSpeed(it) },
                    onCrossfadeToggled = { viewModel.setCrossfade(it, playerState.crossfadeDurationSec) },
                    onCrossfadeDurationChanged = { viewModel.setCrossfade(playerState.isCrossfadeEnabled, it) },
                    onDismiss = { viewModel.closePlaybackSpeedSheet() }
                )
            }
        }
    }
}
