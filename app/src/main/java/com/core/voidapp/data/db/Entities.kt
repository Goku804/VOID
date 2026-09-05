package com.core.voidapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entities. Deliberately store dates as epoch-day Long, times as
 * "HH:MM" String, and enums as their .name String — keeps every field a
 * primitive/String so no @TypeConverters class is needed anywhere.
 * Mapping to/from the app's real data models lives in Mappers.kt.
 */

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val grade: Int,
    val code: String
)

/** One graded component of a subject (Test/Mid/Final/...), score nullable until recorded. */
@Entity(tableName = "assessment_types")
data class AssessmentTypeEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val kind: String,
    val label: String,
    val weightPercent: Double,
    val maxScore: Double,
    val score: Double?,
    val dateRecorded: Long?
)

@Entity(tableName = "units")
data class AcademicUnitEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val unitNumber: Int,
    val name: String,
    val description: String,
    val estimatedStudyMinutes: Int
)

@Entity(tableName = "class_periods")
data class ClassPeriodEntity(
    @PrimaryKey val id: String,
    val day: String,
    val periodNumber: Int,
    val subjectId: String,
    val classType: String,
    val startTime: String?,
    val endTime: String?,
    val location: String?
)

/** One row per day of the week — day itself is the primary key, so save = upsert. */
@Entity(tableName = "night_availability")
data class NightAvailabilityEntity(
    @PrimaryKey val day: String,
    val available: Boolean,
    val startTime: String?,
    val endTime: String?
)

/** The exam event itself — thin, since date/time/subject specifics live on ExamSubjectEntity. */
@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String,
    val examType: String,
    val notes: String
)

/** One subject's sitting within an exam — its own date/time/session/units/grades. */
@Entity(tableName = "exam_subjects")
data class ExamSubjectEntity(
    @PrimaryKey val id: String,
    val examId: String,
    val subjectId: String,
    val date: Long,
    val time: String?,
    val session: String?,
    val location: String,
    val unitIds: String,
    val grades: String
)

@Entity(tableName = "circle_plans")
data class CirclePlanEntity(
    @PrimaryKey val id: String,
    val day: String,
    val subjectId: String,
    val durationMinutes: Int,
    val window: String,
    val strategy: String,
    val currentUnitIndex: Int,
    val fixedUnitId: String?,
    val priority: String
)

/** unitIds stored comma-joined — simple over a junction table for v1.0.0, see schema doc §6.3. */
@Entity(tableName = "temporary_tasks")
data class TemporaryTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val subjectId: String?,
    val startDate: Long?,
    val deadline: Long,
    val requiredMinutes: Int,
    val completedMinutes: Int,
    val priority: String,
    val unitIds: String,
    val notes: String,
    val status: String
)
