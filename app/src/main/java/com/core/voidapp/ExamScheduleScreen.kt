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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.core.voidapp.data.ExamSession
import com.core.voidapp.data.ExamSittingStatus
import com.core.voidapp.data.ExamSubject
import com.core.voidapp.data.ExamType
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.daysRemaining
import com.core.voidapp.data.isUrgent
import com.core.voidapp.data.label
import com.core.voidapp.data.status
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun ExamScheduleScreen() {
    var examType by remember { mutableStateOf(ExamType.MID) }
    var subjectId by remember { mutableStateOf<String?>(null) }
    var dateText by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("") }
    var session by remember { mutableStateOf<ExamSession?>(null) }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedUnitIds by remember { mutableStateOf(setOf<String>()) }
    var selectedGrades by remember { mutableStateOf(setOf<Int>()) }
    var error by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidColors.Background)
            .padding(16.dp)
    ) {
        item {
            Text(APP_NAME, color = VoidColors.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("EXAM SCHEDULE", color = VoidColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("WHAT'S HAPPENING, WHEN", color = VoidColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            EVoidCard {
                Text("REGISTER EXAM", color = VoidColors.Accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                if (VoidRepository.subjects.isEmpty()) {
                    Text("Add a subject in SETTINGS \u2192 ACADEMIC first.", color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                } else {
                    EExamTypeDropdown(selected = examType, onSelected = { examType = it })
                    Spacer(modifier = Modifier.height(6.dp))
                    ESubjectDropdown(selectedId = subjectId, onSelected = { subjectId = it; selectedUnitIds = emptySet() })

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EField(value = dateText, onValueChange = { dateText = it }, label = "Date (YYYY-MM-DD)", modifier = Modifier.weight(1f))
                        EField(value = timeText, onValueChange = { timeText = it }, label = "Time (HH:MM)", modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    ESessionDropdown(selected = session, onSelected = { session = it })

                    if (subjectId != null) {
                        val units = VoidRepository.unitsFor(subjectId!!)
                        if (units.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("UNITS INCLUDED", color = VoidColors.TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            units.forEach { u ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        selectedUnitIds = if (selectedUnitIds.contains(u.id)) selectedUnitIds - u.id else selectedUnitIds + u.id
                                    }
                                ) {
                                    Checkbox(
                                        checked = selectedUnitIds.contains(u.id),
                                        onCheckedChange = { checked -> selectedUnitIds = if (checked) selectedUnitIds + u.id else selectedUnitIds - u.id },
                                        colors = CheckboxDefaults.colors(checkedColor = VoidColors.Accent, uncheckedColor = VoidColors.TextSecondary)
                                    )
                                    Text("U${u.unitNumber} \u2014 ${u.name}", color = VoidColors.TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    if (examType == ExamType.MOCK) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("GRADES COVERED", color = VoidColors.TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Row {
                            listOf(9, 10, 11, 12).forEach { g ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        selectedGrades = if (selectedGrades.contains(g)) selectedGrades - g else selectedGrades + g
                                    }
                                ) {
                                    Checkbox(
                                        checked = selectedGrades.contains(g),
                                        onCheckedChange = { checked -> selectedGrades = if (checked) selectedGrades + g else selectedGrades - g },
                                        colors = CheckboxDefaults.colors(checkedColor = VoidColors.Accent, uncheckedColor = VoidColors.TextSecondary)
                                    )
                                    Text("G$g", color = VoidColors.TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    EField(value = location, onValueChange = { location = it }, label = "Location (optional)")
                    Spacer(modifier = Modifier.height(6.dp))
                    EField(value = notes, onValueChange = { notes = it }, label = "Notes (optional)")

                    if (error != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(error!!, color = VoidColors.Danger, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    EBigButton("+ SAVE EXAM") {
                        val sid = subjectId
                        when {
                            sid == null -> error = "Select a subject"
                            dateText.isBlank() -> error = "Enter a date"
                            else -> {
                                try {
                                    val date = LocalDate.parse(dateText.trim())
                                    val time = if (timeText.isBlank()) null else LocalTime.parse(timeText.trim())
                                    VoidRepository.registerExam(
                                        examType = examType,
                                        subjectId = sid,
                                        date = date,
                                        time = time,
                                        session = session,
                                        location = location.trim(),
                                        unitIds = selectedUnitIds.toList(),
                                        grades = selectedGrades.toList(),
                                        notes = notes.trim()
                                    )
                                    dateText = ""; timeText = ""; location = ""; notes = ""
                                    selectedUnitIds = emptySet(); selectedGrades = emptySet(); error = null
                                } catch (e: Exception) {
                                    error = "Date must be YYYY-MM-DD, time must be HH:MM"
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("UPCOMING EXAMS", color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        val upcoming = VoidRepository.upcomingExamSubjects()
        if (upcoming.isEmpty()) {
            item { Text("No upcoming exams.", color = VoidColors.TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
        }

        items(upcoming) { es -> ExamSubjectRow(es); Spacer(modifier = Modifier.height(8.dp)) }

        item { Spacer(modifier = Modifier.height(90.dp)) }
    }
}

@Composable
private fun ExamSubjectRow(es: ExamSubject) {
    val exam = VoidRepository.examFor(es)
    val status = es.status()
    val urgent = es.isUrgent()

    EVoidCard(borderColor = if (urgent) VoidColors.Warning else VoidColors.Border) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${exam?.examType?.name ?: ""} EXAM",
                    color = VoidColors.TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(VoidRepository.subjectName(es.subjectId), color = VoidColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = buildString {
                        append(es.date.toString())
                        if (es.time != null) append(" \u00b7 ${es.time}")
                        if (es.session != null) append(" \u00b7 ${es.session.name}")
                        if (es.unitIds.isNotEmpty()) append(" \u00b7 ${es.unitIds.size} units")
                    },
                    color = VoidColors.TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = status.label(es.daysRemaining()),
                    color = examCountdownColor(status, es.daysRemaining()),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = VoidColors.TextSecondary,
                    modifier = Modifier
                        .clickable { exam?.let { VoidRepository.deleteExam(it.id) } }
                        .padding(top = 6.dp)
                        .height(16.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// DROPDOWNS + SHARED
// ---------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EExamTypeDropdown(selected: ExamType, onSelected: (ExamType) -> Unit) {
    EDropdownBase(label = "${selected.name} EXAM") { close ->
        ExamType.entries.forEach { t -> DropdownMenuItem(text = { Text("${t.name} EXAM") }, onClick = { onSelected(t); close() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ESubjectDropdown(selectedId: String?, onSelected: (String) -> Unit) {
    val name = VoidRepository.subjects.find { it.id == selectedId }?.name ?: "Select subject"
    EDropdownBase(label = name) { close ->
        VoidRepository.subjects.forEach { s -> DropdownMenuItem(text = { Text(s.name) }, onClick = { onSelected(s.id); close() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ESessionDropdown(selected: ExamSession?, onSelected: (ExamSession?) -> Unit) {
    EDropdownBase(label = selected?.name ?: "Session (optional)") { close ->
        DropdownMenuItem(text = { Text("None") }, onClick = { onSelected(null); close() })
        ExamSession.entries.forEach { s -> DropdownMenuItem(text = { Text(s.name) }, onClick = { onSelected(s); close() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EDropdownBase(label: String, items: @Composable (close: () -> Unit) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(VoidColors.Surface2)
                .border(1.dp, VoidColors.Border, RoundedCornerShape(6.dp))
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth()
        ) {
            Text(label, color = VoidColors.TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = VoidColors.TextSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) { items { open = false } }
    }
}

@Composable
private fun EVoidCard(borderColor: androidx.compose.ui.graphics.Color = VoidColors.Border, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VoidColors.Surface)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun EField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = VoidColors.TextPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VoidColors.Accent,
            unfocusedBorderColor = VoidColors.Border,
            focusedLabelColor = VoidColors.Accent,
            unfocusedLabelColor = VoidColors.TextSecondary,
            cursorColor = VoidColors.Accent,
            focusedTextColor = VoidColors.TextPrimary,
            unfocusedTextColor = VoidColors.TextPrimary
        )
    )
}

@Composable
private fun EBigButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(VoidColors.Cyan.copy(alpha = 0.12f))
            .border(1.dp, VoidColors.Cyan, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(text, color = VoidColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}
