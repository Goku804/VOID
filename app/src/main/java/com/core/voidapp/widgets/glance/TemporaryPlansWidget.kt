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
import com.core.voidapp.data.widgets.ActivePlanInfo
import com.core.voidapp.data.widgets.TemporaryPlansState
import com.core.voidapp.data.widgets.WidgetDataProvider
import com.core.voidapp.data.widgets.WidgetPreferences
import java.time.LocalDate

private val SMALL = DpSize(120.dp, 64.dp)
private val MEDIUM = DpSize(190.dp, 110.dp)
private val LARGE = DpSize(260.dp, 190.dp)

/** Everything comfortably future (>1 day out) reads calm green; anything due today reads urgent red. Real state, not decoration. */
private fun borderFor(state: TemporaryPlansState): VoidBorderColor = when (state) {
    is TemporaryPlansState.NoActivePlans -> VoidBorderColor.SUCCESS
    is TemporaryPlansState.Content -> {
        val today = LocalDate.now()
        val anyDueToday = state.plans.any { it.deadlineLabel == today.toString() }
        if (anyDueToday) VoidBorderColor.DANGER else VoidBorderColor.SUCCESS
    }
}

class TemporaryPlansWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val enabled = WidgetPreferences.isTemporaryPlansWidgetEnabled(context)
            val state = WidgetDataProvider.temporaryPlans()
            VoidBorderCard(border = if (enabled) borderFor(state) else VoidBorderColor.CYAN) {
                if (!enabled) {
                    DisabledContent("TEMPORARY PLANS")
                } else {
                    TemporaryPlansContent(state)
                }
            }
        }
    }
}

class TemporaryPlansWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TemporaryPlansWidget()
}

@Composable
private fun TemporaryPlansContent(state: TemporaryPlansState) {
    val size = LocalSize.current
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text("TEMPORARY PLANS", style = voidMonoStyle(9, GlanceVoidColors.TextSecondary, bold = true))
        Spacer(modifier = GlanceModifier.height(6.dp))

        when (state) {
            is TemporaryPlansState.NoActivePlans -> {
                Text("Nothing active right now", style = voidMonoStyle(12, GlanceVoidColors.TextPrimary, bold = true))
            }
            is TemporaryPlansState.Content -> {
                val visibleCount = when {
                    size.height >= LARGE.height -> minOf(4, state.plans.size)
                    size.height >= MEDIUM.height -> minOf(2, state.plans.size)
                    else -> 1
                }
                if (size.height < MEDIUM.height && state.plans.size > 1) {
                    Text("${state.plans.size} active plans", style = voidMonoStyle(9, GlanceVoidColors.TextSecondary))
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
                state.plans.take(visibleCount).forEachIndexed { index, plan ->
                    if (index > 0) Spacer(modifier = GlanceModifier.height(6.dp))
                    PlanRow(plan)
                }
                if (state.plans.size > visibleCount) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text("+${state.plans.size - visibleCount} more", style = voidMonoStyle(8, GlanceVoidColors.TextSecondary))
                }
            }
        }
    }
}

@Composable
private fun PlanRow(plan: ActivePlanInfo) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text(plan.title, style = voidMonoStyle(12, GlanceVoidColors.TextPrimary, bold = true))
        Text(
            listOfNotNull(
                plan.subjectName.takeIf { it != "\u2014" },
                plan.timeRangeLabel ?: plan.deadlineLabel
            ).joinToString(" \u00b7 "),
            style = voidMonoStyle(9, GlanceVoidColors.TextSecondary)
        )
    }
}
