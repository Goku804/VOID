package com.core.voidapp.widgets.glance

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.core.voidapp.data.VoidRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Update pipeline for VOID Smart Widgets:
 *
 *   VoidRepository mutation -> refreshAll() [immediate]
 *   WorkManager 15-min periodic -> refreshAll() [safety net; 15 min is
 *       Android's own floor for periodic work, it cannot go lower]
 *   AlarmManager exact alarm at the next real boundary -> refreshAll()
 *       + reschedules itself for the boundary after that
 *
 * The exact alarm is what makes "hide the moment classes end" or
 * "switch to tomorrow's Circle Plan right at midnight" actually precise,
 * rather than waiting for the next 15-min tick.
 */
object WidgetUpdateScheduler {
    private const val WORK_NAME = "void_widget_baseline_refresh"
    private const val ALARM_REQUEST_CODE = 4200
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** Recompute and redraw every VOID Smart Widget right now. Cheap no-op if none are placed on a home screen. */
    fun refreshAll(context: Context) {
        scope.launch {
            runCatching { TodayClassesWidget().updateAll(context) }
            runCatching { TodayCirclePlanWidget().updateAll(context) }
            runCatching { TemporaryPlansWidget().updateAll(context) }
        }
        scheduleNextBoundaryAlarm(context)
    }

    /** Call once at app startup — enqueues the always-on safety-net refresh. Safe to call repeatedly (KEEP policy). */
    fun ensureBaselineWork(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        scheduleNextBoundaryAlarm(context)
    }

    /**
     * Finds the nearest future moment where a widget's on-screen state
     * would actually change — a class starting/ending, midnight (Circle
     * Plan day rollover), or a temporary plan reaching its deadline/end
     * time — and arms one exact alarm for it. Falls back to next midnight
     * if nothing else is registered, so there's always at least a daily
     * refresh floor.
     */
    fun scheduleNextBoundaryAlarm(context: Context) {
        val now = LocalDateTime.now()
        val candidates = mutableListOf<LocalDateTime>()

        candidates += now.toLocalDate().plusDays(1).atStartOfDay()

        val todayDay = com.core.voidapp.data.DayOfWeekVoid.valueOf(now.dayOfWeek.name)
        VoidRepository.scheduleFor(todayDay).forEach { period ->
            period.startTime?.let { candidates += LocalDateTime.of(now.toLocalDate(), it) }
            period.endTime?.let { candidates += LocalDateTime.of(now.toLocalDate(), it) }
        }

        VoidRepository.temporaryTasks.forEach { task ->
            val cutoffTime = task.endTime ?: LocalTime.MAX
            candidates += LocalDateTime.of(task.deadline, cutoffTime)
            task.startDate?.let { candidates += it.atStartOfDay() }
            task.startTime?.let { candidates += LocalDateTime.of(task.deadline, it) }
        }

        val next = candidates.filter { it.isAfter(now) }.minOrNull()
            ?: now.toLocalDate().plusDays(1).atStartOfDay()

        armAlarm(context, next)
    }

    private fun armAlarm(context: Context, at: LocalDateTime) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE,
            Intent(context, WidgetAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAtMillis = at.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        runCatching {
            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                // No exact-alarm permission — fall back to an inexact-but-doze-aware
                // alarm rather than skip scheduling (or crash) entirely.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }
    }
}

/** Fires at the next boundary moment; refreshes widgets then arms the following boundary. */
class WidgetAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WidgetUpdateScheduler.refreshAll(context)
    }
}

/** Reschedules the exact alarm after a reboot — AlarmManager alarms don't survive one, WorkManager's periodic work does on its own. Also resumes the Floating Orb if the user had it on. */
class WidgetBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            WidgetUpdateScheduler.ensureBaselineWork(context)
            val prefs = com.core.voidapp.data.widgets.WidgetPreferences
            val overlayOk = com.core.voidapp.widgets.orb.OverlayPermissionHelper.hasOverlayPermission(context)
            if (prefs.isOrbEnabled(context) && overlayOk) {
                com.core.voidapp.widgets.orb.FloatingOrbService.start(context)
            }
        }
    }
}

/** WorkManager safety net — guarantees a refresh at least every 15 minutes even if every exact alarm above got dropped by the OS. */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        WidgetUpdateScheduler.refreshAll(applicationContext)
        return Result.success()
    }
}
