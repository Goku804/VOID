package com.core.voidapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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

/** Small colored status dot — green/yellow/red/blue meaning, not decoration. */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}
