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
    @Query("SELECT * FROM subjects WHERE journeyId = :journeyId AND isArchived = 0 ORDER BY orderIndex ASC, id ASC")
    fun observeSubjectsForJourney(journeyId: Long): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE journeyId = :journeyId ORDER BY orderIndex ASC, id ASC")
    suspend fun getSubjectsForJourneySync(journeyId: Long): List<SubjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    // Chapters
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId AND isArchived = 0 ORDER BY orderIndex ASC, id ASC")
    fun observeChaptersForSubject(subjectId: Long): Flow<List<ChapterEntity>>

    @Query(
        """
        SELECT c.* FROM chapters c
        INNER JOIN subjects s ON c.subjectId = s.id
        WHERE s.journeyId = :journeyId AND s.isArchived = 0 AND c.isArchived = 0
        ORDER BY c.orderIndex ASC, c.id ASC
        """
    )
    fun observeChaptersForJourney(journeyId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY orderIndex ASC, id ASC")
    suspend fun getChaptersForSubjectSync(subjectId: Long): List<ChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)

    // Topics
    @Query("SELECT * FROM topics WHERE chapterId = :chapterId AND isArchived = 0 ORDER BY orderIndex ASC, id ASC")
    fun observeTopicsForChapter(chapterId: Long): Flow<List<TopicEntity>>

    @Query(
        """
        SELECT t.* FROM topics t
        INNER JOIN chapters c ON t.chapterId = c.id
        INNER JOIN subjects s ON c.subjectId = s.id
        WHERE s.journeyId = :journeyId AND s.isArchived = 0 AND c.isArchived = 0 AND t.isArchived = 0
        ORDER BY t.orderIndex ASC, t.id ASC
        """
    )
    fun observeTopicsForJourney(journeyId: Long): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE chapterId = :chapterId ORDER BY orderIndex ASC, id ASC")
    suspend fun getTopicsForChapterSync(chapterId: Long): List<TopicEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Delete
    suspend fun deleteTopic(topic: TopicEntity)

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
