package com.edutrack.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EduTrackDao {

    // Academic Journeys
    @Query("SELECT * FROM academic_journeys ORDER BY createdAtMillis ASC")
    fun observeAllJourneys(): Flow<List<AcademicJourneyEntity>>

    @Query("SELECT * FROM academic_journeys WHERE id = :journeyId")
    fun observeJourneyById(journeyId: Long): Flow<AcademicJourneyEntity?>

    @Query("SELECT * FROM academic_journeys WHERE id = :journeyId")
    suspend fun getJourneyById(journeyId: Long): AcademicJourneyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourney(journey: AcademicJourneyEntity): Long

    @Update
    suspend fun updateJourney(journey: AcademicJourneyEntity)

    @Delete
    suspend fun deleteJourney(journey: AcademicJourneyEntity)

    // Subjects
    @Query("SELECT * FROM subjects WHERE journeyId = :journeyId AND isArchived = 0 ORDER BY orderIndex ASC")
    fun observeSubjectsForJourney(journeyId: Long): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    // Chapters
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId AND isArchived = 0 ORDER BY orderIndex ASC")
    fun observeChaptersForSubject(subjectId: Long): Flow<List<ChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    // Topics
    @Query("SELECT * FROM topics WHERE chapterId = :chapterId AND isArchived = 0 ORDER BY orderIndex ASC")
    fun observeTopicsForChapter(chapterId: Long): Flow<List<TopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Query(
        """
        SELECT COUNT(*) FROM topics t
        INNER JOIN chapters c ON t.chapterId = c.id
        INNER JOIN subjects s ON c.subjectId = s.id
        WHERE s.journeyId = :journeyId AND s.isArchived = 0 AND c.isArchived = 0 AND t.isArchived = 0
        """
    )
    fun observeTotalTopicCount(journeyId: Long): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM topics t
        INNER JOIN chapters c ON t.chapterId = c.id
        INNER JOIN subjects s ON c.subjectId = s.id
        WHERE s.journeyId = :journeyId AND s.isArchived = 0 AND c.isArchived = 0 AND t.isArchived = 0
        AND t.status = 'COMPLETED'
        """
    )
    fun observeCompletedTopicCount(journeyId: Long): Flow<Int>

    // Exam Countdowns
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamCountdown(countdown: ExamCountdownEntity): Long

    @Query("SELECT * FROM exam_countdowns WHERE journeyId = :journeyId ORDER BY examEpochDay ASC")
    fun observeExamCountdowns(journeyId: Long): Flow<List<ExamCountdownEntity>>
}
