package com.example.auramusic.ui.components

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.auramusic.theme.MonochromeBlack
import com.example.auramusic.theme.MonochromeMuted
import com.example.auramusic.theme.MonochromeOutline
import com.example.auramusic.theme.MonochromeOutlineVariant
import com.example.auramusic.theme.MonochromeSilver
import com.example.auramusic.theme.MonochromeSurface
import com.example.auramusic.theme.MonochromeSurfaceContainer
import com.example.auramusic.theme.MonochromeWhite

@Composable
fun PixelSleepTimerDialog(
    visible: Boolean,
    remainingSeconds: Int?,
    onSetTimer: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val timerDurations = listOf(
        15 to "15 Minutes",
        30 to "30 Minutes",
        45 to "45 Minutes",
        60 to "1 Hour"
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MonochromeSurface)
                .border(1.dp, MonochromeOutline, RoundedCornerShape(28.dp))
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MonochromeSurfaceContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep Timer",
                                tint = MonochromeWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sleep Timer",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MonochromeWhite
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MonochromeSilver,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Automatically pause lossless audio stream when timer ends.",
                    fontSize = 12.sp,
                    color = MonochromeSilver
                )

                if (remainingSeconds != null && remainingSeconds > 0) {
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MonochromeSurfaceContainer)
                            .border(1.dp, MonochromeOutline, RoundedCornerShape(14.dp))
                            .padding(vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Active Timer",
                                tint = MonochromeWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active: %d:%02d remaining".format(minutes, seconds),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MonochromeWhite
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Options
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    timerDurations.forEach { (mins, label) ->
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MonochromeBlack)
                                .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(14.dp))
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onSetTimer(mins)
                                    Toast.makeText(context, "Sleep timer set for $mins min", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MonochromeWhite
                            )
                        }
                    }

                    if (remainingSeconds != null && remainingSeconds > 0) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MonochromeWhite)
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onSetTimer(0)
                                    Toast.makeText(context, "Sleep timer turned off", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Turn Off Timer",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MonochromeBlack
                            )
                        }
                    }
                }
            }
        }
    }
}
