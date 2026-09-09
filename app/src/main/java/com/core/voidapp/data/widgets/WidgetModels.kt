package com.core.voidapp.data.widgets

import com.core.voidapp.data.ClassType
import java.time.LocalTime

/**
 * VOID Smart Widgets — data shapes only.
 *
 * These are computed fresh every time by [WidgetDataProvider] from real
 * VoidRepository state; nothing here is ever hand-authored or demo data.
 * Kept free of any Android/Glance import so the computation in
 * WidgetDataProvider stays unit-testable on the JVM.
 */

// ---------------------------------------------------------------------
// Widget 1 — Today's Classes
// ---------------------------------------------------------------------

data class ClassSlotInfo(
    val subjectName: String,
    val classType: ClassType,
    val periodNumber: Int,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val location: String?
)

/**
 * Whole state for the Today's Classes widget. NoSchoolToday means the
 * registered timetable simply has no periods for today (e.g. weekend) —
 * distinct from Finished, which means today HAD classes and they're over.
 */
sealed class TodayClassesState {
    data object NoSchoolToday : TodayClassesState()
    data object Finished : TodayClassesState()
    data class InProgress(
        val current: ClassSlotInfo?,
        val next: ClassSlotInfo?,
        val remaining: List<ClassSlotInfo>,
        val allToday: List<ClassSlotInfo>
    ) : TodayClassesState()
}

// ---------------------------------------------------------------------
// Widget 2 — Today's Circle Plan
// ---------------------------------------------------------------------

/**
 * One of today's Circle Plan slots. There is deliberately no "progress"
 * field here — CirclePlan has no per-session completion tracking in the
 * database (unlike TemporaryTask), so a progress number would have to be
 * invented. resolvedUnitName is the real thing VOID does track: which
 * unit the CONTINUE_NEXT_UNIT cursor (or FIXED_UNIT pin) currently points
 * at for this subject.
 */
data class CirclePlanSlotInfo(
    val subjectName: String,
    val resolvedUnitName: String?,
    val durationMinutes: Int,
    val window: String,
    val priority: String
)

sealed class TodayCirclePlanState {
    data object NoPlanToday : TodayCirclePlanState()
    /** Real cardinality: a day can carry more than one Circle Plan slot (different subjects/windows). */
    data class Content(val slots: List<CirclePlanSlotInfo>) : TodayCirclePlanState()
}

// ---------------------------------------------------------------------
// Widget 3 — Temporary Plans
// ---------------------------------------------------------------------

data class ActivePlanInfo(
    val id: String,
    val title: String,
    val subjectName: String,
    val timeRangeLabel: String?,
    val deadlineLabel: String,
    val progressPercent: Int
)

sealed class TemporaryPlansState {
    data object NoActivePlans : TemporaryPlansState()
    data class Content(val plans: List<ActivePlanInfo>) : TemporaryPlansState()
}
