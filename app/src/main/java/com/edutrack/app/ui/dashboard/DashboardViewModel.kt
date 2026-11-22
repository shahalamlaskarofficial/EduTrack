package com.edutrack.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.edutrack.app.data.local.AcademicJourneyEntity
import com.edutrack.app.data.local.AchievementEntity
import com.edutrack.app.data.local.ChapterEntity
import com.edutrack.app.data.local.ExamCountdownEntity
import com.edutrack.app.data.local.RevisionScheduleEntity
import com.edutrack.app.data.local.StudySessionEntity
import com.edutrack.app.data.local.StudyTaskEntity
import com.edutrack.app.data.local.SubjectEntity
import com.edutrack.app.data.local.TopicEntity
import com.edutrack.app.data.local.TopicStatus
import com.edutrack.app.data.repository.EduTrackRepository
import com.edutrack.app.data.syllabus.SubjectWithProgress
import com.edutrack.app.data.syllabus.TopicSearchResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class NextMilestonePreview(
    val title: String,
    val requirementText: String,
    val progressFraction: Float
)

data class ExamCountdownSummary(
    val id: Long,
    val examName: String,
    val daysRemaining: Long,
    val examDateEpochDay: Long
)

data class DashboardUiState(
    val journey: AcademicJourneyEntity? = null,
    val overallPercent: Int = 0,
    val overallFraction: Float = 0f,
    val totalTopics: Int = 0,
    val completedTopics: Int = 0,
    val totalChapters: Int = 0,
    val completedChapters: Int = 0,
    val remainingChapters: Int = 0,
    val subjects: List<SubjectWithProgress> = emptyList(),
    val inProgressTopics: List<TopicSearchResult> = emptyList(),
    val revisionTopics: List<TopicSearchResult> = emptyList(),
    val recentTopics: List<TopicSearchResult> = emptyList(),
    val todayTasks: List<StudyTaskEntity> = emptyList(),
    val dueRevisionsCount: Int = 0,
    val upcomingExam: ExamCountdownSummary? = null,
    val nextMilestone: NextMilestonePreview? = null,
    val currentStreakDays: Int = 0,
    val unlockedAchievementsCount: Int = 0,
    val totalAchievementsCount: Int = 6
)

