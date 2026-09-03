package com.core.voidapp

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class SettingsSection(val title: String, val subtitle: String, val ready: Boolean) {
    ACADEMIC("ACADEMIC", "Subjects \u00b7 Marks \u00b7 Exams", true),
    SCHEDULE("SCHEDULE", "Timetable \u00b7 Classes \u00b7 Calendar", true),
    EXAMS("EXAMS", "Exam Schedule \u00b7 Preparation", false),
    PLANNING("PLANNING", "Circle Plans \u00b7 Temporary Plans", false),
    REPORTS("REPORTS", "Daily \u00b7 Weekly \u00b7 Monthly", false),
    INTEGRATIONS("INTEGRATIONS", "Telegram", false),
    NOTIFICATIONS("NOTIFICATIONS", "Alerts \u00b7 Reminders", false),
    APPEARANCE("APPEARANCE", "Theme \u00b7 Effects \u00b7 Animation", false),
    DATA("DATA", "Backup \u00b7 Restore \u00b7 Export", false),
    ABOUT("ABOUT", "VOID \u00b7 Version \u00b7 System", true)
}

@Composable
fun SettingsScreen() {
    var open by remember { mutableStateOf<SettingsSection?>(null) }

    when (val section = open) {
        null -> SettingsList(onOpen = { open = it })
        SettingsSection.ACADEMIC -> SettingsSubScreen(section.title, onBack = { open = null }) {
            AcademicScreenReal()
        }
        SettingsSection.SCHEDULE -> SettingsSubScreen(section.title, onBack = { open = null }) {
            SetupScreen()
        }
        SettingsSection.ABOUT -> SettingsSubScreen(section.title, onBack = { open = null }) {
            AboutContent()
        }
        else -> SettingsSubScreen(section.title, onBack = { open = null }) {
            ComingSoonContent(section.title)
        }
    }
}

@Composable
private fun SettingsList(onOpen: (SettingsSection) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidColors.Background)
            .padding(16.dp)
    ) {
        Text("VOID", color = VoidColors.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text("SETTINGS", color = VoidColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("CONFIGURE THE SYSTEM", color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn {
            items(SettingsSection.entries) { section ->
                SettingsRow(section, onClick = { onOpen(section) })
                Spacer(modifier = Modifier.height(8.dp))
            }
            item { Spacer(modifier = Modifier.height(90.dp)) } // clears the floating nav
        }
    }
}

@Composable
private fun SettingsRow(section: SettingsSection, onClick: () -> Unit) {
    VoidCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(section.title, color = VoidColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    if (!section.ready) {
                        Spacer(modifier = Modifier.width(8.dp))
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
                Text(section.subtitle, color = VoidColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = VoidColors.TextSecondary)
        }
    }
}

@Composable
private fun SettingsSubScreen(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = "Back",
                tint = VoidColors.Accent,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp)
            )
            Text("SETTINGS / $title", color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun ComingSoonContent(title: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        VoidCard {
            Text("$title is not built yet.", color = VoidColors.TextPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Coming in a later version, per the build order.", color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun AboutContent() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        VoidCard {
            Text("VOID", color = VoidColors.Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Text(APP_VERSION, color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Academic operating system. Claude-side workspace of the GIDION project.",
                color = VoidColors.TextPrimary,
                fontSize = 12.sp
            )
        }
    }
}
