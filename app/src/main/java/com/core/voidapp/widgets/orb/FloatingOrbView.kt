package com.core.voidapp.widgets.orb

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp

/**
 * Deliberately UI-only, per spec: no AI behavior wired to tap/long-press
 * yet — VOID will define the orb's actual duty later. Tap and long-press
 * are still routed all the way out to the caller so that wiring is a
 * one-line change when the time comes; today both are effectively no-ops.
 */
@Composable
fun OrbRoot(
    sizeDp: Dp,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(sizeDp)
            // Two independent gesture detectors on the same node: a real
            // drag consumes the touch past Compose's slop threshold, so a
            // plain tap never gets far enough to be seen as a drag and
            // still reaches detectTapGestures below.
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, amount -> change.consume(); onDrag(amount.x, amount.y) },
                    onDragEnd = { onDragEnd() }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() })
            }
    ) {
        AnimatedOrbCanvas(modifier = Modifier.size(sizeDp))
    }
}

@Composable
private fun AnimatedOrbCanvas(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "void_orb")

    // One continuous, restrained rotation drives all three arcs — they
    // never flash or jump, only smoothly travel around the perimeter,
    // each holding a fixed angular offset from the others.
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "orb_rotation"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "orb_pulse"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.10f
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        // Dark mysterious core + a very soft purple ambient glow behind the arcs.
        drawCircle(color = Color(0xFF050505), radius = diameter / 2f * 0.76f, center = center)
        drawCircle(color = Color(0xFF9C6CFF).copy(alpha = 0.12f * pulse), radius = diameter / 2f * 0.94f, center = center)

        rotate(degrees = rotation) {
            drawArc(
                color = Color(0xFF00E676).copy(alpha = 0.9f),
                startAngle = 0f, sweepAngle = 95f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        rotate(degrees = rotation + 128f) {
            drawArc(
                color = Color(0xFFFF3D4D).copy(alpha = 0.9f),
                startAngle = 0f, sweepAngle = 85f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        rotate(degrees = rotation + 244f) {
            drawArc(
                color = Color(0xFF42A5F5).copy(alpha = 0.9f),
                startAngle = 0f, sweepAngle = 78f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round)
            )
        }
    }
}
