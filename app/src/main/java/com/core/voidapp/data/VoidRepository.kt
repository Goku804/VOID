package com.core.voidapp.data

import androidx.compose.runtime.mutableStateListOf
import java.util.UUID

/**
 * Single in-memory source of truth for the whole app.
 * NOT persisted yet — data is lost on app restart.
 * This is intentional for now (per the plan): get the UI and data flow
 * working first, add Room once the shape of the data is proven out.
 *
 * Everything here starts EMPTY. No hardcoded subjects, schedule, or exams —
 * the user fills all of it in through the Setup / Academic / Plan screens.
 */
object VoidRepository {

    val subjects = mutableStateListOf<Subject>()
    val units = mutableStateListOf<AcademicUnit>()
    val classPeriods = mutableStateListOf<ClassPeriod>()
    val exams = mutableStateListOf<Exam>()
    val temporaryTasks = mutableStateListOf<TemporaryTask>()
    val nightAvailability = mutableStateListOf<NightAvailability>()
    val circlePlans = mutableStateListOf<CirclePlan>()

    fun newId(): String = UUID.randomUUID().toString()

    fun addSubject(name: String, grade: Int, code: String = ""): Subject {
        val subject = Subject(id = newId(), name = name, grade = grade, code = code)
        subjects.add(subject)
        return subject
    }

    fun addUnit(
        subjectId: String,
        unitNumber: Int,
        name: String,
        description: String = "",
        estimatedStudyMinutes: Int = 0
    ): AcademicUnit {
        val unit = AcademicUnit(
            id = newId(),
            subjectId = subjectId,
            unitNumber = unitNumber,
            name = name,
            description = description,
            estimatedStudyMinutes = estimatedStudyMinutes
        )
        units.add(unit)
        return unit
    }

    fun unitsFor(subjectId: String): List<AcademicUnit> =
        units.filter { it.subjectId == subjectId }.sortedBy { it.unitNumber }

    fun deleteUnit(unitId: String) {
        units.removeAll { it.id == unitId }
    }

    fun addAssessmentType(
        subjectId: String,
        examType: ExamType,
        label: String,
        weightPercent: Double,
        maxScore: Double
    ) {
        val subject = subjects.find { it.id == subjectId } ?: return
        subject.assessmentTypes.add(
            AssessmentType(
                id = newId(),
                examType = examType,
                label = label,
                weightPercent = weightPercent,
                maxScore = maxScore
            )
        )
    }

    fun recordScore(subjectId: String, assessmentTypeId: String, score: Double) {
        val subject = subjects.find { it.id == subjectId } ?: return
        val type = subject.assessmentTypes.find { it.id == assessmentTypeId } ?: return
        type.entry = MarkEntry(score = score)
    }

    fun addClassPeriod(
        day: DayOfWeekVoid,
        periodNumber: Int,
        subjectId: String,
        classType: ClassType = ClassType.REGULAR,
        startTime: java.time.LocalTime? = null,
        endTime: java.time.LocalTime? = null
    ) {
        classPeriods.add(
            ClassPeriod(
                id = newId(),
                day = day,
                periodNumber = periodNumber,
                subjectId = subjectId,
                classType = classType,
                startTime = startTime,
                endTime = endTime
            )
        )
    }

    fun deleteClassPeriod(periodId: String) {
        classPeriods.removeAll { it.id == periodId }
    }

    fun scheduleFor(day: DayOfWeekVoid): List<ClassPeriod> =
        classPeriods.filter { it.day == day }.sortedBy { it.periodNumber }

    /** Current setting for a day, or a sensible "not configured" default if never set. */
    fun nightAvailabilityFor(day: DayOfWeekVoid): NightAvailability =
        nightAvailability.find { it.day == day } ?: NightAvailability(day = day, available = false)

    fun setNightAvailability(day: DayOfWeekVoid, available: Boolean, start: java.time.LocalTime?, end: java.time.LocalTime?) {
        val idx = nightAvailability.indexOfFirst { it.day == day }
        val updated = NightAvailability(day = day, available = available, start = start, end = end)
        if (idx == -1) nightAvailability.add(updated) else nightAvailability[idx] = updated
    }

