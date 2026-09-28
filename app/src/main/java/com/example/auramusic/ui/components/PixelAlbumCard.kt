package com.example.auramusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.auramusic.model.Album
import com.example.auramusic.theme.MonochromeBlack
import com.example.auramusic.theme.MonochromeMuted
import com.example.auramusic.theme.MonochromeOutline
import com.example.auramusic.theme.MonochromeOutlineVariant
import com.example.auramusic.theme.MonochromeSilver
import com.example.auramusic.theme.MonochromeSurface
import com.example.auramusic.theme.MonochromeSurfaceContainer
import com.example.auramusic.theme.MonochromeWhite
import com.example.auramusic.theme.PixelMotion

@Composable
fun PixelAlbumCard(
    album: Album,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = PixelMotion.BouncySpring,
        label = "albumCardScale"
    )

    Box(
        modifier = modifier
            .width(170.dp)
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .background(MonochromeSurface)
            .border(1.dp, MonochromeOutline, RoundedCornerShape(28.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(12.dp)
    ) {
        Column {
            // Album Art with Source Badge & Quick Play Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(146.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MonochromeSurfaceContainer)
                    .border(0.5.dp, MonochromeOutlineVariant, RoundedCornerShape(20.dp))
            ) {
                AsyncImage(
                    model = album.coverArtUrl,
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )

                // Top Source Pill Badge
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MonochromeBlack.copy(alpha = 0.85f))
                        .border(0.5.dp, MonochromeOutlineVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = album.qualityBadge,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MonochromeWhite
                    )
                }

                // Quick Play FAB in Bottom Right
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.BottomEnd)
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MonochromeWhite)
                        .clickable { onPlayClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Album",
                        tint = MonochromeBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Album Title
            Text(
                text = album.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MonochromeWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Artist & Track Count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = album.artist,
                    fontSize = 11.sp,
                    color = MonochromeSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "• ${album.trackCount} tracks",
                    fontSize = 10.sp,
                    color = MonochromeMuted
                )
            }
        }
    }
}
