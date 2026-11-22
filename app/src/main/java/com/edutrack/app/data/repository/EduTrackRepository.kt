package com.edutrack.app.data.repository

import com.edutrack.app.data.local.AcademicCategory
import com.edutrack.app.data.local.AcademicJourneyEntity
import com.edutrack.app.data.local.EduTrackDao
import com.edutrack.app.data.local.ExamCountdownEntity
import com.edutrack.app.data.local.ThemePreference
import com.edutrack.app.data.preferences.AppPreferences
import com.edutrack.app.data.preferences.UserPreferencesManager
import kotlinx.coroutines.flow.Flow

class EduTrackRepository(
    private val dao: EduTrackDao,
    private val prefs: UserPreferencesManager
) {
    val preferencesFlow: Flow<AppPreferences> = prefs.preferencesFlow
    val allJourneysFlow: Flow<List<AcademicJourneyEntity>> = dao.observeAllJourneys()

    fun observeJourney(journeyId: Long): Flow<AcademicJourneyEntity?> =
        dao.observeJourneyById(journeyId)

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
