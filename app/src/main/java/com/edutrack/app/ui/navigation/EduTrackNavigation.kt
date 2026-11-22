package com.edutrack.app.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.edutrack.app.data.local.AcademicJourneyEntity
import com.edutrack.app.data.local.ThemePreference
import com.edutrack.app.data.preferences.AppPreferences
import com.edutrack.app.data.repository.EduTrackRepository
import com.edutrack.app.ui.dashboard.DashboardScreen
import com.edutrack.app.ui.dashboard.DashboardViewModel
import com.edutrack.app.ui.roadmap.RoadmapScreen
import com.edutrack.app.ui.roadmap.RoadmapViewModel
import com.edutrack.app.ui.syllabus.SyllabusScreen
import com.edutrack.app.ui.syllabus.SyllabusViewModel
import com.edutrack.app.ui.theme.SuccessGreen
import com.edutrack.app.ui.theme.WarningAmber
import kotlinx.coroutines.launch

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

    // Auto-sync official syllabus catalog (upgrades existing journeys seamlessly)
    LaunchedEffect(activeJourney?.id) {
        activeJourney?.id?.let { repository.ensureOfficialSyllabusLoaded(it) }
    }

    var showJourneySwitcher by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: BottomNavTab.HOME.route

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

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
                        onClick = { navigateToTab(tab.route) },
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
        val activeId = activeJourney?.id ?: -1L
        NavHost(
            navController = navController,
            startDestination = BottomNavTab.HOME.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(BottomNavTab.HOME.route) {
                val dashboardVm: DashboardViewModel = viewModel(
                    key = "dashboard_$activeId",
                    factory = DashboardViewModel.factory(repository, activeId)
                )
                DashboardScreen(
                    viewModel = dashboardVm,
                    studentName = preferences.studentName,
                    onOpenSyllabus = { navigateToTab(BottomNavTab.SYLLABUS.route) },
                    onOpenRoadmap = { navigateToTab(BottomNavTab.ROADMAP.route) },
                    onOpenPlanner = { navigateToTab(BottomNavTab.PLANNER.route) }
                )
            }
            composable(BottomNavTab.SYLLABUS.route) {
                val syllabusVm: SyllabusViewModel = viewModel(
                    key = "syllabus_$activeId",
                    factory = SyllabusViewModel.factory(repository, activeId)
                )
                SyllabusScreen(viewModel = syllabusVm)
            }
            composable(BottomNavTab.ROADMAP.route) {
                val roadmapVm: RoadmapViewModel = viewModel(
                    key = "roadmap_$activeId",
                    factory = RoadmapViewModel.factory(repository, activeId)
                )
                RoadmapScreen(viewModel = roadmapVm)
            }
            composable(BottomNavTab.PLANNER.route) {
                PhaseReadyScreen(
                    title = "Study Planner & Revision",
                    phaseLabel = "Coming in Phase 4",
                    activeJourney = activeJourney,
                    description = "Daily study missions, weekly goals, spaced-repetition Revision Centre (1/3/7/14/30 days), multi-exam countdowns, and offline reminders will launch in Phase 4."
                )
            }
            composable(BottomNavTab.STATISTICS.route) {
                PhaseReadyScreen(
                    title = "Statistics & Analytics",
                    phaseLabel = "Coming in Phase 5",
                    activeJourney = activeJourney,
                    description = "Subject-wise charts, weekly/monthly completion trends, consistency graphs, JSON Backup/Restore, and PDF Progress Reports will launch in Phase 5."
                )
            }
            composable(BottomNavTab.SETTINGS.route) {
                SettingsScreen(
                    preferences = preferences,
                    journeys = journeys,
                    activeJourney = activeJourney,
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
                TextButton(onClick = { showJourneySwitcher = false }) { Text("Close") }
            }
        )
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
            Column(modifier = Modifier.padding(22.dp)) {
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
                        text = "Active Track: ${activeJourney.title}",
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
private fun SettingsScreen(
    preferences: AppPreferences,
    journeys: List<AcademicJourneyEntity>,
    activeJourney: AcademicJourneyEntity?,
    onSelectTheme: (ThemePreference) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onSelectJourney: (Long) -> Unit,
    onDeleteJourney: (AcademicJourneyEntity) -> Unit,
    onAddJourney: () -> Unit
) {
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<AcademicJourneyEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium) }

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

        activeJourney?.let { journey ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Syllabus Dataset Info", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (journey.isDatasetComplete) "Verified Official" else "Incomplete",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (journey.isDatasetComplete) SuccessGreen else WarningAmber
                            )
                        }
                        Text(
                            text = "Source: ${journey.syllabusSourceRef}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Version: ${journey.syllabusDatasetVersion} • Verified: ${journey.syllabusVerifiedDate}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

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
            val isActive = journey.id == activeJourney?.id
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

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Developer Info", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(10.dp))

                    DeveloperInfoRow(
                        icon = Icons.Rounded.Person,
                        label = "Developer",
                        value = "Shahalam Laskar",
                        onClick = null
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    DeveloperInfoRow(
                        icon = Icons.Rounded.Email,
                        label = "Email",
                        value = "shahalamlaskar.official@gmail.com",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:shahalamlaskar.official@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "EduTrack App Feedback")
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    DeveloperInfoRow(
                        icon = Icons.Rounded.Language,
                        label = "Website",
                        value = "shahalamlaskar.click",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("[https://shahalamlaskar.click](https://shahalamlaskar.click)"))
                            runCatching { context.startActivity(intent) }
                        }
                    )
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
                    Text("About & Privacy", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "EduTrack v1.0.0 (Phase 3)\nEvery chapter brings you closer.\n\nAll academic profiles, syllabus progress, streaks, and achievements stay strictly on your device. No ads, no tracking SDKs, and no cloud account required.",
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
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DeveloperInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
