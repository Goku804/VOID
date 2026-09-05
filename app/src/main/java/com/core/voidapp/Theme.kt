package com.core.voidapp

import androidx.compose.ui.graphics.Color

/**
 * VOID design system — exact palette from the architecture spec.
 * Do not introduce ad-hoc colors elsewhere; reference these.
 */
/**
 * VOID design system — multi-color semantic palette.
 * Colors carry meaning, not decoration: green=success/completed,
 * cyan=interaction/navigation, purple=intelligence/analytics,
 * amber=attention, red=urgent/destructive, blue=informational.
 * Do not introduce ad-hoc colors elsewhere; reference these.
 */
object VoidColors {
    val Background = Color(0xFF050505)
    val Surface = Color(0xFF0D0D0D)
    val Surface2 = Color(0xFF121212)
    val Border = Color(0xFF242424)
    val TextPrimary = Color(0xFFF2F2F2)
    val TextSecondary = Color(0xFF888888)

    val Success = Color(0xFF00E676)   // completed / healthy / active
    val Info = Color(0xFF42A5F5)      // informational / timetable / neutral academic
    val Cyan = Color(0xFF00CFE8)      // interaction / navigation / primary buttons / AI actions
    val Purple = Color(0xFF9C6CFF)    // intelligence / analytics / reports / exam prep
    val Warning = Color(0xFFFFB300)   // attention / conflict / weak area / approaching exam
    val Danger = Color(0xFFFF3D4D)    // urgent / overdue / critical / destructive

    // Back-compat aliases — old screens used these names for the same meaning.
    val Accent = Success
    val Analytics = Purple
    val NavAccent = Cyan
}

/** Animation timing constants per spec — keep all transitions in this range. */
object VoidMotion {
    const val TRANSITION_MS = 200
    const val NAV_HIDE_MS = 200
}
