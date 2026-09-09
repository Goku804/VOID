package com.core.voidapp.widgets.glance

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import com.core.voidapp.R

/**
 * Which of VOID's 5 fixed semantic colors identifies a widget/state.
 * Reuses the SAME legend already defined on VoidColors (Theme.kt) rather
 * than inventing new meaning here:
 *   green = success/healthy, red = urgent/overdue, blue = informational/
 *   timetable, cyan = interaction, purple = intelligence/analytics.
 * Each of the 3 rectangular widgets keeps ONE fixed identity color drawn
 * from that legend (Classes=blue/timetable, Circle Plan=purple/
 * intelligence) and Temporary Plans is the one place color genuinely
 * changes, since deadline urgency is exactly what red vs green already
 * means in this app — never a random cycle.
 */
enum class VoidBorderColor(val drawableRes: Int, val color: Color) {
    SUCCESS(R.drawable.widget_border_success, Color(0xFF00E676)),
    DANGER(R.drawable.widget_border_danger, Color(0xFFFF3D4D)),
    INFO(R.drawable.widget_border_info, Color(0xFF42A5F5)),
    CYAN(R.drawable.widget_border_cyan, Color(0xFF00CFE8)),
    PURPLE(R.drawable.widget_border_purple, Color(0xFF9C6CFF))
}

object GlanceVoidColors {
    val Background = Color(0xFF050505)
    val Surface = Color(0xFF0D0D0D)
    val Surface2 = Color(0xFF121212)
    val TextPrimary = Color(0xFFF2F2F2)
    val TextSecondary = Color(0xFF888888)
}

/**
 * The widget "card": a colored ring (real Android shape-stroke drawable,
 * so it scales correctly at any widget size — never a fixed-size bitmap)
 * around a dark, slightly-inset fill. This is the closest a home-screen
 * App Widget can get to VOID's animated-border identity: RemoteViews
 * (what Glance ultimately renders to) cannot run a continuous per-frame
 * canvas animation the way a real overlay window can — see the Floating
 * AI Orb for the fully animated version of this same visual language.
 */
@Composable
fun VoidBorderCard(
    border: VoidBorderColor,
    modifier: GlanceModifier = GlanceModifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize().background(ImageProvider(border.drawableRes))) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(2.dp)
                .background(ImageProvider(R.drawable.widget_surface_fill))
                .padding(12.dp)
        ) {
            content()
        }
    }
}

fun voidMonoStyle(sizeSp: Int, color: Color, bold: Boolean = false): TextStyle = TextStyle(
    color = ColorProvider(day = color, night = color),
    fontSize = sizeSp.sp,
    fontFamily = FontFamily.Monospace,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
)
