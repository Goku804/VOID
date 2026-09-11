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

/** Assessment type — for things already graded (a Test, an Assignment, etc). NOT the same as ExamType (Exam Schedule). */
enum class AssessmentKind {
    TEST, ASSIGNMENT, MID_EXAM, FINAL_EXAM, MOCK_EXAM, QUIZ, OTHER
}

/** Exam Schedule type — a real upcoming exam event. NOT the same as AssessmentKind. */
enum class ExamType {
    TEST, MID, FINAL, MOCK
}

/**
 * D-Class is NOT a class type — it just means "an afternoon school session
 * exists that day". The actual registered type is always LANGUAGE or LAB.
 */
enum class ClassType {
    REGULAR, LANGUAGE, LAB, LIBRARY, STUDY, NIGHT_STUDY, EXAM
}

/** True if this period is a D-Class-style afternoon session (Language or Lab). */
fun ClassPeriod.isDClassSession(): Boolean = classType == ClassType.LANGUAGE || classType == ClassType.LAB

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
 * This is an already-taken assessment, distinct from the Exam Schedule below.
 */
data class AssessmentType(
    val id: String,
    val kind: AssessmentKind,
    val label: String,       // display name, e.g. "Chapter 3 Test"
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

enum class ExamSession { MORNING, AFTERNOON, EVENING, CUSTOM }

/**
 * The exam event itself: what type it is, and — for MID/FINAL/MOCK, which
 * span several calendar days — the overall period. TEST doesn't need a
 * range (it's a single-day event), so startDate/endDate stay null for it.
 * grades lives here (not on ExamSubject) because "which grade levels sit
 * this" is a property of the exam as a whole, not of any one subject —
 * meaningful mainly for MOCK.
 * Subject/date/time specifics live on ExamSubject: one Exam groups every
 * subject sitting inside it, e.g. "Final Exam" containing Math on Monday
 * and Physics on Tuesday are two ExamSubject rows sharing one Exam.
 */
data class Exam(
    val id: String,
    val examType: ExamType,
    val notes: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val grades: List<Int> = emptyList()
)

/**
 * One subject's sitting within an exam — its own date/time/session/units.
 * "Mathematics Final, Monday 08:30" and "Physics Final, Tuesday 14:00" are
 * two ExamSubject rows under the same Final Exam.
 */
data class ExamSubject(
    val id: String,
    val examId: String,
    val subjectId: String,
    val date: LocalDate,
    val time: LocalTime? = null,
    val session: ExamSession? = null,
    val location: String = "",
    val unitIds: List<String> = emptyList()
)

enum class ExamSittingStatus { UPCOMING, TODAY, STARTED, COMPLETED }

fun ExamSubject.daysRemaining(): Long =
    java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date)

/** Real status computed from device date/time — never hardcoded. */
fun ExamSubject.status(): ExamSittingStatus {
    val today = LocalDate.now()
    return when {
        date.isBefore(today) -> ExamSittingStatus.COMPLETED
        date.isAfter(today) -> ExamSittingStatus.UPCOMING
        time != null && LocalTime.now().isAfter(time) -> ExamSittingStatus.STARTED
        else -> ExamSittingStatus.TODAY
    }
}

fun ExamSittingStatus.label(daysRemaining: Long): String = when (this) {
    ExamSittingStatus.COMPLETED -> "COMPLETED"
    ExamSittingStatus.STARTED -> "STARTED"
    ExamSittingStatus.TODAY -> "TODAY"
    ExamSittingStatus.UPCOMING -> "$daysRemaining DAYS LEFT"
}

/**
 * Urgent Plan window: automatic, not a manually registered plan. Any Mid,
 * Final, or Mock exam sitting entering 16-20 days remaining is "urgent" —
 * this should drive real planning logic later (Exam Prep Engine, v0.11.0),
 * not just a visual label.
 */
fun ExamSubject.isUrgent(): Boolean = daysRemaining() in 16..20

/** Every subject sitting under this exam, earliest first — what a grouped Exam card iterates over. */
fun Exam.subjectsSorted(): List<ExamSubject> =
    VoidRepository.examSubjects
        .filter { it.examId == id }
        .sortedWith(compareBy({ it.date }, { it.time ?: LocalTime.MIN }))

/** The next sitting inside this exam that hasn't happened yet — drives the exam-level countdown on the Home card. */
fun Exam.nearestUpcomingSubject(): ExamSubject? =
    subjectsSorted().firstOrNull { it.status() != ExamSittingStatus.COMPLETED }

/** True once every subject sitting inside this exam is in the past. */
fun Exam.isFullyCompleted(): Boolean {
    val subjects = subjectsSorted()
    return subjects.isNotEmpty() && subjects.all { it.status() == ExamSittingStatus.COMPLETED }
}

/** Every distinct unit id covered anywhere inside this exam, across all its subjects. */
fun Exam.allUnitIds(): List<String> = subjectsSorted().flatMap { it.unitIds }.distinct()

