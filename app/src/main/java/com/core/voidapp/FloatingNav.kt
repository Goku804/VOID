package com.core.voidapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class VoidDestination(val label: String, val icon: ImageVector) {
    HOME("HOME", Icons.Default.Home),
    PLAN("PLAN", Icons.Default.CalendarMonth),
    EXECUTE("EXECUTE", Icons.Default.PlayArrow),
    SETTINGS("SETTINGS", Icons.Default.Settings)
}

/**
 * Tracks scroll direction so the caller can hide/reveal the floating nav —
 * Telegram-style: scroll down = hide, scroll up = reveal.
 * Attach the returned Modifier's nestedScroll to the scrolling content container.
 */
class NavVisibilityState {
    var visible = mutableStateOf(true)
        private set

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y < -4f) visible.value = false
            else if (available.y > 4f) visible.value = true
            return Offset.Zero
        }
    }
}

@Composable
fun rememberNavVisibilityState(): NavVisibilityState = remember { NavVisibilityState() }

@Composable
fun FloatingBottomNav(
    selected: VoidDestination,
    visible: Boolean,
    onSelect: (VoidDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(animationSpec = tween(VoidMotion.NAV_HIDE_MS)) { it } + fadeIn(tween(VoidMotion.NAV_HIDE_MS)),
        exit = slideOutVertically(animationSpec = tween(VoidMotion.NAV_HIDE_MS)) { it } + fadeOut(tween(VoidMotion.NAV_HIDE_MS)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(VoidColors.Surface)
                .border(1.dp, VoidColors.Border, RoundedCornerShape(28.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            VoidDestination.entries.forEach { dest ->
                NavPillItem(
                    destination = dest,
                    isSelected = dest == selected,
                    onClick = { onSelect(dest) }
                )
            }
        }
    }
}

@Composable
private fun NavPillItem(destination: VoidDestination, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) VoidColors.Accent.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                tint = if (isSelected) VoidColors.Accent else VoidColors.TextSecondary,
                modifier = Modifier.width(20.dp)
            )
            if (isSelected) {
                Box(modifier = Modifier.width(6.dp))
                Text(
                    text = destination.label,
                    color = VoidColors.Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
