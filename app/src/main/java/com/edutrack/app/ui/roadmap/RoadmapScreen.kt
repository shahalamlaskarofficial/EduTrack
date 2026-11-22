package com.edutrack.app.ui.roadmap

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edutrack.app.data.local.AchievementEntity
import com.edutrack.app.ui.theme.EduBlue
import com.edutrack.app.ui.theme.SuccessGreen
import com.edutrack.app.ui.theme.WarningAmber

@Composable
fun RoadmapScreen(viewModel: RoadmapViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "milestone_pulse")
    val currentPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp)
    ) {
        // Header Progress Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ACADEMIC ROADMAP",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.journey?.title ?: "Your Journey",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${state.overallPercent}% Complete • ${state.completedChapters}/${state.totalChapters} Chapters • ${state.completedTopics}/${state.totalTopics} Topics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Visual Milestone Timeline
        item {
            Text("Learning Milestones", style = MaterialTheme.typography.titleLarge)
        }

        itemsIndexed(state.milestones, key = { _, m -> m.id }) { index, milestone ->
            val isLast = index == state.milestones.lastIndex
            MilestoneTimelineRow(
                milestone = milestone,
                isLast = isLast,
                pulseScale = if (milestone.state == MilestoneState.CURRENT) currentPulseScale else 1f
            )
        }

        // Achievement Badges Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            val unlockedCount = state.achievements.count { it.unlockedAtMillis != null }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Achievements", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "$unlockedCount / ${state.achievements.size} Unlocked",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        itemsIndexed(state.achievements, key = { _, a -> a.code }) { _, badge ->
            AchievementCard(badge = badge)
        }
    }
}

@Composable
private fun MilestoneTimelineRow(
    milestone: RoadmapMilestone,
    isLast: Boolean,
    pulseScale: Float
) {
    val animatedBar by animateFloatAsState(
        targetValue = milestone.progressFraction,
        animationSpec = tween(durationMillis = 550),
        label = "milestone_bar_${milestone.id}"
    )

    val nodeColor = when (milestone.state) {
        MilestoneState.COMPLETED -> SuccessGreen
        MilestoneState.CURRENT -> EduBlue
        MilestoneState.UPCOMING -> MaterialTheme.colorScheme.outline
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Left timeline track
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(nodeColor.copy(alpha = if (milestone.state == MilestoneState.UPCOMING) 0.25f else 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (milestone.state) {
                        MilestoneState.COMPLETED -> Icons.Rounded.CheckCircle
                        MilestoneState.CURRENT -> Icons.Rounded.Flag
                        MilestoneState.UPCOMING -> Icons.Rounded.Lock
                    },
                    contentDescription = null,
                    tint = nodeColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(64.dp)
                        .background(
                            if (milestone.state == MilestoneState.COMPLETED) SuccessGreen.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.outline
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Milestone Card
        Card(
            modifier = Modifier
                .weight(1f)
                .alpha(if (milestone.state == MilestoneState.UPCOMING) 0.72f else 1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(
                width = if (milestone.state == MilestoneState.CURRENT) 1.5.dp else 1.dp,
                color = when (milestone.state) {
                    MilestoneState.COMPLETED -> SuccessGreen.copy(alpha = 0.5f)
                    MilestoneState.CURRENT -> EduBlue
                    MilestoneState.UPCOMING -> MaterialTheme.colorScheme.outline
                }
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = milestone.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = nodeColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = when (milestone.state) {
                                MilestoneState.COMPLETED -> "Unlocked"
                                MilestoneState.CURRENT -> "Current"
                                MilestoneState.UPCOMING -> "Upcoming"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = nodeColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = milestone.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { animatedBar },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = nodeColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = milestone.progressLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(badge: AchievementEntity) {
    val unlocked = badge.unlockedAtMillis != null
    val accent = if (unlocked) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (unlocked) 1f else 0.65f),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (unlocked) WarningAmber.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = achievementIconFor(badge.code),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(badge.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (unlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                contentDescription = null,
                tint = if (unlocked) SuccessGreen else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun achievementIconFor(code: String): ImageVector = when (code) {
    "FIRST_STEP" -> Icons.Rounded.Star
    "CONSISTENT_LEARNER" -> Icons.Rounded.LocalFireDepartment
    "CHAPTER_MASTER" -> Icons.Rounded.School
    "HALFWAY_THERE" -> Icons.Rounded.Flag
    "REVISION_CHAMPION" -> Icons.Rounded.Refresh
    "SYLLABUS_FINISHER" -> Icons.Rounded.EmojiEvents
    else -> Icons.Rounded.AutoAwesome
}
