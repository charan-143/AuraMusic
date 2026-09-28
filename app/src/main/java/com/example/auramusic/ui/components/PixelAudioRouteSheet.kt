package com.example.auramusic.ui.components

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.provider.Settings
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.theme.MonochromeBlack
import com.example.auramusic.theme.MonochromeMuted
import com.example.auramusic.theme.MonochromeOutline
import com.example.auramusic.theme.MonochromeOutlineVariant
import com.example.auramusic.theme.MonochromeSilver
import com.example.auramusic.theme.MonochromeSurface
import com.example.auramusic.theme.MonochromeSurfaceContainer
import com.example.auramusic.theme.MonochromeWhite

data class AudioRouteItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isLosslessCapable: Boolean
)

@Composable
fun PixelAudioRouteSheet(
    visible: Boolean,
    activeDeviceName: String,
    audioSessionId: Int,
    onDeviceSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onEqualizerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Dynamically detect connected audio outputs
    val detectedRoutes = remember(visible) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val routes = mutableListOf<AudioRouteItem>()

        // 1. Phone Speaker
        routes.add(
            AudioRouteItem(
                id = "speaker",
                title = "Phone Loudspeaker",
                subtitle = "Built-in Stereo Speakers • 48kHz High-Res",
                icon = Icons.Default.Speaker,
                isLosslessCapable = true
            )
        )

        // Query hardware devices
        var hasBt = false
        var hasWired = false
        try {
            val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray()
            for (dev in devices) {
                when (dev.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLE_HEADSET -> {
                        hasBt = true
                        val prodName = dev.productName.toString().ifBlank { "Bluetooth Audio" }
                        routes.add(
                            AudioRouteItem(
                                id = "bt_${dev.id}",
                                title = prodName,
                                subtitle = "Connected Bluetooth Device • LDAC / AAC High-Def",
                                icon = Icons.Default.Bluetooth,
                                isLosslessCapable = true
                            )
                        )
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_USB_DEVICE -> {
                        hasWired = true
                        routes.add(
                            AudioRouteItem(
                                id = "wired_${dev.id}",
                                title = dev.productName.toString().ifBlank { "USB-C Lossless DAC" },
                                subtitle = "Bit-Perfect Lossless Output • 192kHz / 24-bit",
                                icon = Icons.Default.Headphones,
                                isLosslessCapable = true
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        // Add standard Pixel Buds Pro option if no BT was detected
        if (!hasBt) {
            routes.add(
                AudioRouteItem(
                    id = "pixel_buds",
                    title = "Pixel Buds Pro",
                    subtitle = "Spatial Audio with Head Tracking • Lossless Opus",
                    icon = Icons.Default.Headphones,
                    isLosslessCapable = true
                )
            )
        }

        if (!hasWired) {
            routes.add(
                AudioRouteItem(
                    id = "usb_dac",
                    title = "USB-C Lossless DAC",
                    subtitle = "External Audiophile Converter • 24-bit / 96kHz FLAC",
                    icon = Icons.Default.Headphones,
                    isLosslessCapable = true
                )
            )
        }

        routes
    }

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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Drag pill
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(MonochromeMuted)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "AUDIO OUTPUT ROUTE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MonochromeSilver
                        )
                        Text(
                            text = "Media Output & Sound FX",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MonochromeWhite
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MonochromeWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Audio Routes list
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    detectedRoutes.forEach { route ->
                        val isSelected = activeDeviceName.contains(route.title, ignoreCase = true) ||
                                (route.id == "pixel_buds" && activeDeviceName.contains("Pixel", ignoreCase = true)) ||
                                (route.id == "speaker" && activeDeviceName.contains("Speaker", ignoreCase = true))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) MonochromeSurfaceContainer else MonochromeBlack)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MonochromeWhite else MonochromeOutlineVariant,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onDeviceSelected(route.title)
                                    Toast.makeText(context, "Switched to ${route.title}", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MonochromeWhite else MonochromeSurface)
                            ) {
                                Icon(
                                    imageVector = route.icon,
                                    contentDescription = route.title,
                                    tint = if (isSelected) MonochromeBlack else MonochromeSilver,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = route.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MonochromeWhite
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = route.subtitle,
                                    fontSize = 11.sp,
                                    color = MonochromeSilver
                                )
                            }

                            if (isSelected) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(MonochromeWhite)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = MonochromeBlack,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons: Equalizer / Sound settings & Bluetooth settings
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Equalizer & Spatial Sound Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MonochromeSurfaceContainer)
                            .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(14.dp))
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onEqualizerClick()
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Equalizer",
                                tint = MonochromeWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Equalizer FX",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MonochromeWhite
                            )
                        }
                    }

                    // System Bluetooth Settings Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MonochromeSurfaceContainer)
                            .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(14.dp))
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                try {
                                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                } catch (e: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                                }
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MonochromeWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Audio Settings",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MonochromeWhite
                            )
                        }
                    }
                }
            }
        }
    }
}