    fun addCirclePlan(
        day: DayOfWeekVoid,
        subjectId: String,
        durationMinutes: Int,
        window: PreferredWindow,
        strategy: ContentStrategy,
        fixedUnitId: String? = null,
        priority: PlanPriority = PlanPriority.NORMAL
    ): CirclePlan {
        val plan = CirclePlan(
            id = newId(),
            day = day,
            subjectId = subjectId,
            durationMinutes = durationMinutes,
            window = window,
            strategy = strategy,
            fixedUnitId = fixedUnitId,
            priority = priority
        )
        circlePlans.add(plan)
        return plan
    }

    fun deleteCirclePlan(planId: String) {
        circlePlans.removeAll { it.id == planId }
    }

    /** Manually step the CONTINUE_NEXT_UNIT cursor forward (+1) or back (-1). Wraps around. */
    fun stepCircleUnit(planId: String, delta: Int) {
        val idx = circlePlans.indexOfFirst { it.id == planId }
        if (idx == -1) return
        val plan = circlePlans[idx]
        val units = unitsFor(plan.subjectId)
        if (units.isEmpty()) return
        val newIndex = (plan.currentUnitIndex + delta).mod(units.size)
        circlePlans[idx] = plan.copy(currentUnitIndex = newIndex)
    }

    fun circlePlansFor(day: DayOfWeekVoid): List<CirclePlan> =
        circlePlans.filter { it.day == day }

    fun addExam(subjectId: String, type: ExamType, title: String, date: java.time.LocalDate): Exam {
        val exam = Exam(id = newId(), subjectId = subjectId, type = type, title = title, date = date)
        exams.add(exam)
        return exam
    }

    fun upcomingExams(): List<Exam> =
        exams.filter { it.daysRemaining() >= 0 }.sortedBy { it.date }

    fun nearestExam(): Exam? = upcomingExams().firstOrNull()

    fun subjectName(subjectId: String?): String =
        subjects.find { it.id == subjectId }?.name ?: "—"

    fun addTemporaryTask(
        title: String,
        type: TemporaryPlanType,
        subjectId: String?,
        startDate: java.time.LocalDate?,
        deadline: java.time.LocalDate,
        requiredMinutes: Int,
        priority: PlanPriority,
        unitIds: List<String> = emptyList(),
        notes: String = ""
    ): TemporaryTask {
        val task = TemporaryTask(
            id = newId(),
            title = title,
            type = type,
            subjectId = subjectId,
            startDate = startDate,
            deadline = deadline,
            requiredMinutes = requiredMinutes,
            priority = priority,
            unitIds = unitIds,
            notes = notes
        )
        temporaryTasks.add(task)
        return task
    }

    fun addProgress(taskId: String, minutes: Int) {
        val idx = temporaryTasks.indexOfFirst { it.id == taskId }
        if (idx == -1) return
        val task = temporaryTasks[idx]
        val newCompleted = (task.completedMinutes + minutes).coerceAtLeast(0)
        val newStatus = when {
            task.requiredMinutes > 0 && newCompleted >= task.requiredMinutes -> PlanTaskStatus.COMPLETED
            newCompleted > 0 -> PlanTaskStatus.IN_PROGRESS
            else -> PlanTaskStatus.PLANNED
        }
        temporaryTasks[idx] = task.copy(completedMinutes = newCompleted, status = newStatus)
    }

    fun setTaskStatus(taskId: String, status: PlanTaskStatus) {
        val idx = temporaryTasks.indexOfFirst { it.id == taskId }
        if (idx == -1) return
        temporaryTasks[idx] = temporaryTasks[idx].copy(status = status)
    }

    fun deleteTemporaryTask(taskId: String) {
        temporaryTasks.removeAll { it.id == taskId }
    }

    fun activeTemporaryTasks(): List<TemporaryTask> =
        temporaryTasks.filter { it.status != PlanTaskStatus.COMPLETED && it.status != PlanTaskStatus.CANCELLED }
            .sortedBy { it.deadline }

    fun temporaryTasksForDay(day: java.time.LocalDate): List<TemporaryTask> =
        temporaryTasks.filter { it.deadline == day }
}
