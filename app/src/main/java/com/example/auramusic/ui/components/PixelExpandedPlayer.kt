package com.example.auramusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun PixelExpandedPlayer(
    playerState: PlayerState,
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
    onAudioRouteClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    activeAudioDevice: String = "Pixel Buds Pro",
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val track = playerState.currentTrack ?: return

    val playInteractionSource = remember { MutableInteractionSource() }
    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.88f else 1.0f,
        animationSpec = PixelMotion.BouncySpring,
        label = "heroPlayScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MonochromeBlack)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Top Bar: Collapse Icon, Mode Title, Options
        Row(
            modifier = Modifier.fillMaxWidth(),
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

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Hero Pixel Squircle Album Artwork with Spinning Vinyl Grooves
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .weight(1f, fill = false),
            contentAlignment = Alignment.Center
        ) {
            PixelSquircleAlbumArt(
                coverArtUrl = track.coverArtUrl,
                isPlaying = playerState.isPlaying,
                cornerRadius = 32.dp,
                showVinylGrooves = false,
                titleFallback = track.title,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Track Details Row (Title, Artist, Heart Favorite)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MonochromeWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.artist,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = MonochromeSilver,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
                }
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onToggleFavorite()
                }
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) MonochromeWhite else MonochromeSilver,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Pixel Dynamic Squiggly Wavy Seekbar with Travel Buffer
        PixelSquigglySeekbar(
            progress = playerState.progress,
            bufferedProgress = playerState.bufferedProgress,
            isPlaying = playerState.isPlaying,
            onSeek = onSeek,
            activeColor = MonochromeWhite,
            inactiveColor = MonochromeOutline
        )

        // Time labels: Current and Duration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = playerState.formattedCurrentPosition,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MonochromeSilver
            )
            Text(
                text = playerState.formattedDuration,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MonochromeSilver
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                }
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
                    .size(68.dp)
                    .scale(playScale)
                    .clip(RoundedCornerShape(22.dp))
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
                    modifier = Modifier.size(34.dp)
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
                }
            ) {
                Icon(
                    imageVector = if (playerState.isRepeatOne) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = if (playerState.isRepeatOne) MonochromeWhite else MonochromeMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                        val nextSpeed = when (playerState.playbackSpeed) {
                            1.0f -> 1.5f
                            1.5f -> 2.0f
                            else -> 1.0f
                        }
                        onSpeedChange(nextSpeed)
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
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

            // Audio Device Indicator Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MonochromeSurfaceContainer)
                    .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(percent = 50))
                    .clickable {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onAudioRouteClick()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = "Audio Device",
                    tint = MonochromeSilver,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = activeAudioDevice,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MonochromeSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
    }
}
