package com.example.auramusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.collectAsState
import com.example.auramusic.model.TrackLyrics
import com.example.auramusic.player.PlaybackProgress
import com.example.auramusic.player.PlayerState
import com.example.auramusic.theme.MonochromeBlack
import com.example.auramusic.theme.MonochromeLightGrey
import com.example.auramusic.theme.MonochromeMuted
import com.example.auramusic.theme.MonochromeOutline
import com.example.auramusic.theme.MonochromeOutlineVariant
import com.example.auramusic.theme.MonochromeSilver
import com.example.auramusic.theme.MonochromeSurface
import com.example.auramusic.theme.MonochromeSurfaceContainer
import com.example.auramusic.theme.MonochromeSurfaceHighest
import com.example.auramusic.theme.MonochromeWhite
import com.example.auramusic.theme.PixelMotion
import kotlinx.coroutines.flow.StateFlow

@Composable
fun PixelExpandedPlayer(
    playerState: PlayerState,
    playbackProgressFlow: StateFlow<PlaybackProgress>? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onPlayPauseClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onSkipPreviousClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onCollapseClick: () -> Unit,
    onQueueClick: () -> Unit,
    onMoreOptionsClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    lyrics: TrackLyrics? = null,
    isLyricsActive: Boolean = false,
    onToggleLyrics: () -> Unit = {},
    onSeekToTimestamp: (Long) -> Unit = {},
    isDownloaded: Boolean = false,
    downloadProgress: Float? = null,
    onDownloadClick: () -> Unit = {},
    onOpenSpeedSheet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val track = playerState.currentTrack ?: return

    var dragOffsetY by remember { mutableStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = PixelMotion.BouncySpring,
        label = "expandedPlayerDragOffset"
    )

    val playInteractionSource = remember { MutableInteractionSource() }
    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.88f else 1.0f,
        animationSpec = PixelMotion.BouncySpring,
        label = "heroPlayScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.93f)
            .offset { IntOffset(0, animatedOffsetY.toInt()) }
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(MonochromeSurface)
            .border(1.dp, MonochromeOutline, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Intentionally consume all clicks inside the player sheet so they do not fall through to the background dismiss scrim
            }
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Drag Pill Handle with touch gesture support
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffsetY > 250f) {
                                onCollapseClick()
                            }
                            dragOffsetY = 0f
                        },
                        onDragCancel = {
                            dragOffsetY = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MonochromeMuted)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onCollapseClick()
                    }
            )
        }

        // 1. Top Bar: Collapse Icon, Mode Title & Album, Equalizer, Options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCollapseClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = MonochromeWhite,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "PLAYING FROM ALBUM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MonochromeSilver
                )
                Text(
                    text = track.album,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MonochromeLightGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onToggleLyrics()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Synced Lyrics",
                        tint = if (isLyricsActive) MonochromeWhite else MonochromeSilver,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onEqualizerClick()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Equalizer",
                        tint = if (playerState.isEqualizerEnabled) MonochromeWhite else MonochromeSilver,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onMoreOptionsClick()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MonochromeWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.25f).defaultMinSize(minHeight = 8.dp))

        // 2. Hero Pixel Squircle Album Artwork OR Live Synced Lyrics View
        Box(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxWidth(if (isLyricsActive) 1.0f else 0.88f),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = isLyricsActive,
                animationSpec = tween(280),
                label = "lyricsViewCrossfade"
            ) { active ->
                if (active) {
                    PixelExpandedLyricsSection(
                        playbackProgressFlow = playbackProgressFlow,
                        fallbackPositionMs = playerState.currentPositionMs,
                        lyrics = lyrics,
                        onSeekToTimestamp = onSeekToTimestamp,
                        onSwitchToAlbumArt = onToggleLyrics,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onToggleLyrics()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PixelSquircleAlbumArt(
                            coverArtUrl = track.coverArtUrl,
                            isPlaying = playerState.isPlaying,
                            cornerRadius = 28.dp,
                            showVinylGrooves = false,
                            titleFallback = track.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.35f).defaultMinSize(minHeight = 14.dp))

        // 3. Track Details Row (Title, Artist, Badges, Heart Favorite)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = MonochromeWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = track.artist,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MonochromeSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MonochromeSurfaceHighest)
                            .border(1.dp, MonochromeOutline, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (playerState.isAutoQualityEnabled) playerState.activeStreamingQualityBadge else track.qualityBadge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MonochromeWhite,
                            letterSpacing = 0.6.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MonochromeSurfaceContainer)
                            .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(6.dp))
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onEqualizerClick()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = playerState.equalizerProfileBadge,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MonochromeWhite,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Download Action Button
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onDownloadClick()
                },
                modifier = Modifier.size(42.dp)
            ) {
                if (downloadProgress != null) {
                    androidx.compose.material3.CircularProgressIndicator(
                        progress = { downloadProgress },
                        strokeWidth = 2.dp,
                        color = MonochromeWhite,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                        contentDescription = if (isDownloaded) "Downloaded" else "Download Track",
                        tint = if (isDownloaded) MonochromeWhite else MonochromeSilver,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Favorite Button
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onToggleFavorite()
                },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) MonochromeWhite else MonochromeSilver,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Pixel Dynamic Squiggly Wavy Seekbar with Travel Buffer
        PixelExpandedSeekbarSection(
            playbackProgressFlow = playbackProgressFlow,
            fallbackPlayerState = playerState,
            isPlaying = playerState.isPlaying,
            onSeek = onSeek
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Main Pixel Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onToggleShuffle()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (playerState.isShuffle) MonochromeWhite else MonochromeMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Skip Previous
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onSkipPreviousClick()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = MonochromeWhite,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Hero Play/Pause Bouncy Squircle Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .scale(playScale)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MonochromeWhite)
                    .clickable(
                        interactionSource = playInteractionSource,
                        indication = null
                    ) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onPlayPauseClick()
                    }
            ) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                    tint = MonochromeBlack,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Skip Next
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onSkipNextClick()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    tint = MonochromeWhite,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Repeat Mode
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onToggleRepeat()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (playerState.isRepeatOne) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = if (playerState.isRepeatOne) MonochromeWhite else MonochromeMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 6. Bottom Auxiliary Bar: Speed pill, Audio device pill, Queue
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MonochromeSurfaceContainer)
                    .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(percent = 50))
                    .clickable {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onOpenSpeedSheet()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Playback Speed",
                    tint = MonochromeSilver,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${playerState.playbackSpeed}x",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MonochromeWhite
                )
            }

            // Synced Lyrics Pill Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (isLyricsActive) MonochromeWhite else MonochromeSurfaceContainer)
                    .border(1.dp, if (isLyricsActive) MonochromeWhite else MonochromeOutlineVariant, RoundedCornerShape(percent = 50))
                    .clickable {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onToggleLyrics()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Lyrics",
                    tint = if (isLyricsActive) MonochromeBlack else MonochromeSilver,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Lyrics",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLyricsActive) MonochromeBlack else MonochromeWhite
                )
            }

            // Queue List Button
            IconButton(
                onClick = onQueueClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = "Queue",
                    tint = MonochromeWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun PixelExpandedSeekbarSection(
    playbackProgressFlow: StateFlow<PlaybackProgress>?,
    fallbackPlayerState: PlayerState,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val progressState = playbackProgressFlow?.collectAsState()?.value
    val progress = progressState?.progress ?: fallbackPlayerState.progress
    val buffered = progressState?.bufferedProgress ?: fallbackPlayerState.bufferedProgress
    val currentFormatted = progressState?.formattedCurrentPosition ?: fallbackPlayerState.formattedCurrentPosition
    val durationFormatted = progressState?.formattedDuration ?: fallbackPlayerState.formattedDuration

    Column(modifier = modifier.fillMaxWidth()) {
        PixelSquigglySeekbar(
            progress = progress,
            bufferedProgress = buffered,
            isPlaying = isPlaying,
            onSeek = onSeek,
            activeColor = MonochromeWhite,
            inactiveColor = MonochromeOutline
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentFormatted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MonochromeSilver
            )
            Text(
                text = durationFormatted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MonochromeSilver
            )
        }
    }
}

@Composable
private fun PixelExpandedLyricsSection(
    playbackProgressFlow: StateFlow<PlaybackProgress>?,
    fallbackPositionMs: Long,
    lyrics: TrackLyrics?,
    onSeekToTimestamp: (Long) -> Unit,
    onSwitchToAlbumArt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPos = playbackProgressFlow?.collectAsState()?.value?.currentPositionMs ?: fallbackPositionMs
    PixelSyncedLyricsView(
        lyrics = lyrics,
        currentPositionMs = currentPos,
        onSeekToTimestamp = onSeekToTimestamp,
        onSwitchToAlbumArt = onSwitchToAlbumArt,
        modifier = modifier
    )
}
