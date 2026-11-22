package com.edutrack.app.data.repository

import com.edutrack.app.data.local.AcademicCategory
import com.edutrack.app.data.local.AcademicJourneyEntity
import com.edutrack.app.data.local.ChapterEntity
import com.edutrack.app.data.local.EduTrackDao
import com.edutrack.app.data.local.ExamCountdownEntity
import com.edutrack.app.data.local.SubjectEntity
import com.edutrack.app.data.local.ThemePreference
import com.edutrack.app.data.local.TopicEntity
import com.edutrack.app.data.local.TopicStatus
import com.edutrack.app.data.preferences.AppPreferences
import com.edutrack.app.data.preferences.UserPreferencesManager
import com.edutrack.app.data.syllabus.SyllabusCatalogLoader
import kotlinx.coroutines.flow.Flow

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
        val existingSubjects = dao.getSubjectsForJourneySync(journeyId)
        if (existingSubjects.isEmpty() && journey.syllabusVerifiedDate == "Pending") {
            syllabusLoader.syncOfficialSyllabusForJourney(dao, journey)
        }
    }

    suspend fun restoreOfficialSyllabus(journeyId: Long): Boolean {
        val journey = dao.getJourneyById(journeyId) ?: return false
        return syllabusLoader.syncOfficialSyllabusForJourney(dao, journey)
    }

    suspend fun updateTopicStatus(topic: TopicEntity, newStatus: TopicStatus) {
        val now = System.currentTimeMillis()
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
