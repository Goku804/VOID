package com.core.voidapp

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.core.voidapp.data.ClassType
import com.core.voidapp.data.DayOfWeekVoid
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.isDClassSession
import java.time.LocalTime

private val SBlack = Color(0xFF050505)
private val SPanel = Color(0xFF0D0D0D)
private val SPanel2 = Color(0xFF121212)
private val SBorder = Color(0xFF242424)
private val SText = Color(0xFFF2F2F2)
private val SMuted = Color(0xFF888888)
private val SAccent = Color(0xFF00E676)
private val SWarn = Color(0xFFFFB300)

private enum class ScheduleTab { TIMETABLE, NIGHT }

@Composable
fun SetupScreen() {
    var tab by remember { mutableStateOf(ScheduleTab.TIMETABLE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SBlack)
            .padding(16.dp)
    ) {
        Text("VOID", color = SAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text("SCHEDULE", color = SText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("TIMETABLE \u2022 NIGHT STUDY AVAILABILITY", color = SMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            STab("TIMETABLE", tab == ScheduleTab.TIMETABLE, Modifier.weight(1f)) { tab = ScheduleTab.TIMETABLE }
            STab("NIGHT STUDY", tab == ScheduleTab.NIGHT, Modifier.weight(1f)) { tab = ScheduleTab.NIGHT }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (tab) {
            ScheduleTab.TIMETABLE -> TimetableTab()
            ScheduleTab.NIGHT -> NightStudyTab()
        }
    }
}

@Composable
private fun STab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) SAccent.copy(alpha = 0.12f) else SPanel)
            .border(1.dp, if (selected) SAccent else SBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = if (selected) SAccent else SMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------
// TIMETABLE
// ---------------------------------------------------------------------

@Composable
private fun TimetableTab() {
    var selectedDay by remember { mutableStateOf(DayOfWeekVoid.MONDAY) }
    var period by remember { mutableStateOf("") }
    var subjectId by remember { mutableStateOf<String?>(null) }
    var classType by remember { mutableStateOf(ClassType.REGULAR) }
    var startText by remember { mutableStateOf("") }
    var endText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        item {
            SetupPanel {
                Text("REGISTER CLASS", color = SAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                if (VoidRepository.subjects.isEmpty()) {
                    Text("Add a subject in SETTINGS \u2192 ACADEMIC first.", color = SMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                } else {
                    DayDropdown(selected = selectedDay, onSelected = { selectedDay = it })
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoidTextField(value = period, onValueChange = { period = it }, label = "Period #", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.weight(1f)) {
                            SubjectDropdown(selectedId = subjectId, onSelected = { subjectId = it })
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    ClassTypeDropdown(selected = classType, onSelected = { classType = it })

                    val isDClassType = classType == ClassType.LANGUAGE || classType == ClassType.LAB
                    val alreadyHasDClassToday = VoidRepository.scheduleFor(selectedDay).any { it.isDClassSession() }
                    if (isDClassType && !alreadyHasDClassToday && VoidRepository.dClassDayCount() >= 2) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You already have D-Class (Language/Lab) on 2 days this week \u2014 adding a 3rd leaves less afternoon study time.",
                            color = SWarn,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoidTextField(value = startText, onValueChange = { startText = it }, label = "Start (HH:MM)", modifier = Modifier.weight(1f))
                        VoidTextField(value = endText, onValueChange = { endText = it }, label = "End (HH:MM)", modifier = Modifier.weight(1f))
                    }

                    if (error != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(error!!, color = SWarn, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    VoidButton("+ SAVE CLASS") {
                        val p = period.toIntOrNull()
                        if (p == null || subjectId == null) {
                            error = "Period and subject are required"
                        } else {
                            try {
                                val start = if (startText.isBlank()) null else LocalTime.parse(startText.trim())
                                val end = if (endText.isBlank()) null else LocalTime.parse(endText.trim())
                                VoidRepository.addClassPeriod(selectedDay, p, subjectId!!, classType, start, end)
                                period = ""; startText = ""; endText = ""; error = null
                            } catch (e: Exception) {
                                error = "Time must be HH:MM, e.g. 08:30"
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("WEEKLY TIMETABLE", color = SMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(DayOfWeekVoid.entries.filter { it != DayOfWeekVoid.SUNDAY }) { day ->
            val periods = VoidRepository.scheduleFor(day)
            SetupPanel {
                Text(day.name, color = SAccent, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                if (periods.isEmpty()) {
                    Text("No classes set", color = SMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                } else {
                    periods.forEach { p ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text("P${p.periodNumber}", color = SMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(30.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(VoidRepository.subjectName(p.subjectId), color = SText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                if (p.classType != ClassType.REGULAR) {
                                    Text(p.classType.name.replace("_", " "), color = SMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            if (p.startTime != null) {
                                Text(
                                    text = "${p.startTime}" + (p.endTime?.let { "\u2013$it" } ?: ""),
                                    color = SMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        item { Spacer(modifier = Modifier.height(90.dp)) }
    }
}

// ---------------------------------------------------------------------
// NIGHT STUDY AVAILABILITY
// ---------------------------------------------------------------------

@Composable
private fun NightStudyTab() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = "Each day can have its own night study window. VOID never assumes the same hours every night.",
                color = SMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        items(DayOfWeekVoid.entries) { day ->
            NightAvailabilityRow(day)
            Spacer(modifier = Modifier.height(10.dp))
        }

        item { Spacer(modifier = Modifier.height(90.dp)) }
    }
}

@Composable
private fun NightAvailabilityRow(day: DayOfWeekVoid) {
    val current = VoidRepository.nightAvailabilityFor(day)
    var available by remember(day) { mutableStateOf(current.available) }
    var startText by remember(day) { mutableStateOf(current.start?.toString() ?: "") }
    var endText by remember(day) { mutableStateOf(current.end?.toString() ?: "") }
    var error by remember(day) { mutableStateOf<String?>(null) }

    SetupPanel {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(day.name, color = SText, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Switch(
                checked = available,
                onCheckedChange = {
                    available = it
                    val start = startText.toLocalTimeOrNull()
                    val end = endText.toLocalTimeOrNull()
                    VoidRepository.setNightAvailability(day, it, start, end)
                },
                colors = SwitchDefaults.colors(checkedThumbColor = SAccent, checkedTrackColor = SAccent.copy(alpha = 0.4f))
            )
        }

        if (available) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoidTextField(value = startText, onValueChange = { startText = it }, label = "Start (HH:MM)", modifier = Modifier.weight(1f))
                VoidTextField(value = endText, onValueChange = { endText = it }, label = "End (HH:MM)", modifier = Modifier.weight(1f))
            }
            if (error != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(error!!, color = SWarn, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(8.dp))
            VoidButton("SAVE") {
                val start = startText.toLocalTimeOrNull()
                val end = endText.toLocalTimeOrNull()
                if ((startText.isNotBlank() && start == null) || (endText.isNotBlank() && end == null)) {
                    error = "Time must be HH:MM, e.g. 20:00"
                } else {
                    VoidRepository.setNightAvailability(day, available, start, end)
                    error = null
                }
            }
        }
    }
}

private fun String.toLocalTimeOrNull(): LocalTime? =
    if (isBlank()) null else try { LocalTime.parse(trim()) } catch (e: Exception) { null }

// ---------------------------------------------------------------------
// DROPDOWNS
// ---------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDropdown(selected: DayOfWeekVoid, onSelected: (DayOfWeekVoid) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(SPanel2)
                .border(1.dp, SBorder, RoundedCornerShape(6.dp))
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth()
        ) {
            Text(selected.name, color = SText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SMuted)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DayOfWeekVoid.entries.filter { it != DayOfWeekVoid.SUNDAY }.forEach { day ->
                DropdownMenuItem(text = { Text(day.name) }, onClick = { onSelected(day); open = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassTypeDropdown(selected: ClassType, onSelected: (ClassType) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(SPanel2)
                .border(1.dp, SBorder, RoundedCornerShape(6.dp))
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth()
        ) {
            Text(selected.name.replace("_", " "), color = SText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SMuted)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ClassType.entries.forEach { type ->
                DropdownMenuItem(text = { Text(type.name.replace("_", " ")) }, onClick = { onSelected(type); open = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectDropdown(selectedId: String?, onSelected: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val name = VoidRepository.subjects.find { it.id == selectedId }?.name ?: "Select"
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(SPanel2)
                .border(1.dp, SBorder, RoundedCornerShape(6.dp))
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth()
        ) {
            Text(name, color = SText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SMuted)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (VoidRepository.subjects.isEmpty()) {
                DropdownMenuItem(text = { Text("Add a subject first") }, onClick = { open = false })
            }
            VoidRepository.subjects.forEach { subject ->
                DropdownMenuItem(text = { Text(subject.name) }, onClick = { onSelected(subject.id); open = false })
            }
        }
    }
}

// ---------------------------------------------------------------------
// SHARED SETUP-SCREEN COMPONENTS
// ---------------------------------------------------------------------

@Composable
private fun SetupPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SPanel)
            .border(1.dp, SBorder, RoundedCornerShape(10.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun VoidTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = SText),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SAccent,
            unfocusedBorderColor = SBorder,
            focusedLabelColor = SAccent,
            unfocusedLabelColor = SMuted,
            cursorColor = SAccent,
            focusedTextColor = SText,
            unfocusedTextColor = SText
        )
    )
}

@Composable
private fun VoidButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(VoidColors.Cyan.copy(alpha = 0.12f))
            .border(1.dp, VoidColors.Cyan, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = VoidColors.Cyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
