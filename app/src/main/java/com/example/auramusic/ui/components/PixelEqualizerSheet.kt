package com.example.auramusic.ui.components

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.audio.AiAudioTarget
import com.example.auramusic.audio.EqualizerBand
import com.example.auramusic.audio.EqualizerPreset
import com.example.auramusic.audio.EqualizerState
import com.example.auramusic.theme.MonochromeBlack
import com.example.auramusic.theme.MonochromeDarkGrey
import com.example.auramusic.theme.MonochromeLightGrey
import com.example.auramusic.theme.MonochromeMuted
import com.example.auramusic.theme.MonochromeOutline
import com.example.auramusic.theme.MonochromeOutlineVariant
import com.example.auramusic.theme.MonochromeSilver
import com.example.auramusic.theme.MonochromeSurface
import com.example.auramusic.theme.MonochromeSurfaceContainer
import com.example.auramusic.theme.MonochromeSurfaceHighest
import com.example.auramusic.theme.MonochromeWhite

@Composable
fun PixelEqualizerSheet(
    visible: Boolean,
    equalizerState: EqualizerState,
    audioSessionId: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleAiMode: (Boolean) -> Unit,
    onSelectAiTarget: (AiAudioTarget) -> Unit,
    onBandLevelChange: (Int, Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onResetToFlat: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(MonochromeSurface)
                .border(1.dp, MonochromeOutline, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Drag Pill Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(MonochromeMuted)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Header: Title, Master Bypass Switch, Close
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MonochromeSurfaceContainer)
                                .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Equalizer",
                                tint = if (equalizerState.isEnabled) MonochromeWhite else MonochromeMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AUDIO EQUALIZER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = MonochromeSilver
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (equalizerState.isHardwareAttached) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MonochromeSurfaceHighest)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "HARDWARE DSP",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MonochromeWhite
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (equalizerState.isAiMode) "✦ AI Acoustic Intelligence" else "Precision 5-Band Graphic",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MonochromeWhite
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Master Bypass Toggle Pill
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (equalizerState.isEnabled) MonochromeWhite else MonochromeSurfaceContainer)
                                .border(1.dp, if (equalizerState.isEnabled) MonochromeWhite else MonochromeOutline, RoundedCornerShape(50))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleEnabled(!equalizerState.isEnabled)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = "Bypass",
                                    tint = if (equalizerState.isEnabled) MonochromeBlack else MonochromeSilver,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (equalizerState.isEnabled) "ACTIVE" else "BYPASS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (equalizerState.isEnabled) MonochromeBlack else MonochromeSilver
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Close Button
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            },
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
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Mode Selector Segmented Pills: [ ✦ AI Assisted ] | [ ☵ Manual Graphic ]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MonochromeSurfaceContainer)
                        .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(50))
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // AI Assisted Mode Pill
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (equalizerState.isAiMode) MonochromeWhite else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleAiMode(true)
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Mode",
                                    tint = if (equalizerState.isAiMode) MonochromeBlack else MonochromeSilver,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Assisted",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (equalizerState.isAiMode) MonochromeBlack else MonochromeSilver
                                )
                            }
                        }

                        // Manual Graphic Mode Pill
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (!equalizerState.isAiMode) MonochromeWhite else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleAiMode(false)
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Manual Graphic",
                                    tint = if (!equalizerState.isAiMode) MonochromeBlack else MonochromeSilver,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Manual Graphic",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!equalizerState.isAiMode) MonochromeBlack else MonochromeSilver
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Interactive Dynamic Bézier Spline Response Curve Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MonochromeBlack)
                        .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FREQUENCY RESPONSE CURVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                color = MonochromeMuted
                            )
                            Text(
                                text = if (equalizerState.isEnabled) "Range: ±12 dB" else "BYPASS PURE SOURCE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (equalizerState.isEnabled) MonochromeSilver else MonochromeMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        PixelEqualizerCurveCanvas(
                            bands = equalizerState.bands,
                            isEnabled = equalizerState.isEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (equalizerState.isAiMode) {
                    // ==========================================
                    // AI-ASSISTED VIEW
                    // ==========================================

                    // 1. AI Rationale & Acoustic Insights Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MonochromeSurfaceContainer)
                            .border(1.dp, MonochromeOutline, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI",
                                        tint = MonochromeWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ACOUSTIC INTELLIGENCE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.1.sp,
                                        color = MonochromeSilver
                                    )
                                }

                                // Profile Badge Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(MonochromeBlack)
                                        .border(1.dp, MonochromeOutline, RoundedCornerShape(50))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "✦ ${equalizerState.detectedProfileName.uppercase()}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MonochromeWhite
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = equalizerState.aiRationaleTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MonochromeWhite
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = equalizerState.aiRationaleDescription,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = MonochromeLightGrey
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Dynamic DSP Metrics Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricBadge(
                                    label = "BASS BOOST",
                                    value = "${equalizerState.bassBoostPercent}%",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricBadge(
                                    label = "3D SPATIAL",
                                    value = "${equalizerState.virtualizerPercent}%",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricBadge(
                                    label = "TUNING",
                                    value = "AUTO ABR",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2. AI Acoustic Target Styles Quick Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ACOUSTIC TARGET PROFILE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MonochromeSilver,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AiAudioTarget.values().forEach { target ->
                                val isSelected = equalizerState.activeAiTarget == target
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isSelected) MonochromeWhite else MonochromeSurfaceContainer)
                                        .border(
                                            1.dp,
                                            if (isSelected) MonochromeWhite else MonochromeOutlineVariant,
                                            RoundedCornerShape(50)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectAiTarget(target)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = target.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MonochromeBlack else MonochromeLightGrey
                                    )
                                }
                            }
                        }
                    }

                } else {
                    // ==========================================
                    // MANUAL GRAPHIC VIEW
                    // ==========================================

                    // 1. Preset Selector Carousel
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CURATED PRESETS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = MonochromeSilver
                            )

                            // Reset to Flat button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onResetToFlat()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset",
                                    tint = MonochromeSilver,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Reset Flat",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MonochromeSilver
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EqualizerPreset.values().forEach { preset ->
                                val isSelected = equalizerState.currentPreset == preset
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isSelected) MonochromeWhite else MonochromeSurfaceContainer)
                                        .border(
                                            1.dp,
                                            if (isSelected) MonochromeWhite else MonochromeOutlineVariant,
                                            RoundedCornerShape(50)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectPreset(preset)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = preset.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MonochromeBlack else MonochromeLightGrey
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2. 5 Vertical Band Sliders
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MonochromeSurfaceContainer)
                            .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            equalizerState.bands.forEach { band ->
                                PixelVerticalBandSlider(
                                    band = band,
                                    isEnabled = equalizerState.isEnabled,
                                    onLevelChange = { newMilliBels ->
                                        onBandLevelChange(band.index, newMilliBels)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 3. Hardware Bass Boost & 3D Spatial Virtualizer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Bass Boost Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MonochromeSurfaceContainer)
                                .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Waves,
                                            contentDescription = "Bass Boost",
                                            tint = MonochromeWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Hardware Bass Boost",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MonochromeWhite
                                        )
                                    }
                                    Text(
                                        text = "${equalizerState.bassBoostPercent}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MonochromeWhite
                                    )
                                }
                                Slider(
                                    value = equalizerState.bassBoostStrength.toFloat(),
                                    onValueChange = {
                                        onBassBoostChange(it.toInt())
                                    },
                                    valueRange = 0f..1000f,
                                    enabled = equalizerState.isEnabled,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MonochromeWhite,
                                        activeTrackColor = MonochromeWhite,
                                        inactiveTrackColor = MonochromeSurfaceHighest
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // 3D Spatial Virtualizer Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MonochromeSurfaceContainer)
                                .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.SurroundSound,
                                            contentDescription = "3D Spatial",
                                            tint = MonochromeWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "3D Spatial Virtualizer",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MonochromeWhite
                                        )
                                    }
                                    Text(
                                        text = "${equalizerState.virtualizerPercent}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MonochromeWhite
                                    )
                                }
                                Slider(
                                    value = equalizerState.virtualizerStrength.toFloat(),
                                    onValueChange = {
                                        onVirtualizerChange(it.toInt())
                                    },
                                    valueRange = 0f..1000f,
                                    enabled = equalizerState.isEnabled,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MonochromeWhite,
                                        activeTrackColor = MonochromeWhite,
                                        inactiveTrackColor = MonochromeSurfaceHighest
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action: Launch Motorola Dolby Atmos Panel
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MonochromeSurfaceContainer)
                        .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(16.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            try {
                                val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                                    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                                } catch (e2: Exception) {
                                    Toast.makeText(context, "System Equalizer Opened", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SpatialAudio,
                            contentDescription = "Dolby Atmos",
                            tint = MonochromeSilver,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Motorola Dolby Atmos Audio Effects Panel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MonochromeSilver
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dynamic Bézier Spline Response Curve Canvas connecting frequency band gains.
 */
@Composable
private fun PixelEqualizerCurveCanvas(
    bands: List<EqualizerBand>,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingX = 18f
        val usableWidth = width - (paddingX * 2)

        // Center 0dB horizontal reference line
        val centerY = height / 2f
        drawLine(
            color = MonochromeDarkGrey,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1f
        )

        if (bands.isEmpty()) return@Canvas

        // Calculate (x, y) coordinates for all bands
        val points = bands.mapIndexed { index, band ->
            val x = paddingX + (index.toFloat() / (bands.size - 1).coerceAtLeast(1)) * usableWidth
            // Level is in milliBels (-1200 to +1200)
            val level = if (isEnabled) band.levelMilliBels.coerceIn(-1200, 1200) else 0
            // Map -1200 (bottom) to +1200 (top)
            val normalized = (level + 1200) / 2400f // 0f to 1f
            val y = height * (1f - normalized)
            Offset(x, y)
        }

        // Build smooth cubic Bézier spline
        val path = Path()
        path.moveTo(points.first().x, points.first().y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val controlPointX1 = p0.x + (p1.x - p0.x) / 2f
            val controlPointX2 = p0.x + (p1.x - p0.x) / 2f
            path.cubicTo(
                controlPointX1, p0.y,
                controlPointX2, p1.y,
                p1.x, p1.y
            )
        }

        // Fill area under curve with gentle gradient
        val fillPath = Path().apply {
            addPath(path)
            lineTo(points.last().x, height)
            lineTo(points.first().x, height)
            close()
        }

        val gradient = Brush.verticalGradient(
            colors = listOf(
                if (isEnabled) MonochromeWhite.copy(alpha = 0.22f) else MonochromeMuted.copy(alpha = 0.08f),
                Color.Transparent
            ),
            startY = 0f,
            endY = height
        )
        drawPath(fillPath, brush = gradient)

        // Draw the spline stroke
        drawPath(
            path = path,
            color = if (isEnabled) MonochromeWhite else MonochromeMuted,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // Draw frequency nodes
        points.forEach { point ->
            // Outer subtle halo
            drawCircle(
                color = if (isEnabled) MonochromeWhite.copy(alpha = 0.25f) else Color.Transparent,
                radius = 6.dp.toPx(),
                center = point
            )
            // Solid node
            drawCircle(
                color = if (isEnabled) MonochromeWhite else MonochromeSilver,
                radius = 3.5.dp.toPx(),
                center = point
            )
        }
    }
}

/**
 * Vertical Squircle Slider for individual Equalizer Bands (-12dB to +12dB).
 */
@Composable
private fun PixelVerticalBandSlider(
    band: EqualizerBand,
    isEnabled: Boolean,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val sliderHeight = 110.dp
    val density = LocalDensity.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(52.dp)
    ) {
        // dB display badge above slider
        val dbValue = band.levelDb
        val dbText = if (dbValue > 0) "+%.1f".format(dbValue) else "%.1f".format(dbValue)
        Text(
            text = dbText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (dbValue != 0f && isEnabled) MonochromeWhite else MonochromeSilver
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Custom Touch-Draggable Vertical Track
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(28.dp)
                .height(sliderHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(MonochromeBlack)
                .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(14.dp))
                .pointerInput(band.index, isEnabled) {
                    if (!isEnabled) return@pointerInput
                    detectDragGestures { change, _ ->
                        change.consume()
                        val totalHeightPx = with(density) { sliderHeight.toPx() }
                        val clampedY = change.position.y.coerceIn(0f, totalHeightPx)
                        // y=0 is +1200mB, y=totalHeight is -1200mB
                        val fraction = 1f - (clampedY / totalHeightPx) // 0 to 1
                        val newMilliBels = ((fraction * 2400) - 1200).toInt()
                        // Zero snap near center
                        val finalMilliBels = if (kotlin.math.abs(newMilliBels) < 40) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            0
                        } else {
                            (newMilliBels / 50) * 50
                        }
                        onLevelChange(finalMilliBels)
                    }
                }
        ) {
            val totalHeightPx = with(density) { sliderHeight.toPx() }
            val normalized = (band.levelMilliBels + 1200) / 2400f
            val thumbY = (1f - normalized) * totalHeightPx

            // Center zero-dB indicator line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MonochromeMuted)
            )

            // Draggable Thumb
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, (thumbY - with(density) { 10.dp.toPx() }).toInt().coerceIn(0, (totalHeightPx - with(density) { 20.dp.toPx() }).toInt())) }
                    .size(width = 22.dp, height = 18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isEnabled) MonochromeWhite else MonochromeMuted)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Band Center Frequency Label (e.g., 60Hz, 3.6kHz)
        Text(
            text = band.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MonochromeSilver
        )
    }
}

@Composable
private fun MetricBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MonochromeBlack)
            .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MonochromeSilver
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MonochromeWhite
            )
        }
    }
}
