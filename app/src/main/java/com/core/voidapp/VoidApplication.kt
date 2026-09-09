package com.core.voidapp

import android.app.Application
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.widgets.WidgetPreferences
import com.core.voidapp.widgets.glance.WidgetUpdateScheduler
import com.core.voidapp.widgets.orb.FloatingOrbService
import com.core.voidapp.widgets.orb.OverlayPermissionHelper

class VoidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        VoidRepository.init(this)

        // VOID Smart Widgets: arm the periodic + exact-alarm refresh pipeline,
        // and resume the Floating Orb if the user had it on and permission
        // is still granted (e.g. after a device reboot or app update).
        WidgetUpdateScheduler.ensureBaselineWork(this)
        if (WidgetPreferences.isOrbEnabled(this) && OverlayPermissionHelper.hasOverlayPermission(this)) {
            FloatingOrbService.start(this)
        }
    }
}
