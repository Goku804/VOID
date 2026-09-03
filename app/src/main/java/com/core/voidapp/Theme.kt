package com.core.voidapp

import androidx.compose.ui.graphics.Color

/**
 * VOID design system — exact palette from the architecture spec.
 * Do not introduce ad-hoc colors elsewhere; reference these.
 */
object VoidColors {
    val Background = Color(0xFF050505)
    val Surface = Color(0xFF0D0D0D)
    val Surface2 = Color(0xFF121212)
    val Border = Color(0xFF242424)
    val TextPrimary = Color(0xFFF2F2F2)
    val TextSecondary = Color(0xFF888888)

    // Semantic
    val Accent = Color(0xFF00E676)   // = Success. Green = completed / healthy / active
    val Success = Color(0xFF00E676)
    val Warning = Color(0xFFFFB300)  // Yellow = attention
    val Danger = Color(0xFFFF3D3D)   // Red = serious problem / overdue
    val Info = Color(0xFF2196F3)     // Blue = information
    val Analytics = Color(0xFF9C6CFF) // Purple = analytics/intelligence
}

/** Animation timing constants per spec — keep all transitions in this range. */
object VoidMotion {
    const val TRANSITION_MS = 200
    const val NAV_HIDE_MS = 200
}
