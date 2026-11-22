package com.edutrack.app.data.syllabus

import android.content.Context
import com.edutrack.app.data.local.AcademicJourneyEntity
import com.edutrack.app.data.local.ChapterEntity
import com.edutrack.app.data.local.EduTrackDao
import com.edutrack.app.data.local.SubjectEntity
import com.edutrack.app.data.local.TopicEntity
import com.edutrack.app.data.local.TopicStatus
import kotlinx.serialization.json.Json

class SyllabusCatalogLoader(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cachedCatalog: SyllabusCatalogJson? = null

    private fun loadCatalog(): SyllabusCatalogJson? {
        cachedCatalog?.let { return it }
        return runCatching {
            val raw = context.assets.open("syllabus_catalog.json")
                .bufferedReader()
                .use { it.readText() }
            json.decodeFromString<SyllabusCatalogJson>(raw).also { cachedCatalog = it }
        }.getOrNull()
    }

    fun currentCatalogVersion(): String = loadCatalog()?.catalogVersion ?: "2026.2"

    fun findDataset(datasetKey: String): DatasetEntryJson? {
        val catalog = loadCatalog() ?: return null
        val normalized = datasetKey.lowercase()
        return catalog.datasets.firstOrNull { it.datasetId == normalized }
            ?: catalog.datasets
                .filter { ds ->
                    ds.matchTokens.isNotEmpty() && ds.matchTokens.all { token ->
                        normalized.contains(token.lowercase())
                    }
                }
                .maxByOrNull { it.matchTokens.size * 100 + it.matchTokens.sumOf { t -> t.length } }
    }

    /**
     * Populates or upgrades the official syllabus structure for a journey.
     * Never overwrites existing official topic progress or deletes custom items.
     */
    suspend fun syncOfficialSyllabusForJourney(
        dao: EduTrackDao,
        journey: AcademicJourneyEntity
    ): Boolean {
        val catalog = loadCatalog() ?: return false
        val dataset = findDataset(journey.syllabusDatasetId)

        if (dataset == null) {
            dao.updateJourney(
                journey.copy(
                    syllabusDatasetVersion = catalog.catalogVersion,
                    syllabusSourceRef = "Official dataset not yet bundled for this track",
                    syllabusVerifiedDate = catalog.verificationDate,
                    isDatasetComplete = false
                )
            )
            return false
        }

        val existingSubjects = dao.getSubjectsForJourneySync(journey.id)
        val subjectsByKey = existingSubjects.mapNotNull { sub ->
            sub.officialKey?.let { it to sub }
        }.toMap()

        dataset.subjectKeys.forEachIndexed { subIndex, bankKey ->
            val bank = catalog.subjectBanks[bankKey] ?: return@forEachIndexed
            val existingSub = subjectsByKey[bank.key]
            val subjectId = if (existingSub != null) {
                dao.updateSubject(
                    existingSub.copy(
                        name = bank.name,
                        orderIndex = subIndex,
                        isArchived = false
                    )
                )
                existingSub.id
            } else {
                dao.insertSubject(
                    SubjectEntity(
                        journeyId = journey.id,
                        officialKey = bank.key,
                        name = bank.name,
                        orderIndex = subIndex,
                        isCustom = false,
                        isArchived = false
                    )
                )
            }

            val existingChapters = dao.getChaptersForSubjectSync(subjectId)
            val chaptersByKey = existingChapters.mapNotNull { ch ->
                ch.officialKey?.let { it to ch }
            }.toMap()

            bank.chapters.forEachIndexed { chIndex, chJson ->
                val existingCh = chaptersByKey[chJson.key]
                val chapterId = if (existingCh != null) {
                    dao.updateChapter(
                        existingCh.copy(
                            title = chJson.title,
                            unitName = chJson.unit,
                            orderIndex = chIndex,
                            isArchived = false
                        )
                    )
                    existingCh.id
                } else {
                    dao.insertChapter(
                        ChapterEntity(
                            subjectId = subjectId,
                            officialKey = chJson.key,
                            title = chJson.title,
                            unitName = chJson.unit,
                            orderIndex = chIndex,
                            isCustom = false,
                            isArchived = false
                        )
                    )
                }

                val existingTopics = dao.getTopicsForChapterSync(chapterId)
                val topicsByKey = existingTopics.mapNotNull { t ->
                    t.officialKey?.let { it to t }
                }.toMap()

                chJson.topics.forEachIndexed { tIndex, topicTitle ->
                    val topicOfficialKey = "${chJson.key}_t${tIndex + 1}"
                    val existingTopic = topicsByKey[topicOfficialKey]
                    if (existingTopic != null) {
                        dao.updateTopic(
                            existingTopic.copy(
                                title = topicTitle,
                                orderIndex = tIndex,
                                isArchived = false
                            )
                        )
                    } else {
                        dao.insertTopic(
                            TopicEntity(
                                chapterId = chapterId,
                                parentTopicId = null,
                                officialKey = topicOfficialKey,
                                title = topicTitle,
                                status = TopicStatus.NOT_STARTED,
                                orderIndex = tIndex,
                                isCustom = false,
                                isArchived = false
                            )
                        )
                    }
                }
            }
        }

        dao.updateJourney(
            journey.copy(
                syllabusDatasetVersion = dataset.datasetVersion,
                syllabusSourceRef = dataset.officialSourceRef,
                syllabusVerifiedDate = dataset.verificationDate,
                isDatasetComplete = dataset.isComplete
            )
        )
        return true
    }
}