/** A one-off task — homework, urgent revision, anything not part of the normal plan. */
enum class TemporaryPlanType {
    TEST, ASSIGNMENT, HOMEWORK, EXTRA_CLASS, TEACHER_REQUEST,
    PROJECT, MAKE_UP_CLASS, URGENT_REVISION, MISSED_WORK_RECOVERY, OTHER
}

enum class PlanTaskStatus { PLANNED, IN_PROGRESS, COMPLETED, PARTIAL, MISSED, CANCELLED }

/**
 * Short-term interruption or addition — never deletes a Circle Plan slot,
 * the (future) Priority Engine reschedules the Circle Plan around it instead.
 *
 * startTime/endTime are optional, independent of startDate/deadline: a task
 * can be date-scoped only ("due 2026-09-12") or additionally clock-scoped
 * ("18:00 -> 19:30" that same day) — VOID Smart Widgets shows the clock
 * range when both are set, and the plain date otherwise.
 */
data class TemporaryTask(
    val id: String,
    val title: String,
    val type: TemporaryPlanType = TemporaryPlanType.OTHER,
    val subjectId: String? = null,
    val startDate: LocalDate? = null,
    val deadline: LocalDate,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val requiredMinutes: Int = 0,
    val completedMinutes: Int = 0,
    val priority: PlanPriority = PlanPriority.NORMAL,
    val unitIds: List<String> = emptyList(),
    val notes: String = "",
    val status: PlanTaskStatus = PlanTaskStatus.PLANNED
)

fun TemporaryTask.daysUntilDeadline(): Long =
    java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), deadline)

fun TemporaryTask.progressPercent(): Int =
    if (requiredMinutes <= 0) 0
    else ((completedMinutes.toFloat() / requiredMinutes) * 100).toInt().coerceIn(0, 100)

fun TemporaryTask.isOverdue(): Boolean =
    status != PlanTaskStatus.COMPLETED && status != PlanTaskStatus.CANCELLED && daysUntilDeadline() < 0

/** "18:00 -> 19:30" when both clock times are set, "18:00" with only a start, or null when the task is date-only. */
fun TemporaryTask.timeRangeLabel(): String? = when {
    startTime != null && endTime != null -> "$startTime \u2192 $endTime"
    startTime != null -> startTime.toString()
    else -> null
}

/**
 * Whether this task is "live" at the given moment for widget purposes —
 * the single place that decides it, so no widget or screen reimplements
 * this check differently. Not completed/cancelled, not before its start
 * date, and not past its deadline (clock-aware when startTime/endTime are
 * set; otherwise the whole deadline day counts).
 */
fun TemporaryTask.isActiveAt(now: java.time.LocalDateTime): Boolean {
    if (status == PlanTaskStatus.COMPLETED || status == PlanTaskStatus.CANCELLED) return false
    val today = now.toLocalDate()
    if (startDate != null && today.isBefore(startDate)) return false
    if (today.isAfter(deadline)) return false
    if (today.isBefore(deadline)) return true
    // Today IS the deadline day — clock-aware cutoff when an end time was set.
    val cutoff = endTime ?: java.time.LocalTime.MAX
    return !now.toLocalTime().isAfter(cutoff)
}

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

// ---------------------------------------------------------------------
// Study Session — the actual "timer is running" runtime state. Circle
// Plan/Temporary Plan describe what SHOULD be studied; a StudySession is
// VOID recording that the user actually started a clock on one of them.
// This is what EXECUTE and Guardian both key off — "planned" and "active"
// are different things, and this is what makes "active" real.
// ---------------------------------------------------------------------

enum class StudySessionSource { CIRCLE_PLAN, TEMPORARY_TASK, MANUAL }
enum class StudySessionStatus { ACTIVE, COMPLETED, ABANDONED }

data class StudySession(
    val id: String,
    val source: StudySessionSource,
    val subjectId: String?,
    val circlePlanId: String? = null,
    val temporaryTaskId: String? = null,
    val unitId: String? = null,
    val label: String,
    val plannedMinutes: Int,
    val startedAt: java.time.LocalDateTime,
    val endedAt: java.time.LocalDateTime? = null,
    val status: StudySessionStatus = StudySessionStatus.ACTIVE
)

fun StudySession.elapsedMinutes(now: java.time.LocalDateTime = java.time.LocalDateTime.now()): Int {
    val until = endedAt ?: now
    return java.time.Duration.between(startedAt, until).toMinutes().toInt().coerceAtLeast(0)
}

/** Never negative — once elapsed passes plannedMinutes, the session is simply running over, not "-5 minutes left". */
fun StudySession.remainingMinutes(now: java.time.LocalDateTime = java.time.LocalDateTime.now()): Int =
    (plannedMinutes - elapsedMinutes(now)).coerceAtLeast(0)

fun StudySession.isOverPlanned(now: java.time.LocalDateTime = java.time.LocalDateTime.now()): Boolean =
    elapsedMinutes(now) > plannedMinutes
