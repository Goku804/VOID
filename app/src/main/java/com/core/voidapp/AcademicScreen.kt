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
import com.core.voidapp.data.AcademicUnit
import com.core.voidapp.data.AssessmentKind
import com.core.voidapp.data.AssessmentType
import com.core.voidapp.data.Subject
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.totalWeight
import com.core.voidapp.data.weightedTotal

private val ABlack = Color(0xFF050505)
private val APanel = Color(0xFF0D0D0D)
private val APanel2 = Color(0xFF121212)
private val ABorder = Color(0xFF242424)
private val AText = Color(0xFFF2F2F2)
private val AMuted = Color(0xFF888888)
private val AAccent = Color(0xFF00E676)
private val AWarn = Color(0xFFFF3D3D)

private enum class AcademicTab { SUBJECTS, UNITS, MARKS }

@Composable
fun AcademicScreenReal() {
    var tab by remember { mutableStateOf(AcademicTab.SUBJECTS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ABlack)
            .padding(16.dp)
    ) {
        Text("VOID", color = AAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text("ACADEMIC", color = AText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("SUBJECTS \u2022 UNITS \u2022 MARKS", color = AMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ATab("SUBJ", tab == AcademicTab.SUBJECTS, Modifier.weight(1f)) { tab = AcademicTab.SUBJECTS }
            ATab("UNITS", tab == AcademicTab.UNITS, Modifier.weight(1f)) { tab = AcademicTab.UNITS }
            ATab("MARKS", tab == AcademicTab.MARKS, Modifier.weight(1f)) { tab = AcademicTab.MARKS }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (tab) {
            AcademicTab.SUBJECTS -> SubjectsTab()
            AcademicTab.UNITS -> UnitsTab()
            AcademicTab.MARKS -> MarksSection()
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
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------
// SUBJECTS
// ---------------------------------------------------------------------

@Composable
private fun SubjectsTab() {
    var name by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            APanelCard {
                Text("REGISTER SUBJECT", color = AAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                AField(value = name, onValueChange = { name = it }, label = "Subject name (e.g. Mathematics)")
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AField(value = grade, onValueChange = { grade = it }, label = "Grade", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    AField(value = code, onValueChange = { code = it.uppercase() }, label = "Code (e.g. MATH)", modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                ABigButton("+ SAVE SUBJECT") {
                    val g = grade.toIntOrNull()
                    if (name.isNotBlank() && g != null) {
                        VoidRepository.addSubject(name.trim(), g, code.trim())
                        name = ""; grade = ""; code = ""
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("YOUR SUBJECTS", color = AMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (VoidRepository.subjects.isEmpty()) {
            item { Text("No subjects yet.", color = AMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
        }

        items(VoidRepository.subjects) { subject ->
            APanelCard {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(subject.name, color = AText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Grade ${subject.grade}" + if (subject.code.isNotBlank()) " \u00b7 ${subject.code}" else "",
                            color = AMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "${VoidRepository.unitsFor(subject.id).size} units",
                        color = AMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        item { Spacer(modifier = Modifier.height(90.dp)) }
    }
}

// ---------------------------------------------------------------------
// UNITS
// ---------------------------------------------------------------------

@Composable
private fun UnitsTab() {
    var subjectId by remember { mutableStateOf<String?>(null) }
    var unitNumber by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }

    if (VoidRepository.subjects.isEmpty()) {
        EmptyHint("No subjects yet. Add one in the SUBJ tab first.")
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            APanelCard {
                Text("REGISTER UNIT", color = AAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                ASubjectDropdown(selectedId = subjectId, onSelected = { subjectId = it })
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AField(value = unitNumber, onValueChange = { unitNumber = it }, label = "Unit #", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    AField(value = minutes, onValueChange = { minutes = it }, label = "Est. minutes", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
                AField(value = name, onValueChange = { name = it }, label = "Unit name (e.g. Functions)")
                Spacer(modifier = Modifier.height(6.dp))
                AField(value = description, onValueChange = { description = it }, label = "Description (optional)")

                Spacer(modifier = Modifier.height(10.dp))
                ABigButton("+ SAVE UNIT") {
                    val sid = subjectId
                    val num = unitNumber.toIntOrNull()
                    val mins = minutes.toIntOrNull() ?: 0
                    if (sid != null && num != null && name.isNotBlank()) {
                        VoidRepository.addUnit(sid, num, name.trim(), description.trim(), mins)
                        unitNumber = ""; name = ""; description = ""; minutes = ""
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(VoidRepository.subjects) { subject ->
            val subjectUnits = VoidRepository.unitsFor(subject.id)
            if (subjectUnits.isNotEmpty()) {
                Text(subject.name.uppercase(), color = AMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                APanelCard {
                    subjectUnits.forEachIndexed { idx, unit ->
                        UnitRow(unit)
                        if (idx != subjectUnits.lastIndex) Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        item { Spacer(modifier = Modifier.height(90.dp)) }
    }
}

@Composable
private fun UnitRow(unit: AcademicUnit) {
    Column {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("U${unit.unitNumber}", color = AAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
            Text(unit.name, color = AText, fontSize = 13.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            if (unit.estimatedStudyMinutes > 0) {
                Text("${unit.estimatedStudyMinutes}min", color = AMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
        if (unit.description.isNotBlank()) {
            Text(unit.description, color = AMuted, fontSize = 10.sp, modifier = Modifier.padding(start = 32.dp))
        }
    }
}

// ---------------------------------------------------------------------
// MARKS
// ---------------------------------------------------------------------

@Composable
private fun MarksSection() {
    if (VoidRepository.subjects.isEmpty()) {
        EmptyHint("No subjects yet. Add subjects in the SUBJ tab first.")
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(VoidRepository.subjects) { subject ->
            SubjectMarksCard(subject)
            Spacer(modifier = Modifier.height(12.dp))
        }
        item { Spacer(modifier = Modifier.height(90.dp)) }
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
                text = "No assessment types set yet.",
                color = AMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        subject.assessmentTypes.forEach { type ->
            MarkEntryRow(subject, type)
        }

        Spacer(modifier = Modifier.height(8.dp))
        AddAssessmentInline(subject)
    }
}

@Composable
private fun AddAssessmentInline(subject: Subject) {
    var expanded by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf(AssessmentKind.TEST) }
    var label by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var maxScore by remember { mutableStateOf("") }

    Text(
        text = if (expanded) "\u2212 CANCEL" else "+ ADD ASSESSMENT TYPE",
        color = AAccent,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable { expanded = !expanded }
    )

    if (expanded) {
        Spacer(modifier = Modifier.height(8.dp))
        AAssessmentKindDropdown(selected = kind, onSelected = { kind = it })
        Spacer(modifier = Modifier.height(6.dp))
        AField(value = label, onValueChange = { label = it }, label = "Label (e.g. Chapter 3 Test)")
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AField(value = weight, onValueChange = { weight = it }, label = "Weight %", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
            AField(value = maxScore, onValueChange = { maxScore = it }, label = "Max score", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        ASmallButton("+ ADD") {
            val w = weight.toDoubleOrNull()
            val m = maxScore.toDoubleOrNull()
            if (label.isNotBlank() && w != null && m != null) {
                VoidRepository.addAssessmentType(subject.id, kind, label.trim(), w, m)
                label = ""; weight = ""; maxScore = ""; expanded = false
            }
        }
    }
}

@Composable
private fun MarkEntryRow(subject: Subject, type: AssessmentType) {
    var editing by remember { mutableStateOf(false) }
    var scoreText by remember(type.id) { mutableStateOf(type.entry?.score?.toString() ?: "") }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { editing = !editing },
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(type.label, color = AText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Text("${type.weightPercent.toInt()}%", color = AMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(end = 8.dp))
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
                AField(value = scoreText, onValueChange = { scoreText = it }, label = "Score", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
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
// SHARED DROPDOWNS
// ---------------------------------------------------------------------

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
            if (VoidRepository.subjects.isEmpty()) {
                DropdownMenuItem(text = { Text("Add a subject first") }, onClick = { open = false })
            }
            VoidRepository.subjects.forEach { subject ->
                DropdownMenuItem(text = { Text(subject.name) }, onClick = { onSelected(subject.id); open = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AAssessmentKindDropdown(selected: AssessmentKind, onSelected: (AssessmentKind) -> Unit) {
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
            Text(selected.name.replace('_', ' '), color = AText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AMuted)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            AssessmentKind.entries.forEach { kind ->
                DropdownMenuItem(text = { Text(kind.name.replace('_', ' ')) }, onClick = { onSelected(kind); open = false })
            }
        }
    }
}

// ---------------------------------------------------------------------
// SHARED
// ---------------------------------------------------------------------

@Composable
private fun EmptyHint(text: String) {
    Column(modifier = Modifier.fillMaxSize().padding(top = 20.dp, start = 16.dp, end = 16.dp)) {
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
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ASmallButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(VoidColors.Cyan.copy(alpha = 0.15f))
            .border(1.dp, VoidColors.Cyan, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, color = VoidColors.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
