package com.core.voidapp.data.guardian

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.core.voidapp.MainActivity
import com.core.voidapp.R

/**
 * Guardian no longer speaks — every event that used to be a spoken line
 * now surfaces as a plain Android notification instead, using the exact
 * same dialogue text from GuardianDialogue (so the wording/variety work
 * isn't wasted, it just reads instead of plays).
 *
 * Every new notification reuses the same notification ID, so Guardian
 * updates one ongoing alert rather than stacking a new one per event.
 */
object GuardianNotifier {
    private const val CHANNEL_ID = "void_guardian_alerts"
    private const val NOTIFICATION_ID = 4300

    private var onOrbStateChange: ((GuardianOrbState) -> Unit)? = null

    fun setOrbStateListener(listener: (GuardianOrbState) -> Unit) {
        onOrbStateChange = listener
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID, "VOID Guardian Alerts", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Session, distraction, deadline, and exam alerts from VOID Guardian."
        }
        manager.createNotificationChannel(channel)
    }

    /** Posts (or updates) Guardian's alert notification with the given line. Silently does nothing without POST_NOTIFICATIONS on API 33+ — never crashes. */
    fun notify(context: Context, text: String) {
        ensureChannel(context)
        onOrbStateChange?.invoke(GuardianOrbState.SPEAKING)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            onOrbStateChange?.invoke(GuardianOrbState.IDLE)
            return
        }

        val openIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("VOID Guardian")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
        // The orb's brief "speaking" pulse now just mirrors how long a
        // notification takes to register, rather than real speech timing.
        onOrbStateChange?.invoke(GuardianOrbState.IDLE)
    }

    /** Lets Settings preview what a Guardian alert looks like without waiting for a real event. */
    fun testNotification(context: Context) {
        notify(context, "This is Guardian. This is how your alerts will look during a study session.")
    }
}
