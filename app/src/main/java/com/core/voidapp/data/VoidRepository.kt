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
    val classPeriods = mutableStateListOf<ClassPeriod>()
    val exams = mutableStateListOf<Exam>()
    val temporaryTasks = mutableStateListOf<TemporaryTask>()

    fun newId(): String = UUID.randomUUID().toString()

    fun addSubject(name: String, grade: Int): Subject {
        val subject = Subject(id = newId(), name = name, grade = grade)
        subjects.add(subject)
        return subject
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
        subjectId: String
    ) {
        classPeriods.add(
            ClassPeriod(
                id = newId(),
                day = day,
                periodNumber = periodNumber,
                subjectId = subjectId
            )
        )
    }

    fun scheduleFor(day: DayOfWeekVoid): List<ClassPeriod> =
        classPeriods.filter { it.day == day }.sortedBy { it.periodNumber }

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
        subjectId: String?,
        dueDate: java.time.LocalDate,
        highPriority: Boolean = false
    ) {
        temporaryTasks.add(
            TemporaryTask(
                id = newId(),
                title = title,
                subjectId = subjectId,
                dueDate = dueDate,
                isHighPriority = highPriority
            )
        )
    }

    fun toggleTaskDone(taskId: String) {
        val idx = temporaryTasks.indexOfFirst { it.id == taskId }
        if (idx == -1) return
        val task = temporaryTasks[idx]
        temporaryTasks[idx] = task.copy(isCompleted = !task.isCompleted)
    }
}
