package com.edutrack.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AcademicJourneyEntity::class,
        SubjectEntity::class,
        ChapterEntity::class,
        TopicEntity::class,
        StudyTaskEntity::class,
        RevisionScheduleEntity::class,
        ExamCountdownEntity::class,
        AchievementEntity::class,
        StudySessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(EduTrackConverters::class)
abstract class EduTrackDatabase : RoomDatabase() {
    abstract fun eduTrackDao(): EduTrackDao

    companion object {
        @Volatile
        private var INSTANCE: EduTrackDatabase? = null

        fun getInstance(context: Context): EduTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EduTrackDatabase::class.java,
                    "edutrack_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
