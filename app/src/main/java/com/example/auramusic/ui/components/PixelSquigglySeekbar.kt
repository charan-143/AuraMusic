package com.example.auramusic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.auramusic.theme.MonochromeLightGrey
import com.example.auramusic.theme.MonochromeMuted
import com.example.auramusic.theme.MonochromeOutline
import com.example.auramusic.theme.MonochromeWhite
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun PixelSquigglySeekbar(
    progress: Float, // 0f to 1f
    bufferedProgress: Float = 0f, // 0f to 1f (Travel pre-buffering)
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MonochromeWhite,
    bufferColor: Color = Color(0xFF4A4A4A),
    inactiveColor: Color = Color(0xFF222222)
) {
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val effectiveProgress = if (isDragging) dragProgress else progress.coerceIn(0f, 1f)

    // Smoothly animate wave amplitude between 0 (straight line when paused) and 6.5dp when playing
    val targetAmplitude = if (isPlaying && !isDragging) 6.5f else 0f
    val animatedAmplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(durationMillis = 400),
        label = "waveAmplitude"
    )

    // Infinite wave phase for continuous fluid ripple effect
    val infiniteTransition = rememberInfiniteTransition(label = "wavePhase")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing)
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(newProgress)
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeek(dragProgress)
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragProgress = newProgress
                    }
                )
            }
            .padding(vertical = 12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
            val width = size.width
            val centerY = size.height / 2f
            val playedWidth = width * effectiveProgress
            val bufferedWidth = (width * bufferedProgress.coerceIn(effectiveProgress, 1f))
            val strokeWidthPx = 4.dp.toPx()

            // 1. Draw base inactive track
            drawLine(
                color = inactiveColor,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )

            // 2. Draw travel offline pre-buffer line
            if (bufferedWidth > playedWidth) {
                drawLine(
                    color = bufferColor,
                    start = Offset(playedWidth, centerY),
                    end = Offset(bufferedWidth, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            // 3. Draw dynamic squiggly wave for played portion
            if (playedWidth > 0f) {
                val wavePath = Path()
                wavePath.moveTo(0f, centerY)

                val wavelengthPx = 28.dp.toPx()
                val stepPx = 3f
                var x = 0f

                while (x <= playedWidth) {
                    val angle = (2 * PI * (x / wavelengthPx) - wavePhase).toFloat()
                    val y = centerY + animatedAmplitude * sin(angle)
                    wavePath.lineTo(x, y)
                    x += stepPx
                }

                drawPath(
                    path = wavePath,
                    color = activeColor,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // 4. Draw Pixel thumb circle at current played position
                val thumbY = if (animatedAmplitude > 0.1f) {
                    val angle = (2 * PI * (playedWidth / wavelengthPx) - wavePhase).toFloat()
                    centerY + animatedAmplitude * sin(angle)
                } else {
                    centerY
                }

                drawCircle(
                    color = activeColor,
                    radius = if (isDragging) 8.dp.toPx() else 6.5.dp.toPx(),
                    center = Offset(playedWidth.coerceIn(0f, width), thumbY)
                )
            }
        }
    }
}
