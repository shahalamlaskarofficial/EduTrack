package com.edutrack.app.ui.syllabus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.edutrack.app.data.local.AcademicJourneyEntity
import com.edutrack.app.data.local.ChapterEntity
import com.edutrack.app.data.local.SubjectEntity
import com.edutrack.app.data.local.TopicEntity
import com.edutrack.app.data.local.TopicStatus
import com.edutrack.app.data.repository.EduTrackRepository
import com.edutrack.app.data.syllabus.ChapterWithProgress
import com.edutrack.app.data.syllabus.SubjectWithProgress
import com.edutrack.app.data.syllabus.TopicSearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SyllabusSortMode(val label: String) {
    DEFAULT("Syllabus Order"),
    COMPLETION_ASC("Lowest % First"),
    COMPLETION_DESC("Highest % First")
}

enum class ChapterFilterMode(val label: String) {
    ALL("All"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    REVISION("Revision")
}

private data class FilterNavState(
    val selectedSubjectId: Long? = null,
    val selectedChapterId: Long? = null,
    val searchQuery: String = "",
    val sortMode: SyllabusSortMode = SyllabusSortMode.DEFAULT,
    val chapterFilter: ChapterFilterMode = ChapterFilterMode.ALL,
    val topicStatusFilter: TopicStatus? = null,
    val isRestoring: Boolean = false,
    val statusBanner: String? = null
)

data class SyllabusUiState(
    val journey: AcademicJourneyEntity? = null,
    val subjects: List<SubjectWithProgress> = emptyList(),
    val selectedSubject: SubjectWithProgress? = null,
    val chaptersForSelectedSubject: List<ChapterWithProgress> = emptyList(),
    val selectedChapter: ChapterWithProgress? = null,
    val topicsForSelectedChapter: List<TopicEntity> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<TopicSearchResult> = emptyList(),
    val recentlyAccessedTopics: List<TopicSearchResult> = emptyList(),
    val sortMode: SyllabusSortMode = SyllabusSortMode.DEFAULT,
    val chapterFilter: ChapterFilterMode = ChapterFilterMode.ALL,
    val topicStatusFilter: TopicStatus? = null,
    val isRestoring: Boolean = false,
    val statusBanner: String? = null
)

class SyllabusViewModel(
    private val repository: EduTrackRepository,
    private val journeyId: Long
) : ViewModel() {

    private val navFilterState = MutableStateFlow(FilterNavState())

    init {
        if (journeyId > 0) {
            viewModelScope.launch {
                repository.ensureOfficialSyllabusLoaded(journeyId)
            }
        }
    }

    private val dataFlow = combine(
        repository.observeJourney(journeyId),
        repository.observeSubjects(journeyId),
        repository.observeChaptersForJourney(journeyId),
        repository.observeTopicsForJourney(journeyId)
    ) { journey, subjects, chapters, topics ->
        RawSyllabusBundle(journey, subjects, chapters, topics)
    }

    val uiState: StateFlow<SyllabusUiState> = combine(
        dataFlow,
        navFilterState
    ) { raw, nav ->
        val topicsByChapter = raw.topics.groupBy { it.chapterId }
        val chaptersBySubject = raw.chapters.groupBy { it.subjectId }
        val subjectMap = raw.subjects.associateBy { it.id }
        val chapterMap = raw.chapters.associateBy { it.id }

        val chapterProgressMap = raw.chapters.associate { ch ->
            val chTopics = topicsByChapter[ch.id].orEmpty()
            ch.id to ChapterWithProgress(
                chapter = ch,
                totalTopics = chTopics.size,
                completedTopics = chTopics.count { it.status == TopicStatus.COMPLETED },
                inProgressTopics = chTopics.count { it.status == TopicStatus.IN_PROGRESS },
                revisionRequiredTopics = chTopics.count { it.status == TopicStatus.REVISION_REQUIRED }
            )
        }

        val subjectsRaw = raw.subjects.map { sub ->
            val subChapters = chaptersBySubject[sub.id].orEmpty()
                .mapNotNull { chapterProgressMap[it.id] }
            SubjectWithProgress(
                subject = sub,
                totalChapters = subChapters.size,
                completedChapters = subChapters.count { it.isCompleted },
                totalTopics = subChapters.sumOf { it.totalTopics },
                completedTopics = subChapters.sumOf { it.completedTopics }
            )
        }

        val sortedSubjects = when (nav.sortMode) {
            SyllabusSortMode.DEFAULT -> subjectsRaw
            SyllabusSortMode.COMPLETION_ASC -> subjectsRaw.sortedBy { it.progressFraction }
            SyllabusSortMode.COMPLETION_DESC -> subjectsRaw.sortedByDescending { it.progressFraction }
        }

        val selectedSubject = sortedSubjects.firstOrNull { it.subject.id == nav.selectedSubjectId }

        val chaptersForSubject = selectedSubject?.let { sub ->
            val base = chaptersBySubject[sub.subject.id].orEmpty()
                .mapNotNull { chapterProgressMap[it.id] }
            val filtered = when (nav.chapterFilter) {
                ChapterFilterMode.ALL -> base
                ChapterFilterMode.COMPLETED -> base.filter { it.isCompleted }
                ChapterFilterMode.IN_PROGRESS -> base.filter { !it.isCompleted && (it.inProgressTopics > 0 || it.completedTopics > 0) }
                ChapterFilterMode.REVISION -> base.filter { it.revisionRequiredTopics > 0 }
            }
            when (nav.sortMode) {
                SyllabusSortMode.DEFAULT -> filtered
                SyllabusSortMode.COMPLETION_ASC -> filtered.sortedBy { it.progressFraction }
                SyllabusSortMode.COMPLETION_DESC -> filtered.sortedByDescending { it.progressFraction }
            }
        }.orEmpty()

        val selectedChapter = nav.selectedChapterId?.let { chapterProgressMap[it] }

        val topicsForChapter = selectedChapter?.let { ch ->
            val all = topicsByChapter[ch.chapter.id].orEmpty()
            nav.topicStatusFilter?.let { filter -> all.filter { it.status == filter } } ?: all
        }.orEmpty()

        val query = nav.searchQuery.trim()
        val searchResults = if (query.length >= 2) {
            raw.topics.mapNotNull { topic ->
                val ch = chapterMap[topic.chapterId] ?: return@mapNotNull null
                val sub = subjectMap[ch.subjectId] ?: return@mapNotNull null
                val matches = topic.title.contains(query, ignoreCase = true) ||
                    ch.title.contains(query, ignoreCase = true) ||
                    sub.name.contains(query, ignoreCase = true)
                if (matches) {
                    TopicSearchResult(
                        topic = topic,
                        chapterTitle = ch.title,
                        subjectName = sub.name,
                        subjectId = sub.id,
                        chapterId = ch.id
                    )
                } else null
            }.take(30)
        } else emptyList()

        val recentTopics = raw.topics
            .filter { it.status != TopicStatus.NOT_STARTED }
            .sortedByDescending { it.lastUpdatedMillis }
            .take(6)
            .mapNotNull { topic ->
                val ch = chapterMap[topic.chapterId] ?: return@mapNotNull null
                val sub = subjectMap[ch.subjectId] ?: return@mapNotNull null
                TopicSearchResult(
                    topic = topic,
                    chapterTitle = ch.title,
                    subjectName = sub.name,
                    subjectId = sub.id,
                    chapterId = ch.id
                )
            }

        SyllabusUiState(
            journey = raw.journey,
            subjects = sortedSubjects,
            selectedSubject = selectedSubject,
            chaptersForSelectedSubject = chaptersForSubject,
            selectedChapter = selectedChapter,
            topicsForSelectedChapter = topicsForChapter,
            searchQuery = nav.searchQuery,
            searchResults = searchResults,
            recentlyAccessedTopics = recentTopics,
            sortMode = nav.sortMode,
            chapterFilter = nav.chapterFilter,
            topicStatusFilter = nav.topicStatusFilter,
            isRestoring = nav.isRestoring,
            statusBanner = nav.statusBanner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SyllabusUiState()
    )

    fun openSubject(subjectId: Long) {
        navFilterState.update {
            it.copy(
                selectedSubjectId = subjectId,
                selectedChapterId = null,
                chapterFilter = ChapterFilterMode.ALL,
                topicStatusFilter = null
            )
        }
    }

    fun openChapter(subjectId: Long, chapterId: Long) {
        navFilterState.update {
            it.copy(
                selectedSubjectId = subjectId,
                selectedChapterId = chapterId,
                topicStatusFilter = null
            )
        }
    }

    fun navigateUp(): Boolean {
        val current = navFilterState.value
        return when {
            current.selectedChapterId != null -> {
                navFilterState.update { it.copy(selectedChapterId = null, topicStatusFilter = null) }
                true
            }
            current.selectedSubjectId != null -> {
                navFilterState.update { it.copy(selectedSubjectId = null, chapterFilter = ChapterFilterMode.ALL) }
                true
            }
            else -> false
        }
    }

    fun updateSearchQuery(query: String) {
        navFilterState.update { it.copy(searchQuery = query) }
    }

    fun cycleSortMode() {
        navFilterState.update { state ->
            val modes = SyllabusSortMode.entries
            val next = modes[(modes.indexOf(state.sortMode) + 1) % modes.size]
            state.copy(sortMode = next)
        }
    }

    fun setChapterFilter(filter: ChapterFilterMode) {
        navFilterState.update { it.copy(chapterFilter = filter) }
    }

    fun setTopicStatusFilter(status: TopicStatus?) {
        navFilterState.update { it.copy(topicStatusFilter = status) }
    }

    fun clearBanner() {
        navFilterState.update { it.copy(statusBanner = null) }
    }

    fun updateTopicStatus(topic: TopicEntity, newStatus: TopicStatus) {
        viewModelScope.launch {
            repository.updateTopicStatus(topic, newStatus)
        }
    }

    // Custom Missing Content CRUD + Reorder + Archive
    fun addCustomSubject(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCustomSubject(journeyId, name.trim())
            navFilterState.update { it.copy(statusBanner = "Added custom subject \"$name\"") }
        }
    }

    fun renameCustomSubject(subject: SubjectEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repository.updateSubject(subject.copy(name = newName.trim())) }
    }

