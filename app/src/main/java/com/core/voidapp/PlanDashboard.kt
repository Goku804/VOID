package com.core.voidapp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.daysRemaining
import com.core.voidapp.data.isOverdue

/**
 * PLAN is a single scrollable page, not a drill-down menu: everything the
 * user has ever registered (Circle Plan, Temporary Plan, Exam Prep) shows
 * up here directly, today's items first. Registration itself happens in
 * Settings -> Planning; this tab is read/act-only.
 */
@Composable
fun PlanningScreen() {
    val cycleLength = com.core.voidapp.data.CircleCyclePreferences.cycleLengthWeeks(androidx.compose.ui.platform.LocalContext.current)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(APP_NAME, color = VoidColors.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("PLAN", color = VoidColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(java.time.LocalDate.now().toString(), color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            TodaysPlanSummaryCard()
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            val urgent = VoidRepository.urgentExamSubjects()
            if (urgent.isNotEmpty()) {
                UrgentPlanBanner(urgent)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            val circleToday = VoidRepository.circlePlansForToday()
            val tempToday = VoidRepository.temporaryTasksForDay(java.time.LocalDate.now())
            if (circleToday.isNotEmpty() || tempToday.isNotEmpty()) {
                VoidSectionLabel("TODAY")
                Spacer(modifier = Modifier.height(8.dp))
                circleToday.forEach { plan ->
                    CirclePlanRow(plan)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                tempToday.forEach { task ->
                    TemporaryTaskRow(task)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            VoidSectionLabel("CIRCLE PLAN \u2014 EVERY WEEK")
            Spacer(modifier = Modifier.height(8.dp))
        }
        circlePlanBrowseItems(cycleLength)
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            VoidSectionLabel("TEMPORARY PLANS")
            Spacer(modifier = Modifier.height(8.dp))
        }
        temporaryPlanBrowseItems()
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            VoidSectionLabel("EXAM PREPARATION")
            Spacer(modifier = Modifier.height(8.dp))
            ExamPrepComingSoon()
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            VoidSectionLabel("PLANNING STATUS")
            Spacer(modifier = Modifier.height(8.dp))
            PlanningStatusCard()
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
private fun UrgentPlanBanner(urgentSubjects: List<com.core.voidapp.data.ExamSubject>) {
    GlowCard(glowColor = VoidColors.Danger) {
        Text("URGENT PLAN \u2014 ACTIVE", color = VoidColors.Danger, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Automatically triggered \u2014 a Mid/Final/Mock exam is 16-20 days out.",
            color = VoidColors.TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))
        urgentSubjects.forEach { es ->
            val exam = VoidRepository.examFor(es)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(
                    text = "${exam?.examType?.name ?: ""} \u00b7 ${VoidRepository.subjectName(es.subjectId)}",
                    color = VoidColors.TextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                Text("${es.daysRemaining()}d", color = VoidColors.Danger, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Full auto-generated Exam Preparation needs the Priority Engine (v0.11.0) \u2014 for now this is a detection signal only.",
            color = VoidColors.TextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TodaysPlanSummaryCard() {
    val circleToday = VoidRepository.circlePlansForToday()
    val tempDueToday = VoidRepository.temporaryTasksForDay(java.time.LocalDate.now())
    val totalItems = circleToday.size + tempDueToday.size
    val plannedMinutes = circleToday.sumOf { it.durationMinutes } + tempDueToday.sumOf { it.requiredMinutes }

    VoidCard {
        Text("TODAY'S PLAN", color = VoidColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$totalItems items \u00b7 ${plannedMinutes}min planned",
            color = VoidColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Full Today's Plan (auto-scheduled, source-tagged) needs the Priority Engine \u2014 not built yet. This is a raw count of what's due today.",
            color = VoidColors.TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun PlanningStatusCard() {
    val overdue = VoidRepository.temporaryTasks.count { it.isOverdue() }
    val highPriority = VoidRepository.activeTemporaryTasks().count { it.priority == com.core.voidapp.data.PlanPriority.HIGH }

    VoidCard {
        StatusLine(if (overdue > 0) VoidColors.Danger else VoidColors.Success, "$overdue overdue task${if (overdue == 1) "" else "s"}")
        Spacer(modifier = Modifier.height(6.dp))
        StatusLine(if (highPriority > 0) VoidColors.Warning else VoidColors.Success, "$highPriority high-priority task${if (highPriority == 1) "" else "s"}")
        Spacer(modifier = Modifier.height(6.dp))
        StatusLine(VoidColors.Info, "Conflict detection needs the Priority Engine \u2014 coming later")
    }
}

@Composable
private fun StatusLine(color: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(color, Modifier.height(7.dp).then(Modifier.width(7.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = VoidColors.TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun ExamPrepComingSoon() {
    Column(modifier = Modifier.fillMaxSize()) {
        VoidCard {
            Text("Exam Preparation isn't built yet.", color = VoidColors.TextPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "It needs real signals to work honestly: per-unit progress, understanding, marks, and available time \u2014 not just an equal subject rotation. That's v0.11.0, after Exam Schedule (v0.10.0).",
                color = VoidColors.TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
