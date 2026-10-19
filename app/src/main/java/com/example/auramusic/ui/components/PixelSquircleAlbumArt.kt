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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.auramusic.network.ArtworkResolver
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun PixelSquircleAlbumArt(
    coverArtUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    showVinylGrooves: Boolean = false,
    titleFallback: String? = null,
    artist: String? = null,
    audioUri: String? = null
) {
    val context = LocalContext.current
    val colors = com.example.auramusic.theme.PixelTheme.colors
    var imageUrl by remember(coverArtUrl, titleFallback, artist, audioUri) { mutableStateOf(coverArtUrl) }
    LaunchedEffect(coverArtUrl, titleFallback, artist, audioUri) {
        if (!titleFallback.isNullOrBlank() && !artist.isNullOrBlank()) {
            do {
                imageUrl = ArtworkResolver.resolve(context, titleFallback, artist, coverArtUrl, audioUri)
                if (imageUrl == null) delay(60_000)
            } while (isActive && imageUrl == null)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(cornerRadius))
            .background(colors.surfaceContainer)
    ) {
        if (!imageUrl.isNullOrBlank()) {
            val imageRequest = remember(imageUrl) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(150)
                    .allowHardware(true)
                    .memoryCacheKey(imageUrl)
                    .diskCacheKey(imageUrl)
                    .build()
            }
            // Full color, high-definition thumbnail image without grayscale filters
            AsyncImage(
                model = imageRequest,
                onError = { imageUrl = null },
                contentDescription = "Song Thumbnail Artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Vibrant colorful gradient artwork for songs without remote art
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.surfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Music Artwork",
                    tint = colors.textSecondary,
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
