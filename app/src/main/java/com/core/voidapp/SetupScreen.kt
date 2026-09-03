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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.core.voidapp.data.AssessmentType
import com.core.voidapp.data.DayOfWeekVoid
import com.core.voidapp.data.ExamType
import com.core.voidapp.data.Subject
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.totalWeight

private val SBlack = Color(0xFF050505)
private val SPanel = Color(0xFF0B0F0D)
private val SPanel2 = Color(0xFF101512)
private val SBorder = Color(0xFF1E2A24)
private val SText = Color(0xFFF2F2F2)
private val SMuted = Color(0xFF7A8B84)
private val SAccent = Color(0xFF00E676)
private val SWarn = Color(0xFFFF5252)

@Composable
fun SetupScreen() {
    var section by remember { mutableStateOf(0) } // 0 = subjects, 1 = schedule

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SBlack)
            .padding(16.dp)
    ) {
        Text(
            text = "VOID",
            color = SAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "SETUP",
            color = SText,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "SUBJECTS \u2022 SCHEDULE \u2022 WEIGHTS",
            color = SMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SetupTab("SUBJECTS", section == 0, Modifier.weight(1f)) { section = 0 }
            SetupTab("SCHEDULE", section == 1, Modifier.weight(1f)) { section = 1 }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (section) {
            0 -> SubjectsSetup()
            1 -> ScheduleSetup()
        }
    }
}

@Composable
private fun SetupTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) SAccent.copy(alpha = 0.12f) else SPanel)
            .border(1.dp, if (selected) SAccent else SBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
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
// SUBJECTS
// ---------------------------------------------------------------------

@Composable
private fun SubjectsSetup() {
    var name by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        item {
            SetupPanel {
                Text("ADD SUBJECT", color = SAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                VoidTextField(value = name, onValueChange = { name = it }, label = "Subject name")
                Spacer(modifier = Modifier.height(8.dp))
                VoidTextField(value = grade, onValueChange = { grade = it }, label = "Grade", keyboardType = KeyboardType.Number)

                Spacer(modifier = Modifier.height(10.dp))

                VoidButton("+ ADD SUBJECT") {
                    val g = grade.toIntOrNull()
                    if (name.isNotBlank() && g != null) {
                        VoidRepository.addSubject(name.trim(), g)
                        name = ""
                        grade = ""
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("YOUR SUBJECTS", color = SMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (VoidRepository.subjects.isEmpty()) {
            item {
                Text("No subjects yet.", color = SMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        items(VoidRepository.subjects) { subject ->
            SubjectWeightCard(subject)
            Spacer(modifier = Modifier.height(12.dp))
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }
}

@Composable
private fun SubjectWeightCard(subject: Subject) {
    var examType by remember { mutableStateOf(ExamType.TEST) }
    var label by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var maxScore by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    SetupPanel {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(subject.name, color = SText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Grade ${subject.grade}", color = SMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            val total = subject.totalWeight()
            Text(
                text = "${total.toInt()}%",
                color = if (total == 100.0) SAccent else SWarn,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        if (subject.totalWeight() != 100.0 && subject.assessmentTypes.isNotEmpty()) {
            Text(
                text = "Weights should total 100%",
                color = SWarn,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        subject.assessmentTypes.forEach { type ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            ) {
                Text(type.label, color = SText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text("${type.weightPercent.toInt()}%  /  max ${type.maxScore.toInt()}", color = SMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (expanded) "\u2212 CANCEL" else "+ ADD ASSESSMENT TYPE",
            color = SAccent,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { expanded = !expanded }
        )

        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))

            ExamTypeDropdown(selected = examType, onSelected = { examType = it })
            Spacer(modifier = Modifier.height(6.dp))
            VoidTextField(value = label, onValueChange = { label = it }, label = "Label (e.g. Mid Exam)")
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoidTextField(value = weight, onValueChange = { weight = it }, label = "Weight %", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                VoidTextField(value = maxScore, onValueChange = { maxScore = it }, label = "Max score", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            VoidButton("+ ADD") {
                val w = weight.toDoubleOrNull()
                val m = maxScore.toDoubleOrNull()
                if (label.isNotBlank() && w != null && m != null) {
                    VoidRepository.addAssessmentType(subject.id, examType, label.trim(), w, m)
                    label = ""
                    weight = ""
                    maxScore = ""
                    expanded = false
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamTypeDropdown(selected: ExamType, onSelected: (ExamType) -> Unit) {
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
            ExamType.entries.forEach { type ->
                DropdownMenuItem(text = { Text(type.name) }, onClick = { onSelected(type); open = false })
            }
        }
    }
}

// ---------------------------------------------------------------------
// SCHEDULE
// ---------------------------------------------------------------------

@Composable
private fun ScheduleSetup() {
    var selectedDay by remember { mutableStateOf(DayOfWeekVoid.MONDAY) }
    var period by remember { mutableStateOf("") }
    var subjectId by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        item {
            SetupPanel {
                Text("ADD CLASS PERIOD", color = SAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                DayDropdown(selected = selectedDay, onSelected = { selectedDay = it })
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VoidTextField(value = period, onValueChange = { period = it }, label = "Period #", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.weight(1f)) {
                        SubjectDropdown(selectedId = subjectId, onSelected = { subjectId = it })
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                VoidButton("+ ADD PERIOD") {
                    val p = period.toIntOrNull()
                    if (p != null && subjectId != null) {
                        VoidRepository.addClassPeriod(selectedDay, p, subjectId!!)
                        period = ""
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("WEEKLY SCHEDULE", color = SMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
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
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text("P${p.periodNumber}", color = SMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(30.dp))
                            Text(VoidRepository.subjectName(p.subjectId), color = SText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }
}

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
    androidx.compose.foundation.layout.Column(
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
            .background(SAccent.copy(alpha = 0.12f))
            .border(1.dp, SAccent, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = SAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope
