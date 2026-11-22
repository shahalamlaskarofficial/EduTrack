package com.edutrack.app.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.edutrack.app.data.local.AcademicJourneyEntity
import com.edutrack.app.data.local.ChapterEntity
import com.edutrack.app.data.local.ExamCountdownEntity
import com.edutrack.app.data.local.RevisionScheduleEntity
import com.edutrack.app.data.local.StudyTaskEntity
import com.edutrack.app.data.local.SubjectEntity
import com.edutrack.app.data.local.TopicEntity
import com.edutrack.app.data.local.TopicStatus
import com.edutrack.app.data.repository.EduTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class PlannerSection(val label: String) {
    TASKS("Study Planner"),
    REVISION("Revision"),
    EXAMS("Countdowns")
}

data class TopicOption(
    val topic: TopicEntity,
    val subjectName: String,
    val chapterTitle: String
)

data class TaskWithTopic(
    val task: StudyTaskEntity,
    val linkedTopicTitle: String?,
    val linkedSubjectName: String?
)

data class RevisionItemUi(
    val revision: RevisionScheduleEntity,
    val topicTitle: String,
    val chapterTitle: String,
    val subjectName: String,
    val daysFromToday: Long
)

data class CountdownItemUi(
    val entity: ExamCountdownEntity,
    val daysRemaining: Long,
    val syllabusPercent: Int,
    val chaptersRemaining: Int,
    val revisionsDueCount: Int
)

private data class PlannerControls(
    val selectedSection: PlannerSection = PlannerSection.TASKS,
    val selectedEpochDay: Long = LocalDate.now().toEpochDay(),
    val isCalendarMode: Boolean = true,
    val showPreviousTasks: Boolean = false
)

private data class SyllabusBase(
    val journey: AcademicJourneyEntity?,
    val subjects: List<SubjectEntity>,
    val chapters: List<ChapterEntity>,
    val topics: List<TopicEntity>
)

private data class PlannerRecords(
    val tasks: List<StudyTaskEntity>,
    val revisions: List<RevisionScheduleEntity>,
    val countdowns: List<ExamCountdownEntity>,
    val controls: PlannerControls
)

data class PlannerUiState(
    val selectedSection: PlannerSection = PlannerSection.TASKS,
    val selectedEpochDay: Long = LocalDate.now().toEpochDay(),
    val isCalendarMode: Boolean = true,
    val showPreviousTasks: Boolean = false,
    val dailyTasksForSelectedDate: List<TaskWithTopic> = emptyList(),
    val allPendingDailyTasks: List<TaskWithTopic> = emptyList(),
    val weeklyGoals: List<TaskWithTopic> = emptyList(),
    val previousTasks: List<TaskWithTopic> = emptyList(),
    val dueRevisions: List<RevisionItemUi> = emptyList(),
    val upcomingRevisions: List<RevisionItemUi> = emptyList(),
    val completedRevisions: List<RevisionItemUi> = emptyList(),
    val practiceTopics: List<TopicOption> = emptyList(),
    val completedTopicsForRevision: List<TopicOption> = emptyList(),
    val allTopicsForLinking: List<TopicOption> = emptyList(),
    val countdowns: List<CountdownItemUi> = emptyList()
)

