package com.core.voidapp

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

/**
 * Standard VOID card. Deliberately plain — 1px border, subtle surface,
 * no glow/pulse/HUD brackets. Per spec: dark/futuristic/serious/minimal,
 * avoid excessive "hacker UI" decoration.
 */
@Composable
fun VoidCard(
    modifier: Modifier = Modifier,
    borderColor: Color = VoidColors.Border,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VoidColors.Surface)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
fun VoidSectionLabel(text: String) {
    Text(
        text = text,
        color = VoidColors.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
    )
}

/**
 * Glowing variant of VoidCard — used sparingly, for panels that genuinely
 * need visual priority (e.g. next-exam countdown). Cheap: 2 stacked
 * translucent borders + a gentle pulse, no real blur.
 */
@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    glowColor: Color = VoidColors.Accent,
    content: @Composable ColumnScope.() -> Unit
) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "cardGlow")
    val pulse by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1600, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "cardGlowPulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VoidColors.Surface)
            .border(1.dp, glowColor.copy(alpha = 0.10f * pulse), RoundedCornerShape(18.dp))
            .border(1.dp, glowColor.copy(alpha = 0.8f * pulse), RoundedCornerShape(14.dp))
            .padding(14.dp),
        content = content
    )
}

/** Small colored status dot — green/yellow/red/blue meaning, not decoration. */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

/**
 * Exam countdown urgency color, per spec: >20 days neutral/blue, 20-16
 * amber, below 16 red. Today/Started/Completed get their own treatment.
 */
fun examCountdownColor(status: com.core.voidapp.data.ExamSittingStatus, daysRemaining: Long): Color = when (status) {
    com.core.voidapp.data.ExamSittingStatus.COMPLETED -> VoidColors.TextSecondary
    com.core.voidapp.data.ExamSittingStatus.STARTED,
    com.core.voidapp.data.ExamSittingStatus.TODAY -> VoidColors.Danger
    com.core.voidapp.data.ExamSittingStatus.UPCOMING -> when {
        daysRemaining > 20 -> VoidColors.Info
        daysRemaining >= 16 -> VoidColors.Warning
        else -> VoidColors.Danger
    }
}