class DashboardViewModel(
    private val repository: EduTrackRepository,
    private val journeyId: Long
) : ViewModel() {

    private val todayEpochDay: Long = LocalDate.now().toEpochDay()

    init {
        if (journeyId > 0) {
            viewModelScope.launch {
                repository.ensureOfficialSyllabusLoaded(journeyId)
                repository.evaluateAchievements(journeyId)
            }
        }
    }

    private val syllabusBundleFlow = combine(
        repository.observeJourney(journeyId),
        repository.observeSubjects(journeyId),
        repository.observeChaptersForJourney(journeyId),
        repository.observeTopicsForJourney(journeyId)
    ) { journey, subjects, chapters, topics ->
        SyllabusBundle(journey, subjects, chapters, topics)
    }

    private val activityBundleFlow = combine(
        repository.observeTasksForDay(journeyId, todayEpochDay),
        repository.observeDueRevisions(journeyId, todayEpochDay),
        repository.observeExamCountdowns(journeyId),
        repository.observeStudySessions(journeyId),
        repository.observeAchievements(journeyId)
    ) { tasks, dueRevisions, exams, sessions, achievements ->
        ActivityBundle(tasks, dueRevisions, exams, sessions, achievements)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        syllabusBundleFlow,
        activityBundleFlow
    ) { syl, act ->
        val subjectMap = syl.subjects.associateBy { it.id }
        val chapterMap = syl.chapters.associateBy { it.id }
        val topicsByChapter = syl.topics.groupBy { it.chapterId }
        val chaptersBySubject = syl.chapters.groupBy { it.subjectId }

        var totalChapters = 0
        var completedChapters = 0

        val subjectProgressList = syl.subjects.map { sub ->
            val subChapters = chaptersBySubject[sub.id].orEmpty()
            var subCompletedChapters = 0
            var subTotalTopics = 0
            var subCompletedTopics = 0

            subChapters.forEach { ch ->
                val chTopics = topicsByChapter[ch.id].orEmpty()
                val done = chTopics.count { it.status == TopicStatus.COMPLETED }
                subTotalTopics += chTopics.size
                subCompletedTopics += done
                if (chTopics.isNotEmpty() && done == chTopics.size) {
                    subCompletedChapters++
                }
            }

            totalChapters += subChapters.size
            completedChapters += subCompletedChapters

            SubjectWithProgress(
                subject = sub,
                totalChapters = subChapters.size,
                completedChapters = subCompletedChapters,
                totalTopics = subTotalTopics,
                completedTopics = subCompletedTopics
            )
        }

        val totalTopics = syl.topics.size
        val completedTopics = syl.topics.count { it.status == TopicStatus.COMPLETED }
        val overallFraction = if (totalTopics > 0) completedTopics.toFloat() / totalTopics else 0f
        val overallPercent = (overallFraction * 100).toInt()

        fun toResult(topic: TopicEntity): TopicSearchResult? {
            val ch = chapterMap[topic.chapterId] ?: return null
            val sub = subjectMap[ch.subjectId] ?: return null
            return TopicSearchResult(topic, ch.title, sub.name, sub.id, ch.id)
        }

        val inProgressTopics = syl.topics
            .filter { it.status == TopicStatus.IN_PROGRESS }
            .sortedByDescending { it.lastUpdatedMillis }
            .take(5)
            .mapNotNull(::toResult)

        val revisionTopics = syl.topics
            .filter { it.status == TopicStatus.REVISION_REQUIRED }
            .sortedByDescending { it.lastUpdatedMillis }
            .take(5)
            .mapNotNull(::toResult)

        val recentTopics = syl.topics
            .filter { it.status != TopicStatus.NOT_STARTED }
            .sortedByDescending { it.lastUpdatedMillis }
            .take(5)
            .mapNotNull(::toResult)

        val today = LocalDate.now()
        val nearestExam = act.exams
            .map {
                ExamCountdownSummary(
                    id = it.id,
                    examName = it.examName,
                    daysRemaining = ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(it.examEpochDay)),
                    examDateEpochDay = it.examEpochDay
                )
            }
            .firstOrNull { it.daysRemaining >= 0 }
            ?: syl.journey?.targetExamDateEpochDay?.let { epoch ->
                ExamCountdownSummary(
                    id = 0L,
                    examName = syl.journey.title,
                    daysRemaining = ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(epoch)),
                    examDateEpochDay = epoch
                )
            }

        val nextMilestone = computeNextMilestone(
            completedTopics = completedTopics,
            completedChapters = completedChapters,
            overallPercent = overallPercent,
            overallFraction = overallFraction
        )

        val streak = calculateCurrentStreak(act.sessions, todayEpochDay)

        DashboardUiState(
            journey = syl.journey,
            overallPercent = overallPercent,
            overallFraction = overallFraction,
            totalTopics = totalTopics,
            completedTopics = completedTopics,
            totalChapters = totalChapters,
            completedChapters = completedChapters,
            remainingChapters = (totalChapters - completedChapters).coerceAtLeast(0),
            subjects = subjectProgressList,
            inProgressTopics = inProgressTopics,
            revisionTopics = revisionTopics,
            recentTopics = recentTopics,
            todayTasks = act.tasks,
            dueRevisionsCount = act.dueRevisions.size + revisionTopics.size,
            upcomingExam = nearestExam,
            nextMilestone = nextMilestone,
            currentStreakDays = streak,
            unlockedAchievementsCount = act.achievements.count { it.unlockedAtMillis != null },
            totalAchievementsCount = act.achievements.size.coerceAtLeast(6)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun quickCompleteTopic(topic: TopicEntity) {
        viewModelScope.launch {
            repository.updateTopicStatus(topic, TopicStatus.COMPLETED)
        }
    }

    fun toggleTaskCompleted(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.updateStudyTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    private fun computeNextMilestone(
        completedTopics: Int,
        completedChapters: Int,
        overallPercent: Int,
        overallFraction: Float
    ): NextMilestonePreview {
        return when {
            completedTopics < 1 -> NextMilestonePreview(
                title = "First Step",
                requirementText = "Complete your first topic",
                progressFraction = 0f
            )
            completedChapters < 1 -> NextMilestonePreview(
                title = "First Chapter Completed",
                requirementText = "Finish all topics in any 1 chapter",
                progressFraction = overallFraction.coerceIn(0.1f, 0.9f)
            )
            overallPercent < 25 -> NextMilestonePreview(
                title = "25% Syllabus Completion",
                requirementText = "${25 - overallPercent}% more to unlock",
                progressFraction = (overallPercent / 25f).coerceIn(0f, 1f)
            )
            completedChapters < 10 -> NextMilestonePreview(
                title = "First 10 Chapters Completed",
                requirementText = "$completedChapters / 10 chapters done",
                progressFraction = (completedChapters / 10f).coerceIn(0f, 1f)
            )
            overallPercent < 50 -> NextMilestonePreview(
                title = "50% Syllabus Completion",
                requirementText = "${50 - overallPercent}% more to halfway",
                progressFraction = (overallPercent / 50f).coerceIn(0f, 1f)
            )
            overallPercent < 75 -> NextMilestonePreview(
                title = "75% Syllabus Completion",
                requirementText = "${75 - overallPercent}% more to unlock",
                progressFraction = (overallPercent / 75f).coerceIn(0f, 1f)
            )
            overallPercent < 100 -> NextMilestonePreview(
                title = "100% Syllabus Mastery",
                requirementText = "${100 - overallPercent}% remaining to finish",
                progressFraction = overallFraction
            )
            else -> NextMilestonePreview(
                title = "Syllabus Complete!",
                requirementText = "Focus on spaced revisions and mastery",
                progressFraction = 1f
            )
        }
    }

    private fun calculateCurrentStreak(sessions: List<StudySessionEntity>, today: Long): Int {
        val activeDays = sessions
            .filter { it.topicsCompletedCount > 0 || it.revisionsCompletedCount > 0 }
            .map { it.epochDay }
            .toSet()
        if (activeDays.isEmpty()) return 0

        var cursor = if (today in activeDays) today else if ((today - 1) in activeDays) today - 1 else return 0
        var streak = 0
        while (cursor in activeDays) {
            streak++
            cursor--
        }
        return streak
    }

    private data class SyllabusBundle(
        val journey: AcademicJourneyEntity?,
        val subjects: List<SubjectEntity>,
        val chapters: List<ChapterEntity>,
        val topics: List<TopicEntity>
    )

    private data class ActivityBundle(
        val tasks: List<StudyTaskEntity>,
        val dueRevisions: List<RevisionScheduleEntity>,
        val exams: List<ExamCountdownEntity>,
        val sessions: List<StudySessionEntity>,
        val achievements: List<AchievementEntity>
    )

    companion object {
        fun factory(repository: EduTrackRepository, journeyId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(repository, journeyId) as T
                }
            }
    }
}
