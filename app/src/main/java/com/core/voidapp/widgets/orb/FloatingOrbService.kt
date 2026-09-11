package com.core.voidapp.widgets.orb

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.core.voidapp.MainActivity
import com.core.voidapp.R
import com.core.voidapp.data.guardian.GuardianEngine
import com.core.voidapp.data.widgets.WidgetPreferences

/**
 * Hosts the Floating AI Orb across every app, not just VOID — the whole
 * reason this is a WindowManager overlay Service rather than an Activity
 * or a home-screen widget (see OverlayPermissionHelper's doc comment for
 * why a widget can't do this). UI-only for now: onTap/onLongPress are
 * intentionally no-ops until the orb's actual AI duty is specified.
 */
class FloatingOrbService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        startForeground(NOTIFICATION_ID, buildNotification())
        addOrbView()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        removeOrbView()
        super.onDestroy()
    }

    private fun addOrbView() {
        if (!OverlayPermissionHelper.hasOverlayPermission(this)) {
            // Permission was revoked between "start requested" and now (e.g.
            // the user pulled it back in system Settings) — never crash,
            // just decline to show anything and stop the service cleanly.
            stopSelf()
            return
        }

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val orbSize = WidgetPreferences.orbSize(this)
        val sizePx = dpToPx(orbSize.sizeDp)

        @Suppress("DEPRECATION")
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val savedPosition = WidgetPreferences.orbPosition(this)
        val params = WindowManager.LayoutParams(
            sizePx, sizePx,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedPosition?.first ?: 40
            y = savedPosition?.second ?: 300
        }
        layoutParams = params

        val view = ComposeView(this)
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        view.setContent {
            OrbRoot(
                sizeDp = orbSize.sizeDp.dp,
                orbState = GuardianEngine.orbState.value,
                onDrag = { dx, dy ->
                    val p = layoutParams ?: return@OrbRoot
                    p.x += dx.toInt()
                    p.y += dy.toInt()
                    runCatching { windowManager?.updateViewLayout(view, p) }
                },
                onDragEnd = {
                    layoutParams?.let { WidgetPreferences.saveOrbPosition(this, it.x, it.y) }
                },
                onTap = {
                    // Reserved for the orb's future AI duty — not implemented yet, on purpose.
                },
                onLongPress = {
                    // Reserved for future long-press configuration behavior — not implemented yet, on purpose.
                }
            )
        }
        composeView = view
        runCatching { wm.addView(view, params) }
    }

    private fun removeOrbView() {
        runCatching { composeView?.let { windowManager?.removeView(it) } }
        composeView = null
        windowManager = null
        layoutParams = null
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun buildNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "VOID Floating Orb", NotificationManager.IMPORTANCE_MIN)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VOID AI Orb is active")
            .setContentText("Tap to open VOID")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 4201
        private const val CHANNEL_ID = "void_floating_orb"

        fun start(context: Context) {
            val intent = Intent(context, FloatingOrbService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingOrbService::class.java))
        }

        /** Restart is how a running orb picks up a new size/position from Settings — simplest reliable way to re-read prefs into fresh LayoutParams. */
        fun restartIfRunning(context: Context) {
            stop(context)
            if (WidgetPreferences.isOrbEnabled(context) && OverlayPermissionHelper.hasOverlayPermission(context)) {
                start(context)
            }
        }
    }
}
