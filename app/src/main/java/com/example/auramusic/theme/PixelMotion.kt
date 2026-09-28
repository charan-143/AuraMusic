package com.example.auramusic.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object PixelMotion {
    // Tactile bouncy spring for interactive buttons (Play/Pause, Hearts, Chips)
    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Smooth physics spring for sliding sheets, player transitions, and drawer panels
    val SheetSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val IntOffsetSheetSpring = spring<androidx.compose.ui.unit.IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // Quick responsive spring for icon micro-interactions
    val SnappySpring = spring<Float>(
        dampingRatio = 0.65f,
        stiffness = Spring.StiffnessMedium
    )

    // Smooth cubic bezier easing for long morph transitions
    val PixelEasing = tween<Float>(
        durationMillis = 350,
        easing = FastOutSlowInEasing
    )
}

object PixelShapes {
    // Google Pixel Squircle Card geometry
    val SquircleCard = RoundedCornerShape(28.dp)
    val SquircleLarge = RoundedCornerShape(32.dp)
    val SquircleMedium = RoundedCornerShape(20.dp)
    val SquircleSmall = RoundedCornerShape(14.dp)
    val PillShape = RoundedCornerShape(percent = 50)
    val BottomSheetShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
}