    fun moveSubject(subject: SubjectEntity, moveUp: Boolean) {
        viewModelScope.launch { repository.moveSubject(journeyId, subject.id, moveUp) }
    }

    fun archiveSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.updateSubject(subject.copy(isArchived = true))
            if (navFilterState.value.selectedSubjectId == subject.id) {
                navFilterState.update { it.copy(selectedSubjectId = null, selectedChapterId = null) }
            }
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
            if (navFilterState.value.selectedSubjectId == subject.id) {
                navFilterState.update { it.copy(selectedSubjectId = null, selectedChapterId = null) }
            }
        }
    }

    fun addCustomChapter(subjectId: Long, title: String, unitName: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addCustomChapter(subjectId, title.trim(), unitName.trim())
            navFilterState.update { it.copy(statusBanner = "Added custom chapter \"$title\"") }
        }
    }

    fun renameCustomChapter(chapter: ChapterEntity, newTitle: String, newUnit: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repository.updateChapter(chapter.copy(title = newTitle.trim(), unitName = newUnit.trim()))
        }
    }

    fun moveChapter(subjectId: Long, chapter: ChapterEntity, moveUp: Boolean) {
        viewModelScope.launch { repository.moveChapter(subjectId, chapter.id, moveUp) }
    }

    fun archiveChapter(chapter: ChapterEntity) {
        viewModelScope.launch {
            repository.updateChapter(chapter.copy(isArchived = true))
            if (navFilterState.value.selectedChapterId == chapter.id) {
                navFilterState.update { it.copy(selectedChapterId = null) }
            }
        }
    }

    fun deleteChapter(chapter: ChapterEntity) {
        viewModelScope.launch {
            repository.deleteChapter(chapter)
            if (navFilterState.value.selectedChapterId == chapter.id) {
                navFilterState.update { it.copy(selectedChapterId = null) }
            }
        }
    }

    fun addCustomTopic(chapterId: Long, title: String, parentTopicId: Long? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addCustomTopic(chapterId, title.trim(), parentTopicId)
        }
    }

    fun renameCustomTopic(topic: TopicEntity, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch { repository.updateTopic(topic.copy(title = newTitle.trim())) }
    }

    fun moveTopic(chapterId: Long, topic: TopicEntity, moveUp: Boolean) {
        viewModelScope.launch { repository.moveTopic(chapterId, topic.id, moveUp) }
    }

    fun archiveTopic(topic: TopicEntity) {
        viewModelScope.launch { repository.updateTopic(topic.copy(isArchived = true)) }
    }

    fun deleteTopic(topic: TopicEntity) {
        viewModelScope.launch { repository.deleteTopic(topic) }
    }

    fun restoreOfficialSyllabus() {
        viewModelScope.launch {
            navFilterState.update { it.copy(isRestoring = true) }
            val restored = repository.restoreOfficialSyllabus(journeyId)
            navFilterState.update {
                it.copy(
                    isRestoring = false,
                    statusBanner = if (restored) {
                        "Official syllabus restored. Existing progress preserved."
                    } else {
                        "No bundled official dataset available for this track yet."
                    }
                )
            }
        }
    }

    private data class RawSyllabusBundle(
        val journey: AcademicJourneyEntity?,
        val subjects: List<SubjectEntity>,
        val chapters: List<ChapterEntity>,
        val topics: List<TopicEntity>
    )

    companion object {
        fun factory(repository: EduTrackRepository, journeyId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SyllabusViewModel(repository, journeyId) as T
                }
            }
    }
}
