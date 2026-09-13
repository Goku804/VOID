package com.core.voidapp.data.db

import com.core.voidapp.data.AcademicUnit
import com.core.voidapp.data.AssessmentKind
import com.core.voidapp.data.AssessmentType
import com.core.voidapp.data.ChatConversation
import com.core.voidapp.data.ChatMessage
import com.core.voidapp.data.ChatRole
import com.core.voidapp.data.CirclePlan
import com.core.voidapp.data.ClassPeriod
import com.core.voidapp.data.ClassType
import com.core.voidapp.data.ContentStrategy
import com.core.voidapp.data.DayOfWeekVoid
import com.core.voidapp.data.Exam
import com.core.voidapp.data.ExamSession
import com.core.voidapp.data.ExamSubject
import com.core.voidapp.data.ExamType
import com.core.voidapp.data.MarkEntry
import com.core.voidapp.data.NightAvailability
import com.core.voidapp.data.PlanPriority
import com.core.voidapp.data.PlanTaskStatus
import com.core.voidapp.data.PreferredWindow
import com.core.voidapp.data.StudySession
import com.core.voidapp.data.StudySessionSource
import com.core.voidapp.data.StudySessionStatus
import com.core.voidapp.data.Subject
import com.core.voidapp.data.TemporaryPlanType
import com.core.voidapp.data.TemporaryTask
import java.time.LocalDate
import java.time.LocalTime

// ---------------------------------------------------------------------
// Subject (assessmentTypes handled separately — see AssessmentType below)
// ---------------------------------------------------------------------

fun Subject.toEntity() = SubjectEntity(id = id, name = name, grade = grade, code = code)

fun SubjectEntity.toModel() = Subject(id = id, name = name, grade = grade, code = code)

fun AssessmentType.toEntity(subjectId: String) = AssessmentTypeEntity(
    id = id,
    subjectId = subjectId,
    kind = kind.name,
    label = label,
    weightPercent = weightPercent,
    maxScore = maxScore,
    score = entry?.score,
    dateRecorded = entry?.dateRecorded?.toEpochDay()
)

fun AssessmentTypeEntity.toModel() = AssessmentType(
    id = id,
    kind = AssessmentKind.valueOf(kind),
    label = label,
    weightPercent = weightPercent,
    maxScore = maxScore,
    entry = if (score != null) MarkEntry(score = score, dateRecorded = dateRecorded?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()) else null
)

// ---------------------------------------------------------------------
// AcademicUnit
// ---------------------------------------------------------------------

fun AcademicUnit.toEntity() = AcademicUnitEntity(
    id = id, subjectId = subjectId, unitNumber = unitNumber,
    name = name, description = description, estimatedStudyMinutes = estimatedStudyMinutes
)

fun AcademicUnitEntity.toModel() = AcademicUnit(
    id = id, subjectId = subjectId, unitNumber = unitNumber,
    name = name, description = description, estimatedStudyMinutes = estimatedStudyMinutes
)

// ---------------------------------------------------------------------
// ClassPeriod
// ---------------------------------------------------------------------

fun ClassPeriod.toEntity() = ClassPeriodEntity(
    id = id, day = day.name, periodNumber = periodNumber, subjectId = subjectId,
    classType = classType.name, startTime = startTime?.toString(), endTime = endTime?.toString(), location = location
)

fun ClassPeriodEntity.toModel() = ClassPeriod(
    id = id, day = DayOfWeekVoid.valueOf(day), periodNumber = periodNumber, subjectId = subjectId,
    classType = ClassType.valueOf(classType),
    startTime = startTime?.let { LocalTime.parse(it) },
    endTime = endTime?.let { LocalTime.parse(it) },
    location = location
)

// ---------------------------------------------------------------------
// NightAvailability
// ---------------------------------------------------------------------

fun NightAvailability.toEntity() = NightAvailabilityEntity(
    day = day.name, available = available,
    startTime = start?.toString(), endTime = end?.toString()
)

fun NightAvailabilityEntity.toModel() = NightAvailability(
    day = DayOfWeekVoid.valueOf(day), available = available,
    start = startTime?.let { LocalTime.parse(it) },
    end = endTime?.let { LocalTime.parse(it) }
)

// ---------------------------------------------------------------------
// Exam
// ---------------------------------------------------------------------

fun Exam.toEntity() = ExamEntity(
    id = id, examType = examType.name, notes = notes,
    startDate = startDate?.toEpochDay(), endDate = endDate?.toEpochDay(),
    grades = grades.joinToString(",")
)

fun ExamEntity.toModel() = Exam(
    id = id, examType = ExamType.valueOf(examType), notes = notes,
    startDate = startDate?.let { LocalDate.ofEpochDay(it) },
    endDate = endDate?.let { LocalDate.ofEpochDay(it) },
    grades = if (grades.isBlank()) emptyList() else grades.split(",").map { it.toInt() }
)

fun ExamSubject.toEntity() = ExamSubjectEntity(
    id = id, examId = examId, subjectId = subjectId, date = date.toEpochDay(),
    time = time?.toString(), session = session?.name, location = location,
    unitIds = unitIds.joinToString(",")
)

