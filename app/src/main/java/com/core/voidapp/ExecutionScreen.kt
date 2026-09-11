package com.core.voidapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.core.voidapp.data.CirclePlan
import com.core.voidapp.data.StudySession
import com.core.voidapp.data.StudySessionSource
import com.core.voidapp.data.TemporaryTask
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.elapsedMinutes
import com.core.voidapp.data.guardian.GuardianEngine
import com.core.voidapp.data.guardian.GuardianRepository
import com.core.voidapp.data.guardian.isCurrentlyActive
import com.core.voidapp.data.guardian.remainingMinutes as breakRemainingMinutes
import com.core.voidapp.data.remainingMinutes
import com.core.voidapp.data.resolvedUnit
import kotlinx.coroutines.delay

@Composable
fun ExecutionScreen() {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val activeSession = VoidRepository.activeSession()

    // One shared clock for this screen: drives the live countdown, checks
    // for a naturally-expired authorized break, and — once wired in a
    // later phase — will also be where a foreground-app distraction check
    // gets polled from while a session is active.
    LaunchedEffect(activeSession?.id) {
        while (true) {
            delay(1000)
            tick++
            if (activeSession != null) GuardianEngine.checkBreakExpiry(context)
        }
    }
    LaunchedEffect(Unit) {
        GuardianEngine.scanForUrgentSituations(context)
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(APP_NAME, color = VoidColors.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(6.dp))
        Text("EXECUTE", color = VoidColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("ACTIVE STUDY SESSION", color = VoidColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(20.dp))

        GuardianStatusStrip()
        Spacer(modifier = Modifier.height(16.dp))

        // reading `tick` here (even though unused otherwise) is what makes this
        // Column recompose every second while a session is live
        key(tick) {
            if (activeSession != null) {
                ActiveSessionCard(activeSession, context)
            } else {
                StartSessionList(context)
            }
        }
    }
}

@Composable
private fun GuardianStatusStrip() {
    val commitment = GuardianRepository.currentCommitment()
    val settings = GuardianRepository.settings()
    VoidCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(
                if (commitment?.isCurrentlyActive() == true) VoidColors.Purple else VoidColors.TextSecondary,
                Modifier.width(7.dp).height(7.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (GuardianRepository.isCommitted())
                    "GUARDIAN ACTIVE \u00b7 ${settings.enforcementMode.name}"
                else
                    "GUARDIAN NOT COMMITTED \u00b7 SETTINGS \u2192 GUARDIAN",
                color = VoidColors.TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ActiveSessionCard(session: StudySession, context: android.content.Context) {
    val onBreak = GuardianEngine.isOnAuthorizedBreak()
    val brk = GuardianEngine.currentBreak()
    val subjectName = VoidRepository.subjectName(session.subjectId)
    val remaining = session.remainingMinutes()
    val elapsed = session.elapsedMinutes()
    val progress = if (session.plannedMinutes > 0) (elapsed.toFloat() / session.plannedMinutes).coerceIn(0f, 1f) else 0f

    VoidCard {
        Text(subjectName, color = VoidColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(session.label, color = VoidColors.TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = if (onBreak) VoidColors.Cyan else VoidColors.Purple,
            trackColor = VoidColors.Surface2,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (onBreak && brk != null) {
            Text(
                "ON BREAK \u00b7 ${brk.breakRemainingMinutes()} min left",
                color = VoidColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
            )
        } else {
            Text(
                "$remaining min remaining \u00b7 $elapsed min elapsed",
                color = VoidColors.TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (onBreak) {
            ActionPill("END BREAK EARLY", VoidColors.Cyan) { GuardianEngine.endBreakEarly(context) }
        } else {
            Text("TAKE A BREAK", color = VoidColors.TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 20).forEach { minutes ->
                    ActionPill("$minutes MIN", VoidColors.Surface2, textColor = VoidColors.TextPrimary) {
                        GuardianEngine.startBreak(context, minutes)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionPill("COMPLETE SESSION", VoidColors.Success, modifier = Modifier.weight(1f)) {
                VoidRepository.completeSession(session.id)
            }
            ActionPill("ABANDON", VoidColors.Danger, modifier = Modifier.weight(1f)) {
                VoidRepository.abandonSession(session.id)
            }
        }
    }
}

@Composable
private fun StartSessionList(context: android.content.Context) {
    val today = todayAsVoidDay()
    val circleSlots = VoidRepository.circlePlansFor(today)
    val tasks = VoidRepository.activeTemporaryTasks()
        .filter { it.requiredMinutes > it.completedMinutes }

    if (circleSlots.isEmpty() && tasks.isEmpty()) {
        VoidCard {
            Text(
                "Nothing to start yet. Set up today's Circle Plan or add a Temporary Plan with time remaining.",
                color = VoidColors.TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace
            )
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (circleSlots.isNotEmpty()) {
            item {
                VoidSectionLabel("TODAY'S CIRCLE PLAN")
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(circleSlots) { plan -> CirclePlanStartRow(plan, context) }
            item { Spacer(modifier = Modifier.height(6.dp)) }
        }
        if (tasks.isNotEmpty()) {
            item {
                VoidSectionLabel("TEMPORARY PLANS")
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(tasks) { task -> TemporaryTaskStartRow(task, context) }
        }
        item { Spacer(modifier = Modifier.height(90.dp)) }
    }
}

@Composable
private fun CirclePlanStartRow(plan: CirclePlan, context: android.content.Context) {
    val subjectName = VoidRepository.subjectName(plan.subjectId)
    val unitName = plan.resolvedUnit()?.name
    VoidCard(
        modifier = Modifier.clickable {
            VoidRepository.startSession(
                source = StudySessionSource.CIRCLE_PLAN,
                subjectId = plan.subjectId,
                label = unitName ?: "Circle Plan session",
                plannedMinutes = plan.durationMinutes,
                circlePlanId = plan.id,
                unitId = plan.resolvedUnit()?.id
            )
        }
    ) {
        Text(subjectName, color = VoidColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        if (unitName != null) {
            Text(unitName, color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Text("${plan.durationMinutes} min \u00b7 ${plan.window.name}", color = VoidColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun TemporaryTaskStartRow(task: TemporaryTask, context: android.content.Context) {
    val subjectName = VoidRepository.subjectName(task.subjectId)
    val remaining = (task.requiredMinutes - task.completedMinutes).coerceAtLeast(1)
    VoidCard(
        modifier = Modifier.clickable {
            VoidRepository.startSession(
                source = StudySessionSource.TEMPORARY_TASK,
                subjectId = task.subjectId,
                label = task.title,
                plannedMinutes = remaining,
                temporaryTaskId = task.id
            )
        }
    ) {
        Text(task.title, color = VoidColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(subjectName, color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("$remaining min remaining \u00b7 due ${task.deadline}", color = VoidColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ActionPill(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
