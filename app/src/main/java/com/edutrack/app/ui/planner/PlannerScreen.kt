package com.edutrack.app.ui.planner

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edutrack.app.notifications.ReminderScheduler
import com.edutrack.app.ui.theme.EduBlue
import com.edutrack.app.ui.theme.SuccessGreen
import com.edutrack.app.ui.theme.WarningAmber
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(viewModel: PlannerViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddRevisionDialog by remember { mutableStateOf(false) }
    var preselectedTopicForRevision by remember { mutableStateOf<TopicOption?>(null) }
    var showAddCountdownDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* User decision respected gracefully */ }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (state.selectedSection) {
                        PlannerSection.TASKS -> showAddTaskDialog = true
                        PlannerSection.REVISION -> {
                            preselectedTopicForRevision = null
                            showAddRevisionDialog = true
                        }
                        PlannerSection.EXAMS -> showAddCountdownDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Segmented Section Bar: Study Planner | Revision | Countdowns
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlannerSection.entries.forEach { sec ->
                    val selected = state.selectedSection == sec
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.selectSection(sec) }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sec.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            when (state.selectedSection) {
                PlannerSection.TASKS -> TasksSectionView(
                    state = state,
                    onSelectDate = viewModel::selectDate,
                    onToggleCalendarMode = viewModel::toggleCalendarMode,
                    onTogglePrevious = viewModel::toggleShowPreviousTasks,
                    onToggleTask = viewModel::toggleTaskCompleted,
                    onDeleteTask = { item ->
                        ReminderScheduler.cancelReminder(context, "task_${item.task.id}")
                        viewModel.deleteTask(item.task)
                    }
                )

                PlannerSection.REVISION -> RevisionSectionView(
                    state = state,
                    onCompleteRevision = { item -> viewModel.completeRevision(item.revision) },
                    onDeleteRevision = { item -> viewModel.deleteRevision(item.revision) },
                    onScheduleForTopic = { opt ->
                        preselectedTopicForRevision = opt
                        showAddRevisionDialog = true
                    }
                )

                PlannerSection.EXAMS -> CountdownSectionView(
                    countdowns = state.countdowns,
                    onAddClick = { showAddCountdownDialog = true },
                    onDeleteCountdown = { cd -> viewModel.deleteExamCountdown(cd.entity) }
                )
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            initialEpochDay = state.selectedEpochDay,
            topicOptions = state.allTopicsForLinking,
            onDismiss = { showAddTaskDialog = false },
            onRequestNotificationPermission = ::ensureNotificationPermission,
            onSave = { title, notes, topicId, epochDay, isWeekly, enableReminder ->
                val reminderMillis = if (enableReminder) {
                    val targetDate = LocalDate.ofEpochDay(epochDay)
                    val trigger = LocalDateTime.of(targetDate, LocalTime.of(9, 0))
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    if (trigger > System.currentTimeMillis()) trigger else System.currentTimeMillis() + 60_000L
                } else null

                viewModel.addStudyTask(
                    title = title,
                    notes = notes,
                    linkedTopicId = topicId,
                    targetEpochDay = epochDay,
                    isWeeklyGoal = isWeekly,
                    reminderMillis = reminderMillis
                ) { taskId ->
                    if (reminderMillis != null) {
                        ReminderScheduler.scheduleOneTimeReminder(
                            context = context,
                            uniqueKey = "task_$taskId",
                            title = "Study Mission Reminder",
                            body = title,
                            triggerAtMillis = reminderMillis,
                            notificationId = (3000 + taskId).toInt()
                        )
                    }
                }
                showAddTaskDialog = false
            }
        )
    }

    if (showAddRevisionDialog) {
        ScheduleRevisionDialog(
            preselected = preselectedTopicForRevision,
            availableTopics = if (state.completedTopicsForRevision.isNotEmpty()) {
                state.completedTopicsForRevision
            } else {
                state.allTopicsForLinking
            },
            onDismiss = { showAddRevisionDialog = false },
            onSchedule = { topicOpt, intervals ->
                ensureNotificationPermission()
                viewModel.scheduleRevisionIntervals(topicOpt.topic.id, intervals) {
                    val firstDelayDays = intervals.minOrNull() ?: 1
                    val triggerAt = LocalDateTime.of(LocalDate.now().plusDays(firstDelayDays.toLong()), LocalTime.of(9, 0))
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    ReminderScheduler.scheduleOneTimeReminder(
                        context = context,
                        uniqueKey = "rev_${topicOpt.topic.id}_$firstDelayDays",
                        title = "Revision Due: ${topicOpt.topic.title}",
                        body = "${topicOpt.subjectName} • ${topicOpt.chapterTitle}",
                        triggerAtMillis = triggerAt,
                        notificationId = (5000 + topicOpt.topic.id).toInt()
                    )
                }
                showAddRevisionDialog = false
            }
        )
    }

    if (showAddCountdownDialog) {
        AddCountdownDialog(
            onDismiss = { showAddCountdownDialog = false },
            onSave = { name, epochDay ->
                viewModel.addExamCountdown(name, epochDay)
                showAddCountdownDialog = false
            }
        )
    }
}

