package com.pupil.app.ui.screens.result

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pupil.app.data.model.Concept
import com.pupil.app.data.model.GradingStatus
import com.pupil.app.ui.components.CreatureState
import com.pupil.app.ui.components.CreatureWidget
import com.pupil.app.ui.components.LatencyBanner
import com.pupil.app.ui.components.MockModeBanner
import com.pupil.app.ui.theme.PupilPrimaryLight
import com.pupil.app.ui.theme.StatusMisconception
import com.pupil.app.ui.theme.StatusPartial
import com.pupil.app.ui.theme.StatusUnderstood

/**
 * SummaryScreen (Screen 6 in Clean Notebook spec):
 * Creature celebration at top, "Nice work!", "+120 XP", Level progress bar,
 * Got it / Shaky / Needs work cards, "Teach the gaps", "Done".
 */
@Composable
fun ResultScreen(
    subjectId: String,
    topicId: String? = null,
    viewModel: ResultViewModel,
    onReteachClick: () -> Unit,
    onBackToTopicsClick: () -> Unit,
    onTeachGapsClick: () -> Unit,
    onOpenGraphClick: (conceptId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val concepts: List<Concept> by remember(subjectId, topicId) {
        if (topicId != null)
            viewModel.getConceptsForTopicFlow(topicId)
        else
            viewModel.getConceptsForSubjectFlow(subjectId)
    }.collectAsState(initial = emptyList())

    val creature by viewModel.creature.collectAsState()
    val lastMetrics by viewModel.lastMetrics.collectAsState()

    val understoodList = concepts.filter { it.status == GradingStatus.UNDERSTOOD }
    val shakyList = concepts.filter { it.status == GradingStatus.PARTIAL }
    val needsWorkList = concepts.filter {
        it.status == GradingStatus.MISCONCEPTION ||
        it.status == GradingStatus.MISSED ||
        it.status == GradingStatus.UNSTUDIED
    }

    val hasGaps = shakyList.isNotEmpty() || needsWorkList.isNotEmpty()
        var selectedCategoryName by remember { mutableStateOf<String?>(null) }
    var selectedCategoryConcepts by remember { mutableStateOf<List<Concept>>(emptyList()) }

    Scaffold(
        topBar = {
            Column {
                MockModeBanner(isMock = viewModel.isMockActive)
                LatencyBanner(metrics = lastMetrics)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackToTopicsClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Creature celebration at top with interactive tap
            var resultCreatureTapped by remember { mutableStateOf(false) }
            val resultState = when {
                resultCreatureTapped -> CreatureState.EXCITED
                !hasGaps -> CreatureState.EXCITED
                else -> CreatureState.HAPPY
            }

            CreatureWidget(
                state = resultState,
                level = creature.level,
                totalXp = creature.totalXp,
                streakDays = creature.streakDays,
                speechBubbleText = if (!hasGaps) "Amazing explanation! I get it completely!" else "Good job! Let's clear up those few gaps soon!",
                showProgressBar = false,
                compact = true,
                onCreatureTapped = {
                    resultCreatureTapped = !resultCreatureTapped
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // "Nice work!"
            Text(
                text = "Nice work!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // "+120 XP" in primary blue
            val sessionEarnedXp = concepts.sumOf { it.earnedXp }
            val currentLevelBase = com.pupil.app.core.creature.CreatureDialogue.getCurrentLevelBaseXp(creature.level)
            val nextLevelBase = com.pupil.app.core.creature.CreatureDialogue.getNextLevelXp(creature.level)
            val levelProgress = if (nextLevelBase > currentLevelBase) {
                ((creature.totalXp - currentLevelBase).toFloat() / (nextLevelBase - currentLevelBase)).coerceIn(0f, 1f)
            } else 1f

            // Dynamic XP announcement
            Text(
                text = if (sessionEarnedXp > 0) "+$sessionEarnedXp XP" else "+0 XP",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PupilPrimaryLight
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Level Progress Bar
            Column(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { levelProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PupilPrimaryLight,
                    trackColor = Color(0xFFE5E7EB)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Level ${creature.level} · ${creature.totalXp} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Breakdown Cards: Got it, Shaky, Needs work
            // 1. Got it (Green)
            BreakdownCard(
                label = "Got it",
                count = understoodList.size,
                badgeColor = StatusUnderstood,
                icon = Icons.Default.Check,
                onClick = {
                    if (understoodList.isNotEmpty()) {
                        selectedCategoryName = "Got it"
                        selectedCategoryConcepts = understoodList
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Shaky (Yellow)
            BreakdownCard(
                label = "Shaky",
                count = shakyList.size,
                badgeColor = StatusPartial,
                icon = Icons.Default.PriorityHigh,
                onClick = {
                    if (shakyList.isNotEmpty()) {
                        selectedCategoryName = "Shaky"
                        selectedCategoryConcepts = shakyList
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Needs work (Orange/Coral)
            BreakdownCard(
                label = "Needs work",
                count = needsWorkList.size,
                badgeColor = StatusMisconception,
                icon = Icons.Default.Close,
                onClick = {
                    if (needsWorkList.isNotEmpty()) {
                        selectedCategoryName = "Needs work"
                        selectedCategoryConcepts = needsWorkList
                    }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons: "Teach the gaps" & "Done"
            if (hasGaps) {
                Button(
                    onClick = onTeachGapsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PupilPrimaryLight,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Teach the gaps",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedButton(
                onClick = onBackToTopicsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal dialog to inspect concepts when a category card is tapped
    val activeCategory = selectedCategoryName
    if (activeCategory != null) {
        AlertDialog(
            onDismissRequest = {
                selectedCategoryName = null
                selectedCategoryConcepts = emptyList()
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedCategoryName = null
                    selectedCategoryConcepts = emptyList()
                }) {
                    Text("Close")
                }
            },
            title = {
                Text(
                    text = "$activeCategory (${selectedCategoryConcepts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    selectedCategoryConcepts.forEach { concept ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = concept.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (concept.evidence.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = concept.evidence,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (concept.followupQuestion.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tip: ${concept.followupQuestion}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PupilPrimaryLight
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun BreakdownCard(
    label: String,
    count: Int,
    badgeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "$label ($count)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
