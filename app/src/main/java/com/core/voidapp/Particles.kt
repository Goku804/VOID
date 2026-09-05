package com.core.voidapp

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val seedX: Float,
    val seedSpeed: Float,
    val seedSize: Float,
    val seedPhase: Float,
    val color: Color
)

/**
 * Bright drifting dots behind Home content — 22 particles cycling through
 * gold, green, white, and blue, moving bottom-to-top behind cards/boxes.
 */
@Composable
fun ParticleField(modifier: Modifier = Modifier, count: Int = 22) {
    val palette = listOf(
        Color(0xFFFFD700), // gold
        Color(0xFF00E676), // green
        Color(0xFFFFFFFF), // white
        Color(0xFF42A5F5)  // blue
    )

    val particles = remember {
        val rnd = Random(42)
        List(count) { i ->
            Particle(
                seedX = rnd.nextFloat(),
                seedSpeed = 0.4f + rnd.nextFloat() * 0.6f,
                seedSize = 3.5f + rnd.nextFloat() * 3.5f,
                seedPhase = rnd.nextFloat() * 6.28f,
                color = palette[i % palette.size]
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "particles")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleTime"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            // Vertical drift, wraps around; slight horizontal sway via sine.
            val yProgress = (time * p.seedSpeed + p.seedPhase) % 1f
            val y = h * (1f - yProgress)
            val sway = sin((time * 6.28f * p.seedSpeed) + p.seedPhase) * 10f
            val x = (p.seedX * w) + sway

            // Fade in/out near top and bottom edges instead of popping.
            val edgeFade = when {
                yProgress < 0.08f -> yProgress / 0.08f
                yProgress > 0.92f -> (1f - yProgress) / 0.08f
                else -> 1f
            }

            // Bright glow: soft outer halo plus a solid bright core.
            drawCircle(
                color = p.color.copy(alpha = 0.35f * edgeFade),
                radius = p.seedSize * 2.5f,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
            drawCircle(
                color = p.color.copy(alpha = 0.95f * edgeFade),
                radius = p.seedSize,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}