@Composable
private fun TasksSectionView(
    state: PlannerUiState,
    onSelectDate: (Long) -> Unit,
    onToggleCalendarMode: () -> Unit,
    onTogglePrevious: () -> Unit,
    onToggleTask: (com.edutrack.app.data.local.StudyTaskEntity) -> Unit,
    onDeleteTask: (TaskWithTopic) -> Unit
) {
    val today = remember { LocalDate.now() }
    val weekDates = remember { (-2L..11L).map { today.plusDays(it) } }
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd MMM") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp)
    ) {
        // View Toggle Row (Calendar vs List + Jump to Today)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.isCalendarMode,
                        onClick = { if (!state.isCalendarMode) onToggleCalendarMode() },
                        label = { Text("Calendar") },
                        leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = !state.isCalendarMode,
                        onClick = { if (state.isCalendarMode) onToggleCalendarMode() },
                        label = { Text("List View") },
                        leadingIcon = { Icon(Icons.Rounded.Checklist, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                TextButton(onClick = { onSelectDate(today.toEpochDay()) }) {
                    Icon(Icons.Rounded.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Today")
                }
            }
        }

        // Interactive Date Strip in Calendar Mode
        if (state.isCalendarMode) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(weekDates, key = { it.toEpochDay() }) { date ->
                        val epoch = date.toEpochDay()
                        val selected = epoch == state.selectedEpochDay
                        val isToday = date == today
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                when {
                                    selected -> MaterialTheme.colorScheme.primary
                                    isToday -> EduBlue.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            ),
                            modifier = Modifier
                                .width(56.dp)
                                .clickable { onSelectDate(epoch) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${date.dayOfMonth}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daily Study Missions
        val activeDaily = if (state.isCalendarMode) state.dailyTasksForSelectedDate else state.allPendingDailyTasks
        item {
            val selectedDateLabel = LocalDate.ofEpochDay(state.selectedEpochDay).format(dateFmt)
            Text(
                text = if (state.isCalendarMode) "Daily Missions ($selectedDateLabel)" else "Upcoming Daily Missions",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (activeDaily.isEmpty()) {
            item {
                EmptyPlannerCard("No study missions here yet. Tap + to add a daily mission or link a syllabus topic.")
            }
        } else {
            items(activeDaily, key = { "daily_${it.task.id}" }) { item ->
                TaskCard(
                    item = item,
                    showDate = !state.isCalendarMode,
                    onToggle = { onToggleTask(item.task) },
                    onDelete = { onDeleteTask(item) }
                )
            }
        }

        // Weekly Goals
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Weekly Goals (${state.weeklyGoals.size})", style = MaterialTheme.typography.titleMedium)
        }
        if (state.weeklyGoals.isEmpty()) {
            item {
                EmptyPlannerCard("No weekly goals set. Tap + and switch on \"Weekly Goal\" to track larger weekly targets.")
            }
        } else {
            items(state.weeklyGoals, key = { "weekly_${it.task.id}" }) { item ->
                TaskCard(
                    item = item,
                    showDate = true,
                    onToggle = { onToggleTask(item.task) },
                    onDelete = { onDeleteTask(item) }
                )
            }
        }

        // Previous & Completed Tasks
        if (state.previousTasks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Previous & Completed (${state.previousTasks.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onTogglePrevious) {
                        Text(if (state.showPreviousTasks) "Hide" else "Show")
                    }
                }
            }
            if (state.showPreviousTasks) {
                items(state.previousTasks, key = { "prev_${it.task.id}" }) { item ->
                    TaskCard(
                        item = item,
                        showDate = true,
                        onToggle = { onToggleTask(item.task) },
                        onDelete = { onDeleteTask(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    item: TaskWithTopic,
    showDate: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd MMM") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (item.task.isCompleted) SuccessGreen.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.task.isCompleted, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (item.task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (item.task.notes.isNotBlank()) {
                    Text(
                        text = item.task.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showDate) {
                        Text(
                            text = LocalDate.ofEpochDay(item.task.targetEpochDay).format(dateFmt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!item.linkedTopicTitle.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Link,
                                contentDescription = null,
                                tint = EduBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = item.linkedTopicTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = EduBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (item.task.reminderTimeMillis != null && !item.task.isCompleted) {
                        Icon(
                            Icons.Rounded.NotificationsActive,
                            contentDescription = "Reminder Active",
                            tint = WarningAmber,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RevisionSectionView(
    state: PlannerUiState,
    onCompleteRevision: (RevisionItemUi) -> Unit,
    onDeleteRevision: (RevisionItemUi) -> Unit,
    onScheduleForTopic: (TopicOption) -> Unit
) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp)
    ) {
        // Summary Strip
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RevisionStatColumn("${state.dueRevisions.size}", "Due Today", WarningAmber)
                    RevisionStatColumn("${state.upcomingRevisions.size}", "Upcoming", EduBlue)
                    RevisionStatColumn("${state.completedRevisions.size}", "Completed", SuccessGreen)
                }
            }
        }

        // Due Today
        item { Text("Revision Due Today (${state.dueRevisions.size})", style = MaterialTheme.typography.titleMedium) }
        if (state.dueRevisions.isEmpty()) {
            item {
                EmptyPlannerCard("No revisions due today. Tap + to schedule 1 / 3 / 7 / 14 / 30-day spaced repetition for completed topics.")
            }
        } else {
            items(state.dueRevisions, key = { "due_${it.revision.id}" }) { item ->
                RevisionCard(
                    item = item,
                    dateLabel = if (item.daysFromToday < 0) "${-item.daysFromToday}d overdue" else "Due Today",
                    accentColor = WarningAmber,
                    showCompleteButton = true,
                    onComplete = { onCompleteRevision(item) },
                    onDelete = { onDeleteRevision(item) }
                )
            }
        }

        // Topics Needing Additional Practice
        if (state.practiceTopics.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Needs Additional Practice (${state.practiceTopics.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(state.practiceTopics, key = { "prac_${it.topic.id}" }) { opt ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(opt.topic.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${opt.subjectName} • ${opt.chapterTitle}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(
                            onClick = { onScheduleForTopic(opt) },
                            label = { Text("Schedule") },
                            leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }

        // Upcoming Revisions
        if (state.upcomingRevisions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Upcoming Revisions (${state.upcomingRevisions.size})", style = MaterialTheme.typography.titleMedium)
            }
            items(state.upcomingRevisions, key = { "up_${it.revision.id}" }) { item ->
                val dateStr = LocalDate.ofEpochDay(item.revision.dueEpochDay).format(dateFmt)
                RevisionCard(
                    item = item,
                    dateLabel = "In ${item.daysFromToday}d ($dateStr)",
                    accentColor = EduBlue,
                    showCompleteButton = true,
                    onComplete = { onCompleteRevision(item) },
                    onDelete = { onDeleteRevision(item) }
                )
            }
        }

        // Completed Revisions
        if (state.completedRevisions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Completed Revisions (${state.completedRevisions.size})", style = MaterialTheme.typography.titleMedium)
            }
            items(state.completedRevisions, key = { "done_${it.revision.id}" }) { item ->
                RevisionCard(
                    item = item,
                    dateLabel = "Completed (${item.revision.intervalDays}d cycle)",
                    accentColor = SuccessGreen,
                    showCompleteButton = false,
                    onComplete = {},
                    onDelete = { onDeleteRevision(item) }
                )
            }
        }
    }
}

@Composable
private fun RevisionStatColumn(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RevisionCard(
    item: RevisionItemUi,
    dateLabel: String,
    accentColor: Color,
    showCompleteButton: Boolean,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "${item.revision.intervalDays}d Interval",
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(dateLabel, style = MaterialTheme.typography.labelMedium, color = accentColor)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.topicTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${item.subjectName} • ${item.chapterTitle}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showCompleteButton) {
                IconButton(onClick = onComplete) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Complete Revision", tint = SuccessGreen)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete Revision",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CountdownSectionView(
    countdowns: List<CountdownItemUi>,
    onAddClick: () -> Unit,
    onDeleteCountdown: (CountdownItemUi) -> Unit
) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp)
    ) {
        if (countdowns.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("No Exam Countdowns Yet", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Add your Unit Tests, Pre-Boards, Practical Exams, Final Boards, or NEET/JEE dates to track days remaining alongside your live syllabus progress.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onAddClick, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Examination")
                        }
                    }
                }
            }
        } else {
            items(countdowns, key = { it.entity.id }) { cd ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = LocalDate.ofEpochDay(cd.entity.examEpochDay).format(dateFmt),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(cd.entity.examName, style = MaterialTheme.typography.titleLarge)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (cd.daysRemaining <= 14) WarningAmber.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (cd.daysRemaining >= 0) "${cd.daysRemaining}" else "0",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = if (cd.daysRemaining <= 14) WarningAmber else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (cd.daysRemaining >= 0) "days left" else "passed",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (cd.daysRemaining <= 14) WarningAmber else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteCountdown(cd) }) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Delete Countdown",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { cd.syllabusPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (cd.syllabusPercent == 100) SuccessGreen else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Syllabus: ${cd.syllabusPercent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${cd.chaptersRemaining} chapters left",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${cd.revisionsDueCount} rev. due",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (cd.revisionsDueCount > 0) WarningAmber else SuccessGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPlannerCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(
    initialEpochDay: Long,
    topicOptions: List<TopicOption>,
    onDismiss: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSave: (String, String, Long?, Long, Boolean, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedEpochDay by remember { mutableStateOf(initialEpochDay) }
    var isWeeklyGoal by remember { mutableStateOf(false) }
    var enableReminder by remember { mutableStateOf(false) }
    var linkedTopic by remember { mutableStateOf<TopicOption?>(null) }
    var showTopicPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFmt = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Study Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task / Mission Title") },
                    placeholder = { Text("e.g., Solve 30 Electrostatics PYQs") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Target Date: ${LocalDate.ofEpochDay(selectedEpochDay).format(dateFmt)}")
                }

                if (topicOptions.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showTopicPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = linkedTopic?.topic?.title ?: "Link Syllabus Topic (Optional)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mark as Weekly Goal", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isWeeklyGoal, onCheckedChange = { isWeeklyGoal = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Local Reminder (9:00 AM)", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = enableReminder,
                        onCheckedChange = {
                            enableReminder = it
                            if (it) onRequestNotificationPermission()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(title, notes, linkedTopic?.topic?.id, selectedEpochDay, isWeeklyGoal, enableReminder)
                }
            ) { Text("Save Task") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            selectedEpochDay = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .toEpochDay()
                        }
                        showDatePicker = false
                    }
                ) { Text("Set Date") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }

    if (showTopicPicker) {
        TopicPickerDialog(
            topics = topicOptions,
            onDismiss = { showTopicPicker = false },
            onSelect = { opt ->
                linkedTopic = opt
                if (title.isBlank()) title = "Study: ${opt.topic.title}"
                showTopicPicker = false
            }
        )
    }
}

@Composable
private fun ScheduleRevisionDialog(
    preselected: TopicOption?,
    availableTopics: List<TopicOption>,
    onDismiss: () -> Unit,
    onSchedule: (TopicOption, List<Int>) -> Unit
) {
    var selectedTopic by remember(preselected) { mutableStateOf(preselected ?: availableTopics.firstOrNull()) }
    var showTopicPicker by remember { mutableStateOf(false) }
    val selectedIntervals = remember { mutableStateListOf(1, 3, 7) }
    var customIntervalText by remember { mutableStateOf("") }

    val defaultIntervals = listOf(1, 3, 7, 14, 30)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Spaced Revision") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Completing a revision builds mastery without changing your syllabus completion status.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { showTopicPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = selectedTopic?.topic?.title ?: "Select a topic",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text("Select Revision Intervals (Days)", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    defaultIntervals.forEach { days ->
                        val active = days in selectedIntervals
                        FilterChip(
                            selected = active,
                            onClick = {
                                if (active) selectedIntervals.remove(days) else selectedIntervals.add(days)
                            },
                            label = { Text("${days}d") }
                        )
                    }
                }

                OutlinedTextField(
                    value = customIntervalText,
                    onValueChange = { customIntervalText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Custom Interval in Days (Optional)") },
                    placeholder = { Text("e.g., 5 or 21") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedTopic != null && (selectedIntervals.isNotEmpty() || customIntervalText.isNotBlank()),
                onClick = {
                    val custom = customIntervalText.toIntOrNull()
                    val allIntervals = selectedIntervals.toMutableList()
                    if (custom != null && custom > 0) allIntervals.add(custom)
                    selectedTopic?.let { onSchedule(it, allIntervals) }
                }
            ) { Text("Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showTopicPicker) {
        TopicPickerDialog(
            topics = availableTopics,
            onDismiss = { showTopicPicker = false },
            onSelect = { opt ->
                selectedTopic = opt
                showTopicPicker = false
            }
        )
    }
}

@Composable
private fun TopicPickerDialog(
    topics: List<TopicOption>,
    onDismiss: () -> Unit,
    onSelect: (TopicOption) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(topics, query) {
        val q = query.trim()
        if (q.isBlank()) topics.take(40)
        else topics.filter {
            it.topic.title.contains(q, ignoreCase = true) ||
                it.chapterTitle.contains(q, ignoreCase = true) ||
                it.subjectName.contains(q, ignoreCase = true)
        }.take(40)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Syllabus Topic") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search topic or chapter...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.topic.id }) { opt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(opt) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(opt.topic.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                                Text(
                                    text = "${opt.subjectName} • ${opt.chapterTitle}",
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
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCountdownDialog(
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit
) {
    var examName by remember { mutableStateOf("") }
    var selectedEpochDay by remember { mutableStateOf(LocalDate.now().plusDays(60).toEpochDay()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Examination Countdown") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = examName,
                    onValueChange = { examName = it },
                    label = { Text("Examination Name") },
                    placeholder = { Text("e.g., NEET UG 2027 / HS Final Exam") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exam Date: ${LocalDate.ofEpochDay(selectedEpochDay).format(dateFmt)}")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = examName.isNotBlank(),
                onClick = { onSave(examName, selectedEpochDay) }
            ) { Text("Add Exam") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            selectedEpochDay = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .toEpochDay()
                        }
                        showDatePicker = false
                    }
                ) { Text("Set Date") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
}
