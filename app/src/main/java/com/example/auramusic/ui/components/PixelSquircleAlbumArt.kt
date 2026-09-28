package com.example.auramusic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.auramusic.theme.MonochromeBlack
import com.example.auramusic.theme.MonochromeOutline
import com.example.auramusic.theme.MonochromeSurface
import com.example.auramusic.theme.MonochromeWhite
import kotlin.math.absoluteValue

// Vibrant, dynamic color palettes for colorful thumbnail generation & fallbacks
private val colorfulGradientPalettes = listOf(
    listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)), // Fiery Sunset Orange-Red
    listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)), // Neon Magenta & Amber
    listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)), // Bright Cyan & Emerald
    listOf(Color(0xFFFC466B), Color(0xFF3F5EFB)), // Cyberpunk Pink & Royal Blue
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)), // Vivid Mint & Teal
    listOf(Color(0xFF654EA3), Color(0xFFEAAFC8)), // Velvet Violet & Rose
    listOf(Color(0xFFF12711), Color(0xFFF5AF19)), // Golden Sunshine
    listOf(Color(0xFF4776E6), Color(0xFF8E54E9)), // Deep Indigo Glow
    listOf(Color(0xFF00B4DB), Color(0xFF0083B0)), // Electric Azure
    listOf(Color(0xFFFA709A), Color(0xFFFEE140))  // Mango Peach Neon
)

@Composable
fun rememberColorfulGradient(seed: String?): Brush {
    val index = (seed?.hashCode() ?: 0).absoluteValue % colorfulGradientPalettes.size
    val colors = colorfulGradientPalettes[index]
    return remember(seed) { Brush.linearGradient(colors) }
}

@Composable
fun PixelSquircleAlbumArt(
    coverArtUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    showVinylGrooves: Boolean = false,
    titleFallback: String? = null
) {
    val context = LocalContext.current
    val fallbackGradient = rememberColorfulGradient(coverArtUrl ?: titleFallback)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.White.copy(alpha = 0.05f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(MonochromeSurface)
            .border(1.dp, MonochromeOutline, RoundedCornerShape(cornerRadius))
    ) {
        if (!coverArtUrl.isNullOrBlank()) {
            val imageRequest = remember(coverArtUrl) {
                ImageRequest.Builder(context)
                    .data(coverArtUrl)
                    .crossfade(150)
                    .allowHardware(true)
                    .memoryCacheKey(coverArtUrl)
                    .diskCacheKey(coverArtUrl)
                    .build()
            }
            // Full color, high-definition thumbnail image without grayscale filters
            AsyncImage(
                model = imageRequest,
                contentDescription = "Song Thumbnail Artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Vibrant colorful gradient artwork for songs without remote art
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fallbackGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Music Artwork",
                    tint = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.size((cornerRadius.value * 1.3f).dp.coerceIn(20.dp, 56.dp))
                )
            }
        }

        // Concentric Vinyl Disc Sheen (ONLY rendered when showVinylGrooves is explicitly true)
        if (showVinylGrooves) {
            SpinningVinylSheen(isPlaying = isPlaying)

            // Minimalist discrete spindle center dot (only in vinyl mode)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MonochromeBlack.copy(alpha = 0.85f))
                    .border(1.dp, MonochromeWhite.copy(alpha = 0.5f), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MonochromeWhite)
                )
            }
        }
    }
}

@Composable
private fun SpinningVinylSheen(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinylSpin")
    val spinningRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .rotate(if (isPlaying) spinningRotation else 0f)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f

        // Subtle transparent outer vinyl grooves
        val grooveSteps = 4
        for (i in 1..grooveSteps) {
            val r = maxRadius * (0.45f + (i * 0.12f))
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = r,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Vinyl sheen sweep gradient reflection
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.06f),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = center
            ),
            radius = maxRadius,
            center = center
        )
    }
}
