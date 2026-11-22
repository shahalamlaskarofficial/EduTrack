package com.edutrack.app.data.syllabus

import com.edutrack.app.data.local.ChapterEntity
import com.edutrack.app.data.local.SubjectEntity
import com.edutrack.app.data.local.TopicEntity
import kotlinx.serialization.Serializable

@Serializable
data class SyllabusCatalogJson(
    val catalogVersion: String,
    val verificationDate: String,
    val subjectBanks: Map<String, SubjectBankJson>,
    val datasets: List<DatasetEntryJson>
)

@Serializable
data class SubjectBankJson(
    val key: String,
    val name: String,
    val chapters: List<ChapterJson>
)

@Serializable
data class ChapterJson(
    val key: String,
    val unit: String = "",
    val title: String,
    val topics: List<String>
)

@Serializable
data class DatasetEntryJson(
    val datasetId: String,
    val matchPrefix: String,
    val datasetVersion: String,
    val officialSourceRef: String,
    val verificationDate: String,
    val isComplete: Boolean,
    val subjectKeys: List<String>
)

data class ChapterWithProgress(
    val chapter: ChapterEntity,
    val totalTopics: Int,
    val completedTopics: Int,
    val inProgressTopics: Int,
    val revisionRequiredTopics: Int
) {
    val progressFraction: Float
        get() = if (totalTopics > 0) completedTopics.toFloat() / totalTopics else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()

    val isCompleted: Boolean
        get() = totalTopics > 0 && completedTopics == totalTopics
}

data class SubjectWithProgress(
    val subject: SubjectEntity,
    val totalChapters: Int,
    val completedChapters: Int,
    val totalTopics: Int,
    val completedTopics: Int
) {
    val remainingChapters: Int
        get() = (totalChapters - completedChapters).coerceAtLeast(0)

    val progressFraction: Float
        get() = if (totalTopics > 0) completedTopics.toFloat() / totalTopics else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()
}

data class TopicSearchResult(
    val topic: TopicEntity,
    val chapterTitle: String,
    val subjectName: String,
    val subjectId: Long,
    val chapterId: Long
)
