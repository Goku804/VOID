package com.core.voidapp.widgets.orb

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * SYSTEM_ALERT_WINDOW ("draw over other apps") is a special permission —
 * it can't be requested through the normal runtime-permission dialog, only
 * by sending the user to its dedicated Settings screen. This is the one
 * hard Android requirement behind the Floating AI Orb: a home-screen App
 * Widget genuinely cannot appear over YouTube/TikTok/Termux, only a real
 * overlay window can, and that always needs this permission granted first.
 */
object OverlayPermissionHelper {
    fun hasOverlayPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    /** Launch this with an Activity result launcher — takes the user straight to VOID's row in the overlay-permission list. */
    fun requestOverlayPermissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
}
