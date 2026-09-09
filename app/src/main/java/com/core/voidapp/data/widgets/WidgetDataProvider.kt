package com.core.voidapp.data.widgets

import com.core.voidapp.data.ClassPeriod
import com.core.voidapp.data.DayOfWeekVoid
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.isActiveAt
import com.core.voidapp.data.progressPercent
import com.core.voidapp.data.resolvedUnit
import com.core.voidapp.data.timeRangeLabel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Single source of truth for what every VOID Smart Widget shows. No widget
 * (Glance or overlay) computes its own state — they all call in here, which
 * reads straight off VoidRepository:
 *
 *     DATABASE -> REPOSITORY -> WIDGET DATA -> WIDGET UI
 *
 * Every function takes `now` as a parameter (defaulting to the real clock)
 * so the logic itself has no hidden dependency on wall-clock time and can
 * be exercised directly from a JVM unit test.
 */
object WidgetDataProvider {

    fun todayAsVoidDay(date: LocalDate = LocalDate.now()): DayOfWeekVoid =
        DayOfWeekVoid.valueOf(date.dayOfWeek.name)

    // -------------------------------------------------------------
    // Widget 1 — Today's Classes
    // -------------------------------------------------------------

    fun todayClasses(now: LocalDateTime = LocalDateTime.now()): TodayClassesState {
        val today = VoidRepository.scheduleFor(todayAsVoidDay(now.toLocalDate()))
        if (today.isEmpty()) return TodayClassesState.NoSchoolToday

        val slots = today.map { it.toSlotInfo() }
        val timedPeriods = today.filter { it.startTime != null }

        // If nothing has a registered start time, VOID has no basis to say
        // "current/next" or "finished" — show the full list and never
        // auto-hide rather than guess.
        if (timedPeriods.isEmpty()) {
            return TodayClassesState.InProgress(current = null, next = null, remaining = slots, allToday = slots)
        }

        val sortedByStart = timedPeriods.sortedBy { it.startTime }
        val nowTime = now.toLocalTime()

        fun effectiveEnd(period: ClassPeriod): LocalTime {
            val idx = sortedByStart.indexOf(period)
            val nextStart = sortedByStart.getOrNull(idx + 1)?.startTime
            return period.endTime ?: nextStart ?: period.startTime ?: LocalTime.MAX
        }

        val lastPeriod = sortedByStart.last()
        val hasEnded = nowTime.isAfter(effectiveEnd(lastPeriod))
        if (hasEnded) return TodayClassesState.Finished

        val current = sortedByStart.find { p ->
            val start = p.startTime ?: return@find false
            !nowTime.isBefore(start) && nowTime.isBefore(effectiveEnd(p))
        }
        val upcoming = sortedByStart.filter { (it.startTime ?: LocalTime.MAX).isAfter(nowTime) }
        val next = upcoming.firstOrNull()

        return TodayClassesState.InProgress(
            current = current?.toSlotInfo(),
            next = next?.toSlotInfo(),
            remaining = upcoming.map { it.toSlotInfo() },
            allToday = slots
        )
    }

    private fun ClassPeriod.toSlotInfo() = ClassSlotInfo(
        subjectName = VoidRepository.subjectName(subjectId),
        classType = classType,
        periodNumber = periodNumber,
        startTime = startTime,
        endTime = endTime,
        location = location
    )

    // -------------------------------------------------------------
    // Widget 2 — Today's Circle Plan
    // -------------------------------------------------------------

    fun todayCirclePlan(date: LocalDate = LocalDate.now()): TodayCirclePlanState {
        val plans = VoidRepository.circlePlansFor(todayAsVoidDay(date))
        if (plans.isEmpty()) return TodayCirclePlanState.NoPlanToday
        return TodayCirclePlanState.Content(
            plans.map { plan ->
                CirclePlanSlotInfo(
                    subjectName = VoidRepository.subjectName(plan.subjectId),
                    resolvedUnitName = plan.resolvedUnit()?.name,
                    durationMinutes = plan.durationMinutes,
                    window = plan.window.name,
                    priority = plan.priority.name
                )
            }
        )
    }

    // -------------------------------------------------------------
    // Widget 3 — Temporary Plans
    // -------------------------------------------------------------

    fun temporaryPlans(now: LocalDateTime = LocalDateTime.now()): TemporaryPlansState {
        val active = VoidRepository.temporaryTasks
            .filter { it.isActiveAt(now) }
            .sortedWith(compareBy({ it.deadline }, { it.startTime ?: LocalTime.MIN }))
        if (active.isEmpty()) return TemporaryPlansState.NoActivePlans
        return TemporaryPlansState.Content(
            active.map { task ->
                ActivePlanInfo(
                    id = task.id,
                    title = task.title,
                    subjectName = VoidRepository.subjectName(task.subjectId),
                    timeRangeLabel = task.timeRangeLabel(),
                    deadlineLabel = task.deadline.toString(),
                    progressPercent = task.progressPercent()
                )
            }
        )
    }
}
