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
import androidx.compose.runtime.LaunchedEffect
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
import com.core.voidapp.data.Exam
import com.core.voidapp.data.ExamSession
import com.core.voidapp.data.ExamSubject
import com.core.voidapp.data.ExamType
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.allUnitIds
import com.core.voidapp.data.daysRemaining
import com.core.voidapp.data.isUrgent
import com.core.voidapp.data.label
import com.core.voidapp.data.nearestUpcomingSubject
import com.core.voidapp.data.status
import com.core.voidapp.data.subjectsSorted
import java.time.LocalDate
import java.time.LocalTime

/**
 * Everything selected for one subject inside a single exam-registration
 * pass: which units it covers, and — for MID/FINAL/MOCK, which span
 * several days — its own date/time/session inside the overall period.
 * For TEST these date/time/session fields go unused; the shared fields
 * above the subject list apply to every subject instead.
 *
 * `date` is a real LocalDate, not free text: it's only ever set by
 * picking one of the days generated from the exam period below, so there
 * is nothing here left to parse or mistype.
 */
private data class SubjectExamDraft(
    val unitIds: Set<String> = emptySet(),
    val date: LocalDate? = null,
    val timeText: String = "",
    val session: ExamSession? = null
)

@Composable
fun ExamScheduleScreen() {
    var examType by remember { mutableStateOf(ExamType.TEST) }
    var subjectDrafts by remember { mutableStateOf(mapOf<String, SubjectExamDraft>()) }

    // TEST only — one date/time/session shared by every selected subject.
    var sharedDateText by remember { mutableStateOf("") }
    var sharedTimeText by remember { mutableStateOf("") }
    var sharedSession by remember { mutableStateOf<ExamSession?>(null) }

    // MID / FINAL / MOCK only — the overall period the exam spans; each
    // subject then gets its own date (below) inside that period.
    var startDateText by remember { mutableStateOf("") }
    var endDateText by remember { mutableStateOf("") }

    // MOCK only — which grade levels this mock covers.
    var selectedGrades by remember { mutableStateOf(setOf<Int>()) }

    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    // Every calendar date inclusive of the exam period, recalculated only
    // when the start/end text actually changes. Empty (and thus "no valid
    // period yet") whenever either field is blank, unparsable, or the
    // range is backwards.
    val periodDates = remember(startDateText, endDateText) {
        val start = startDateText.trim().let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val end = endDateText.trim().let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (start != null && end != null && !end.isBefore(start)) {
            generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
        } else {
            emptyList()
        }
    }

    // Error handling: if the period is redefined and a subject's already-
    // picked date no longer falls inside it, clear that selection instead
    // of silently keeping a now-invalid date attached to the subject.
    LaunchedEffect(periodDates) {
        if (examType != ExamType.TEST && subjectDrafts.values.any { it.date != null && it.date !in periodDates }) {
            val validDates = periodDates.toSet()
            subjectDrafts = subjectDrafts.mapValues { (_, d) ->
                if (d.date != null && d.date !in validDates) d.copy(date = null) else d
            }
        }
    }

    fun toggleSubject(subjectId: String) {
        subjectDrafts = if (subjectDrafts.containsKey(subjectId)) {
            subjectDrafts - subjectId
        } else {
            subjectDrafts + (subjectId to SubjectExamDraft())
        }
    }

    fun updateDraft(subjectId: String, update: (SubjectExamDraft) -> SubjectExamDraft) {
        val current = subjectDrafts[subjectId] ?: return
        subjectDrafts = subjectDrafts + (subjectId to update(current))
    }

    fun resetForm() {
        subjectDrafts = emptyMap()
        sharedDateText = ""; sharedTimeText = ""; sharedSession = null
        startDateText = ""; endDateText = ""
        selectedGrades = emptySet()
        location = ""; notes = ""
        error = null
    }

    fun save() {
        if (subjectDrafts.isEmpty()) {
            error = "Select at least one subject"
            return
        }
        try {
            var sharedExamId: String? = null

            if (examType == ExamType.TEST) {
                if (sharedDateText.isBlank()) {
                    error = "Enter a date"
                    return
                }
                val date = LocalDate.parse(sharedDateText.trim())
                val time = sharedTimeText.trim().takeIf { it.isNotBlank() }?.let { LocalTime.parse(it) }

                subjectDrafts.forEach { (subjectId, draft) ->
                    val examId = sharedExamId
                    if (examId == null) {
                        val created = VoidRepository.registerExam(
                            examType = ExamType.TEST, subjectId = subjectId, date = date, time = time,
                            session = sharedSession, location = location.trim(), unitIds = draft.unitIds.toList(),
                            notes = notes.trim()
                        )
                        sharedExamId = created.examId
                    } else {
                        VoidRepository.addExamSubject(examId, subjectId, date, time, sharedSession, location.trim(), draft.unitIds.toList())
                    }
                }
            } else {
                if (startDateText.isBlank() || endDateText.isBlank()) {
                    error = "Enter a start and end date for the exam period"
                    return
                }
                if (subjectDrafts.values.any { it.date == null }) {
                    error = "Pick a date for every selected subject"
                    return
                }
                val startDate = LocalDate.parse(startDateText.trim())
                val endDate = LocalDate.parse(endDateText.trim())

                subjectDrafts.forEach { (subjectId, draft) ->
                    val date = draft.date!!
                    val time = draft.timeText.trim().takeIf { it.isNotBlank() }?.let { LocalTime.parse(it) }
                    val examId = sharedExamId
                    if (examId == null) {
                        val created = VoidRepository.registerExam(
                            examType = examType, subjectId = subjectId, date = date, time = time,
                            session = draft.session, location = location.trim(), unitIds = draft.unitIds.toList(),
                            grades = selectedGrades.toList(), notes = notes.trim(),
                            startDate = startDate, endDate = endDate
                        )
                        sharedExamId = created.examId
                    } else {
                        VoidRepository.addExamSubject(examId, subjectId, date, time, draft.session, location.trim(), draft.unitIds.toList())
                    }
                }
            }
            resetForm()
        } catch (e: Exception) {
            error = "Dates must be YYYY-MM-DD, time must be HH:MM"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
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
                    Spacer(modifier = Modifier.height(10.dp))

                    if (examType == ExamType.TEST) {
                        Text("WHEN", color = VoidColors.TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EDateField(value = sharedDateText, onValueChange = { sharedDateText = it }, label = "Date", modifier = Modifier.weight(1f))
                            ETimeField(value = sharedTimeText, onValueChange = { sharedTimeText = it }, label = "Time (opt)", modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        ESessionDropdown(selected = sharedSession, onSelected = { sharedSession = it })
                    } else {
                        Text("EXAM PERIOD", color = VoidColors.TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EDateField(value = startDateText, onValueChange = { startDateText = it }, label = "Start date", modifier = Modifier.weight(1f))
                            EDateField(value = endDateText, onValueChange = { endDateText = it }, label = "End date", modifier = Modifier.weight(1f))
                        }
                        if (startDateText.isNotBlank() && endDateText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (periodDates.isNotEmpty()) "${periodDates.size} day(s) in this period" else "End date must be on or after the start date",
                                color = if (periodDates.isNotEmpty()) VoidColors.TextSecondary else VoidColors.Danger,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("SUBJECTS", color = VoidColors.TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    VoidRepository.subjects.forEach { s ->
                        val draft = subjectDrafts[s.id]
                        val checked = draft != null

                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { toggleSubject(s.id) }
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { toggleSubject(s.id) },
                                    colors = CheckboxDefaults.colors(checkedColor = VoidColors.Accent, uncheckedColor = VoidColors.TextSecondary)
                                )
                                Text(s.name, color = VoidColors.TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            }

                            if (draft != null) {
                                Column(modifier = Modifier.padding(start = 40.dp, bottom = 8.dp)) {
                                    val units = VoidRepository.unitsFor(s.id)
                                    if (units.isNotEmpty()) {
                                        Text("UNITS", color = VoidColors.TextSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        units.forEach { u ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    updateDraft(s.id) { d -> d.copy(unitIds = if (d.unitIds.contains(u.id)) d.unitIds - u.id else d.unitIds + u.id) }
                                                }
                                            ) {
                                                Checkbox(
                                                    checked = draft.unitIds.contains(u.id),
                                                    onCheckedChange = { c -> updateDraft(s.id) { d -> d.copy(unitIds = if (c) d.unitIds + u.id else d.unitIds - u.id) } },
                                                    colors = CheckboxDefaults.colors(checkedColor = VoidColors.Accent, uncheckedColor = VoidColors.TextSecondary)
                                                )
                                                Text("U${u.unitNumber} \u2014 ${u.name}", color = VoidColors.TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    if (examType != ExamType.TEST) {
                                        EExamPeriodDateDropdown(
                                            label = "${s.name} date",
                                            selected = draft.date,
                                            options = periodDates,
                                            onSelected = { picked -> updateDraft(s.id) { d -> d.copy(date = picked) } }
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        ETimeField(
                                            value = draft.timeText,
                                            onValueChange = { v -> updateDraft(s.id) { d -> d.copy(timeText = v) } },
                                            label = "Time (opt)"
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        ESessionDropdown(selected = draft.session, onSelected = { sel -> updateDraft(s.id) { d -> d.copy(session = sel) } })
                                    }
                                }
                            }
                        }
                    }

                    if (examType == ExamType.MOCK) {
                        Spacer(modifier = Modifier.height(8.dp))
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
                                        onCheckedChange = { checked2 -> selectedGrades = if (checked2) selectedGrades + g else selectedGrades - g },
                                        colors = CheckboxDefaults.colors(checkedColor = VoidColors.Accent, uncheckedColor = VoidColors.TextSecondary)
                                    )
                                    Text("G$g", color = VoidColors.TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    EField(value = location, onValueChange = { location = it }, label = "Location (optional)")
                    Spacer(modifier = Modifier.height(6.dp))
                    EField(value = notes, onValueChange = { notes = it }, label = "Notes (optional)")

                    if (error != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(error!!, color = VoidColors.Danger, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    EBigButton("+ SAVE EXAM") { save() }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("UPCOMING EXAMS", color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        val upcoming = VoidRepository.upcomingExams()
        if (upcoming.isEmpty()) {
            item { Text("No upcoming exams.", color = VoidColors.TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
        }

        items(upcoming, key = { it.id }) { exam ->
            ExamGroupCard(
                exam = exam,
                onDeleteExam = { VoidRepository.deleteExam(exam.id) },
                onDeleteSubject = { es -> VoidRepository.deleteExamSubject(es.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item { Spacer(modifier = Modifier.height(90.dp)) }
    }
}

// ---------------------------------------------------------------------
// SHARED GROUPED EXAM CARD — one Exam (Test/Mid/Final/Mock), every
// subject sitting inside it, grouped by date. Used here in the full
// "Upcoming Exams" list (with delete affordances) AND on the Home
// screen's "Next Exam" card (read-only, no delete callbacks passed).
// ---------------------------------------------------------------------

@Composable
fun ExamGroupCard(
    exam: Exam,
    onDeleteExam: (() -> Unit)? = null,
    onDeleteSubject: ((ExamSubject) -> Unit)? = null
) {
    val subjects = exam.subjectsSorted()
    val nearest = exam.nearestUpcomingSubject()
    val urgent = subjects.any { it.isUrgent() }
    val byDate = subjects.groupBy { it.date }.toSortedMap()

    EVoidCard(borderColor = if (urgent) VoidColors.Warning else VoidColors.Border) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${exam.examType.name} EXAM",
                color = VoidColors.Accent,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (nearest != null) {
                Text(
                    text = nearest.status().label(nearest.daysRemaining()),
                    color = examCountdownColor(nearest.status(), nearest.daysRemaining()),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            if (onDeleteExam != null) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete exam",
                    tint = VoidColors.TextSecondary,
                    modifier = Modifier.clickable { onDeleteExam() }.padding(start = 10.dp).height(16.dp)
                )
            }
        }

        if (exam.startDate != null && exam.endDate != null) {
            Text(
                text = "PERIOD: ${exam.startDate} \u2192 ${exam.endDate}",
                color = VoidColors.TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        byDate.forEach { (date, subjectsOnDate) ->
            val names = subjectsOnDate.joinToString(", ") { VoidRepository.subjectName(it.subjectId) }
            Text(
                text = "${date.dayOfWeek.name} \u00b7 $date",
                color = VoidColors.TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(names, color = VoidColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            subjectsOnDate.forEach { es ->
                val detail = buildString {
                    if (es.time != null) append("Starts ${es.time}")
                    if (es.session != null) {
                        if (isNotEmpty()) append(" \u00b7 ")
                        append(es.session.name)
                    }
                    if (es.unitIds.isNotEmpty()) {
                        if (isNotEmpty()) append(" \u00b7 ")
                        append("${es.unitIds.size} unit(s)")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (detail.isNotEmpty()) "${VoidRepository.subjectName(es.subjectId)}: $detail" else VoidRepository.subjectName(es.subjectId),
                        color = VoidColors.TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    if (onDeleteSubject != null) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove ${VoidRepository.subjectName(es.subjectId)} from this exam",
                            tint = VoidColors.TextSecondary,
                            modifier = Modifier.clickable { onDeleteSubject(es) }.height(14.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (exam.examType == ExamType.MOCK) {
            if (exam.grades.isNotEmpty()) {
                Text(
                    text = "GRADES COVERED: ${exam.grades.sorted().joinToString(", ") { "G$it" }}",
                    color = VoidColors.TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            val unitCount = exam.allUnitIds().size
            Text(
                text = "SUBJECTS: ${subjects.map { it.subjectId }.distinct().size}${if (unitCount > 0) "  \u00b7  UNITS COVERED: $unitCount" else ""}",
                color = VoidColors.TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ---------------------------------------------------------------------
// DROPDOWNS + SHARED FIELDS
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
private fun ESessionDropdown(selected: ExamSession?, onSelected: (ExamSession?) -> Unit) {
    EDropdownBase(label = selected?.name ?: "Session (optional)") { close ->
        DropdownMenuItem(text = { Text("None") }, onClick = { onSelected(null); close() })
        ExamSession.entries.forEach { s -> DropdownMenuItem(text = { Text(s.name) }, onClick = { onSelected(s); close() }) }
    }
}

/**
 * Replaces manual per-subject date entry: options are only the calendar
 * days that actually fall inside the exam period above, each labeled with
 * its day name so there's no need to cross-check a calendar by hand — a
 * subject can only ever be scheduled on a day that's genuinely inside the
 * registered period.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EExamPeriodDateDropdown(
    label: String,
    selected: LocalDate?,
    options: List<LocalDate>,
    onSelected: (LocalDate) -> Unit
) {
    val placeholder = if (options.isEmpty()) "Set exam period above first" else label
    EDropdownBase(label = selected?.let { formatExamDateOption(it) } ?: placeholder) { close ->
        if (options.isEmpty()) {
            DropdownMenuItem(text = { Text("Set the exam period above first", color = VoidColors.TextSecondary) }, onClick = { close() })
        } else {
            options.forEach { d ->
                DropdownMenuItem(text = { Text(formatExamDateOption(d)) }, onClick = { onSelected(d); close() })
            }
        }
    }
}

private fun formatExamDateOption(date: LocalDate): String {
    val dayName = date.dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase() }
    return "$dayName ($date)"
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

/**
 * Date field that types like a plain number pad: digits go in, hyphens
 * appear on their own as "YYYY-MM-DD" fills in. Typing "20261012" becomes
 * "2026-10-12" without the user ever having to find the hyphen key.
 */
@Composable
private fun EDateField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(8)
            val formatted = buildString {
                for (i in digits.indices) {
                    append(digits[i])
                    if (i == 3 || i == 5) append('-')
                }
            }
            onValueChange(formatted)
        },
        label = { Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
        placeholder = { Text("0000-00-00", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = VoidColors.TextSecondary) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

/**
 * Time field that types like a plain number pad: digits go in, the colon
 * appears on its own as "HH:MM" fills in. Typing "0930" becomes "09:30"
 * without the user ever having to find the colon key.
 */
@Composable
private fun ETimeField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(4)
            val formatted = buildString {
                for (i in digits.indices) {
                    append(digits[i])
                    if (i == 1) append(':')
                }
            }
            onValueChange(formatted)
        },
        label = { Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
        placeholder = { Text("00:00", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = VoidColors.TextSecondary) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