fun ExamSubjectEntity.toModel() = ExamSubject(
    id = id, examId = examId, subjectId = subjectId, date = LocalDate.ofEpochDay(date),
    time = time?.let { LocalTime.parse(it) }, session = session?.let { ExamSession.valueOf(it) }, location = location,
    unitIds = if (unitIds.isBlank()) emptyList() else unitIds.split(",")
)

// ---------------------------------------------------------------------
// CirclePlan
// ---------------------------------------------------------------------

fun CirclePlan.toEntity() = CirclePlanEntity(
    id = id, day = day.name, subjectId = subjectId, durationMinutes = durationMinutes,
    window = window.name, strategy = strategy.name, currentUnitIndex = currentUnitIndex,
    fixedUnitId = fixedUnitId, priority = priority.name
)

fun CirclePlanEntity.toModel() = CirclePlan(
    id = id, day = DayOfWeekVoid.valueOf(day), subjectId = subjectId, durationMinutes = durationMinutes,
    window = PreferredWindow.valueOf(window), strategy = ContentStrategy.valueOf(strategy),
    currentUnitIndex = currentUnitIndex, fixedUnitId = fixedUnitId, priority = PlanPriority.valueOf(priority)
)

// ---------------------------------------------------------------------
// TemporaryTask
// ---------------------------------------------------------------------

fun TemporaryTask.toEntity() = TemporaryTaskEntity(
    id = id, title = title, type = type.name, subjectId = subjectId,
    startDate = startDate?.toEpochDay(), deadline = deadline.toEpochDay(),
    startTimeSec = startTime?.toSecondOfDay(), endTimeSec = endTime?.toSecondOfDay(),
    requiredMinutes = requiredMinutes, completedMinutes = completedMinutes,
    priority = priority.name, unitIds = unitIds.joinToString(","), notes = notes, status = status.name
)

fun TemporaryTaskEntity.toModel() = TemporaryTask(
    id = id, title = title, type = TemporaryPlanType.valueOf(type), subjectId = subjectId,
    startDate = startDate?.let { LocalDate.ofEpochDay(it) }, deadline = LocalDate.ofEpochDay(deadline),
    startTime = startTimeSec?.let { LocalTime.ofSecondOfDay(it.toLong()) },
    endTime = endTimeSec?.let { LocalTime.ofSecondOfDay(it.toLong()) },
    requiredMinutes = requiredMinutes, completedMinutes = completedMinutes,
    priority = PlanPriority.valueOf(priority),
    unitIds = if (unitIds.isBlank()) emptyList() else unitIds.split(","),
    notes = notes, status = PlanTaskStatus.valueOf(status)
)

// ---------------------------------------------------------------------
// Chat (conversations + messages)
// ---------------------------------------------------------------------

fun ChatConversation.toEntity() = ChatConversationEntity(id = id, title = title, createdAt = createdAt)

fun ChatConversationEntity.toModel() = ChatConversation(id = id, title = title, createdAt = createdAt)

fun ChatMessage.toEntity() = ChatMessageEntity(
    id = id, conversationId = conversationId, role = role.name, content = content, timestamp = timestamp
)

fun ChatMessageEntity.toModel() = ChatMessage(
    id = id, conversationId = conversationId, role = ChatRole.valueOf(role), content = content, timestamp = timestamp
)

fun StudySession.toEntity() = StudySessionEntity(
    id = id, source = source.name, subjectId = subjectId, circlePlanId = circlePlanId,
    temporaryTaskId = temporaryTaskId, unitId = unitId, label = label, plannedMinutes = plannedMinutes,
    startedAt = startedAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
    endedAt = endedAt?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
    status = status.name
)

fun StudySessionEntity.toModel() = StudySession(
    id = id, source = StudySessionSource.valueOf(source), subjectId = subjectId, circlePlanId = circlePlanId,
    temporaryTaskId = temporaryTaskId, unitId = unitId, label = label, plannedMinutes = plannedMinutes,
    startedAt = java.time.Instant.ofEpochMilli(startedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
    endedAt = endedAt?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() },
    status = StudySessionStatus.valueOf(status)
)

fun com.core.voidapp.data.guardian.GuardianCommitment.toEntity() = GuardianCommitmentEntity(
    id = id, durationName = duration.name,
    startedAt = startedAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
    endsAt = endsAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
    status = status.name
)

fun GuardianCommitmentEntity.toModel() = com.core.voidapp.data.guardian.GuardianCommitment(
    id = id, duration = com.core.voidapp.data.guardian.CommitmentDuration.valueOf(durationName),
    startedAt = java.time.Instant.ofEpochMilli(startedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
    endsAt = java.time.Instant.ofEpochMilli(endsAt).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
    status = com.core.voidapp.data.guardian.CommitmentStatus.valueOf(status)
)

fun com.core.voidapp.data.guardian.GuardianSettings.toEntity() = GuardianSettingsEntity(
    enforcementMode = enforcementMode.name, allowedPackages = allowedPackages.joinToString(",")
)

fun GuardianSettingsEntity.toModel() = com.core.voidapp.data.guardian.GuardianSettings(
    enforcementMode = com.core.voidapp.data.guardian.EnforcementMode.valueOf(enforcementMode),
    allowedPackages = if (allowedPackages.isBlank()) emptySet() else allowedPackages.split(",").toSet()
)
