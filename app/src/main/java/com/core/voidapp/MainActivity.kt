package com.core.voidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.core.voidapp.data.countdown

private val Black = Color(0xFF050505)
private val Panel = Color(0xFF0B0F0D)
private val Panel2 = Color(0xFF101512)
private val Border = Color(0xFF1E2A24)
private val TextPrimary = Color(0xFFF2F2F2)
private val TextSecondary = Color(0xFF7A8B84)
private val Accent = Color(0xFF00E676)

private const val APP_NAME = "VOID"
private const val APP_VERSION = "VOID v0.4.0"

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
    var selectedScreen by remember { mutableStateOf(0) }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Black
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedScreen) {
                        0 -> HomeScreen()
                        1 -> PlanningScreen()
                        2 -> AcademicScreen()
                        3 -> ExecutionScreen()
                        4 -> ReportsScreen()
                        5 -> SetupScreen()
                    }
                }

                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = Panel,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedScreen == 0,
                        onClick = { selectedScreen = 0 },
                        icon = { Icon(Icons.Default.Home, "Home") },
                        label = { Text("HOME") },
                        colors = navigationColors()
                    )

                    NavigationBarItem(
                        selected = selectedScreen == 1,
                        onClick = { selectedScreen = 1 },
                        icon = { Icon(Icons.Default.CalendarMonth, "Planning") },
                        label = { Text("PLAN") },
                        colors = navigationColors()
                    )

                    NavigationBarItem(
                        selected = selectedScreen == 2,
                        onClick = { selectedScreen = 2 },
                        icon = { Icon(Icons.Default.MenuBook, "Academic") },
                        label = { Text("ACADEMIC") },
                        colors = navigationColors()
                    )

                    NavigationBarItem(
                        selected = selectedScreen == 3,
                        onClick = { selectedScreen = 3 },
                        icon = { Icon(Icons.Default.PlayArrow, "Execution") },
                        label = { Text("EXECUTE") },
                        colors = navigationColors()
                    )

                    NavigationBarItem(
                        selected = selectedScreen == 4,
                        onClick = { selectedScreen = 4 },
                        icon = { Icon(Icons.Default.Assessment, "Reports") },
                        label = { Text("REPORTS") },
                        colors = navigationColors()
                    )

                    NavigationBarItem(
                        selected = selectedScreen == 5,
                        onClick = { selectedScreen = 5 },
                        icon = { Icon(Icons.Default.Settings, "Setup") },
                        label = { Text("SETUP") },
                        colors = navigationColors()
                    )
                }
            }
        }
    }
}

@Composable
fun navigationColors() =
    NavigationBarItemDefaults.colors(
        selectedIconColor = Accent,
        selectedTextColor = Accent,
        indicatorColor = Color.Transparent,
        unselectedIconColor = TextSecondary,
        unselectedTextColor = TextSecondary
    )

/**
 * Breathing glow value shared by any composable that wants a pulsing accent.
 * Cheap: just an animated float, no blur/render-effect, safe on low-end devices.
 */
@Composable
fun rememberGlowPulse(): Float {
    val transition = rememberInfiniteTransition(label = "glow")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    return pulse
}

/**
 * Simulated glow: 3 stacked borders, each wider and more transparent than the last.
 * No real blur -> cheap on a 3GB / Android 9 device.
 */
@Composable
fun Modifier.glowBorder(pulse: Float, radius: Int = 10): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    return this
        .background(Panel, shape)
        .border(1.dp, Accent.copy(alpha = 0.06f * pulse), shape.let { RoundedCornerShape((radius + 6).dp) })
        .border(1.dp, Accent.copy(alpha = 0.9f * pulse), shape)
}

@Composable
fun GlowPanelCard(content: @Composable ColumnScope.() -> Unit) {
    val pulse = rememberGlowPulse()
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .glowBorder(pulse, 10)
                .padding(14.dp),
            content = content
        )
        HudCorners(pulse = pulse)
    }
}

/** Draws small L-shaped bracket marks in each corner — HUD / target-lock look. */
@Composable
fun HudCorners(pulse: Float) {
    Canvas(modifier = Modifier.fillMaxWidth().fillMaxSize()) {
        val len = 14.dp.toPx()
        val stroke = Stroke(width = 2.dp.toPx())
        val c = Accent.copy(alpha = (0.5f + 0.5f * pulse))

        // top-left
        drawLine(c, Offset(0f, 0f), Offset(len, 0f), stroke.width)
        drawLine(c, Offset(0f, 0f), Offset(0f, len), stroke.width)
        // top-right
        drawLine(c, Offset(size.width, 0f), Offset(size.width - len, 0f), stroke.width)
        drawLine(c, Offset(size.width, 0f), Offset(size.width, len), stroke.width)
        // bottom-left
        drawLine(c, Offset(0f, size.height), Offset(len, size.height), stroke.width)
        drawLine(c, Offset(0f, size.height), Offset(0f, size.height - len), stroke.width)
        // bottom-right
        drawLine(c, Offset(size.width, size.height), Offset(size.width - len, size.height), stroke.width)
        drawLine(c, Offset(size.width, size.height), Offset(size.width, size.height - len), stroke.width)
    }
}

