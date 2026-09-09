package com.core.voidapp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.core.voidapp.data.widgets.OrbSize
import com.core.voidapp.data.widgets.WidgetPreferences
import com.core.voidapp.widgets.glance.WidgetUpdateScheduler
import com.core.voidapp.widgets.orb.FloatingOrbService
import com.core.voidapp.widgets.orb.OverlayPermissionHelper

@Composable
fun WidgetsSettingsScreen() {
    val context = LocalContext.current

    var classesEnabled by remember { mutableStateOf(WidgetPreferences.isClassesWidgetEnabled(context)) }
    var circleEnabled by remember { mutableStateOf(WidgetPreferences.isCirclePlanWidgetEnabled(context)) }
    var tempPlansEnabled by remember { mutableStateOf(WidgetPreferences.isTemporaryPlansWidgetEnabled(context)) }

    var orbEnabled by remember { mutableStateOf(WidgetPreferences.isOrbEnabled(context)) }
    var orbSize by remember { mutableStateOf(WidgetPreferences.orbSize(context)) }
    var hasOverlayPermission by remember { mutableStateOf(OverlayPermissionHelper.hasOverlayPermission(context)) }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasOverlayPermission = OverlayPermissionHelper.hasOverlayPermission(context)
        if (hasOverlayPermission && orbEnabled) {
            FloatingOrbService.start(context)
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort only — orb still works without it */ }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "VOID SMART WIDGETS",
            color = VoidColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            "Four independent widgets/overlays. Each has its own on/off switch \u2014 none of them require the others.",
            color = VoidColors.TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )

        VoidSectionLabel("TODAY'S CLASSES")
        WidgetToggleCard(
            title = "Today's Classes",
            subtitle = "Current/next/remaining periods from your registered timetable \u2014 hides once today's classes actually end.",
            checked = classesEnabled,
            onCheckedChange = {
                classesEnabled = it
                WidgetPreferences.setClassesWidgetEnabled(context, it)
                WidgetUpdateScheduler.refreshAll(context)
            }
        )

        VoidSectionLabel("TODAY'S CIRCLE PLAN")
        WidgetToggleCard(
            title = "Today's Circle Plan",
            subtitle = "Every Circle Plan slot registered for today \u2014 stays up all day, switches automatically at midnight.",
            checked = circleEnabled,
            onCheckedChange = {
                circleEnabled = it
                WidgetPreferences.setCirclePlanWidgetEnabled(context, it)
                WidgetUpdateScheduler.refreshAll(context)
            }
        )

        VoidSectionLabel("TEMPORARY PLANS")
        WidgetToggleCard(
            title = "Temporary Plans",
            subtitle = "Every currently-active plan \u2014 each disappears independently once it reaches its own deadline/end time.",
            checked = tempPlansEnabled,
            onCheckedChange = {
                tempPlansEnabled = it
                WidgetPreferences.setTemporaryPlansWidgetEnabled(context, it)
                WidgetUpdateScheduler.refreshAll(context)
            }
        )

        VoidSectionLabel("FLOATING AI ORB")
        VoidCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Floating AI Orb", color = VoidColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "A circular overlay that floats above other apps (YouTube, TikTok, Termux, anything). UI-only for now \u2014 its actual AI behavior isn't implemented yet.",
                        color = VoidColors.TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = orbEnabled,
                    onCheckedChange = { wantsOn ->
                        if (wantsOn) {
                            if (hasOverlayPermission) {
                                orbEnabled = true
                                WidgetPreferences.setOrbEnabled(context, true)
                                FloatingOrbService.start(context)
                                maybeRequestNotificationPermission(context, notificationPermissionLauncher)
                            } else {
                                // Don't flip the switch on until the permission is actually granted —
                                // the launcher callback above finishes turning it on if the user allows it.
                                overlayPermissionLauncher.launch(OverlayPermissionHelper.requestOverlayPermissionIntent(context))
                            }
                        } else {
                            orbEnabled = false
                            WidgetPreferences.setOrbEnabled(context, false)
                            FloatingOrbService.stop(context)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VoidColors.Purple,
                        checkedTrackColor = VoidColors.Purple.copy(alpha = 0.4f),
                        uncheckedThumbColor = VoidColors.TextSecondary,
                        uncheckedTrackColor = VoidColors.Surface2
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(if (hasOverlayPermission) VoidColors.Success else VoidColors.Danger, Modifier.size(7.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (hasOverlayPermission) "Overlay permission granted" else "Overlay permission not granted",
                    color = VoidColors.TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                if (!hasOverlayPermission) {
                    Text(
                        "GRANT",
                        color = VoidColors.Cyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable {
                            overlayPermissionLauncher.launch(OverlayPermissionHelper.requestOverlayPermissionIntent(context))
                        }.padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("SIZE", color = VoidColors.TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrbSize.entries.forEach { option ->
                    SizeChip(
                        label = option.label,
                        selected = orbSize == option,
                        onClick = {
                            orbSize = option
                            WidgetPreferences.setOrbSize(context, option)
                            FloatingOrbService.restartIfRunning(context)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "RESET POSITION",
                color = VoidColors.Danger,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.clickable {
                    WidgetPreferences.resetOrbPosition(context)
                    FloatingOrbService.restartIfRunning(context)
                }.padding(4.dp)
            )
        }
    }
}

@Composable
private fun WidgetToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    VoidCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = VoidColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = VoidColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VoidColors.Success,
                    checkedTrackColor = VoidColors.Success.copy(alpha = 0.4f),
                    uncheckedThumbColor = VoidColors.TextSecondary,
                    uncheckedTrackColor = VoidColors.Surface2
                )
            )
        }
    }
}

@Composable
private fun SizeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) VoidColors.Purple.copy(alpha = 0.18f) else VoidColors.Surface2
    val border = if (selected) VoidColors.Purple else VoidColors.Border
    Text(
        text = label.uppercase(),
        color = if (selected) VoidColors.TextPrimary else VoidColors.TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

private fun maybeRequestNotificationPermission(
    context: Context,
    launcher: androidx.activity.compose.ManagedActivityResultLauncher<String, Boolean>
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
}
