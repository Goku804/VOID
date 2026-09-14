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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import com.core.voidapp.data.CirclePlan
import com.core.voidapp.data.ContentStrategy
import com.core.voidapp.data.DayOfWeekVoid
import com.core.voidapp.data.PlanPriority
import com.core.voidapp.data.PreferredWindow
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.resolvedUnit

@Composable
fun CirclePlansContent() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        circlePlanBrowseItems()
    }
}

/** Embeddable version of the Circle Plan browsing list — used directly inside PLAN's single merged page, and by the standalone wrapper above. */
@Composable
fun LazyListScope.circlePlanBrowseItems() {
    val cycleLength = com.core.voidapp.data.CircleCyclePreferences.cycleLengthWeeks(androidx.compose.ui.platform.LocalContext.current)

    item {
        Text(
            "Register new Circle Plan slots in SETTINGS \u2192 PLANNING. This is where you browse and step through what's already registered.",
            color = VoidColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (VoidRepository.circlePlans.isEmpty()) {
        item { Text("Nothing registered yet.", color = VoidColors.TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
    }

    if (cycleLength <= 1) {
        item {
            Text("WEEKLY CIRCLE", color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(DayOfWeekVoid.entries.filter { it != DayOfWeekVoid.SUNDAY }) { d ->
            val plans = VoidRepository.circlePlansFor(d).filter { it.weekInCycle == 1 }
            if (plans.isNotEmpty()) {
                Text(d.name, color = VoidColors.Accent, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                plans.forEach { plan ->
                    CirclePlanRow(plan)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    } else {
        items((1..cycleLength).toList()) { week ->
            val weekPlans = VoidRepository.circlePlans.filter { it.weekInCycle == week }
            if (weekPlans.isNotEmpty()) {
                Text("WEEK $week OF $cycleLength", color = VoidColors.Purple, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                DayOfWeekVoid.entries.filter { it != DayOfWeekVoid.SUNDAY }.forEach { d ->
                    val plans = weekPlans.filter { it.day == d }
                    if (plans.isNotEmpty()) {
                        Text(d.name, color = VoidColors.Accent, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        plans.forEach { plan ->
                            CirclePlanRow(plan)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/** Registration form — lives in Settings -> Planning. What gets registered here shows up as browsable cards in PLAN -> Circle Plan. */
@Composable
fun CirclePlanRegistrationContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var cycleLength by remember { mutableStateOf(com.core.voidapp.data.CircleCyclePreferences.cycleLengthWeeks(context)) }
    var weekInCycle by remember { mutableStateOf(1) }
    var gradeText by remember { mutableStateOf(com.core.voidapp.data.AcademicPreferences.currentGrade(context)?.toString() ?: "") }
    var day by remember { mutableStateOf(DayOfWeekVoid.MONDAY) }
    var subjectId by remember { mutableStateOf<String?>(null) }
    var duration by remember { mutableStateOf("") }
    var window by remember { mutableStateOf(PreferredWindow.ANYTIME) }
    var strategy by remember { mutableStateOf(ContentStrategy.CONTINUE_NEXT_UNIT) }
    var fixedUnitId by remember { mutableStateOf<String?>(null) }
    var priority by remember { mutableStateOf(PlanPriority.NORMAL) }
    var error by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        item {
            PVoidCard {
                Text("CIRCLE CYCLE LENGTH", color = VoidColors.Accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "1 week repeats the same schedule every week. A longer cycle lets a day mean something different from one week to the next before repeating \u2014 e.g. Monday is Math (Grade 9) in week 1 but Physics (Grade 11) in week 2.",
                    color = VoidColors.TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..4).forEach { weeks ->
                        CycleChip(label = "${weeks}W", selected = cycleLength == weeks) {
                            cycleLength = weeks
                            com.core.voidapp.data.CircleCyclePreferences.setCycleLengthWeeks(context, weeks)
                            if (weekInCycle > weeks) weekInCycle = 1
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            PVoidCard {
                Text("REGISTER CIRCLE PLAN", color = VoidColors.Accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                if (VoidRepository.subjects.isEmpty()) {
                    Text("Add a subject in SETTINGS \u2192 ACADEMIC first.", color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                } else {
                    if (cycleLength > 1) {
                        Text("WEEK IN CYCLE", color = VoidColors.TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (1..cycleLength).forEach { w ->
                                CycleChip(label = "WEEK $w", selected = weekInCycle == w) { weekInCycle = w }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    PDayDropdown(selected = day, onSelected = { day = it })
                    Spacer(modifier = Modifier.height(6.dp))

                    Text("GRADE", color = VoidColors.TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Which grade's subjects should this slot pick from? Defaults to your current grade (Settings \u2192 Academic).",
                        color = VoidColors.TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    PField(
                        value = gradeText,
                        onValueChange = { gradeText = it; subjectId = null },
                        label = "Grade",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    PSubjectDropdown(selectedId = subjectId, onSelected = { subjectId = it; fixedUnitId = null }, gradeFilter = gradeText.toIntOrNull())
                    Spacer(modifier = Modifier.height(6.dp))

                    PField(value = duration, onValueChange = { duration = it }, label = "Duration (min)", keyboardType = KeyboardType.Number)
                    Spacer(modifier = Modifier.height(6.dp))

                    PWindowDropdown(selected = window, onSelected = { window = it })
                    Spacer(modifier = Modifier.height(6.dp))
                    PStrategyDropdown(selected = strategy, onSelected = { strategy = it })

                    if (strategy == ContentStrategy.FIXED_UNIT && subjectId != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        PUnitDropdown(subjectId = subjectId!!, selectedId = fixedUnitId, onSelected = { fixedUnitId = it })
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    PPriorityDropdown(selected = priority, onSelected = { priority = it })

                    if (error != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(error!!, color = VoidColors.Danger, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    PBigButton("+ SAVE CIRCLE PLAN") {
                        val sid = subjectId
                        val dur = duration.toIntOrNull()
                        when {
                            sid == null -> error = "Select a subject"
                            dur == null -> error = "Enter a duration"
                            strategy == ContentStrategy.FIXED_UNIT && fixedUnitId == null -> error = "Select a unit"
                            else -> {
                                VoidRepository.addCirclePlan(day, sid, dur, window, strategy, fixedUnitId, priority, weekInCycle)
                                duration = ""
                                error = null
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item { Spacer(modifier = Modifier.height(90.dp)) }
    }
}

@Composable
private fun CycleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) VoidColors.TextPrimary else VoidColors.TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) VoidColors.Purple.copy(alpha = 0.2f) else VoidColors.Surface2)
            .border(1.dp, if (selected) VoidColors.Purple else VoidColors.Border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
fun CirclePlanRow(plan: CirclePlan) {
    val unit = plan.resolvedUnit()
    PVoidCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = VoidRepository.subjectName(plan.subjectId),
                    color = VoidColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${plan.durationMinutes}min \u00b7 ${plan.window.name} \u00b7 ${plan.priority.name}",
                    color = VoidColors.TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (plan.priority == PlanPriority.HIGH) {
                StatusDot(VoidColors.Warning, Modifier.size(7.dp))
                Spacer(modifier = Modifier.width(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (unit == null) {
            Text(
                text = if (plan.strategy == ContentStrategy.FIXED_UNIT) "No unit selected" else "No units registered for this subject yet",
                color = VoidColors.TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (plan.strategy == ContentStrategy.CONTINUE_NEXT_UNIT) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous unit",
                        tint = VoidColors.Accent,
                        modifier = Modifier
                            .clickable { VoidRepository.stepCircleUnit(plan.id, -1) }
                            .size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                    Text(
                        text = "U${unit.unitNumber} \u2014 ${unit.name}",
                        color = VoidColors.Accent,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (plan.strategy == ContentStrategy.CONTINUE_NEXT_UNIT) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next unit",
                        tint = VoidColors.Accent,
                        modifier = Modifier
                            .clickable { VoidRepository.stepCircleUnit(plan.id, 1) }
                            .size(20.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// DROPDOWNS
// ---------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PDayDropdown(selected: DayOfWeekVoid, onSelected: (DayOfWeekVoid) -> Unit) {
    PDropdownBase(label = selected.name) { close ->
        DayOfWeekVoid.entries.filter { it != DayOfWeekVoid.SUNDAY }.forEach { d ->
            DropdownMenuItem(text = { Text(d.name) }, onClick = { onSelected(d); close() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PSubjectDropdown(selectedId: String?, onSelected: (String) -> Unit, gradeFilter: Int? = null) {
    val subject = VoidRepository.subjects.find { it.id == selectedId }
    val name = subject?.let { "${it.name} (Grade ${it.grade})" } ?: "Select subject"
    val options = if (gradeFilter != null) VoidRepository.subjects.filter { it.grade == gradeFilter } else VoidRepository.subjects
    PDropdownBase(label = name) { close ->
        if (options.isEmpty()) {
            DropdownMenuItem(text = { Text("No Grade $gradeFilter subjects \u2014 add one in Settings \u2192 Academic") }, onClick = { close() })
        }
        options.forEach { s ->
            DropdownMenuItem(text = { Text("${s.name} (Grade ${s.grade})") }, onClick = { onSelected(s.id); close() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PUnitDropdown(subjectId: String, selectedId: String?, onSelected: (String) -> Unit) {
    val units = VoidRepository.unitsFor(subjectId)
    val name = units.find { it.id == selectedId }?.let { "U${it.unitNumber} \u2014 ${it.name}" } ?: "Select unit"
    PDropdownBase(label = name) { close ->
        if (units.isEmpty()) {
            DropdownMenuItem(text = { Text("Add units in SETTINGS \u2192 ACADEMIC first") }, onClick = { close() })
        }
        units.forEach { u ->
            DropdownMenuItem(text = { Text("U${u.unitNumber} \u2014 ${u.name}") }, onClick = { onSelected(u.id); close() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PWindowDropdown(selected: PreferredWindow, onSelected: (PreferredWindow) -> Unit) {
    PDropdownBase(label = "Window: ${selected.name}") { close ->
        PreferredWindow.entries.forEach { w ->
            DropdownMenuItem(text = { Text(w.name) }, onClick = { onSelected(w); close() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PStrategyDropdown(selected: ContentStrategy, onSelected: (ContentStrategy) -> Unit) {
    val label = if (selected == ContentStrategy.CONTINUE_NEXT_UNIT) "Continue next unit" else "Fixed unit"
    PDropdownBase(label = label) { close ->
        DropdownMenuItem(text = { Text("Continue next unit") }, onClick = { onSelected(ContentStrategy.CONTINUE_NEXT_UNIT); close() })
        DropdownMenuItem(text = { Text("Fixed unit") }, onClick = { onSelected(ContentStrategy.FIXED_UNIT); close() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PPriorityDropdown(selected: PlanPriority, onSelected: (PlanPriority) -> Unit) {
    PDropdownBase(label = "Priority: ${selected.name}") { close ->
        PlanPriority.entries.forEach { p ->
            DropdownMenuItem(text = { Text(p.name) }, onClick = { onSelected(p); close() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PDropdownBase(label: String, items: @Composable (close: () -> Unit) -> Unit) {
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
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items { open = false }
        }
    }
}

// ---------------------------------------------------------------------
// SHARED
// ---------------------------------------------------------------------

@Composable
private fun PVoidCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VoidColors.Surface)
            .border(1.dp, VoidColors.Border, RoundedCornerShape(12.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun PField(
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
private fun PBigButton(text: String, onClick: () -> Unit) {
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
