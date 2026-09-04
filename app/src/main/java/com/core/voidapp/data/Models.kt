package com.core.voidapp.data

import java.time.LocalDate
import java.time.LocalTime

/**
 * VOID core data models.
 * Nothing here is hardcoded content — these are just the shapes.
 * All actual data (subjects, schedule, exams, marks) is entered by the user.
 */

enum class DayOfWeekVoid {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

enum class ExamType {
    TEST, ASSIGNMENT, MID, FINAL, MOCK
}

enum class ClassType {
    REGULAR, D_CLASS, LANGUAGE, LAB, LIBRARY, STUDY, NIGHT_STUDY, EXAM
}

/** Subject code, e.g. "MATH" — short label used in dense timetable views. */
data class Subject(
    val id: String,
    val name: String,
    val grade: Int,
    val code: String = "",
    val assessmentTypes: MutableList<AssessmentType> = mutableListOf()
)

/**
 * One graded component inside a subject (e.g. "Mid" worth 20%).
 * weightPercent is user-defined per subject — not shared globally.
 */
data class AssessmentType(
    val id: String,
    val examType: ExamType,
    val label: String,       // display name, e.g. "Mid Exam"
    val weightPercent: Double,
    val maxScore: Double,
    var entry: MarkEntry? = null
)

/** The actual score the user got for one AssessmentType. */
data class MarkEntry(
    val score: Double,
    val dateRecorded: LocalDate = LocalDate.now()
)

/**
 * One period in the weekly class schedule.
 * day + period together define the slot; subjectId points at a Subject.
 */
data class ClassPeriod(
    val id: String,
    val day: DayOfWeekVoid,
    val periodNumber: Int,
    val subjectId: String,
    val classType: ClassType = ClassType.REGULAR,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val location: String? = null
)

/**
 * A unit within a subject (e.g. Mathematics Grade 11 Unit 2 — Functions).
 * Belongs to Academic data — Circle Plans and Exam Prep read from this later.
 * Named AcademicUnit to avoid clashing with kotlin.Unit.
 */
data class AcademicUnit(
    val id: String,
    val subjectId: String,
    val unitNumber: Int,
    val name: String,
    val description: String = "",
    val estimatedStudyMinutes: Int = 0
)

enum class PreferredWindow { MORNING, AFTERNOON, NIGHT, ANYTIME }

/** CONTINUE_NEXT_UNIT walks through the subject's units via a manual cursor. FIXED_UNIT stays locked on one. */
enum class ContentStrategy { CONTINUE_NEXT_UNIT, FIXED_UNIT }

enum class PlanPriority { LOW, NORMAL, HIGH }

/**
 * A recurring weekly slot: "Monday -> Mathematics". Persistent — survives
 * Temporary Plans and exams, which only interrupt/overlay it, never delete it.
 * Content is NOT the same unit forever: currentUnitIndex is a manual cursor
 * the user advances/rewinds through the subject's unit list.
 */
data class CirclePlan(
    val id: String,
    val day: DayOfWeekVoid,
    val subjectId: String,
    val durationMinutes: Int,
    val window: PreferredWindow,
    val strategy: ContentStrategy,
    val currentUnitIndex: Int = 0,
    val fixedUnitId: String? = null,
    val priority: PlanPriority = PlanPriority.NORMAL
)

/** Resolves which unit is "due" right now for this Circle Plan. */
fun CirclePlan.resolvedUnit(): AcademicUnit? = when (strategy) {
    ContentStrategy.FIXED_UNIT -> VoidRepository.units.find { it.id == fixedUnitId }
    ContentStrategy.CONTINUE_NEXT_UNIT -> {
        val units = VoidRepository.unitsFor(subjectId)
        if (units.isEmpty()) null else units[currentUnitIndex.mod(units.size)]
    }
}

/**
 * Night study availability for one day of the week. Must be configurable
 * per day — VOID must never assume every night has the same window.
 */
data class NightAvailability(
    val day: DayOfWeekVoid,
    val available: Boolean,
    val start: LocalTime? = null,
    val end: LocalTime? = null
)

/**
 * An exam/test event with a real date, used to drive the live countdown.
 */
data class Exam(
    val id: String,
    val subjectId: String,
    val type: ExamType,
    val title: String,
    val date: LocalDate
)

/** A one-off task — homework, urgent revision, anything not part of the normal plan. */
data class TemporaryTask(
    val id: String,
    val title: String,
    val subjectId: String? = null,
    val dueDate: LocalDate,
    val isCompleted: Boolean = false,
    val isHighPriority: Boolean = false
)

/**
 * Computes weighted total (0-100 scale) for a subject from whatever
 * AssessmentTypes currently have an entry. Missing entries are skipped,
 * not counted as zero, so a partial term still shows a fair running total.
 */
fun Subject.weightedTotal(): Double {
    val graded = assessmentTypes.filter { it.entry != null }
    if (graded.isEmpty()) return 0.0

    val totalWeightGraded = graded.sumOf { it.weightPercent }
    if (totalWeightGraded == 0.0) return 0.0

    val earned = graded.sumOf { type ->
        val pct = (type.entry!!.score / type.maxScore) * type.weightPercent
        pct
    }

    // Scale to the weight actually graded so an in-progress term still reads 0-100 fairly.
    return earned
}

/** Sum of all weight percentages currently defined for the subject. Should equal 100. */
fun Subject.totalWeight(): Double = assessmentTypes.sumOf { it.weightPercent }

/** True once every AssessmentType for the subject has a recorded score. */
fun Subject.isFullyGraded(): Boolean = assessmentTypes.isNotEmpty() && assessmentTypes.all { it.entry != null }

/** Days between today and the exam date. Negative if the date has passed. */
fun Exam.daysRemaining(): Long =
    java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date)

/** Exact years / months / days remaining, for a readable long-range countdown. */
data class Countdown(val years: Int, val months: Int, val days: Int, val totalDays: Long)

fun Exam.countdown(): Countdown {
    val today = LocalDate.now()
    val period = java.time.Period.between(today, date)
    val totalDays = java.time.temporal.ChronoUnit.DAYS.between(today, date)
    return Countdown(
        years = period.years,
        months = period.months,
        days = period.days,
        totalDays = totalDays
    )
}
