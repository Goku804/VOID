package com.core.voidapp.data.widgets

import android.content.Context

enum class OrbSize(val sizeDp: Int, val label: String) {
    SMALL(48, "Small"),
    MEDIUM(64, "Medium"),
    LARGE(84, "Large")
}

/**
 * Every user-facing on/off and configuration knob for VOID Smart Widgets
 * lives here — nothing about visibility logic (that's WidgetDataProvider),
 * just "does the user want this thing running at all, and how".
 */
object WidgetPreferences {
    private const val PREFS_NAME = "void_widget_prefs"

    private const val KEY_CLASSES_ENABLED = "classes_enabled"
    private const val KEY_CIRCLE_ENABLED = "circle_enabled"
    private const val KEY_TEMP_PLANS_ENABLED = "temp_plans_enabled"
    private const val KEY_ORB_ENABLED = "orb_enabled"
    private const val KEY_ORB_SIZE = "orb_size"
    private const val KEY_ORB_X = "orb_x"
    private const val KEY_ORB_Y = "orb_y"
    private const val KEY_ORB_POSITION_SET = "orb_position_set"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isClassesWidgetEnabled(context: Context) = prefs(context).getBoolean(KEY_CLASSES_ENABLED, true)
    fun setClassesWidgetEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_CLASSES_ENABLED, enabled).apply()

    fun isCirclePlanWidgetEnabled(context: Context) = prefs(context).getBoolean(KEY_CIRCLE_ENABLED, true)
    fun setCirclePlanWidgetEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_CIRCLE_ENABLED, enabled).apply()

    fun isTemporaryPlansWidgetEnabled(context: Context) = prefs(context).getBoolean(KEY_TEMP_PLANS_ENABLED, true)
    fun setTemporaryPlansWidgetEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_TEMP_PLANS_ENABLED, enabled).apply()

    /** Whether the user WANTS the floating orb running. Actually starting it also needs the overlay permission — see OverlayPermissionHelper. */
    fun isOrbEnabled(context: Context) = prefs(context).getBoolean(KEY_ORB_ENABLED, false)
    fun setOrbEnabled(context: Context, enabled: Boolean) = prefs(context).edit().putBoolean(KEY_ORB_ENABLED, enabled).apply()

    fun orbSize(context: Context): OrbSize =
        runCatching { OrbSize.valueOf(prefs(context).getString(KEY_ORB_SIZE, OrbSize.MEDIUM.name)!!) }.getOrDefault(OrbSize.MEDIUM)
    fun setOrbSize(context: Context, size: OrbSize) = prefs(context).edit().putString(KEY_ORB_SIZE, size.name).apply()

    /** Null until the user has actually placed the orb once — lets the service pick a sensible first-run default. */
    fun orbPosition(context: Context): Pair<Int, Int>? {
        val p = prefs(context)
        if (!p.getBoolean(KEY_ORB_POSITION_SET, false)) return null
        return p.getInt(KEY_ORB_X, 0) to p.getInt(KEY_ORB_Y, 0)
    }

    fun saveOrbPosition(context: Context, x: Int, y: Int) {
        prefs(context).edit()
            .putInt(KEY_ORB_X, x)
            .putInt(KEY_ORB_Y, y)
            .putBoolean(KEY_ORB_POSITION_SET, true)
            .apply()
    }

    fun resetOrbPosition(context: Context) {
        prefs(context).edit().putBoolean(KEY_ORB_POSITION_SET, false).apply()
    }
}
