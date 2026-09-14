package com.core.voidapp.data.report

import com.core.voidapp.data.ClassPeriod
import com.core.voidapp.data.DayOfWeekVoid
import com.core.voidapp.data.StudySession
import com.core.voidapp.data.StudySessionSource
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.isDClassSession
import com.core.voidapp.data.resolvedUnit
import java.time.LocalDate

/** One planned-or-actual study item for a day, resolved against any StudySession that actually ran for it. */
data class PlannedStudyItem(
    val label: String,
    val subjectId: String?,
    val circlePlanId: String? = null,
    val temporaryTaskId: String? = null,
    val matchedSession: StudySession?
)

object DailyReportEngine {

    fun classPeriodsFor(date: LocalDate): List<ClassPeriod> =
        VoidRepository.scheduleFor(DayOfWeekVoid.valueOf(date.dayOfWeek.name))

    /** The afternoon D-Class slot(s) for the day, if any — Language or Lab, never a generic "D-Class subject" (spec #4). */
    fun languageOrLabPeriodsFor(date: LocalDate): List<ClassPeriod> =
        classPeriodsFor(date).filter { it.isDClassSession() }

    fun studySessionsFor(date: LocalDate): List<StudySession> =
        VoidRepository.studySessions.filter { it.startedAt.toLocalDate() == date }

    /**
     * Every study-relevant item for the day — Circle Plan slots, Temporary
     * Plans due that day, and any manually-started session — each matched
     * against a real StudySession where one actually ran. An item with no
     * matchedSession is what the Study Report should ask about as
     * Missed; one with a session lets the report ask real completion
     * questions instead of guessing.
     */
    fun plannedStudyItemsFor(date: LocalDate): List<PlannedStudyItem> {
        val sessions = studySessionsFor(date)

        val circleItems = VoidRepository.circlePlansForToday(date).map { plan ->
            val label = plan.resolvedUnit()?.name ?: VoidRepository.subjectName(plan.subjectId)
            PlannedStudyItem(
                label = label, subjectId = plan.subjectId, circlePlanId = plan.id,
                matchedSession = sessions.find { it.circlePlanId == plan.id }
            )
        }

        val tempItems = VoidRepository.temporaryTasksForDay(date).map { task ->
            PlannedStudyItem(
                label = task.title, subjectId = task.subjectId, temporaryTaskId = task.id,
                matchedSession = sessions.find { it.temporaryTaskId == task.id }
            )
        }

        // A session started ad hoc (not from a Circle Plan slot or Temporary Plan) still happened today and deserves a report entry.
        val linkedSessionIds = (circleItems.mapNotNull { it.matchedSession?.id } + tempItems.mapNotNull { it.matchedSession?.id }).toSet()
        val manualItems = sessions
            .filter { it.id !in linkedSessionIds }
            .map { s -> PlannedStudyItem(label = s.label, subjectId = s.subjectId, matchedSession = s) }

        return circleItems + tempItems + manualItems
    }

    fun alreadyReportedClassPeriodIds(dailyReportId: String): Set<String> =
        VoidRepository.classReportsFor(dailyReportId).map { it.classPeriodId }.toSet()

    fun alreadyReportedStudyLabels(dailyReportId: String): Set<String> =
        VoidRepository.studyReportsFor(dailyReportId).map { it.label }.toSet()
}
