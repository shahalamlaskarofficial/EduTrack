package com.edutrack.app.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.edutrack.app.data.local.AcademicJourneyEntity
import com.edutrack.app.data.local.ThemePreference
import com.edutrack.app.data.preferences.AppPreferences
import com.edutrack.app.data.repository.EduTrackRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Rounded.Home),
    SYLLABUS("syllabus", "Syllabus", Icons.Rounded.MenuBook),
    ROADMAP("roadmap", "Roadmap", Icons.Rounded.Timeline),
    PLANNER("planner", "Planner", Icons.Rounded.CalendarMonth),
    STATISTICS("statistics", "Stats", Icons.Rounded.BarChart),
    SETTINGS("settings", "Settings", Icons.Rounded.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduTrackMainShell(
    repository: EduTrackRepository,
    preferences: AppPreferences,
    onAddNewJourney: () -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val journeys by repository.allJourneysFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeJourney = journeys.firstOrNull { it.id == preferences.activeJourneyId } ?: journeys.firstOrNull()

    val totalTopics by remember(activeJourney?.id) {
        repository.observeTotalTopics(activeJourney?.id ?: -1L)
    }.collectAsStateWithLifecycle(initialValue = 0)

    val completedTopics by remember(activeJourney?.id) {
        repository.observeCompletedTopics(activeJourney?.id ?: -1L)
    }.collectAsStateWithLifecycle(initialValue = 0)

    var showJourneySwitcher by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: BottomNavTab.HOME.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Column {
                        Text(
                            text = "EduTrack",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeJourney != null) {
                            Text(
                                text = activeJourney.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (journeys.isNotEmpty()) {
                        IconButton(onClick = { showJourneySwitcher = true }) {
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = "Switch Academic Journey"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                BottomNavTab.entries.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(21.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavTab.HOME.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(BottomNavTab.HOME.route) {
                HomePhase1Screen(
                    studentName = preferences.studentName,
                    activeJourney = activeJourney,
                    totalTopics = totalTopics,
                    completedTopics = completedTopics,
                    journeyCount = journeys.size,
                    onSwitchJourney = { showJourneySwitcher = true },
                    onAddJourney = onAddNewJourney
                )
            }
            composable(BottomNavTab.SYLLABUS.route) {
                PhaseReadyScreen(
                    title = "Syllabus Tracker",
                    phaseLabel = "Ready for Phase 2",
                    activeJourney = activeJourney,
                    description = "Verified preloaded official syllabus datasets, automatic loading, 4-state topic progress, and Custom Missing Content will plug directly into this Room database in Phase 2."
                )
            }
            composable(BottomNavTab.ROADMAP.route) {
                PhaseReadyScreen(
                    title = "Academic Roadmap",
                    phaseLabel = "Ready for Phase 3",
                    activeJourney = activeJourney,
                    description = "Milestone timeline (First Chapter → 25% → 50% → 75% → 100%) and unlockable achievement badges will activate in Phase 3."
                )
            }
            composable(BottomNavTab.PLANNER.route) {
                PhaseReadyScreen(
                    title = "Study Planner & Revision",
                    phaseLabel = "Ready for Phase 4",
                    activeJourney = activeJourney,
                    description = "Daily missions, weekly goals, spaced-repetition Revision Centre (1/3/7/14/30 days), and WorkManager reminders will connect here in Phase 4."
                )
            }
            composable(BottomNavTab.STATISTICS.route) {
                PhaseReadyScreen(
                    title = "Statistics & Analytics",
                    phaseLabel = "Ready for Phase 5",
                    activeJourney = activeJourney,
                    description = "Subject-wise charts, weekly/monthly completion trends, consistency streaks, and PDF/JSON exports will be added in Phase 5."
                )
            }
            composable(BottomNavTab.SETTINGS.route) {
                SettingsPhase1Screen(
                    preferences = preferences,
                    journeys = journeys,
                    activeJourneyId = activeJourney?.id ?: -1L,
                    onSelectTheme = { theme -> scope.launch { repository.setTheme(theme) } },
                    onToggleNotifications = { enabled ->
                        scope.launch { repository.setNotificationsEnabled(enabled) }
                    },
                    onSelectJourney = { id -> scope.launch { repository.selectActiveJourney(id) } },
                    onDeleteJourney = { journey ->
                        scope.launch { repository.deleteJourney(journey, journeys) }
                    },
                    onAddJourney = onAddNewJourney
                )
            }
        }
    }

    if (showJourneySwitcher) {
        AlertDialog(
            onDismissRequest = { showJourneySwitcher = false },
            title = { Text("Academic Journeys") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    journeys.forEach { journey ->
                        val isActive = journey.id == activeJourney?.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { repository.selectActiveJourney(journey.id) }
                                    showJourneySwitcher = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(journey.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Session: ${journey.academicSession}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isActive) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Active",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showJourneySwitcher = false
                        onAddNewJourney()
                    }
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Journey")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJourneySwitcher = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun HomePhase1Screen(
    studentName: String,
    activeJourney: AcademicJourneyEntity?,
    totalTopics: Int,
    completedTopics: Int,
    journeyCount: Int,
    onSwitchJourney: () -> Unit,
    onAddJourney: () -> Unit
) {
    val progressFraction = if (totalTopics > 0) completedTopics.toFloat() / totalTopics else 0f
    val progressPercent = (progressFraction * 100).toInt()
    val daysRemaining = activeJourney?.targetExamDateEpochDay?.let { targetDay ->
        ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.ofEpochDay(targetDay))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (studentName.isNotBlank()) "Welcome back, $studentName" else "Your Academic Journey",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Every chapter brings you closer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "EDUTRACK • ACTIVE JOURNEY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeJourney?.title ?: "No active journey",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$completedTopics / $totalTopics topics completed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Session: ${activeJourney?.academicSession ?: "-"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (daysRemaining != null) {
                            Text(
                                text = if (daysRemaining >= 0) "$daysRemaining days to exam" else "Exam date passed",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Phase 1 Foundation Active", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Local Room database, DataStore preferences, Dark/Light themes, 6-tab navigation, and multi-journey tracking ($journeyCount active track${if (journeyCount == 1) "" else "s"}) are running offline on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onAddJourney, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add 2nd Journey")
                        }
                        TextButton(onClick = onSwitchJourney) {
                            Text("Switch Track")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhaseReadyScreen(
    title: String,
    phaseLabel: String,
    activeJourney: AcademicJourneyEntity?,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = phaseLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(6.dp))
                if (activeJourney != null) {
                    Text(
                        text = "Bound to: ${activeJourney.title} (Dataset Key: ${activeJourney.syllabusDatasetId})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsPhase1Screen(
    preferences: AppPreferences,
    journeys: List<AcademicJourneyEntity>,
    activeJourneyId: Long,
    onSelectTheme: (ThemePreference) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onSelectJourney: (Long) -> Unit,
    onDeleteJourney: (AcademicJourneyEntity) -> Unit,
    onAddJourney: () -> Unit
) {
    var pendingDelete by remember { mutableStateOf<AcademicJourneyEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }

        // Theme selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemePreference.entries.forEach { mode ->
                            val selected = preferences.themePreference == mode
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectTheme(mode) }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Notifications toggle
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Local Study Reminders", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Offline revision & planner notifications",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.notificationsEnabled,
                        onCheckedChange = onToggleNotifications
                    )
                }
            }
        }

        // Academic Journeys management
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Academic Journeys", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onAddJourney) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Track")
                }
            }
        }

        items(journeys, key = { it.id }) { journey ->
            val isActive = journey.id == activeJourneyId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectJourney(journey.id) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(journey.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${journey.academicSession} • ${if (isActive) "Active" else "Tap to switch"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { pendingDelete = journey }) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Delete Journey",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Privacy & About
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About & Privacy", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "EduTrack v1.0.0 (Phase 1)\nAll academic profiles and syllabus progress stay strictly on your device. No ads, no tracking SDKs, no mandatory cloud account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    pendingDelete?.let { journeyToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Academic Journey?") },
            text = {
                Text("This will permanently remove \"${journeyToDelete.title}\" and all its local progress from this device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteJourney(journeyToDelete)
                        pendingDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
