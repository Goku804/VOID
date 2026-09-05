package com.core.voidapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SubjectEntity::class,
        AssessmentTypeEntity::class,
        AcademicUnitEntity::class,
        ClassPeriodEntity::class,
        NightAvailabilityEntity::class,
        ExamEntity::class,
        ExamSubjectEntity::class,
        CirclePlanEntity::class,
        TemporaryTaskEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class VoidDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun assessmentTypeDao(): AssessmentTypeDao
    abstract fun unitDao(): AcademicUnitDao
    abstract fun classPeriodDao(): ClassPeriodDao
    abstract fun nightAvailabilityDao(): NightAvailabilityDao
    abstract fun examDao(): ExamDao
    abstract fun examSubjectDao(): ExamSubjectDao
    abstract fun circlePlanDao(): CirclePlanDao
    abstract fun temporaryTaskDao(): TemporaryTaskDao

    companion object {
        @Volatile private var INSTANCE: VoidDatabase? = null

        fun getInstance(context: Context): VoidDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VoidDatabase::class.java,
                    "void.db"
                )
                    // Destructive migration is fine while the app is in active
                    // development with disposable data. Switch to real
                    // Migration objects before this holds real semester data
                    // — see VOID_DATABASE_SCHEMA.md §7.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
