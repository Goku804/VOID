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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp

/**
 * AI Chat is deliberately NOT a destination here anymore — it's an
 * isolated full-screen overlay reachable from its own floating button
 * (see FloatingAiButton below), not a tab you switch into. EXECUTE fills
 * the slot it used to occupy, moved here from inside Settings.
 */
enum class VoidDestination(val label: String, val icon: ImageVector) {
    HOME("HOME", Icons.Default.Home),
    PLAN("PLAN", Icons.Default.CalendarMonth),
    EXECUTE("EXECUTE", Icons.Default.Bolt),
    SETTINGS("SETTINGS", Icons.Default.Settings)
}

/**
 * Tracks scroll direction so the caller can hide/reveal the floating nav —
 * Telegram-style: scroll down = hide, scroll up = reveal.
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
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(VoidColors.Surface)
                .border(1.dp, VoidColors.Border, RoundedCornerShape(24.dp))
                .padding(vertical = 10.dp),
        ) {
            VoidDestination.entries.forEach { dest ->
                NavItem(
                    destination = dest,
                    isSelected = dest == selected,
                    onClick = { onSelect(dest) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Isolated entry point into AI Chat — deliberately separate from
 * FloatingBottomNav, in its own corner, with its own tap target. Tapping
 * it opens a full-screen overlay (not a tab swap), so it needs to read as
 * its own thing rather than a fifth item bolted onto the nav row.
 */
@Composable
fun FloatingAiButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(VoidMotion.NAV_HIDE_MS)),
        exit = fadeOut(tween(VoidMotion.NAV_HIDE_MS)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(50))
                .background(VoidColors.Surface)
                .border(1.dp, VoidColors.Purple.copy(alpha = 0.5f), RoundedCornerShape(50))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Open AI Chat",
                tint = VoidColors.Purple,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Icon-only nav — no label text below the icon, to keep the bar compact.
 * Selected icon gets a soft layered glow behind it (cheap: 2 stacked
 * translucent circles, no real blur) plus an accent tint; accessibility
 * label still lives on contentDescription for screen readers.
 */
@Composable
private fun NavItem(
    destination: VoidDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (isSelected) VoidColors.NavAccent else VoidColors.TextPrimary

    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(VoidColors.NavAccent.copy(alpha = 0.10f))
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(50))
                        .background(VoidColors.NavAccent.copy(alpha = 0.16f))
                )
            }
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
