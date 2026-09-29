package com.example.auramusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun PixelPlaybackSpeedSheet(
    currentSpeed: Float,
    isCrossfadeEnabled: Boolean,
    crossfadeDurationSec: Int,
    onSpeedSelected: (Float) -> Unit,
    onCrossfadeToggled: (Boolean) -> Unit,
    onCrossfadeDurationChanged: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val speedPresets = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val crossfadePresets = listOf(1, 2, 3, 5, 8)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(MonochromeSurface)
            .border(1.dp, MonochromeOutline, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Drag Handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MonochromeSilver.copy(alpha = 0.4f))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MonochromeSurfaceContainer)
                        .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MonochromeWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Audio & Playback Engine",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MonochromeWhite
                    )
                    Text(
                        text = "Pitch-preserved tempo & DJ transitions",
                        fontSize = 12.sp,
                        color = MonochromeSilver
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MonochromeSilver,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Playback Speed Section
        Text(
            text = "PLAYBACK SPEED (${currentSpeed}x)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MonochromeSilver,
            letterSpacing = 0.8.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Speed preset chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            speedPresets.forEach { speed ->
                val isSelected = kotlin.math.abs(currentSpeed - speed) < 0.05f
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1.0f,
                    animationSpec = PixelMotion.BouncySpring,
                    label = "speedChipScale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .scale(scale)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) MonochromeWhite else MonochromeSurfaceContainer)
                        .border(
                            1.dp,
                            if (isSelected) MonochromeWhite else MonochromeOutlineVariant,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onSpeedSelected(speed)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${speed}x",
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MonochromeBlack else MonochromeWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // DJ Crossfade Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DJ Seamless Crossfade",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MonochromeWhite
                )
                Text(
                    text = "Smooth volume fade between consecutive tracks",
                    fontSize = 12.sp,
                    color = MonochromeSilver
                )
            }

            Switch(
                checked = isCrossfadeEnabled,
                onCheckedChange = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onCrossfadeToggled(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MonochromeBlack,
                    checkedTrackColor = MonochromeWhite,
                    uncheckedThumbColor = MonochromeSilver,
                    uncheckedTrackColor = MonochromeSurfaceContainer
                )
            )
        }

        if (isCrossfadeEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                crossfadePresets.forEach { sec ->
                    val isSelected = crossfadeDurationSec == sec
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MonochromeWhite else MonochromeSurfaceContainer)
                            .border(
                                1.dp,
                                if (isSelected) MonochromeWhite else MonochromeOutlineVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onCrossfadeDurationChanged(sec)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${sec}s",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MonochromeBlack else MonochromeWhite
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Normalization & Audio Engine Info Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MonochromeSurfaceContainer)
                .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MonochromeSilver,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Dynamic Bitrate Normalization",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MonochromeWhite
                    )
                    Text(
                        text = "Volume level matching active • ReplayGain LUFS compliant",
                        fontSize = 11.sp,
                        color = MonochromeSilver
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
