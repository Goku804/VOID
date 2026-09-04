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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import com.core.voidapp.data.PlanPriority
import com.core.voidapp.data.PlanTaskStatus
import com.core.voidapp.data.TemporaryPlanType
import com.core.voidapp.data.TemporaryTask
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.daysUntilDeadline
import com.core.voidapp.data.isOverdue
import com.core.voidapp.data.progressPercent
import java.time.LocalDate

@Composable
fun TemporaryPlanContent() {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TemporaryPlanType.OTHER) }
    var subjectId by remember { mutableStateOf<String?>(null) }
    var deadlineText by remember { mutableStateOf("") }
    var requiredMinutes by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(PlanPriority.NORMAL) }
    var notes by remember { mutableStateOf("") }
    var selectedUnitIds by remember { mutableStateOf(setOf<String>()) }
    var error by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        item {
            TVoidCard {
                Text("REGISTER TEMPORARY PLAN", color = VoidColors.Accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                TField(value = title, onValueChange = { title = it }, label = "Title")
                Spacer(modifier = Modifier.height(6.dp))
                TTypeDropdown(selected = type, onSelected = { type = it })
                Spacer(modifier = Modifier.height(6.dp))
                TSubjectDropdown(selectedId = subjectId, onSelected = { subjectId = it; selectedUnitIds = emptySet() })

                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TField(value = deadlineText, onValueChange = { deadlineText = it }, label = "Deadline (YYYY-MM-DD)", modifier = Modifier.weight(1f))
                    TField(value = requiredMinutes, onValueChange = { requiredMinutes = it }, label = "Required min", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(6.dp))
                TPriorityDropdown(selected = priority, onSelected = { priority = it })

                if (subjectId != null) {
                    val units = VoidRepository.unitsFor(subjectId!!)
                    if (units.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("UNITS / CONTENT", color = VoidColors.TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        units.forEach { u ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    selectedUnitIds = if (selectedUnitIds.contains(u.id)) selectedUnitIds - u.id else selectedUnitIds + u.id
                                }
                            ) {
                                Checkbox(
                                    checked = selectedUnitIds.contains(u.id),
                                    onCheckedChange = { checked ->
                                        selectedUnitIds = if (checked) selectedUnitIds + u.id else selectedUnitIds - u.id
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = VoidColors.Accent, uncheckedColor = VoidColors.TextSecondary)
                                )
                                Text("U${u.unitNumber} \u2014 ${u.name}", color = VoidColors.TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                TField(value = notes, onValueChange = { notes = it }, label = "Notes (optional)")

                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(error!!, color = VoidColors.Danger, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(10.dp))
                TBigButton("+ SAVE TEMPORARY PLAN") {
                    val minutes = requiredMinutes.toIntOrNull()
                    when {
                        title.isBlank() -> error = "Enter a title"
                        minutes == null -> error = "Enter required minutes"
                        else -> {
                            try {
                                val deadline = LocalDate.parse(deadlineText.trim())
                                VoidRepository.addTemporaryTask(
                                    title = title.trim(),
                                    type = type,
                                    subjectId = subjectId,
                                    startDate = null,
                                    deadline = deadline,
                                    requiredMinutes = minutes,
                                    priority = priority,
                                    unitIds = selectedUnitIds.toList(),
                                    notes = notes.trim()
                                )
                                title = ""; requiredMinutes = ""; deadlineText = ""; notes = ""; selectedUnitIds = emptySet()
                                error = null
                            } catch (e: Exception) {
                                error = "Deadline must be YYYY-MM-DD"
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text("ACTIVE", color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        val active = VoidRepository.activeTemporaryTasks()
        if (active.isEmpty()) {
            item { Text("No temporary plans yet.", color = VoidColors.TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
        }

        items(active) { task ->
            TemporaryTaskRow(task)
            Spacer(modifier = Modifier.height(10.dp))
        }

        item { Spacer(modifier = Modifier.height(90.dp)) }
    }
}

@Composable
private fun TemporaryTaskRow(task: TemporaryTask) {
    var expanded by remember { mutableStateOf(false) }
    val overdue = task.isOverdue()
    val priorityColor = when {
        overdue -> VoidColors.Danger
        task.priority == PlanPriority.HIGH -> VoidColors.Danger
        task.priority == PlanPriority.NORMAL -> VoidColors.Warning
        else -> VoidColors.TextSecondary
    }

    TVoidCard(modifier = Modifier.clickable { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(priorityColor, Modifier.width(8.dp).height(8.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, color = VoidColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${task.type.name.replace('_', ' ')} \u00b7 ${VoidRepository.subjectName(task.subjectId)}",
                    color = VoidColors.TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = if (overdue) "OVERDUE" else "${task.daysUntilDeadline()}d left",
                color = if (overdue) VoidColors.Danger else VoidColors.TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        ProgressBar(task.progressPercent())
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${task.completedMinutes}/${task.requiredMinutes} min \u00b7 ${task.status.name}",
            color = VoidColors.TextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )

        if (expanded) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TSmallButton("+15 MIN", VoidColors.Accent) { VoidRepository.addProgress(task.id, 15) }
                TSmallButton("+30 MIN", VoidColors.Accent) { VoidRepository.addProgress(task.id, 30) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TSmallButton("COMPLETE", VoidColors.Success, icon = Icons.Default.Check) { VoidRepository.setTaskStatus(task.id, PlanTaskStatus.COMPLETED) }
                TSmallButton("CANCEL", VoidColors.Danger, icon = Icons.Default.Close) { VoidRepository.setTaskStatus(task.id, PlanTaskStatus.CANCELLED) }
            }
            if (task.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(task.notes, color = VoidColors.TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ProgressBar(percent: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(VoidColors.Border)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(percent / 100f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(VoidColors.Accent)
        )
    }
}

// ---------------------------------------------------------------------
// DROPDOWNS + SHARED
// ---------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TTypeDropdown(selected: TemporaryPlanType, onSelected: (TemporaryPlanType) -> Unit) {
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
            Text(selected.name.replace('_', ' '), color = VoidColors.TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = VoidColors.TextSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            TemporaryPlanType.entries.forEach { t ->
                DropdownMenuItem(text = { Text(t.name.replace('_', ' ')) }, onClick = { onSelected(t); open = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TSubjectDropdown(selectedId: String?, onSelected: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val name = VoidRepository.subjects.find { it.id == selectedId }?.name ?: "Subject (optional)"
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
            Text(name, color = VoidColors.TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = VoidColors.TextSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            VoidRepository.subjects.forEach { s ->
                DropdownMenuItem(text = { Text(s.name) }, onClick = { onSelected(s.id); open = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TPriorityDropdown(selected: PlanPriority, onSelected: (PlanPriority) -> Unit) {
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
            Text("Priority: ${selected.name}", color = VoidColors.TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = VoidColors.TextSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            PlanPriority.entries.forEach { p ->
                DropdownMenuItem(text = { Text(p.name) }, onClick = { onSelected(p); open = false })
            }
        }
    }
}

@Composable
private fun TVoidCard(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VoidColors.Surface)
            .border(1.dp, VoidColors.Border, RoundedCornerShape(12.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun TField(
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
private fun TBigButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(VoidColors.Accent.copy(alpha = 0.12f))
            .border(1.dp, VoidColors.Accent, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(text, color = VoidColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun TSmallButton(text: String, color: androidx.compose.ui.graphics.Color, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.height(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}
