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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.auramusic.theme.MonochromeBlack
import com.example.auramusic.theme.MonochromeOutline
import com.example.auramusic.theme.MonochromeOutlineVariant
import com.example.auramusic.theme.MonochromeSilver
import com.example.auramusic.theme.MonochromeSurface
import com.example.auramusic.theme.MonochromeSurfaceHighest
import com.example.auramusic.theme.MonochromeWhite

@Composable
fun PixelSquircleAlbumArt(
    coverArtUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    showVinylGrooves: Boolean = true
) {
    // Rotation transition for spinning vinyl effect
    val infiniteTransition = rememberInfiniteTransition(label = "vinylSpin")
    val spinningRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // Smooth scale bounce when playing
    val scaleByPlay by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.96f,
        animationSpec = tween(durationMillis = 500),
        label = "scale"
    )

    // Monochrome grayscale color filter for imagery
    val monochromeColorMatrix = rememberMonochromeFilter()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .shadow(elevation = 20.dp, shape = RoundedCornerShape(cornerRadius), ambientColor = Color.White.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(cornerRadius))
            .background(MonochromeSurface)
            .border(1.5.dp, MonochromeOutline, RoundedCornerShape(cornerRadius))
    ) {
        if (!coverArtUrl.isNullOrBlank()) {
            AsyncImage(
                model = coverArtUrl,
                contentDescription = "Monochrome Album Artwork",
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(monochromeColorMatrix),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayerAlpha()
            )
        }

        // Concentric Vinyl Disc Etchings & Stylus Reflection
        if (showVinylGrooves) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(if (isPlaying) spinningRotation else 0f)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension / 2f

                // Draw subtle vinyl micro-grooves
                val grooveSteps = 5
                for (i in 1..grooveSteps) {
                    val r = maxRadius * (0.35f + (i * 0.11f))
                    drawCircle(
                        color = Color.White.copy(alpha = 0.04f),
                        radius = r,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Vinyl sheen gradient reflection
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    radius = maxRadius,
                    center = center
                )
            }
        }

        // Center Spindle Hole / Stylus Center Badge
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MonochromeBlack)
                .border(2.dp, MonochromeWhite.copy(alpha = 0.4f), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MonochromeWhite)
            )
        }
    }
}

private fun Modifier.graphicsLayerAlpha(): Modifier = this

private fun rememberMonochromeFilter(): ColorMatrix {
    val matrix = ColorMatrix()
    matrix.setToSaturation(0f) // Enforce pristine monochrome black & white
    return matrix
}
