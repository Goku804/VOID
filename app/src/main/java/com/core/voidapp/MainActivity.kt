package com.core.voidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Black = Color(0xFF050505)
private val Panel = Color(0xFF0D0D0D)
private val Panel2 = Color(0xFF121212)
private val Border = Color(0xFF242424)
private val TextPrimary = Color(0xFFF2F2F2)
private val TextSecondary = Color(0xFF888888)
private val Accent = Color(0xFF00E676)

private const val APP_NAME = "VOID"
private const val APP_VERSION = "VOID v0.1.0"

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

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitle("TODAY STATUS")
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusCard("PLANNED", "00", Modifier.weight(1f))
                StatusCard("DONE", "00", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusCard("REMAINING", "00", Modifier.weight(1f))
                StatusCard("STUDY", "00:00", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitle("SYSTEM STATUS")
            Spacer(modifier = Modifier.height(8.dp))

            PanelCard {
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
fun StatusCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Panel)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
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
fun PanelCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Panel)
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
fun SystemStatus(name: String) {
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
                .background(Accent)
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
    PlaceholderScreen(
        title = "ACADEMIC",
        subtitle = "GRADES • SUBJECTS • UNITS",
        message = "Academic database initializing..."
    )
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

        PanelCard {
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
