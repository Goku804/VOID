package com.core.voidapp.data

import android.content.Context

/**
 * VOID can't always infer "what grade is the student in right now" purely
 * from registered subjects/classes — a timetable can legitimately mix
 * grades (D-Class periods, electives taken with another grade, etc). This
 * is the one place that answer lives, set explicitly in Settings ->
 * Academic, and read anywhere that needs a sensible default grade (new
 * subject registration, unit bulk-registration, etc).
 */
object AcademicPreferences {
    private const val PREFS_NAME = "void_academic_prefs"
    private const val KEY_CURRENT_GRADE = "current_grade"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun currentGrade(context: Context): Int? {
        val value = prefs(context).getInt(KEY_CURRENT_GRADE, -1)
        return if (value == -1) null else value
    }

    fun setCurrentGrade(context: Context, grade: Int) {
        prefs(context).edit().putInt(KEY_CURRENT_GRADE, grade).apply()
    }
}
