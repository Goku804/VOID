package com.core.voidapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects")
    suspend fun getAll(): List<SubjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface AssessmentTypeDao {
    @Query("SELECT * FROM assessment_types")
    suspend fun getAll(): List<AssessmentTypeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AssessmentTypeEntity)

    @Query("DELETE FROM assessment_types WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface AcademicUnitDao {
    @Query("SELECT * FROM units")
    suspend fun getAll(): List<AcademicUnitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AcademicUnitEntity)

    @Query("DELETE FROM units WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ClassPeriodDao {
    @Query("SELECT * FROM class_periods")
    suspend fun getAll(): List<ClassPeriodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ClassPeriodEntity)

    @Query("DELETE FROM class_periods WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface NightAvailabilityDao {
    @Query("SELECT * FROM night_availability")
    suspend fun getAll(): List<NightAvailabilityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NightAvailabilityEntity)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams")
    suspend fun getAll(): List<ExamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExamEntity)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ExamSubjectDao {
    @Query("SELECT * FROM exam_subjects")
    suspend fun getAll(): List<ExamSubjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExamSubjectEntity)

    @Query("DELETE FROM exam_subjects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM exam_subjects WHERE examId = :examId")
    suspend fun deleteByExamId(examId: String)
}

@Dao
interface CirclePlanDao {
    @Query("SELECT * FROM circle_plans")
    suspend fun getAll(): List<CirclePlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CirclePlanEntity)

    @Query("DELETE FROM circle_plans WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface TemporaryTaskDao {
    @Query("SELECT * FROM temporary_tasks")
    suspend fun getAll(): List<TemporaryTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TemporaryTaskEntity)

    @Query("DELETE FROM temporary_tasks WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_conversations")
    suspend fun getAllConversations(): List<ChatConversationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(entity: ChatConversationEntity)

    @Query("SELECT * FROM chat_messages")
    suspend fun getAllMessages(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(entity: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Query("DELETE FROM chat_conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions")
    suspend fun getAll(): List<StudySessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StudySessionEntity)

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface GuardianCommitmentDao {
    @Query("SELECT * FROM guardian_commitments")
    suspend fun getAll(): List<GuardianCommitmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GuardianCommitmentEntity)

    @Query("DELETE FROM guardian_commitments WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface GuardianSettingsDao {
    @Query("SELECT * FROM guardian_settings WHERE id = 0")
    suspend fun get(): GuardianSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GuardianSettingsEntity)
}

@Dao
interface DailyReportDao {
    @Query("SELECT * FROM daily_reports")
    suspend fun getAllReports(): List<DailyReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReport(entity: DailyReportEntity)

    @Query("SELECT * FROM daily_class_reports")
    suspend fun getAllClassReports(): List<DailyClassReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClassReport(entity: DailyClassReportEntity)

    @Query("SELECT * FROM daily_study_reports")
    suspend fun getAllStudyReports(): List<DailyStudyReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudyReport(entity: DailyStudyReportEntity)

    @Query("SELECT * FROM daily_report_assignments")
    suspend fun getAllAssignments(): List<DailyReportAssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignment(entity: DailyReportAssignmentEntity)

    @Query("SELECT * FROM daily_report_test_preps")
    suspend fun getAllTestPreps(): List<DailyReportTestPrepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTestPrep(entity: DailyReportTestPrepEntity)
}
