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
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(val seedX: Float, val seedSpeed: Float, val seedSize: Float, val seedPhase: Float)

/**
 * Extremely subtle drifting dots behind Home content.
 * One Canvas, one animated float driving everything — cheap on a 3GB device.
 * Deliberately low particle count and low alpha: per spec, "almost
 * invisible behind content" — info always wins over decoration.
 */
@Composable
fun ParticleField(modifier: Modifier = Modifier, count: Int = 14) {
    val particles = remember {
        val rnd = Random(42)
        List(count) {
            Particle(
                seedX = rnd.nextFloat(),
                seedSpeed = 0.4f + rnd.nextFloat() * 0.6f,
                seedSize = 1.2f + rnd.nextFloat() * 2.2f,
                seedPhase = rnd.nextFloat() * 6.28f
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

            drawCircle(
                color = VoidColors.Accent.copy(alpha = 0.05f * edgeFade),
                radius = p.seedSize,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}
