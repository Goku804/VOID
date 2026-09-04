package com.core.voidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.core.voidapp.data.countdown

const val APP_NAME = "VOID"
const val APP_VERSION = "VOID v0.7.0"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VoidApp()
        }
    }
}

@Composable
fun VoidApp() {
    var selectedDest by remember { mutableStateOf(VoidDestination.HOME) }
    val navVisibility = rememberNavVisibilityState()

    // Back should return to HOME first, then exit on a second press —
    // matches normal Android behavior instead of closing from any tab.
    androidx.activity.compose.BackHandler(enabled = selectedDest != VoidDestination.HOME) {
        selectedDest = VoidDestination.HOME
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = VoidColors.Background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(navVisibility.connection)
                ) {
                    when (selectedDest) {
                        VoidDestination.HOME -> HomeScreen()
                        VoidDestination.PLAN -> PlanningScreen()
                        VoidDestination.EXECUTE -> ExecutionScreen()
                        VoidDestination.SETTINGS -> SettingsScreen()
                    }
                }

                FloatingBottomNav(
                    selected = selectedDest,
                    visible = navVisibility.visible.value,
                    onSelect = { selectedDest = it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
fun HomeScreen() {
    Box(modifier = Modifier.fillMaxSize().background(VoidColors.Background)) {
        ParticleField(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

        item {
            Text(
                text = APP_NAME,
                color = VoidColors.Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "COMMAND CENTER",
                color = VoidColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ACADEMIC CONTROL SYSTEM",
                color = VoidColors.TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            NearestExamCountdown()
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            val today = todayAsVoidDay()
            val todaysClasses = com.core.voidapp.data.VoidRepository.scheduleFor(today)
            val doneTasks = com.core.voidapp.data.VoidRepository.temporaryTasks.count { it.isCompleted }
            val remainingTasks = com.core.voidapp.data.VoidRepository.temporaryTasks.count { !it.isCompleted }

            VoidSectionLabel("TODAY STATUS")
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("CLASSES", todaysClasses.size.toString().padStart(2, '0'), Modifier.weight(1f))
                StatCard("DONE", doneTasks.toString().padStart(2, '0'), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("REMAINING", remainingTasks.toString().padStart(2, '0'), Modifier.weight(1f))
                StatCard("STUDY", "00:00", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            VoidSectionLabel("TODAY'S CLASSES")
            Spacer(modifier = Modifier.height(8.dp))

            val today = todayAsVoidDay()
            val todaysClasses = com.core.voidapp.data.VoidRepository.scheduleFor(today)

            VoidCard {
                if (todaysClasses.isEmpty()) {
                    Text(
                        text = "No classes set for today. Add them in SETTINGS \u2192 SCHEDULE.",
                        color = VoidColors.TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    todaysClasses.forEach { period ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(
                                text = "P${period.periodNumber}",
                                color = VoidColors.Accent,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(36.dp)
                            )
                            Text(
                                text = com.core.voidapp.data.VoidRepository.subjectName(period.subjectId),
                                color = VoidColors.TextPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            VoidSectionLabel("SYSTEM STATUS")
            Spacer(modifier = Modifier.height(8.dp))
            VoidCard {
                SystemStatusRow("PLANNING")
                SystemStatusRow("ACADEMIC")
                SystemStatusRow("EXECUTION")
                SystemStatusRow("REPORTING")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            VoidSectionLabel("QUICK ACTIONS")
            Spacer(modifier = Modifier.height(8.dp))
            QuickAction("+ ADD TASK")
            QuickAction("+ STUDY SESSION")
            QuickAction("+ EXAM / TEST")
            QuickAction("+ DAILY REPORT")
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = APP_VERSION,
                color = VoidColors.TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(90.dp)) // clears the floating nav
            }
        }
    }
}

/** Maps the real device date to our own DayOfWeekVoid enum. */
fun todayAsVoidDay(): com.core.voidapp.data.DayOfWeekVoid {
    val d = java.time.LocalDate.now().dayOfWeek
    return com.core.voidapp.data.DayOfWeekVoid.valueOf(d.name)
}

/** Live countdown card to the nearest upcoming exam — reads real device time. */
@Composable
fun NearestExamCountdown() {
    val exam = com.core.voidapp.data.VoidRepository.nearestExam()

    GlowCard {
        Text(
            text = "NEXT EXAM",
            color = VoidColors.TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (exam == null) {
            Text(
                text = "No exams scheduled. Add one in SETTINGS \u2192 ACADEMIC.",
                color = VoidColors.TextPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        } else {
            val cd = exam.countdown()
            Text(
                text = "${exam.title} \u00b7 ${com.core.voidapp.data.VoidRepository.subjectName(exam.subjectId)}",
                color = VoidColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildString {
                    if (cd.years > 0) append("${cd.years}y ")
                    if (cd.months > 0 || cd.years > 0) append("${cd.months}m ")
                    append("${cd.days}d")
                    append("  (${cd.totalDays} days total)")
                },
                color = VoidColors.Accent,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    VoidCard(modifier = modifier) {
        Text(title, color = VoidColors.TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(5.dp))
        Text(value, color = VoidColors.TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun SystemStatusRow(name: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(VoidColors.Success, Modifier.width(7.dp).height(7.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(name, color = VoidColors.TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.weight(1f))
        Text("READY", color = VoidColors.Success, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun QuickAction(text: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(VoidColors.Surface2)
            .padding(14.dp)
    ) {
        Text(text, color = VoidColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun PlanningScreen() {
    PlaceholderScreen(
        title = "PLAN",
        subtitle = "CIRCLE \u00b7 TEMPORARY \u00b7 EXAM PREP",
        message = "Circle Plans, Temporary Plans, and the Exam Preparation Engine build here — coming per the roadmap."
    )
}

@Composable
fun ExecutionScreen() {
    PlaceholderScreen(
        title = "EXECUTE",
        subtitle = "ACTIVE STUDY SESSION",
        message = "Session runner initializing..."
    )
}

@Composable
fun PlaceholderScreen(title: String, subtitle: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidColors.Background)
            .padding(20.dp)
    ) {
        Text(APP_NAME, color = VoidColors.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, color = VoidColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = VoidColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(30.dp))
        VoidCard {
            Text("[ SYSTEM ]", color = VoidColors.Accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(10.dp))
            Text(message, color = VoidColors.TextPrimary, fontSize = 14.sp)
        }
    }
}
