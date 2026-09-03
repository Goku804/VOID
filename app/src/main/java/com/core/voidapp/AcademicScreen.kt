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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.core.voidapp.data.AssessmentType
import com.core.voidapp.data.Exam
import com.core.voidapp.data.ExamType
import com.core.voidapp.data.Subject
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.countdown
import com.core.voidapp.data.totalWeight
import com.core.voidapp.data.weightedTotal
import java.time.LocalDate

private val ABlack = Color(0xFF050505)
private val APanel = Color(0xFF0D0D0D)
private val APanel2 = Color(0xFF121212)
private val ABorder = Color(0xFF242424)
private val AText = Color(0xFFF2F2F2)
private val AMuted = Color(0xFF888888)
private val AAccent = Color(0xFF00E676)
private val AWarn = Color(0xFFFF3D3D)

@Composable
fun AcademicScreenReal() {
    var section by remember { mutableStateOf(0) } // 0 = subjects/marks, 1 = exams

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ABlack)
            .padding(16.dp)
    ) {
        Text("VOID", color = AAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text("ACADEMIC", color = AText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("SUBJECTS \u2022 MARKS \u2022 EXAMS", color = AMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ATab("MARKS", section == 0, Modifier.weight(1f)) { section = 0 }
            ATab("EXAMS", section == 1, Modifier.weight(1f)) { section = 1 }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (section) {
            0 -> MarksSection()
            1 -> ExamsSection()
        }
    }
}

@Composable
private fun ATab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) AAccent.copy(alpha = 0.12f) else APanel)
            .border(1.dp, if (selected) AAccent else ABorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = if (selected) AAccent else AMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------
// MARKS
// ---------------------------------------------------------------------

@Composable
private fun MarksSection() {
    if (VoidRepository.subjects.isEmpty()) {
        EmptyHint("No subjects yet. Add subjects in SETUP first.")
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(VoidRepository.subjects) { subject ->
            SubjectMarksCard(subject)
            Spacer(modifier = Modifier.height(12.dp))
        }
        item { Spacer(modifier = Modifier.height(30.dp)) }
    }
}

@Composable
private fun SubjectMarksCard(subject: Subject) {
    APanelCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(subject.name, color = AText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Grade ${subject.grade}", color = AMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            val graded = subject.assessmentTypes.count { it.entry != null }
            val total = subject.assessmentTypes.size
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    text = "${subject.weightedTotal().let { "%.1f".format(it) }}%",
                    color = AAccent,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text("$graded/$total graded", color = AMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }

        if (subject.assessmentTypes.isEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No assessment types set. Add them in SETUP.",
                color = AMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            return@APanelCard
        }

        Spacer(modifier = Modifier.height(10.dp))

        subject.assessmentTypes.forEach { type ->
            MarkEntryRow(subject, type)
        }
    }
}

@Composable
private fun MarkEntryRow(subject: Subject, type: AssessmentType) {
    var editing by remember { mutableStateOf(false) }
    var scoreText by remember(type.id) { mutableStateOf(type.entry?.score?.toString() ?: "") }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { editing = !editing },
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = type.label,
                color = AText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${type.weightPercent.toInt()}%",
                color = AMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = type.entry?.let { "${it.score.toInt()}/${type.maxScore.toInt()}" } ?: "\u2014 / ${type.maxScore.toInt()}",
                color = if (type.entry != null) AAccent else AMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        if (editing) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                AField(
                    value = scoreText,
                    onValueChange = { scoreText = it },
                    label = "Score",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ASmallButton("SAVE") {
                    val s = scoreText.toDoubleOrNull()
                    if (s != null) {
                        VoidRepository.recordScore(subject.id, type.id, s)
                        editing = false
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// EXAMS
// ---------------------------------------------------------------------

@Composable
private fun ExamsSection() {
    var subjectId by remember { mutableStateOf<String?>(null) }
    var examType by remember { mutableStateOf(ExamType.MID) }
    var title by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        item {
            APanelCard {
                Text("ADD EXAM", color = AAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                if (VoidRepository.subjects.isEmpty()) {
                    Text("Add a subject in SETUP first.", color = AMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                } else {
                    ASubjectDropdown(selectedId = subjectId, onSelected = { subjectId = it })
                    Spacer(modifier = Modifier.height(6.dp))
                    AExamTypeDropdown(selected = examType, onSelected = { examType = it })
                    Spacer(modifier = Modifier.height(6.dp))
                    AField(value = title, onValueChange = { title = it }, label = "Title (e.g. Unit 3 Mid)")
                    Spacer(modifier = Modifier.height(6.dp))
                    AField(value = dateText, onValueChange = { dateText = it }, label = "Date (YYYY-MM-DD)")

                    if (error != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(error!!, color = AWarn, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    ABigButton("+ ADD EXAM") {
                        val sid = subjectId
                        if (sid == null) {
                            error = "Select a subject"
                        } else if (title.isBlank()) {
                            error = "Enter a title"
                        } else {
                            try {
                                val date = LocalDate.parse(dateText.trim())
                                VoidRepository.addExam(sid, examType, title.trim(), date)
                                title = ""
                                dateText = ""
                                error = null
                            } catch (e: Exception) {
                                error = "Date must be YYYY-MM-DD"
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("UPCOMING EXAMS", color = AMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        val upcoming = VoidRepository.upcomingExams()
        if (upcoming.isEmpty()) {
            item {
                Text("No upcoming exams.", color = AMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        items(upcoming) { exam ->
            ExamRow(exam)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }
}

@Composable
private fun ExamRow(exam: Exam) {
    val cd = exam.countdown()
    APanelCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(exam.title, color = AText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${VoidRepository.subjectName(exam.subjectId)} \u00b7 ${exam.type.name}",
                    color = AMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = if (cd.totalDays == 0L) "TODAY" else "${cd.totalDays}d",
                color = AAccent,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ASubjectDropdown(selectedId: String?, onSelected: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val name = VoidRepository.subjects.find { it.id == selectedId }?.name ?: "Select subject"
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(APanel2)
                .border(1.dp, ABorder, RoundedCornerShape(6.dp))
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth()
        ) {
            Text(name, color = AText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AMuted)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            VoidRepository.subjects.forEach { subject ->
                DropdownMenuItem(text = { Text(subject.name) }, onClick = { onSelected(subject.id); open = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AExamTypeDropdown(selected: ExamType, onSelected: (ExamType) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(APanel2)
                .border(1.dp, ABorder, RoundedCornerShape(6.dp))
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth()
        ) {
            Text(selected.name, color = AText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AMuted)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ExamType.entries.forEach { type ->
                DropdownMenuItem(text = { Text(type.name) }, onClick = { onSelected(type); open = false })
            }
        }
    }
}

// ---------------------------------------------------------------------
// SHARED
// ---------------------------------------------------------------------

@Composable
private fun EmptyHint(text: String) {
    Column(modifier = Modifier.fillMaxSize().padding(top = 20.dp)) {
        Text(text, color = AMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun APanelCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(APanel)
            .border(1.dp, ABorder, RoundedCornerShape(10.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun AField(
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
        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = AText),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AAccent,
            unfocusedBorderColor = ABorder,
            focusedLabelColor = AAccent,
            unfocusedLabelColor = AMuted,
            cursorColor = AAccent,
            focusedTextColor = AText,
            unfocusedTextColor = AText
        )
    )
}

@Composable
private fun ABigButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AAccent.copy(alpha = 0.12f))
            .border(1.dp, AAccent, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = AAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ASmallButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AAccent.copy(alpha = 0.15f))
            .border(1.dp, AAccent, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, color = AAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
