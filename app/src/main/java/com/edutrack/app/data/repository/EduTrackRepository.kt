package com.edutrack.app.data.repository

import com.edutrack.app.data.local.AcademicCategory
import com.edutrack.app.data.local.AcademicJourneyEntity
import com.edutrack.app.data.local.AchievementEntity
import com.edutrack.app.data.local.ChapterEntity
import com.edutrack.app.data.local.EduTrackDao
import com.edutrack.app.data.local.ExamCountdownEntity
import com.edutrack.app.data.local.RevisionScheduleEntity
import com.edutrack.app.data.local.StudySessionEntity
import com.edutrack.app.data.local.StudyTaskEntity
import com.edutrack.app.data.local.SubjectEntity
import com.edutrack.app.data.local.ThemePreference
import com.edutrack.app.data.local.TopicEntity
import com.edutrack.app.data.local.TopicStatus
import com.edutrack.app.data.preferences.AppPreferences
import com.edutrack.app.data.preferences.UserPreferencesManager
import com.edutrack.app.data.syllabus.SyllabusCatalogLoader
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class EduTrackRepository(
    private val dao: EduTrackDao,
    private val prefs: UserPreferencesManager,
    private val syllabusLoader: SyllabusCatalogLoader
) {
    val preferencesFlow: Flow<AppPreferences> = prefs.preferencesFlow
    val allJourneysFlow: Flow<List<AcademicJourneyEntity>> = dao.observeAllJourneys()

    fun observeJourney(journeyId: Long): Flow<AcademicJourneyEntity?> =
        dao.observeJourneyById(journeyId)

    fun observeSubjects(journeyId: Long): Flow<List<SubjectEntity>> =
        dao.observeSubjectsForJourney(journeyId)

    fun observeChaptersForJourney(journeyId: Long): Flow<List<ChapterEntity>> =
        dao.observeChaptersForJourney(journeyId)

    fun observeTopicsForJourney(journeyId: Long): Flow<List<TopicEntity>> =
        dao.observeTopicsForJourney(journeyId)

    fun observeTotalTopics(journeyId: Long): Flow<Int> = dao.observeTotalTopicCount(journeyId)

    fun observeCompletedTopics(journeyId: Long): Flow<Int> = dao.observeCompletedTopicCount(journeyId)

    fun observeExamCountdowns(journeyId: Long): Flow<List<ExamCountdownEntity>> =
        dao.observeExamCountdowns(journeyId)

    fun observeTasksForDay(journeyId: Long, epochDay: Long): Flow<List<StudyTaskEntity>> =
        dao.observeTasksForDay(journeyId, epochDay)

    fun observeDueRevisions(journeyId: Long, upToEpochDay: Long): Flow<List<RevisionScheduleEntity>> =
        dao.observeDueRevisions(journeyId, upToEpochDay)

    fun observeAchievements(journeyId: Long): Flow<List<AchievementEntity>> =
        dao.observeAchievements(journeyId)

    fun observeStudySessions(journeyId: Long): Flow<List<StudySessionEntity>> =
        dao.observeStudySessions(journeyId)

    suspend fun createAcademicJourney(
        category: AcademicCategory,
        boardOrExam: String,
        classOrLevel: String,
        stream: String,
        academicSession: String,
        targetExamDateEpochDay: Long?,
        studentName: String? = null
    ): Long {
        val cleanTitle = buildString {
            append(boardOrExam)
            if (classOrLevel.isNotBlank()) append(" • $classOrLevel")
            if (stream.isNotBlank()) append(" ($stream)")
        }

        val datasetId = buildDatasetKey(category, boardOrExam, classOrLevel, stream)

        val journeyId = dao.insertJourney(
            AcademicJourneyEntity(
                title = cleanTitle,
                category = category,
                boardOrExam = boardOrExam,
                classOrLevel = classOrLevel,
                stream = stream,
                academicSession = academicSession,
                targetExamDateEpochDay = targetExamDateEpochDay,
                syllabusDatasetId = datasetId
            )
        )

        dao.getJourneyById(journeyId)?.let { created ->
            syllabusLoader.syncOfficialSyllabusForJourney(dao, created)
        }
        seedDefaultAchievementsIfMissing(journeyId)

        if (targetExamDateEpochDay != null) {
            dao.insertExamCountdown(
                ExamCountdownEntity(
                    journeyId = journeyId,
                    examName = cleanTitle,
                    examEpochDay = targetExamDateEpochDay
                )
            )
        }

        if (!studentName.isNullOrBlank()) {
            prefs.setStudentName(studentName)
        }

        prefs.setOnboardingCompleted(completed = true, activeJourneyId = journeyId)
        return journeyId
    }

    suspend fun ensureOfficialSyllabusLoaded(journeyId: Long) {
        val journey = dao.getJourneyById(journeyId) ?: return
        val latestVersion = syllabusLoader.currentCatalogVersion()
        val existingSubjects = dao.getSubjectsForJourneySync(journeyId)
        if (existingSubjects.isEmpty() || journey.syllabusDatasetVersion != latestVersion) {
            syllabusLoader.syncOfficialSyllabusForJourney(dao, journey)
        }
        seedDefaultAchievementsIfMissing(journeyId)
    }

    suspend fun restoreOfficialSyllabus(journeyId: Long): Boolean {
        val journey = dao.getJourneyById(journeyId) ?: return false
        return syllabusLoader.syncOfficialSyllabusForJourney(dao, journey)
    }

    suspend fun updateTopicStatus(topic: TopicEntity, newStatus: TopicStatus) {
        val now = System.currentTimeMillis()
        val wasCompleted = topic.status == TopicStatus.COMPLETED
        val completedAt = when (newStatus) {
            TopicStatus.COMPLETED -> topic.completedAtMillis ?: now
            TopicStatus.REVISION_REQUIRED -> topic.completedAtMillis
            else -> null
        }
        dao.updateTopic(
            topic.copy(
                status = newStatus,
                lastUpdatedMillis = now,
                completedAtMillis = completedAt
            )
        )

        val journeyId = dao.getJourneyIdForChapter(topic.chapterId) ?: return
        if (!wasCompleted && newStatus == TopicStatus.COMPLETED) {
            recordStudyActivity(journeyId = journeyId, topicsDelta = 1, revisionsDelta = 0)
        } else if (topic.status == TopicStatus.REVISION_REQUIRED && newStatus == TopicStatus.COMPLETED) {
            recordStudyActivity(journeyId = journeyId, topicsDelta = 0, revisionsDelta = 1)
        }
        evaluateAchievements(journeyId)
    }

    suspend fun updateStudyTask(task: StudyTaskEntity) = dao.updateStudyTask(task)

    private suspend fun recordStudyActivity(journeyId: Long, topicsDelta: Int, revisionsDelta: Int) {
        val today = LocalDate.now().toEpochDay()
        val existing = dao.getStudySessionByDay(journeyId, today)
        if (existing != null) {
            dao.updateStudySession(
                existing.copy(
                    topicsCompletedCount = existing.topicsCompletedCount + topicsDelta,
                    revisionsCompletedCount = existing.revisionsCompletedCount + revisionsDelta
                )
            )
        } else {
            dao.insertStudySession(
                StudySessionEntity(
                    journeyId = journeyId,
                    epochDay = today,
                    topicsCompletedCount = topicsDelta,
                    revisionsCompletedCount = revisionsDelta
                )
            )
        }
    }

    private suspend fun seedDefaultAchievementsIfMissing(journeyId: Long) {
        val existingCodes = dao.getAchievementsSync(journeyId).map { it.code }.toSet()
        val defaults = listOf(
            AchievementEntity(journeyId = journeyId, code = "FIRST_STEP", title = "First Step", description = "Complete your very first syllabus topic"),
            AchievementEntity(journeyId = journeyId, code = "CONSISTENT_LEARNER", title = "Consistent Learner", description = "Log study progress on 3 different days"),
            AchievementEntity(journeyId = journeyId, code = "CHAPTER_MASTER", title = "Chapter Master", description = "Complete 10 full chapters"),
            AchievementEntity(journeyId = journeyId, code = "HALFWAY_THERE", title = "Halfway There", description = "Reach 50% overall syllabus completion"),
            AchievementEntity(journeyId = journeyId, code = "REVISION_CHAMPION", title = "Revision Champion", description = "Complete a scheduled revision cycle"),
            AchievementEntity(journeyId = journeyId, code = "SYLLABUS_FINISHER", title = "Syllabus Finisher", description = "Reach 100% syllabus completion")
        )
        defaults.forEach { badge ->
            if (badge.code !in existingCodes) {
                dao.insertAchievement(badge)
            }
        }
    }

    suspend fun evaluateAchievements(journeyId: Long) {
        seedDefaultAchievementsIfMissing(journeyId)
        val badges = dao.getAchievementsSync(journeyId)
        val chapters = dao.getChaptersForJourneySync(journeyId)
        val topics = dao.getTopicsForJourneySync(journeyId)

        val totalTopics = topics.size
        val completedTopics = topics.count { it.status == TopicStatus.COMPLETED }
        val topicsByChapter = topics.groupBy { it.chapterId }
        val completedChapters = chapters.count { ch ->
            val list = topicsByChapter[ch.id].orEmpty()
            list.isNotEmpty() && list.all { it.status == TopicStatus.COMPLETED }
        }
        val percent = if (totalTopics > 0) (completedTopics * 100) / totalTopics else 0
        val activeStudyDays = dao.getActiveStudyDaysCount(journeyId)
        val completedRevisions = dao.getCompletedRevisionsCount(journeyId)

        val now = System.currentTimeMillis()
        badges.forEach { badge ->
            if (badge.unlockedAtMillis != null) return@forEach
            val earned = when (badge.code) {
                "FIRST_STEP" -> completedTopics >= 1
                "CONSISTENT_LEARNER" -> activeStudyDays >= 3
                "CHAPTER_MASTER" -> completedChapters >= 10
                "HALFWAY_THERE" -> totalTopics > 0 && percent >= 50
                "REVISION_CHAMPION" -> completedRevisions >= 1
                "SYLLABUS_FINISHER" -> totalTopics > 0 && completedTopics == totalTopics
                else -> false
            }
            if (earned) {
                dao.updateAchievement(badge.copy(unlockedAtMillis = now))
            }
        }
    }

    suspend fun addCustomSubject(journeyId: Long, name: String) {
        val current = dao.getSubjectsForJourneySync(journeyId)
        val nextIndex = (current.maxOfOrNull { it.orderIndex } ?: -1) + 1
        dao.insertSubject(
            SubjectEntity(
                journeyId = journeyId,
                officialKey = null,
                name = name,
                orderIndex = nextIndex,
                isCustom = true
            )
        )
    }

    suspend fun updateSubject(subject: SubjectEntity) = dao.updateSubject(subject)

    suspend fun deleteSubject(subject: SubjectEntity) = dao.deleteSubject(subject)

    suspend fun moveSubject(journeyId: Long, subjectId: Long, moveUp: Boolean) {
        val active = dao.getSubjectsForJourneySync(journeyId).filter { !it.isArchived }.toMutableList()
        val idx = active.indexOfFirst { it.id == subjectId }
        val target = if (moveUp) idx - 1 else idx + 1
        if (idx < 0 || target !in active.indices) return
        val item = active.removeAt(idx)
        active.add(target, item)
        active.forEachIndexed { index, sub ->
            if (sub.orderIndex != index) dao.updateSubject(sub.copy(orderIndex = index))
        }
    }

    suspend fun addCustomChapter(subjectId: Long, title: String, unitName: String) {
        val current = dao.getChaptersForSubjectSync(subjectId)
        val nextIndex = (current.maxOfOrNull { it.orderIndex } ?: -1) + 1
        dao.insertChapter(
            ChapterEntity(
                subjectId = subjectId,
                officialKey = null,
                title = title,
                unitName = unitName,
                orderIndex = nextIndex,
                isCustom = true
            )
        )
    }

    suspend fun updateChapter(chapter: ChapterEntity) = dao.updateChapter(chapter)

    suspend fun deleteChapter(chapter: ChapterEntity) = dao.deleteChapter(chapter)

    suspend fun moveChapter(subjectId: Long, chapterId: Long, moveUp: Boolean) {
        val active = dao.getChaptersForSubjectSync(subjectId).filter { !it.isArchived }.toMutableList()
        val idx = active.indexOfFirst { it.id == chapterId }
        val target = if (moveUp) idx - 1 else idx + 1
        if (idx < 0 || target !in active.indices) return
        val item = active.removeAt(idx)
        active.add(target, item)
        active.forEachIndexed { index, ch ->
            if (ch.orderIndex != index) dao.updateChapter(ch.copy(orderIndex = index))
        }
    }

    suspend fun addCustomTopic(chapterId: Long, title: String, parentTopicId: Long?) {
        val current = dao.getTopicsForChapterSync(chapterId)
        val nextIndex = (current.maxOfOrNull { it.orderIndex } ?: -1) + 1
        dao.insertTopic(
            TopicEntity(
                chapterId = chapterId,
                parentTopicId = parentTopicId,
                officialKey = null,
                title = title,
                status = TopicStatus.NOT_STARTED,
                orderIndex = nextIndex,
                isCustom = true
            )
        )
    }

    suspend fun updateTopic(topic: TopicEntity) = dao.updateTopic(topic)

    suspend fun deleteTopic(topic: TopicEntity) = dao.deleteTopic(topic)

    suspend fun moveTopic(chapterId: Long, topicId: Long, moveUp: Boolean) {
        val active = dao.getTopicsForChapterSync(chapterId).filter { !it.isArchived }.toMutableList()
        val idx = active.indexOfFirst { it.id == topicId }
        val target = if (moveUp) idx - 1 else idx + 1
        if (idx < 0 || target !in active.indices) return
        val item = active.removeAt(idx)
        active.add(target, item)
        active.forEachIndexed { index, t ->
            if (t.orderIndex != index) dao.updateTopic(t.copy(orderIndex = index))
        }
    }

    suspend fun deleteJourney(journey: AcademicJourneyEntity, remainingJourneys: List<AcademicJourneyEntity>) {
        dao.deleteJourney(journey)
        val fallback = remainingJourneys.firstOrNull { it.id != journey.id }
        if (fallback != null) {
            prefs.setActiveJourneyId(fallback.id)
        } else {
            prefs.setOnboardingCompleted(completed = false, activeJourneyId = -1L)
        }
    }

    suspend fun selectActiveJourney(journeyId: Long) = prefs.setActiveJourneyId(journeyId)

    suspend fun setTheme(theme: ThemePreference) = prefs.setThemePreference(theme)

    suspend fun setNotificationsEnabled(enabled: Boolean) = prefs.setNotificationsEnabled(enabled)

    private fun buildDatasetKey(
        category: AcademicCategory,
        boardOrExam: String,
        classOrLevel: String,
        stream: String
    ): String {
        return listOf(category.name, boardOrExam, classOrLevel, stream)
            .filter { it.isNotBlank() }
            .joinToString("_")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }
}
