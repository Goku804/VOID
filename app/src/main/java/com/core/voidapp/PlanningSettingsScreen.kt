package com.core.voidapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class PlanningSettingsTab(val label: String) {
    CIRCLE("CIRCLE"), TEMPORARY("TEMPORARY"), EXAM_PREP("EXAM PREP")
}

@Composable
fun PlanningSettingsScreen() {
    var tab by remember { mutableStateOf(PlanningSettingsTab.CIRCLE) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            "Register Circle Plan slots, Temporary Plans, and (once built) Exam Preparation here \u2014 everything you register shows up as a browsable/actionable card in the PLAN tab.",
            color = VoidColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PlanningTabChip(PlanningSettingsTab.CIRCLE.label, tab == PlanningSettingsTab.CIRCLE, Modifier.weight(1f)) { tab = PlanningSettingsTab.CIRCLE }
            PlanningTabChip(PlanningSettingsTab.TEMPORARY.label, tab == PlanningSettingsTab.TEMPORARY, Modifier.weight(1f)) { tab = PlanningSettingsTab.TEMPORARY }
            PlanningTabChip(PlanningSettingsTab.EXAM_PREP.label, tab == PlanningSettingsTab.EXAM_PREP, Modifier.weight(1f)) { tab = PlanningSettingsTab.EXAM_PREP }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(modifier = Modifier.weight(1f).fillMaxSize()) {
            when (tab) {
                PlanningSettingsTab.CIRCLE -> CirclePlanRegistrationContent()
                PlanningSettingsTab.TEMPORARY -> TemporaryPlanRegistrationContent()
                PlanningSettingsTab.EXAM_PREP -> ExamPrepComingSoon()
            }
        }
    }
}

@Composable
private fun PlanningTabChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) VoidColors.Purple.copy(alpha = 0.18f) else VoidColors.Surface2)
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
        Text(
            label,
            color = if (selected) VoidColors.TextPrimary else VoidColors.TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
