package com.core.voidapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.core.voidapp.data.ClassPeriod
import com.core.voidapp.data.ExamType
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.report.AttentionLevel
import com.core.voidapp.data.report.DailyClassReport
import com.core.voidapp.data.report.DailyReportEngine
import com.core.voidapp.data.report.DailyReportStatus
import com.core.voidapp.data.report.DifficultyLevel
import com.core.voidapp.data.report.YesNoPartial
import java.time.LocalDate

private enum class ReportStep(val label: String) {
    SCHOOL("SCHOOL"), AFTERNOON("AFTERNOON"), STUDY("STUDY"), PERFORMANCE("PERFORMANCE"), GUARDIAN("GUARDIAN"), TOMORROW("TOMORROW")
}

@Composable
fun DailyReportScreen(onClose: () -> Unit) {
    val today = remember { LocalDate.now() }
    val reportId = remember { VoidRepository.getOrCreateDailyReport(today).id }
    val report = VoidRepository.dailyReports.find { it.id == reportId }
    var step by remember { mutableStateOf(ReportStep.SCHOOL) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("DAILY REPORT", color = VoidColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(
                    "$today \u00b7 ${report?.status?.name ?: DailyReportStatus.NOT_STARTED.name}",
                    color = VoidColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                )
            }
            Icon(Icons.Default.Close, contentDescription = "Close", tint = VoidColors.TextSecondary, modifier = Modifier.clickable { onClose() })
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ReportStep.entries.forEach { s ->
                StepChip(s.label, selected = step == s, modifier = Modifier.weight(1f)) { step = s }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f, fill = true)) {
            when (step) {
                ReportStep.SCHOOL -> SchoolClassesStep(reportId, today)
                ReportStep.AFTERNOON -> AfternoonStep(today)
                else -> ComingSoonStep(step.label)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (report?.status != DailyReportStatus.COMPLETED) {
                ReportActionPill("MARK COMPLETE", VoidColors.Success, modifier = Modifier.weight(1f)) {
                    VoidRepository.setDailyReportStatus(reportId, DailyReportStatus.COMPLETED)
                }
            }
            ReportActionPill("SAVE & EXIT", VoidColors.Surface2, textColor = VoidColors.TextPrimary, modifier = Modifier.weight(1f)) { onClose() }
        }
    }
}

// =====================================================================
// SCHOOL CLASSES — fully functional, per spec section 3
// =====================================================================

