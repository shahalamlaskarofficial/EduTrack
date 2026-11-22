package com.edutrack.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class AcademicCategory { SCHOOL, COMPETITIVE }

enum class TopicStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    REVISION_REQUIRED
}

enum class ThemePreference { DARK, LIGHT, SYSTEM }

@Entity(tableName = "academic_journeys")
data class AcademicJourneyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: AcademicCategory,
    val boardOrExam: String,
    val classOrLevel: String,
    val stream: String = "",
    val academicSession: String,
    val targetExamDateEpochDay: Long? = null,
    val syllabusDatasetId: String,
    val syllabusDatasetVersion: String = "1.0.0",
    val syllabusSourceRef: String = "Pending Phase 2 Verification",
    val syllabusVerifiedDate: String = "Pending",
    val isDatasetComplete: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = AcademicJourneyEntity::class,
            parentColumns = ["id"],
            childColumns = ["journeyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("journeyId")]
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journeyId: Long,
    val officialKey: String?,
    val name: String,
    val orderIndex: Int = 0,
    val isCustom: Boolean = false,
    val isArchived: Boolean = false
)

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val officialKey: String?,
    val title: String,
    val unitName: String = "",
    val orderIndex: Int = 0,
    val isCustom: Boolean = false,
    val isArchived: Boolean = false
)

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chapterId")]
)
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Long,
    val parentTopicId: Long? = null,
    val officialKey: String?,
    val title: String,
    val status: TopicStatus = TopicStatus.NOT_STARTED,
    val orderIndex: Int = 0,
    val isCustom: Boolean = false,
    val isArchived: Boolean = false,
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val completedAtMillis: Long? = null
)

@Entity(
    tableName = "study_tasks",
    foreignKeys = [
        ForeignKey(
            entity = AcademicJourneyEntity::class,
            parentColumns = ["id"],
            childColumns = ["journeyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("journeyId"), Index("linkedTopicId")]
)
data class StudyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journeyId: Long,
    val title: String,
    val notes: String = "",
    val linkedTopicId: Long? = null,
    val targetEpochDay: Long,
    val isWeeklyGoal: Boolean = false,
    val isCompleted: Boolean = false,
    val reminderTimeMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "revision_schedules",
    foreignKeys = [
        ForeignKey(
            entity = AcademicJourneyEntity::class,
            parentColumns = ["id"],
            childColumns = ["journeyId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("journeyId"), Index("topicId")]
)
data class RevisionScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journeyId: Long,
    val topicId: Long,
    val intervalDays: Int,
    val dueEpochDay: Long,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null
)

@Entity(
    tableName = "exam_countdowns",
    foreignKeys = [
        ForeignKey(
            entity = AcademicJourneyEntity::class,
            parentColumns = ["id"],
            childColumns = ["journeyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("journeyId")]
)
data class ExamCountdownEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journeyId: Long,
    val examName: String,
    val examEpochDay: Long
)

@Entity(
    tableName = "achievements",
    foreignKeys = [
        ForeignKey(
            entity = AcademicJourneyEntity::class,
            parentColumns = ["id"],
            childColumns = ["journeyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("journeyId")]
)
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journeyId: Long,
    val code: String,
    val title: String,
    val description: String,
    val unlockedAtMillis: Long? = null
)

@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = AcademicJourneyEntity::class,
            parentColumns = ["id"],
            childColumns = ["journeyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("journeyId")]
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journeyId: Long,
    val epochDay: Long,
    val topicsCompletedCount: Int = 0,
    val revisionsCompletedCount: Int = 0
)

class EduTrackConverters {
    @TypeConverter
    fun fromCategory(value: AcademicCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): AcademicCategory = AcademicCategory.valueOf(value)

    @TypeConverter
    fun fromTopicStatus(value: TopicStatus): String = value.name

    @TypeConverter
    fun toTopicStatus(value: String): TopicStatus = TopicStatus.valueOf(value)
}
