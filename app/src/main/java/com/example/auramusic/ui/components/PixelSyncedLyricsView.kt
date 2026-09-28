package com.example.auramusic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.model.TrackLyrics
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

@Composable
fun PixelSyncedLyricsView(
    lyrics: TrackLyrics?,
    currentPositionMs: Long,
    onSeekToTimestamp: (Long) -> Unit,
    onSwitchToAlbumArt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    val lines = lyrics?.lines ?: emptyList()

    // Find the currently active line index based on current playback position
    val activeIndex = remember(currentPositionMs, lines) {
        val idx = lines.indexOfLast { it.timestampMs <= currentPositionMs }
        if (idx >= 0) idx else 0
    }

    // Auto-scroll to keep active line centered
    LaunchedEffect(activeIndex) {
        if (lines.isNotEmpty() && activeIndex in lines.indices) {
            val targetScroll = (activeIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    // Live pulsing dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "lyricsPulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(MonochromeBlack)
            .border(1.dp, MonochromeOutline, RoundedCornerShape(28.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar: Synced Badge, Source, and Album Art Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MonochromeSurfaceContainer)
                        .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MonochromeWhite.copy(alpha = dotAlpha))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE SYNCED LYRICS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = MonochromeWhite
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = lyrics?.source ?: "LRCLIB Sync",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MonochromeSilver,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    // Quick Toggle button to switch back to Album Artwork
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onSwitchToAlbumArt()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = "Show Album Art",
                            tint = MonochromeWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (lyrics == null || lyrics.isLoading) {
                // Loading State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MonochromeWhite,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Synchronizing lyrics from LRCLIB...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MonochromeSilver
                        )
                    }
                }
            } else if (lines.isEmpty()) {
                // Empty / Instrumental State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MonochromeSilver,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "✦ Instrumental Soundscape ✦",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MonochromeWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enjoy the high-fidelity acoustic performance",
                            fontSize = 12.sp,
                            color = MonochromeSilver
                        )
                    }
                }
            } else {
                // Synced Lyrics LazyColumn
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(lines) { index, line ->
                        val isCurrent = index == activeIndex
                        val isPast = index < activeIndex

                        val scale by animateFloatAsState(
                            targetValue = if (isCurrent) 1.04f else 1.0f,
                            animationSpec = PixelMotion.BouncySpring,
                            label = "lyricScale_$index"
                        )

                        val textColor = when {
                            isCurrent -> MonochromeWhite
                            isPast -> MonochromeSilver.copy(alpha = 0.85f)
                            else -> MonochromeMuted
                        }

                        val fontSize = if (isCurrent) 20.sp else 16.sp
                        val fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isCurrent) MonochromeSurfaceContainer.copy(alpha = 0.55f)
                                    else Color.Transparent
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    onSeekToTimestamp(line.timestampMs)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(18.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MonochromeWhite)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = line.text,
                                    fontSize = fontSize,
                                    fontWeight = fontWeight,
                                    color = textColor,
                                    lineHeight = 26.sp
                                )
                            }

                            // Timestamp indicator for past or active lines
                            if (isCurrent) {
                                Text(
                                    text = line.formattedTimestamp,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MonochromeSilver,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom gradient fade overlay for smooth aesthetic falloff
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(36.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MonochromeBlack)
                    )
                )
        )
    }
}