@Composable
fun HomeScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(16.dp)
    ) {

        item {
            Text(
                text = APP_NAME,
                color = Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "COMMAND CENTER",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "ACADEMIC CONTROL SYSTEM",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(6.dp))
            ScanLine()
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            NearestExamCountdown()
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            val today = todayAsVoidDay()
            val todaysClasses = com.core.voidapp.data.VoidRepository.scheduleFor(today)
            val doneTasks = com.core.voidapp.data.VoidRepository.temporaryTasks.count { it.isCompleted }
            val remainingTasks = com.core.voidapp.data.VoidRepository.temporaryTasks.count { !it.isCompleted }

            SectionTitle("TODAY STATUS")
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlowStatusCard("CLASSES", todaysClasses.size.toString().padStart(2, '0'), Modifier.weight(1f))
                GlowStatusCard("DONE", doneTasks.toString().padStart(2, '0'), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlowStatusCard("REMAINING", remainingTasks.toString().padStart(2, '0'), Modifier.weight(1f))
                GlowStatusCard("STUDY", "00:00", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitle("TODAY'S CLASSES")
            Spacer(modifier = Modifier.height(8.dp))

            val today = todayAsVoidDay()
            val todaysClasses = com.core.voidapp.data.VoidRepository.scheduleFor(today)

            GlowPanelCard {
                if (todaysClasses.isEmpty()) {
                    Text(
                        text = "No classes set for today. Add them in SETUP.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    todaysClasses.forEach { period ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(
                                text = "P${period.periodNumber}",
                                color = Accent,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(36.dp)
                            )
                            Text(
                                text = com.core.voidapp.data.VoidRepository.subjectName(period.subjectId),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitle("SYSTEM STATUS")
            Spacer(modifier = Modifier.height(8.dp))

            GlowPanelCard {
                SystemStatus("PLANNING")
                SystemStatus("ACADEMIC")
                SystemStatus("EXECUTION")
                SystemStatus("REPORTING")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitle("QUICK ACTIONS")
            Spacer(modifier = Modifier.height(8.dp))

            QuickAction("+ ADD TASK")
            QuickAction("+ STUDY SESSION")
            QuickAction("+ EXAM / TEST")
            QuickAction("+ DAILY REPORT")

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = APP_VERSION,
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth(),
            )
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

    GlowPanelCard {
        Text(
            text = "NEXT EXAM",
            color = TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (exam == null) {
            Text(
                text = "No exams scheduled. Add one in ACADEMIC.",
                color = TextPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        } else {
            val cd = exam.countdown()
            Text(
                text = "${exam.title} \u00b7 ${com.core.voidapp.data.VoidRepository.subjectName(exam.subjectId)}",
                color = TextPrimary,
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
                color = Accent,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Thin horizontal glowing line under the header — cheap "system online" accent. */
@Composable
fun ScanLine() {
    val pulse = rememberGlowPulse()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Accent.copy(alpha = 0.15f + 0.5f * pulse))
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
    )
}

@Composable
fun GlowStatusCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val pulse = rememberGlowPulse()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .glowBorder(pulse, 8)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = value,
                color = TextPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun SystemStatus(name: String) {
    val pulse = rememberGlowPulse()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(50))
                .background(Accent.copy(alpha = 0.5f + 0.5f * pulse))
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = name,
            color = TextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "READY",
            color = Accent,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun QuickAction(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Panel2)
            .border(1.dp, Border, RoundedCornerShape(7.dp))
            .clickable { }
            .padding(14.dp)
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun PlanningScreen() {
    PlaceholderScreen(
        title = "PLANNING",
        subtitle = "TASK & SCHEDULE CONTROL",
        message = "Planning system initializing..."
    )
}

@Composable
fun AcademicScreen() {
    AcademicScreenReal()
}

@Composable
fun ExecutionScreen() {
    PlaceholderScreen(
        title = "EXECUTION",
        subtitle = "ACTIVE STUDY SESSION",
        message = "Execution system initializing..."
    )
}

@Composable
fun ReportsScreen() {
    PlaceholderScreen(
        title = "REPORTS",
        subtitle = "PERFORMANCE ANALYTICS",
        message = "Reporting system initializing..."
    )
}

@Composable
fun PlaceholderScreen(
    title: String,
    subtitle: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(20.dp)
    ) {
        Text(
            text = APP_NAME,
            color = Accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(30.dp))

        GlowPanelCard {
            Text(
                text = "[ SYSTEM ]",
                color = Accent,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = message,
                color = TextPrimary,
                fontSize = 15.sp
            )
        }
    }
}
