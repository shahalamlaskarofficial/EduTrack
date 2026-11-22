package com.edutrack.app.ui.roadmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.edutrack.app.data.local.AcademicJourneyEntity
import com.edutrack.app.data.local.AchievementEntity
import com.edutrack.app.data.local.ChapterEntity
import com.edutrack.app.data.local.TopicEntity
import com.edutrack.app.data.local.TopicStatus
import com.edutrack.app.data.repository.EduTrackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MilestoneState { COMPLETED, CURRENT, UPCOMING }

data class RoadmapMilestone(
    val id: String,
    val title: String,
    val subtitle: String,
    val state: MilestoneState,
    val progressFraction: Float,
    val progressLabel: String
)

data class RoadmapUiState(
    val journey: AcademicJourneyEntity? = null,
    val overallPercent: Int = 0,
    val completedTopics: Int = 0,
    val totalTopics: Int = 0,
    val completedChapters: Int = 0,
    val totalChapters: Int = 0,
    val milestones: List<RoadmapMilestone> = emptyList(),
    val achievements: List<AchievementEntity> = emptyList()
)

class RoadmapViewModel(
    private val repository: EduTrackRepository,
    private val journeyId: Long
) : ViewModel() {

    init {
        if (journeyId > 0) {
            viewModelScope.launch {
                repository.evaluateAchievements(journeyId)
            }
        }
    }

    val uiState: StateFlow<RoadmapUiState> = combine(
        repository.observeJourney(journeyId),
        repository.observeChaptersForJourney(journeyId),
        repository.observeTopicsForJourney(journeyId),
        repository.observeAchievements(journeyId)
    ) { journey: AcademicJourneyEntity?, chapters: List<ChapterEntity>, topics: List<TopicEntity>, achievements: List<AchievementEntity> ->
        val topicsByChapter = topics.groupBy { it.chapterId }
        val totalTopics = topics.size
        val completedTopics = topics.count { it.status == TopicStatus.COMPLETED }
        val totalChapters = chapters.size
        val completedChapters = chapters.count { ch ->
            val chTopics = topicsByChapter[ch.id].orEmpty()
            chTopics.isNotEmpty() && chTopics.all { it.status == TopicStatus.COMPLETED }
        }
        val overallPercent = if (totalTopics > 0) ((completedTopics * 100f) / totalTopics).toInt() else 0

        val rawDefs = listOf(
            MilestoneDef(
                id = "journey_started",
                title = "Journey Started",
                subtitle = "Academic roadmap initialized offline",
                isUnlocked = journey != null,
                progress = 1f,
                label = "Active"
            ),
            MilestoneDef(
                id = "first_topic",
                title = "First Topic Completed",
                subtitle = "Take your first concrete step",
                isUnlocked = completedTopics >= 1,
                progress = if (completedTopics >= 1) 1f else 0f,
                label = "${completedTopics.coerceAtMost(1)} / 1 topic"
            ),
            MilestoneDef(
                id = "first_chapter",
                title = "First Chapter Completed",
                subtitle = "Complete every topic inside 1 chapter",
                isUnlocked = completedChapters >= 1,
                progress = if (completedChapters >= 1) 1f else 0f,
                label = "${completedChapters.coerceAtMost(1)} / 1 chapter"
            ),
            MilestoneDef(
                id = "ten_chapters",
                title = "First 10 Chapters Completed",
                subtitle = "Build serious chapter momentum",
                isUnlocked = completedChapters >= 10,
                progress = (completedChapters / 10f).coerceIn(0f, 1f),
                label = "${completedChapters.coerceAtMost(10)} / 10 chapters"
            ),
            MilestoneDef(
                id = "pct_25",
                title = "25% Syllabus Completion",
                subtitle = "First quarter conquered",
                isUnlocked = overallPercent >= 25,
                progress = (overallPercent / 25f).coerceIn(0f, 1f),
                label = "${overallPercent.coerceAtMost(25)}% / 25%"
            ),
            MilestoneDef(
                id = "pct_50",
                title = "50% Syllabus Completion",
                subtitle = "Halfway through your academic journey",
                isUnlocked = overallPercent >= 50,
                progress = (overallPercent / 50f).coerceIn(0f, 1f),
                label = "${overallPercent.coerceAtMost(50)}% / 50%"
            ),
            MilestoneDef(
                id = "pct_75",
                title = "75% Syllabus Completion",
                subtitle = "Final stretch ahead",
                isUnlocked = overallPercent >= 75,
                progress = (overallPercent / 75f).coerceIn(0f, 1f),
                label = "${overallPercent.coerceAtMost(75)}% / 75%"
            ),
            MilestoneDef(
                id = "pct_100",
                title = "100% Syllabus Completion",
                subtitle = "Every official & custom topic mastered",
                isUnlocked = totalTopics > 0 && completedTopics == totalTopics,
                progress = if (totalTopics > 0) completedTopics.toFloat() / totalTopics else 0f,
                label = "$overallPercent% / 100%"
            )
        )

        var foundCurrent = false
        val milestones = rawDefs.map { def ->
            val state = when {
                def.isUnlocked -> MilestoneState.COMPLETED
                !foundCurrent -> {
                    foundCurrent = true
                    MilestoneState.CURRENT
                }
                else -> MilestoneState.UPCOMING
            }
            RoadmapMilestone(
                id = def.id,
                title = def.title,
                subtitle = def.subtitle,
                state = state,
                progressFraction = def.progress,
                progressLabel = def.label
            )
        }

        RoadmapUiState(
            journey = journey,
            overallPercent = overallPercent,
            completedTopics = completedTopics,
            totalTopics = totalTopics,
            completedChapters = completedChapters,
            totalChapters = totalChapters,
            milestones = milestones,
            achievements = achievements
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RoadmapUiState()
    )

    private data class MilestoneDef(
        val id: String,
        val title: String,
        val subtitle: String,
        val isUnlocked: Boolean,
        val progress: Float,
        val label: String
    )

    companion object {
        fun factory(repository: EduTrackRepository, journeyId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RoadmapViewModel(repository, journeyId) as T
                }
            }
    }
}
