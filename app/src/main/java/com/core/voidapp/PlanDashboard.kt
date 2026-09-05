package com.core.voidapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.core.voidapp.data.PlanTaskStatus
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.daysRemaining
import com.core.voidapp.data.isOverdue

private enum class PlanSection(val title: String) {
    CIRCLE("CIRCLE PLAN"), TEMPORARY("TEMPORARY PLAN"), EXAM_PREP("EXAM PREPARATION")
}

@Composable
fun PlanningScreen() {
    var open by remember { mutableStateOf<PlanSection?>(null) }

    BackHandler(enabled = open != null) { open = null }

    when (val section = open) {
        null -> PlanDashboard(onOpen = { open = it })
        PlanSection.CIRCLE -> PlanSubScreen(section.title, onBack = { open = null }) { CirclePlansContent() }
        PlanSection.TEMPORARY -> PlanSubScreen(section.title, onBack = { open = null }) { TemporaryPlanContent() }
        PlanSection.EXAM_PREP -> PlanSubScreen(section.title, onBack = { open = null }) { ExamPrepComingSoon() }
    }
}

@Composable
private fun PlanDashboard(onOpen: (PlanSection) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidColors.Background)
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanNavCard("CIRCLE PLAN", "${VoidRepository.circlePlans.size} slots", Modifier.weight(1f)) { onOpen(PlanSection.CIRCLE) }
                PlanNavCard("TEMPORARY PLAN", "${VoidRepository.activeTemporaryTasks().size} active", Modifier.weight(1f)) { onOpen(PlanSection.TEMPORARY) }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            val urgent = VoidRepository.urgentExamSubjects()
            if (urgent.isNotEmpty()) {
                UrgentPlanBanner(urgent)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            PlanNavCard(
                title = "EXAM PREPARATION",
                subtitle = "Coming in v0.11.0 \u2014 needs the Priority Engine",
                modifier = Modifier.fillMaxWidth(),
                soon = true
            ) { onOpen(PlanSection.EXAM_PREP) }
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
    GlowCard(glowColor = VoidColors.Warning) {
        Text("URGENT PLAN \u2014 ACTIVE", color = VoidColors.Warning, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
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
                Text("${es.daysRemaining()}d", color = VoidColors.Warning, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
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
    val today = todayAsVoidDay()
    val circleToday = VoidRepository.circlePlansFor(today)
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
private fun PlanNavCard(title: String, subtitle: String, modifier: Modifier = Modifier, soon: Boolean = false, onClick: () -> Unit) {
    VoidCard(modifier = modifier.clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = VoidColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(subtitle, color = VoidColors.TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            if (soon) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(VoidColors.Warning.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("SOON", color = VoidColors.Warning, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PlanSubScreen(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(VoidColors.Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = "Back",
                tint = VoidColors.Accent,
                modifier = Modifier.clickable { onBack() }.padding(end = 12.dp)
            )
            Text("PLAN / $title", color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            content()
        }
    }
}

@Composable
private fun ExamPrepComingSoon() {
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
