package com.core.voidapp.widgets.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.Text
import com.core.voidapp.data.widgets.CirclePlanSlotInfo
import com.core.voidapp.data.widgets.TodayCirclePlanState
import com.core.voidapp.data.widgets.WidgetDataProvider
import com.core.voidapp.data.widgets.WidgetPreferences

private val SMALL = DpSize(120.dp, 64.dp)
private val MEDIUM = DpSize(190.dp, 110.dp)
private val LARGE = DpSize(260.dp, 190.dp)

class TodayCirclePlanWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val enabled = WidgetPreferences.isCirclePlanWidgetEnabled(context)
            VoidBorderCard(border = VoidBorderColor.PURPLE) {
                if (!enabled) {
                    DisabledContent("TODAY'S CIRCLE PLAN")
                } else {
                    CirclePlanContent(WidgetDataProvider.todayCirclePlan())
                }
            }
        }
    }
}

class TodayCirclePlanWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayCirclePlanWidget()
}

@Composable
private fun CirclePlanContent(state: TodayCirclePlanState) {
    val size = LocalSize.current
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text("CIRCLE PLAN", style = voidMonoStyle(9, GlanceVoidColors.TextSecondary, bold = true))
        Spacer(modifier = GlanceModifier.height(6.dp))

        when (state) {
            is TodayCirclePlanState.NoPlanToday -> {
                Text("No Circle Plan set for today", style = voidMonoStyle(12, GlanceVoidColors.TextPrimary, bold = true))
            }
            is TodayCirclePlanState.Content -> {
                val visibleCount = when {
                    size.height >= LARGE.height -> state.slots.size
                    size.height >= MEDIUM.height -> minOf(2, state.slots.size)
                    else -> 1
                }
                state.slots.take(visibleCount).forEachIndexed { index, slot ->
                    if (index > 0) Spacer(modifier = GlanceModifier.height(8.dp))
                    SlotRow(slot)
                }
                if (state.slots.size > visibleCount) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text("+${state.slots.size - visibleCount} more today", style = voidMonoStyle(8, GlanceVoidColors.TextSecondary))
                }
            }
        }
    }
}

@Composable
private fun SlotRow(slot: CirclePlanSlotInfo) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text(slot.subjectName, style = voidMonoStyle(14, GlanceVoidColors.TextPrimary, bold = true))
        if (slot.resolvedUnitName != null) {
            Text(slot.resolvedUnitName, style = voidMonoStyle(10, GlanceVoidColors.TextSecondary))
        }
        Text(
            "${slot.durationMinutes}min \u00b7 ${slot.window} \u00b7 ${slot.priority}",
            style = voidMonoStyle(9, GlanceVoidColors.TextSecondary)
        )
    }
}