@Composable
private fun SchoolClassesStep(reportId: String, date: LocalDate) {
    val periods = DailyReportEngine.classPeriodsFor(date)

    if (periods.isEmpty()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            VoidCard { Text("No classes registered for today.", color = VoidColors.TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(periods) { period -> ClassReportCard(reportId, period) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ClassReportCard(reportId: String, period: ClassPeriod) {
    val existing = VoidRepository.classReportsFor(reportId).find { it.classPeriodId == period.id }
    val subjectName = VoidRepository.subjectName(period.subjectId)

    var attendance by remember(existing) { mutableStateOf(existing?.attendance ?: YesNoPartial.YES) }
    var understanding by remember(existing) { mutableStateOf(existing?.understanding ?: YesNoPartial.YES) }
    var difficulty by remember(existing) { mutableStateOf(existing?.difficulty ?: DifficultyLevel.MEDIUM) }
    var attention by remember(existing) { mutableStateOf(existing?.attention ?: AttentionLevel.MOSTLY) }
    var tookNotes by remember(existing) { mutableStateOf(existing?.tookNotes ?: YesNoPartial.YES) }
    var difficultyReason by remember(existing) { mutableStateOf(existing?.difficultyReason ?: "") }
    var importantContent by remember(existing) { mutableStateOf(existing?.importantContent ?: "") }

    var hasAssignment by remember { mutableStateOf(false) }
    var assignmentTitle by remember { mutableStateOf("") }
    var assignmentDeadline by remember { mutableStateOf("") }
    var assignmentSaved by remember { mutableStateOf(false) }

    var hasTestPrep by remember { mutableStateOf(false) }
    var testExamType by remember { mutableStateOf(ExamType.MID) }
    var testExpectedDate by remember { mutableStateOf("") }
    var testNote by remember { mutableStateOf("") }
    var testSaved by remember { mutableStateOf(false) }

    VoidCard(borderColor = if (existing != null) VoidColors.Success else VoidColors.Border) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("P${period.periodNumber}", color = VoidColors.Accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
            Text(subjectName, color = VoidColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(10.dp))

        ReportQuestion("Did you attend this class?")
        ChipRow(YesNoPartial.entries, attendance) { attendance = it }
        Spacer(modifier = Modifier.height(8.dp))

        ReportQuestion("Did you learn/understand what was taught?")
        ChipRow(YesNoPartial.entries, understanding) { understanding = it }
        Spacer(modifier = Modifier.height(8.dp))

        ReportQuestion("How difficult was this subject today?")
        ChipRow(DifficultyLevel.entries, difficulty) { difficulty = it }
        if (difficulty != DifficultyLevel.EASY) {
            Spacer(modifier = Modifier.height(6.dp))
            ReportTextField(difficultyReason, { difficultyReason = it }, "What made it difficult? (optional)")
        }
        Spacer(modifier = Modifier.height(8.dp))

        ReportQuestion("How well did you pay attention?")
        ChipRow(AttentionLevel.entries, attention) { attention = it }
        Spacer(modifier = Modifier.height(8.dp))

        ReportQuestion("Did you take notes?")
        ChipRow(YesNoPartial.entries, tookNotes) { tookNotes = it }
        Spacer(modifier = Modifier.height(8.dp))

        ReportQuestion("What important topic or content was covered today? (optional)")
        ReportTextField(importantContent, { importantContent = it }, "e.g. Chapter 4 \u2014 quadratic equations")
        Spacer(modifier = Modifier.height(10.dp))

        ReportActionPill(if (existing != null) "UPDATE" else "SAVE", VoidColors.Cyan, textColor = Color.Black) {
            VoidRepository.saveDailyClassReport(
                DailyClassReport(
                    id = existing?.id ?: VoidRepository.newId(),
                    dailyReportId = reportId,
                    classPeriodId = period.id,
                    attendance = attendance,
                    understanding = understanding,
                    difficulty = difficulty,
                    attention = attention,
                    tookNotes = tookNotes,
                    importantContent = importantContent.trim(),
                    difficultyReason = difficultyReason.trim()
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        ReportQuestion("Did the teacher give you an assignment or homework?")
        ChipRow(listOf(false, true), hasAssignment, labels = listOf("No", "Yes")) { hasAssignment = it }
        if (hasAssignment) {
            Spacer(modifier = Modifier.height(6.dp))
            ReportTextField(assignmentTitle, { assignmentTitle = it; assignmentSaved = false }, "What's the assignment?")
            Spacer(modifier = Modifier.height(6.dp))
            ReportTextField(assignmentDeadline, { assignmentDeadline = it; assignmentSaved = false }, "Deadline (YYYY-MM-DD, if known)")
            Spacer(modifier = Modifier.height(6.dp))
            ReportActionPill(
                if (assignmentSaved) "SAVED \u2713" else "SAVE ASSIGNMENT",
                if (assignmentSaved) VoidColors.Surface2 else VoidColors.Purple,
                textColor = if (assignmentSaved) VoidColors.TextSecondary else Color.White
            ) {
                if (assignmentTitle.isNotBlank()) {
                    val deadline = runCatching { LocalDate.parse(assignmentDeadline.trim()) }.getOrDefault(LocalDate.now().plusDays(3))
                    VoidRepository.addAssignmentFromDailyReport(reportId, assignmentTitle.trim(), period.subjectId, deadline, "")
                    assignmentSaved = true
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        ReportQuestion("Did the teacher tell you to prepare for a test or exam?")
        ChipRow(listOf(false, true), hasTestPrep, labels = listOf("No", "Yes")) { hasTestPrep = it }
        if (hasTestPrep) {
            Spacer(modifier = Modifier.height(6.dp))
            ChipRow(ExamType.entries, testExamType) { testExamType = it }
            Spacer(modifier = Modifier.height(6.dp))
            ReportTextField(testExpectedDate, { testExpectedDate = it; testSaved = false }, "Expected date (YYYY-MM-DD, if known)")
            Spacer(modifier = Modifier.height(6.dp))
            ReportTextField(testNote, { testNote = it; testSaved = false }, "Topics/units or notes")
            Spacer(modifier = Modifier.height(6.dp))
            ReportActionPill(
                if (testSaved) "SAVED \u2713" else "SAVE TEST PREP",
                if (testSaved) VoidColors.Surface2 else VoidColors.Purple,
                textColor = if (testSaved) VoidColors.TextSecondary else Color.White
            ) {
                val expectedDate = testExpectedDate.trim().takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                VoidRepository.addTestPrepFromDailyReport(reportId, period.subjectId, testExamType, expectedDate, testNote.trim())
                testSaved = true
            }
        }
    }
}

// =====================================================================
// AFTERNOON — Language/Lab detection only, per spec section 4
// =====================================================================

@Composable
private fun AfternoonStep(date: LocalDate) {
    val dClassPeriods = DailyReportEngine.languageOrLabPeriodsFor(date)
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (dClassPeriods.isEmpty()) {
            VoidCard {
                Text(
                    "No Language/Lab session today \u2014 this afternoon is available for study, covered in the Study step.",
                    color = VoidColors.TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace
                )
            }
        } else {
            dClassPeriods.forEach { period ->
                Text(period.classType.name, color = VoidColors.Accent, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                ClassReportCard(VoidRepository.getOrCreateDailyReport(date).id, period)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ComingSoonStep(label: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        VoidCard {
            Text("$label report isn't built yet.", color = VoidColors.TextPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "This step is next in line for the Daily Report system. School Classes is fully working \u2014 answer those now, and this section will pick up from real data once it's built.",
                color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace
            )
        }
    }
}

// =====================================================================
// Shared small pieces
// =====================================================================

@Composable
private fun ReportQuestion(text: String) {
    Text(text, color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun ReportTextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = VoidColors.TextPrimary),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoidColors.Accent, unfocusedBorderColor = VoidColors.Border, cursorColor = VoidColors.Accent)
    )
}

@Composable
private fun <T> ChipRow(options: List<T>, selected: T, labels: List<String>? = null, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { idx, option ->
            val label = labels?.getOrNull(idx) ?: option.toString()
            val isSelected = option == selected
            Text(
                text = label,
                color = if (isSelected) VoidColors.TextPrimary else VoidColors.TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) VoidColors.Accent.copy(alpha = 0.2f) else VoidColors.Surface2)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun StepChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) VoidColors.Purple.copy(alpha = 0.2f) else VoidColors.Surface2)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) VoidColors.TextPrimary else VoidColors.TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ReportActionPill(text: String, color: Color, textColor: Color = Color.Black, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
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
