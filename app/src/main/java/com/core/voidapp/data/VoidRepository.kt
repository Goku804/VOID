package com.core.voidapp.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.core.voidapp.data.db.VoidDatabase
import com.core.voidapp.data.db.toEntity
import com.core.voidapp.data.db.toModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID

/**
 * Single in-memory source of truth for the whole app, backed by Room.
 *
 * Design: the in-memory mutableStateListOf collections stay exactly as
 * before (every screen keeps reading/writing them the same way — no screen
 * needed to change). init() loads everything from Room once at startup;
 * every mutation function additionally writes through to Room on a
 * background coroutine. This keeps persistence real without touching the
 * ~10 screen files that already read these lists directly.
 *
 * See VOID_DATABASE_SCHEMA.md for the full schema design and the two
 * places (Assessments, Exams) where the "real" relational shape differs
 * from what's implemented today — those are deferred to when their
 * screens get rebuilt, not silently done here.
 */
object VoidRepository {

    val subjects = mutableStateListOf<Subject>()
    val units = mutableStateListOf<AcademicUnit>()
    val classPeriods = mutableStateListOf<ClassPeriod>()
    val exams = mutableStateListOf<Exam>()
    val examSubjects = mutableStateListOf<ExamSubject>()
    val temporaryTasks = mutableStateListOf<TemporaryTask>()
    val nightAvailability = mutableStateListOf<NightAvailability>()
    val circlePlans = mutableStateListOf<CirclePlan>()

    private var database: VoidDatabase? = null
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Call once, before the first screen renders (from the Application
     * class). Loads everything from disk synchronously — the dataset is
     * small (personal use, not multi-user), so a brief blocking load on
     * startup is simpler and safer than wiring Flow through every screen.
     */
    fun init(context: Context) {
        if (database != null) return
        val db = VoidDatabase.getInstance(context)
        database = db

        runBlocking(Dispatchers.IO) {
            val subjectEntities = db.subjectDao().getAll()
            val typeEntities = db.assessmentTypeDao().getAll()

            subjects.addAll(
                subjectEntities.map { se ->
                    se.toModel().also { subject ->
                        subject.assessmentTypes.addAll(
                            typeEntities.filter { it.subjectId == se.id }.map { it.toModel() }
                        )
                    }
                }
            )
            units.addAll(db.unitDao().getAll().map { it.toModel() })
            classPeriods.addAll(db.classPeriodDao().getAll().map { it.toModel() })
            nightAvailability.addAll(db.nightAvailabilityDao().getAll().map { it.toModel() })
            exams.addAll(db.examDao().getAll().map { it.toModel() })
            examSubjects.addAll(db.examSubjectDao().getAll().map { it.toModel() })
            circlePlans.addAll(db.circlePlanDao().getAll().map { it.toModel() })
            temporaryTasks.addAll(db.temporaryTaskDao().getAll().map { it.toModel() })
        }
    }

    fun newId(): String = UUID.randomUUID().toString()

    fun addSubject(name: String, grade: Int, code: String = ""): Subject {
        val subject = Subject(id = newId(), name = name, grade = grade, code = code)
        subjects.add(subject)
        persistSubject(subject)
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
        ioScope.launch { database?.unitDao()?.upsert(unit.toEntity()) }
        return unit
    }

    fun unitsFor(subjectId: String): List<AcademicUnit> =
        units.filter { it.subjectId == subjectId }.sortedBy { it.unitNumber }

    fun deleteUnit(unitId: String) {
        units.removeAll { it.id == unitId }
        ioScope.launch { database?.unitDao()?.deleteById(unitId) }
    }

    fun addAssessmentType(
        subjectId: String,
        kind: AssessmentKind,
        label: String,
        weightPercent: Double,
        maxScore: Double
    ) {
        val subject = subjects.find { it.id == subjectId } ?: return
        val type = AssessmentType(
            id = newId(),
            kind = kind,
            label = label,
            weightPercent = weightPercent,
            maxScore = maxScore
        )
        subject.assessmentTypes.add(type)
        ioScope.launch { database?.assessmentTypeDao()?.upsert(type.toEntity(subjectId)) }
    }

    fun recordScore(subjectId: String, assessmentTypeId: String, score: Double) {
        val subject = subjects.find { it.id == subjectId } ?: return
        val type = subject.assessmentTypes.find { it.id == assessmentTypeId } ?: return
        type.entry = MarkEntry(score = score)
        ioScope.launch { database?.assessmentTypeDao()?.upsert(type.toEntity(subjectId)) }
    }

    fun addClassPeriod(
        day: DayOfWeekVoid,
        periodNumber: Int,
        subjectId: String,
        classType: ClassType = ClassType.REGULAR,
        startTime: java.time.LocalTime? = null,
        endTime: java.time.LocalTime? = null
    ) {
        val period = ClassPeriod(
            id = newId(),
            day = day,
            periodNumber = periodNumber,
            subjectId = subjectId,
            classType = classType,
            startTime = startTime,
            endTime = endTime
        )
        classPeriods.add(period)
        ioScope.launch { database?.classPeriodDao()?.upsert(period.toEntity()) }
    }

    fun deleteClassPeriod(periodId: String) {
        classPeriods.removeAll { it.id == periodId }
        ioScope.launch { database?.classPeriodDao()?.deleteById(periodId) }
    }

    fun scheduleFor(day: DayOfWeekVoid): List<ClassPeriod> =
        classPeriods.filter { it.day == day }.sortedBy { it.periodNumber }