class PlannerViewModel(
    private val repository: EduTrackRepository,
    private val journeyId: Long
) : ViewModel() {

    private val controls = MutableStateFlow(PlannerControls())

    private val baseFlow = combine(
        repository.observeJourney(journeyId),
        repository.observeSubjects(journeyId),
        repository.observeChaptersForJourney(journeyId),
        repository.observeTopicsForJourney(journeyId)
    ) { journey, subjects, chapters, topics ->
        SyllabusBase(journey, subjects, chapters, topics)
    }

    private val recordsFlow = combine(
        repository.observeAllTasks(journeyId),
        repository.observeAllRevisions(journeyId),
        repository.observeExamCountdowns(journeyId),
        controls
    ) { tasks, revisions, countdowns, ctrl ->
        PlannerRecords(tasks, revisions, countdowns, ctrl)
    }

    val uiState: StateFlow<PlannerUiState> = combine(baseFlow, recordsFlow) { base, rec ->
        val today = LocalDate.now().toEpochDay()
        val subjectMap = base.subjects.associateBy { it.id }
        val chapterMap = base.chapters.associateBy { it.id }
        val topicMap = base.topics.associateBy { it.id }

        val topicOptions = base.topics.mapNotNull { topic ->
            val ch = chapterMap[topic.chapterId] ?: return@mapNotNull null
            val sub = subjectMap[ch.subjectId] ?: return@mapNotNull null
            TopicOption(topic = topic, subjectName = sub.name, chapterTitle = ch.title)
        }

        val enrichedTasks = rec.tasks.map { task ->
            val topic = task.linkedTopicId?.let { topicMap[it] }
            val ch = topic?.let { chapterMap[it.chapterId] }
            val sub = ch?.let { subjectMap[it.subjectId] }
            TaskWithTopic(
                task = task,
                linkedTopicTitle = topic?.title,
                linkedSubjectName = sub?.name
            )
        }

        val dailyForDate = enrichedTasks.filter {
            !it.task.isWeeklyGoal && it.task.targetEpochDay == rec.controls.selectedEpochDay
        }
        val allPendingDaily = enrichedTasks.filter {
            !it.task.isWeeklyGoal && !it.task.isCompleted && it.task.targetEpochDay >= today
        }.sortedBy { it.task.targetEpochDay }
        val weeklyGoals = enrichedTasks.filter { it.task.isWeeklyGoal }
        val previousTasks = enrichedTasks.filter {
            !it.task.isWeeklyGoal && (it.task.targetEpochDay < today || it.task.isCompleted)
        }.sortedByDescending { it.task.targetEpochDay }

        val enrichedRevisions = rec.revisions.mapNotNull { rev ->
            val topic = topicMap[rev.topicId] ?: return@mapNotNull null
            val ch = chapterMap[topic.chapterId] ?: return@mapNotNull null
            val sub = subjectMap[ch.subjectId] ?: return@mapNotNull null
            val diff = ChronoUnit.DAYS.between(LocalDate.ofEpochDay(today), LocalDate.ofEpochDay(rev.dueEpochDay))
            RevisionItemUi(
                revision = rev,
                topicTitle = topic.title,
                chapterTitle = ch.title,
                subjectName = sub.name,
                daysFromToday = diff
            )
        }

        val dueRevisions = enrichedRevisions
            .filter { !it.revision.isCompleted && it.revision.dueEpochDay <= today }
            .sortedBy { it.revision.dueEpochDay }

        val upcomingRevisions = enrichedRevisions
            .filter { !it.revision.isCompleted && it.revision.dueEpochDay > today }
            .sortedBy { it.revision.dueEpochDay }

        val completedRevisions = enrichedRevisions
            .filter { it.revision.isCompleted }
            .sortedByDescending { it.revision.completedAtMillis ?: 0L }
            .take(25)

        val practiceTopics = topicOptions.filter { it.topic.status == TopicStatus.REVISION_REQUIRED }
        val completedForRevision = topicOptions.filter {
            it.topic.status == TopicStatus.COMPLETED || it.topic.status == TopicStatus.REVISION_REQUIRED
        }

        val totalTopics = base.topics.size
        val completedTopics = base.topics.count { it.status == TopicStatus.COMPLETED }
        val syllabusPercent = if (totalTopics > 0) (completedTopics * 100) / totalTopics else 0
        val topicsByChapter = base.topics.groupBy { it.chapterId }
        val completedChapters = base.chapters.count { ch ->
            val list = topicsByChapter[ch.id].orEmpty()
            list.isNotEmpty() && list.all { it.status == TopicStatus.COMPLETED }
        }
        val chaptersRemaining = (base.chapters.size - completedChapters).coerceAtLeast(0)

        val countdownItems = rec.countdowns.map { cd ->
            val daysLeft = ChronoUnit.DAYS.between(
                LocalDate.now(),
                LocalDate.ofEpochDay(cd.examEpochDay)
            )
            CountdownItemUi(
                entity = cd,
                daysRemaining = daysLeft,
                syllabusPercent = syllabusPercent,
                chaptersRemaining = chaptersRemaining,
                revisionsDueCount = dueRevisions.size
            )
        }

        PlannerUiState(
            selectedSection = rec.controls.selectedSection,
            selectedEpochDay = rec.controls.selectedEpochDay,
            isCalendarMode = rec.controls.isCalendarMode,
            showPreviousTasks = rec.controls.showPreviousTasks,
            dailyTasksForSelectedDate = dailyForDate,
            allPendingDailyTasks = allPendingDaily,
            weeklyGoals = weeklyGoals,
            previousTasks = previousTasks,
            dueRevisions = dueRevisions,
            upcomingRevisions = upcomingRevisions,
            completedRevisions = completedRevisions,
            practiceTopics = practiceTopics,
            completedTopicsForRevision = completedForRevision,
            allTopicsForLinking = topicOptions,
            countdowns = countdownItems
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlannerUiState()
    )

    fun selectSection(section: PlannerSection) {
        controls.update { it.copy(selectedSection = section) }
    }

    fun selectDate(epochDay: Long) {
        controls.update { it.copy(selectedEpochDay = epochDay) }
    }

    fun toggleCalendarMode() {
        controls.update { it.copy(isCalendarMode = !it.isCalendarMode) }
    }

    fun toggleShowPreviousTasks() {
        controls.update { it.copy(showPreviousTasks = !it.showPreviousTasks) }
    }

    fun addStudyTask(
        title: String,
        notes: String,
        linkedTopicId: Long?,
        targetEpochDay: Long,
        isWeeklyGoal: Boolean,
        reminderMillis: Long?,
        onCreated: (Long) -> Unit = {}
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val id = repository.addStudyTask(
                journeyId = journeyId,
                title = title.trim(),
                notes = notes.trim(),
                linkedTopicId = linkedTopicId,
                targetEpochDay = targetEpochDay,
                isWeeklyGoal = isWeeklyGoal,
                reminderTimeMillis = reminderMillis
            )
            onCreated(id)
        }
    }

    fun toggleTaskCompleted(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.updateStudyTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: StudyTaskEntity) {
        viewModelScope.launch { repository.deleteStudyTask(task) }
    }

    fun scheduleRevisionIntervals(
        topicId: Long,
        intervalsDays: List<Int>,
        onScheduled: () -> Unit = {}
    ) {
        if (intervalsDays.isEmpty()) return
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            intervalsDays.distinct().filter { it >= 0 }.forEach { days ->
                repository.addRevisionSchedule(
                    journeyId = journeyId,
                    topicId = topicId,
                    intervalDays = days,
                    dueEpochDay = today + days
                )
            }
            onScheduled()
        }
    }

    fun completeRevision(revision: RevisionScheduleEntity) {
        viewModelScope.launch {
            repository.completeRevision(revision)
        }
    }

    fun deleteRevision(revision: RevisionScheduleEntity) {
        viewModelScope.launch { repository.deleteRevisionSchedule(revision) }
    }

    fun addExamCountdown(examName: String, examEpochDay: Long) {
        if (examName.isBlank()) return
        viewModelScope.launch {
            repository.addExamCountdown(journeyId, examName.trim(), examEpochDay)
        }
    }

    fun deleteExamCountdown(countdown: ExamCountdownEntity) {
        viewModelScope.launch { repository.deleteExamCountdown(countdown) }
    }

    companion object {
        fun factory(repository: EduTrackRepository, journeyId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlannerViewModel(repository, journeyId) as T
                }
            }
    }
}
