package com.core.voidapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.core.voidapp.data.guardian.CommitmentDuration
import com.core.voidapp.data.guardian.EnforcementMode
import com.core.voidapp.data.guardian.GuardianCommitment
import com.core.voidapp.data.guardian.GuardianRepository
import com.core.voidapp.data.guardian.GuardianSettings
import com.core.voidapp.data.guardian.GuardianVoice
import com.core.voidapp.data.guardian.daysRemaining
import com.core.voidapp.data.guardian.isCurrentlyActive
import java.time.format.DateTimeFormatter

private val guardianDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
fun GuardianSettingsScreen() {
    val context = LocalContext.current
    var refreshTick by remember { mutableStateOf(0) }
    val commitment = GuardianRepository.currentCommitment()
    val isActive = commitment?.isCurrentlyActive() == true

    var settings by remember { mutableStateOf(GuardianRepository.settings()) }
    var showEndConfirm by remember { mutableStateOf(false) }
    var voicesReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        GuardianVoice.init(context) { voicesReady = true }
    }

    fun applySettings(transform: (GuardianSettings) -> GuardianSettings) {
        GuardianRepository.updateSettings(transform)
        settings = GuardianRepository.settings()
        GuardianVoice.applyVoicePreferences()
    }

    // Reading refreshTick anywhere below forces this screen to re-pull
    // commitment/settings after a mutating action (begin/end commitment).
    key(refreshTick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "VOID GUARDIAN",
                color = VoidColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "Discipline, focus, and voice enforcement for your study sessions. Guardian only acts while a commitment is active \u2014 there is no plain on/off switch.",
                color = VoidColors.TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            if (isActive && commitment != null) {
                GuardianActiveCard(commitment, settings) { showEndConfirm = true }
            } else {
                CommitmentPickerCard { duration ->
                    GuardianRepository.beginCommitment(duration)
                    refreshTick++
                }
            }

            VoidSectionLabel("ENFORCEMENT MODE")
            VoidCard {
                Text(
                    "OFF: silent. GENTLE: voice + visual warnings. FOCUS: firmer, faster warnings. STRICT: full seriousness escalation.",
                    color = VoidColors.TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EnforcementMode.entries.forEach { mode ->
                        ModeChip(
                            label = mode.name,
                            selected = settings.enforcementMode == mode,
                            onClick = { applySettings { it.copy(enforcementMode = mode) } }
                        )
                    }
                }
            }

            VoidSectionLabel("VOICE")
            VoidCard {
                Text("SPEED", color = VoidColors.TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Slider(
                    value = settings.speechRate,
                    onValueChange = { applySettings { s -> s.copy(speechRate = it) } },
                    valueRange = 0.6f..1.3f,
                    colors = SliderDefaults.colors(thumbColor = VoidColors.Purple, activeTrackColor = VoidColors.Purple)
                )
                Text("PITCH \u2014 lower is deeper", color = VoidColors.TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Slider(
                    value = settings.pitch,
                    onValueChange = { applySettings { s -> s.copy(pitch = it) } },
                    valueRange = 0.5f..1.2f,
                    colors = SliderDefaults.colors(thumbColor = VoidColors.Purple, activeTrackColor = VoidColors.Purple)
                )
                Text("VOLUME", color = VoidColors.TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Slider(
                    value = settings.volume,
                    onValueChange = { applySettings { s -> s.copy(volume = it) } },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = VoidColors.Purple, activeTrackColor = VoidColors.Purple)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "TEST VOICE",
                    color = VoidColors.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { GuardianVoice.testVoice(context) }.padding(6.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("AVAILABLE VOICES", color = VoidColors.TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))
                if (!voicesReady) {
                    Text("Loading voices from the device's speech engine...", color = VoidColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                } else {
                    val voices = GuardianVoice.availableVoices(context)
                        .filterNot { it.isNetworkConnectionRequired }
                        .sortedByDescending { it.quality }
                        .take(12)
                    if (voices.isEmpty()) {
                        Text("No offline voices reported by this device.", color = VoidColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    } else {
                        voices.forEach { voice ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { applySettings { it.copy(voiceName = voice.name) } }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.voiceName == voice.name,
                                    onClick = { applySettings { it.copy(voiceName = voice.name) } },
                                    colors = RadioButtonDefaults.colors(selectedColor = VoidColors.Purple)
                                )
                                Text(voice.name, color = VoidColors.TextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            VoidSectionLabel("ALLOWED APPS")
            AllowedAppsCard(settings.allowedPackages) { updated ->
                applySettings { it.copy(allowedPackages = updated) }
            }

            VoidSectionLabel("DISTRACTION DETECTION")
            VoidCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(VoidColors.TextSecondary, Modifier.width(7.dp).height(7.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Not yet active on this build \u2014 Guardian will still narrate sessions, breaks, and deadlines, but won't detect leaving the app until this is implemented.",
                        color = VoidColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showEndConfirm && commitment != null) {
        AlertDialog(
            onDismissRequest = { showEndConfirm = false },
            title = { Text("END GUARDIAN COMMITMENT?", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your commitment is still active until ${commitment.endsAt.format(guardianDateFmt)}. Ending it now disables Guardian enforcement before your selected end date. Your study history, plans, progress, sessions, and exams are not affected.",
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    GuardianRepository.endCommitmentEarly()
                    showEndConfirm = false
                    refreshTick++
                }) { Text("END COMMITMENT", color = VoidColors.Danger, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirm = false }) {
                    Text("KEEP COMMITMENT", color = VoidColors.TextSecondary, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = VoidColors.Surface
        )
    }
}

@Composable
private fun GuardianActiveCard(
    commitment: GuardianCommitment,
    settings: GuardianSettings,
    onRequestEnd: () -> Unit
) {
    VoidCard(borderColor = VoidColors.Purple) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(VoidColors.Purple, Modifier.width(8.dp).height(8.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("GUARDIAN ACTIVE", color = VoidColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(10.dp))
        GuardianStatRow("Commitment", commitment.duration.label)
        GuardianStatRow("Started", commitment.startedAt.format(guardianDateFmt))
        GuardianStatRow("Ends", commitment.endsAt.format(guardianDateFmt))
        GuardianStatRow("Days Remaining", commitment.daysRemaining().toString())
        GuardianStatRow("Enforcement", settings.enforcementMode.name)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "END COMMITMENT EARLY",
            color = VoidColors.Danger, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
            modifier = Modifier.clickable { onRequestEnd() }.padding(4.dp)
        )
    }
}

@Composable
private fun GuardianStatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = VoidColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
        Text(value, color = VoidColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun CommitmentPickerCard(onBegin: (CommitmentDuration) -> Unit) {
    var selected by remember { mutableStateOf(CommitmentDuration.ONE_MONTH) }
    VoidCard {
        Text("GUARDIAN COMMITMENT", color = VoidColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "How long do you want VOID Guardian to remain in control?",
            color = VoidColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(10.dp))
        CommitmentDuration.entries.forEach { duration ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selected = duration }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == duration,
                    onClick = { selected = duration },
                    colors = RadioButtonDefaults.colors(selectedColor = VoidColors.Purple)
                )
                Text(duration.label, color = VoidColors.TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(VoidColors.Purple)
                .clickable { onBegin(selected) }
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("BEGIN COMMITMENT", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) VoidColors.Purple.copy(alpha = 0.2f) else VoidColors.Surface2
    val border = if (selected) VoidColors.Purple else VoidColors.Border
    Text(
        text = label,
        color = if (selected) VoidColors.TextPrimary else VoidColors.TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun AllowedAppsCard(allowed: Set<String>, onChange: (Set<String>) -> Unit) {
    var newPackage by remember { mutableStateOf("") }
    VoidCard {
        Text(
            "Package names Guardian never treats as a distraction, even during an active session (e.g. a calculator or dictionary app).",
            color = VoidColors.TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (allowed.isEmpty()) {
            Text("None added.", color = VoidColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        } else {
            allowed.forEach { pkg ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(pkg, color = VoidColors.TextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    Text(
                        "REMOVE", color = VoidColors.Danger, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable { onChange(allowed - pkg) }.padding(4.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newPackage,
                onValueChange = { newPackage = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("com.example.app", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = VoidColors.TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoidColors.Purple, unfocusedBorderColor = VoidColors.Border, cursorColor = VoidColors.Purple)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "ADD", color = VoidColors.Purple, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                modifier = Modifier.clickable {
                    val trimmed = newPackage.trim()
                    if (trimmed.isNotBlank()) {
                        onChange(allowed + trimmed)
                        newPackage = ""
                    }
                }.padding(8.dp)
            )
        }
    }
}
