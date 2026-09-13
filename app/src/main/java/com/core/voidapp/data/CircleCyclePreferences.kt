package com.core.voidapp.data

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The user's Circle Plan doesn't have to repeat every single week — this
 * is the one place that says how many weeks the cycle actually runs
 * (1 = the original simple behavior, every week identical; 3 = week 1,
 * week 2, week 3, then back to week 1) and which real calendar week counts
 * as "week 1" of that cycle.
 */
object CircleCyclePreferences {
    private const val PREFS_NAME = "void_circle_cycle_prefs"
    private const val KEY_CYCLE_LENGTH = "cycle_length_weeks"
    private const val KEY_ANCHOR_EPOCH_DAY = "cycle_anchor_epoch_day"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun cycleLengthWeeks(context: Context): Int =
        prefs(context).getInt(KEY_CYCLE_LENGTH, 1).coerceAtLeast(1)

    /** Changing the length doesn't move the anchor — week numbering for existing slots stays meaningful, it just wraps at a new point. */
    fun setCycleLengthWeeks(context: Context, weeks: Int) {
        prefs(context).edit().putInt(KEY_CYCLE_LENGTH, weeks.coerceAtLeast(1)).apply()
        ensureAnchor(context)
    }

    /** The Monday that counts as day 1 of week 1 of the cycle. Set once, lazily, the first time it's needed — never silently reset afterward. */
    fun anchorDate(context: Context): LocalDate {
        ensureAnchor(context)
        val epochDay = prefs(context).getLong(KEY_ANCHOR_EPOCH_DAY, -1)
        return LocalDate.ofEpochDay(epochDay)
    }

    private fun ensureAnchor(context: Context) {
        val p = prefs(context)
        if (p.getLong(KEY_ANCHOR_EPOCH_DAY, -1) != -1L) return
        val mostRecentMonday = LocalDate.now().let { it.minusDays((it.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong()) }
        p.edit().putLong(KEY_ANCHOR_EPOCH_DAY, mostRecentMonday.toEpochDay()).apply()
    }

    /** Lets the user deliberately re-sync "week 1 starts now" (e.g. after a long break) without changing the cycle length. */
    fun resetAnchorToThisWeek(context: Context) {
        val mostRecentMonday = LocalDate.now().let { it.minusDays((it.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong()) }
        prefs(context).edit().putLong(KEY_ANCHOR_EPOCH_DAY, mostRecentMonday.toEpochDay()).apply()
    }

    /** 1-based week-in-cycle for the given date. Always 1 when the cycle length is 1 (the original, simple behavior). */
    fun weekInCycle(context: Context, date: LocalDate = LocalDate.now()): Int {
        val length = cycleLengthWeeks(context)
        if (length <= 1) return 1
        val anchor = anchorDate(context)
        val weeksSinceAnchor = ChronoUnit.WEEKS.between(anchor, date)
        // floorMod handles dates before the anchor gracefully too, instead of yielding a negative index.
        return Math.floorMod(weeksSinceAnchor, length.toLong()).toInt() + 1
    }
}
