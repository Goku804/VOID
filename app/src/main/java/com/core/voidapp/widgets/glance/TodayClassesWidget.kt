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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.Text
import com.core.voidapp.data.widgets.ClassSlotInfo
import com.core.voidapp.data.widgets.TodayClassesState
import com.core.voidapp.data.widgets.WidgetDataProvider
import com.core.voidapp.data.widgets.WidgetPreferences

private val SMALL = DpSize(120.dp, 64.dp)
private val MEDIUM = DpSize(190.dp, 110.dp)
private val LARGE = DpSize(260.dp, 190.dp)

class TodayClassesWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val enabled = WidgetPreferences.isClassesWidgetEnabled(context)
            VoidBorderCard(border = VoidBorderColor.INFO) {
                if (!enabled) {
                    DisabledContent("TODAY'S CLASSES")
                } else {
                    ClassesContent(WidgetDataProvider.todayClasses())
                }
            }
        }
    }
}

class TodayClassesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayClassesWidget()
}

@Composable
private fun ClassesContent(state: TodayClassesState) {
    val size = LocalSize.current
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text("TODAY'S CLASSES", style = voidMonoStyle(9, GlanceVoidColors.TextSecondary, bold = true))
        Spacer(modifier = GlanceModifier.height(6.dp))

        when (state) {
            is TodayClassesState.NoSchoolToday -> {
                Text("No school today", style = voidMonoStyle(12, GlanceVoidColors.TextPrimary, bold = true))
            }
            is TodayClassesState.Finished -> {
                Text("Classes ended for today", style = voidMonoStyle(12, GlanceVoidColors.TextPrimary, bold = true))
                if (size.height >= MEDIUM.height) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text("Back with tomorrow's timetable", style = voidMonoStyle(9, GlanceVoidColors.TextSecondary))
                }
            }
            is TodayClassesState.InProgress -> {
                if (state.current != null) {
                    SlotBlock(label = "NOW", slot = state.current, accent = GlanceVoidColors.TextPrimary)
                } else if (state.next != null) {
                    Text("Between classes", style = voidMonoStyle(11, GlanceVoidColors.TextSecondary))
                } else {
                    Text("No timed periods left today", style = voidMonoStyle(11, GlanceVoidColors.TextSecondary))
                }

                if (size.height >= MEDIUM.height && state.next != null) {
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    SlotBlock(label = "NEXT", slot = state.next, accent = GlanceVoidColors.TextSecondary)
                }

                if (size.height >= LARGE.height) {
                    val remainder = state.remaining.drop(if (state.next != null) 1 else 0)
                    if (remainder.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Text("LATER TODAY", style = voidMonoStyle(8, GlanceVoidColors.TextSecondary, bold = true))
                        remainder.take(3).forEach { slot ->
                            Text(
                                "${slot.startTime ?: ""} \u00b7 ${slot.subjectName}",
                                style = voidMonoStyle(9, GlanceVoidColors.TextSecondary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotBlock(label: String, slot: ClassSlotInfo, accent: androidx.compose.ui.graphics.Color) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text(label, style = voidMonoStyle(8, GlanceVoidColors.TextSecondary, bold = true))
        Text(slot.subjectName, style = voidMonoStyle(15, accent, bold = true))
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(slot.classType.name, style = voidMonoStyle(9, GlanceVoidColors.TextSecondary))
            if (slot.startTime != null) {
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text("${slot.startTime}${slot.endTime?.let { "\u2013$it" } ?: ""}", style = voidMonoStyle(9, GlanceVoidColors.TextSecondary))
            }
        }
    }
}

@Composable
internal fun DisabledContent(name: String) {
    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        Text(name, style = voidMonoStyle(9, GlanceVoidColors.TextSecondary, bold = true))
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text("Disabled in Settings \u2192 Widgets", style = voidMonoStyle(9, GlanceVoidColors.TextSecondary))
    }
}
