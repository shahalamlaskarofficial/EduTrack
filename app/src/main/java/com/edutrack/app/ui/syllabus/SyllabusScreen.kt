package com.edutrack.app.ui.syllabus

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edutrack.app.data.local.ChapterEntity
import com.edutrack.app.data.local.SubjectEntity
import com.edutrack.app.data.local.TopicEntity
import com.edutrack.app.data.local.TopicStatus
import com.edutrack.app.data.syllabus.ChapterWithProgress
import com.edutrack.app.data.syllabus.SubjectWithProgress
import com.edutrack.app.data.syllabus.TopicSearchResult
import com.edutrack.app.ui.theme.EduBlue
import com.edutrack.app.ui.theme.SuccessGreen
import com.edutrack.app.ui.theme.WarningAmber

@Composable
fun SyllabusScreen(viewModel: SyllabusViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var addTopicParent by remember { mutableStateOf<Pair<Long, Long?>?>(null) } // chapterId to parentTopicId
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var editingChapter by remember { mutableStateOf<ChapterEntity?>(null) }
    var editingTopic by remember { mutableStateOf<TopicEntity?>(null) }

    BackHandler(enabled = state.selectedSubject != null || state.selectedChapter != null) {
        viewModel.navigateUp()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Optional status notification banner
        state.statusBanner?.let { banner ->
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = banner,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::clearBanner, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        when {
            state.selectedChapter != null && state.selectedSubject != null -> {
                TopicListView(
                    subjectName = state.selectedSubject!!.subject.name,
                    chapter = state.selectedChapter!!,
                    topics = state.topicsForSelectedChapter,
                    activeStatusFilter = state.topicStatusFilter,
                    onBack = { viewModel.navigateUp() },
                    onSelectFilter = viewModel::setTopicStatusFilter,
                    onChangeStatus = viewModel::updateTopicStatus,
                    onAddTopic = { addTopicParent = state.selectedChapter!!.chapter.id to null },
                    onAddSubtopic = { parentId ->
                        addTopicParent = state.selectedChapter!!.chapter.id to parentId
                    },
                    onEditTopic = { editingTopic = it },
                    onMoveTopic = { topic, up ->
                        viewModel.moveTopic(state.selectedChapter!!.chapter.id, topic, up)
                    },
                    onArchiveTopic = viewModel::archiveTopic,
                    onDeleteTopic = viewModel::deleteTopic
                )
            }

            state.selectedSubject != null -> {
                ChapterListView(
                    subject = state.selectedSubject!!,
                    chapters = state.chaptersForSelectedSubject,
                    sortMode = state.sortMode,
                    chapterFilter = state.chapterFilter,
                    onBack = { viewModel.navigateUp() },
                    onOpenChapter = { chId ->
                        viewModel.openChapter(state.selectedSubject!!.subject.id, chId)
                    },
                    onCycleSort = viewModel::cycleSortMode,
                    onSelectFilter = viewModel::setChapterFilter,
                    onAddChapter = { showAddChapterDialog = true },
                    onEditChapter = { editingChapter = it },
                    onMoveChapter = { ch, up ->
                        viewModel.moveChapter(state.selectedSubject!!.subject.id, ch, up)
                    },
                    onArchiveChapter = viewModel::archiveChapter,
                    onDeleteChapter = viewModel::deleteChapter
                )
            }

            else -> {
                SubjectsOverviewView(
                    state = state,
                    onSearchChange = viewModel::updateSearchQuery,
                    onCycleSort = viewModel::cycleSortMode,
                    onOpenSubject = viewModel::openSubject,
                    onOpenSearchResult = { result ->
                        viewModel.updateSearchQuery("")
                        viewModel.openChapter(result.subjectId, result.chapterId)
                    },
                    onAddSubject = { showAddSubjectDialog = true },
                    onRestoreOfficial = { showRestoreDialog = true },
                    onReportMissing = { showReportDialog = true },
                    onEditSubject = { editingSubject = it },
                    onMoveSubject = viewModel::moveSubject,
                    onArchiveSubject = viewModel::archiveSubject,
                    onDeleteSubject = viewModel::deleteSubject
                )
            }
        }
    }

    // Add Custom Subject Dialog
    if (showAddSubjectDialog) {
        SimpleInputDialog(
            title = "Add Missing Subject",
            hint = "Subject name (e.g., Computer Science)",
            confirmLabel = "Add Custom Subject",
            onDismiss = { showAddSubjectDialog = false },
            onConfirm = { name ->
                viewModel.addCustomSubject(name)
                showAddSubjectDialog = false
            }
        )
    }

    // Add Custom Chapter Dialog
    if (showAddChapterDialog && state.selectedSubject != null) {
        ChapterInputDialog(
            title = "Add Missing Chapter",
            initialTitle = "",
            initialUnit = "",
            parentLabel = "Subject: ${state.selectedSubject!!.subject.name}",
            confirmLabel = "Add Custom Chapter",
            onDismiss = { showAddChapterDialog = false },
            onConfirm = { title, unit ->
                viewModel.addCustomChapter(state.selectedSubject!!.subject.id, title, unit)
                showAddChapterDialog = false
            }
        )
    }

    // Add Custom Topic / Subtopic Dialog
    addTopicParent?.let { (chapterId, parentTopicId) ->
        SimpleInputDialog(
            title = if (parentTopicId == null) "Add Missing Topic" else "Add Missing Subtopic",
            hint = if (parentTopicId == null) "Topic title" else "Subtopic title",
            confirmLabel = "Add Custom",
            onDismiss = { addTopicParent = null },
            onConfirm = { title ->
                viewModel.addCustomTopic(chapterId, title, parentTopicId)
                addTopicParent = null
            }
        )
    }

    // Edit Custom Subject Dialog
    editingSubject?.let { sub ->
        SimpleInputDialog(
            title = "Rename Custom Subject",
            hint = "Subject name",
            initialValue = sub.name,
            confirmLabel = "Save",
            onDismiss = { editingSubject = null },
            onConfirm = { newName ->
                viewModel.renameCustomSubject(sub, newName)
                editingSubject = null
            }
        )
    }

    // Edit Custom Chapter Dialog
    editingChapter?.let { ch ->
        ChapterInputDialog(
            title = "Edit Custom Chapter",
            initialTitle = ch.title,
            initialUnit = ch.unitName,
            parentLabel = null,
            confirmLabel = "Save",
            onDismiss = { editingChapter = null },
            onConfirm = { newTitle, newUnit ->
                viewModel.renameCustomChapter(ch, newTitle, newUnit)
                editingChapter = null
            }
        )
    }

    // Edit Custom Topic Dialog
    editingTopic?.let { topic ->
        SimpleInputDialog(
            title = "Rename Custom Topic",
            hint = "Topic title",
            initialValue = topic.title,
            confirmLabel = "Save",
            onDismiss = { editingTopic = null },
            onConfirm = { newTitle ->
                viewModel.renameCustomTopic(topic, newTitle)
                editingTopic = null
            }
        )
    }

    // Restore Official Syllabus Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Official Syllabus?") },
            text = {
                Text(
                    "This restores any archived or missing official subjects, chapters, and topics for your active journey without deleting your completed topic progress or custom items."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreOfficialSyllabus()
                        showRestoreDialog = false
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Report Missing Syllabus Consent Dialog
    if (showReportDialog) {
        var missingDetails by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Missing Syllabus Content") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Nothing is sent automatically. Tapping \"Open Email\" opens your device's email app with a draft addressed to the EduTrack developer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = missingDetails,
                        onValueChange = { missingDetails = it },
                        label = { Text("What syllabus content is missing?") },
                        placeholder = { Text("e.g., Board, Class, Subject or Chapter name") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val journeyTitle = state.journey?.title ?: "Unknown Track"
                        val body = buildString {
                            appendLine("EduTrack Missing Syllabus Report")
                            appendLine("Journey: $journeyTitle")
                            appendLine("Dataset ID: ${state.journey?.syllabusDatasetId ?: "-"}")
                            appendLine()
                            appendLine("Details:")
                            appendLine(missingDetails.ifBlank { "Please add official syllabus support for this track." })
                        }
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:shahalamlaskar.official@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "EduTrack Syllabus Report: $journeyTitle")
                            putExtra(Intent.EXTRA_TEXT, body)
                        }
                        runCatching { context.startActivity(intent) }
                        showReportDialog = false
                    }
                ) {
                    Text("Open Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SubjectsOverviewView(
    state: SyllabusUiState,
    onSearchChange: (String) -> Unit,
    onCycleSort: () -> Unit,
    onOpenSubject: (Long) -> Unit,
    onOpenSearchResult: (TopicSearchResult) -> Unit,
    onAddSubject: () -> Unit,
    onRestoreOfficial: () -> Unit,
    onReportMissing: () -> Unit,
    onEditSubject: (SubjectEntity) -> Unit,
    onMoveSubject: (SubjectEntity, Boolean) -> Unit,
    onArchiveSubject: (SubjectEntity) -> Unit,
    onDeleteSubject: (SubjectEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp)
    ) {
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search subjects, chapters, or topics...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Live Search Results
        if (state.searchQuery.trim().length >= 2) {
            if (state.searchResults.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = "No topics matched \"${state.searchQuery.trim()}\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "Search Results (${state.searchResults.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(state.searchResults, key = { it.topic.id }) { res ->
                    SearchResultCard(result = res, onClick = { onOpenSearchResult(res) })
                }
            }
            return@LazyColumn
        }

        // Action / Sort strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onCycleSort,
                    label = { Text(state.sortMode.label) },
                    leadingIcon = { Icon(Icons.Rounded.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = onAddSubject,
                    label = { Text("Add Subject") },
                    leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = onRestoreOfficial,
                    label = { Text("Restore Official") },
                    leadingIcon = { Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = onReportMissing,
                    label = { Text("Report Missing") },
                    leadingIcon = { Icon(Icons.Rounded.Flag, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        // Incomplete Dataset Honest Warning
        if (state.journey != null && !state.journey.isDatasetComplete) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Official Dataset Not Yet Bundled",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarningAmber
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "EduTrack never invents unverified syllabus data. You can add your subjects and chapters with \"Add Subject\" or request official support for this track.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onAddSubject, shape = RoundedCornerShape(10.dp)) {
                                Text("Add Subject")
                            }
                            OutlinedButton(onClick = onReportMissing, shape = RoundedCornerShape(10.dp)) {
                                Text("Report Track")
                            }
                        }
                    }
                }
            }
        }

        // Recently Accessed Topics
        if (state.recentlyAccessedTopics.isNotEmpty()) {
            item {
                Text("Recently Studied", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.recentlyAccessedTopics, key = { it.topic.id }) { item ->
                        Card(
                            modifier = Modifier
                                .width(210.dp)
                                .clickable { onOpenSearchResult(item) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                StatusPillBadge(status = item.topic.status)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.topic.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${item.subjectName} • ${item.chapterTitle}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Subjects Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subjects (${state.subjects.size})", style = MaterialTheme.typography.titleLarge)
                state.journey?.let { j ->
                    if (j.isDatasetComplete) {
                        Text(
                            text = "Official v${j.syllabusDatasetVersion}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SuccessGreen
                        )
                    }
                }
            }
        }

        if (state.subjects.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("No subjects loaded yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap \"Add Subject\" above to create custom subjects or switch to a supported official track.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(state.subjects, key = { it.subject.id }) { item ->
                SubjectCard(
                    item = item,
                    onClick = { onOpenSubject(item.subject.id) },
                    onEdit = { onEditSubject(item.subject) },
                    onMoveUp = { onMoveSubject(item.subject, true) },
                    onMoveDown = { onMoveSubject(item.subject, false) },
                    onArchive = { onArchiveSubject(item.subject) },
                    onDelete = { onDeleteSubject(item.subject) }
                )
            }
        }
    }
}

@Composable
private fun SubjectCard(
    item: SubjectWithProgress,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (item.subject.isCustom) WarningAmber.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.subject.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.subject.isCustom) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CustomBadge()
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.progressPercent}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (item.progressPercent == 100) SuccessGreen else MaterialTheme.colorScheme.primary
                    )
                    if (item.subject.isCustom) {
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Options")
                            }
                            CustomItemMenu(
                                expanded = menuExpanded,
                                onDismiss = { menuExpanded = false },
                                onEdit = onEdit,
                                onMoveUp = onMoveUp,
                                onMoveDown = onMoveDown,
                                onArchive = onArchive,
                                onDelete = onDelete
                            )
                        }
                    } else {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { item.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (item.progressPercent == 100) SuccessGreen else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.completedChapters}/${item.totalChapters} chapters completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${item.remainingChapters} remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChapterListView(
    subject: SubjectWithProgress,
    chapters: List<ChapterWithProgress>,
    sortMode: SyllabusSortMode,
    chapterFilter: ChapterFilterMode,
    onBack: () -> Unit,
    onOpenChapter: (Long) -> Unit,
    onCycleSort: () -> Unit,
    onSelectFilter: (ChapterFilterMode) -> Unit,
    onAddChapter: () -> Unit,
    onEditChapter: (ChapterEntity) -> Unit,
    onMoveChapter: (ChapterEntity, Boolean) -> Unit,
    onArchiveChapter: (ChapterEntity) -> Unit,
    onDeleteChapter: (ChapterEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Subjects")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.subject.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${subject.progressPercent}% • ${subject.completedChapters}/${subject.totalChapters} chapters completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onCycleSort) {
                Icon(Icons.Rounded.Sort, contentDescription = sortMode.label)
            }
            IconButton(onClick = onAddChapter) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Missing Chapter")
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChapterFilterMode.entries.forEach { mode ->
                FilterChip(
                    selected = chapterFilter == mode,
                    onClick = { onSelectFilter(mode) },
                    label = { Text(mode.label) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            if (chapters.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("No chapters match this filter", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(onClick = onAddChapter) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Missing Chapter")
                            }
                        }
                    }
                }
            } else {
                items(chapters, key = { it.chapter.id }) { item ->
                    ChapterCard(
                        item = item,
                        onClick = { onOpenChapter(item.chapter.id) },
                        onEdit = { onEditChapter(item.chapter) },
                        onMoveUp = { onMoveChapter(item.chapter, true) },
                        onMoveDown = { onMoveChapter(item.chapter, false) },
                        onArchive = { onArchiveChapter(item.chapter) },
                        onDelete = { onDeleteChapter(item.chapter) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterCard(
    item: ChapterWithProgress,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            when {
                item.chapter.isCustom -> WarningAmber.copy(alpha = 0.6f)
                item.isCompleted -> SuccessGreen.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.chapter.unitName.isNotBlank()) {
                            Text(
                                text = item.chapter.unitName.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (item.chapter.isCustom) {
                            CustomBadge()
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.chapter.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.progressPercent}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (item.isCompleted) SuccessGreen else MaterialTheme.colorScheme.primary
                    )
                    if (item.chapter.isCustom) {
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Options")
                            }
                            CustomItemMenu(
                                expanded = menuExpanded,
                                onDismiss = { menuExpanded = false },
                                onEdit = onEdit,
                                onMoveUp = onMoveUp,
                                onMoveDown = onMoveDown,
                                onArchive = onArchive,
                                onDelete = onDelete
                            )
                        }
                    } else {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { item.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (item.isCompleted) SuccessGreen else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.completedTopics}/${item.totalTopics} topics completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.revisionRequiredTopics > 0) {
                    Text(
                        text = "${item.revisionRequiredTopics} need revision",
                        style = MaterialTheme.typography.labelMedium,
                        color = WarningAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicListView(
    subjectName: String,
    chapter: ChapterWithProgress,
    topics: List<TopicEntity>,
    activeStatusFilter: TopicStatus?,
    onBack: () -> Unit,
    onSelectFilter: (TopicStatus?) -> Unit,
    onChangeStatus: (TopicEntity, TopicStatus) -> Unit,
    onAddTopic: () -> Unit,
    onAddSubtopic: (Long) -> Unit,
    onEditTopic: (TopicEntity) -> Unit,
    onMoveTopic: (TopicEntity, Boolean) -> Unit,
    onArchiveTopic: (TopicEntity) -> Unit,
    onDeleteTopic: (TopicEntity) -> Unit
) {
    val rootTopics = topics.filter { it.parentTopicId == null }
    val subtopicsByParent = topics.filter { it.parentTopicId != null }.groupBy { it.parentTopicId }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Chapters")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = chapter.chapter.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onAddTopic) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Missing Topic")
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${chapter.completedTopics} of ${chapter.totalTopics} topics completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${chapter.progressPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (chapter.isCompleted) SuccessGreen else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { chapter.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (chapter.isCompleted) SuccessGreen else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // Status filter strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeStatusFilter == null,
                onClick = { onSelectFilter(null) },
                label = { Text("All") }
            )
            TopicStatus.entries.forEach { st ->
                FilterChip(
                    selected = activeStatusFilter == st,
                    onClick = { onSelectFilter(if (activeStatusFilter == st) null else st) },
                    label = { Text(statusShortLabel(st)) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp)
        ) {
            if (topics.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("No topics here yet", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(onClick = onAddTopic) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Missing Topic")
                            }
                        }
                    }
                }
            } else {
                val orderedDisplay = if (activeStatusFilter == null) rootTopics else topics
                items(orderedDisplay, key = { it.id }) { topic ->
                    TopicItemCard(
                        topic = topic,
                        isSubtopic = topic.parentTopicId != null,
                        onSelectStatus = { newStatus -> onChangeStatus(topic, newStatus) },
                        onAddSubtopic = if (topic.parentTopicId == null) {
                            { onAddSubtopic(topic.id) }
                        } else null,
                        onEdit = { onEditTopic(topic) },
                        onMoveUp = { onMoveTopic(topic, true) },
                        onMoveDown = { onMoveTopic(topic, false) },
                        onArchive = { onArchiveTopic(topic) },
                        onDelete = { onDeleteTopic(topic) }
                    )

                    if (activeStatusFilter == null) {
                        val children = subtopicsByParent[topic.id].orEmpty()
                        children.forEach { sub ->
                            Spacer(modifier = Modifier.height(6.dp))
                            TopicItemCard(
                                topic = sub,
                                isSubtopic = true,
                                onSelectStatus = { newStatus -> onChangeStatus(sub, newStatus) },
                                onAddSubtopic = null,
                                onEdit = { onEditTopic(sub) },
                                onMoveUp = { onMoveTopic(sub, true) },
                                onMoveDown = { onMoveTopic(sub, false) },
                                onArchive = { onArchiveTopic(sub) },
                                onDelete = { onDeleteTopic(sub) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicItemCard(
    topic: TopicEntity,
    isSubtopic: Boolean,
    onSelectStatus: (TopicStatus) -> Unit,
    onAddSubtopic: (() -> Unit)?,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isSubtopic) 18.dp else 0.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            when {
                topic.isCustom -> WarningAmber.copy(alpha = 0.55f)
                topic.status == TopicStatus.COMPLETED -> SuccessGreen.copy(alpha = 0.45f)
                topic.status == TopicStatus.REVISION_REQUIRED -> WarningAmber.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSubtopic) {
                            Text(
                                text = "SUBTOPIC",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (topic.isCustom) {
                            CustomBadge()
                        }
                    }
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onAddSubtopic != null) {
                        IconButton(onClick = onAddSubtopic, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Add Subtopic",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (topic.isCustom) {
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Options")
                            }
                            CustomItemMenu(
                                expanded = menuExpanded,
                                onDismiss = { menuExpanded = false },
                                onEdit = onEdit,
                                onMoveUp = onMoveUp,
                                onMoveDown = onMoveDown,
                                onArchive = onArchive,
                                onDelete = onDelete
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4-State One-Tap Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TopicStatus.entries.forEach { st ->
                    val selected = topic.status == st
                    val accent = statusColor(st)
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (selected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 0.dp,
                            color = if (selected) accent else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectStatus(st) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (st) {
                                    TopicStatus.NOT_STARTED -> Icons.Rounded.RadioButtonUnchecked
                                    TopicStatus.IN_PROGRESS -> Icons.Rounded.PlayCircle
                                    TopicStatus.COMPLETED -> Icons.Rounded.CheckCircle
                                    TopicStatus.REVISION_REQUIRED -> Icons.Rounded.Refresh
                                },
                                contentDescription = null,
                                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = statusCompactLabel(st),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(result: TopicSearchResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(result.topic.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${result.subjectName} • ${result.chapterTitle}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusPillBadge(status = result.topic.status)
        }
    }
}

@Composable
private fun CustomBadge() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = WarningAmber.copy(alpha = 0.16f)
    ) {
        Text(
            text = "Custom",
            style = MaterialTheme.typography.labelMedium,
            color = WarningAmber,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StatusPillBadge(status: TopicStatus) {
    val color = statusColor(status)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.16f)
    ) {
        Text(
            text = statusShortLabel(status),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun CustomItemMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Rename") },
            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
            onClick = { onDismiss(); onEdit() }
        )
        DropdownMenuItem(
            text = { Text("Move Up") },
            leadingIcon = { Icon(Icons.Rounded.ArrowUpward, contentDescription = null) },
            onClick = { onDismiss(); onMoveUp() }
        )
        DropdownMenuItem(
            text = { Text("Move Down") },
            leadingIcon = { Icon(Icons.Rounded.ArrowDownward, contentDescription = null) },
            onClick = { onDismiss(); onMoveDown() }
        )
        DropdownMenuItem(
            text = { Text("Archive") },
            leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
            onClick = { onDismiss(); onArchive() }
        )
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            leadingIcon = {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            onClick = { onDismiss(); onDelete() }
        )
    }
}

@Composable
private fun SimpleInputDialog(
    title: String,
    hint: String,
    initialValue: String = "",
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(hint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { onConfirm(text) }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ChapterInputDialog(
    title: String,
    initialTitle: String,
    initialUnit: String,
    parentLabel: String?,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var chTitle by remember(initialTitle) { mutableStateOf(initialTitle) }
    var chUnit by remember(initialUnit) { mutableStateOf(initialUnit) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!parentLabel.isNullOrBlank()) {
                    Text(
                        text = parentLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                OutlinedTextField(
                    value = chTitle,
                    onValueChange = { chTitle = it },
                    label = { Text("Chapter Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = chUnit,
                    onValueChange = { chUnit = it },
                    label = { Text("Unit Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = chTitle.isNotBlank(),
                onClick = { onConfirm(chTitle, chUnit) }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun statusColor(status: TopicStatus): Color = when (status) {
    TopicStatus.NOT_STARTED -> Color(0xFF94A3B8)
    TopicStatus.IN_PROGRESS -> EduBlue
    TopicStatus.COMPLETED -> SuccessGreen
    TopicStatus.REVISION_REQUIRED -> WarningAmber
}

private fun statusShortLabel(status: TopicStatus): String = when (status) {
    TopicStatus.NOT_STARTED -> "Not Started"
    TopicStatus.IN_PROGRESS -> "In Progress"
    TopicStatus.COMPLETED -> "Completed"
    TopicStatus.REVISION_REQUIRED -> "Revision"
}

private fun statusCompactLabel(status: TopicStatus): String = when (status) {
    TopicStatus.NOT_STARTED -> "Todo"
    TopicStatus.IN_PROGRESS -> "Active"
    TopicStatus.COMPLETED -> "Done"
    TopicStatus.REVISION_REQUIRED -> "Revise"
}
