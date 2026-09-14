package com.core.voidapp.data.report

import java.time.LocalDate
import java.time.LocalDateTime

// ---------------------------------------------------------------------
// Shared response scales — used across school-class and study reports so
// "Medium difficulty" or "Mostly focused" means the same thing everywhere.
// ---------------------------------------------------------------------

enum class YesNoPartial { YES, PARTIAL, NO }
enum class DifficultyLevel { EASY, MEDIUM, HARD }
enum class AttentionLevel { FULLY, MOSTLY, PARTIALLY, NOT_REALLY }
enum class StudyCompletionResult { COMPLETED, PARTIAL, MISSED }

enum class IncompleteReason {
    NOT_ENOUGH_TIME, TOO_DIFFICULT, DISTRACTED, TIRED, SCHOOL_WORK, UNEXPECTED_TASK, OTHER
}

enum class DailyReportStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }

/** One calendar day's report — the container everything below hangs off. At most one per date. */
data class DailyReport(
    val id: String,
    val date: LocalDate,
    val status: DailyReportStatus = DailyReportStatus.NOT_STARTED,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime? = null
)

/**
 * Reflection on one of today's actual registered ClassPeriod occurrences.
 * classPeriodId points at the real timetable entry — this never stores
 * subject/time/day again, only what the ClassPeriod record can't know:
 * how the day actually went.
 */
data class DailyClassReport(
    val id: String,
    val dailyReportId: String,
    val classPeriodId: String,
    val attendance: YesNoPartial,
    val understanding: YesNoPartial,
    val difficulty: DifficultyLevel,
    val attention: AttentionLevel,
    val tookNotes: YesNoPartial,
    val importantContent: String = "",
    val difficultyReason: String = ""
)

/**
 * Reflection on a study block for the day. Points at a real StudySession
 * when one actually ran (studySessionId) — subject/planned-duration/
 * actual-duration/status all live on StudySession already and are never
 * duplicated here. subjectId/label are kept only as a fallback for
 * planned-but-never-started sessions (status = MISSED, no StudySession
 * ever existed to point at).
 */
data class DailyStudyReport(
    val id: String,
    val dailyReportId: String,
    val studySessionId: String?,
    val subjectId: String?,
    val label: String,
    val completion: StudyCompletionResult,
    val difficulty: DifficultyLevel,
    val understanding: YesNoPartial,
    val attention: AttentionLevel,
    val tookNotes: YesNoPartial,
    val incompleteReason: IncompleteReason? = null,
    val incompleteNotes: String = ""
)

/**
 * Links a Daily Report to the REAL TemporaryTask created from a teacher's
 * assignment — per spec #3/#25, the assignment itself becomes actual
 * planning data (a TemporaryTask), not a report-only record.
 */
data class DailyReportAssignment(
    val id: String,
    val dailyReportId: String,
    val temporaryTaskId: String
)

/**
 * A teacher's test/exam instruction. If subject + an expected date were
 * both given, examSubjectId points at the real Exam/ExamSubject that got
 * registered from it; otherwise this is just the raw note, since VOID
 * must not invent an exam from insufficient information (spec #3).
 */
data class DailyReportTestPrep(
    val id: String,
    val dailyReportId: String,
    val subjectId: String?,
    val examSubjectId: String? = null,
    val rawNote: String = ""
)
