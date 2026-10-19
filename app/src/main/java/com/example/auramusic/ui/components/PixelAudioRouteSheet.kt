package com.example.auramusic.ui.components

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import com.example.auramusic.theme.MonochromeWhite

@OptIn(ExperimentalMaterial3Api::class)
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

    if (visible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MonochromeSurface,
            contentColor = MonochromeWhite,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MonochromeMuted)
                )
            },
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MonochromeSurfaceContainer)
                            .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speaker,
                            contentDescription = null,
                            tint = MonochromeWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Audio Output Route",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MonochromeWhite
                        )
                        Text(
                            text = "Active: ${activeDeviceName.ifBlank { "Phone Speaker" }}",
                            fontSize = 12.sp,
                            color = MonochromeSilver
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Option 1: Open System Sound Settings
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MonochromeSurfaceContainer)
                        .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(16.dp))
                        .clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            try {
                                context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                            } catch (e: Exception) {
                                // fallback
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speaker,
                            contentDescription = null,
                            tint = MonochromeWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "System Sound Settings",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MonochromeWhite
                            )
                            Text(
                                text = "Manage media volumes & system output devices",
                                fontSize = 11.sp,
                                color = MonochromeSilver
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: Connect Bluetooth Device
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MonochromeSurfaceContainer)
                        .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(16.dp))
                        .clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            try {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                            } catch (e: Exception) {
                                // fallback
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = MonochromeWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bluetooth Audio Devices",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MonochromeWhite
                            )
                            Text(
                                text = "Pair or switch headphones, earbuds & speakers",
                                fontSize = 11.sp,
                                color = MonochromeSilver
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option 3: Equalizer & Sound Tuning
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MonochromeSurfaceContainer)
                        .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(16.dp))
                        .clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onDismiss()
                            onEqualizerClick()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MonochromeWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Audio Equalizer & Sound Engine",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MonochromeWhite
                            )
                            Text(
                                text = "Tune 5-band faders, bass boost & spatial virtualizer",
                                fontSize = 11.sp,
                                color = MonochromeSilver
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
