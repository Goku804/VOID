package com.core.voidapp.widgets.orb

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.core.voidapp.VoidColors
import com.core.voidapp.data.guardian.GuardianOrbState

/**
 * The one orb VOID has — this same visual now doubles as Guardian's
 * presence, per spec section 22 ("Guardian uses the existing floating
 * circular orb"). Its look is entirely driven by [orbState]; when
 * Guardian has no active commitment it simply sits at IDLE, which is the
 * plain ambient "VOID AI" look this started as.
 *
 * Tap/long-press are still deliberately no-ops — VOID's actual AI duty
 * for the orb isn't specified yet — but they're wired all the way through
 * so turning them on later is a one-line change.
 */
@Composable
fun OrbRoot(
    sizeDp: Dp,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    orbState: GuardianOrbState = GuardianOrbState.IDLE
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
        AnimatedOrbCanvas(state = orbState, modifier = Modifier.size(sizeDp))
    }
}

/**
 * Per-state animation character. Rather than one fixed loop, every
 * Guardian state gets its own tempo and color pairing drawn from VOID's
 * semantic legend (section 22) — energy that visibly speeds up under
 * warning/urgent, and visibly calms under break/idle, rather than a
 * single animation with a color swapped in.
 */
private data class OrbTheme(
    val primary: Color,
    val secondary: Color,
    val ringMs: Int,
    val counterRingMs: Int,
    val pulseMs: Int,
    val pingMs: Int,
    val glowAlpha: Float
)

private fun themeFor(state: GuardianOrbState): OrbTheme = when (state) {
    GuardianOrbState.IDLE -> OrbTheme(VoidColors.Cyan, VoidColors.Info, 9000, 13000, 3200, 4200, 0.16f)
    GuardianOrbState.LISTENING -> OrbTheme(VoidColors.Purple, VoidColors.Cyan, 3600, 5200, 1300, 1800, 0.24f)
    GuardianOrbState.THINKING -> OrbTheme(VoidColors.Purple, VoidColors.Cyan, 1900, 2800, 850, 1400, 0.26f)
    GuardianOrbState.SPEAKING -> OrbTheme(VoidColors.Cyan, VoidColors.Purple, 3200, 4600, 640, 1000, 0.30f)
    GuardianOrbState.FOCUS -> OrbTheme(VoidColors.Purple, VoidColors.Info, 6200, 9000, 2400, 3400, 0.20f)
    GuardianOrbState.WARNING -> OrbTheme(VoidColors.Warning, VoidColors.Danger, 2400, 3400, 950, 1300, 0.28f)
    GuardianOrbState.URGENT -> OrbTheme(VoidColors.Danger, VoidColors.Warning, 1300, 1900, 480, 700, 0.36f)
    GuardianOrbState.BREAK -> OrbTheme(VoidColors.Cyan, VoidColors.Info, 11000, 15000, 3600, 4800, 0.14f)
    GuardianOrbState.COMPLETED -> OrbTheme(VoidColors.Success, VoidColors.Cyan, 5200, 7400, 1100, 1200, 0.26f)
    GuardianOrbState.ERROR -> OrbTheme(VoidColors.Danger, VoidColors.Danger, 2600, 3600, 700, 1000, 0.22f)
}

@Composable
private fun AnimatedOrbCanvas(state: GuardianOrbState, modifier: Modifier = Modifier) {
    val theme = themeFor(state)

    // Colors ease between states over 300ms rather than cutting — this is
    // the "smoothly changes color" requirement, and it's what keeps state
    // changes reading as one continuous intelligence rather than a skin swap.
    val primary by animateColorAsState(theme.primary, tween(300), label = "orb_primary")
    val secondary by animateColorAsState(theme.secondary, tween(300), label = "orb_secondary")

    val infinite = rememberInfiniteTransition(label = "void_orb")

    // Main comet-trail ring: a sweep gradient (solid head, fading tail)
    // rotating continuously — this alone reads far more like "energy
    // travelling around a circuit" than a flat-color arc ever does.
    val ringRotation by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(theme.ringMs, easing = LinearEasing)),
        label = "ring_rotation"
    )
    // A second, thinner ring counter-rotating at a different tempo for depth —
    // two independent layers moving against each other is what sells "alive."
    val counterRotation by infinite.animateFloat(
        360f, 0f,
        infiniteRepeatable(tween(theme.counterRingMs, easing = LinearEasing)),
        label = "counter_rotation"
    )
    // Slow breathing glow behind everything.
    val pulse by infinite.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(theme.pulseMs, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "orb_pulse"
    )
    // A soft ring that continuously expands from the core and fades out —
    // a sonar "ping" that reads as active presence, faster/tighter under
    // urgency and slow/rare at idle.
    val pingProgress by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(theme.pingMs, easing = LinearEasing)),
        label = "orb_ping"
    )

    Canvas(modifier = modifier) {
        val maxRadius = size.minDimension / 2f
        val coreRadius = maxRadius * 0.62f
        val ringRadius = maxRadius * 0.82f
        val counterRingRadius = maxRadius * 0.94f
        val strokeWidth = maxRadius * 0.11f

        // --- outer soft glow (radial gradient, no blur needed => cheap + works on every API level) ---
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = theme.glowAlpha * pulse), Color.Transparent),
                center = center,
                radius = maxRadius * 1.05f
            ),
            radius = maxRadius * 1.05f,
            center = center
        )

        // --- sonar ping: expands from the core edge outward, fading as it grows ---
        val pingRadius = coreRadius + (maxRadius - coreRadius) * pingProgress
        drawCircle(
            color = primary.copy(alpha = (1f - pingProgress) * 0.35f),
            radius = pingRadius,
            center = center,
            style = Stroke(width = strokeWidth * 0.35f)
        )

        // --- dark mysterious core, with the faintest inner tint for depth ---
        drawCircle(color = Color(0xFF050505), radius = coreRadius, center = center)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.10f), Color.Transparent),
                center = center,
                radius = coreRadius
            ),
            radius = coreRadius,
            center = center
        )

        // --- primary comet-trail ring ---
        rotate(degrees = ringRotation) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        primary.copy(alpha = 0.05f),
                        primary.copy(alpha = 0.55f),
                        primary,
                        Color.Transparent
                    ),
                    center = center
                ),
                radius = ringRadius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // --- secondary, thinner counter-rotating ring for depth ---
        rotate(degrees = counterRotation) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        secondary.copy(alpha = 0.45f),
                        secondary,
                        Color.Transparent,
                        Color.Transparent
                    ),
                    center = center
                ),
                radius = counterRingRadius,
                center = center,
                style = Stroke(width = strokeWidth * 0.45f, cap = StrokeCap.Round)
            )
        }

        // --- crisp bright rim right at the core's edge, for sharpness/definition ---
        drawCircle(
            color = primary.copy(alpha = 0.55f),
            radius = coreRadius,
            center = center,
            style = Stroke(width = strokeWidth * 0.12f)
        )
    }
}

