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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
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
fun PixelCreatePlaylistDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, isAlbum: Boolean) -> Unit
) {
    if (!visible) return

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isCustomAlbum by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MonochromeSurface)
                .border(1.dp, MonochromeOutline, RoundedCornerShape(28.dp))
                .padding(22.dp)
        ) {
            Column {
                Text(
                    text = if (isCustomAlbum) "Create Custom Album" else "Create New Playlist",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MonochromeWhite
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Organize Spotify, YouTube Music, and Lossless FLAC streams into custom collections.",
                    fontSize = 12.sp,
                    color = MonochromeSilver
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle between Playlist and Album
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Playlist Option
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (!isCustomAlbum) MonochromeWhite else MonochromeSurfaceContainer)
                            .border(1.dp, if (!isCustomAlbum) MonochromeWhite else MonochromeOutlineVariant, RoundedCornerShape(14.dp))
                            .clickable { isCustomAlbum = false }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "Playlist",
                                tint = if (!isCustomAlbum) MonochromeBlack else MonochromeSilver,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Playlist",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isCustomAlbum) MonochromeBlack else MonochromeSilver
                            )
                        }
                    }

                    // Album Option
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCustomAlbum) MonochromeWhite else MonochromeSurfaceContainer)
                            .border(1.dp, if (isCustomAlbum) MonochromeWhite else MonochromeOutlineVariant, RoundedCornerShape(14.dp))
                            .clickable { isCustomAlbum = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = "Album",
                                tint = if (isCustomAlbum) MonochromeBlack else MonochromeSilver,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Album",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCustomAlbum) MonochromeBlack else MonochromeSilver
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                Text(
                    text = "TITLE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    color = MonochromeSilver
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MonochromeSurfaceContainer)
                        .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (name.isEmpty()) {
                        Text(
                            text = if (isCustomAlbum) "e.g. Midnight Drive Vol. 1" else "e.g. Focus & Coding",
                            fontSize = 13.sp,
                            color = MonochromeMuted
                        )
                    }
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = TextStyle(
                            color = MonochromeWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(MonochromeWhite),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description Input
                Text(
                    text = "DESCRIPTION (OPTIONAL)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    color = MonochromeSilver
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MonochromeSurfaceContainer)
                        .border(1.dp, MonochromeOutlineVariant, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (description.isEmpty()) {
                        Text(
                            text = "Add optional notes or genre tags...",
                            fontSize = 13.sp,
                            color = MonochromeMuted
                        )
                    }
                    BasicTextField(
                        value = description,
                        onValueChange = { description = it },
                        textStyle = TextStyle(
                            color = MonochromeWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(MonochromeWhite),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MonochromeSilver,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MonochromeWhite)
                            .clickable {
                                if (name.isNotBlank()) {
                                    onCreate(name, description, isCustomAlbum)
                                    onDismiss()
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Create",
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
