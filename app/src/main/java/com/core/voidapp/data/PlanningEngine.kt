package com.core.voidapp.data

/**
 * One thing worth the user's attention right now, with a plain-language
 * reason. Produced only by PlanningEngine's deterministic rules — never
 * by the AI. The AI Chat explains and elaborates on this list; it must
 * never invent its own competing schedule.
 */
data class FocusItem(
    val label: String,
    val detail: String,
    val minutes: Int,
    val reason: String,
    val sourceType: FocusSourceType
)

enum class FocusSourceType { OVERDUE_TASK, URGENT_EXAM, TEMPORARY_TASK, CIRCLE_PLAN }

/**
 * Deterministic, rule-based ranking of what deserves focus right now.
 * This is the single source of truth AI Chat must defer to when asked
 * things like "what should I study tonight" — it reads real
 * VoidRepository data and ranks it by fixed rules, nothing guessed.
 *
 * Order:
 *   1. Overdue temporary tasks (missed deadlines)
 *   2. Exam sittings that are TODAY/STARTED or within 5 days
 *   3. Active (non-overdue) temporary tasks, soonest deadline first
 *   4. Exam sittings inside the automatic 16-20 day urgent window
 *   5. Today's Circle Plan slots
 */
object PlanningEngine {

    fun rankedFocus(day: DayOfWeekVoid = todayAsVoidDay()): List<FocusItem> {
        val items = mutableListOf<FocusItem>()

        VoidRepository.activeTemporaryTasks().filter { it.isOverdue() }.forEach { t ->
            items += FocusItem(
                label = t.title,
                detail = VoidRepository.subjectName(t.subjectId),
                minutes = (t.requiredMinutes - t.completedMinutes).coerceAtLeast(0),
                reason = "Overdue by ${-t.daysUntilDeadline()} day(s)",
                sourceType = FocusSourceType.OVERDUE_TASK
            )
        }

        VoidRepository.upcomingExamSubjects().filter { es ->
            val s = es.status()
            s == ExamSittingStatus.TODAY || s == ExamSittingStatus.STARTED || es.daysRemaining() in 0..5
        }.forEach { es ->
            val exam = VoidRepository.examFor(es)
            items += FocusItem(
                label = "${exam?.examType?.name ?: "Exam"} \u00b7 ${VoidRepository.subjectName(es.subjectId)}",
                detail = es.status().label(es.daysRemaining()),
                minutes = 0,
                reason = "Exam sitting is imminent",
                sourceType = FocusSourceType.URGENT_EXAM
            )
        }

        VoidRepository.activeTemporaryTasks().filter { !it.isOverdue() }.forEach { t ->
            items += FocusItem(
                label = t.title,
                detail = VoidRepository.subjectName(t.subjectId),
                minutes = (t.requiredMinutes - t.completedMinutes).coerceAtLeast(0),
                reason = "Due in ${t.daysUntilDeadline()} day(s)",
                sourceType = FocusSourceType.TEMPORARY_TASK
            )
        }

        VoidRepository.urgentExamSubjects().forEach { es ->
            val exam = VoidRepository.examFor(es)
            items += FocusItem(
                label = "${exam?.examType?.name ?: "Exam"} \u00b7 ${VoidRepository.subjectName(es.subjectId)}",
                detail = es.status().label(es.daysRemaining()),
                minutes = 0,
                reason = "Inside the automatic exam-prep window (16-20 days out)",
                sourceType = FocusSourceType.URGENT_EXAM
            )
        }

        VoidRepository.circlePlansFor(day).forEach { plan ->
            items += FocusItem(
                label = VoidRepository.subjectName(plan.subjectId),
                detail = plan.resolvedUnit()?.name ?: "No unit set",
                minutes = plan.durationMinutes,
                reason = "Scheduled Circle Plan for today",
                sourceType = FocusSourceType.CIRCLE_PLAN
            )
        }

        return items
    }

    private fun todayAsVoidDay(): DayOfWeekVoid {
        val d = java.time.LocalDate.now().dayOfWeek
        return DayOfWeekVoid.valueOf(d.name)
    }
}
