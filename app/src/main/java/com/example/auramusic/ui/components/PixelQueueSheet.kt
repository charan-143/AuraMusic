package com.example.auramusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.model.Track
import com.example.auramusic.theme.MonochromeBlack
import com.example.auramusic.theme.MonochromeLightGrey
import com.example.auramusic.theme.MonochromeMuted
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import com.example.auramusic.theme.MonochromeOutline
import com.example.auramusic.theme.MonochromeSilver
import com.example.auramusic.theme.MonochromeSurface
import com.example.auramusic.theme.MonochromeSurfaceContainer
import com.example.auramusic.theme.MonochromeWhite
import com.example.auramusic.theme.PixelMotion

@Composable
fun PixelQueueSheet(
    queue: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onTrackClick: (Track) -> Unit,
    onCloseClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onClearQueue: () -> Unit = {},
    onRemoveTrack: (Track) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    var dragOffsetY by remember { mutableStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = PixelMotion.BouncySpring,
        label = "queueDragOffset"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .offset { IntOffset(0, animatedOffsetY.toInt()) }
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(MonochromeSurface)
            .border(1.dp, MonochromeOutline, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .navigationBarsPadding()
            .padding(top = 10.dp, bottom = 24.dp)
    ) {
        // Drag Pill Handle with vertical swipe gesture
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffsetY > 120f) {
                                onCloseClick()
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
                        onCloseClick()
                    }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Header: Queue title, track count, Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffsetY > 120f) {
                                onCloseClick()
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "UP NEXT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MonochromeSilver
                )
                Text(
                    text = "${queue.size} Tracks in Queue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MonochromeWhite
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Shuffle Queue Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onShuffleClick()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle Queue",
                        tint = MonochromeWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Clear Queue Button
                if (queue.size > 1) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onClearQueue()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Queue",
                            tint = MonochromeSilver,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Close Queue Button
                IconButton(onClick = onCloseClick) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Queue",
                        tint = MonochromeWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Queue list items
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(horizontal = 12.dp)
        ) {
            itemsIndexed(
                items = queue,
                key = { index, track -> "${track.id}_$index" }
            ) { index, track ->
                val isSelected = track.id == currentTrackId
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        PixelTrackTile(
                            track = track,
                            isSelected = isSelected,
                            isPlaying = isPlaying,
                            onClick = { onTrackClick(track) },
                            onMoreClick = { onRemoveTrack(track) }
                        )
                    }

                    // Direct remove button from queue
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onRemoveTrack(track)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove from Queue",
                            tint = MonochromeSilver,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
