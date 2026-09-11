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
    val chatConversations = mutableStateListOf<ChatConversation>()
    val chatMessages = mutableStateListOf<ChatMessage>()
    val studySessions = mutableStateListOf<StudySession>()

    private var database: VoidDatabase? = null
    private var appContext: Context? = null
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
        appContext = context.applicationContext

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
            chatConversations.addAll(db.chatDao().getAllConversations().map { it.toModel() })
            chatMessages.addAll(db.chatDao().getAllMessages().map { it.toModel() })
            studySessions.addAll(db.studySessionDao().getAll().map { it.toModel() })
        }

        com.core.voidapp.data.guardian.GuardianRepository.init(context)
    }

    fun newId(): String = UUID.randomUUID().toString()

    /**
     * Pings VOID Smart Widgets (the 3 home-screen widgets) to recompute
     * and redraw from current state. Cheap no-op if no widget of that kind
     * is actually placed, and a no-op entirely before init() has run.
     * Never called for chat/AI mutations — only for data the widgets read.
     */
    private fun notifyWidgets() {
        appContext?.let { com.core.voidapp.widgets.glance.WidgetUpdateScheduler.refreshAll(it) }
    }

    // ---------------------------------------------------------------
    // Study Sessions — the real "clock is running" state Guardian and
    // EXECUTE both key off. See StudySession in Models.kt.
    // ---------------------------------------------------------------

    fun activeSession(): StudySession? = studySessions.find { it.status == StudySessionStatus.ACTIVE }

    fun startSession(
        source: StudySessionSource,
        subjectId: String?,
        label: String,
        plannedMinutes: Int,
        circlePlanId: String? = null,
        temporaryTaskId: String? = null,
        unitId: String? = null
    ): StudySession {
        val session = StudySession(
            id = newId(), source = source, subjectId = subjectId, circlePlanId = circlePlanId,
            temporaryTaskId = temporaryTaskId, unitId = unitId, label = label,
            plannedMinutes = plannedMinutes, startedAt = java.time.LocalDateTime.now()
        )
        studySessions.add(session)
        ioScope.launch { database?.studySessionDao()?.upsert(session.toEntity()) }
        appContext?.let { com.core.voidapp.data.guardian.GuardianEngine.onSessionStarted(it, session) }
        return session
    }

    fun completeSession(sessionId: String) {
        val idx = studySessions.indexOfFirst { it.id == sessionId }
        if (idx == -1) return
        val updated = studySessions[idx].copy(endedAt = java.time.LocalDateTime.now(), status = StudySessionStatus.COMPLETED)
        studySessions[idx] = updated
        ioScope.launch { database?.studySessionDao()?.upsert(updated.toEntity()) }
        appContext?.let { com.core.voidapp.data.guardian.GuardianEngine.onSessionCompleted(it, updated) }
        notifyWidgets()
    }

    fun abandonSession(sessionId: String) {
        val idx = studySessions.indexOfFirst { it.id == sessionId }
        if (idx == -1) return
        val updated = studySessions[idx].copy(endedAt = java.time.LocalDateTime.now(), status = StudySessionStatus.ABANDONED)
        studySessions[idx] = updated
        ioScope.launch { database?.studySessionDao()?.upsert(updated.toEntity()) }
        appContext?.let { com.core.voidapp.data.guardian.GuardianEngine.onSessionAbandoned(it, updated) }
        notifyWidgets()
    }

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

    /** Edits name/grade/code on an existing subject. Any parameter left null keeps its current value. */
    fun updateSubject(subjectId: String, name: String? = null, grade: Int? = null, code: String? = null): Subject? {
        val idx = subjects.indexOfFirst { it.id == subjectId }
        if (idx == -1) return null
        val current = subjects[idx]
        val updated = current.copy(
            name = name ?: current.name,
            grade = grade ?: current.grade,
            code = code ?: current.code
        )
        subjects[idx] = updated
        persistSubject(updated)
        return updated
    }

    /**
     * Deletes a subject and everything that only makes sense attached to
     * it — its marks (assessment types), units, weekly class periods,
     * Circle Plan slots, and its sittings inside any exam (removing the
     * exam itself if that was its last remaining subject). Temporary
     * tasks that referenced it are left alone with a dangling subjectId —
     * subjectName() already renders that gracefully — since a homework
     * item shouldn't vanish just because the subject record was removed.
     */
    fun deleteSubject(subjectId: String) {
        val subject = subjects.find { it.id == subjectId } ?: return
        val assessmentTypeIds = subject.assessmentTypes.map { it.id }
        val unitIds = units.filter { it.subjectId == subjectId }.map { it.id }
        val periodIds = classPeriods.filter { it.subjectId == subjectId }.map { it.id }
        val planIds = circlePlans.filter { it.subjectId == subjectId }.map { it.id }
        val affectedExamSubjects = examSubjects.filter { it.subjectId == subjectId }
        val examSubjectIds = affectedExamSubjects.map { it.id }
        val touchedExamIds = affectedExamSubjects.map { it.examId }.distinct()

        subjects.removeAll { it.id == subjectId }
        units.removeAll { it.subjectId == subjectId }
        classPeriods.removeAll { it.subjectId == subjectId }
        circlePlans.removeAll { it.subjectId == subjectId }
        examSubjects.removeAll { it.subjectId == subjectId }
        val emptiedExamIds = touchedExamIds.filter { examId -> examSubjects.none { it.examId == examId } }
        exams.removeAll { it.id in emptiedExamIds }

        ioScope.launch {
            database?.subjectDao()?.deleteById(subjectId)
            assessmentTypeIds.forEach { database?.assessmentTypeDao()?.deleteById(it) }
            unitIds.forEach { database?.unitDao()?.deleteById(it) }
            periodIds.forEach { database?.classPeriodDao()?.deleteById(it) }
            planIds.forEach { database?.circlePlanDao()?.deleteById(it) }
            examSubjectIds.forEach { database?.examSubjectDao()?.deleteById(it) }
            emptiedExamIds.forEach { database?.examDao()?.deleteById(it) }
        }
        notifyWidgets()
    }

    /** Edits an existing unit. Any parameter left null keeps its current value. */
    fun updateUnit(
        unitId: String,
        unitNumber: Int? = null,
        name: String? = null,
        description: String? = null,
        estimatedStudyMinutes: Int? = null
    ): AcademicUnit? {
        val idx = units.indexOfFirst { it.id == unitId }
        if (idx == -1) return null
        val current = units[idx]
        val updated = current.copy(
            unitNumber = unitNumber ?: current.unitNumber,
            name = name ?: current.name,
            description = description ?: current.description,
            estimatedStudyMinutes = estimatedStudyMinutes ?: current.estimatedStudyMinutes
        )
        units[idx] = updated
        ioScope.launch { database?.unitDao()?.upsert(updated.toEntity()) }
        return updated
    }

    /** Edits an existing mark's definition (label/weight/max score). The score itself is set separately via recordScore. */
    fun updateAssessmentType(
        subjectId: String,
        assessmentTypeId: String,
        label: String? = null,
        weightPercent: Double? = null,
        maxScore: Double? = null
    ): AssessmentType? {
        val subject = subjects.find { it.id == subjectId } ?: return null
        val idx = subject.assessmentTypes.indexOfFirst { it.id == assessmentTypeId }
        if (idx == -1) return null
        val current = subject.assessmentTypes[idx]
        val updated = current.copy(
            label = label ?: current.label,
            weightPercent = weightPercent ?: current.weightPercent,
            maxScore = maxScore ?: current.maxScore
        )
        subject.assessmentTypes[idx] = updated
        ioScope.launch { database?.assessmentTypeDao()?.upsert(updated.toEntity(subjectId)) }
        return updated
    }

    /** Deletes a mark definition entirely — including whatever score was recorded on it. */
    fun deleteAssessmentType(subjectId: String, assessmentTypeId: String) {
        val subject = subjects.find { it.id == subjectId } ?: return
        subject.assessmentTypes.removeAll { it.id == assessmentTypeId }
        ioScope.launch { database?.assessmentTypeDao()?.deleteById(assessmentTypeId) }
    }

    /** Clears a recorded score back to ungraded without deleting the mark definition itself. */
    fun clearScore(subjectId: String, assessmentTypeId: String) {
        val subject = subjects.find { it.id == subjectId } ?: return
        val type = subject.assessmentTypes.find { it.id == assessmentTypeId } ?: return
        type.entry = null
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
        notifyWidgets()
    }

    fun deleteClassPeriod(periodId: String) {
        classPeriods.removeAll { it.id == periodId }
        ioScope.launch { database?.classPeriodDao()?.deleteById(periodId) }
        notifyWidgets()
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
        notifyWidgets()
        return plan
    }

    fun deleteCirclePlan(planId: String) {
        circlePlans.removeAll { it.id == planId }
        ioScope.launch { database?.circlePlanDao()?.deleteById(planId) }
        notifyWidgets()
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
        notifyWidgets()
    }

    fun circlePlansFor(day: DayOfWeekVoid): List<CirclePlan> =
        circlePlans.filter { it.day == day }

    /**
     * Registers a new exam sitting, creating a fresh Exam (type + period +
     * grades) and its first ExamSubject in one step. To add more subjects
     * under the SAME exam (e.g. Physics also sitting the same Final),
     * call addExamSubject with the returned Exam's id instead of calling
     * this again — that's what keeps multiple subjects grouped into one
     * exam rather than spawning a separate exam per subject.
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
        notes: String = "",
        startDate: java.time.LocalDate? = null,
        endDate: java.time.LocalDate? = null
    ): ExamSubject {
        val exam = Exam(id = newId(), examType = examType, notes = notes, startDate = startDate, endDate = endDate, grades = grades)
        exams.add(exam)
        ioScope.launch { database?.examDao()?.upsert(exam.toEntity()) }
        return addExamSubject(exam.id, subjectId, date, time, session, location, unitIds)
    }

    /** Adds another subject's sitting under an existing Exam — what groups "Math Monday, Physics Tuesday" into one Final Exam card instead of two separate exams. */
    fun addExamSubject(
        examId: String,
        subjectId: String,
        date: java.time.LocalDate,
        time: java.time.LocalTime?,
        session: ExamSession?,
        location: String = "",
        unitIds: List<String> = emptyList()
    ): ExamSubject {
        val examSubject = ExamSubject(
            id = newId(), examId = examId, subjectId = subjectId, date = date,
            time = time, session = session, location = location, unitIds = unitIds
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

    /** Removes one subject's sitting from an exam; if that was the last subject in it, the now-empty exam is removed too. */
    fun deleteExamSubject(examSubjectId: String) {
        val target = examSubjects.find { it.id == examSubjectId } ?: return
        examSubjects.removeAll { it.id == examSubjectId }
        ioScope.launch { database?.examSubjectDao()?.deleteById(examSubjectId) }
        if (examSubjects.none { it.examId == target.examId }) {
            deleteExam(target.examId)
        }
    }

    fun examFor(examSubject: ExamSubject): Exam? = exams.find { it.id == examSubject.examId }

    fun upcomingExamSubjects(): List<ExamSubject> =
        examSubjects.filter { it.status() != ExamSittingStatus.COMPLETED }.sortedBy { it.date }

    fun nearestExamSubject(): ExamSubject? = upcomingExamSubjects().firstOrNull()

    /** Every Exam with at least one sitting still ahead, grouped (not flattened per-subject) — what the Home "Next Exam" card and the Exam Schedule list iterate over. */
    fun upcomingExams(): List<Exam> =
        exams.filter { exam -> !exam.isFullyCompleted() && exam.subjectsSorted().isNotEmpty() }
            .sortedBy { exam -> exam.nearestUpcomingSubject()?.date ?: java.time.LocalDate.MAX }

    fun nearestExam(): Exam? = upcomingExams().firstOrNull()

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
        notes: String = "",
        startTime: java.time.LocalTime? = null,
        endTime: java.time.LocalTime? = null
    ): TemporaryTask {
        val task = TemporaryTask(
            id = newId(),
            title = title,
            type = type,
            subjectId = subjectId,
            startDate = startDate,
            deadline = deadline,
            startTime = startTime,
            endTime = endTime,
            requiredMinutes = requiredMinutes,
            priority = priority,
            unitIds = unitIds,
            notes = notes
        )
        temporaryTasks.add(task)
        ioScope.launch { database?.temporaryTaskDao()?.upsert(task.toEntity()) }
        notifyWidgets()
        return task
    }

    /** Edits the optional clock-time window on an existing task (e.g. attaching "18:00 -> 19:30" after creation). Pass null to clear either side. */
    fun updateTaskSchedule(taskId: String, startTime: java.time.LocalTime?, endTime: java.time.LocalTime?) {
        val idx = temporaryTasks.indexOfFirst { it.id == taskId }
        if (idx == -1) return
        val updated = temporaryTasks[idx].copy(startTime = startTime, endTime = endTime)
        temporaryTasks[idx] = updated
        ioScope.launch { database?.temporaryTaskDao()?.upsert(updated.toEntity()) }
        notifyWidgets()
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
        notifyWidgets()
    }

    fun setTaskStatus(taskId: String, status: PlanTaskStatus) {
        val idx = temporaryTasks.indexOfFirst { it.id == taskId }
        if (idx == -1) return
        val updated = temporaryTasks[idx].copy(status = status)
        temporaryTasks[idx] = updated
        ioScope.launch { database?.temporaryTaskDao()?.upsert(updated.toEntity()) }
        notifyWidgets()
    }

    fun deleteTemporaryTask(taskId: String) {
        temporaryTasks.removeAll { it.id == taskId }
        ioScope.launch { database?.temporaryTaskDao()?.deleteById(taskId) }
        notifyWidgets()
    }

    fun activeTemporaryTasks(): List<TemporaryTask> =
        temporaryTasks.filter { it.status != PlanTaskStatus.COMPLETED && it.status != PlanTaskStatus.CANCELLED }
            .sortedBy { it.deadline }

    fun temporaryTasksForDay(day: java.time.LocalDate): List<TemporaryTask> =
        temporaryTasks.filter { it.deadline == day }

    private fun persistSubject(subject: Subject) {
        ioScope.launch { database?.subjectDao()?.upsert(subject.toEntity()) }
    }

    /** All AI Chat conversations, most recently created first — the shape the sidebar reads. */
    fun allConversations(): List<ChatConversation> =
        chatConversations.sortedByDescending { it.createdAt }

    /** Starts a brand-new, empty conversation and makes it current. */
    fun createConversation(title: String = "New chat"): ChatConversation {
        val conversation = ChatConversation(id = newId(), title = title)
        chatConversations.add(conversation)
        ioScope.launch { database?.chatDao()?.upsertConversation(conversation.toEntity()) }
        return conversation
    }

    fun renameConversation(conversationId: String, title: String) {
        val idx = chatConversations.indexOfFirst { it.id == conversationId }
        if (idx == -1) return
        val updated = chatConversations[idx].copy(title = title)
        chatConversations[idx] = updated
        ioScope.launch { database?.chatDao()?.upsertConversation(updated.toEntity()) }
    }

    /** Deletes a whole conversation and every message in it — unlike clearChatHistory, this removes the thread itself. */
    fun deleteConversation(conversationId: String) {
        chatConversations.removeAll { it.id == conversationId }
        chatMessages.removeAll { it.conversationId == conversationId }
        ioScope.launch {
            database?.chatDao()?.deleteMessagesForConversation(conversationId)
            database?.chatDao()?.deleteConversation(conversationId)
        }
    }

    fun messagesFor(conversationId: String): List<ChatMessage> =
        chatMessages.filter { it.conversationId == conversationId }.sortedBy { it.timestamp }

    /**
     * Appends a message and persists it. Also auto-titles the conversation
     * from the first user message it ever receives (still "New chat" up to
     * that point) — same behavior users already know from other AI chat
     * apps, so a growing conversation list stays scannable without the
     * user ever having to name anything themselves.
     */
    fun addChatMessage(conversationId: String, role: ChatRole, content: String): ChatMessage {
        val message = ChatMessage(id = newId(), conversationId = conversationId, role = role, content = content)
        chatMessages.add(message)
        ioScope.launch { database?.chatDao()?.upsertMessage(message.toEntity()) }

        if (role == ChatRole.USER) {
            val idx = chatConversations.indexOfFirst { it.id == conversationId }
            if (idx != -1) {
                val convo = chatConversations[idx]
                val isFirstUserMessage = chatMessages.none {
                    it.conversationId == conversationId && it.role == ChatRole.USER && it.id != message.id
                }
                if (isFirstUserMessage && (convo.title.isBlank() || convo.title == "New chat" || convo.title == "VOID AI")) {
                    val trimmed = content.trim()
                    val autoTitle = if (trimmed.length > 40) "${trimmed.take(40)}\u2026" else trimmed
                    val updated = convo.copy(title = autoTitle.ifBlank { "New chat" })
                    chatConversations[idx] = updated
                    ioScope.launch { database?.chatDao()?.upsertConversation(updated.toEntity()) }
                }
            }
        }

        return message
    }

    /** Clears every message in a conversation but keeps the thread itself — use deleteConversation to remove the thread too. */
    fun clearChatHistory(conversationId: String) {
        chatMessages.removeAll { it.conversationId == conversationId }
        ioScope.launch { database?.chatDao()?.deleteMessagesForConversation(conversationId) }
    }
}