    /** Distinct days that already have a D-Class-style afternoon session (Language/Lab). Soft max is 2/week — user-configured, never assumed. */
    fun dClassDayCount(): Int = classPeriods.filter { it.isDClassSession() }.map { it.day }.distinct().size

    /** Current setting for a day, or a sensible "not configured" default if never set. */
    fun nightAvailabilityFor(day: DayOfWeekVoid): NightAvailability =
        nightAvailability.find { it.day == day } ?: NightAvailability(day = day, available = false)

    fun setNightAvailability(day: DayOfWeekVoid, available: Boolean, start: java.time.LocalTime?, end: java.time.LocalTime?) {
        val idx = nightAvailability.indexOfFirst { it.day == day }
        val updated = NightAvailability(day = day, available = available, start = start, end = end)
        if (idx == -1) nightAvailability.add(updated) else nightAvailability[idx] = updated
        ioScope.launch { database?.nightAvailabilityDao()?.upsert(updated.toEntity()) }
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
        ioScope.launch { database?.circlePlanDao()?.upsert(plan.toEntity()) }
        return plan
    }

    fun deleteCirclePlan(planId: String) {
        circlePlans.removeAll { it.id == planId }
        ioScope.launch { database?.circlePlanDao()?.deleteById(planId) }
    }

    /** Manually step the CONTINUE_NEXT_UNIT cursor forward (+1) or back (-1). Wraps around. */
    fun stepCircleUnit(planId: String, delta: Int) {
        val idx = circlePlans.indexOfFirst { it.id == planId }
        if (idx == -1) return
        val plan = circlePlans[idx]
        val units = unitsFor(plan.subjectId)
        if (units.isEmpty()) return
        val newIndex = (plan.currentUnitIndex + delta).mod(units.size)
        val updated = plan.copy(currentUnitIndex = newIndex)
        circlePlans[idx] = updated
        ioScope.launch { database?.circlePlanDao()?.upsert(updated.toEntity()) }
    }

    fun circlePlansFor(day: DayOfWeekVoid): List<CirclePlan> =
        circlePlans.filter { it.day == day }

    /**
     * Registers a full exam sitting in one step: creates the parent Exam
     * (type + notes) and its ExamSubject (date/time/session/units/grades)
     * together. Multiple sittings can later share one Exam by calling this
     * again with the same examId if that grouping UI gets built.
     */
    fun registerExam(
        examType: ExamType,
        subjectId: String,
        date: java.time.LocalDate,
        time: java.time.LocalTime?,
        session: ExamSession?,
        location: String = "",
        unitIds: List<String> = emptyList(),
        grades: List<Int> = emptyList(),
        notes: String = ""
    ): ExamSubject {
        val exam = Exam(id = newId(), examType = examType, notes = notes)
        exams.add(exam)
        ioScope.launch { database?.examDao()?.upsert(exam.toEntity()) }

        val examSubject = ExamSubject(
            id = newId(),
            examId = exam.id,
            subjectId = subjectId,
            date = date,
            time = time,
            session = session,
            location = location,
            unitIds = unitIds,
            grades = grades
        )
        examSubjects.add(examSubject)
        ioScope.launch { database?.examSubjectDao()?.upsert(examSubject.toEntity()) }
        return examSubject
    }

    fun deleteExam(examId: String) {
        exams.removeAll { it.id == examId }
        examSubjects.removeAll { it.examId == examId }
        ioScope.launch {
            database?.examDao()?.deleteById(examId)
            database?.examSubjectDao()?.deleteByExamId(examId)
        }
    }

    fun examFor(examSubject: ExamSubject): Exam? = exams.find { it.id == examSubject.examId }

    fun upcomingExamSubjects(): List<ExamSubject> =
        examSubjects.filter { it.status() != ExamSittingStatus.COMPLETED }.sortedBy { it.date }

    fun nearestExamSubject(): ExamSubject? = upcomingExamSubjects().firstOrNull()

    /** Any Mid/Final/Mock sitting in the automatic 16-20 day Urgent Plan window. */
    fun urgentExamSubjects(): List<ExamSubject> =
        examSubjects.filter { it.isUrgent() }.sortedBy { it.date }

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
        ioScope.launch { database?.temporaryTaskDao()?.upsert(task.toEntity()) }
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
        val updated = task.copy(completedMinutes = newCompleted, status = newStatus)
        temporaryTasks[idx] = updated
        ioScope.launch { database?.temporaryTaskDao()?.upsert(updated.toEntity()) }
    }

    fun setTaskStatus(taskId: String, status: PlanTaskStatus) {
        val idx = temporaryTasks.indexOfFirst { it.id == taskId }
        if (idx == -1) return
        val updated = temporaryTasks[idx].copy(status = status)
        temporaryTasks[idx] = updated
        ioScope.launch { database?.temporaryTaskDao()?.upsert(updated.toEntity()) }
    }

    fun deleteTemporaryTask(taskId: String) {
        temporaryTasks.removeAll { it.id == taskId }
        ioScope.launch { database?.temporaryTaskDao()?.deleteById(taskId) }
    }

    fun activeTemporaryTasks(): List<TemporaryTask> =
        temporaryTasks.filter { it.status != PlanTaskStatus.COMPLETED && it.status != PlanTaskStatus.CANCELLED }
            .sortedBy { it.deadline }

    fun temporaryTasksForDay(day: java.time.LocalDate): List<TemporaryTask> =
        temporaryTasks.filter { it.deadline == day }

    private fun persistSubject(subject: Subject) {
        ioScope.launch { database?.subjectDao()?.upsert(subject.toEntity()) }
    }
}
